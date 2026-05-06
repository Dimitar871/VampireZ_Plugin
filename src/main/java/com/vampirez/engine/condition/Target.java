package com.vampirez.engine.condition;

import com.vampirez.engine.HookContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Selector for which entity in a HookContext a condition or action operates on.
 * "self" is an alias for "owner" — the player who owns this perk in this hook.
 */
public enum Target {
    OWNER,
    SELF,
    ATTACKER,
    VICTIM;

    public static Target fromYaml(String s) {
        if (s == null) return OWNER;
        return Target.valueOf(s.toUpperCase());
    }

    /**
     * @return Player resolved from the context, or null if not present.
     */
    public Player resolvePlayer(HookContext ctx) {
        switch (this) {
            case OWNER:
            case SELF:
                return ctx.owner;
            case ATTACKER:
                return ctx.attacker;
            case VICTIM:
                return ctx.victimPlayer;
        }
        return null;
    }

    /**
     * @return LivingEntity (player or mob) resolved from the context, or null if not present.
     */
    public LivingEntity resolveLivingEntity(HookContext ctx) {
        switch (this) {
            case OWNER:
            case SELF:
                return ctx.owner;
            case ATTACKER:
                return ctx.attacker;
            case VICTIM:
                if (ctx.victimPlayer != null) return ctx.victimPlayer;
                if (ctx.victim instanceof LivingEntity) return (LivingEntity) ctx.victim;
                return null;
        }
        return null;
    }
}
