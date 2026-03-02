package com.example.mcpvp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
public class SkinManager {
    private static final Map<String, SkinTextures> cache = new HashMap();
    private static final Set<String> fetching = new HashSet();
    private static final Gson GSON = new Gson();

    public SkinManager() {
        super();
    }

    public static SkinTextures getSkin(String name) {
        if (cache.containsKey(name)) {
            return (SkinTextures)cache.get(name);
        } else {
            MinecraftClient client = MinecraftClient.getInstance();
            if (name.equals(client.getSession().getUsername()) && client.player != null) {
                SkinTextures textures = client.getSkinProvider().getSkinTextures(client.player.getGameProfile());
                cache.put(name, textures);
                return textures;
            } else {
                if (client.getNetworkHandler() != null) {
                    PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(name);
                    if (entry != null) {
                        SkinTextures textures = entry.getSkinTextures();
                        cache.put(name, textures);
                        return textures;
                    }
                }

                fetchSkinAsync(name);
                return DefaultSkinHelper.getSkinTextures(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)));
            }
        }
    }

    private static void fetchSkinAsync(String name) {
        if (!fetching.contains(name)) {
            fetching.add(name);
            CompletableFuture.runAsync(() -> {
                try {
                    MinecraftClient client = MinecraftClient.getInstance();
                    URL url = URI.create("https://api.mojang.com/users/profiles/minecraft/" + name).toURL();
                    HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                    if (connection.getResponseCode() == 200) {
                        InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                        JsonObject json = GSON.fromJson(reader, JsonObject.class);
                        String id = json.get("id").getAsString();
                        UUID uuid = UUID.fromString(id.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                        ProfileResult result = client.getSessionService().fetchProfile(uuid, true);
                        if (result != null) {
                            client.getSkinProvider().fetchSkinTextures(result.profile()).thenAccept(var1x -> var1x.ifPresent(var1xx -> cache.put(name, var1xx)));
                        }
                    }
                } catch (Exception var12) {
                } finally {
                    fetching.remove(name);
                }
            });
        }
    }
}
