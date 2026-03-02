package com.example.mcpvp.screens;

import com.example.mcpvp.MatchContext;
import com.example.mcpvp.StatsManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Objects;


@Environment(EnvType.CLIENT)
public class SettingsScreen extends Screen {
    private final Screen parent;

    public SettingsScreen(Screen parent) {
        super(Text.literal("Ranked Mod Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Back"), btn -> this.client.setScreen(this.parent))
                        .dimensions(this.width / 2 - 100, this.height - 30, 200, 20).build()
        );

        String rankedText = MatchContext.rankedEnabled ? "Enabled" : "Disabled";
        Formatting rankedColor = MatchContext.rankedEnabled ? Formatting.GREEN : Formatting.RED;
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Ranked Matches: ").append(Text.literal(rankedText).formatted(rankedColor)),
                btn -> {
                    if (MatchContext.isBanned) {
                        if (Objects.requireNonNull(this.client).player != null) {
                            this.client.player.sendMessage(
                                    Text.literal("§c§lRANKED BAN: §7You cannot enable ranked matches. Reason: §f" + MatchContext.banReason), false);
                        }
                        MatchContext.rankedEnabled = false;
                        StatsManager.save();
                        this.client.setScreen(new SettingsScreen(this.parent));
                    } else if (MatchContext.inMatch) {
                        if (Objects.requireNonNull(this.client).player != null) {
                            this.client.player.sendMessage(
                                    Text.literal("§cYou cannot change ranked settings while in a match!"), false);
                        }
                        this.close();
                    } else {
                        MatchContext.rankedEnabled = !MatchContext.rankedEnabled;
                        StatsManager.save();
                        if (this.client.player != null) {
                            String status = MatchContext.rankedEnabled ? "§aENABLED" : "§cDISABLED";
                            this.client.player.sendMessage(Text.literal("§6Ranked matches are now " + status), false);
                        }
                        this.client.setScreen(new SettingsScreen(this.parent));
                    }
                }).dimensions(this.width / 2 - 100, 50, 200, 20).build()
        );

        String publicText = MatchContext.statsPublic ? "Public" : "Private";
        Formatting publicColor = MatchContext.statsPublic ? Formatting.GREEN : Formatting.RED;
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Stats Visibility: ").append(Text.literal(publicText).formatted(publicColor)),
                btn -> {
                    MatchContext.statsPublic = !MatchContext.statsPublic;
                    StatsManager.save();
                    this.client.setScreen(new SettingsScreen(this.parent));
                }).dimensions(this.width / 2 - 100, 75, 200, 20).build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 16777215);

        if (MatchContext.isBanned) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("YOU ARE CURRENTLY BANNED FROM RANKED PLAY").formatted(Formatting.RED, Formatting.BOLD),
                    this.width / 2, 40, 16777215);
        }

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("If Ranked is Disabled, matches will not affect your ELO.").formatted(Formatting.WHITE),
                this.width / 2, 105, 16777215);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("If set to Private, other players can only see your current ELO.").formatted(Formatting.WHITE),
                this.width / 2, 115, 16777215);
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}