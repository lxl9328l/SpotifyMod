package com.bxt.spotifymod

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

object AdPatcher {

    // 精准命中播放器的 AdBreak 上下文
    private val CLASS_HINTS = listOf(
        "AdBreakContext", "AdsModeModel", "AdQueue", "AdsOverlay"
    )

    fun patch(cl: ClassLoader): String {
        val classes = DexScanner.scan(cl) { n ->
            n.startsWith("com.spotify") && CLASS_HINTS.any { n.contains(it) }
        }
        var h = 0
        for (c in classes) {
            // 1. 干掉广告队列、拦截 Context 里的广告间隙
            for (mt in c.declaredMethods) {
                val nm = mt.name
                // 找到返回 List 的广告队列方法，直接返回空
                if (mt.returnType == List::class.java && (nm.contains("queue", true) || nm.contains("ads", true))) {
                    runCatching {
                        XposedBridge.hookAllMethods(c, nm, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                param.result = emptyList<Any>()
                            }
                        })
                        h++
                    }
                }
                // 找到判断是不是广告的属性
                if (mt.returnType == Boolean::class.javaPrimitiveType && (nm.contains("isAd", true) || nm.contains("shouldShow", true))) {
                    runCatching {
                        XposedBridge.hookAllMethods(c, nm, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                param.result = false
                            }
                        })
                        h++
                    }
                }
            }
        }
        return "A(类:${classes.size} 挂载:$h)"
    }
}
