package com.example.mixin;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
    @Inject(method = "getPlayerListEntry", at = @At("HEAD"), cancellable = true)
    private void onGetPlayerListEntry(CallbackInfoReturnable<PlayerListEntry> cir) {
        if (MinecraftClient.getInstance().getNetworkHandler() == null) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "getSkinTextures", at = @At("HEAD"), cancellable = true)
    private void onGetSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) {
            cir.setReturnValue(client.getSkinProvider().getSkinTextures(client.getGameProfile()));
        }
    }
}
