package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class NetheriteArsenalPerk extends Perk {

    public NetheriteArsenalPerk() {
        super("netherite_arsenal", "Netherite Arsenal", PerkTier.PRISMATIC, PerkTeam.HUMAN,
                Material.NETHERITE_SWORD,
                "Netherite Sword (Sharp I)",
                "+ Netherite Helmet & Chestplate");
    }

    @Override
    public void apply(Player player) {
        // Netherite sword with Sharpness I (nerfed from II)
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        sword.addEnchantment(Enchantment.SHARPNESS, 1);
        player.getInventory().addItem(sword);

        // Only helmet + chestplate (no full set, no enchants)
        player.getInventory().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
    }

    @Override
    public void remove(Player player) {}
}
