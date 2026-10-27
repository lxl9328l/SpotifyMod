package com.bxt.spotifymod

import android.app.Application
import android.app.Instrumentation
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class HookEntry : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TARGET_PKG) return
        XposedBridge.log("[SpotifyMod] injected into ${lpparam.processName}")

        XposedHelpers.findAndHookMethod(
            Instrumentation::class.java,
            "callApplicationOnCreate",
            Application::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val cl = lpparam.classLoader
                    runCatching { PremiumPatcher.patch(cl) }
                        .onFailure { XposedBridge.log("[SpotifyMod] premium error: $it") }
                    runCatching { AdPatcher.patch(cl) }
                        .onFailure { XposedBridge.log("[SpotifyMod] ads error: $it") }
                }
            }
        )
    }

    companion object {
        const val TARGET_PKG = "com.spotify.music"
    }
}
