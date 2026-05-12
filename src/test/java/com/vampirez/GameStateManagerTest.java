package com.vampirez;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameStateManagerTest {

    private GameStateManager sm;

    @BeforeEach
    void setUp() {
        sm = new GameStateManager();
    }

    @Test
    void freshInstance_startsInLobby() {
        assertEquals(GameState.LOBBY, sm.getState());
        assertTrue(sm.isLobby());
        assertFalse(sm.isStarting());
        assertFalse(sm.isActive());
        assertFalse(sm.isEnding());
    }

    @Test
    void setState_updatesAllPredicates() {
        sm.setState(GameState.STARTING);
        assertTrue(sm.isStarting());
        assertFalse(sm.isLobby());

        sm.setState(GameState.ACTIVE);
        assertTrue(sm.isActive());
        assertFalse(sm.isStarting());

        sm.setState(GameState.ENDING);
        assertTrue(sm.isEnding());
        assertFalse(sm.isActive());

        sm.setState(GameState.LOBBY);
        assertTrue(sm.isLobby());
        assertFalse(sm.isEnding());
    }

    @Test
    void lastStartForced_defaultsFalse_persistsAcrossStateChanges() {
        assertFalse(sm.isLastStartForced());

        sm.setLastStartForced(true);
        assertTrue(sm.isLastStartForced());

        // State transition shouldn't reset the flag — it's set once per startGame call.
        sm.setState(GameState.ACTIVE);
        assertTrue(sm.isLastStartForced());

        sm.setLastStartForced(false);
        assertFalse(sm.isLastStartForced());
    }
}
