package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Bukkit;
import com.vampirez.MM;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class TrapperPerk extends Perk {

    private final Map<UUID, Long> lastRefill = new HashMap<>();
    private static final long REFILL_INTERVAL_MS = 30000;
    private static final int MAX_WEBS = 5;
    private static final Set<Block> placedWebs = new HashSet<>();

    public TrapperPerk() {
        super("trapper", "Trapper", PerkTier.PRISMATIC, PerkTeam.HUMAN,
                Material.COBWEB,
                "5 placeable cobwebs (+1 every 30s)",
                "to trap and slow enemies (auto-despawn 10s)");
    }

    public static ItemStack createTrapWeb(int amount) {
        ItemStack webs = new ItemStack(Material.COBWEB, amount);
        ItemMeta meta = webs.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Trap Web").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            meta.lore(Arrays.asList(Component.text("Place to trap enemies").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            webs.setItemMeta(meta);
        }
        return webs;
    }

    public static boolean isTrapWeb(ItemStack item) {
        return item != null && item.getType() == Material.COBWEB
                && item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                && item.getItemMeta().getDisplayName().contains("Trap Web");
    }

    public static void trackPlacedWeb(Block block, org.bukkit.plugin.Plugin plugin) {
        placedWebs.add(block);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (block.getType() == Material.COBWEB) {
                block.setType(Material.AIR);
            }
            placedWebs.remove(block);
        }, 200L); // 10 seconds
    }

    public static void clearAllWebs() {
        for (Block block : placedWebs) {
            if (block.getType() == Material.COBWEB) {
                block.setType(Material.AIR);
            }
        }
        placedWebs.clear();
    }

    @Override
    public void clearGlobalState() {
        clearAllWebs();
        lastRefill.clear();
    }

    @Override
    public void apply(Player player) {
        player.getInventory().addItem(createTrapWeb(MAX_WEBS));
    }

    @Override
    public void remove(Player player) {
        lastRefill.remove(player.getUniqueId());
        for (ItemStack item : player.getInventory().getContents()) {
            if (isTrapWeb(item)) {
                player.getInventory().remove(item);
            }
        }
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastRefill.get(uuid);
        if (last == null) { lastRefill.put(uuid, now); return; }
        if ((now - last) >= REFILL_INTERVAL_MS) {
            lastRefill.put(uuid, now);

            // Count current cobwebs in inventory
            int currentWebs = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (isTrapWeb(item)) {
                    currentWebs += item.getAmount();
                }
            }
            if (currentWebs < MAX_WEBS) {
                player.getInventory().addItem(createTrapWeb(1));
                player.sendMessage(MM.parse("<white>Trap Web refilled! (" + (currentWebs + 1) + "/" + MAX_WEBS + ")"));
            }
        }
    }
}
