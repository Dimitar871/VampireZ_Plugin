package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GoldenFeastPerk extends Perk {

    public GoldenFeastPerk() {
        super("golden_feast", "Golden Feast", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.GOLDEN_APPLE,
                "Start with 9 extra",
                "Golden Apples (12 total)");
    }

    @Override
    public void apply(Player player) {
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 9));
    }

    @Override
    public void remove(Player player) {}
}
