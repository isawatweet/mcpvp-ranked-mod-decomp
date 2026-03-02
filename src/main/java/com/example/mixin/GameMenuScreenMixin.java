package com.example.mixin;

import com.example.mcpvp.MatchContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        int x = 10;
        int y = 10;

        MatchContext.KitType kit = MatchContext.currentKit != null
                ? MatchContext.currentKit
                : (MatchContext.lastMatchKit != null ? MatchContext.lastMatchKit : MatchContext.KitType.SWORD);

        int elo = MatchContext.kitElo.getOrDefault(kit, 0);
        String rank = MatchContext.getRankDisplay(elo, 101);
        Formatting color = MatchContext.getEloColor(elo, 101);

        context.drawTextWithShadow(client.textRenderer,
                Text.literal("Current/Last Kit: ").formatted(Formatting.GRAY)
                        .append(Text.literal(kit.apiName).formatted(Formatting.WHITE)),
                x, y, 16777215);

        context.drawTextWithShadow(client.textRenderer,
                Text.literal("ELO: ").formatted(Formatting.WHITE)
                        .append(Text.literal(String.valueOf(elo)).formatted(color)),
                x, y + 10, 16777215);

        context.drawTextWithShadow(client.textRenderer,
                Text.literal("Rank: ").formatted(Formatting.WHITE)
                        .append(Text.literal(rank).formatted(color)),
                x, y + 20, 16777215);
    }
}
