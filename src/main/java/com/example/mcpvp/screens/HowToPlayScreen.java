package com.example.mcpvp.screens;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Environment(EnvType.CLIENT)
public class HowToPlayScreen extends Screen {
    private final Screen parent;

    public HowToPlayScreen(Screen parent) {
        super(Text.literal("How to Play - MCPVP Ranked"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Back"), btn -> this.client.setScreen(this.parent))
                        .dimensions(this.width / 2 - 100, this.height - 30, 200, 20)
                        .build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title.copy().formatted(Formatting.GOLD, Formatting.BOLD),
                this.width / 2, 15, 16777215
        );

        int x = this.width / 2 - 150;
        int y = 40;
        y += this.drawSection(context, "1. Getting Started",
                "Join mcpvp.club and enter any duel queue. The mod will automatically detect your kit and start tracking your ELO.", x, y);
        y += this.drawSection(context, "2. ELO & Ranks",
                "Win matches to gain ELO and climb ranks from Leather to Dragon. Your first 10 matches in each kit are placements.", x, y);
        y += this.drawSection(context, "3. Bonuses",
                "Gain extra ELO for win streaks, flawless victories (high health), or defeating higher-ranked opponents.", x, y);
        y += this.drawSection(context, "4. Commands & Keys",
                "Press 'J' in-game to see your progress. Use /ranked status to see your current session stats.", x, y);
        this.drawSection(context, "5. Fair Play",
                "Leaving a ranked match early counts as an automatic loss. Play fair and have fun!", x, y);
    }

    private int drawSection(DrawContext context, String title, String description, int x, int y) {
        context.drawTextWithShadow(
                this.textRenderer,
                Text.literal(title).formatted(Formatting.YELLOW, Formatting.BOLD),
                x, y, 16777215
        );
        List<OrderedText> lines = this.textRenderer.wrapLines(Text.literal(description).formatted(Formatting.WHITE), 300);
        int lineY = y + 11;
        for (OrderedText line : lines) {
            context.drawText(this.textRenderer, line, x, lineY, 16777215, false);
            lineY += 9;
        }
        return 11 + lines.size() * 9 + 6;
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}