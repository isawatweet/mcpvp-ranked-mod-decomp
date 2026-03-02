package com.example.mixin;

import com.example.mcpvp.CosmeticManager;
import com.example.mcpvp.MatchContext;
import com.example.mcpvp.StatsManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Environment(EnvType.CLIENT)
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
    private void onRenderItem(
            LivingEntity entity,
            ItemStack stack,
            ModelTransformationMode renderMode,
            boolean leftHanded,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            World world,
            int light,
            int overlay,
            int seed,
            CallbackInfo ci
    ) {
        if (!stack.isEmpty()) {
            String kitName = null;
            if (stack.isOf(Items.NETHERITE_SWORD)) {
                kitName = "Netherite OP";
            } else if (stack.isOf(Items.IRON_SWORD)) {
                kitName = MatchContext.currentKit != null ? MatchContext.currentKit.apiName : "Sword";
            }

            if (kitName != null) {
                String playerName = null;
                if (entity != null) {
                    playerName = entity.getName().getString();
                } else if (MinecraftClient.getInstance().player != null) {
                    playerName = MinecraftClient.getInstance().player.getName().getString();
                }

                if (playerName != null) {
                    String cosmetic = "default";
                    if (playerName.equals(MinecraftClient.getInstance().getSession().getUsername())) {
                        cosmetic = CosmeticManager.getSelectedCosmetic(kitName);
                    } else {
                        Map<String, String> otherPlayerCosmetics = (Map<String, String>) StatsManager.playerKitCosmetics.get(playerName);
                        if (otherPlayerCosmetics != null) {
                            cosmetic = otherPlayerCosmetics.getOrDefault(kitName, "default");
                        }
                    }

                    if (!cosmetic.equals("default")) {
                        Identifier modelId = Identifier.of("mcpvp", "item/" + cosmetic + "_sword_cosmetic");
                        BakedModel customModel = MinecraftClient.getInstance().getBakedModelManager()
                                .getModel(new ModelIdentifier(modelId, "inventory"));

                        if (customModel != null && !customModel.equals(MinecraftClient.getInstance()
                                .getBakedModelManager().getMissingBlockModel())) {
                            matrices.push();
                            customModel.getTransformation().getTransformation(renderMode).apply(leftHanded, matrices);
                            matrices.translate(-0.5F, -0.5F, -0.5F);
                            RenderLayer renderLayer = TexturedRenderLayers.getItemEntityTranslucentCull();
                            ItemRenderState.Glint glint = stack.hasGlint() ? ItemRenderState.Glint.STANDARD : ItemRenderState.Glint.NONE;
                            ItemRenderer.renderItem(renderMode, matrices, vertexConsumers, light, overlay, new int[0], customModel, renderLayer, glint);
                            matrices.pop();
                            ci.cancel();
                        } else if (MinecraftClient.getInstance().player != null
                                && renderMode == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND) {
                            /*
                            } else if (MinecraftClient.getInstance().player != null && renderMode == ModelTransformationMode.field_4317) {
                                }
                             */
                        }
                    }
                }
            }
        }
    }
}