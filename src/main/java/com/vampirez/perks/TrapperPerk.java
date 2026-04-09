package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class TrapperPerk extends Perk {

    public TrapperPerk() {
        super("trapper", "Trapper", PerkTier.PRISMATIC, PerkTeam.HUMAN,
                Material.COBWEB,
                "Gives 5 placeable cobwebs",
                "to trap and slow enemies");
    }

    @Override
    public void apply(Player player) {
        ItemStack webs = new ItemStack(Material.COBWEB, 5);
        ItemMeta meta = webs.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + "Trap Web");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Place to trap enemies"));
            webs.setItemMeta(meta);
        }
        player.getInventory().addItem(webs);
    }

    @Override
    public void remove(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.COBWEB) {
                player.getInventory().remove(item);
            }
        }
    }
}
