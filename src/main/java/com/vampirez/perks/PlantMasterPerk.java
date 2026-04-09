package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class PlantMasterPerk extends Perk {

    private enum FlowerType {
        RED(Material.POPPY, 10.0),
        BLUE(Material.BLUE_ORCHID, 10.0),
        PURPLE(Material.ALLIUM, 15.0),
        BLACK(Material.WITHER_ROSE, 10.0);

        final Material material;
        final double radius;

        FlowerType(Material material, double radius) {
            this.material = material;
            this.radius = radius;
        }
    }

    private static class PlantData {
        final Location location;
        int ticksRemaining;

        PlantData(Location location) {
            this.location = location;
            this.ticksRemaining = 10;
        }
    }

    private static final long COOLDOWN_MS = 30000;

    private static final PotionEffectType[] CHAOS_EFFECTS = {
            PotionEffectType.SPEED, PotionEffectType.SLOWNESS, PotionEffectType.STRENGTH,
            PotionEffectType.WEAKNESS, PotionEffectType.REGENERATION, PotionEffectType.POISON,
            PotionEffectType.RESISTANCE, PotionEffectType.HUNGER, PotionEffectType.BLINDNESS,
            PotionEffectType.WITHER, PotionEffectType.LEVITATION, PotionEffectType.INVISIBILITY,
            PotionEffectType.FIRE_RESISTANCE, PotionEffectType.ABSORPTION
    };

    private final Map<UUID, Map<FlowerType, PlantData>> activePlants = new HashMap<>();
    private final Map<UUID, Map<FlowerType, Long>> cooldowns = new HashMap<>();

    public PlantMasterPerk() {
        super("plant_master", "Plant Master", PerkTier.PRISMATIC, PerkTeam.HUMAN,
                Material.POPPY,
                "4 magical flowers to plant anywhere",
                "Each creates an AoE zone for 10s (30s cd)");
    }

    @Override
    public void apply(Player player) {
        giveFlowerItem(player, Material.POPPY, "&cRed Flower", "Regen I to allies (10 block radius)");
        giveFlowerItem(player, Material.BLUE_ORCHID, "&9Blue Flower", "Speed I to allies (10 block radius)");
        giveFlowerItem(player, Material.ALLIUM, "&5Purple Flower", "Weakness I to enemies (15 block radius)");
        giveFlowerItem(player, Material.WITHER_ROSE, "&8Black Rose", "Random chaos effect to ALL nearby (10 block radius)");
    }

    private void giveFlowerItem(Player player, Material mat, String name, String effectDesc) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name + " &7(Right-Click)"));
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Plant anywhere (Right-Click)",
                    ChatColor.YELLOW + effectDesc,
                    ChatColor.GRAY + "Duration: 10s | Cooldown: 30s"
            ));
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
    }

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();

        // Destroy active plants
        Map<FlowerType, PlantData> plants = activePlants.remove(uuid);
        if (plants != null) {
            for (PlantData data : plants.values()) {
                Block block = data.location.getBlock();
                if (isFlowerMaterial(block.getType())) {
                    block.setType(Material.AIR);
                }
            }
        }

        cooldowns.remove(uuid);

        // Remove flower items from inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isFlowerMaterial(item.getType()) && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Flower")
                    || (item != null && item.getType() == Material.WITHER_ROSE && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Black Rose"))) {
                player.getInventory().remove(item);
            }
        }
    }

    private boolean isFlowerMaterial(Material mat) {
        return mat == Material.POPPY || mat == Material.BLUE_ORCHID
                || mat == Material.ALLIUM || mat == Material.WITHER_ROSE;
    }

    private FlowerType getFlowerType(Material mat) {
        for (FlowerType type : FlowerType.values()) {
            if (type.material == mat) return type;
        }
        return null;
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        FlowerType flowerType = getFlowerType(item.getType());
        if (flowerType == null || !item.hasItemMeta()) return;

        String displayName = item.getItemMeta().getDisplayName();
        if (!displayName.contains("Flower") && !displayName.contains("Black Rose")) return;

        event.setCancelled(true);

        // Find placement location
        Block placeBlock;
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            // Place on top of the clicked block
            placeBlock = event.getClickedBlock().getRelative(BlockFace.UP);
        } else {
            // Right-click air: place at the player's feet
            placeBlock = player.getLocation().getBlock();
        }

        // Don't replace existing blocks — only place in air
        if (placeBlock.getType() != Material.AIR) {
            player.sendMessage(ChatColor.RED + "Can't place a flower there — the space is occupied!");
            return;
        }

        UUID uuid = player.getUniqueId();

        // Check cooldown
        Map<FlowerType, Long> playerCooldowns = cooldowns.computeIfAbsent(uuid, k -> new EnumMap<>(FlowerType.class));
        Long lastUse = playerCooldowns.get(flowerType);
        long now = System.currentTimeMillis();
        long effectiveCd = getEffectiveCooldown(player, COOLDOWN_MS);
        if (lastUse != null && (now - lastUse) < effectiveCd) {
            long remaining = (effectiveCd - (now - lastUse)) / 1000 + 1;
            player.sendMessage(ChatColor.RED + "That flower is on cooldown! " + remaining + "s");
            return;
        }

        // Place flower - remove old one of same type first
        Map<FlowerType, PlantData> playerPlants = activePlants.computeIfAbsent(uuid, k -> new EnumMap<>(FlowerType.class));
        PlantData existing = playerPlants.remove(flowerType);
        if (existing != null) {
            Block oldBlock = existing.location.getBlock();
            if (oldBlock.getType() == flowerType.material) {
                oldBlock.setType(Material.AIR);
            }
        }

        // Place the flower
        placeBlock.setType(flowerType.material);

        PlantData plantData = new PlantData(placeBlock.getLocation());
        playerPlants.put(flowerType, plantData);
        playerCooldowns.put(flowerType, now);

        player.getWorld().playSound(placeBlock.getLocation(), Sound.BLOCK_GRASS_PLACE, 1.0f, 1.2f);
        incrementStat(uuid, "plants_placed");

        String flowerName = switch (flowerType) {
            case RED -> ChatColor.RED + "Red Flower";
            case BLUE -> ChatColor.BLUE + "Blue Flower";
            case PURPLE -> ChatColor.DARK_PURPLE + "Purple Flower";
            case BLACK -> ChatColor.DARK_GRAY + "Black Rose";
        };
        player.sendMessage(ChatColor.GREEN + "Planted " + flowerName + ChatColor.GREEN + "! (10s duration)");
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        Map<FlowerType, PlantData> playerPlants = activePlants.get(uuid);
        if (playerPlants == null || playerPlants.isEmpty()) return;

        Iterator<Map.Entry<FlowerType, PlantData>> it = playerPlants.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<FlowerType, PlantData> entry = it.next();
            FlowerType type = entry.getKey();
            PlantData data = entry.getValue();

            data.ticksRemaining--;

            if (data.ticksRemaining <= 0) {
                // Expired - remove flower block
                Block block = data.location.getBlock();
                if (block.getType() == type.material) {
                    block.setType(Material.AIR);
                }
                data.location.getWorld().playSound(data.location, Sound.BLOCK_GRASS_BREAK, 0.8f, 1.0f);
                it.remove();
                continue;
            }

            Location loc = data.location.clone().add(0.5, 0.5, 0.5);

            // Spawn particle ring
            spawnParticleRing(loc, type);

            // Apply effects to nearby players
            applyFlowerEffects(player, uuid, type, loc, data);
        }
    }

    private void spawnParticleRing(Location center, FlowerType type) {
        org.bukkit.Color color = switch (type) {
            case RED -> org.bukkit.Color.fromRGB(255, 50, 50);
            case BLUE -> org.bukkit.Color.fromRGB(50, 100, 255);
            case PURPLE -> org.bukkit.Color.fromRGB(180, 50, 255);
            case BLACK -> org.bukkit.Color.fromRGB(30, 30, 30);
        };

        double radius = type.radius;
        int points = (int) (radius * 6);
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, 0.1, z), 3, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(color, 2.0f));
            // Second elevated ring for visibility
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, 1.0, z), 2, 0.05, 0.2, 0.05, 0,
                    new Particle.DustOptions(color, 1.5f));
        }
        // Fill area with scattered particles
        center.getWorld().spawnParticle(Particle.DUST, center, 30, radius * 0.5, 0.5, radius * 0.5, 0,
                new Particle.DustOptions(color, 1.8f));
    }

    private void applyFlowerEffects(Player owner, UUID ownerUuid, FlowerType type, Location center, PlantData data) {
        double radius = type.radius;
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof Player target)) continue;

            switch (type) {
                case RED -> {
                    // Regen I to teammates — use 100-tick duration so 50-tick heal timer fires
                    if (target.getUniqueId().equals(ownerUuid) || isSameTeam(owner, target)) {
                        PotionEffect existingRegen = target.getPotionEffect(PotionEffectType.REGENERATION);
                        if (existingRegen == null || existingRegen.getDuration() < 30) {
                            target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, true, true));
                        }
                        addStat(ownerUuid, "players_affected", 1);
                    }
                }
                case BLUE -> {
                    // Speed I to teammates
                    if (target.getUniqueId().equals(ownerUuid) || isSameTeam(owner, target)) {
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30, 0, true, true));
                        addStat(ownerUuid, "players_affected", 1);
                    }
                }
                case PURPLE -> {
                    // Weakness I to enemies
                    if (!target.getUniqueId().equals(ownerUuid) && !isSameTeam(owner, target)) {
                        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 30, 0, true, true));
                        addStat(ownerUuid, "players_affected", 1);
                    }
                }
                case BLACK -> {
                    // Random chaos effect to ALL nearby
                    PotionEffectType randomEffect = CHAOS_EFFECTS[ThreadLocalRandom.current().nextInt(CHAOS_EFFECTS.length)];
                    target.addPotionEffect(new PotionEffect(randomEffect, 40, 0, true, true));
                    addStat(ownerUuid, "players_affected", 1);
                }
            }
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("plants_placed", "Plants Placed");
        labels.put("players_affected", "Players Affected");
        return labels;
    }
}
