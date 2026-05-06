package com.vampirez;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds debug-tool items (paper, tagged via PDC) for spawning / healing / killing /
 * checking the HP of dummy zombies, and listens for left-click on them to run the
 * matching command. Bypasses the vanilla "are you sure?" confirmation that books with
 * clickEvents trigger.
 *
 * Issued via the /vz tools admin subcommand.
 */
public final class DebugBookManager implements Listener {

    private static final NamespacedKey ACTION_KEY = NamespacedKey.fromString("vampirez:debug_action");

    /** Spawn keeps zombies in any biome: NoAI + Silent + PersistenceRequired + permanent fire resistance. */
    private static final String SUMMON_CMD =
            "/summon zombie ~ ~ ~ {NoAI:1b,Silent:1b,PersistenceRequired:1b,"
            + "CustomName:'\"Dummy\"',CustomNameVisible:1b,Tags:[\"dummy\"],"
            + "active_effects:[{id:\"minecraft:fire_resistance\",duration:1000000,amplifier:0,show_particles:0b,ambient:1b}],"
            + "attributes:[{id:\"minecraft:generic.max_health\",base:100}],Health:100f}";

    private static final String HEAL_CMD =
            "/data merge entity @e[tag=dummy,limit=1,sort=nearest] {Health:100f}";

    private static final String KILL_CMD = "/kill @e[tag=dummy]";

    private static final String CHECK_HP_CMD =
            "/data get entity @e[tag=dummy,limit=1,sort=nearest] Health";

    private static final Map<String, String> ACTION_TO_CMD = Map.of(
            "spawn", SUMMON_CMD,
            "heal",  HEAL_CMD,
            "kill",  KILL_CMD,
            "check", CHECK_HP_CMD
    );

    /** Adds the four debug items to the player's inventory. Returns the number actually added. */
    public static int giveBooks(Player p) {
        int added = 0;
        added += giveIfMissing(p, "spawn", "Spawn Dummy", NamedTextColor.GREEN,
                "Left-click in air to spawn a 100 HP dummy at your feet.");
        added += giveIfMissing(p, "heal", "Heal Dummy", NamedTextColor.AQUA,
                "Left-click to heal the nearest dummy back to 100 HP.");
        added += giveIfMissing(p, "kill", "Kill All Dummies", NamedTextColor.RED,
                "Left-click to remove all dummies in this world.");
        added += giveIfMissing(p, "check", "Check HP", NamedTextColor.YELLOW,
                "Left-click to print the nearest dummy's HP to chat.");
        return added;
    }

    private static int giveIfMissing(Player p, String actionId, String displayName,
                                     NamedTextColor color, String loreLine) {
        if (hasItem(p, actionId)) return 0;

        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        Component name = Component.text(displayName, color, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false);
        meta.displayName(name);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(loreLine, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, actionId);
        item.setItemMeta(meta);

        var leftover = p.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            p.getWorld().dropItem(p.getLocation(), item);
        }
        return 1;
    }

    private static boolean hasItem(Player p, String actionId) {
        for (ItemStack item : p.getInventory().getContents()) {
            if (item == null) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            String tag = meta.getPersistentDataContainer().get(ACTION_KEY, PersistentDataType.STRING);
            if (actionId.equals(tag)) return true;
        }
        return false;
    }

    // ===== Per-hit damage readout =====

    private static final Set<UUID> dmgDebugEnabled = ConcurrentHashMap.newKeySet();

    /** Toggles the per-hit damage readout for {@code p}. Returns the new state. */
    public static boolean toggleDamageDebug(Player p) {
        UUID id = p.getUniqueId();
        if (dmgDebugEnabled.remove(id)) return false;
        dmgDebugEnabled.add(id);
        return true;
    }

    /**
     * MONITOR runs after every other handler (including PerkListener at HIGHEST), so
     * {@link EntityDamageByEntityEvent#getFinalDamage()} reflects the true post-perk number.
     * The victim's HP hasn't been decremented yet at this point — we compute the post-hit
     * value ourselves.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPostDamage(EntityDamageByEntityEvent event) {
        if (dmgDebugEnabled.isEmpty()) return;

        Entity damager = event.getDamager();
        Player attacker;
        if (damager instanceof Player p) {
            attacker = p;
        } else if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        } else {
            attacker = null;
        }

        Entity victimEntity = event.getEntity();
        if (!(victimEntity instanceof LivingEntity victim)) return;

        double dmg = event.getFinalDamage();
        double preHp = victim.getHealth();
        double postHp = Math.max(0, preHp - dmg);

        String victimLabel;
        if (victim instanceof Player vp) {
            victimLabel = vp.getName();
        } else if (victim.customName() != null) {
            victimLabel = PlainTextComponentSerializer.plainText().serialize(victim.customName());
        } else {
            victimLabel = victim.getType().name();
        }

        // Outgoing readout for the attacker (DMG <amount>  <victim>: pre → post)
        if (attacker != null && dmgDebugEnabled.contains(attacker.getUniqueId())) {
            Component out = Component.text("DMG ", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .append(Component.text(String.format("%.2f", dmg), NamedTextColor.RED))
                    .append(Component.text("  ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(victimLabel + ": ", NamedTextColor.GRAY))
                    .append(Component.text(String.format("%.1f", preHp), NamedTextColor.YELLOW))
                    .append(Component.text(" → ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(String.format("%.1f", postHp),
                            postHp <= 0 ? NamedTextColor.DARK_RED : NamedTextColor.GREEN));
            attacker.sendActionBar(out);
        }

        // Incoming readout for the victim if they're a player with debug on. Skip if the victim
        // IS the attacker (self-damage from fall etc. is handled by other event types anyway).
        if (victim instanceof Player victimPlayer
                && (attacker == null || !attacker.getUniqueId().equals(victimPlayer.getUniqueId()))
                && dmgDebugEnabled.contains(victimPlayer.getUniqueId())) {
            String sourceLabel;
            if (attacker != null) {
                sourceLabel = attacker.getName();
            } else if (damager instanceof LivingEntity le && le.customName() != null) {
                sourceLabel = PlainTextComponentSerializer.plainText().serialize(le.customName());
            } else {
                sourceLabel = damager.getType().name();
            }
            Component in = Component.text("TOOK ", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .append(Component.text(String.format("%.2f", dmg), NamedTextColor.RED))
                    .append(Component.text(" from ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(sourceLabel, NamedTextColor.GRAY))
                    .append(Component.text("  HP: ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(String.format("%.1f", preHp), NamedTextColor.YELLOW))
                    .append(Component.text(" → ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(String.format("%.1f", postHp),
                            postHp <= 0 ? NamedTextColor.DARK_RED : NamedTextColor.GREEN));
            victimPlayer.sendActionBar(in);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        String tag = meta.getPersistentDataContainer().get(ACTION_KEY, PersistentDataType.STRING);
        if (tag == null) return;

        String cmd = ACTION_TO_CMD.get(tag);
        if (cmd == null) return;

        event.setCancelled(true);
        // dispatchCommand expects no leading slash
        String dispatch = cmd.startsWith("/") ? cmd.substring(1) : cmd;
        Bukkit.dispatchCommand(event.getPlayer(), dispatch);
    }

    /** Convenience for diagnostics — returns the plain-text display name of an item, or null. */
    @SuppressWarnings("unused")
    private static String plainDisplayName(ItemMeta meta) {
        Component dn = meta.displayName();
        return dn == null ? null : PlainTextComponentSerializer.plainText().serialize(dn);
    }
}
