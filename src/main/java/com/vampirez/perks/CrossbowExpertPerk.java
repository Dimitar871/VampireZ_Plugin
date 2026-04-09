package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CrossbowExpertPerk extends Perk {

    public CrossbowExpertPerk() {
        super("crossbow_expert", "Crossbow Expert", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.CROSSBOW,
                "Gives Crossbow with Quick Charge II",
                "and Piercing");
    }

    @Override
    public void apply(Player player) {
        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
        crossbow.addEnchantment(Enchantment.QUICK_CHARGE, 2);
        crossbow.addEnchantment(Enchantment.PIERCING, 1);
        player.getInventory().addItem(crossbow);
        // Ensure arrows
        if (!player.getInventory().contains(Material.ARROW)) {
            player.getInventory().addItem(new ItemStack(Material.ARROW, 16));
        }
    }

    @Override
    public void remove(Player player) {}
}
