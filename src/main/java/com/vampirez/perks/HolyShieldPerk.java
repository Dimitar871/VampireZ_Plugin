package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.*;

public class HolyShieldPerk extends Perk {

    private final Map<UUID, Double> accumulated = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 20_000; // 20 seconds

    public HolyShieldPerk() {
        super("holy_shield", "Holy Shield", PerkTier.PRISMATIC, PerkTeam.HUMAN,
                Material.GOLDEN_CHESTPLATE,
                "Absorb damage. At 10 accumulated damage,",
                "auto-explode for 4 hearts AoE (5 blocks)",
                "20 second cooldown between explosions");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        accumulated.remove(player.getUniqueId());
        cooldowns.remove(player.getUniqueId());
    }

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        UUID uuid = victim.getUniqueId();
        double total = accumulated.getOrDefault(uuid, 0.0) + event.getDamage();

        if (total >= 10.0) {
            long now = System.currentTimeMillis();
            long lastUse = cooldowns.getOrDefault(uuid, 0L);
            if (now - lastUse < getEffectiveCooldown(victim, COOLDOWN_MS)) {
                // On cooldown - keep accumulated at threshold so it triggers immediately when ready
                accumulated.put(uuid, 10.0);
                long remaining = (getEffectiveCooldown(victim, COOLDOWN_MS) - (now - lastUse)) / 1000;
                victim.sendMessage(ChatColor.GOLD + "Holy Shield on cooldown! " + remaining + "s remaining");
                return;
            }
            accumulated.put(uuid, 0.0);
            cooldowns.put(uuid, now);

            // Explode
            victim.getWorld().spawnParticle(Particle.EXPLOSION, victim.getLocation().add(0, 1, 0), 5, 1, 0.5, 1, 0);
            victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0), 40, 2, 1, 2, 0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 215, 0), 2.0f));
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);

            int hits = 0;
            double totalDamage = 0;
            for (Entity entity : victim.getNearbyEntities(5, 5, 5)) {
                if (!(entity instanceof Player target)) continue;
                if (isSameTeam(victim, target)) continue;

                target.damage(8.0, victim);
                Vector knockback = target.getLocation().toVector().subtract(victim.getLocation().toVector()).normalize().multiply(0.6).setY(0.3);
                target.setVelocity(knockback);
                target.sendMessage(ChatColor.GOLD + "Holy Shield explosion!");
                hits++;
                totalDamage += 8.0;
            }

            incrementStat(uuid, "explosions");
            addStat(uuid, "aoe_damage", totalDamage);
            victim.sendMessage(ChatColor.GOLD + "Holy Shield detonated! Hit " + hits + " enemies!");
        } else {
            accumulated.put(uuid, total);
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("explosions", "Explosions");
        labels.put("aoe_damage", "AoE Damage");
        return labels;
    }
}
