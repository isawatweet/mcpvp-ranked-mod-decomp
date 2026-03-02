package com.example.mcpvp;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Environment(EnvType.CLIENT)
public class MenuRenderer {
    public static void renderMatchInfo(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            int x = 10;
            int y = 10;
            MatchContext.KitType kit = MatchContext.currentKit != null
                    ? MatchContext.currentKit
                    : (MatchContext.lastMatchKit != null ? MatchContext.lastMatchKit : MatchContext.KitType.SWORD);
            int elo = (Integer)MatchContext.kitElo.getOrDefault(kit, 0);
            Formatting color = MatchContext.getEloColor(elo, 101);

            if (kit.customTexture != null) {
                context.drawTexture(RenderLayer::getGuiTextured, kit.customTexture, x, y, 0.0F, 0.0F, 16, 16, 16, 16);
            } else {
                context.drawItem(new ItemStack(kit.icon), x, y);
            }

            context.drawTextWithShadow(client.textRenderer,
                    Text.literal(kit.apiName).formatted(Formatting.WHITE),
                    x + 20, y + 4, 16777215);
            y += 20;

            context.drawTextWithShadow(client.textRenderer,
                    Text.literal("ELO: ").formatted(Formatting.WHITE)
                            .append(Text.literal(String.valueOf(elo)).formatted(color)),
                    x, y, 16777215);
            y += 10;

            String rank = MatchContext.getRankDisplay(elo, 101);
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal("Rank: ").formatted(Formatting.WHITE).append(Text.literal(rank)),
                    x, y, 16777215);
        }
    }
}