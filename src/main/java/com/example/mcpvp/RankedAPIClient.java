package com.example.mcpvp;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
@Environment(EnvType.CLIENT)
public class RankedAPIClient {
    private static final String BASE_URL = "https://mcpvp-ranked-bot-production.up.railway.app";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();

    public RankedAPIClient() {
        super();
    }

    private static String getSessionToken() {
        return MinecraftClient.getInstance().getSession().getAccessToken();
    }

    public static void registerPlayer(String playerName) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://mcpvp-ranked-bot-production.up.railway.app/register?name=" + playerName))
                .header("Authorization", "Bearer " + getSessionToken())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public static void fetchAllStats(String playerName, Consumer<Map<String, Integer>> callback) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://mcpvp-ranked-bot-production.up.railway.app/get_elo?name=" + playerName))
                .GET()
                .build();
        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(HttpResponse::body).thenAccept(var1x -> {
            try {
                JsonObject json = (JsonObject)GSON.fromJson(var1x, JsonObject.class);
                Map stats = new HashMap();
                String[] kits = new String[]{"Sword", "Axe", "UHC", "Mace", "Spear PVP", "Netherite OP", "Pot", "SMP", "Crystal"};

                for (String kit : kits) {
                    if (json.has(kit)) {
                        stats.put(kit, json.get(kit).getAsInt());
                    } else {
                        stats.put(kit, 0);
                    }
                }

                callback.accept(stats);
            } catch (Exception var9) {
                callback.accept(null);
            }
        });
    }

    public static void fetchLeaderboard(String kitName, Consumer<JsonArray> callback) {
        String encodedKit = kitName.replace(" ", "%20");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://mcpvp-ranked-bot-production.up.railway.app/leaderboard?kit=" + encodedKit))
                .GET()
                .build();
        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(HttpResponse::body).thenAccept(var1x -> {
            try {
                callback.accept((JsonArray)GSON.fromJson(var1x, JsonArray.class));
            } catch (Exception var3x) {
                callback.accept(new JsonArray());
            }
        });
    }

    public static void verifyAccount(String password, String playerName, Consumer<String> callback) {
        JsonObject payload = new JsonObject();
        payload.addProperty("password", password);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://mcpvp-ranked-bot-production.up.railway.app/verify_link?name=" + playerName))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + getSessionToken())
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
                .build();
        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(HttpResponse::body).thenAccept(var1x -> {
            try {
                JsonObject json = (JsonObject)GSON.fromJson(var1x, JsonObject.class);
                callback.accept(json.get("message").getAsString());
            } catch (Exception var3x) {
                callback.accept("Error connecting to verification server.");
            }
        });
    }

    public static void unlinkAccount(String playerName, Consumer<Boolean> callback) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://mcpvp-ranked-bot-production.up.railway.app/unlink_account?name=" + playerName))
                .header("Authorization", "Bearer " + getSessionToken())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(HttpResponse::body).thenAccept(var1x -> {
            try {
                JsonObject json = (JsonObject)GSON.fromJson(var1x, JsonObject.class);
                callback.accept(json.get("status").getAsString().equals("ok"));
            } catch (Exception var3) {
                callback.accept(false);
            }
        });
    }
}
