package com.example.filter.etc;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.filter.items.StickerItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class StickerStore {
    private static final StickerStore INSTANCE = new StickerStore();
    private static final String PREF = "stickers.pref";
    private static final String KEY_ALL = "all";
    private volatile boolean loaded = false;
    private Context appCtx;
    private final List<StickerItem> all = new ArrayList<>();
    private final Deque<StickerItem> pending = new ArrayDeque<>();

    private StickerStore() {
    }

    public static StickerStore get() {
        return INSTANCE;
    }

    public synchronized void init(Context context) {
        if (appCtx == null) {
            appCtx = context.getApplicationContext();
        }
        ensureLoaded();
    }

    private SharedPreferences sp() {
        if (appCtx == null) throw new IllegalStateException("StickerStore.init(context) 먼저 호출하세요.");
        return appCtx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private synchronized void ensureLoaded() {
        if (loaded) return;
        String json = sp().getString(KEY_ALL, null);
        if (json != null) {
            try {
                JSONArray arr = new JSONArray(json);
                all.clear();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    StickerItem it = StickerItem.fromJson(o);
                    if (it != null) all.add(it);
                }
            } catch (JSONException ignored) {}
        }
        loaded = true;
    }

    private void saveAsync() {
        if (appCtx == null) return;
        final ArrayList<StickerItem> snapshot;
        synchronized (this) { snapshot = new ArrayList<>(all); }
        new Thread(() -> {
            JSONArray arr = new JSONArray();
            for (StickerItem it : snapshot) {
                try { arr.put(it.toJson()); } catch (JSONException ignored) {}
            }
            sp().edit().putString(KEY_ALL, arr.toString()).apply();
        }).start();
    }

    public synchronized List<StickerItem> snapshotAll() {
        ensureLoaded();
        return new ArrayList<>(all);
    }

    public synchronized void seedDefaultsIfEmpty(List<StickerItem> defaults) {
        ensureLoaded();
        if (all.isEmpty() && defaults != null && !defaults.isEmpty()) {
            all.addAll(defaults);
            saveAsync();
        }
    }

    public synchronized void enqueuePending(StickerItem item) {
        pending.addLast(item);
    }

    public synchronized void addToAllFront(StickerItem item) {
        ensureLoaded();
        all.add(0, item);
        saveAsync();
    }

    public synchronized StickerItem pollPending() {
        return pending.pollFirst();
    }

    public synchronized boolean removeByKey(String key) {
        ensureLoaded();
        if (key == null) return false;
        for (int i = 0; i < all.size(); i++) {
            StickerItem it = all.get(i);
            String k = it.key();
            if (key.equals(k)) {
                all.remove(i);
                saveAsync();
                return true;
            }
        }
        return false;
    }
}

