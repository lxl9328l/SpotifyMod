package com.bxt.spotifymod

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

object PremiumPatcher {

    fun patch(cl: ClassLoader): String {
        val classes = DexScanner.scan(cl) { n ->
            n.startsWith("com.spotify") && (
                n.contains("State") || n.contains("User") || n.contains("Product") || n.contains("Account") || n.contains("Subscription")
            )
        }
        var f = 0
        var m = 0
        
        for (c in classes) {
            // 强改字段
            val targets = c.declaredFields.filter { fl ->
                fl.type == Boolean::class.javaPrimitiveType && (
                    fl.name.contains("premium", true) || 
                    fl.name.contains("paid", true) || 
                    fl.name.contains("free", true) ||
                    fl.name.contains("ads", true)
                )
            }
            if (targets.isNotEmpty()) {
                for (fl in targets) fl.isAccessible = true
                XposedBridge.hookAllConstructors(c, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val o = param.thisObject ?: return
                        for (fl in targets) {
                            runCatching {
                                if (fl.name.contains("premium", true) || fl.name.contains("paid", true)) {
                                    fl.setBoolean(o, true)
                                } else {
                                    fl.setBoolean(o, false)
                                }
                            }
                        }
                    }
                })
                f += targets.size
            }
            
            // 强改方法
            for (mt in c.declaredMethods) {
                if (mt.returnType != Boolean::class.javaPrimitiveType) continue
                val nm = mt.name
                if (nm.contains("premium", true) || nm.contains("paid", true) || nm.contains("subscribed", true) || nm.contains("adFree", true)) {
                    runCatching {
                        XposedBridge.hookAllMethods(c, nm, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                param.result = true
                            }
                        })
                        m++
                    }
                }
            }
        }
        return "P(类:${classes.size} 字段:$f 方法:$m)"
    }
}
