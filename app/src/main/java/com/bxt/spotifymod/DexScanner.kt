package com.bxt.spotifymod

import dalvik.system.DexFile
import de.robv.android.xposed.XposedBridge

object DexScanner {

    fun scan(cl: ClassLoader, predicate: (String) -> Boolean): List<Class<*>> {
        val out = ArrayList<Class<*>>()
        try {
            val baseDexCl = Class.forName("dalvik.system.BaseDexClassLoader")
            val pathList = baseDexCl.getDeclaredField("pathList").apply { isAccessible = true }.get(cl) ?: return out
            
            val elems = pathList.javaClass.getDeclaredField("dexElements").apply { isAccessible = true }
                .get(pathList) as? Array<*> ?: return out
                
            for (el in elems) {
                if (el == null) continue
                val dexFile = el.javaClass.getDeclaredField("dexFile").apply { isAccessible = true }
                    .get(el) as? DexFile ?: continue
                    
                val en = dexFile.entries()
                while (en.hasMoreElements()) {
                    val name = en.nextElement() as? String ?: continue
                    if (!predicate(name)) continue
                    runCatching { Class.forName(name, false, cl) }.getOrNull()?.let { out.add(it) }
                }
            }
        } catch (t: Throwable) { 
            XposedBridge.log("[SpotifyMod] scan fail: $t") 
        }
        XposedBridge.log("[SpotifyMod] scan hit ${out.size} classes")
        return out
    }
}
