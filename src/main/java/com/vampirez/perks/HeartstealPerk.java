package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.*;

public class HeartstealPerk extends Perk {

    // attacker UUID -> target UUID -> first-seen timestamp
    private final Map<UUID, Map<UUID, Long>> nearbyTracking = new HashMap<>();
    // attacker UUID -> target UUID -> last-proc timestamp
    private final Map<UUID, Map<UUID, Long>> targetCooldowns = new HashMap<>();

    private static final long PROXIMITY_TIME_MS = 5000;
    private static final long COOLDOWN_MS = 60000;
    private static final double PROXIMITY_RANGE = 10.0;

    // Tracks total HP gained per player so remove() can subtract the correct amount
    private final Map<UUID, Double> totalHpGained = new HashMap<>();

    public HeartstealPerk() {
        super("heartsteal", "Heartsteal", PerkTier.GOLD, PerkTeam.BOTH,
                Material.GOLDEN_APPLE,
                "Hit a player near you for 5+ seconds",
                "to permanently gain +0.5 hearts max HP.",
                "(60s cooldown per target)");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        nearbyTracking.remove(uuid);
        targetCooldowns.remove(uuid);
        // Subtract only the HP gained from this perk, not a hard reset to 20
        double gained = totalHpGained.getOrDefault(uuid, 0.0);
        if (gained > 0) {
            double current = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(Math.max(current - gained, 2.0));
            if (player.getHealth() > player.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
            }
        }
        totalHpGained.remove(uuid);
    }

    @Override
    public void onTick(Player player) {
        UUID attackerUUID = player.getUniqueId();
        long now = System.currentTimeMillis();

        Map<UUID, Long> tracking = nearbyTracking.computeIfAbsent(attackerUUID, k -> new HashMap<>());

        // Get currently nearby players
        Set<UUID> currentlyNearby = new HashSet<>();
        for (Entity entity : player.getNearbyEntities(PROXIMITY_RANGE, PROXIMITY_RANGE, PROXIMITY_RANGE)) {
            if (entity instanceof Player nearby && !nearby.getUniqueId().equals(attackerUUID)) {
                if (!isSameTeam(player, nearby)) {
                    currentlyNearby.add(nearby.getUniqueId());
                }
            }
        }

        // Add new nearby players
        for (UUID nearbyUUID : currentlyNearby) {
            tracking.putIfAbsent(nearbyUUID, now);
        }

        // Remove players no longer in range
        tracking.keySet().removeIf(uuid -> !currentlyNearby.contains(uuid));
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player target)) return;

        UUID attackerUUID = attacker.getUniqueId();
        UUID targetUUID = target.getUniqueId();
        long now = System.currentTimeMillis();

        // Check proximity time
        Map<UUID, Long> tracking = nearbyTracking.get(attackerUUID);
        if (tracking == null) return;
        Long firstSeen = tracking.get(targetUUID);
        if (firstSeen == null || (now - firstSeen) < PROXIMITY_TIME_MS) return;

        // Check cooldown
        Map<UUID, Long> cooldowns = targetCooldowns.computeIfAbsent(attackerUUID, k -> new HashMap<>());
        Long lastProc = cooldowns.get(targetUUID);
        if (lastProc != null && (now - lastProc) < getEffectiveCooldown(attacker, COOLDOWN_MS)) return;

        // Proc: gain +1 max health (half a heart)
        cooldowns.put(targetUUID, now);
        double currentMax = attacker.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
        double newMax = currentMax + 1.0;
        attacker.getAttribute(Attribute.MAX_HEALTH).setBaseValue(newMax);
        attacker.setHealth(Math.min(attacker.getHealth() + 1.0, newMax));
        totalHpGained.merge(attackerUUID, 1.0, Double::sum);

        attacker.playSound(attacker.getLocation(), Sound.ENTITY_WITCH_DRINK, 0.5f, 1.5f);
        // Particles and feedback
        attacker.getWorld().spawnParticle(Particle.HEART, attacker.getLocation().add(0, 2, 0), 8, 0.4, 0.4, 0.4, 0);
        attacker.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, attacker.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0);
        attacker.sendMessage(ChatColor.GREEN + "Heartsteal! Gained +0.5 hearts max HP from " + target.getName());

        incrementStat(attackerUUID, "hearts_gained");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("hearts_gained", "Hearts Gained");
        return labels;
    }
}
