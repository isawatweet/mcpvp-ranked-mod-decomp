package com.example.mcpvp.screens;

import com.example.mcpvp.CosmeticManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.client.gui.screen.Screen;
@Environment(EnvType.CLIENT)
public class TitleSelectionScreen extends Screen {
    private final Screen parent;

    public TitleSelectionScreen(Screen parent) {
        super(Text.literal("Select Title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        int y = 40;

        String noneText = CosmeticManager.selectedTitle.isEmpty() ? "None ✔" : "None";
        this.addDrawableChild(ButtonWidget.builder(Text.literal(noneText).formatted(Formatting.WHITE), btn -> {
            CosmeticManager.updateSelectedTitle("");
            this.client.setScreen(new TitleSelectionScreen(this.parent));
        }).dimensions(x, y, 200, 20).build());
        y += 22;

        synchronized (CosmeticManager.ownedTitles) {
            for (String title : CosmeticManager.ownedTitles) {
                boolean isSelected = title.equals(CosmeticManager.selectedTitle);
                Formatting rarityColor = CosmeticManager.getTitleColor(title);
                Text buttonText = Text.literal(title + (isSelected ? " ✔" : "")).formatted(rarityColor);

                final String titleFinal = title;
                final int yFinal = y;
                this.addDrawableChild(ButtonWidget.builder(buttonText, btn -> {
                    CosmeticManager.updateSelectedTitle(titleFinal);
                    this.client.setScreen(new TitleSelectionScreen(this.parent));
                }).dimensions(x, yFinal, 200, 20).build());
                y += 22;
            }
        }

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Back"), btn -> this.client.setScreen(this.parent))
                        .dimensions(this.width / 2 - 100, this.height - 30, 200, 20).build()
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