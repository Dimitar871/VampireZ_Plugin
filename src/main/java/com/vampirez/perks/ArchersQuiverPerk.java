package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ArchersQuiverPerk extends Perk {

    public ArchersQuiverPerk() {
        super("archers_quiver", "Archer's Quiver", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.BOW,
                "Gives Infinity + Power I bow");
    }

    @Override
    public void apply(Player player) {
        ItemStack bow = new ItemStack(Material.BOW);
        bow.addEnchantment(Enchantment.INFINITY, 1);
        bow.addEnchantment(Enchantment.POWER, 1);
        player.getInventory().addItem(bow);
        if (!player.getInventory().contains(Material.ARROW)) {
            player.getInventory().addItem(new ItemStack(Material.ARROW, 1));
        }
    }

    @Override
    public void remove(Player player) {}
}
