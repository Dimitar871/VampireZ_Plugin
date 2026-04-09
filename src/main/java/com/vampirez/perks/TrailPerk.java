package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class TrailPerk extends Perk {

    private static final int MAX_TRAIL_LENGTH = 25;
    private static final long TRAIL_FADE_MS = 5000; // 5 seconds
    private static final double TRAIL_CHECK_RADIUS = 2.5;

    // Per-player trail: ordered deque of trail points with timestamps
    private static final Map<UUID, Deque<TrailPoint>> playerTrails = new HashMap<>();
    private final Map<UUID, BukkitTask> particleTasks = new HashMap<>();

    public TrailPerk() {
        super("trail", "Trail", PerkTier.SILVER, PerkTeam.BOTH,
                Material.SOUL_TORCH,
                "Sprinting leaves a 25-block trail.",
                "Enemies on the trail get Slowness I.",
                "Teammates on the trail get Speed I.",
                "Trail fades after 5 seconds.");
    }

    @Override
    public void apply(Player player) {
        UUID uuid = player.getUniqueId();
        playerTrails.put(uuid, new ConcurrentLinkedDeque<>());

        // Particle rendering task — runs every 5 ticks (4x/sec) for smooth visuals
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(getPlugin(), () -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) return;
            Deque<TrailPoint> trail = playerTrails.get(uuid);
            if (trail == null || trail.isEmpty()) return;

            long now = System.currentTimeMillis();
            for (TrailPoint point : trail) {
                float fade = 1.0f - ((float)(now - point.timestamp) / TRAIL_FADE_MS);
                if (fade <= 0) continue;
                int particleCount = Math.max(3, (int)(10 * fade));
                float size = Math.max(0.5f, 1.5f * fade);
                p.getWorld().spawnParticle(Particle.DUST,
                        point.location.clone().add(0, 0.3, 0), particleCount,
                        1.0, 0.15, 1.0, 0,
                        new Particle.DustOptions(org.bukkit.Color.fromRGB(100, 200, 255), size));
                if (fade > 0.3f) {
                    p.getWorld().spawnParticle(Particle.END_ROD,
                            point.location.clone().add(0, 0.5, 0), 2,
                            0.5, 0.1, 0.5, 0.005);
                }
            }
        }, 5L, 5L);
        particleTasks.put(uuid, task);
    }

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        playerTrails.remove(uuid);
        BukkitTask task = particleTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        Deque<TrailPoint> trail = playerTrails.get(uuid);
        if (trail == null) return;

        long now = System.currentTimeMillis();

        // Remove expired trail points
        while (!trail.isEmpty() && now - trail.peekFirst().timestamp > TRAIL_FADE_MS) {
            trail.pollFirst();
        }

        // Add new trail point if sprinting and moved far enough from last point
        if (player.isSprinting()) {
            Location loc = player.getLocation().getBlock().getLocation().add(0.5, 0.1, 0.5);
            boolean shouldAdd = true;

            if (!trail.isEmpty()) {
                Location lastLoc = trail.peekLast().location;
                if (lastLoc.getWorld().equals(loc.getWorld()) && lastLoc.distanceSquared(loc) < 1.0) {
                    shouldAdd = false; // Too close to last point
                }
            }

            if (shouldAdd) {
                trail.addLast(new TrailPoint(loc, now));
                // Trim to max length
                while (trail.size() > MAX_TRAIL_LENGTH) {
                    trail.pollFirst();
                }
            }
        }

        // Apply effects to nearby players walking on the trail
        if (trail.isEmpty()) return;

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.getUniqueId().equals(uuid)) continue;
            if (!other.getWorld().equals(player.getWorld())) continue;

            Location otherLoc = other.getLocation();
            boolean onTrail = false;

            for (TrailPoint point : trail) {
                // Horizontal-only (XZ) distance so height differences don't reduce effective width
                double dx = point.location.getX() - otherLoc.getX();
                double dz = point.location.getZ() - otherLoc.getZ();
                if (dx * dx + dz * dz <= TRAIL_CHECK_RADIUS * TRAIL_CHECK_RADIUS) {
                    onTrail = true;
                    break;
                }
            }

            if (onTrail) {
                if (isSameTeam(player, other)) {
                    // Teammate: Speed I for 2 seconds (refreshes while on trail)
                    other.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, true, false, true), true);
                } else {
                    // Enemy: Slowness I for 2 seconds (refreshes while on trail)
                    other.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, true, false, true), true);
                    incrementStat(uuid, "enemies_slowed");
                }
            }
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("enemies_slowed", "Enemies Slowed");
        return labels;
    }

    private static class TrailPoint {
        final Location location;
        final long timestamp;

        TrailPoint(Location location, long timestamp) {
            this.location = location;
            this.timestamp = timestamp;
        }
    }
}
