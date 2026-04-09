package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FinalFormPerk extends Perk {

    private final Map<UUID, Integer> tickCounters = new HashMap<>();
    private final Set<UUID> activeBoost = new HashSet<>();
    private static final int COOLDOWN_TICKS = 60; // 60 seconds
    private static final int DURATION_TICKS = 8; // 8 seconds

    public FinalFormPerk() {
        super("final_form", "Final Form", PerkTier.PRISMATIC, PerkTeam.VAMPIRE,
                Material.NETHER_STAR,
                "Every 60s: Absorption IV + 25% lifesteal",
                "for 8 seconds");
    }

    @Override
    public void apply(Player player) {
        tickCounters.put(player.getUniqueId(), 0);
    }

    @Override
    public void remove(Player player) {
        tickCounters.remove(player.getUniqueId());
        activeBoost.remove(player.getUniqueId());
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        int count = tickCounters.getOrDefault(uuid, 0) + 1;
        tickCounters.put(uuid, count);

        if (count == COOLDOWN_TICKS) {
            // Activate Final Form - massive transformation burst
            activeBoost.add(uuid);
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, DURATION_TICKS * 20, 3, false, true));
            player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation().add(0, 1, 0), 60, 0.8, 1.5, 0.8, 0.08);
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 40, 1.0, 1.5, 1.0, 0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(100, 0, 150), 2.5f));
            player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 25, 0.5, 1, 0.5, 0.05);
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 2.0f);
        }

        // Persistent dark aura while active
        if (activeBoost.contains(uuid)) {
            double angle = (count % 10) * 0.628; // rotating ring
            double radius = 0.8;
            double ox = Math.cos(angle) * radius;
            double oz = Math.sin(angle) * radius;
            player.getWorld().spawnParticle(Particle.DRAGON_BREATH,
                    player.getLocation().add(ox, 1.2, oz), 3, 0.05, 0.1, 0.05, 0.01);
            player.getWorld().spawnParticle(Particle.DUST,
                    player.getLocation().add(-ox, 0.8, -oz), 2, 0.05, 0.1, 0.05, 0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(80, 0, 120), 1.2f));
        }

        if (count == COOLDOWN_TICKS + DURATION_TICKS) {
            // Deactivate
            activeBoost.remove(uuid);
            tickCounters.put(uuid, 0);
        }
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (activeBoost.contains(attacker.getUniqueId())) {
            // 25% lifesteal during active period
            double healAmount = event.getDamage() * 0.25;
            double newHealth = Math.min(attacker.getHealth() + healAmount, attacker.getMaxHealth());
            attacker.setHealth(newHealth);
        }
    }
}
