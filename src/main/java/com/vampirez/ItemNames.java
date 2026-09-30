package com.vampirez;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Ability items are recognised by type + a fragment of their display name. */
public final class ItemNames {

    private ItemNames() {}

    public static String plain(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return "";

        Component name = item.getItemMeta().displayName();
        if (name == null)
            return "";

        return PlainTextComponentSerializer.plainText().serialize(name);
    }

    public static boolean matches(ItemStack item, Material type, String fragment) {
        return item != null && item.getType() == type && plain(item).contains(fragment);
    }

    public static void removeAll(Player player, Material type, String fragment) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (matches(item, type, fragment))
                player.getInventory().remove(item);
        }
    }
}
