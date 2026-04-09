package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class FlameArrowsPerk extends Perk {

    public FlameArrowsPerk() {
        super("flame_arrows", "Flame Arrows", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.SPECTRAL_ARROW,
                "Your bow gets",
                "Flame I enchantment");
    }

    @Override
    public void apply(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.BOW) {
                item.addUnsafeEnchantment(Enchantment.FLAME, 1);
                return;
            }
        }
    }

    @Override
    public void remove(Player player) {}
}
