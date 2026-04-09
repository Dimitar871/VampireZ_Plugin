package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ProtectionBoostPerk extends Perk {

    public ProtectionBoostPerk() {
        super("protection_boost", "Protection Boost", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.IRON_CHESTPLATE,
                "Upgrades all armor to",
                "Protection II");
    }

    @Override
    public void apply(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack piece : armor) {
            if (piece != null && piece.getType() != Material.AIR) {
                piece.addUnsafeEnchantment(Enchantment.PROTECTION, 2);
            }
        }
        player.getInventory().setArmorContents(armor);
    }

    @Override
    public void remove(Player player) {}
}
