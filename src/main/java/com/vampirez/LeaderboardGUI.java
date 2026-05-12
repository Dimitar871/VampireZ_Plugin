package com.vampirez;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Leaderboard browser with three sort modes (kills / wins / win-rate). Built on triumph-gui.
 * Per-player sort preference is remembered between opens via {@link #playerModes}.
 */
public class LeaderboardGUI {

    private enum SortMode { KILLS, WINS, WIN_RATE }

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

        Component title = Component.text("★ ").color(NamedTextColor.DARK_RED)
                .append(Component.text("Leaderboard — " + label(mode)).color(NamedTextColor.GOLD));

        Gui gui = Gui.gui()
                .title(title)
                .rows(6)
                .disableAllInteractions()
                .create();

        List<Map.Entry<UUID, PlayerStatsManager.PlayerStats>> entries = switch (mode) {
            case KILLS    -> statsManager.getTopByKills(45);
            case WINS     -> statsManager.getTopByWins(45);
            case WIN_RATE -> statsManager.getTopByWinRate(45);
        };

        for (int i = 0; i < Math.min(entries.size(), 45); i++) {
            UUID uuid = entries.get(i).getKey();
            PlayerStatsManager.PlayerStats s = entries.get(i).getValue();
            String name = s.name != null ? s.name : uuid.toString().substring(0, 8) + "…";
            gui.setItem(i, new GuiItem(skullFor(uuid, name, i + 1, s)));
        }

        if (entries.isEmpty()) {
            gui.setItem(22, new GuiItem(emptyMarker()));
        }

        // Bottom row
        ItemStack filler = fillerPane();
        for (int slot = 45; slot < 54; slot++) gui.setItem(slot, new GuiItem(filler));
        gui.setItem(46, sortButton(Material.IRON_SWORD,      "Kills",    mode == SortMode.KILLS,    () -> openWith(player, SortMode.KILLS)));
        gui.setItem(49, sortButton(Material.TOTEM_OF_UNDYING, "Wins",     mode == SortMode.WINS,     () -> openWith(player, SortMode.WINS)));
        gui.setItem(52, sortButton(Material.NETHER_STAR,      "Win Rate", mode == SortMode.WIN_RATE, () -> openWith(player, SortMode.WIN_RATE)));

        gui.open(player);
    }

    private static String label(SortMode mode) {
        return switch (mode) {
            case KILLS -> "Kills";
            case WINS -> "Wins";
            case WIN_RATE -> "Win Rate";
        };
    }

    private ItemStack skullFor(UUID uuid, String name, int rank, PlayerStatsManager.PlayerStats s) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
            meta.displayName(Component.empty().decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("#" + rank + "  ").color(NamedTextColor.GOLD))
                    .append(Component.text(name).color(NamedTextColor.WHITE)));
            int played = s.getGamesPlayed();
            String rateStr = played == 0 ? "N/A" : String.format("%.1f%%", s.getWinRate() * 100);
            meta.lore(List.of(
                    Component.text("─────────────────").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                    stat("Kills:    ", String.valueOf(s.kills)),
                    stat("Wins:     ", String.valueOf(s.wins)),
                    stat("Losses:   ", String.valueOf(s.losses)),
                    stat("Win Rate: ", rateStr)
            ));
            skull.setItemMeta(meta);
        }
        return skull;
    }

    private ItemStack emptyMarker() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta m = item.getItemMeta();
        if (m != null) {
            m.displayName(Component.text("No data yet").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            m.lore(List.of(Component.text("Stats are recorded when games end.").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            item.setItemMeta(m);
        }
        return item;
    }

    private GuiItem sortButton(Material mat, String label, boolean active, Runnable onClick) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty().decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("Sort: ").color(NamedTextColor.GOLD))
                    .append(Component.text(label).color(NamedTextColor.YELLOW)));
            meta.lore(List.of(active
                    ? Component.text("▶ Currently selected").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
                    : Component.text("Click to sort").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            if (active) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return new GuiItem(item, event -> onClick.run());
    }

    private ItemStack fillerPane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty().decoration(TextDecoration.ITALIC, false));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private static Component stat(String label, String value) {
        return Component.empty().decoration(TextDecoration.ITALIC, false)
                .append(Component.text(label).color(NamedTextColor.YELLOW))
                .append(Component.text(value).color(NamedTextColor.WHITE));
    }
}
