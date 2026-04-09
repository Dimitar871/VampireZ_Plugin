package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class FireAspectPerk extends Perk {

    public FireAspectPerk() {
        super("fire_aspect", "Fire Aspect", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.BLAZE_POWDER,
                "Your sword gets",
                "Fire Aspect I");
    }

    @Override
    public void apply(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.IRON_SWORD) {
                item.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 1);
                return;
            }
        }
    }

    @Override
    public void remove(Player player) {}
}
