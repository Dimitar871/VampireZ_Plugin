package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class WarHorsePerk extends Perk {

    private final Map<UUID, List<Horse>> playerHorses = new HashMap<>();
    private final Map<UUID, Boolean> wasMounted = new HashMap<>();

    public WarHorsePerk() {
        super("war_horse", "War Horse", PerkTier.GOLD, PerkTeam.BOTH,
                Material.SADDLE,
                "Spawn 2 war horses (2x speed, 3x HP,",
                "diamond armor, anyone can ride).",
                "While mounted: Regen I + Strength II.",
                "Use Horse Whistle to call them back.");
    }

    @Override
    public void apply(Player player) {
        UUID uuid = player.getUniqueId();
        List<Horse> horses = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            Horse horse = player.getWorld().spawn(
                    player.getLocation().add(i * 2 - 1, 0, 0), Horse.class);

            // Pre-tamed — no owner so anyone can mount
            horse.setTamed(true);
            horse.setAdult();

            // 3x health (normal horse ~30 HP, so ~90 HP)
            double baseHealth = horse.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
            horse.getAttribute(Attribute.MAX_HEALTH).setBaseValue(baseHealth * 3.0);
            horse.setHealth(baseHealth * 3.0);

            // 2x speed (normal ~0.225, so ~0.45)
            double baseSpeed = horse.getAttribute(Attribute.MOVEMENT_SPEED).getBaseValue();
            horse.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(baseSpeed * 2.0);

            // Diamond armor + saddle
            horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
            horse.getInventory().setArmor(new ItemStack(Material.DIAMOND_HORSE_ARMOR));

            // Tag for cleanup — store owner UUID for buff purposes
            horse.setMetadata("vampirez_war_horse", new FixedMetadataValue(getPlugin(), uuid.toString()));
            horse.setCustomName(ChatColor.GOLD + player.getName() + "'s War Horse");
            horse.setCustomNameVisible(true);

            horses.add(horse);
        }

        playerHorses.put(uuid, horses);

        // Give Horse Whistle item
        giveWhistle(player);

        player.sendMessage(ChatColor.GOLD + "Your War Horses have arrived! Anyone can ride them.");
        player.playSound(player.getLocation(), Sound.ENTITY_HORSE_AMBIENT, 1.0f, 1.0f);
    }

    private void giveWhistle(Player player) {
        ItemStack whistle = new ItemStack(Material.GOAT_HORN);
        ItemMeta meta = whistle.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Horse Whistle " + ChatColor.GRAY + "(Right-Click)");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Call your war horses to you",
                    ChatColor.YELLOW + "Won't call horses with riders"
            ));
            whistle.setItemMeta(meta);
        }
        player.getInventory().addItem(whistle);
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.GOAT_HORN) return;
        if (!item.hasItemMeta() || !item.getItemMeta().getDisplayName().contains("Horse Whistle")) return;

        event.setCancelled(true);

        UUID uuid = player.getUniqueId();
        List<Horse> horses = playerHorses.get(uuid);
        if (horses == null || horses.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You have no living war horses!");
            return;
        }

        int called = 0;
        int ridden = 0;
        Location target = player.getLocation();

        for (Horse horse : horses) {
            if (horse == null || horse.isDead()) continue;

            // Don't teleport horses that have riders
            if (!horse.getPassengers().isEmpty()) {
                ridden++;
                continue;
            }

            horse.teleport(target.clone().add(called * 2 - 0.5, 0, 1));
            called++;
        }

        if (called > 0) {
            player.sendMessage(ChatColor.GOLD + "Called " + called + " war horse" + (called > 1 ? "s" : "") + " to you!");
            player.playSound(player.getLocation(), Sound.ENTITY_HORSE_AMBIENT, 1.0f, 1.2f);
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 10, 1, 0.5, 1, 0);
        } else if (ridden > 0) {
            player.sendMessage(ChatColor.RED + "All your horses are being ridden!");
        } else {
            player.sendMessage(ChatColor.RED + "You have no living war horses!");
        }
    }

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        // Remove mounted buffs
        player.removePotionEffect(PotionEffectType.REGENERATION);
        player.removePotionEffect(PotionEffectType.STRENGTH);
        wasMounted.remove(uuid);

        // Despawn horses
        List<Horse> horses = playerHorses.remove(uuid);
        if (horses != null) {
            for (Horse horse : horses) {
                if (horse != null && !horse.isDead()) {
                    // Eject any riders first
                    horse.eject();
                    horse.remove();
                }
            }
        }

        // Remove whistle from inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.GOAT_HORN && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Horse Whistle")) {
                player.getInventory().remove(item);
            }
        }
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        boolean mounted = player.getVehicle() instanceof Horse horse
                && horse.hasMetadata("vampirez_war_horse");

        boolean previouslyMounted = wasMounted.getOrDefault(uuid, false);

        if (mounted && !previouslyMounted) {
            // Just mounted — apply buffs with long duration
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 1, false, false));
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 5, 0.5, 0.5, 0.5, 0);
        } else if (mounted) {
            // Still mounted — only refresh when about to expire (avoids resetting regen heal timer)
            PotionEffect regen = player.getPotionEffect(PotionEffectType.REGENERATION);
            if (regen == null || regen.getDuration() < 30) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, false, false));
            }
            PotionEffect str = player.getPotionEffect(PotionEffectType.STRENGTH);
            if (str == null || str.getDuration() < 30) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 1, false, false));
            }
        } else if (previouslyMounted) {
            // Just dismounted — remove buffs
            player.removePotionEffect(PotionEffectType.REGENERATION);
            player.removePotionEffect(PotionEffectType.STRENGTH);
        }

        wasMounted.put(uuid, mounted);

        // Clean up dead horses from the list
        List<Horse> horses = playerHorses.get(uuid);
        if (horses != null) {
            horses.removeIf(h -> h == null || h.isDead());
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        return Collections.emptyMap();
    }
}
