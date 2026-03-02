package com.example;


import com.example.mcpvp.*;

import java.util.Map.Entry;

import com.example.mcpvp.screens.EloScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
@Environment(EnvType.CLIENT)
public class MCPVPClient implements ClientModInitializer {
    private static KeyBinding eloKeyBinding;

    public MCPVPClient() {
        super();
    }

    public void onInitializeClient() {
        StatsManager.init();
        KitIdentifier.init();
        CosmeticManager.init();
        //VersionManager.init();
        ServerPreloader.preload();
        StatsManager.syncWithServer();
        ModelLoadingPlugin.register(new ModelLoaderPlugin());
        eloKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.mcpvp.elo_status", Type.KEYSYM, 74, "category.mcpvp.ranked"));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (eloKeyBinding.wasPressed()) {
                if (mc.currentScreen == null) {
                    mc.setScreen(new EloScreen());
                }
            }
        });
        ClientSendMessageEvents.COMMAND
                .register(
                        command -> {
                            String baseCommand = command.split(" ")[0].toLowerCase();
                            if ((baseCommand.equals("leave") || baseCommand.equals("resign")) && MatchContext.inMatch && MatchContext.isRanked) {
                                MatchContext.KitType kit = MatchContext.currentKit != null ? MatchContext.currentKit : MatchContext.currentQueuedKit;
                                if (kit != null) {
                                    MatchContext.updateElo(kit, false);
                                    MinecraftClient client = MinecraftClient.getInstance();
                                    if (client.player != null) {
                                        client.player
                                                .sendMessage(
                                                        Text.literal("\u00a7c\u00a7l[RankedMod] \u00a7fMatch resigned via /" + baseCommand + ". Loss recorded.")
                                                                .formatted(Formatting.RED),
                                                        false
                                                );
                                    }
                                }

                                MatchContext.reset(true);
                            }
                        }
                );
        ClientCommandRegistrationCallback.EVENT
                .register(
                        (dispatcher, commandRegistryAccess) -> dispatcher.register(
                                ClientCommandManager.literal("ranked")
                                        .then(
                                                ClientCommandManager.literal("status")
                                                        .executes(
                                                                var0x -> {
                                                                    MinecraftClient client = MinecraftClient.getInstance();
                                                                    String kitName = MatchContext.currentKit != null ? MatchContext.currentKit.name() : "None";
                                                                    String score = !MatchContext.lastScore.isEmpty() ? MatchContext.lastScore : "0 - 0";
                                                                    ServerInfo serverInfo = client.getCurrentServerEntry();
                                                                    String serverStatus = serverInfo != null && serverInfo.address.toLowerCase().contains("mcpvp.club")
                                                                            ? "Verified (mcpvp.club)"
                                                                            : "Not Connected to mcpvp.club";
                                                                    var0x.getSource()
                                                                            .sendFeedback(Text.literal("--- Ranked Mod Status ---").formatted(Formatting.GOLD));
                                                                    var0x.getSource()
                                                                            .sendFeedback(
                                                                                    Text.literal("Current Detected Kit: ")
                                                                                            .formatted(Formatting.YELLOW)
                                                                                            .append(Text.literal(kitName).formatted(Formatting.WHITE))
                                                                            );
                                                                    var0x.getSource()
                                                                            .sendFeedback(
                                                                                    Text.literal("Current Session Score: ")
                                                                                            .formatted(Formatting.YELLOW)
                                                                                            .append(Text.literal(score).formatted(Formatting.WHITE))
                                                                            );
                                                                    var0x.getSource()
                                                                            .sendFeedback(
                                                                                    Text.literal("Server Connection: ")
                                                                                            .formatted(Formatting.YELLOW)
                                                                                            .append(Text.literal(serverStatus).formatted(Formatting.WHITE))
                                                                            );
                                                                    var0x.getSource()
                                                                            .sendFeedback(Text.literal("--- Your ELO Stats ---").formatted(Formatting.GOLD));

                                                                    for (Entry entry : MatchContext.kitElo.entrySet()) {
                                                                        var0x.getSource()
                                                                                .sendFeedback(
                                                                                        Text.literal(entry.getKey().toString() + ": ")
                                                                                                .formatted(Formatting.YELLOW)
                                                                                                .append(Text.literal(entry.getValue().toString()).formatted(Formatting.WHITE))
                                                                                                .append(Text.literal(" (" + MatchContext.getRankDisplay((Integer) entry.getValue(), 101) + Formatting.WHITE + ")"))
                                                                                );
                                                                    }

                                                                    return 1;
                                                                }
                                                        )
                                        )
                        )
                );
    }

    public static void debugLog(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player
                    .sendMessage(
                            Text.literal("[RankedMod-Debug] ").formatted(Formatting.GOLD).append(Text.literal(message).formatted(Formatting.WHITE)), false
                    );
        }
    }
}
