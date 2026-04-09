package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DeftPerk extends Perk {

    public DeftPerk() {
        super("deft", "Deft", PerkTier.SILVER, PerkTeam.BOTH,
                Material.FEATHER, "Permanent Speed I");
    }

    @Override
    public void apply(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 0, false, false));
    }

    @Override
    public void remove(Player player) {
        player.removePotionEffect(PotionEffectType.SPEED);
    }
}
