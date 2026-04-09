package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MarksmanPerk extends Perk {

    public MarksmanPerk() {
        super("marksman", "Marksman", PerkTier.PRISMATIC, PerkTeam.HUMAN,
                Material.BOW,
                "Gives Power III + Punch I bow",
                "and 32 arrows");
    }

    @Override
    public void apply(Player player) {
        ItemStack bow = new ItemStack(Material.BOW);
        bow.addEnchantment(Enchantment.POWER, 3);
        bow.addEnchantment(Enchantment.PUNCH, 1);
        player.getInventory().addItem(bow);
        player.getInventory().addItem(new ItemStack(Material.ARROW, 32));
    }

    @Override
    public void remove(Player player) {}
}
