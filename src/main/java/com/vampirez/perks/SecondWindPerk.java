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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SecondWindPerk extends Perk {

    private final Set<UUID> activeState = new HashSet<>();

    public SecondWindPerk() {
        super("second_wind", "Second Wind", PerkTier.SILVER, PerkTeam.BOTH,
                Material.GHAST_TEAR, "Regeneration I when below 50% HP");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        activeState.remove(player.getUniqueId());
    }

    @Override
    public void onTick(Player player) {
        if (player.getHealth() < player.getMaxHealth() * 0.5) {
            if (activeState.add(player.getUniqueId())) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BREATH, 0.5f, 1.0f);
            }
            // Use 100-tick duration so Regen I's 50-tick heal timer can fire.
            // Only refresh when remaining < 30 to avoid resetting the internal timer.
            PotionEffect existing = player.getPotionEffect(PotionEffectType.REGENERATION);
            if (existing == null || existing.getDuration() < 30) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, false, true), true);
            }
            // Swirling green healing wind particles
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1.2, 0), 12, 0.5, 0.6, 0.5, 0,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(50, 200, 50), 1.0f));
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 3, 0.4, 0.4, 0.4, 0);
        } else {
            activeState.remove(player.getUniqueId());
        }
    }
}
