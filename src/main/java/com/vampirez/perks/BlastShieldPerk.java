package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BlastShieldPerk extends Perk {

    public BlastShieldPerk() {
        super("blast_shield", "Blast Shield", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.TNT,
                "Your armor gets",
                "Blast Protection II");
    }

    @Override
    public void apply(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack piece : armor) {
            if (piece != null && piece.getType() != Material.AIR) {
                piece.addUnsafeEnchantment(Enchantment.BLAST_PROTECTION, 2);
            }
        }
        player.getInventory().setArmorContents(armor);
    }

    @Override
    public void remove(Player player) {}
}
