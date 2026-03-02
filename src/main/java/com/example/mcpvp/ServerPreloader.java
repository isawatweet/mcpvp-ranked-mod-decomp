package com.example.mcpvp;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;

@Environment(EnvType.CLIENT)
public class ServerPreloader {
    public ServerPreloader() {
        super();
    }

    public static void preload() {
        MinecraftClient client = MinecraftClient.getInstance();
        ServerList serverList = new ServerList(client);
        serverList.loadFile();
        boolean exists = false;

        for (int i = 0; i < serverList.size(); i++) {
            ServerInfo info = serverList.get(i);
            if (info.address.equalsIgnoreCase("mcpvp.club")) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            ServerInfo mcpvp = new ServerInfo("MCPVP CLUB", "mcpvp.club", ServerInfo.ServerType.OTHER);
            serverList.add(mcpvp, false);
            serverList.saveFile();
        }
    }
}
