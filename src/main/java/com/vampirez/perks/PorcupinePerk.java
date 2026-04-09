package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class PorcupinePerk extends Perk {

    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private final Map<UUID, Integer> stationaryTicks = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 5000;
    private static final int TICKS_TO_FORTIFY = 2;

    public PorcupinePerk() {
        super("porcupine", "Porcupine", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.CACTUS,
                "Stand still for 2s: attackers take",
                "2 hearts + Slowness I 2s (5s cd).");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        lastLocations.remove(uuid);
        stationaryTicks.remove(uuid);
        cooldowns.remove(uuid);
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        Location current = player.getLocation();
        Location last = lastLocations.get(uuid);

        if (last != null && last.getWorld().equals(current.getWorld())
                && last.distanceSquared(current) < 0.01) {
            int ticks = stationaryTicks.getOrDefault(uuid, 0) + 1;
            stationaryTicks.put(uuid, ticks);

            if (ticks >= TICKS_TO_FORTIFY) {
                // Fortified - show particles
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0),
                        8, 0.5, 0.5, 0.5, 0, new Particle.DustOptions(Color.GREEN, 1.2f));
            }
        } else {
            stationaryTicks.put(uuid, 0);
        }

        lastLocations.put(uuid, current.clone());
    }

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        if (!(attacker instanceof Player attackerPlayer)) return;

        UUID uuid = victim.getUniqueId();
        int ticks = stationaryTicks.getOrDefault(uuid, 0);
        if (ticks < TICKS_TO_FORTIFY) return;

        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(victim, COOLDOWN_MS)) return;

        cooldowns.put(uuid, now);

        // Deal 2 hearts (4 HP) back to attacker
        attackerPlayer.damage(4.0);
        attackerPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, true), true);

        victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0),
                15, 0.5, 0.5, 0.5, 0, new Particle.DustOptions(Color.fromRGB(0, 180, 0), 1.5f));
        victim.playSound(victim.getLocation(), Sound.ENTITY_GUARDIAN_HURT, 1.0f, 1.5f);
        attackerPlayer.sendMessage(ChatColor.GREEN + "Porcupine thorns hit you!");
        victim.sendMessage(ChatColor.GREEN + "Porcupine! Reflected damage!");
        incrementStat(uuid, "procs");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("procs", "Procs");
        return labels;
    }
}
