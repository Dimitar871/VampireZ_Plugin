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

import java.util.LinkedHashMap;
import java.util.Map;

public class AdrenalineRushPerk extends Perk {

    public AdrenalineRushPerk() {
        super("adrenaline_rush", "Adrenaline Rush", PerkTier.SILVER, PerkTeam.BOTH,
                Material.SUGAR,
                "Taking damage grants",
                "Speed I for 3 seconds");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, false, true), true);
        victim.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_BREATH, 0.6f, 0.5f);
        // Yellow speed burst particles
        victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 0.5, 0), 15, 0.4, 0.1, 0.4, 0,
                new Particle.DustOptions(org.bukkit.Color.YELLOW, 1.0f));
        victim.getWorld().spawnParticle(Particle.CLOUD, victim.getLocation(), 8, 0.3, 0.1, 0.3, 0.03);
        incrementStat(victim.getUniqueId(), "procs");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("procs", "Procs");
        return labels;
    }
}
