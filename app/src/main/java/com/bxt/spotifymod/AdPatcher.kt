package com.bxt.spotifymod

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

object AdPatcher {

    private val CLASS_HINTS = listOf(
        "Ad", "Ads", "AdSlot", "AdBreak", "AdPlayback",
        "AdRules", "AdManager", "AdModel", "AdState", "SpotifyAd"
    )
    private val BOOL_HINTS = listOf(
        "shouldShow", "isAd", "hasAd", "adsEnabled",
        "isAdPlaying", "canShow", "getIsAd", "shouldPlay"
    )
    private val CORE_HINTS = listOf("advertisement", "adslot", "adbreak")

    fun patch(cl: ClassLoader) {
        val classes = DexScanner.scan(cl) { n ->
            n.startsWith("com.spotify") && (
                CLASS_HINTS.any { n.contains(it) } ||
                    CORE_HINTS.any { n.lowercase().contains(it) }
                )
        }
        var h = 0
        for (c in classes) {
            for (mt in c.declaredMethods) {
                val nm = mt.name
                if (BOOL_HINTS.any { nm.equals(it, true) || nm.startsWith(it, true) }) {
                    runCatching {
                        val ret = mt.returnType
                        XposedBridge.hookAllMethods(c, nm, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                if (ret == Boolean::class.javaPrimitiveType) {
                                    param.result = false
                                } else if (ret != Void.TYPE) {
                                    param.result = null
                                }
                            }
                        })
                        h++
                    }
                }
            }

            for (mt in c.declaredMethods) {
                if (mt.returnType == List::class.java && mt.name.contains("ad", true)) {
                    runCatching {
                        XposedBridge.hookAllMethods(c, mt.name, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                param.result = emptyList<Any>()
                            }
                        })
                        h++
                    }
                }
            }
        }
        XposedBridge.log("[SpotifyMod] ads: classes=${classes.size} hooks=$h")
    }
}
