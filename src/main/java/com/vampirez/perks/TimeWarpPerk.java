package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.*;

public class TimeWarpPerk extends Perk {

    private static final int BUFFER_SIZE = 3;
    private static final long COOLDOWN_MS = 90000;

    private final Map<UUID, LocationSnapshot[]> snapshots = new HashMap<>();
    private final Map<UUID, Integer> bufferIndex = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public TimeWarpPerk() {
        super("time_warp", "Time Warp", PerkTier.PRISMATIC, PerkTeam.HUMAN,
                Material.RECOVERY_COMPASS,
                "On fatal damage: rewind to position + HP",
                "from 3 seconds ago (90s cooldown)");
    }

    @Override
    public void apply(Player player) {
        snapshots.put(player.getUniqueId(), new LocationSnapshot[BUFFER_SIZE]);
        bufferIndex.put(player.getUniqueId(), 0);
    }

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        snapshots.remove(uuid);
        bufferIndex.remove(uuid);
        cooldowns.remove(uuid);
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        LocationSnapshot[] buffer = snapshots.get(uuid);
        if (buffer == null) return;

        int idx = bufferIndex.getOrDefault(uuid, 0);
        buffer[idx] = new LocationSnapshot(player.getLocation().clone(), player.getHealth());
        bufferIndex.put(uuid, (idx + 1) % BUFFER_SIZE);
    }

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        UUID uuid = victim.getUniqueId();
        double damageAfter = victim.getHealth() - event.getFinalDamage();
        if (damageAfter > 0) return; // Not fatal

        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(victim, COOLDOWN_MS)) return;

        LocationSnapshot[] buffer = snapshots.get(uuid);
        if (buffer == null) return;

        // Find oldest snapshot (3 ticks ago)
        int idx = bufferIndex.getOrDefault(uuid, 0);
        LocationSnapshot oldest = buffer[idx]; // oldest entry in ring buffer
        if (oldest == null) return;

        cooldowns.put(uuid, now);
        event.setCancelled(true);

        victim.teleport(oldest.location);
        victim.setHealth(Math.max(oldest.health, 2.0)); // At least 1 heart
        victim.getWorld().spawnParticle(Particle.PORTAL, victim.getLocation().add(0, 1, 0), 80, 0.5, 1, 0.5, 0.5);
        victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0), 40, 0.5, 1, 0.5, 0,
                new Particle.DustOptions(org.bukkit.Color.PURPLE, 1.5f));
        victim.playSound(victim.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 2.0f);
        victim.sendMessage(ChatColor.LIGHT_PURPLE + "Time Warp! Rewound 3 seconds!");
        incrementStat(uuid, "rewinds");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("rewinds", "Rewinds");
        return labels;
    }

    private static class LocationSnapshot {
        final Location location;
        final double health;

        LocationSnapshot(Location location, double health) {
            this.location = location;
            this.health = health;
        }
    }
}
