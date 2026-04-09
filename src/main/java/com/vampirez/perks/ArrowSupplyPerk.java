package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ArrowSupplyPerk extends Perk {

    public ArrowSupplyPerk() {
        super("arrow_supply", "Arrow Supply", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.ARROW,
                "Start with 16 extra arrows",
                "(40 total)");
    }

    @Override
    public void apply(Player player) {
        player.getInventory().addItem(new ItemStack(Material.ARROW, 16));
    }

    @Override
    public void remove(Player player) {}
}
