package com.tiertagger.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class TierFetcher {
    private static final Gson GSON = new Gson();
    private static final Type TYPE = new TypeToken<Map<String, List<TierEntry>>>() {}.getType();

    private final HttpClient client = HttpClient.newBuilder().build();
    private final TierDataStore store;
    private final String url;

    public TierFetcher(TierDataStore store, String url) {
        this.store = store;
        this.url = url;
    }

    public CompletableFuture<Boolean> refresh() {
        if (url == null || url.isBlank() || url.contains("YOUR_GITHUB_USERNAME")) {
            return CompletableFuture.completedFuture(false);
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        return false;
                    }
                    try {
                        Map<String, List<TierEntry>> parsed = GSON.fromJson(response.body(), TYPE);
                        if (parsed == null) {
                            return false;
                        }
                        store.replace(parsed);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                });
    }
}
