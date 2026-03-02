package com.example.mixin;


import com.example.mcpvp.MatchContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ChatHud.class)
public class ChatMixin {
    public ChatMixin() {
        super();
    }

    @Inject(method = "addMessage", at = @At("HEAD"))
    private void onAddMessage(Text message, MessageSignatureData var2, MessageIndicator var3, CallbackInfo var4) {
        if (MatchContext.isAllowedServer()) {
            String content = message.getString();
            String playerName = MinecraftClient.getInstance().getSession().getUsername();
            if ((content.contains("Welcome to mcpvp.club") || content.contains("Welcome back to mcpvp.club"))
                    && MatchContext.pendingAbandonmentPenalty
                    && MatchContext.abandonedKit != null) {
                MatchContext.updateElo(MatchContext.abandonedKit, false);
                MinecraftClient.getInstance()
                        .player
                        .sendMessage(
                                Text.literal("\u00a7c\u00a7l[RankedMod] \u00a7fYou left your last match early! -15 ELO penalty applied.").formatted(Formatting.RED),
                                false
                        );
                MatchContext.pendingAbandonmentPenalty = false;
                MatchContext.abandonedKit = null;
                StatsManager.save();
            }

            if (content.contains("Duel started") || content.contains("Match started")) {
                MatchContext.isMatchActive = true;
            }

            if (content.contains("was slain by") || content.contains("was shot by") || content.contains("was blown up by")) {
                String[] parts = content.split(" ");
                if (parts.length >= 4) {
                    String victim = parts[0];
                    String killer = parts[parts.length - 1];
                    if (killer.equals(playerName)) {
                        MatchContext.opponentName = victim;
                    } else if (victim.equals(playerName)) {
                        MatchContext.opponentName = killer;
                    }
                }
            }

            if (content.contains("won the match!") || content.contains("Winner:") || content.contains("Match Completed")) {
                boolean won = content.contains(playerName);
                if (MatchContext.inMatch && MatchContext.isRanked) {
                    MatchContext.updateElo(MatchContext.currentKit != null ? MatchContext.currentKit : MatchContext.currentQueuedKit, won);
                    MatchContext.reset(true);
                }

                MatchContext.isMatchActive = false;
            }

            if (content.contains("resigned")) {
                if (MatchContext.inMatch && MatchContext.isRanked) {
                    boolean iResigned = content.contains(playerName);
                    if (!iResigned) {
                        MatchContext.updateElo(MatchContext.currentKit != null ? MatchContext.currentKit : MatchContext.currentQueuedKit, true);
                    } else {
                        MatchContext.updateElo(MatchContext.currentKit != null ? MatchContext.currentKit : MatchContext.currentQueuedKit, false);
                    }

                    MatchContext.reset(true);
                }

                MatchContext.isMatchActive = false;
            }

            if (content.contains("HP") && content.contains("/")) {
                try {
                    String[] parts = content.split(" ");

                    for (String part : parts) {
                        if (part.contains("/")) {
                            String healthStr = part.split("/")[0];
                            MatchContext.lastRoundHealth = Float.parseFloat(healthStr);
                            break;
                        }
                    }
                } catch (Exception e) {
                }
            }
        }
    }
}
