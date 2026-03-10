package com.example.mcpvp;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin.Context;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ModelLoaderPlugin implements ModelLoadingPlugin {
    public ModelLoaderPlugin() {
        super();
    }

    public void initialize(Context context) {
        String[] cosmetics = new String[]{"gold", "iron", "wood", "netherite"};

        for (String cosmetic : cosmetics) {
            context.addModels(Identifier.of("template-mod", "item/" + cosmetic + "_sword_cosmetic"));
        }
    }
}
