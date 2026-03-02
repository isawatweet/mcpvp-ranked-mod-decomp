package com.example.mcpvp;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.MCPVPClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.Game;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public class KitIdentifier {
    private static final Pattern ROUND_WON_PATTERN = Pattern.compile("\u2694\ufe0f (\\w+) won the (round|match)! \\((\\d+) - (\\d+)\\)");
    private static final Pattern SCORE_PATTERN = Pattern.compile("\\((\\d+) - (\\d+)\\)");

    public KitIdentifier() {
        super();
    }

    public static void init() {
        ClientReceiveMessageEvents.GAME
                .register(
                        (var0, var1) -> {
                            String text = var0.getString();
                            if (!text.startsWith("[RankedMod-Debug]")) {
                                MinecraftClient client = MinecraftClient.getInstance();
                                if (client.player != null) {
                                    String playerName = client.getSession().getUsername();
                                    if (text.contains("\u2795") && text.contains("joined the party!") && !MatchContext.wasInPartyBool) {
                                        MatchContext.wasInPartyBool = true;
                                        MCPVPClient.debugLog("Party Joined! Next match will NOT count towards ELO.");
                                    }

                                    if (text.contains("\u274c") && text.contains("The party has been disbanded by the host.") && MatchContext.wasInPartyBool) {
                                        MatchContext.wasInPartyBool = false;
                                        MCPVPClient.debugLog("Party Disbanded. Ranked status restored.");
                                    }

                                    if ((
                                            text.contains("Your") && text.contains("duel request was sent to")
                                                    || text.contains("has requested to duel you")
                                                    || text.contains("sent you a") && text.contains("duel request!")
                                    )
                                            && !MatchContext.duelRequestSent) {
                                        MatchContext.duelRequestSent = true;
                                        MCPVPClient.debugLog("Specific Duel Request Detected! Next match will NOT count towards ELO unless retracted.");
                                    }

                                    if ((text.contains("Your duel request was retracted.") || text.contains("duel request was rejected")) && MatchContext.duelRequestSent) {
                                        MatchContext.duelRequestSent = false;
                                        MCPVPClient.debugLog("Duel Request Retracted/Rejected. Ranked status restored.");
                                    }

                                    if (text.contains("resigned")) {
                                        if (MatchContext.inMatch && MatchContext.currentKit != null) {
                                            boolean iResigned = text.contains(playerName);
                                            if (MatchContext.isRanked) {
                                                MatchContext.updateElo(MatchContext.currentKit, !iResigned);
                                            }

                                            MatchContext.reset(true);
                                            MCPVPClient.debugLog((iResigned ? "You" : "Opponent") + " resigned. Match ended.");
                                        }
                                    } else if (text.contains("\u2694\ufe0f") || text.contains("\ud83d\udde1\ufe0f")) {
                                        Matcher matcher = ROUND_WON_PATTERN.matcher(text);
                                        if (matcher.find()) {
                                            String winner = matcher.group(1);
                                            String type = matcher.group(2);
                                            int score1 = Integer.parseInt(matcher.group(3));
                                            int score2 = Integer.parseInt(matcher.group(4));
                                            boolean iWon = winner.equalsIgnoreCase(client.player.getName().getString());
                                            if (iWon) {
                                                MatchContext.sessionKills = score1;
                                                MatchContext.sessionDeaths = score2;
                                            } else {
                                                MatchContext.sessionKills = score2;
                                                MatchContext.sessionDeaths = score1;
                                            }

                                            MCPVPClient.debugLog(
                                                    "Round/Match Won by: " + winner + " (Me: " + iWon + "). Stats: " + MatchContext.sessionKills + " - " + MatchContext.sessionDeaths
                                            );
                                            boolean isMatchEnd = false;
                                            if (MatchContext.currentKit != MatchContext.KitType.SWORD
                                                    && MatchContext.currentKit != MatchContext.KitType.AXE
                                                    && MatchContext.currentKit != MatchContext.KitType.POT) {
                                                isMatchEnd = true;
                                            } else {
                                                isMatchEnd = type.equals("match") || score1 >= 2 || score2 >= 2;
                                            }

                                            if (isMatchEnd && MatchContext.currentKit != null) {
                                                if (MatchContext.isRanked) {
                                                    MatchContext.updateElo(MatchContext.currentKit, iWon);
                                                }

                                                MatchContext.reset(true);
                                                MCPVPClient.debugLog(MatchContext.currentKit.name() + " Match Ended (Round Rule)");
                                            }
                                        }
                                    } else if (text.contains("Match Complete") && MatchContext.inMatch) {
                                        Matcher matcher = SCORE_PATTERN.matcher(text);
                                        if (matcher.find()) {
                                            int myScore = Integer.parseInt(matcher.group(1));
                                            int oppScore = Integer.parseInt(matcher.group(2));
                                            boolean wonMatch = myScore > oppScore;
                                            if (MatchContext.currentKit != null && MatchContext.isRanked) {
                                                MatchContext.updateElo(MatchContext.currentKit, wonMatch);
                                            }

                                            MatchContext.reset(true);
                                            MCPVPClient.debugLog("Match Ended (Score extracted from Match Complete)");
                                        }
                                    }
                                }
                            }
                        }
                );
    }
}
