package com.vampirez.discord;

import com.vampirez.GameState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Table-driven tests for the bot presence string across every {@link GameState}.
 * Lives at the same package as the SUT so we can test the package-private static helper
 * without exposing it on the public surface.
 */
class PresenceTextTest {

    @Test
    void lobby_showsJoinedOverMin() {
        // 7 of 10 joined, no one in teams yet, full game time
        String text = DiscordStatusUpdater.presenceText(
                GameState.LOBBY, 7, 10, 0, 0, 1500);
        assertEquals("VampireZ: lobby 7/10", text);
    }

    @Test
    void lobby_emptyServer() {
        String text = DiscordStatusUpdater.presenceText(
                GameState.LOBBY, 0, 10, 0, 0, 1500);
        assertEquals("VampireZ: lobby 0/10", text);
    }

    @Test
    void starting_showsBareMessage() {
        String text = DiscordStatusUpdater.presenceText(
                GameState.STARTING, 12, 10, 0, 0, 1500);
        assertTrue(text.startsWith("VampireZ: starting"), text);
    }

    @Test
    void active_showsTeamCounts() {
        String text = DiscordStatusUpdater.presenceText(
                GameState.ACTIVE, 15, 10, 12, 3, 900);
        assertEquals("In game: 12H vs 3V", text);
    }

    @Test
    void active_handlesZeroHumans() {
        // Edge: vampires just won, humans=0 — we still produce a valid string.
        String text = DiscordStatusUpdater.presenceText(
                GameState.ACTIVE, 15, 10, 0, 15, 0);
        assertEquals("In game: 0H vs 15V", text);
    }

    @Test
    void ending_showsRoundEnding() {
        String text = DiscordStatusUpdater.presenceText(
                GameState.ENDING, 15, 10, 8, 7, 0);
        assertEquals("VampireZ: round ending", text);
    }

    @Test
    void allStatesProduceNonEmptyText() {
        for (GameState s : GameState.values()) {
            String text = DiscordStatusUpdater.presenceText(s, 5, 10, 3, 2, 600);
            assertTrue(text != null && !text.isBlank(), "empty for " + s);
        }
    }
}
