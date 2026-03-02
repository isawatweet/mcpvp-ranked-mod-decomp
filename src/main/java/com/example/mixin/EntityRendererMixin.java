package com.example.mixin;

import com.example.mcpvp.CosmeticManager;
import com.example.mcpvp.MatchContext;
import com.example.mcpvp.StatsManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Environment(EnvType.CLIENT)
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

    @ModifyArgs(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/EntityRenderer;renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
            )
    )
    private void onRenderLabel(Args args) {
        EntityRenderState state = args.get(0);
        Text text = args.get(1);
        if (state instanceof PlayerEntityRenderState playerState) {
            String name = playerState.name;
            if (name == null) return;

            String localName = MinecraftClient.getInstance().getSession().getUsername();
            String title = "";
            int elo;
            if (name.equals(localName)) {
                elo = MatchContext.currentKit != null
                        ? (Integer)MatchContext.kitElo.getOrDefault(MatchContext.currentKit, 0)
                        : (Integer)MatchContext.kitElo.values().stream().max(Integer::compare).orElse(0);
                title = CosmeticManager.selectedTitle;
            } else {
                StatsManager.fetchPlayerElo(name);
                elo = (Integer)MatchContext.playerEloByName.getOrDefault(name, 0);
            }

            MutableText newText = Text.literal("");
            if (title != null && !title.isEmpty()) {
                newText.append(Text.literal("[" + title + "] ").formatted(Formatting.GOLD));
            }
            newText.append(Text.literal("[")
                    .append(Text.literal(String.valueOf(elo)).formatted(MatchContext.getEloColor(elo, 101)))
                    .append("] ")
                    .append(text));
            args.set(1, newText);
        }
    }
}