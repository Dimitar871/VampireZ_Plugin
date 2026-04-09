package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedHashMap;
import java.util.Map;

public class FrostBitePerk extends Perk {

    public FrostBitePerk() {
        super("frost_bite", "Frost Bite", PerkTier.GOLD, PerkTeam.VAMPIRE,
                Material.BLUE_ICE,
                "Attacks apply Slowness I for 3s",
                "and Frost Walker enchant on boots");
    }

    @Override
    public void apply(Player player) {
        // Add Frost Walker to boots if wearing any
        if (player.getInventory().getBoots() != null) {
            player.getInventory().getBoots().addUnsafeEnchantment(
                    org.bukkit.enchantments.Enchantment.FROST_WALKER, 2);
        }
    }

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (victim instanceof LivingEntity target) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, true));
            victim.getWorld().spawnParticle(Particle.SNOWFLAKE, victim.getLocation().add(0, 1, 0), 30, 0.3, 0.3, 0.3, 0.02);
            victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0,
                    new Particle.DustOptions(Color.AQUA, 1.5f));
            attacker.playSound(attacker.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.5f, 1.8f);
            incrementStat(attacker.getUniqueId(), "slows_applied");
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("slows_applied", "Slows Applied");
        return labels;
    }
}
