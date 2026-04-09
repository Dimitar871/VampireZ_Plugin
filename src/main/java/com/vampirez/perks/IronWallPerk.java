package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class IronWallPerk extends Perk {

    public IronWallPerk() {
        super("iron_wall", "Iron Wall", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.SHIELD, "Gives you a Shield to block attacks");
    }

    @Override
    public void apply(Player player) {
        if (!player.getInventory().contains(Material.SHIELD)) {
            player.getInventory().addItem(new ItemStack(Material.SHIELD));
        }
    }

    @Override
    public void remove(Player player) {
        player.getInventory().remove(Material.SHIELD);
    }
}
