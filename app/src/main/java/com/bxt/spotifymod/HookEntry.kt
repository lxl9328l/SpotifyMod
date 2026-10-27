package com.bxt.spotifymod

import android.app.Application
import android.app.Instrumentation
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class HookEntry : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TARGET_PKG) return
        
        // 弹窗确认模块活着
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(lpparam.classLoader.loadClass("android.app.ActivityThread").getMethod("currentApplication").invoke(null) as android.content.Context, "SpotifyMod 已成功注入！", Toast.LENGTH_LONG).show()
        }

        XposedHelpers.findAndHookMethod(
            Instrumentation::class.java,
            "callApplicationOnCreate",
            Application::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val cl = lpparam.classLoader
                    runCatching { PremiumPatcher.patch(cl) }
                    runCatching { AdPatcher.patch(cl) }
                }
            }
        )
    }

    companion object {
        const val TARGET_PKG = "com.spotify.music"
    }
}
