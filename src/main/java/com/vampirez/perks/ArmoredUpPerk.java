package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ArmoredUpPerk extends Perk {

    public ArmoredUpPerk() {
        super("armored_up", "Armored Up", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.DIAMOND_CHESTPLATE,
                "Upgrades your armor to Diamond",
                "(no enchantments)");
    }

    @Override
    public void apply(Player player) {
        player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
    }

    @Override
    public void remove(Player player) {}
}
