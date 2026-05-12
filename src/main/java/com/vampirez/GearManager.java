package com.vampirez;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

public class GearManager {

    public void giveHumanGear(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // Iron Sword with Sharpness I
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        sword.addEnchantment(Enchantment.SHARPNESS, 1);
        player.getInventory().setItem(0, sword);

        // Bow
        ItemStack bow = new ItemStack(Material.BOW);
        player.getInventory().setItem(1, bow);

        // Golden Apples
        player.getInventory().setItem(2, new ItemStack(Material.GOLDEN_APPLE, 3));

        // Cooked Beef
        player.getInventory().setItem(3, new ItemStack(Material.COOKED_BEEF, 12));

        // Arrows
        player.getInventory().setItem(4, new ItemStack(Material.ARROW, 24));

        // Shop Emerald
        player.getInventory().setItem(8, createShopItem());

        // Iron Armor with Protection I
        ItemStack helmet = new ItemStack(Material.IRON_HELMET);
        helmet.addEnchantment(Enchantment.PROTECTION, 1);
        ItemStack chestplate = new ItemStack(Material.IRON_CHESTPLATE);
        chestplate.addEnchantment(Enchantment.PROTECTION, 1);
        ItemStack leggings = new ItemStack(Material.IRON_LEGGINGS);
        leggings.addEnchantment(Enchantment.PROTECTION, 1);
        ItemStack boots = new ItemStack(Material.IRON_BOOTS);
        boots.addEnchantment(Enchantment.PROTECTION, 1);

        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);
    }

    public void giveVampireGear(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // Stone Sword
        player.getInventory().setItem(0, new ItemStack(Material.STONE_SWORD));

        // Rotten Flesh
        player.getInventory().setItem(1, new ItemStack(Material.ROTTEN_FLESH, 8));

        // Vampire Leap item
        player.getInventory().setItem(2, createLeapItem());

        // Shop Emerald
        player.getInventory().setItem(8, createShopItem());

        // Custom Vampire Head
        ItemStack vHelmet = createVampireHead();

        // Black Leather Armor (unbreakable)
        ItemStack vChestplate = createBlackLeather(Material.LEATHER_CHESTPLATE);
        ItemStack vLeggings = createBlackLeather(Material.LEATHER_LEGGINGS);
        ItemStack vBoots = createBlackLeather(Material.LEATHER_BOOTS);

        player.getInventory().setHelmet(vHelmet);
        player.getInventory().setChestplate(vChestplate);
        player.getInventory().setLeggings(vLeggings);
        player.getInventory().setBoots(vBoots);

        // Night Vision (permanent, 999999 ticks)
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 999999, 0, false, false));
    }

    private ItemStack createShopItem() {
        ItemStack emerald = new ItemStack(Material.EMERALD);
        ItemMeta meta = emerald.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty()
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("Perk Shop").color(NamedTextColor.GREEN))
                    .append(Component.text(" (Right-Click)").color(NamedTextColor.GRAY)));
            meta.lore(java.util.List.of(
                Component.text("Right-click to open the perk shop")
                        .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            emerald.setItemMeta(meta);
        }
        return emerald;
    }

    private ItemStack createBlackLeather(Material material) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta != null) {
            meta.setColor(Color.BLACK);
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createVampireHead() {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if (skullMeta != null) {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "");
            PlayerTextures textures = profile.getTextures();
            try {
                textures.setSkin(new URL(
                        "http://textures.minecraft.net/texture/16b76d73c65089cc1264a6291990574a03783ba540a343a352bd49c073c5cf5c"));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            profile.setTextures(textures);
            skullMeta.setOwnerProfile(profile);
            skullMeta.displayName(Component.text("Vampire").color(NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
            skullMeta.setUnbreakable(true);
            skull.setItemMeta(skullMeta);
        }
        return skull;
    }

    public static ItemStack createLeapItem() {
        ItemStack ghastTear = new ItemStack(Material.GHAST_TEAR);
        ItemMeta meta = ghastTear.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty()
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("Vampire Leap").color(NamedTextColor.DARK_PURPLE))
                    .append(Component.text(" (Right-Click)").color(NamedTextColor.GRAY)));
            meta.lore(java.util.List.of(
                Component.text("Launch forward in the direction you're looking")
                        .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty().decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("Cooldown: ").color(NamedTextColor.YELLOW))
                        .append(Component.text("8 seconds").color(NamedTextColor.WHITE))
            ));
            ghastTear.setItemMeta(meta);
        }
        return ghastTear;
    }
}
