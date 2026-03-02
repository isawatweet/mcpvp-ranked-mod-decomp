package com.example.mcpvp.screens;

import com.example.mcpvp.MatchContext;
import com.example.mcpvp.SkinManager;
import com.example.mcpvp.StatsManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class LeaderboardScreen extends Screen {
    private final Screen parent;
    private List<StatsManager.LeaderboardEntry> allEntries = new ArrayList<>();
    private List<StatsManager.LeaderboardEntry> filteredEntries = new ArrayList<>();
    private boolean loading = true;
    private MatchContext.KitType currentKit = MatchContext.KitType.SWORD;
    private TextFieldWidget searchBox;
    private double scrollAmount = 0.0;
    private static final int ENTRY_HEIGHT = 20;
    private static final int VISIBLE_ENTRIES = 8;

    public LeaderboardScreen(Screen parent) {
        super(Text.literal("MCPVP Leaderboard"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.searchBox = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 35, 200, 20, Text.literal("Search..."));
        this.searchBox.setPlaceholder(Text.literal("Search players..."));
        this.searchBox.setChangedListener(this::onSearchChanged);
        this.addDrawableChild(this.searchBox);

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Back"), btn -> this.client.setScreen(this.parent))
                        .dimensions(this.width - 70, this.height - 30, 60, 20).build()
        );
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), btn -> {
            StatsManager.pushToGlobal();
            this.refresh();
        }).dimensions(this.width / 2 - 45, this.height - 30, 90, 20).build());

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("<"), btn -> this.scrollAmount = Math.max(0.0, this.scrollAmount - 8.0))
                        .dimensions(this.width / 2 - 70, this.height - 30, 20, 20).build()
        );
        this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), btn -> {
            int maxScroll = Math.max(0, this.filteredEntries.size() - 8);
            this.scrollAmount = Math.min(maxScroll, this.scrollAmount + 8.0);
        }).dimensions(this.width / 2 + 50, this.height - 30, 20, 20).build());

        int kitX = 10;
        int kitY = 40;
        for (MatchContext.KitType kit : MatchContext.KitType.values()) {
            final MatchContext.KitType kitFinal = kit;
            this.addDrawableChild(ButtonWidget.builder(Text.literal(kit.apiName), btn -> {
                this.currentKit = kitFinal;
                this.refresh();
            }).dimensions(kitX + 25, kitY, 80, 20).build());
            kitY += 22;
        }

        this.refresh();
    }

    private void onSearchChanged(String query) {
        this.filteredEntries = query.isEmpty()
                ? new ArrayList<>(this.allEntries)
                : this.allEntries.stream()
                .filter(e -> e.name().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
        this.scrollAmount = 0.0;
    }

    private void refresh() {
        this.loading = true;
        StatsManager.fetchLeaderboard(this.currentKit, entries -> {
            this.allEntries = entries;
            this.onSearchChanged(this.searchBox.getText());
            this.loading = false;
        });
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, this.filteredEntries.size() - 8);
        this.scrollAmount = MathHelper.clamp(this.scrollAmount - verticalAmount, 0.0, maxScroll);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.searchBox.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.searchBox);
            return true;
        }

        int x = this.width / 2 - 120;
        int startY = 60;
        if (!this.loading && !this.filteredEntries.isEmpty()) {
            int startIndex = (int)this.scrollAmount;
            int endIndex = Math.min(startIndex + 8, this.filteredEntries.size());
            for (int i = startIndex; i < endIndex; i++) {
                int renderY = startY + (i - startIndex) * 20;
                if (mouseX >= x && mouseX <= x + 200 && mouseY >= renderY && mouseY <= renderY + 20) {
                    this.client.setScreen(new PlayerStatsScreen(this, this.filteredEntries.get(i).name()));
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 16777215);

        int kitX = 10;
        int kitY = 40;
        for (MatchContext.KitType kit : MatchContext.KitType.values()) {
            context.drawTexture(RenderLayer::getGuiTextured, kit.customTexture, kitX + 4, kitY + 2, 0.0F, 0.0F, 16, 16, 16, 16);
            kitY += 22;
        }

        int x = this.width / 2 - 120;
        int startY = 60;

        if (this.loading) {
            context.drawCenteredTextWithShadow(this.textRenderer, "Loading...", this.width / 2, this.height / 2, 11184810);
        } else if (this.filteredEntries.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, "No players found.", this.width / 2, this.height / 2, 11184810);
        } else {
            int startIndex = (int)this.scrollAmount;
            int endIndex = Math.min(startIndex + 8, this.filteredEntries.size());
            for (int i = startIndex; i < endIndex; i++) {
                StatsManager.LeaderboardEntry entry = this.filteredEntries.get(i);
                int rankPos = this.allEntries.indexOf(entry) + 1;
                int renderY = startY + (i - startIndex) * 20;

                PlayerSkinDrawer.draw(context, SkinManager.getSkin(entry.name()), x, renderY, 16);
                Formatting color = MatchContext.getEloColor(entry.elo(), rankPos);
                MutableText line = Text.literal(rankPos + ". ").formatted(Formatting.WHITE);
                line.append(Text.literal(entry.name()).formatted(color))
                        .append(Text.literal(" - " + entry.elo()).formatted(Formatting.WHITE));
                context.drawTextWithShadow(this.textRenderer, line, x + 20, renderY + 4, 16777215);
                context.drawTexture(RenderLayer::getGuiTextured, MatchContext.getRankIcon(entry.elo(), rankPos), x + 180, renderY + 2, 0.0F, 0.0F, 16, 16, 16, 16);
            }
        }
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}