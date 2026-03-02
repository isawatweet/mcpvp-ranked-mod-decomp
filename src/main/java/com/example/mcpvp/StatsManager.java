package com.example.mcpvp;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;


@Environment(EnvType.CLIENT)
public class StatsManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File configFile;
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Set<String> fetchingPlayers = new HashSet();
    private static boolean hasSuccessfullyPulled = false;
    private static boolean isSyncing = false;
    private static final String API_BASE_URL = "https://mcpvp-ranked-bot-production.up.railway.app";
    public static final Map<String, Map<String, String>> playerKitCosmetics = new HashMap();

    public StatsManager() {
        super();
    }

    public static void init() {
        configFile = new File(MinecraftClient.getInstance().runDirectory, "config/mcpvp_stats.json");
        load();
    }

    private static String getSessionToken() {
        return /* MinecraftClient.getInstance().getSession().getAccessToken()*/ "0";
    }

    public static void save() {
        try {
            FileWriter writer = new FileWriter(configFile);

            try {
                JsonObject root = new JsonObject();
                root.add("elo", GSON.toJsonTree(MatchContext.kitElo));
                root.add("mastery_xp", GSON.toJsonTree(MatchContext.kitMasteryXP));
                root.add("matches", GSON.toJsonTree(MatchContext.placementMatches));
                root.add("wins", GSON.toJsonTree(MatchContext.placementWins));
                root.add("total_matches", GSON.toJsonTree(MatchContext.totalMatches));
                root.add("total_wins", GSON.toJsonTree(MatchContext.totalWins));
                root.add("win_streaks", GSON.toJsonTree(MatchContext.winStreak));
                root.addProperty("public", MatchContext.statsPublic);
                root.addProperty("ranked_enabled", MatchContext.rankedEnabled);
                root.addProperty("pending_abandonment_penalty", MatchContext.pendingAbandonmentPenalty);
                if (MatchContext.abandonedKit != null) {
                    root.addProperty("abandoned_kit", MatchContext.abandonedKit.name());
                }

                GSON.toJson(root, writer);
            } catch (Throwable var4) {
                try {
                    writer.close();
                } catch (Throwable var3) {
                    var4.addSuppressed(var3);
                }

                throw var4;
            }

            writer.close();
        } catch (IOException var5) {
            var5.printStackTrace();
        }
    }

    public static void load() {
        if (configFile.exists()) {
            try {
                FileReader reader = new FileReader(configFile);

                try {
                    JsonObject root = (JsonObject)GSON.fromJson(reader, JsonObject.class);
                    if (root != null) {
                        Type type = (new TypeToken<Map<MatchContext.KitType, Integer>>() {}).getType();
                        if (root.has("elo")) {
                            Map elo = (Map<MatchContext.KitType, Integer>)GSON.fromJson(root.get("elo"), type);
                            if (elo != null) {
                                MatchContext.kitElo.putAll(elo);
                            }
                        }

                        if (root.has("mastery_xp")) {
                            Map mastery = (Map<MatchContext.KitType, Integer>)GSON.fromJson(root.get("mastery_xp"), type);
                            if (mastery != null) {
                                MatchContext.kitMasteryXP.putAll(mastery);
                            }
                        }

                        if (root.has("matches")) {
                            Map matches = (Map<MatchContext.KitType, Integer>)GSON.fromJson(root.get("matches"), type);
                            if (matches != null) {
                                MatchContext.placementMatches.putAll(matches);
                            }
                        }

                        if (root.has("wins")) {
                            Map wins = (Map<MatchContext.KitType, Integer>)GSON.fromJson(root.get("wins"), type);
                            if (wins != null) {
                                MatchContext.placementWins.putAll(wins);
                            }
                        }

                        if (root.has("total_matches")) {
                            Map totalMatches = (Map<MatchContext.KitType, Integer>)GSON.fromJson(root.get("total_matches"), type);
                            if (totalMatches != null) {
                                MatchContext.totalMatches.putAll(totalMatches);
                            }
                        }

                        if (root.has("total_wins")) {
                            Map totalWins = (Map<MatchContext.KitType, Integer>)GSON.fromJson(root.get("total_wins"), type);
                            if (totalWins != null) {
                                MatchContext.totalWins.putAll(totalWins);
                            }
                        }

                        if (root.has("win_streaks")) {
                            Map streaks = (Map<MatchContext.KitType, Integer>)GSON.fromJson(root.get("win_streaks"), type);
                            if (streaks != null) {
                                MatchContext.winStreak.putAll(streaks);
                            }
                        }

                        if (root.has("public")) {
                            MatchContext.statsPublic = root.get("public").getAsBoolean();
                        }

                        if (root.has("ranked_enabled")) {
                            MatchContext.rankedEnabled = root.get("ranked_enabled").getAsBoolean();
                        }

                        if (root.has("pending_abandonment_penalty")) {
                            MatchContext.pendingAbandonmentPenalty = root.get("pending_abandonment_penalty").getAsBoolean();
                        }

                        if (root.has("abandoned_kit")) {
                            MatchContext.abandonedKit = MatchContext.KitType.valueOf(root.get("abandoned_kit").getAsString());
                        }
                    }
                } catch (Throwable var5) {
                    try {
                        reader.close();
                    } catch (Throwable var4) {
                        var5.addSuppressed(var4);
                    }

                    throw var5;
                }

                reader.close();
            } catch (Exception var6) {
            }
        }
    }

    public static void syncWithServer() {
        String playerName = MinecraftClient.getInstance().getSession().getUsername();
        if (playerName != null && !playerName.equals("Player") && !isSyncing) {
            isSyncing = true;
            fetchFullPlayerStats(playerName, var0x -> {
                if (var0x != null) {
                    MatchContext.isBanned = var0x.isBanned();
                    MatchContext.banReason = var0x.banReason();
                    if (var0x.selectedCosmetics() != null) {
                        synchronized (CosmeticManager.selectedCosmetics) {
                            CosmeticManager.selectedCosmetics.clear();
                            CosmeticManager.selectedCosmetics.putAll(var0x.selectedCosmetics());
                        }
                    }

                    if (var0x.kitElo() != null) {
                        for (Map.Entry entry : var0x.kitElo().entrySet()) {
                            MatchContext.kitElo.put((MatchContext.KitType) entry.getKey(), (Integer) entry.getValue());
                        }
                    }

                    if (var0x.kitPlacements() != null) {
                        for (Map.Entry entry : var0x.kitPlacements().entrySet()) {
                            MatchContext.placementMatches.put((MatchContext.KitType) entry.getKey(), (Integer) entry.getValue());
                        }
                    }

                    if (var0x.totalMatches() != null) {
                        MatchContext.totalMatches.putAll(var0x.totalMatches());
                    }

                    if (var0x.totalWins() != null) {
                        MatchContext.totalWins.putAll(var0x.totalWins());
                    }

                    if (var0x.kitMasteryXP() != null) {
                        MatchContext.kitMasteryXP.putAll(var0x.kitMasteryXP());
                    }

                    MatchContext.statsPublic = var0x.isPublic();
                    hasSuccessfullyPulled = true;
                    save();
                }

                isSyncing = false;
            });
        }
    }

    public static void pushToGlobal() {
        if (!hasSuccessfullyPulled) {
            syncWithServer();
        } else {
            String playerName = MinecraftClient.getInstance().getSession().getUsername();
            if (playerName != null && !playerName.equals("Player")) {
                Map apiPayload = new HashMap();
                apiPayload.put("public", MatchContext.statsPublic);
                apiPayload.put("version", "1.4.1-Beta");
                Map<String, Integer> masteryMap = new HashMap<>();

                for (Map.Entry<MatchContext.KitType, Integer> entry : MatchContext.kitMasteryXP.entrySet()) {
                    masteryMap.put(entry.getKey().apiName, entry.getValue());
                }

                apiPayload.put("mastery_xp", masteryMap);
                String json = GSON.toJson(apiPayload);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://mcpvp-ranked-bot-production.up.railway.app/update_settings?name=" + playerName))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + getSessionToken())
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            }
        }
    }

    public static void fetchFullPlayerStats(String playerName, Consumer<PlayerStats> callback) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://mcpvp-ranked-bot-production.up.railway.app/get_elo?name=" + playerName))
                .GET()
                .build();
        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(
                        var2x -> {
                            Map elos = new HashMap();
                            Map mastery = new HashMap();
                            Map placements = new HashMap();
                            Map totalMatches = new HashMap();
                            Map totalWins = new HashMap();
                            Map cosmetics = new HashMap();
                            List history = new ArrayList();
                            boolean isBanned = false;
                            String banReason = "";
                            String selectedTitle = "";

                            try {
                                JsonObject json = (JsonObject)GSON.fromJson(var2x, JsonObject.class);
                                if (json.has("is_banned")) {
                                    isBanned = json.get("is_banned").getAsBoolean();
                                }

                                if (json.has("ban_reason")) {
                                    banReason = json.get("ban_reason").getAsString();
                                }

                                if (json.has("selected_title")) {
                                    selectedTitle = json.get("selected_title").getAsString();
                                }

                                if (json.has("selected_cosmetics")) {
                                    JsonObject cosJson = json.getAsJsonObject("selected_cosmetics");

                                    for (Map.Entry entry : cosJson.entrySet()) {
                                        cosmetics.put(entry.getKey(), entry.getValue().toString());
                                    }
                                }

                                if (json.has("history")) {
                                    for (JsonElement e : json.getAsJsonArray("history")) {
                                        JsonObject obj = e.getAsJsonObject();
                                        history.add(
                                                new MatchHistoryEntry(
                                                        obj.get("opponent").getAsString(),
                                                        obj.get("kit").getAsString(),
                                                        obj.get("won").getAsBoolean(),
                                                        "",
                                                        String.valueOf(obj.get("health").getAsFloat()),
                                                        obj.get("date").getAsString()
                                                )
                                        );
                                    }
                                }

                                JsonObject masteryJson = json.has("mastery_xp") ? json.getAsJsonObject("mastery_xp") : null;
                                JsonObject totalMatchesJson = json.has("total_matches") ? json.getAsJsonObject("total_matches") : null;
                                JsonObject totalWinsJson = json.has("total_wins") ? json.getAsJsonObject("total_wins") : null;
                                JsonObject placementStats = json.has("placement_stats") ? json.getAsJsonObject("placement_stats") : null;
                                JsonObject placementMatches = placementStats != null && placementStats.has("matches") ? placementStats.getAsJsonObject("matches") : null;

                                for (MatchContext.KitType kit : MatchContext.KitType.values()) {
                                    if (json.has(kit.apiName)) {
                                        elos.put(kit, json.get(kit.apiName).getAsInt());
                                    } else {
                                        elos.put(kit, 0);
                                    }

                                    if (masteryJson != null && masteryJson.has(kit.apiName)) {
                                        mastery.put(kit, masteryJson.get(kit.apiName).getAsInt());
                                    } else {
                                        mastery.put(kit, 0);
                                    }

                                    if (placementMatches != null && placementMatches.has(kit.apiName)) {
                                        placements.put(kit, placementMatches.get(kit.apiName).getAsInt());
                                    } else {
                                        placements.put(kit, 0);
                                    }

                                    if (totalMatchesJson != null && totalMatchesJson.has(kit.apiName)) {
                                        totalMatches.put(kit, totalMatchesJson.get(kit.apiName).getAsInt());
                                    } else {
                                        totalMatches.put(kit, 0);
                                    }

                                    if (totalWinsJson != null && totalWinsJson.has(kit.apiName)) {
                                        totalWins.put(kit, totalWinsJson.get(kit.apiName).getAsInt());
                                    } else {
                                        totalWins.put(kit, 0);
                                    }
                                }

                                callback.accept(
                                        new PlayerStats(
                                                playerName, elos, mastery, placements, totalMatches, totalWins, true, history, selectedTitle, cosmetics, isBanned, banReason
                                        )
                                );
                            } catch (Exception var23) {
                                callback.accept(null);
                            }
                        }
                );
    }

    public static void fetchLeaderboard(MatchContext.KitType kit, Consumer<List<LeaderboardEntry>> callback) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://mcpvp-ranked-bot-production.up.railway.app/leaderboard?kit=" + kit.apiName.replace(" ", "%20")))
                .GET()
                .build();
        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(HttpResponse::body).thenAccept(var2x -> {
            List<StatsManager.LeaderboardEntry> entries = new ArrayList();

            try {
                for (JsonElement element : GSON.fromJson(var2x, JsonArray.class)) {
                    JsonObject obj = element.getAsJsonObject();
                    String name = obj.get("name").getAsString();
                    int elo = obj.has(kit.apiName) ? obj.get(kit.apiName).getAsInt() : 0;
                    String title = obj.has("selected_title") ? obj.get("selected_title").getAsString() : "";
                    Map cosmetics = new HashMap();
                    if (obj.has("selected_cosmetics")) {
                        JsonObject cosJson = obj.getAsJsonObject("selected_cosmetics");

                        for (Map.Entry cosEntry : cosJson.entrySet()) {
                            cosmetics.put(cosEntry.getKey(), cosEntry.getValue().toString());
                        }
                    }

                    entries.add(new LeaderboardEntry(name, elo, title, cosmetics));
                    playerKitCosmetics.put(name, cosmetics);
                }
            } catch (Exception var15) {
            }

            entries.sort((var0x, var1x) -> Integer.compare(var1x.elo(), var0x.elo()));
            callback.accept(entries);
        });
    }

    public static void pushMatchResult(MatchContext.KitType kit, boolean won, float health, String opponent) {
        String playerName = MinecraftClient.getInstance().getSession().getUsername();
        JsonObject matchData = new JsonObject();
        matchData.addProperty("opponent", opponent);
        matchData.addProperty("kit", kit.apiName);
        matchData.addProperty("won", won);
        matchData.addProperty("health", health);
        matchData.addProperty("version", "1.4.1-Beta");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://mcpvp-ranked-bot-production.up.railway.app/add_history?name=" + playerName))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + getSessionToken())
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(matchData)))
                .build();
        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(HttpResponse::body).thenAccept(var0x -> {
            try {
                JsonObject json = (JsonObject)GSON.fromJson(var0x, JsonObject.class);
                if (json.has("status") && json.get("status").getAsString().equals("ok")) {
                    int oldElo = json.get("old_elo").getAsInt();
                    int newElo = json.get("new_elo").getAsInt();
                    int baseChange = json.get("base_change").getAsInt();
                    int bonus = json.get("bonus").getAsInt();
                    int placements = json.get("placements").getAsInt();
                    MinecraftClient.getInstance().execute(() -> EloBarRenderer.show(oldElo, newElo, baseChange, bonus, placements));
                }
            } catch (Exception var7) {
                var7.printStackTrace();
            }

            CompletableFuture.delayedExecutor(2L, TimeUnit.SECONDS).execute(StatsManager::syncWithServer);
        });
    }

    public static void fetchPlayerElo(String playerName) {
        if (!fetchingPlayers.contains(playerName)) {
            fetchingPlayers.add(playerName);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://mcpvp-ranked-bot-production.up.railway.app/get_elo?name=" + playerName))
                    .GET()
                    .build();
            HTTP_CLIENT.sendAsync(request, BodyHandlers.ofString()).thenApply(HttpResponse::body).thenAccept(var1x -> {
                try {
                    JsonObject json = (JsonObject)GSON.fromJson(var1x, JsonObject.class);
                    String kitName = MatchContext.currentKit != null ? MatchContext.currentKit.apiName : "Sword";
                    if (json.has(kitName)) {
                        MatchContext.playerEloByName.put(playerName, json.get(kitName).getAsInt());
                    }

                    if (json.has("selected_cosmetics")) {
                        JsonObject cosJson = json.getAsJsonObject("selected_cosmetics");
                        Map cosmetics = new HashMap();

                        for (Entry entry : cosJson.entrySet()) {
                            cosmetics.put(entry.getKey(), entry.getValue().toString());
                        }

                        playerKitCosmetics.put(playerName, cosmetics);
                    }
                } catch (Exception var11) {
                } finally {
                    fetchingPlayers.remove(playerName);
                }
            });
        }
    }

    public static void pushMatchStart() {
        String playerName = MinecraftClient.getInstance().getSession().getUsername();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://mcpvp-ranked-bot-production.up.railway.app/match_start?name=" + playerName + "&version=1.4.1-Beta"))
                .header("Authorization", "Bearer " + getSessionToken())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    @Environment(EnvType.CLIENT)
    public record LeaderboardEntry(String name, int elo, String selectedTitle, Map<String, String> selectedCosmetics) {
        public LeaderboardEntry(String name, int elo, String selectedTitle, Map<String, String> selectedCosmetics) {
            this.name = name;
            this.elo = elo;
            this.selectedTitle = selectedTitle;
            this.selectedCosmetics = selectedCosmetics;
        }
    }

    @Environment(EnvType.CLIENT)
    public record MatchHistoryEntry(String opponent, String kit, boolean won, String eloChange, String health, String date) {
        public MatchHistoryEntry(String opponent, String kit, boolean won, String eloChange, String health, String date) {
            this.opponent = opponent;
            this.kit = kit;
            this.won = won;
            this.eloChange = eloChange;
            this.health = health;
            this.date = date;
        }
    }

    @Environment(EnvType.CLIENT)
    public record PlayerStats(
            String name,
            Map<MatchContext.KitType, Integer> kitElo,
            Map<MatchContext.KitType, Integer> kitMasteryXP,
            Map<MatchContext.KitType, Integer> kitPlacements,
            Map<MatchContext.KitType, Integer> totalMatches,
            Map<MatchContext.KitType, Integer> totalWins,
            boolean isPublic,
            List<MatchHistoryEntry> history,
            String selectedTitle,
            Map<String, String> selectedCosmetics,
            boolean isBanned,
            String banReason
    ) {
        public PlayerStats(
                String name,
                Map<MatchContext.KitType, Integer> kitElo,
                Map<MatchContext.KitType, Integer> kitMasteryXP,
                Map<MatchContext.KitType, Integer> kitPlacements,
                Map<MatchContext.KitType, Integer> totalMatches,
                Map<MatchContext.KitType, Integer> totalWins,
                boolean isPublic,
                List<StatsManager.MatchHistoryEntry> history,
                String selectedTitle,
                Map<String, String> selectedCosmetics,
                boolean isBanned,
                String banReason
        ) {
            this.name = name;
            this.kitElo = kitElo;
            this.kitMasteryXP = kitMasteryXP;
            this.kitPlacements = kitPlacements;
            this.totalMatches = totalMatches;
            this.totalWins = totalWins;
            this.isPublic = isPublic;
            this.history = history;
            this.selectedTitle = selectedTitle;
            this.selectedCosmetics = selectedCosmetics;
            this.isBanned = isBanned;
            this.banReason = banReason;
        }
    }
}
