package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DiamondEdgePerk extends Perk {

    public DiamondEdgePerk() {
        super("diamond_edge", "Diamond Edge", PerkTier.PRISMATIC, PerkTeam.HUMAN,
                Material.DIAMOND_SWORD,
                "Replaces your iron sword with",
                "a Diamond Sword (Sharpness I)");
    }

    @Override
    public void apply(Player player) {
        // Remove iron sword
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.IRON_SWORD) {
                player.getInventory().setItem(i, null);
                break;
            }
        }
        // Give diamond sword
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addEnchantment(Enchantment.SHARPNESS, 1);
        player.getInventory().setItem(0, sword);
    }

    @Override
    public void remove(Player player) {}
}
