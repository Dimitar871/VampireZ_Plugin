package com.vampirez.engine.condition;

import com.vampirez.Perk;
import com.vampirez.engine.HookContext;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player cooldown gate. If the player is still on cooldown, returns false.
 * If not, stamps the current time and returns true — meaning "the cooldown
 * passed and this trigger is now consumed for the next baseMs milliseconds".
 *
 * Cooldown duration is reduced by the Haste perk via {@link Perk#getEffectiveCooldown}.
 */
public class CooldownCondition implements Condition {

    private final long baseCooldownMs;
    private final Map<UUID, Long> lastTrigger = new HashMap<>();

    public CooldownCondition(long baseCooldownMs) {
        this.baseCooldownMs = baseCooldownMs;
    }

    @Override
    public boolean test(HookContext ctx) {
        Player owner = ctx.owner;
        if (owner == null) return false;
        UUID uuid = owner.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastTrigger.get(uuid);
        long effective = Perk.getEffectiveCooldown(owner, baseCooldownMs);
        if (last != null && (now - last) < effective) {
            return false;
        }
        lastTrigger.put(uuid, now);
        return true;
    }
}
