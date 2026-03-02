package com.example.mcpvp.screens;

import com.example.mcpvp.MatchContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Environment(EnvType.CLIENT)
public class CosmeticScreen extends Screen {
    private final Screen parent;
    private boolean showingKits = false;

    public CosmeticScreen(Screen parent) {
        super(Text.literal("Cosmetics"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (!this.showingKits) {
            this.addDrawableChild(
                    ButtonWidget.builder(
                                    Text.literal("Select Title").formatted(Formatting.GOLD),
                                    btn -> this.client.setScreen(new TitleSelectionScreen(this)))
                            .dimensions(centerX - 100, centerY - 40, 200, 20)
                            .build()
            );
            this.addDrawableChild(
                    ButtonWidget.builder(
                                    Text.literal("Item Cosmetics").formatted(Formatting.AQUA),
                                    btn -> {
                                        this.showingKits = true;
                                        this.clearAndInit();
                                    })
                            .dimensions(centerX - 100, centerY - 15, 200, 20)
                            .build()
            );
        } else {
            int y = centerY - 40;
            MatchContext.KitType[] kits = new MatchContext.KitType[]{
                    MatchContext.KitType.SWORD,
                    MatchContext.KitType.AXE,
                    MatchContext.KitType.NETHERITE_OP
            };
            for (MatchContext.KitType kit : kits) {
                this.addDrawableChild(
                        ButtonWidget.builder(
                                        Text.literal(kit.apiName + " Kit"),
                                        btn -> this.client.setScreen(new SwordCosmeticScreen(this, kit)))
                                .dimensions(centerX - 80, y, 160, 20)
                                .build()
                );
                y += 25;
            }
        }

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Back"), btn -> {
                    if (this.showingKits) {
                        this.showingKits = false;
                        this.clearAndInit();
                    } else {
                        this.client.setScreen(this.parent);
                    }
                }).dimensions(centerX - 100, this.height - 30, 200, 20).build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 20, 16777215);

        if (this.showingKits) {
            context.drawCenteredTextWithShadow(this.textRenderer, "§6§lKits", centerX, centerY - 55, 16777215);
            int y = centerY - 38;
            MatchContext.KitType[] kits = new MatchContext.KitType[]{
                    MatchContext.KitType.SWORD,
                    MatchContext.KitType.AXE,
                    MatchContext.KitType.NETHERITE_OP
            };
            for (MatchContext.KitType kit : kits) {
                context.drawTexture(RenderLayer::getGuiTextured, kit.customTexture, centerX - 105, y, 0.0F, 0.0F, 16, 16, 16, 16);
                y += 25;
            }
        }
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}