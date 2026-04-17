package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class KnockbackPerk extends Perk {

    public KnockbackPerk() {
        super("knockback", "Knockback", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.PISTON,
                "Your sword gets",
                "Knockback II");
    }

    @Override
    public void apply(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType().name().contains("SWORD")) {
                item.addUnsafeEnchantment(Enchantment.KNOCKBACK, 2);
                return;
            }
        }
    }

    @Override
    public void remove(Player player) {}
}
