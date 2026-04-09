package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SharpnessBoostPerk extends Perk {

    public SharpnessBoostPerk() {
        super("sharpness_boost", "Sharpness Boost", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.IRON_SWORD,
                "Upgrades your sword to",
                "Sharpness II");
    }

    @Override
    public void apply(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.IRON_SWORD) {
                item.addUnsafeEnchantment(Enchantment.SHARPNESS, 2);
                return;
            }
        }
    }

    @Override
    public void remove(Player player) {}
}
