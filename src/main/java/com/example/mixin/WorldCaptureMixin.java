package com.example.mixin;

import com.example.mcpvp.StatsManager;
import com.example.mcpvp.WorldManager;
import com.example.mcpvp.MatchContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public class WorldCaptureMixin {

    @Inject(method = "setWorld", at = @At("HEAD"))
    private void onSetWorld(ClientWorld world, CallbackInfo cir) {
        if (world == null) {
            if (MatchContext.inMatch && MatchContext.isRanked) {
                MatchContext.reset(false);
                StatsManager.save();
            }
        } else {
            WorldManager.PERSISTENT_WORLD = world;
        }
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void onStop(CallbackInfo var1) {
        if (MatchContext.inMatch && MatchContext.isRanked) {
            MatchContext.reset(false);
            StatsManager.save();
        }
    }
}
