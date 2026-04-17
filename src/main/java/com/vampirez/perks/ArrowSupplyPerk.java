package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

public class ArrowSupplyPerk extends Perk {

    public ArrowSupplyPerk() {
        super("arrow_supply", "Arrow Supply", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.TIPPED_ARROW,
                "16 Poison arrows, 16 Slowness",
                "arrows, 16 Blindness arrows");
    }

    @Override
    public void apply(Player player) {
        // 16 Poison arrows
        ItemStack poisonArrows = new ItemStack(Material.TIPPED_ARROW, 16);
        PotionMeta poisonMeta = (PotionMeta) poisonArrows.getItemMeta();
        if (poisonMeta != null) {
            poisonMeta.setBasePotionData(new PotionData(PotionType.POISON, false, false));
            poisonMeta.setDisplayName(ChatColor.GREEN + "Poison Arrow");
            poisonArrows.setItemMeta(poisonMeta);
        }
        player.getInventory().addItem(poisonArrows);

        // 16 Slowness arrows
        ItemStack slowArrows = new ItemStack(Material.TIPPED_ARROW, 16);
        PotionMeta slowMeta = (PotionMeta) slowArrows.getItemMeta();
        if (slowMeta != null) {
            slowMeta.setBasePotionData(new PotionData(PotionType.SLOWNESS, false, false));
            slowMeta.setDisplayName(ChatColor.AQUA + "Slowness Arrow");
            slowArrows.setItemMeta(slowMeta);
        }
        player.getInventory().addItem(slowArrows);

        // 16 Blindness arrows (use Thick as base, add custom effect)
        ItemStack blindArrows = new ItemStack(Material.TIPPED_ARROW, 16);
        PotionMeta blindMeta = (PotionMeta) blindArrows.getItemMeta();
        if (blindMeta != null) {
            blindMeta.setBasePotionData(new PotionData(PotionType.MUNDANE, false, false));
            blindMeta.addCustomEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.BLINDNESS, 60, 0, false, true), true);
            blindMeta.setDisplayName(ChatColor.DARK_GRAY + "Blindness Arrow");
            blindArrows.setItemMeta(blindMeta);
        }
        player.getInventory().addItem(blindArrows);
    }

    @Override
    public void remove(Player player) {}
}
