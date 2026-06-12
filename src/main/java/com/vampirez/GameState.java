package com.vampirez;

public enum GameState {
    LOBBY,
    STARTING,
    ACTIVE,
    ENDING;

    /**
     * Whether perk grants (free picks, timed picks, conversion picks, auto-assigns)
     * are allowed in this state. Perks added in LOBBY/ENDING leak into the next game
     * — every perk-adding UI path must check this guard (see BUG_PERK_PERSISTENCE.md).
     */
    public boolean allowsPerkSelection() {
        return this == ACTIVE || this == STARTING;
    }
}
