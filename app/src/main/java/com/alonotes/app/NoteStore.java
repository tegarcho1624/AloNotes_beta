package com.alonotes.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Biblioteca de notas y carpetas. SharedPreferences "notes", clave "items" (JSON):
 * [{"i":id,"p":idCarpetaPadre ("" = raíz),"n":nombre,"f":esCarpeta,"t":fecha ms,
 *   "pg":[{"d":"A4","o":"L"|"P","bg":colorARGB}]}]
 * JSON corrupto => lista vacía, sin crash.
 */
final class NoteStore {
    private NoteStore() {}

    static final class Item {
        String id, parent = "", name = "";
        boolean folder;
        long t;
        JSONArray pages = new JSONArray();
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("notes", Context.MODE_PRIVATE);
    }

    private static synchronized List<Item> load(Context c) {
        List<Item> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(sp(c).getString("items", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o == null) continue;
                Item it = new Item();
                it.id = o.optString("i");
                it.parent = o.optString("p", "");
                it.name = o.optString("n");
                it.folder = o.optBoolean("f");
                it.t = o.optLong("t");
                JSONArray pg = o.optJSONArray("pg");
                if (pg != null) it.pages = pg;
                out.add(it);
            }
        } catch (Exception ignored) {
            // JSON dañado: se parte de una lista vacía y la app sigue funcionando.
        }
        return out;
    }

    private static synchronized void save(Context c, List<Item> l) {
        try {
            JSONArray a = new JSONArray();
            for (Item it : l) {
                JSONObject o = new JSONObject();
                o.put("i", it.id);
                o.put("p", it.parent);
                o.put("n", it.name);
                o.put("f", it.folder);
                o.put("t", it.t);
                o.put("pg", it.pages);
                a.put(o);
            }
            sp(c).edit().putString("items", a.toString()).apply();
        } catch (Exception ignored) {
            // Si no se puede guardar, se conserva el estado anterior.
        }
    }

    /** Carpetas primero; luego lo más reciente arriba. */
    static List<Item> children(Context c, String parent) {
        List<Item> out = new ArrayList<>();
        for (Item it : load(c)) if (it.parent.equals(parent)) out.add(it);
        out.sort((x, y) -> x.folder != y.folder ? (x.folder ? -1 : 1) : Long.compare(y.t, x.t));
        return out;
    }

    static Item get(Context c, String id) {
        for (Item it : load(c)) if (it.id.equals(id)) return it;
        return null;
    }

    static synchronized Item add(Context c, String parent, boolean folder, String name) {
        List<Item> all = load(c);
        if (name == null || name.trim().isEmpty()) {
            int n = 1;
            for (Item it : all) if (it.folder == folder) n++;
            name = (folder ? "Carpeta " : "Nota ") + n;
        }
        Item it = new Item();
        it.id = java.util.UUID.randomUUID().toString();
        it.parent = parent;
        it.name = name.trim();
        it.folder = folder;
        it.t = System.currentTimeMillis();
        all.add(it);
        save(c, all);
        return it;
    }

    static synchronized void rename(Context c, String id, String name) {
        List<Item> all = load(c);
        for (Item it : all) if (it.id.equals(id)) { it.name = name.trim(); it.t = System.currentTimeMillis(); }
        save(c, all);
    }

    /** Borra el elemento y, si es carpeta, todo su contenido. */
    static synchronized void delete(Context c, String id) {
        List<Item> all = load(c);
        Set<String> gone = new HashSet<>();
        gone.add(id);
        boolean grew = true;
        while (grew) {
            grew = false;
            for (Item it : all) if (gone.contains(it.parent) && gone.add(it.id)) grew = true;
        }
        List<Item> keep = new ArrayList<>();
        for (Item it : all) if (!gone.contains(it.id)) keep.add(it);
        save(c, keep);
    }

    static synchronized void addPages(Context c, String id, int n, String dim, boolean landscape, int bg) {
        List<Item> all = load(c);
        for (Item it : all) {
            if (!it.id.equals(id)) continue;
            try {
                for (int i = 0; i < n; i++) {
                    JSONObject o = new JSONObject();
                    o.put("d", dim);
                    o.put("o", landscape ? "L" : "P");
                    o.put("bg", bg);
                    it.pages.put(o);
                }
            } catch (Exception ignored) {
                // No debería ocurrir: son tipos simples.
            }
            it.t = System.currentTimeMillis();
        }
        save(c, all);
    }
}
