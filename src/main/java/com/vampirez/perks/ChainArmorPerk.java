package com.vampirez.perks;

import com.vampirez.GearManager;
import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ChainArmorPerk extends Perk {

    public ChainArmorPerk() {
        super("chain_armor", "Chain Armor", PerkTier.SILVER, PerkTeam.VAMPIRE,
                Material.CHAINMAIL_CHESTPLATE,
                "Upgrades leather armor",
                "to chainmail armor");
    }

    @Override
    public void apply(Player player) {
        // Keep the custom vampire head
        ItemStack helmet = GearManager.createVampireHead();

        ItemStack chestplate = new ItemStack(Material.CHAINMAIL_CHESTPLATE);
        ItemMeta cMeta = chestplate.getItemMeta();
        if (cMeta != null) { cMeta.setUnbreakable(true); chestplate.setItemMeta(cMeta); }

        ItemStack leggings = new ItemStack(Material.CHAINMAIL_LEGGINGS);
        ItemMeta lMeta = leggings.getItemMeta();
        if (lMeta != null) { lMeta.setUnbreakable(true); leggings.setItemMeta(lMeta); }

        ItemStack boots = new ItemStack(Material.CHAINMAIL_BOOTS);
        ItemMeta bMeta = boots.getItemMeta();
        if (bMeta != null) { bMeta.setUnbreakable(true); boots.setItemMeta(bMeta); }

        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
    }

    @Override
    public void remove(Player player) {}
}
