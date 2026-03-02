package com.example.mixin;

import com.example.mcpvp.EloBarRenderer;
import com.example.mcpvp.MatchContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.*;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (this.client.world != null) {
            EloBarRenderer.render(context);
            MatchContext.KitType displayKit = MatchContext.currentKit != null ? MatchContext.currentKit : MatchContext.currentQueuedKit;
            if (displayKit != null) {
                int x = 5;
                int y = 5;
                int elo = (Integer)MatchContext.kitElo.getOrDefault(displayKit, 0);
                int placements = (Integer)MatchContext.placementMatches.getOrDefault(displayKit, 0);
                Formatting color = MatchContext.getEloColor(elo, 101);

                context.drawTexture(RenderLayer::getGuiTextured, displayKit.customTexture, x, y, 0.0F, 0.0F, 16, 16, 16, 16);
                Text kitNameText = Text.literal(displayKit.apiName).formatted(Formatting.AQUA, Formatting.BOLD);
                context.drawTextWithShadow(this.client.textRenderer, kitNameText, x + 20, y + 4, 16777215);
                int kitWidth = this.client.textRenderer.getWidth(kitNameText);
                context.drawTextWithShadow(this.client.textRenderer, " | ", x + 20 + kitWidth, y + 4, 11184810);
                int sepWidth = this.client.textRenderer.getWidth(" | ");

                if (elo <= 0 && placements < 10) {
                    Text placementText = Text.literal("Placements: ").formatted(Formatting.WHITE)
                            .append(Text.literal(placements + "/10").formatted(Formatting.YELLOW, Formatting.BOLD));
                    context.drawTextWithShadow(this.client.textRenderer, placementText, x + 20 + kitWidth + sepWidth, y + 4, 16777215);
                } else {
                    context.drawTexture(RenderLayer::getGuiTextured, MatchContext.getRankIcon(elo, 101), x + 20 + kitWidth + sepWidth, y, 0.0F, 0.0F, 16, 16, 16, 16);
                    Text eloText = Text.literal(String.valueOf(elo)).formatted(color, Formatting.BOLD);
                    context.drawTextWithShadow(this.client.textRenderer, eloText, x + 20 + kitWidth + sepWidth + 20, y + 4, 16777215);
                }
            }

            ServerInfo serverInfo = this.client.getCurrentServerEntry();
            boolean isMCPVP = serverInfo != null && serverInfo.address.toLowerCase().contains("mcpvp.club");
            boolean isSinglePlayer = this.client.isInSingleplayer();

            if (serverInfo != null && !isMCPVP && !isSinglePlayer) {
                if (MatchContext.inMatch) {
                    MatchContext.reset(true);
                }
            } else {
                Scoreboard scoreboard = this.client.world.getScoreboard();
                ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
                boolean foundScore = false;
                String currentScoreText = "";

                if (objective != null) {
                    String title = objective.getDisplayName().getString();
                    if (title.contains("Score:")) {
                        foundScore = true;
                        currentScoreText = title.split("Score:")[1].trim();
                    } else {
                        for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(objective)) {
                            String owner = entry.owner();
                            Team team = scoreboard.getScoreHolderTeam(owner);
                            String entryText = team != null
                                    ? team.getPrefix().getString() + owner + team.getSuffix().getString()
                                    : owner;
                            if (entryText.contains("Score:")) {
                                foundScore = true;
                                currentScoreText = entryText.split("Score:")[1].trim();
                                break;
                            }
                            if (!owner.equals(this.client.getSession().getUsername()) && !owner.startsWith("#") && !owner.contains("Score:")) {
                                MatchContext.opponentName = Formatting.strip(owner);
                            }
                        }
                    }
                }

                if (foundScore && MatchContext.inMatch && !currentScoreText.equals(MatchContext.lastScore)) {
                    MatchContext.lastScore = currentScoreText;
                }

                if (this.client.player != null) {
                    boolean hasIronSword = this.hasItem(Items.IRON_SWORD);
                    boolean hasLectern = this.hasItem(Items.LECTERN);
                    boolean hasBarrier = this.hasItem(Items.BARRIER);

                    if (!hasIronSword && !hasLectern && !hasBarrier) {
                        if (MatchContext.kitSelectedInMenu && !MatchContext.inMatch) {
                            MatchContext.inMatch = true;
                            MatchContext.currentKit = MatchContext.currentQueuedKit;
                            MatchContext.isRanked = MatchContext.rankedEnabled && !MatchContext.wasInPartyBool && !MatchContext.duelRequestSent;
                            if (MatchContext.isRanked) {
                                StatsManager.pushMatchStart();
                                if (!MatchContext.opponentName.equals("Unknown")) {
                                    StatsManager.fetchPlayerElo(MatchContext.opponentName);
                                }
                            }
                            this.client.player.sendMessage(
                                    Text.literal("§6§l[RankedMod] §fMatch detected! Kit: §a" + MatchContext.currentKit.apiName), false);
                        }
                    } else if (MatchContext.inMatch || MatchContext.currentKit != null) {
                        MatchContext.reset(true);
                    }
                }
            }
        }
    }

    private boolean hasItem(Item item) {
        if (this.client.player == null) return false;
        for (int i = 0; i < this.client.player.getInventory().size(); i++) {
            if (this.client.player.getInventory().getStack(i).isOf(item)) {
                return true;
            }
        }
        return false;
    }
}