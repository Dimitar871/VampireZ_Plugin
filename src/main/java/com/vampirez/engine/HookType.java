package com.vampirez.engine;

public enum HookType {
    APPLY,
    REMOVE,
    ON_DAMAGE_DEALT,
    ON_DAMAGE_TAKEN,
    ON_KILL,
    ON_DEATH,
    ON_TICK,
    ON_HEALTH_REGAIN,
    ON_INTERACT,
    ON_RESPAWN,
    NEGATES_FALL_DAMAGE;

    public static HookType fromYamlKey(String key) {
        return HookType.valueOf(key.toUpperCase());
    }
}
