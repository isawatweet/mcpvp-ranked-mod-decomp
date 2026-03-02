package com.example.mixin;

import com.example.mcpvp.MatchContext;
import com.example.mcpvp.StatsManager;
import com.example.mcpvp.screens.HowToPlayScreen;
import com.example.mcpvp.screens.LeaderboardScreen;
import com.example.mcpvp.screens.PlayerStatsScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    private static final Identifier TROPHY_ICON = Identifier.of("mcpvp", "textures/item/trophy.png");
    private static final Identifier STATS_ICON = Identifier.of("minecraft", "textures/item/writable_book.png");
    private static final Identifier HELP_ICON = Identifier.of("minecraft", "textures/item/knowledge_book.png");

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void onInitHead(CallbackInfo ci) {
//        VersionManager.checkVersion();
        StatsManager.syncWithServer();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        int x = 10;
        int y = 50;
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Leaderboard"), btn -> this.client.setScreen(new LeaderboardScreen(this)))
                        .dimensions(x + 20, y, 80, 20)
                        .build()
        );
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("My Stats"), btn -> this.client.setScreen(new PlayerStatsScreen(this, this.client.getSession().getUsername())))
                        .dimensions(x + 20, y + 22, 80, 20)
                        .build()
        );
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("How to Play"), btn -> this.client.setScreen(new HowToPlayScreen(this)))
                        .dimensions(x + 20, y + 44, 80, 20)
                        .build()
        );
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        int x = 10;
        int y = 10;
        MatchContext.KitType kit = MatchContext.lastMatchKit != null ? MatchContext.lastMatchKit : MatchContext.KitType.SWORD;
        int elo = (Integer) MatchContext.kitElo.getOrDefault(kit, 0);
        String rank = MatchContext.getRankDisplay(elo, 101).replace("§", "");
        Formatting color = MatchContext.getEloColor(elo, 101);

        context.drawTextWithShadow(client.textRenderer,
                Text.literal("Last Kit: ").formatted(Formatting.WHITE)
                        .append(Text.literal(kit.apiName).formatted(Formatting.AQUA)),
                x, y, 16777215);
        context.drawTextWithShadow(client.textRenderer,
                Text.literal("ELO: ").formatted(Formatting.WHITE)
                        .append(Text.literal(String.valueOf(elo)).formatted(color)),
                x, y + 10, 16777215);
        context.drawTextWithShadow(client.textRenderer,
                Text.literal("Rank: ").formatted(Formatting.WHITE)
                        .append(Text.literal(rank).formatted(color)),
                x, y + 20, 16777215);

        context.drawTexture(RenderLayer::getGuiTextured, MatchContext.getRankIcon(elo, 101), x + 80, y + 15, 0.0F, 0.0F, 16, 16, 16, 16);
        context.drawTexture(RenderLayer::getGuiTextured, TROPHY_ICON, x + 2, 52, 0.0F, 0.0F, 16, 16, 16, 16);
        context.drawTexture(RenderLayer::getGuiTextured, STATS_ICON, x + 2, 74, 0.0F, 0.0F, 16, 16, 16, 16);
        context.drawTexture(RenderLayer::getGuiTextured, HELP_ICON, x + 2, 96, 0.0F, 0.0F, 16, 16, 16, 16);
    }
}