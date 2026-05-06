package com.vampirez.engine.action;

import com.vampirez.engine.HookContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;

/**
 * Applies a {@link PotionEffect} to every player within {@code radius} blocks of the owner,
 * filtered by team membership when {@code alliesOnly} is true. Used by aura-style perks
 * (Buff Buddies, Rally Cry-style team buffs).
 */
public class ApplyPotionAuraAction implements Action {

    private final double radius;
    private final boolean alliesOnly;
    private final boolean includeSelf;
    private final PotionEffectType type;
    private final int durationTicks;
    private final int amplifier;
    private final boolean ambient;
    private final boolean particles;

    public ApplyPotionAuraAction(double radius, boolean alliesOnly, boolean includeSelf,
                                 PotionEffectType type, int durationTicks, int amplifier,
                                 boolean ambient, boolean particles) {
        this.radius = radius;
        this.alliesOnly = alliesOnly;
        this.includeSelf = includeSelf;
        this.type = type;
        this.durationTicks = durationTicks;
        this.amplifier = amplifier;
        this.ambient = ambient;
        this.particles = particles;
    }

    @Override
    public void run(HookContext ctx) {
        Player owner = ctx.owner;
        if (owner == null || type == null) return;

        Team ownerTeam = alliesOnly ? owner.getScoreboard().getEntryTeam(owner.getName()) : null;
        if (alliesOnly && ownerTeam == null) return;

        for (Entity e : owner.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof Player nearby)) continue;
            if (!includeSelf && nearby.equals(owner)) continue;
            if (alliesOnly && !ownerTeam.hasEntry(nearby.getName())) continue;
            nearby.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, ambient, particles), true);
        }

        if (includeSelf) {
            owner.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, ambient, particles), true);
        }
    }
}
