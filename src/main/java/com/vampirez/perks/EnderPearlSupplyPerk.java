package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnderPearlSupplyPerk extends Perk {

    private final Map<UUID, Integer> tickCounters = new HashMap<>();
    private static final int REGEN_TICKS = 30; // 30 seconds

    public EnderPearlSupplyPerk() {
        super("ender_pearl_supply", "Ender Pearl Supply", PerkTier.GOLD, PerkTeam.BOTH,
                Material.ENDER_PEARL,
                "Gives 3 Ender Pearls",
                "Regenerate 1 every 30 seconds");
    }

    @Override
    public void apply(Player player) {
        player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 3));
        tickCounters.put(player.getUniqueId(), 0);
    }

    @Override
    public void remove(Player player) {
        tickCounters.remove(player.getUniqueId());
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        int count = tickCounters.getOrDefault(uuid, 0) + 1;
        tickCounters.put(uuid, count);

        if (count >= REGEN_TICKS) {
            tickCounters.put(uuid, 0);
            // Only give if player has less than 3
            int current = countEnderPearls(player);
            if (current < 3) {
                player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 1));
            }
        }
    }

    private int countEnderPearls(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.ENDER_PEARL) {
                count += item.getAmount();
            }
        }
        return count;
    }
}
