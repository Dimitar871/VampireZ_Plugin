package com.vampirez.discord;

import com.vampirez.GameState;
import com.vampirez.VampireZPlugin;
import com.vampirez.api.event.VampireZGameEndEvent;
import com.vampirez.config.PluginConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.time.Instant;
import java.util.List;

/**
 * Pure builder — no Bukkit or JDA-thread concerns. All player names are pre-resolved
 * by the caller (on the main thread) before being passed in. Colors come from the
 * {@link PluginConfig.DiscordSection} so admins can customize without recompiling.
 */
public class DiscordEmbedFactory {

    private final VampireZPlugin plugin;

    public DiscordEmbedFactory(VampireZPlugin plugin) {
        this.plugin = plugin;
    }

    private PluginConfig.DiscordSection cfg() { return plugin.getPluginConfig().discord; }

    /** Live channel embed — edited every N seconds during ACTIVE games. */
    public MessageEmbed liveStatus(GameState state, int remainingSeconds, int joined,
                                   int humans, int vampires, boolean night, int minPlayers) {
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("VampireZ — Live Status")
                .setColor(colorFor(state));

        eb.addField("State", labelFor(state), true);
        if (state == GameState.LOBBY) {
            eb.addField("Players", joined + "/" + minPlayers, true);
        } else {
            eb.addField("Humans",   String.valueOf(humans), true);
            eb.addField("Vampires", String.valueOf(vampires), true);
        }
        if (state == GameState.ACTIVE) {
            eb.addField("Time Left", formatMmSs(remainingSeconds), true);
            eb.addField("Phase", night ? "🌙 Night" : "☀ Day", true);
        }
        eb.setTimestamp(Instant.now());
        return eb.build();
    }

    /** Announcement when a game starts. */
    public MessageEmbed gameStart(List<String> humanNames, List<String> vampireNames, boolean forced) {
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("⚔ VampireZ — Game Started" + (forced ? " (forced)" : ""))
                .setColor(parseHex(cfg().colorActive))
                .addField("Humans (" + humanNames.size() + ")",   namesList(humanNames),   false)
                .addField("Vampires (" + vampireNames.size() + ")", namesList(vampireNames), false)
                .setTimestamp(Instant.now());
        return eb.build();
    }

    /** Announcement when a game ends naturally (humans/vampires won) or is stopped. */
    public MessageEmbed gameEnd(VampireZGameEndEvent.Winner winner, int durationSeconds) {
        String title;
        int color;
        switch (winner) {
            case HUMANS    -> { title = "🛡 Humans Win!";   color = parseHex(cfg().colorEnded); }
            case VAMPIRES  -> { title = "🩸 Vampires Win!"; color = parseHex(cfg().colorEnded); }
            case STOPPED   -> { title = "⏹ Game Stopped";   color = parseHex(cfg().colorLobby); }
            default        -> { title = "Game Ended";       color = parseHex(cfg().colorEnded); }
        }
        return new EmbedBuilder()
                .setTitle(title)
                .setColor(color)
                .addField("Duration", formatMmSs(durationSeconds), true)
                .setTimestamp(Instant.now())
                .build();
    }

    /** Announcement when a human is converted to a vampire (gated by config). */
    public MessageEmbed conversion(String victimName, int humansAlive, int vampireCount) {
        return new EmbedBuilder()
                .setTitle("🩸 " + victimName + " has fallen")
                .setDescription("Risen again as a Vampire.")
                .setColor(parseHex(cfg().colorActive))
                .addField("Humans Remaining", String.valueOf(humansAlive), true)
                .addField("Vampires", String.valueOf(vampireCount), true)
                .setTimestamp(Instant.now())
                .build();
    }

    /** Announcement when day↔night flips. */
    public MessageEmbed dayPhase(boolean nowNight) {
        return new EmbedBuilder()
                .setTitle(nowNight ? "🌙 Night has fallen" : "☀ The sun rises")
                .setDescription(nowNight
                        ? "Vampires grow stronger…"
                        : "Vampires are weakened…")
                .setColor(nowNight ? 0x4B0082 : 0xF1C40F)
                .setTimestamp(Instant.now())
                .build();
    }

    // ===== package-private helpers (also used by tests) =====

    static String formatMmSs(int totalSeconds) {
        int s = Math.max(0, totalSeconds);
        return String.format("%d:%02d", s / 60, s % 60);
    }

    static String labelFor(GameState state) {
        return switch (state) {
            case LOBBY    -> "Lobby";
            case STARTING -> "Starting";
            case ACTIVE   -> "Active";
            case ENDING   -> "Ending";
        };
    }

    int colorFor(GameState state) {
        return switch (state) {
            case LOBBY                       -> parseHex(cfg().colorLobby);
            case STARTING, ACTIVE            -> parseHex(cfg().colorActive);
            case ENDING                      -> parseHex(cfg().colorEnded);
        };
    }

    static int parseHex(String hex) {
        if (hex == null || hex.isBlank()) return 0x808080;
        try {
            return Integer.parseInt(hex.replace("#", "").trim(), 16);
        } catch (NumberFormatException e) {
            return 0x808080;
        }
    }

    private static String namesList(List<String> names) {
        if (names == null || names.isEmpty()) return "_(none)_";
        return String.join(", ", names);
    }
}
