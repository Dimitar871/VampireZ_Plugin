package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PowerShotPerk extends Perk {

    public PowerShotPerk() {
        super("power_shot", "Power Shot", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.BOW,
                "Your bow gets",
                "Power I");
    }

    @Override
    public void apply(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.BOW) {
                // Only upgrade, never downgrade (Galeforce bow may already have higher Power)
                if (item.getEnchantmentLevel(Enchantment.POWER) < 1) {
                    item.addUnsafeEnchantment(Enchantment.POWER, 1);
                }
                return;
            }
        }
    }

    @Override
    public void remove(Player player) {}
}
