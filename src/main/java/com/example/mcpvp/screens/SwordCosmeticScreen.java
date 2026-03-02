package com.example.mcpvp.screens;

import java.util.LinkedHashMap;
import java.util.Map;

import com.example.mcpvp.CosmeticManager;
import com.example.mcpvp.MatchContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
@Environment(EnvType.CLIENT)
public class SwordCosmeticScreen extends Screen {
    private final Screen parent;
    private final MatchContext.KitType kit;
    private static final Map<String, Integer> COSMETIC_RANKS = new LinkedHashMap<>();

    static {
        COSMETIC_RANKS.put("default", 0);
        COSMETIC_RANKS.put("wood", 1);
        COSMETIC_RANKS.put("iron", 800);
        COSMETIC_RANKS.put("gold", 1600);
        COSMETIC_RANKS.put("netherite", 3400);
    }

    public SwordCosmeticScreen(Screen parent, MatchContext.KitType kit) {
        super(Text.literal(kit.apiName + " Cosmetics"));
        this.parent = parent;
        this.kit = kit;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = 40;
        int kitPeakElo = (Integer)CosmeticManager.peakElo.getOrDefault(this.kit.apiName, 0);
        int currentElo = (Integer)MatchContext.kitElo.getOrDefault(this.kit, 0);
        int unlockElo = Math.max(kitPeakElo, currentElo);

        for (Map.Entry<String, Integer> entry : COSMETIC_RANKS.entrySet()) {
            String id = entry.getKey();
            int requiredElo = entry.getValue();
            boolean unlocked = unlockElo >= requiredElo;

            if (unlocked) {
                boolean isSelected = CosmeticManager.getSelectedCosmetic(this.kit.apiName).equals(id);
                String displayName = id.equals("default") ? "Default"
                        : id.substring(0, 1).toUpperCase() + id.substring(1) + " Sword";

                MutableText buttonText = Text.literal(displayName);
                if (isSelected) {
                    buttonText.append(Text.literal(" ✔").formatted(Formatting.GREEN));
                }

                final String idFinal = id;
                final int yFinal = y;
                this.addDrawableChild(ButtonWidget.builder(buttonText, btn -> {
                    CosmeticManager.updateKitCosmetic(this.kit.apiName, idFinal);
                    this.client.setScreen(new SwordCosmeticScreen(this.parent, this.kit));
                }).dimensions(centerX - 100, yFinal, 200, 20).build());
                y += 22;
            }
        }

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Back"), btn -> this.client.setScreen(this.parent))
                        .dimensions(centerX - 100, this.height - 30, 200, 20).build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 16777215);
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}