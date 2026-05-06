package com.vampirez.engine.action;

import com.vampirez.engine.HookContext;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

/**
 * Heals every player within {@code radius} blocks of the owner by {@code amount} HP, capped
 * at the recipient's max health. Filtered by scoreboard team membership when
 * {@code alliesOnly} is true. Used by group-heal perks (Martyr, Infectious Bite).
 */
public class AoeHealAction implements Action {

    private final double radius;
    private final boolean alliesOnly;
    private final boolean includeSelf;
    private final double amount;

    public AoeHealAction(double radius, boolean alliesOnly, boolean includeSelf, double amount) {
        this.radius = radius;
        this.alliesOnly = alliesOnly;
        this.includeSelf = includeSelf;
        this.amount = amount;
    }

    @Override
    public void run(HookContext ctx) {
        Player owner = ctx.owner;
        if (owner == null || amount <= 0) return;

        Team ownerTeam = alliesOnly ? owner.getScoreboard().getEntryTeam(owner.getName()) : null;
        if (alliesOnly && ownerTeam == null) return;

        for (Entity e : owner.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof Player nearby)) continue;
            if (!includeSelf && nearby.equals(owner)) continue;
            if (alliesOnly && !ownerTeam.hasEntry(nearby.getName())) continue;
            heal(nearby);
        }

        if (includeSelf) heal(owner);
    }

    private void heal(Player p) {
        double max = p.getAttribute(Attribute.MAX_HEALTH).getValue();
        p.setHealth(Math.min(p.getHealth() + amount, max));
    }
}
