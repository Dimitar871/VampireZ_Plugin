package com.vampirez.engine.condition;

import com.vampirez.engine.HookContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

/**
 * True if at least one OTHER player on the same scoreboard team as the owner is within
 * {@code radius} blocks. Same logic as the existing aura perks (BuffBuddies, GuardiansOath).
 */
public class AllyWithinRangeCondition implements Condition {

    private final double radius;

    public AllyWithinRangeCondition(double radius) {
        this.radius = radius;
    }

    @Override
    public boolean test(HookContext ctx) {
        Player owner = ctx.owner;
        if (owner == null) return false;
        Team ownerTeam = owner.getScoreboard().getEntryTeam(owner.getName());
        if (ownerTeam == null) return false;
        for (Entity e : owner.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof Player p) || p.equals(owner)) continue;
            if (ownerTeam.hasEntry(p.getName())) return true;
        }
        return false;
    }
}
