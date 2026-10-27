package com.bxt.spotifymod

import android.app.Application
import android.app.Instrumentation
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class HookEntry : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TARGET_PKG) return

        XposedHelpers.findAndHookMethod(
            Instrumentation::class.java,
            "callApplicationOnCreate",
            Application::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val cl = lpparam.classLoader
                    val context = param.args[0] as Context
                    
                    val premResult = runCatching { PremiumPatcher.patch(cl) }
                        .getOrElse { "P-错误:${it.message}" }
                        
                    val adResult = runCatching { AdPatcher.patch(cl) }
                        .getOrElse { "A-错误:${it.message}" }
                        
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            context,
                            "注入成功\n$premResult\n$adResult",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }

    companion object {
        const val TARGET_PKG = "com.spotify.music"
    }
}
