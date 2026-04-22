package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class NinjaTurtlePerk extends Perk {

    private static final int Y_THRESHOLD = 20;

    public NinjaTurtlePerk() {
        super("ninja_turtle", "Ninja Turtle", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.TURTLE_HELMET,
                "Below y=20: Resistance II + Speed II.",
                "If also in water: Strength I.");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.STRENGTH);
    }

    @Override
    public void onTick(Player player) {
        if (player.getLocation().getBlockY() >= Y_THRESHOLD) {
            player.removePotionEffect(PotionEffectType.RESISTANCE);
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.STRENGTH);
            return;
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 1, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false, true));

        if (player.isInWater()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0, false, false, true));
        } else {
            player.removePotionEffect(PotionEffectType.STRENGTH);
        }
    }
}
