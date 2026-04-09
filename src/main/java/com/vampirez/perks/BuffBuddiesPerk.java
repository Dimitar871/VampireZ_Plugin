package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BuffBuddiesPerk extends Perk {

    public BuffBuddiesPerk() {
        super("buff_buddies", "Buff Buddies", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.BEACON, "Allies within 10 blocks get Resistance I");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onTick(Player player) {
        for (Player nearby : player.getWorld().getPlayers()) {
            if (nearby.getUniqueId().equals(player.getUniqueId())) continue;
            if (nearby.getLocation().distance(player.getLocation()) <= 10.0) {
                nearby.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 0, false, false), true);
            }
        }
    }
}
