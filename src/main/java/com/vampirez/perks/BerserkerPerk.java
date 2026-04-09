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

public class BerserkerPerk extends Perk {

    private final Set<UUID> activeState = new HashSet<>();

    public BerserkerPerk() {
        super("berserker", "Berserker", PerkTier.GOLD, PerkTeam.BOTH,
                Material.BLAZE_POWDER,
                "Below 30% HP: Strength I + Speed I",
                "Above 30% HP: effects removed");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.SPEED);
        activeState.remove(player.getUniqueId());
    }

    @Override
    public void onTick(Player player) {
        if (player.getHealth() < player.getMaxHealth() * 0.3) {
            if (activeState.add(player.getUniqueId())) {
                player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 1.0f);
            }
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 30, 0, false, true), true);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30, 0, false, true), true);
            player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0.01);
        } else {
            activeState.remove(player.getUniqueId());
        }
    }
}
