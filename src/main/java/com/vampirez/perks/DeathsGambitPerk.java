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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class DeathsGambitPerk extends Perk {

    private final Set<UUID> used = new HashSet<>();
    private final Random random = new Random();

    public DeathsGambitPerk() {
        super("deaths_gambit", "Death's Gambit", PerkTier.PRISMATIC, PerkTeam.BOTH,
                Material.SOUL_LANTERN,
                "Fatal damage: 50% survive at 1 HP + 3s invuln",
                "50% take 50% MORE damage. Once per life.");
    }

    @Override
    public void apply(Player player) {
        used.remove(player.getUniqueId());
    }

    @Override
    public void remove(Player player) {
        used.remove(player.getUniqueId());
    }

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        UUID uuid = victim.getUniqueId();
        if (used.contains(uuid)) return;

        double damageAfter = victim.getHealth() - event.getFinalDamage();
        if (damageAfter > 0) return; // Not fatal

        used.add(uuid);

        if (random.nextBoolean()) {
            // Lucky - survive!
            event.setCancelled(true);
            victim.setHealth(2.0); // 1 heart
            victim.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 254, false, true)); // near invuln
            victim.getWorld().spawnParticle(Particle.SOUL, victim.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.05);
            victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0), 40, 0.5, 0.5, 0.5, 0,
                    new Particle.DustOptions(org.bukkit.Color.LIME, 1.5f));
            victim.playSound(victim.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 0.5f);
            victim.sendMessage(ChatColor.GREEN + "Death's Gambit: LUCKY! You cheat death!");
            incrementStat(uuid, "lucky");
        } else {
            // Unlucky - extra damage
            event.setDamage(event.getDamage() * 1.5);
            victim.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, victim.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.05);
            victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0), 40, 0.5, 0.5, 0.5, 0,
                    new Particle.DustOptions(org.bukkit.Color.RED, 1.5f));
            victim.playSound(victim.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 0.5f);
            victim.sendMessage(ChatColor.RED + "Death's Gambit: UNLUCKY! Extra damage!");
            incrementStat(uuid, "unlucky");
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("lucky", "Lucky Saves");
        labels.put("unlucky", "Unlucky Hits");
        return labels;
    }
}
