package com.vampirez;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemNamesTest {

    private static ItemStack item(Material type, Component name) {
        ItemStack item = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        when(item.getType()).thenReturn(type);
        when(item.hasItemMeta()).thenReturn(true);
        when(item.getItemMeta()).thenReturn(meta);
        when(meta.displayName()).thenReturn(name);
        return item;
    }

    @Test
    void plainStripsFormatting() {
        ItemStack bat = item(Material.PHANTOM_MEMBRANE,
                Component.text("Bat Form (Right-Click)").color(NamedTextColor.DARK_PURPLE));
        assertEquals("Bat Form (Right-Click)", ItemNames.plain(bat));
    }

    @Test
    void matchesNeedsTypeAndFragment() {
        ItemStack bat = item(Material.PHANTOM_MEMBRANE, Component.text("Bat Form (Right-Click)"));
        assertTrue(ItemNames.matches(bat, Material.PHANTOM_MEMBRANE, "Bat Form"));
        assertFalse(ItemNames.matches(bat, Material.PAPER, "Bat Form"));
        assertFalse(ItemNames.matches(bat, Material.PHANTOM_MEMBRANE, "Smoke Bomb"));
    }

    @Test
    void itemsWithoutNameOrMetaAreEmpty() {
        ItemStack unnamed = item(Material.PAPER, null);
        assertEquals("", ItemNames.plain(unnamed));

        ItemStack bare = mock(ItemStack.class);
        when(bare.hasItemMeta()).thenReturn(false);
        assertEquals("", ItemNames.plain(bare));
        assertFalse(ItemNames.matches(null, Material.PAPER, "x"));
    }
}
