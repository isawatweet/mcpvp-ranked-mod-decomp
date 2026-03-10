package com.example.mcpvp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class MatchContext {
    public static MatchContext.KitType currentKit = null;
    public static MatchContext.KitType currentQueuedKit = null;
    public static boolean kitSelectedInMenu = false;
    public static MatchContext.KitType lastMatchKit = null;
    public static boolean inMatch = false;
    public static boolean isMatchActive = false;
    public static boolean isRanked = false;
    public static boolean rankedEnabled = true;
    public static boolean isBanned = false;
    public static String banReason = "";
    public static boolean wasInPartyBool = false;
    public static boolean duelRequestSent = false;
    public static String lastScore = "";
    public static String opponentName = "Unknown";
    public static float lastRoundHealth = 0.0F;
    public static int sessionKills = 0;
    public static int sessionDeaths = 0;
    public static boolean pendingAbandonmentPenalty = false;
    public static MatchContext.KitType abandonedKit = null;
    public static final Map<MatchContext.KitType, Integer> kitElo = new HashMap();
    public static final Map<MatchContext.KitType, Integer> kitMasteryXP = new HashMap();
    public static final Map<MatchContext.KitType, Integer> placementMatches = new HashMap();
    public static final Map<MatchContext.KitType, Integer> placementWins = new HashMap();
    public static final Map<MatchContext.KitType, Integer> totalMatches = new HashMap();
    public static final Map<MatchContext.KitType, Integer> totalWins = new HashMap();
    public static final Map<MatchContext.KitType, Integer> winStreak = new HashMap();
    public static final Map<String, Integer> playerEloByName = new HashMap();
    public static boolean statsPublic = true;
    private static final List<String> ALLOWED_SERVERS = List.of("mcpvp.club", "catpvp.xyz");

    public MatchContext() {
        super();
    }

    public static boolean isAllowedServer() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isInSingleplayer()) {
            return true;
        } else {
            ServerInfo serverInfo = client.getCurrentServerEntry();
            if (serverInfo == null) {
                return false;
            } else {
                String address = serverInfo.address.toLowerCase();
                Stream<String> var10000 = ALLOWED_SERVERS.stream();
                Objects.requireNonNull(address);
                return var10000.anyMatch(address::contains);
            }
        }
    }

    public static void reset() {
        reset(false);
    }

    public static void reset(boolean cleanExit) {
        if (inMatch && isRanked && (currentKit != null || currentQueuedKit != null) && !cleanExit) {
            updateElo(currentKit != null ? currentKit : currentQueuedKit, false);
        }

        if (currentKit != null) {
            lastMatchKit = currentKit;
        } else if (currentQueuedKit != null) {
            lastMatchKit = currentQueuedKit;
        }

        currentKit = null;
        currentQueuedKit = null;
        kitSelectedInMenu = false;
        inMatch = false;
        isMatchActive = false;
        isRanked = false;
        duelRequestSent = false;
        lastScore = "";
        if (!opponentName.equals("Unknown")) {
            playerEloByName.remove(opponentName);
        }

        opponentName = "Unknown";
        lastRoundHealth = 0.0F;
    }

    public static void updateElo(MatchContext.KitType kit, boolean won) {
        if (kit != null) {
            int xpGained = won ? 25 : 10;
            kitMasteryXP.put(kit, (Integer)kitMasteryXP.getOrDefault(kit, 0) + xpGained);
            if (!isRanked) {
                StatsManager.save();
                StatsManager.pushToGlobal();
            } else {
                inMatch = false;
                isMatchActive = false;
                lastMatchKit = kit;
                totalMatches.put(kit, (Integer)totalMatches.getOrDefault(kit, 0) + 1);
                if (won) {
                    totalWins.put(kit, (Integer)totalWins.getOrDefault(kit, 0) + 1);
                }

                float health = lastRoundHealth;
                if (won && health == 0.0F && MinecraftClient.getInstance().player != null) {
                    health = MinecraftClient.getInstance().player.getHealth();
                }

                StatsManager.save();
                StatsManager.pushToGlobal();
                StatsManager.pushMatchResult(kit, won, health, opponentName);
                if (MinecraftClient.getInstance().player != null) {
                    Formatting resultColor = won ? Formatting.GREEN : Formatting.RED;
                    MinecraftClient.getInstance().player.sendMessage(Text.literal("\u00a78\u00a7m----------------------------------------"), false);
                    MinecraftClient.getInstance().player.sendMessage(Text.literal("\u00a76\u00a7lRANKED MATCH SUMMARY"), false);
                    MinecraftClient.getInstance().player.sendMessage(Text.literal("\u00a77Opponent: \u00a7f" + opponentName), false);
                    MinecraftClient.getInstance()
                            .player
                            .sendMessage(Text.literal("\u00a77Result: ").append(Text.literal(won ? "WIN" : "LOSS").formatted(resultColor)), false);
                    MinecraftClient.getInstance()
                            .player
                            .sendMessage(Text.literal("\u00a77Mastery XP: \u00a7a+" + xpGained + " \u00a78(\u00a7f" + kitMasteryXP.get(kit) + " Total\u00a78)"), false);
                    MinecraftClient.getInstance().player.sendMessage(Text.literal("\u00a7e\u00a7oSyncing ELO with server..."), false);
                    MinecraftClient.getInstance().player.sendMessage(Text.literal("\u00a78\u00a7m----------------------------------------"), false);
                }
            }
        }
    }

    public static int getMasteryLevel(int xp) {
        return (int)Math.floor(Math.sqrt(xp / 10.0)) + 1;
    }

    public static int getXPForLevel(int level) {
        return level <= 1 ? 0 : (int)(Math.pow(level - 1, 2.0) * 10.0);
    }

    public static Identifier getRankIcon(int leaderboardPos, int elo) {
        String name = "barrier";
        if (leaderboardPos <= 100 && elo >= 3400) {
            name = "dragon_breath";
        } else if (elo >= 3400) {
            name = "netherite_ingot";
        } else if (elo >= 2800) {
            name = "diamond";
        } else if (elo >= 2400) {
            name = "emerald";
        } else if (elo >= 2000) {
            name = "amethyst_shard";
        } else if (elo >= 1600) {
            name = "gold_ingot";
        } else if (elo >= 1200) {
            name = "lapis_lazuli";
        } else if (elo >= 800) {
            name = "iron_ingot";
        } else if (elo >= 400) {
            name = "coal";
        } else if (elo > 0) {
            name = "leather";
        }

        return Identifier.ofVanilla("textures/item/" + name + ".png");
    }

    public static Identifier getMasteryIconPath(MatchContext.KitType kit, int level) {
        switch (kit) {
            case SWORD:
                if (level >= 51) {
                    return Identifier.ofVanilla("textures/item/netherite_sword.png");
                } else if (level >= 41) {
                    return Identifier.ofVanilla("textures/item/diamond_sword.png");
                } else if (level >= 31) {
                    return Identifier.ofVanilla("textures/item/golden_sword.png");
                } else if (level >= 21) {
                    return Identifier.ofVanilla("textures/item/iron_sword.png");
                } else {
                    if (level >= 11) {
                        return Identifier.ofVanilla("textures/item/stone_sword.png");
                    }

                    return Identifier.ofVanilla("textures/item/wooden_sword.png");
                }
            case AXE:
                if (level >= 51) {
                    return Identifier.ofVanilla("textures/item/netherite_axe.png");
                } else if (level >= 41) {
                    return Identifier.ofVanilla("textures/item/diamond_axe.png");
                } else if (level >= 31) {
                    return Identifier.ofVanilla("textures/item/golden_axe.png");
                } else if (level >= 21) {
                    return Identifier.ofVanilla("textures/item/iron_axe.png");
                } else {
                    if (level >= 11) {
                        return Identifier.ofVanilla("textures/item/stone_axe.png");
                    }

                    return Identifier.ofVanilla("textures/item/wooden_axe.png");
                }
            case UHC:
                if (level >= 41) {
                    return Identifier.ofVanilla("textures/item/enchanted_golden_apple.png");
                } else {
                    if (level >= 21) {
                        return Identifier.ofVanilla("textures/item/golden_apple.png");
                    }

                    return Identifier.ofVanilla("textures/item/apple.png");
                }
            case MACE:
                if (level >= 21) {
                    return Identifier.ofVanilla("textures/item/mace.png");
                }

                return Identifier.ofVanilla("textures/item/heavy_core.png");
            case SPEAR:
                return Identifier.ofVanilla("textures/item/trident.png");
            case NETHERITE_OP:
                if (level >= 41) {
                    return Identifier.ofVanilla("textures/item/netherite_chestplate.png");
                } else {
                    if (level >= 21) {
                        return Identifier.ofVanilla("textures/item/netherite_ingot.png");
                    }

                    return Identifier.ofVanilla("textures/item/netherite_scrap.png");
                }
            case POT:
                if (level >= 31) {
                    return Identifier.ofVanilla("textures/item/lingering_potion.png");
                } else if (level >= 21) {
                    return Identifier.ofVanilla("textures/item/splash_potion.png");
                } else {
                    if (level >= 11) {
                        return Identifier.ofVanilla("textures/item/potion.png");
                    }

                    return Identifier.ofVanilla("textures/item/glass_bottle.png");
                }
            case SMP:
                return Identifier.ofVanilla("textures/item/ender_pearl.png");
            case CRYSTAL:
                if (level >= 41) {
                    return Identifier.ofVanilla("textures/item/nether_star.png");
                } else {
                    if (level >= 21) {
                        return Identifier.ofVanilla("textures/item/end_crystal.png");
                    }

                    return Identifier.ofVanilla("textures/item/ghast_tear.png");
                }
            default:
                return Identifier.ofVanilla("textures/item/wooden_shovel.png");
        }
    }

    public static String getRankDisplay(int leaderboardPos, int elo) {
        if (leaderboardPos <= 100 && elo >= 3400) {
            return "Dragon";
        } else if (elo >= 3400) {
            return "Netherite";
        } else if (elo >= 2800) {
            return "Diamond";
        } else if (elo >= 2400) {
            return "Emerald";
        } else if (elo >= 2000) {
            return "Amethyst";
        } else if (elo >= 1600) {
            return "Gold";
        } else if (elo >= 1200) {
            return "Lapis";
        } else if (elo >= 800) {
            return "Iron";
        } else if (elo >= 400) {
            return "Coal";
        } else {
            return elo > 0 ? "Leather" : "Unranked";
        }
    }

    public static Formatting getEloColor(int leaderboardPos, int elo) {
        if (leaderboardPos <= 100 && elo >= 3400) {
            return Formatting.DARK_RED;
        } else if (elo >= 3400) {
            return Formatting.DARK_PURPLE;
        } else if (elo >= 2800) {
            return Formatting.AQUA;
        } else if (elo >= 2400) {
            return Formatting.GREEN;
        } else if (elo >= 2000) {
            return Formatting.LIGHT_PURPLE;
        } else if (elo >= 1600) {
            return Formatting.GOLD;
        } else if (elo >= 1200) {
            return Formatting.BLUE;
        } else if (elo >= 800) {
            return Formatting.WHITE;
        } else if (elo >= 400) {
            return Formatting.DARK_GRAY;
        } else {
            return elo > 0 ? Formatting.GOLD : Formatting.GRAY;
        }
    }

    static {
        for (MatchContext.KitType type : MatchContext.KitType.values()) {
            kitElo.put(type, 0);
            kitMasteryXP.put(type, 0);
            placementMatches.put(type, 0);
            placementWins.put(type, 0);
            totalMatches.put(type, 0);
            totalWins.put(type, 0);
            winStreak.put(type, 0);
        }
    }

    @Environment(EnvType.CLIENT)
    public static enum KitType {
        SWORD("Sword", Items.DIAMOND_SWORD, Identifier.of("template-mod", "textures/item/sword_icon.png")),
        AXE("Axe", Items.DIAMOND_AXE, Identifier.of("template-mod", "textures/item/axe_icon.png")),
        UHC("UHC", Items.GOLDEN_APPLE, Identifier.of("template-mod", "textures/item/uhc_icon.png")),
        MACE("Mace", Items.MACE, Identifier.of("template-mod", "textures/item/mace_icon.png")),
        SPEAR("Spear PVP", Items.TRIDENT, Identifier.of("template-mod", "textures/item/spear.png")),
        NETHERITE_OP("Netherite OP", Items.NETHERITE_HELMET, Identifier.of("template-mod", "textures/item/netheriteop_icon.png")),
        POT("Pot", Items.SPLASH_POTION, Identifier.ofVanilla("textures/item/potion.png")),
        SMP("SMP", Items.ENDER_PEARL, Identifier.ofVanilla("textures/item/ender_pearl.png")),
        CRYSTAL("Crystal", Items.END_CRYSTAL, Identifier.of("template-mod", "textures/item/crystal_icon.png"));

        public final String apiName;
        public final Item icon;
        public final Identifier customTexture;

        private KitType(String apiName, Item icon, Identifier customTexture) {
            this.apiName = apiName;
            this.icon = icon;
            this.customTexture = customTexture;
        }

        public static MatchContext.KitType fromItem(Item item) {
            if (item == Items.DIAMOND_SWORD) {
                return SWORD;
            } else if (item == Items.DIAMOND_AXE) {
                return AXE;
            } else if (item == Items.MACE) {
                return MACE;
            } else if (item == Items.GOLDEN_APPLE) {
                return UHC;
            } else if (item == Items.TRIDENT) {
                return SPEAR;
            } else if (item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE) {
                return NETHERITE_OP;
            } else if (item == Items.SPLASH_POTION) {
                return POT;
            } else if (item == Items.ENDER_PEARL || item == Items.SHIELD) {
                return SMP;
            } else {
                return item == Items.END_CRYSTAL ? CRYSTAL : null;
            }
        }

        // $VF: synthetic method
        private static MatchContext.KitType[] $values() {
            return new MatchContext.KitType[]{SWORD, AXE, UHC, MACE, SPEAR, NETHERITE_OP, POT, SMP, CRYSTAL};
        }
    }
}
