package com.bxt.spotifymod

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

object PremiumPatcher {

    // 填入刚刚扫出来的真类名关键字
    private val CLASS_HINTS = listOf(
        "PremiumBadge", "SubscriptionState", "UserState", "ProductState"
    )
    private val BOOL_TRUE = listOf(
        "premium", "paid", "subscribed", "isAdFree", "canSkip"
    )
    private val STR_HINTS = listOf(
        "productType", "userType", "tier", "product", "plan"
    )

    fun patch(cl: ClassLoader): String {
        val classes = DexScanner.scan(cl) { n ->
            n.startsWith("com.spotify") && CLASS_HINTS.any { n.contains(it) }
        }
        var f = 0
        var m = 0
        for (c in classes) {
            f += patchFields(c)
            m += patchMethods(c)
        }
        return "P(类:${classes.size} 字段:$f 方法:$m)"
    }

    private fun patchFields(c: Class<*>): Int {
        val targets = c.declaredFields.filter { fl ->
            when {
                fl.type == Boolean::class.javaPrimitiveType ->
                    BOOL_TRUE.any { fl.name.contains(it, true) }
                fl.type == String::class.java ->
                    STR_HINTS.any { fl.name.contains(it, true) }
                else -> false
            }
        }
        if (targets.isEmpty()) return 0
        for (fl in targets) fl.isAccessible = true

        XposedBridge.hookAllConstructors(c, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val o = param.thisObject ?: return
                for (fl in targets) {
                    runCatching {
                        if (fl.type == Boolean::class.javaPrimitiveType) {
                            fl.setBoolean(o, true)
                        } else {
                            fl.set(o, "premium")
                        }
                    }
                }
            }
        })
        return targets.size
    }

    private fun patchMethods(c: Class<*>): Int {
        var n = 0
        for (mt in c.declaredMethods) {
            if (mt.returnType != Boolean::class.javaPrimitiveType) continue
            if (!BOOL_TRUE.any { mt.name.contains(it, true) }) continue
            runCatching {
                XposedBridge.hookAllMethods(c, mt.name, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = true
                    }
                })
                n++
            }
        }
        return n
    }
}
