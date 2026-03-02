package com.example.mcpvp;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Formatting;


@Environment(EnvType.CLIENT)
public class CosmeticManager {
    private static final String API_BASE_URL = "https://mcpvp-ranked-bot-production.up.railway.app";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();
    public static final List<String> ownedTitles = new ArrayList();
    public static String selectedTitle = "";
    public static final Map<String, String> selectedCosmetics = new HashMap();
    public static final Map<String, Integer> peakElo = new HashMap();
    private static final Map<String, CosmeticManager.Rarity> TITLE_RARITIES = new HashMap();

    public CosmeticManager() {
        super();
    }

    public static void init() {
        fetchCosmetics();
    }

    public static Formatting getTitleColor(String title) {
        CosmeticManager.Rarity rarity = TITLE_RARITIES.getOrDefault(title, Rarity.COMMON);
        return rarity.color;
    }

    public static String getSelectedCosmetic(String kitName) {
        return (String)selectedCosmetics.getOrDefault(kitName, "default");
    }

    public static void fetchCosmetics() {
        String playerName = MinecraftClient.getInstance().getSession().getUsername();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://mcpvp-ranked-bot-production.up.railway.app/get_cosmetics?name=" + playerName))
                .GET()
                .build();
        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(HttpResponse::body).thenAccept(var0x -> {
            try {
                JsonObject json = GSON.fromJson(var0x, JsonObject.class);
                if (json.has("titles")) {
                    JsonArray titlesArray = json.getAsJsonArray("titles");
                    synchronized (ownedTitles) {
                        ownedTitles.clear();

                        for (JsonElement e : titlesArray) {
                            ownedTitles.add(e.getAsString());
                        }

                        for (MatchContext.KitType kit : MatchContext.KitType.values()) {
                            int xp = MatchContext.kitMasteryXP.getOrDefault(kit, 0);
                            if (MatchContext.getMasteryLevel(xp) >= 51) {
                                String masteryTitle = kit.apiName + " Master";
                                if (!ownedTitles.contains(masteryTitle)) {
                                    ownedTitles.add(masteryTitle);
                                }
                            }
                        }
                    }
                }

                if (json.has("selected_title")) {
                    selectedTitle = json.get("selected_title").getAsString();
                }

                if (json.has("selected_cosmetics")) {
                    JsonObject cosmetics = json.getAsJsonObject("selected_cosmetics");
                    synchronized (selectedCosmetics) {
                        selectedCosmetics.clear();

                        for (Map.Entry<String, JsonElement> entry : cosmetics.entrySet()) {
                            selectedCosmetics.put(entry.getKey(), entry.getValue().getAsString());
                        }
                    }
                }

                if (json.has("peak_elo")) {
                    JsonObject peak = json.getAsJsonObject("peak_elo");
                    synchronized (peakElo) {
                        peakElo.clear();

                        for (Map.Entry<String, JsonElement> entry : peak.entrySet()) {
                            peakElo.put(entry.getKey(), entry.getValue().getAsInt());
                        }
                    }
                }
            } catch (Exception var16) {
            }
        });
    }

    public static void updateSelectedTitle(String title) {
        selectedTitle = title;
        String playerName = MinecraftClient.getInstance().getSession().getUsername();
        JsonObject payload = new JsonObject();
        payload.addProperty("selected_title", title);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://mcpvp-ranked-bot-production.up.railway.app/update_cosmetics?name=" + playerName))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
                .build();
        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    public static void updateKitCosmetic(String kitName, String cosmetic) {
        selectedCosmetics.put(kitName, cosmetic);
        String playerName = MinecraftClient.getInstance().getSession().getUsername();
        JsonObject cosmeticsPayload = new JsonObject();
        cosmeticsPayload.addProperty(kitName, cosmetic);
        JsonObject payload = new JsonObject();
        payload.add("selected_cosmetics", cosmeticsPayload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://mcpvp-ranked-bot-production.up.railway.app/update_cosmetics?name=" + playerName))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
                .build();
        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    static {
        TITLE_RARITIES.put("Beater", CosmeticManager.Rarity.LEGENDARY);

        for (MatchContext.KitType kit : MatchContext.KitType.values()) {
            TITLE_RARITIES.put(kit.apiName + " Master", CosmeticManager.Rarity.LEGENDARY);
        }
    }

    @Environment(EnvType.CLIENT)
    public static enum Rarity {
        COMMON(Formatting.WHITE),
        UNCOMMON(Formatting.GREEN),
        RARE(Formatting.BLUE),
        EPIC(Formatting.DARK_PURPLE),
        LEGENDARY(Formatting.GOLD);

        public final Formatting color;

        Rarity(Formatting color) {
            this.color = color;
        }
    }
}