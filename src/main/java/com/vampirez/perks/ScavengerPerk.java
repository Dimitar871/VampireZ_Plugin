package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ScavengerPerk extends Perk {

    public ScavengerPerk() {
        super("scavenger", "Scavenger", PerkTier.SILVER, PerkTeam.VAMPIRE,
                Material.GOLDEN_APPLE, "Kills drop a Golden Apple");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onKill(Player killer, Player victim) {
        killer.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 1));
        killer.playSound(killer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 0.8f);
        killer.sendMessage(ChatColor.GOLD + "Scavenger: +1 Golden Apple!");
    }
}
