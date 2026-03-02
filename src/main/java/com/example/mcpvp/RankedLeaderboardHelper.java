package com.example.mcpvp;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class RankedLeaderboardHelper {
    private static final String API_BASE_URL = "https://mcpvp-ranked-bot-production.up.railway.app";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();
    public static void getLeaderboard(String kitName, Consumer<List<LeaderboardEntry>> callback) {
        String encodedKit = kitName.replace(" ", "%20");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://mcpvp-ranked-bot-production.up.railway.app/leaderboard?kit=" + encodedKit))
                .GET()
                .build();
        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(
                        var2x -> {
                            List entries = new ArrayList();

                            try {
                                JsonArray array = GSON.fromJson(var2x, JsonArray.class);
                                int position = 1;

                                for (JsonElement element : array) {
                                    JsonObject obj = element.getAsJsonObject();
                                    String name = obj.get("name").getAsString();
                                    int elo = obj.has(kitName) ? obj.get(kitName).getAsInt() : 0;
                                    entries.add(
                                            new RankedLeaderboardHelper.LeaderboardEntry(
                                                    name, elo, MatchContext.getRankDisplay(elo, position), MatchContext.getEloColor(elo, position), getRankItem(elo, position)
                                            )
                                    );
                                    position++;
                                }

                                callback.accept(entries);
                            } catch (Exception var11) {
                                callback.accept(new ArrayList());
                            }
                        }
                );
    }

    private static Item getRankItem(int pos, int elo) {
        if (pos <= 100 && elo >= 3400) {
            return Items.DRAGON_BREATH;
        } else if (elo >= 3400) {
            return Items.NETHERITE_INGOT;
        } else if (elo >= 2800) {
            return Items.DIAMOND;
        } else if (elo >= 2400) {
            return Items.EMERALD;
        } else if (elo >= 2000) {
            return Items.AMETHYST_SHARD;
        } else if (elo >= 1600) {
            return Items.GOLD_INGOT;
        } else if (elo >= 1200) {
            return Items.LAPIS_LAZULI;
        } else if (elo >= 800) {
            return Items.IRON_INGOT;
        } else if (elo >= 400) {
            return Items.COAL;
        } else {
            return elo > 0 ? Items.LEATHER : Items.BARRIER;
        }
    }

    @Environment(EnvType.CLIENT)
    public record LeaderboardEntry(String name, int elo, String rankName, Formatting color, Item icon) {
        public LeaderboardEntry(String name, int elo, String rankName, Formatting color, Item icon) {
            this.name = name;
            this.elo = elo;
            this.rankName = rankName;
            this.color = color;
            this.icon = icon;
        }
    }
}
