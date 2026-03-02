package com.example.mcpvp.screens;

import com.example.mcpvp.CosmeticManager;
import com.example.mcpvp.MatchContext;
import com.example.mcpvp.SkinManager;
import com.example.mcpvp.StatsManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;


@Environment(EnvType.CLIENT)
public class PlayerStatsScreen extends Screen {
    private final Screen parent;
    private final String playerName;
    private StatsManager.PlayerStats stats;
    private boolean loading = true;
    private boolean showHistory = false;
    private double scrollAmount = 0.0;
    private static final int VISIBLE_HISTORY = 10;

    public PlayerStatsScreen(Screen parent, String playerName) {
        super(Text.literal("Stats: " + playerName));
        this.parent = parent;
        this.playerName = playerName;
    }

    @Override
    protected void init() {
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Back"), btn -> this.client.setScreen(this.parent))
                        .dimensions(10, this.height - 30, 60, 20).build()
        );
        this.addDrawableChild(ButtonWidget.builder(Text.literal(this.showHistory ? "Show ELO" : "Show History"), btn -> {
            this.showHistory = !this.showHistory;
            this.scrollAmount = 0.0;
        }).dimensions(this.width / 2 - 45, 35, 90, 20).build());

        if (this.playerName.equals(this.client.getSession().getUsername())) {
            this.addDrawableChild(
                    ButtonWidget.builder(Text.literal("Cosmetics").formatted(Formatting.GOLD),
                                    btn -> this.client.setScreen(new CosmeticScreen(this)))
                            .dimensions(this.width - 145, this.height - 30, 70, 20).build()
            );
            this.addDrawableChild(
                    ButtonWidget.builder(Text.literal("Settings"),
                                    btn -> this.client.setScreen(new SettingsScreen(this)))
                            .dimensions(this.width - 70, this.height - 30, 60, 20).build()
            );
        }

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("<"), btn -> this.scrollAmount = Math.max(0.0, this.scrollAmount - 10.0))
                        .dimensions(this.width / 2 - 70, 35, 20, 20).build()
        );
        this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), btn -> {
            if (this.stats != null && this.showHistory) {
                int maxScroll = Math.max(0, this.stats.history().size() - 10);
                this.scrollAmount = Math.min(maxScroll, this.scrollAmount + 10.0);
            }
        }).dimensions(this.width / 2 + 50, 35, 20, 20).build());

        if (this.stats == null) {
            StatsManager.fetchFullPlayerStats(this.playerName, s -> {
                this.stats = s;
                this.loading = false;
            });
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.showHistory && this.stats != null) {
            int maxScroll = Math.max(0, this.stats.history().size() - 10);
            this.scrollAmount = MathHelper.clamp(this.scrollAmount - verticalAmount, 0.0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 16777215);

        int x = this.width / 2 - 100;
        int y = 60;
        PlayerSkinDrawer.draw(context, SkinManager.getSkin(this.playerName), x - 40, y, 32);

        if (this.loading) {
            context.drawTextWithShadow(this.textRenderer, "Loading stats...", x, y, 11184810);
        } else if (this.stats != null) {
            if (this.stats.selectedTitle() != null && !this.stats.selectedTitle().isEmpty()) {
                Formatting titleColor = CosmeticManager.getTitleColor(this.stats.selectedTitle());
                context.drawCenteredTextWithShadow(this.textRenderer,
                        Text.literal("[" + this.stats.selectedTitle() + "]").formatted(titleColor),
                        x - 24, y + 35, 16777215);
            }

            if (!this.stats.isPublic() && !this.playerName.equals(this.client.getSession().getUsername())) {
                context.drawTextWithShadow(this.textRenderer,
                        Text.literal("This player has set their stats to private.").formatted(Formatting.RED),
                        x, y, 16777215);
            } else if (this.showHistory) {
                int renderY = y + 10;
                for (int i = (int)this.scrollAmount; i < Math.min((int)this.scrollAmount + 10, this.stats.history().size()); i++) {
                    StatsManager.MatchHistoryEntry entry = this.stats.history().get(i);
                    Text line = Text.literal("[" + (entry.won() ? "W" : "L") + "] ")
                            .formatted(entry.won() ? Formatting.GREEN : Formatting.RED)
                            .append(Text.literal(entry.kit() + " vs " + entry.opponent()).formatted(Formatting.WHITE));
                    context.drawTextWithShadow(this.textRenderer, line, x, renderY, 16777215);
                    renderY += 12;
                }
            } else {
                for (MatchContext.KitType kit : MatchContext.KitType.values()) {
                    int elo = (Integer)this.stats.kitElo().getOrDefault(kit, 0);
                    int total = (Integer)this.stats.totalMatches().getOrDefault(kit, 0);
                    int wins = (Integer)this.stats.totalWins().getOrDefault(kit, 0);
                    String winRate = total > 0 ? String.format("%.1f%%", wins * 100.0F / total) : "0%";

                    context.drawTexture(RenderLayer::getGuiTextured, kit.customTexture, x, y, 0.0F, 0.0F, 16, 16, 16, 16);
                    Text eloText = Text.literal(kit.apiName + ": ").formatted(Formatting.WHITE)
                            .append(Text.literal(String.valueOf(elo)).formatted(MatchContext.getEloColor(elo, 101)));
                    context.drawTextWithShadow(this.textRenderer, eloText, x + 20, y, 16777215);

                    Text winRateText = Text.literal("WR: ").formatted(Formatting.DARK_GRAY)
                            .append(Text.literal(winRate).formatted(Formatting.YELLOW))
                            .append(Text.literal(" (" + wins + "/" + total + ")").formatted(Formatting.WHITE));
                    context.drawTextWithShadow(this.textRenderer, winRateText, x + 20, y + 9, 16777215);
                    context.drawTexture(RenderLayer::getGuiTextured, MatchContext.getRankIcon(elo, 101), x + 160, y + 2, 0.0F, 0.0F, 16, 16, 16, 16);
                    y += 24;
                }
            }
        }
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}