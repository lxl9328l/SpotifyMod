package com.bxt.spotifymod

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

object AdPatcher {

    fun patch(cl: ClassLoader): String {
        // 全量扫描所有 Spotify 类
        val classes = DexScanner.scan(cl) { n -> n.startsWith("com.spotify") }
        var h = 0
        
        // 暴力拦截：只要类名带这些词，全杀
        val targetKeywords = listOf("Ad", "Ads", "AdBreak", "AdSlot", "AdsMode", "AdQueue")
        
        for (c in classes) {
            // 过滤出广告相关的类
            if (!targetKeywords.any { c.name.contains(it) }) continue

            for (mt in c.declaredMethods) {
                val ret = mt.returnType
                
                // 1. 只要是返回 List 的广告列表，统统变成空列表
                if (ret == List::class.java) {
                    runCatching {
                        XposedBridge.hookAllMethods(c, mt.name, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                param.result = emptyList<Any>()
                            }
                        })
                        h++
                    }
                }
                
                // 2. 只要是返回 boolean 的，如果是获取广告就返回 false
                if (ret == Boolean::class.javaPrimitiveType) {
                    runCatching {
                        XposedBridge.hookAllMethods(c, mt.name, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                if (mt.name.contains("premium", true) || mt.name.contains("subscribed", true)) {
                                    param.result = true
                                } else {
                                    param.result = false
                                }
                            }
                        })
                        h++
                    }
                }
            }
        }
        return "狂暴拦截 A(挂载:$h)"
    }
}
