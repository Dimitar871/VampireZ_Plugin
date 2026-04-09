package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ThornsEnchantPerk extends Perk {

    public ThornsEnchantPerk() {
        super("thorns_enchant", "Thorns Enchant", PerkTier.PRISMATIC, PerkTeam.HUMAN,
                Material.CACTUS,
                "All armor gets Thorns II",
                "and Unbreaking III");
    }

    @Override
    public void apply(Player player) {
        ItemStack[] armor = {
            player.getInventory().getHelmet(),
            player.getInventory().getChestplate(),
            player.getInventory().getLeggings(),
            player.getInventory().getBoots()
        };
        for (ItemStack piece : armor) {
            if (piece != null) {
                piece.addUnsafeEnchantment(Enchantment.THORNS, 2);
                piece.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
            }
        }
    }

    @Override
    public void remove(Player player) {}
}
