package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GetExcitedPerk extends Perk {

    public GetExcitedPerk() {
        super("get_excited", "Get Excited", PerkTier.GOLD, PerkTeam.BOTH,
                Material.SUGAR, "On kill: Speed II + Strength I for 6s");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onKill(Player killer, Player victim) {
        killer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, 1, false, true)); // 6s
        killer.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 120, 0, false, true));
        // Excitement burst - firework-like particles
        killer.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, killer.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);
        killer.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, killer.getLocation().add(0, 1.5, 0), 15, 0.3, 0.5, 0.3, 0.2);
        killer.getWorld().spawnParticle(Particle.DUST, killer.getLocation().add(0, 2, 0), 20, 0.6, 0.6, 0.6, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 255, 0), 1.5f));
        killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f);
    }
}
