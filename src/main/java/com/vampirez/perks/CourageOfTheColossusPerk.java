package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CourageOfTheColossusPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 30000; // 30 seconds

    public CourageOfTheColossusPerk() {
        super("courage_colossus", "Courage of the Colossus", PerkTier.PRISMATIC, PerkTeam.HUMAN,
                Material.SHIELD,
                "Hit a player to gain 2 hearts absorption",
                "30 second cooldown");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        cooldowns.remove(player.getUniqueId());
        player.removePotionEffect(PotionEffectType.ABSORPTION);
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player)) return;

        UUID uuid = attacker.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(attacker, COOLDOWN_MS)) return;

        cooldowns.put(uuid, now);

        // Absorption I = 2 hearts, lasts 30 seconds (600 ticks)
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 600, 0, false, true), true);
        attacker.getWorld().spawnParticle(Particle.END_ROD, attacker.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);

        attacker.playSound(attacker.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 0.5f, 1.5f);
        incrementStat(uuid, "activations");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new java.util.LinkedHashMap<>();
        labels.put("activations", "Times Activated");
        return labels;
    }
}
