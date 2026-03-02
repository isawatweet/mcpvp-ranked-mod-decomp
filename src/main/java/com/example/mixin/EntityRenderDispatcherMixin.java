package com.example.mixin;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Shadow
    public Camera camera;

    @Inject(method = "getSquaredDistanceToCamera(DDD)D", at = @At("HEAD"), cancellable = true)
    private void onGetSquaredDistanceToCamera(double x, double y, double z, CallbackInfoReturnable<Double> cir) {
        if (this.camera == null) {
            cir.setReturnValue(0.0);
        }
    }

    @Inject(method = "getSquaredDistanceToCamera(Lnet/minecraft/entity/Entity;)D", at = @At("HEAD"), cancellable = true)
    private void onGetSquaredDistanceToCameraEntity(Entity entity, CallbackInfoReturnable<Double> cir) {
        if (this.camera == null) {
            cir.setReturnValue(0.0);
        }
    }
}
