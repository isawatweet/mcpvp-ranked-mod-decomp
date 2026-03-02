package com.example.mcpvp;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
@Environment(EnvType.CLIENT)
public class VersionManager {
    public static final String CURRENT_VERSION = "1.4.1-Beta";
    private static final String API_BASE_URL = "https://mcpvp-ranked-bot-production.up.railway.app";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public VersionManager() {
        super();
    }

    public static void init() {
        checkVersion();
        scheduler.scheduleAtFixedRate(VersionManager::checkVersion, 5L, 5L, TimeUnit.MINUTES);
    }

    public static void checkVersion() {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://mcpvp-ranked-bot-production.up.railway.app/get_version")).GET().build();
        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(HttpResponse::body).thenAccept(var0x -> {
            try {
                JsonObject json = GSON.fromJson(var0x, JsonObject.class);
                String latestVersion = json.get("version").getAsString();
//                if (!"1.4.1-Beta".equals(latestVersion)) {
//                    MinecraftClient.getInstance().execute(() -> {
//                        if (!(MinecraftClient.getInstance().currentScreen instanceof UpdateRequiredScreen)) {
//                            MinecraftClient.getInstance().setScreen(new UpdateRequiredScreen());
//                        }
//                    });
//                }
            } catch (Exception e) {}
        });
    }
}
