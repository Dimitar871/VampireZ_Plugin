package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class PlagueCarrierPerk extends Perk {

    private static final double SPREAD_RADIUS = 4.0;
    private static final Set<PotionEffectType> NEGATIVE_EFFECTS = new HashSet<>(Arrays.asList(
            PotionEffectType.POISON, PotionEffectType.SLOWNESS, PotionEffectType.WEAKNESS,
            PotionEffectType.WITHER, PotionEffectType.BLINDNESS, PotionEffectType.HUNGER,
            PotionEffectType.MINING_FATIGUE
    ));

    public PlagueCarrierPerk() {
        super("plague_carrier", "Plague Carrier", PerkTier.GOLD, PerkTeam.VAMPIRE,
                Material.FERMENTED_SPIDER_EYE,
                "Damage spreads victim's negative",
                "effects to enemies within 4 blocks.");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player target)) return;

        // Collect victim's negative effects
        boolean spread = false;
        for (PotionEffect effect : target.getActivePotionEffects()) {
            if (!NEGATIVE_EFFECTS.contains(effect.getType())) continue;

            // Spread to nearby enemies of the attacker
            for (Entity entity : target.getNearbyEntities(SPREAD_RADIUS, SPREAD_RADIUS, SPREAD_RADIUS)) {
                if (!(entity instanceof Player nearby)) continue;
                if (nearby.getUniqueId().equals(attacker.getUniqueId())) continue;
                if (nearby.getUniqueId().equals(target.getUniqueId())) continue;
                if (isSameTeam(attacker, nearby)) continue;

                nearby.addPotionEffect(new PotionEffect(effect.getType(), effect.getDuration(), effect.getAmplifier(), false, true), true);
                nearby.getWorld().spawnParticle(Particle.DUST, nearby.getLocation().add(0, 1, 0),
                        8, 0.3, 0.3, 0.3, 0, new Particle.DustOptions(Color.fromRGB(50, 120, 0), 1.2f));
                spread = true;
            }
        }

        if (spread) {
            target.getWorld().spawnParticle(Particle.WITCH, target.getLocation().add(0, 1, 0), 15, 1.5, 0.5, 1.5, 0);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.5f);
            attacker.sendMessage(ChatColor.DARK_GREEN + "Plague spreads from your target!");
            incrementStat(attacker.getUniqueId(), "spreads");
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("spreads", "Plague Spreads");
        return labels;
    }
}
