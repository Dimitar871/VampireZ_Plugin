package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class IronRationsPerk extends Perk {

    public IronRationsPerk() {
        super("iron_rations", "Iron Rations", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.COOKED_BEEF,
                "Start with 8 extra cooked beef",
                "and 2 extra golden apples");
    }

    @Override
    public void apply(Player player) {
        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 8));
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 2));
    }

    @Override
    public void remove(Player player) {}
}
