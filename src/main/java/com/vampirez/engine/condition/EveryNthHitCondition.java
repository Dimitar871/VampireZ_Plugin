package com.vampirez.engine.condition;

import com.vampirez.engine.HookContext;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Passes on every Nth evaluation per owner (a rhythm gate: "every 5th hit
 * summons lightning"). The counter increments on each test and resets when it
 * fires. Per-instance state, like {@link CooldownCondition}.
 */
public class EveryNthHitCondition implements Condition {

    private final int n;
    private final Map<UUID, Integer> counters = new HashMap<>();

    public EveryNthHitCondition(int n) {
        this.n = Math.max(1, n);
    }

    @Override
    public boolean test(HookContext ctx) {
        Player owner = ctx.owner;
        if (owner == null) return false;
        UUID uuid = owner.getUniqueId();
        int count = counters.merge(uuid, 1, Integer::sum);
        if (count >= n) {
            counters.put(uuid, 0);
            return true;
        }
        return false;
    }
}
