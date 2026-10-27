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
import java.io.File

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

                    // 1. 跑原来的打补丁逻辑
                    val premResult = runCatching { PremiumPatcher.patch(cl) }
                        .getOrElse { "P-错误:${it.message}" }
                    val adResult = runCatching { AdPatcher.patch(cl) }
                        .getOrElse { "A-错误:${it.message}" }

                    // 2. 扫全量广告类并写进文件
                    val sb = StringBuilder()
                    sb.append("=== Scan Start ===\n")
                    sb.append("$premResult\n$adResult\n")
                    
                    val adClasses = DexScanner.scan(cl) { n -> 
                        n.startsWith("com.spotify") && (
                            n.contains("ad", true) || 
                            n.contains("advert", true) || 
                            n.contains("banner", true) || 
                            n.contains("sponsor", true)
                        )
                    }
                    sb.append("\n--- Ad Classes Found (${adClasses.size}) ---\n")
                    for (c in adClasses) {
                        sb.append(c.name).append("\n")
                    }

                    // 3. 写入手机沙盒目录，方便查看
                    val file = File(context.getExternalFilesDir(null), "scan_result.txt")
                    file.writeText(sb.toString())

                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            context,
                            "注入成功，结果已写入文件！\n$premResult\n$adResult",
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
