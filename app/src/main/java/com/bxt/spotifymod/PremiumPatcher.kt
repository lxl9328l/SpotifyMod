package com.bxt.spotifymod

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

object PremiumPatcher {

    private val CLASS_HINTS = listOf(
        "ProductState", "SubscriptionState", "UserState", "AccountState", "Premium"
    )
    private val BOOL_TRUE = listOf(
        "premium", "paid", "subscribed", "isAdFree", "canSkip"
    )
    private val BOOL_FALSE = listOf(
        "trial", "isAd", "hasAd", "adsEnabled"
    )
    private val STR_HINTS = listOf(
        "productType", "userType", "tier", "product", "plan"
    )

    fun patch(cl: ClassLoader) {
        val classes = DexScanner.scan(cl) { n ->
            n.startsWith("com.spotify") && CLASS_HINTS.any { n.contains(it) }
        }
        var f = 0
        var m = 0
        for (c in classes) {
            f += patchFields(c)
            m += patchMethods(c)
        }
        XposedBridge.log("[SpotifyMod] premium: classes=${classes.size} fields=$f methods=$m")
    }

    private fun patchFields(c: Class<*>): Int {
        val targets = c.declaredFields.filter { fl ->
            when {
                fl.type == Boolean::class.javaPrimitiveType ->
                    BOOL_TRUE.any { fl.name.contains(it, true) } ||
                        BOOL_FALSE.any { fl.name.contains(it, true) }
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
                            fl.setBoolean(o, BOOL_TRUE.any { fl.name.contains(it, true) })
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
            val nm = mt.name
            if (!BOOL_TRUE.any { nm.contains(it, true) }) continue
            runCatching {
                XposedBridge.hookAllMethods(c, nm, object : XC_MethodHook() {
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
