package com.example.mcpvp;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class EloBarRenderer {
    private static final Identifier EXPERIENCE_BAR_BACKGROUND_TEXTURE = Identifier.ofVanilla("hud/experience_bar_background");
    private static boolean visible = false;
    private static long startTime = 0L;
    private static final long DURATION = 5000L;
    private static final long ANIMATION_START_DELAY = 1000L;
    private static final long ANIMATION_DURATION = 2000L;
    private static int oldElo = 0;
    private static int newElo = 0;
    private static int baseChange = 0;
    private static int bonus = 0;
    private static int placements = 0;

    public EloBarRenderer() {
        super();
    }

    public static void show(int oldE, int newE, int base, int bon, int p) {
        oldElo = oldE;
        newElo = newE;
        baseChange = base;
        bonus = bon;
        placements = p;
        startTime = System.currentTimeMillis();
        visible = true;
    }

    public static void render(DrawContext context) {
        if (visible) {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > 5000L) {
                visible = false;
            } else {
                MinecraftClient client = MinecraftClient.getInstance();
                int centerX = client.getWindow().getScaledWidth() / 2;
                int centerY = 20;
                float animationProgress = 0.0F;
                if (elapsed > 1000L) {
                    animationProgress = MathHelper.clamp((float)(elapsed - 1000L) / 2000.0F, 0.0F, 1.0F);
                }

                float barProgress;
                String eloText;
                String changeText;
                if (placements < 10) {
                    float oldProgress = (placements - 1) / 10.0F;
                    float newProgress = placements / 10.0F;
                    barProgress = MathHelper.lerp(animationProgress, oldProgress, newProgress);
                    eloText = "Placement " + placements + "/10";
                    changeText = "\u00a7e+1 Match";
                } else if (placements == 10 && oldElo == 0) {
                    int currentDisplayElo = MathHelper.lerp(animationProgress, 0, newElo);
                    int nextRankElo = getNextRankElo(currentDisplayElo);
                    int prevRankElo = getPrevRankElo(currentDisplayElo);
                    barProgress = nextRankElo > prevRankElo ? (float)(currentDisplayElo - prevRankElo) / (nextRankElo - prevRankElo) : 1.0F;
                    eloText = "Placed: " + currentDisplayElo;
                    changeText = "\u00a7a+" + newElo;
                } else {
                    int currentDisplayElo = MathHelper.lerp(animationProgress, oldElo, newElo);
                    int nextRankElo = getNextRankElo(currentDisplayElo);
                    int prevRankElo = getPrevRankElo(currentDisplayElo);
                    barProgress = nextRankElo > prevRankElo ? (float)(currentDisplayElo - prevRankElo) / (nextRankElo - prevRankElo) : 1.0F;
                    eloText = String.valueOf(currentDisplayElo);
                    changeText = (baseChange >= 0 ? "\u00a7a+" : "\u00a7c") + baseChange;
                    if (bonus > 0) {
                        changeText = changeText + " \u00a76(+" + bonus + " Bonus)";
                    }
                }

                barProgress = MathHelper.clamp(barProgress, 0.0F, 1.0F);
                int barWidth = 182;
                int barHeight = 5;
                int barX = centerX - barWidth / 2;
                context.drawGuiTexture(RenderLayer::getGuiTextured, EXPERIENCE_BAR_BACKGROUND_TEXTURE, barX, centerY, barWidth, barHeight);
                if (barProgress > 0.0F) {
                    int filledWidth = (int)(barProgress * barWidth);
                    int color = placements < 10 ? -171 : -11141121;
                    context.fill(barX, centerY, barX + filledWidth, centerY + barHeight, color);
                    context.fill(barX, centerY, barX + filledWidth, centerY + 1, -1996488705);
                }

                context.drawCenteredTextWithShadow(client.textRenderer, "\u00a7b" + eloText, centerX, centerY - 10, 16777215);
                context.drawTextWithShadow(client.textRenderer, changeText, barX + barWidth + 5, centerY - 2, 16777215);
            }
        }
    }

    private static int getNextRankElo(int elo) {
        if (elo < 400) {
            return 400;
        } else if (elo < 800) {
            return 800;
        } else if (elo < 1200) {
            return 1200;
        } else if (elo < 1600) {
            return 1600;
        } else if (elo < 2000) {
            return 2000;
        } else if (elo < 2400) {
            return 2400;
        } else if (elo < 2800) {
            return 2800;
        } else {
            return elo < 3400 ? 3400 : Integer.MAX_VALUE;
        }
    }

    private static int getPrevRankElo(int elo) {
        if (elo >= 3400) {
            return 3400;
        } else if (elo >= 2800) {
            return 2800;
        } else if (elo >= 2400) {
            return 2400;
        } else if (elo >= 2000) {
            return 2000;
        } else if (elo >= 1600) {
            return 1600;
        } else if (elo >= 1200) {
            return 1200;
        } else if (elo >= 800) {
            return 800;
        } else {
            return elo >= 400 ? 400 : 0;
        }
    }
}
