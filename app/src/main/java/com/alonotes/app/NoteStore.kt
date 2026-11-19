package com.alonotes.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Biblioteca de notas y carpetas. SharedPreferences "notes", clave "items" (JSON):
 * [{"i":id,"p":idCarpetaPadre ("" = raíz),"n":nombre,"f":esCarpeta,"t":fecha ms,
 *   "pg":[{"d":"A4","o":"L"|"P","bg":colorARGB}]}]
 * JSON corrupto => lista vacía, sin crash.
 */
internal object NoteStore {
    class Item {
        var id = ""
        var parent = ""
        var name = ""
        var folder = false
        var t = 0L
        var pages = JSONArray()
    }

    private fun sp(c: Context): SharedPreferences =
        c.getSharedPreferences("notes", Context.MODE_PRIVATE)

    @Synchronized
    private fun load(c: Context): MutableList<Item> {
        val out = ArrayList<Item>()
        try {
            val a = JSONArray(sp(c).getString("items", "[]"))
            for (i in 0 until a.length()) {
                val o = a.optJSONObject(i) ?: continue
                val it = Item()
                it.id = o.optString("i")
                it.parent = o.optString("p", "")
                it.name = o.optString("n")
                it.folder = o.optBoolean("f")
                it.t = o.optLong("t")
                o.optJSONArray("pg")?.let { pg -> it.pages = pg }
                out.add(it)
            }
        } catch (ignored: Exception) {
            // JSON dañado: se parte de una lista vacía y la app sigue funcionando.
        }
        return out
    }

    @Synchronized
    private fun save(c: Context, l: List<Item>) {
        try {
            val a = JSONArray()
            for (it in l) {
                val o = JSONObject()
                o.put("i", it.id)
                o.put("p", it.parent)
                o.put("n", it.name)
                o.put("f", it.folder)
                o.put("t", it.t)
                o.put("pg", it.pages)
                a.put(o)
            }
            sp(c).edit().putString("items", a.toString()).apply()
        } catch (ignored: Exception) {
            // Si no se puede guardar, se conserva el estado anterior.
        }
    }

    /** Carpetas primero; luego lo más reciente arriba. */
    fun children(c: Context, parent: String): List<Item> =
        load(c).filter { it.parent == parent }.sortedWith(Comparator { x, y ->
            if (x.folder != y.folder) (if (x.folder) -1 else 1) else y.t.compareTo(x.t)
        })

    fun get(c: Context, id: String): Item? = load(c).firstOrNull { it.id == id }

    @Synchronized
    fun add(c: Context, parent: String, folder: Boolean, name: String?): Item {
        val all = load(c)
        var nm = name?.trim().orEmpty()
        if (nm.isEmpty()) {
            val n = all.count { it.folder == folder } + 1
            nm = (if (folder) "Carpeta " else "Nota ") + n
        }
        val it = Item()
        it.id = UUID.randomUUID().toString()
        it.parent = parent
        it.name = nm
        it.folder = folder
        it.t = System.currentTimeMillis()
        all.add(it)
        save(c, all)
        return it
    }

    @Synchronized
    fun rename(c: Context, id: String, name: String) {
        val all = load(c)
        for (it in all) if (it.id == id) {
            it.name = name.trim()
            it.t = System.currentTimeMillis()
        }
        save(c, all)
    }

    /** Borra el elemento y, si es carpeta, todo su contenido. */
    @Synchronized
    fun delete(c: Context, id: String) {
        val all = load(c)
        val gone = HashSet<String>()
        gone.add(id)
        var grew = true
        while (grew) {
            grew = false
            for (it in all) if (gone.contains(it.parent) && gone.add(it.id)) grew = true
        }
        save(c, all.filter { !gone.contains(it.id) })
    }

    @Synchronized
    fun addPages(c: Context, id: String, n: Int, dim: String, landscape: Boolean, bg: Int) {
        val all = load(c)
        for (item in all) {
            if (item.id != id) continue
            try {
                for (k in 0 until n) {
                    val o = JSONObject()
                    o.put("d", dim)
                    o.put("o", if (landscape) "L" else "P")
                    o.put("bg", bg)
                    item.pages.put(o)
                }
            } catch (ignored: Exception) {
                // No debería ocurrir: son tipos simples.
            }
            item.t = System.currentTimeMillis()
        }
        save(c, all)
    }
}
