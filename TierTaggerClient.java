package com.tiertagger.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TierTaggerClient implements ClientModInitializer {
    private static final Gson GSON = new Gson();
    private static final String DEFAULT_URL =
            "https://raw.githubusercontent.com/YOUR_GITHUB_USERNAME/YOUR_REPO/main/data/tiers.json";

    private TierDataStore store;
    private TierFetcher fetcher;
    private KeyMapping openKey;
    private long ticksUntilRefresh;

    @Override
    public void onInitializeClient() {
        store = new TierDataStore();
        store.loadCache();

        Config config = loadConfig();
        fetcher = new TierFetcher(store, config.dataUrl());

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("tiertagger", "main")
        );

        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tiertagger.open",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_RIGHT_SHIFT,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                client.setScreen(new TierScreen(store));
            }

            if (ticksUntilRefresh > 0) {
                ticksUntilRefresh--;
            } else {
                ticksUntilRefresh = Math.max(200L, config.refreshSeconds() * 20L);
                fetcher.refresh();
            }
        });

        ticksUntilRefresh = 20;
    }

    private Config loadConfig() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("tiertagger.json");
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                try (InputStream in = TierTaggerClient.class.getClassLoader()
                        .getResourceAsStream("tiertagger.json")) {
                    if (in != null) {
                        Files.copy(in, path);
                    } else {
                        Files.writeString(path,
                                "{\"dataUrl\":\"" + DEFAULT_URL + "\",\"refreshSeconds\":60}\n",
                                StandardCharsets.UTF_8);
                    }
                }
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                String url = json != null && json.has("dataUrl")
                        ? json.get("dataUrl").getAsString() : DEFAULT_URL;
                int seconds = json != null && json.has("refreshSeconds")
                        ? Math.max(10, json.get("refreshSeconds").getAsInt()) : 60;
                return new Config(url, seconds);
            }
        } catch (Exception e) {
            return new Config(DEFAULT_URL, 60);
        }
    }

    private record Config(String dataUrl, int refreshSeconds) {}
}
