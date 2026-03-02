package com.example.mcpvp;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;

@Environment(EnvType.CLIENT)
public class WorldManager {
    public static ClientWorld PERSISTENT_WORLD;
}
