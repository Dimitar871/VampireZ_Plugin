package com.vampirez.discord;

import com.vampirez.GameState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure-builder tests — no Bukkit/JDA needed. Focus on the static helpers since
 * the {@link DiscordEmbedFactory} instance methods need a {@code VampireZPlugin}
 * to read config colors (covered indirectly via the integration smoke test).
 */
class DiscordEmbedFactoryTest {

    @Test
    void formatMmSs_zero() {
        assertEquals("0:00", DiscordEmbedFactory.formatMmSs(0));
    }

    @Test
    void formatMmSs_belowOneMinute() {
        assertEquals("0:05", DiscordEmbedFactory.formatMmSs(5));
        assertEquals("0:59", DiscordEmbedFactory.formatMmSs(59));
    }

    @Test
    void formatMmSs_exactlyOneMinute() {
        assertEquals("1:00", DiscordEmbedFactory.formatMmSs(60));
    }

    @Test
    void formatMmSs_overOneHour() {
        // Display rolls over to >60 minutes — this is fine for our use case (max game ~25min).
        assertEquals("60:00", DiscordEmbedFactory.formatMmSs(3600));
        assertEquals("25:00", DiscordEmbedFactory.formatMmSs(1500));
    }

    @Test
    void formatMmSs_negativeClampsToZero() {
        assertEquals("0:00", DiscordEmbedFactory.formatMmSs(-7));
    }

    @Test
    void labelFor_allStates() {
        assertEquals("Lobby",    DiscordEmbedFactory.labelFor(GameState.LOBBY));
        assertEquals("Starting", DiscordEmbedFactory.labelFor(GameState.STARTING));
        assertEquals("Active",   DiscordEmbedFactory.labelFor(GameState.ACTIVE));
        assertEquals("Ending",   DiscordEmbedFactory.labelFor(GameState.ENDING));
    }

    @Test
    void parseHex_validHex() {
        assertEquals(0x5865F2, DiscordEmbedFactory.parseHex("5865F2"));
        assertEquals(0xFF0000, DiscordEmbedFactory.parseHex("FF0000"));
    }

    @Test
    void parseHex_stripsHash() {
        assertEquals(0x123456, DiscordEmbedFactory.parseHex("#123456"));
    }

    @Test
    void parseHex_invalidFallsBackToGray() {
        assertEquals(0x808080, DiscordEmbedFactory.parseHex("not-hex"));
        assertEquals(0x808080, DiscordEmbedFactory.parseHex(""));
        assertEquals(0x808080, DiscordEmbedFactory.parseHex(null));
    }
}
