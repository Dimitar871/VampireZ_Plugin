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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PoisonQuiverPerk extends Perk {

    private final Map<UUID, Long> lastRegen = new HashMap<>();
    private static final long REGEN_INTERVAL_MS = 10000;

    public PoisonQuiverPerk() {
        super("poison_quiver", "Poison Quiver", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.TIPPED_ARROW,
                "Start with 8 Poison tipped arrows",
                "Regenerate 1 every 10 seconds");
    }

    private ItemStack createArrow(int amount) {
        ItemStack arrows = new ItemStack(Material.TIPPED_ARROW, amount);
        PotionMeta meta = (PotionMeta) arrows.getItemMeta();
        if (meta != null) {
            meta.setBasePotionData(new PotionData(PotionType.POISON, false, false));
            arrows.setItemMeta(meta);
        }
        return arrows;
    }

    @Override
    public void apply(Player player) {
        player.getInventory().addItem(createArrow(8));
    }

    @Override
    public void remove(Player player) {
        lastRegen.remove(player.getUniqueId());
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastRegen.get(uuid);
        if (last == null) { lastRegen.put(uuid, now); return; }
        if ((now - last) >= REGEN_INTERVAL_MS) {
            lastRegen.put(uuid, now);
            player.getInventory().addItem(createArrow(1));
            player.sendMessage(ChatColor.GREEN + "A poison arrow has regenerated!");
        }
    }
}
