package com.vampirez;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class LeaderboardGUI implements Listener {

    private enum SortMode { KILLS, WINS, WIN_RATE }

    private static final String TITLE_KILLS    = ChatColor.DARK_RED + "★ " + ChatColor.GOLD + "Leaderboard — Kills";
    private static final String TITLE_WINS     = ChatColor.DARK_RED + "★ " + ChatColor.GOLD + "Leaderboard — Wins";
    private static final String TITLE_WIN_RATE = ChatColor.DARK_RED + "★ " + ChatColor.GOLD + "Leaderboard — Win Rate";
    private static final Set<String> ALL_TITLES = Set.of(TITLE_KILLS, TITLE_WINS, TITLE_WIN_RATE);

    private final PlayerStatsManager statsManager;
    private final Map<UUID, SortMode> playerModes = new HashMap<>();

    public LeaderboardGUI(PlayerStatsManager statsManager) {
        this.statsManager = statsManager;
    }

    public void open(Player player) {
        openWith(player, playerModes.getOrDefault(player.getUniqueId(), SortMode.KILLS));
    }

    private void openWith(Player player, SortMode mode) {
        playerModes.put(player.getUniqueId(), mode);

        String title = switch (mode) {
            case KILLS    -> TITLE_KILLS;
            case WINS     -> TITLE_WINS;
            case WIN_RATE -> TITLE_WIN_RATE;
        };

        Inventory inv = Bukkit.createInventory(null, 54, title);

        List<Map.Entry<UUID, PlayerStatsManager.PlayerStats>> entries = switch (mode) {
            case KILLS    -> statsManager.getTopByKills(45);
            case WINS     -> statsManager.getTopByWins(45);
            case WIN_RATE -> statsManager.getTopByWinRate(45);
        };

        for (int i = 0; i < Math.min(entries.size(), 45); i++) {
            UUID uuid = entries.get(i).getKey();
            PlayerStatsManager.PlayerStats s = entries.get(i).getValue();
            String name = s.name != null ? s.name : uuid.toString().substring(0, 8) + "…";

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
                meta.setDisplayName(ChatColor.GOLD + "#" + (i + 1) + "  " + ChatColor.WHITE + name);
                int played = s.getGamesPlayed();
                String rateStr = played == 0 ? "N/A" : String.format("%.1f%%", s.getWinRate() * 100);
                meta.setLore(Arrays.asList(
                        ChatColor.DARK_GRAY + "─────────────────",
                        ChatColor.YELLOW + "Kills:    " + ChatColor.WHITE + s.kills,
                        ChatColor.YELLOW + "Wins:     " + ChatColor.WHITE + s.wins,
                        ChatColor.YELLOW + "Losses:   " + ChatColor.WHITE + s.losses,
                        ChatColor.YELLOW + "Win Rate: " + ChatColor.WHITE + rateStr
                ));
                skull.setItemMeta(meta);
            }
            inv.setItem(i, skull);
        }

        if (entries.isEmpty()) {
            ItemStack none = new ItemStack(Material.BARRIER);
            ItemMeta m = none.getItemMeta();
            if (m != null) {
                m.setDisplayName(ChatColor.RED + "No data yet");
                m.setLore(Collections.singletonList(ChatColor.GRAY + "Stats are recorded when games end."));
                none.setItemMeta(m);
            }
            inv.setItem(22, none);
        }

        // Bottom row
        ItemStack filler = fillerPane();
        for (int slot = 45; slot < 54; slot++) inv.setItem(slot, filler);

        inv.setItem(46, sortButton(Material.IRON_SWORD,      ChatColor.YELLOW + "Kills",    mode == SortMode.KILLS));
        inv.setItem(49, sortButton(Material.TOTEM_OF_UNDYING, ChatColor.YELLOW + "Wins",     mode == SortMode.WINS));
        inv.setItem(52, sortButton(Material.NETHER_STAR,      ChatColor.YELLOW + "Win Rate", mode == SortMode.WIN_RATE));

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!ALL_TITLES.contains(event.getView().getTitle())) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == 46) openWith(player, SortMode.KILLS);
        else if (slot == 49) openWith(player, SortMode.WINS);
        else if (slot == 52) openWith(player, SortMode.WIN_RATE);
    }

    private ItemStack sortButton(Material mat, String label, boolean active) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Sort: " + label);
            meta.setLore(Collections.singletonList(
                    active ? ChatColor.GREEN + "▶ Currently selected" : ChatColor.GRAY + "Click to sort"));
            if (active) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack fillerPane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); pane.setItemMeta(meta); }
        return pane;
    }
}
