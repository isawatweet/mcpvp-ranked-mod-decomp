package com.example.mcpvp;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;

@Environment(EnvType.CLIENT)
public class KitScanner {
    public KitScanner() {
        super();
    }

    public static MatchContext.KitType detectKit(PlayerInventory inventory) {
        boolean hasMace = false;
        boolean hasElytra = false;
        boolean hasWindCharge = false;
        boolean hasEndCrystal = false;
        boolean hasRespawnAnchor = false;
        boolean hasCobweb = false;
        boolean hasLava = false;
        boolean hasWater = false;
        boolean hasPickaxe = false;
        boolean hasShield = false;
        boolean hasTotem = false;
        boolean hasDiamondSword = false;
        boolean hasNetheriteSword = false;
        boolean hasDiamondAxe = false;
        boolean hasNetheriteAxe = false;
        boolean hasEnderPearl = false;
        boolean hasXpBottle = false;
        boolean hasGapple = false;
        boolean hasTrident = false;
        boolean hasBow = false;
        boolean hasCrossbow = false;
        boolean hasSplashPotion = false;
        boolean hasStrength2 = false;
        boolean hasSpeed2 = false;
        boolean hasFireRes = false;
        boolean hasHealingSplash = false;
        boolean hasRegenSplash = false;
        boolean hasDiaHelmet = inventory.getArmorStack(3).isOf(Items.DIAMOND_HELMET);
        boolean hasDiaChest = inventory.getArmorStack(2).isOf(Items.DIAMOND_CHESTPLATE);
        boolean hasDiaLegs = inventory.getArmorStack(1).isOf(Items.DIAMOND_LEGGINGS);
        boolean hasDiaBoots = inventory.getArmorStack(0).isOf(Items.DIAMOND_BOOTS);
        boolean hasFullDiamondArmor = hasDiaHelmet && hasDiaChest && hasDiaLegs && hasDiaBoots;
        boolean hasNethHelmet = inventory.getArmorStack(3).isOf(Items.NETHERITE_HELMET);
        boolean hasNethChest = inventory.getArmorStack(2).isOf(Items.NETHERITE_CHESTPLATE);
        boolean hasNethLegs = inventory.getArmorStack(1).isOf(Items.NETHERITE_LEGGINGS);
        boolean hasNethBoots = inventory.getArmorStack(0).isOf(Items.NETHERITE_BOOTS);
        boolean hasFullNetheriteArmor = hasNethHelmet && hasNethChest && hasNethLegs && hasNethBoots;

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                if (stack.isOf(Items.MACE)) {
                    hasMace = true;
                }

                if (stack.isOf(Items.ELYTRA)) {
                    hasElytra = true;
                }

                if (stack.isOf(Items.WIND_CHARGE)) {
                    hasWindCharge = true;
                }

                if (stack.isOf(Items.END_CRYSTAL)) {
                    hasEndCrystal = true;
                }

                if (stack.isOf(Items.RESPAWN_ANCHOR)) {
                    hasRespawnAnchor = true;
                }

                if (stack.isOf(Items.COBWEB)) {
                    hasCobweb = true;
                }

                if (stack.isOf(Items.LAVA_BUCKET)) {
                    hasLava = true;
                }

                if (stack.isOf(Items.WATER_BUCKET)) {
                    hasWater = true;
                }

                if (stack.isOf(Items.DIAMOND_PICKAXE) || stack.isOf(Items.NETHERITE_PICKAXE)) {
                    hasPickaxe = true;
                }

                if (stack.isOf(Items.SHIELD)) {
                    hasShield = true;
                }

                if (stack.isOf(Items.TOTEM_OF_UNDYING)) {
                    hasTotem = true;
                }

                if (stack.isOf(Items.DIAMOND_SWORD)) {
                    hasDiamondSword = true;
                }

                if (stack.isOf(Items.NETHERITE_SWORD)) {
                    hasNetheriteSword = true;
                }

                if (stack.isOf(Items.DIAMOND_AXE)) {
                    hasDiamondAxe = true;
                }

                if (stack.isOf(Items.NETHERITE_AXE)) {
                    hasNetheriteAxe = true;
                }

                if (stack.isOf(Items.ENDER_PEARL)) {
                    hasEnderPearl = true;
                }

                if (stack.isOf(Items.EXPERIENCE_BOTTLE)) {
                    hasXpBottle = true;
                }

                if (stack.isOf(Items.GOLDEN_APPLE)) {
                    hasGapple = true;
                }

                if (stack.isOf(Items.TRIDENT)) {
                    hasTrident = true;
                }

                if (stack.isOf(Items.BOW)) {
                    hasBow = true;
                }

                if (stack.isOf(Items.CROSSBOW)) {
                    hasCrossbow = true;
                }

                if (stack.isOf(Items.SPLASH_POTION)) {
                    hasSplashPotion = true;
                    PotionContentsComponent potion = stack.get(DataComponentTypes.POTION_CONTENTS);
                    if (potion != null) {
                        if (potion.matches(Potions.STRONG_STRENGTH)) {
                            hasStrength2 = true;
                        }
                        if (potion.matches(Potions.STRONG_SWIFTNESS)) {
                            hasSpeed2 = true;
                        }
                        if (potion.matches(Potions.HEALING) || potion.matches(Potions.STRONG_HEALING)) {
                            hasHealingSplash = true;
                        }
                        if (potion.matches(Potions.REGENERATION) || potion.matches(Potions.STRONG_REGENERATION)) {
                            hasRegenSplash = true;
                        }
                        if (potion.matches(Potions.FIRE_RESISTANCE) || potion.matches(Potions.LONG_FIRE_RESISTANCE)) {
                            hasFireRes = true;
                        }
                    }
                }
            }
        }

        if (hasMace && hasElytra) {
            return MatchContext.KitType.MACE;
        } else if (hasEndCrystal && hasRespawnAnchor) {
            return MatchContext.KitType.CRYSTAL;
        } else if (hasFullDiamondArmor && hasLava && hasWater && hasCobweb && hasPickaxe) {
            return MatchContext.KitType.UHC;
        } else if (hasFullNetheriteArmor && hasEnderPearl && hasXpBottle && hasFireRes && hasShield) {
            return MatchContext.KitType.SMP;
        } else if (hasFullNetheriteArmor && hasTotem && hasHealingSplash && hasGapple && !hasShield) {
            return MatchContext.KitType.NETHERITE_OP;
        } else if (hasFullDiamondArmor && hasShield && hasDiamondAxe && hasBow && hasCrossbow) {
            return MatchContext.KitType.AXE;
        } else if (hasFullDiamondArmor && hasSplashPotion && (hasRegenSplash || hasHealingSplash || hasStrength2 || hasSpeed2)) {
            return MatchContext.KitType.POT;
        } else if (hasTrident) {
            return MatchContext.KitType.SPEAR;
        } else {
            return hasFullDiamondArmor && hasDiamondSword && !hasShield && !hasDiamondAxe && !hasSplashPotion ? MatchContext.KitType.SWORD : null;
        }
    }
}
