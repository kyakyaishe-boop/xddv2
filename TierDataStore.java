package com.tiertagger.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class TierDataStore {
    private static final Gson GSON = new Gson();
    private static final Type TYPE = new TypeToken<Map<String, List<TierEntry>>>() {}.getType();

    private final Path cachePath = FabricLoader.getInstance().getConfigDir().resolve("tiertagger-cache.json");
    private Map<String, List<TierEntry>> entries = Map.of();

    public synchronized void loadCache() {
        if (!Files.exists(cachePath)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(cachePath)) {
            Map<String, List<TierEntry>> loaded = GSON.fromJson(reader, TYPE);
            if (loaded != null) {
                entries = loaded;
            }
        } catch (Exception ignored) {
        }
    }

    public synchronized void replace(Map<String, List<TierEntry>> newEntries) throws IOException {
        entries = newEntries;
        Files.createDirectories(cachePath.getParent());
        try (Writer writer = Files.newBufferedWriter(cachePath)) {
            GSON.toJson(entries, TYPE, writer);
        }
    }

    public synchronized List<TierEntry> search(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        List<TierEntry> result = new ArrayList<>();
        for (Map.Entry<String, List<TierEntry>> e : entries.entrySet()) {
            if (q.isEmpty() || e.getKey().toLowerCase().contains(q)) {
                result.addAll(e.getValue());
            }
        }
        result.sort(Comparator.comparing(TierEntry::player).thenComparing(TierEntry::gamemode));
        return result;
    }

    public synchronized int size() {
        return entries.values().stream().mapToInt(List::size).sum();
    }

    public synchronized Map<String, List<TierEntry>> snapshot() {
        return Map.copyOf(entries);
    }
}
