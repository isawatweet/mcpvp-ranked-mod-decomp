package com.example.mcpvp.screens;

import com.example.mcpvp.MatchContext;
import com.example.mcpvp.StatsManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class EloScreen extends Screen {
    private static final Identifier EXPERIENCE_BAR_BACKGROUND_TEXTURE = Identifier.ofVanilla("hud/experience_bar_background");
    private MatchContext.KitType selectedKit;
    private int leaderboardPos = -1;

    public EloScreen() {
        super(Text.literal("ELO Status"));
        this.selectedKit = MatchContext.currentKit != null
                ? MatchContext.currentKit
                : (MatchContext.lastMatchKit != null ? MatchContext.lastMatchKit : MatchContext.KitType.SWORD);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), btn -> this.cycleKit(-1))
                .dimensions(centerX - 110, centerY - 65, 20, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), btn -> this.cycleKit(1))
                .dimensions(centerX + 90, centerY - 65, 20, 20).build());

        Text rankedText = MatchContext.rankedEnabled
                ? Text.literal("Ranked: §aENABLED")
                : Text.literal("Ranked: §cDISABLED");
        this.addDrawableChild(ButtonWidget.builder(rankedText, btn -> {
            MatchContext.rankedEnabled = !MatchContext.rankedEnabled;
            btn.setMessage(MatchContext.rankedEnabled
                    ? Text.literal("Ranked: §aENABLED")
                    : Text.literal("Ranked: §cDISABLED"));
            StatsManager.save();
        }).dimensions(centerX - 50, centerY + 65, 100, 20).build());

        this.fetchLeaderboardPos();
    }

    private void fetchLeaderboardPos() {
        this.leaderboardPos = -1;
        String myName = this.client.getSession().getUsername();
        StatsManager.fetchLeaderboard(this.selectedKit, entries -> {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).name().equalsIgnoreCase(myName)) {
                    this.leaderboardPos = i + 1;
                    break;
                }
            }
        });
    }

    private void cycleKit(int direction) {
        MatchContext.KitType[] kits = MatchContext.KitType.values();
        int currentIndex = 0;
        for (int i = 0; i < kits.length; i++) {
            if (kits[i] == this.selectedKit) {
                currentIndex = i;
                break;
            }
        }
        this.selectedKit = kits[(currentIndex + direction + kits.length) % kits.length];
        this.fetchLeaderboardPos();
    }

    private int getHexColor(Formatting formatting) {
        Integer color = formatting.getColorValue();
        return color != null ? color | 0xFF000000 : -1;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        context.drawCenteredTextWithShadow(this.textRenderer, "§6§lYOUR RANKED PROGRESS", centerX, centerY - 85, 16777215);
        int kitNameWidth = this.textRenderer.getWidth(this.selectedKit.apiName);
        context.drawTexture(RenderLayer::getGuiTextured, this.selectedKit.customTexture, centerX - kitNameWidth / 2 - 20, centerY - 63, 0.0F, 0.0F, 16, 16, 16, 16);
        context.drawCenteredTextWithShadow(this.textRenderer, "§f§l" + this.selectedKit.apiName, centerX, centerY - 60, 16777215);

        int elo = (Integer)MatchContext.kitElo.getOrDefault(this.selectedKit, 0);
        int placements = (Integer)MatchContext.placementMatches.getOrDefault(this.selectedKit, 0);
        String rankName = MatchContext.getRankDisplay(elo, 101);
        Formatting rankColor = MatchContext.getEloColor(elo, 101);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Rank: " + rankName).formatted(rankColor), centerX, centerY - 50, 16777215);
        context.drawTexture(RenderLayer::getGuiTextured, MatchContext.getRankIcon(elo, 101), centerX + 55, centerY - 55, 0.0F, 0.0F, 16, 16, 16, 16);

        String posText = this.leaderboardPos > 0
                ? "§7Leaderboard: §e#" + this.leaderboardPos
                : "§7Leaderboard: §8N/A";
        context.drawCenteredTextWithShadow(this.textRenderer, posText, centerX, centerY - 40, 16777215);

        int barWidth = 182;
        int barHeight = 5;
        int barX = centerX - barWidth / 2;
        int barY = centerY - 20;
        context.drawGuiTexture(RenderLayer::getGuiTextured, EXPERIENCE_BAR_BACKGROUND_TEXTURE, barX, barY, barWidth, barHeight);

        if (elo <= 0 && placements < 10) {
            context.drawCenteredTextWithShadow(this.textRenderer, "§ePlacements: §f" + placements + "/10", centerX, barY - 10, 16777215);
            float progress = Math.min(1.0F, placements / 10.0F);
            int progressWidth = (int)(progress * barWidth);
            context.fill(barX, barY, barX + progressWidth, barY + barHeight, -171);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("ELO: " + elo).formatted(rankColor), centerX, barY - 10, 16777215);
            float progress = Math.min(1.0F, elo / 4000.0F);
            int progressWidth = (int)(progress * barWidth);
            context.fill(barX, barY, barX + progressWidth, barY + barHeight, this.getHexColor(rankColor));
        }

        int xp = (Integer)MatchContext.kitMasteryXP.getOrDefault(this.selectedKit, 0);
        int level = MatchContext.getMasteryLevel(xp);
        int currentLevelXP = MatchContext.getXPForLevel(level);
        int nextLevelXP = MatchContext.getXPForLevel(level + 1);
        int xpInLevel = xp - currentLevelXP;
        int xpNeeded = nextLevelXP - currentLevelXP;
        int masteryBarY = centerY + 20;

        context.drawCenteredTextWithShadow(this.textRenderer, "§d§lKIT MASTERY", centerX, masteryBarY - 15, 16777215);
        context.drawCenteredTextWithShadow(this.textRenderer, "§7Level: §e" + level, centerX, masteryBarY - 5, 16777215);
        context.drawTexture(RenderLayer::getGuiTextured, MatchContext.getMasteryIconPath(this.selectedKit, level), centerX - 70, masteryBarY - 10, 0.0F, 0.0F, 16, 16, 16, 16);
        context.drawGuiTexture(RenderLayer::getGuiTextured, EXPERIENCE_BAR_BACKGROUND_TEXTURE, barX, masteryBarY + 5, barWidth, barHeight);

        float masteryProgress = Math.min(1.0F, (float)xpInLevel / xpNeeded);
        int masteryProgressWidth = (int)(masteryProgress * barWidth);
        context.fill(barX, masteryBarY + 5, barX + masteryProgressWidth, masteryBarY + 5 + barHeight, -43521);
        context.drawCenteredTextWithShadow(this.textRenderer, "§dXP: " + xp, centerX, masteryBarY + 12, 16777215);

        if (this.client != null && this.client.player != null) {
            InventoryScreen.drawEntity(context, centerX - 140, centerY - 40, centerX - 100, centerY + 40, 30, 0.0625F, mouseX, mouseY, this.client.player);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 74) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}