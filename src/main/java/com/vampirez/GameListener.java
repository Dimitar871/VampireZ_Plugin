package com.vampirez;

import org.bukkit.attribute.Attribute;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class GameListener implements Listener {

    private final JavaPlugin plugin;
    private final GameManager gameManager;
    private final GearManager gearManager;
    private final EconomyManager economyManager;
    private final PerkManager perkManager;
    private final PerkShopGUI perkShopGUI;

    public GameListener(JavaPlugin plugin, GameManager gameManager, GearManager gearManager,
                        EconomyManager economyManager, PerkManager perkManager, PerkShopGUI perkShopGUI) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.gearManager = gearManager;
        this.economyManager = economyManager;
        this.perkManager = perkManager;
        this.perkShopGUI = perkShopGUI;
    }

    // ===== MOB TARGETING CONTROL =====

    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player target)) return;
        Entity entity = event.getEntity();
        if (!entity.hasMetadata("vampirez_team")) return;

        String mobTeam = entity.getMetadata("vampirez_team").get(0).asString();
        boolean targetIsVampire = gameManager.isVampire(target.getUniqueId());
        String targetTeam = targetIsVampire ? "VAMPIRE" : "HUMAN";

        if (mobTeam.equals(targetTeam)) {
            event.setCancelled(true);
        }
    }

    // ===== TAGGED MOB PROTECTION =====

    @EventHandler(priority = EventPriority.HIGH)
    public void onMobDamage(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        if (victim instanceof Player) return; // handled by PvP control
        if (!victim.hasMetadata("vampirez_team")) return;

        // Find the player who attacked
        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }
        if (attacker == null) return;

        String mobTeam = victim.getMetadata("vampirez_team").get(0).asString();
        boolean attackerIsVampire = gameManager.isVampire(attacker.getUniqueId());
        String attackerTeam = attackerIsVampire ? "VAMPIRE" : "HUMAN";

        if (mobTeam.equals(attackerTeam)) {
            event.setCancelled(true); // Can't damage your own team's mobs
        }
    }

    // ===== PVP CONTROL =====

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = resolveAttacker(event);
        if (attacker == null) return;

        // Only allow PvP during active game
        if (gameManager.getState() != GameState.ACTIVE) {
            event.setCancelled(true);
            return;
        }

        // Block combat involving unreleased vampires (scouting phase)
        if (!gameManager.isVampiresReleased()) {
            if (gameManager.isVampire(attacker.getUniqueId()) || gameManager.isVampire(victim.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }

        UUID attackerUUID = attacker.getUniqueId();
        UUID victimUUID = victim.getUniqueId();

        // Prevent friendly fire
        boolean attackerHuman = gameManager.isHuman(attackerUUID);
        boolean victimHuman = gameManager.isHuman(victimUUID);
        boolean attackerVamp = gameManager.isVampire(attackerUUID);
        boolean victimVamp = gameManager.isVampire(victimUUID);

        if ((attackerHuman && victimHuman) || (attackerVamp && victimVamp)) {
            event.setCancelled(true);
            return;
        }

        // Team-based flat damage with partial armor, only when holding a weapon
        ItemStack weaponItem = attacker.getInventory().getItemInMainHand();
        Material weapon = weaponItem.getType();
        if (weapon.name().contains("SWORD") || weapon.name().contains("AXE")) {
            // Humans deal 5.0 HP (2.5 hearts), vampires deal 4.0 HP (2 hearts)
            // Scale by attack cooldown so attack speed perks are meaningful
            double cooldown = attacker.getAttackCooldown(); // 0.0 to 1.0
            double baseDamage = (attackerHuman ? 5.0 : 4.0);

            // Weapon material bonus (iron is baseline for humans)
            if (weapon.name().contains("DIAMOND")) {
                baseDamage += 0.5;
            } else if (weapon.name().contains("NETHERITE")) {
                baseDamage += 1.0;
            }

            // Sharpness bonus: +0.5 HP per level
            int sharpness = weaponItem.getEnchantmentLevel(Enchantment.SHARPNESS);
            baseDamage += sharpness * 0.5;

            baseDamage *= cooldown;

            // Calculate armor reduction: use 30% of vanilla armor value
            double armorPoints = victim.getAttribute(Attribute.ARMOR) != null
                    ? victim.getAttribute(Attribute.ARMOR).getValue() : 0;
            // Vanilla formula: armor reduces damage by armorPoints / 25 (capped)
            double armorReduction = armorPoints / 25.0;
            if (armorReduction > 0.8) armorReduction = 0.8;
            // Only apply 30% of the armor reduction
            double afterArmor = baseDamage * (1.0 - armorReduction * 0.3);

            // Protection enchantment reduction: sum Protection levels across all armor
            int totalProtection = 0;
            for (ItemStack piece : victim.getInventory().getArmorContents()) {
                if (piece != null && piece.getType() != Material.AIR) {
                    totalProtection += piece.getEnchantmentLevel(Enchantment.PROTECTION);
                }
            }
            // Each Protection level reduces damage by 4% (vanilla formula)
            // Cap at 80% reduction (Protection 20, effectively unreachable but safe)
            double protReduction = Math.min(totalProtection * 0.04, 0.80);
            double finalDamage = afterArmor * (1.0 - protReduction);

            // Set our calculated damage as the final result
            event.setDamage(finalDamage);

            // Zero out vanilla armor and enchantment modifiers to prevent double-application
            // Our formula already accounts for armor and Protection
            try {
                if (event.isApplicable(EntityDamageEvent.DamageModifier.ARMOR)) {
                    event.setDamage(EntityDamageEvent.DamageModifier.ARMOR, 0);
                }
                if (event.isApplicable(EntityDamageEvent.DamageModifier.MAGIC)) {
                    event.setDamage(EntityDamageEvent.DamageModifier.MAGIC, 0);
                }
                if (event.isApplicable(EntityDamageEvent.DamageModifier.RESISTANCE)) {
                    event.setDamage(EntityDamageEvent.DamageModifier.RESISTANCE, 0);
                }
            } catch (UnsupportedOperationException ignored) {
                // Some modifiers may not be applicable depending on damage cause
            }
        }

        // Track damage for assists
        economyManager.recordDamage(victimUUID, attackerUUID);
    }

    // ===== DEATH HANDLING =====

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (gameManager.getState() != GameState.ACTIVE) return;

        Player victim = event.getEntity();
        UUID victimUUID = victim.getUniqueId();

        // Clear drops and death message
        event.getDrops().clear();
        event.setDeathMessage(null);
        event.setKeepInventory(false);
        event.setKeepLevel(false);

        // Award kill gold
        Player killer = victim.getKiller();
        if (killer != null) {
            economyManager.awardKillRewards(killer.getUniqueId(), victimUUID);
        }

        if (gameManager.isHuman(victimUUID)) {
            // Human dies → convert to vampire (handled after respawn)
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> gameManager.convertHumanToVampire(victim), 2L);
        } else if (gameManager.isVampire(victimUUID)) {
            // Vampire dies → play death cackle and respawn after delay
            SoundEffects.playVampireDeathCackle(victim, plugin);
            gameManager.respawnVampire(victim);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (gameManager.getState() != GameState.ACTIVE) return;

        if (gameManager.isVampire(uuid) && gameManager.getVampireSpawn() != null) {
            event.setRespawnLocation(gameManager.getVampireSpawn());
        } else if (gameManager.isHuman(uuid) && gameManager.getHumanSpawn() != null) {
            // Human converting to vampire - spawn at vampire spawn
            event.setRespawnLocation(gameManager.getVampireSpawn() != null ?
                    gameManager.getVampireSpawn() : event.getRespawnLocation());
        }
    }

    // ===== PLAYER JOIN / QUIT =====

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        gameManager.handlePlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        gameManager.handlePlayerQuit(event.getPlayer());
    }

    // ===== SHOP INTERACTION =====

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() != Material.EMERALD) return;

        String itemName = player.getInventory().getItemInMainHand().getItemMeta() != null
                ? player.getInventory().getItemInMainHand().getItemMeta().getDisplayName() : "";
        if (!itemName.contains("Perk Shop")) return;

        event.setCancelled(true);

        if (gameManager.getState() != GameState.ACTIVE) {
            player.sendMessage(ChatColor.RED + "The shop is only available during an active game!");
            return;
        }

        PerkTeam team = gameManager.isHuman(player.getUniqueId()) ? PerkTeam.HUMAN : PerkTeam.VAMPIRE;
        perkShopGUI.openTierSelection(player, team);
    }

    // ===== ARENA PROTECTION =====

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer() != null) {
            // Protect arena world from ALL players, always
            if (isArenaWorld(event.getPlayer().getWorld())) {
                event.setCancelled(true);
                return;
            }
            if (gameManager.isJoined(event.getPlayer().getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isArenaWorld(event.getPlayer().getWorld())) {
            // Allow Trapper perk cobweb placement
            if (event.getBlock().getType() == Material.COBWEB
                    && com.vampirez.perks.TrapperPerk.isTrapWeb(event.getItemInHand())
                    && gameManager.getState() == GameState.ACTIVE) {
                com.vampirez.perks.TrapperPerk.trackPlacedWeb(event.getBlock(), plugin);
                return;
            }
            event.setCancelled(true);
            return;
        }
        if (gameManager.isJoined(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (gameManager.isJoined(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (gameManager.isJoined(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!gameManager.isJoined(player.getUniqueId())) return;

        // During active game, allow chests (for map loot) and anvils (for enchanted books)
        if (gameManager.getState() == GameState.ACTIVE) {
            InventoryType invType = event.getInventory().getType();
            if (invType == InventoryType.CHEST || invType == InventoryType.ANVIL) {
                return; // Allow interaction
            }
        }

        // Block interaction with real-world containers (chests, barrels, etc.)
        // Plugin GUIs use null holder, real containers have a block holder
        if (event.getInventory().getHolder() != null
                && !(event.getInventory().getHolder() instanceof Player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLobbyInteract(PlayerInteractEvent event) {
        if (!gameManager.isJoined(event.getPlayer().getUniqueId())) return;
        if (gameManager.getState() != GameState.LOBBY && gameManager.getState() != GameState.ENDING) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Material type = event.getClickedBlock().getType();
        // Block opening containers, doors, trapdoors, gates, levers, buttons in lobby
        if (type.name().contains("CHEST") || type.name().contains("BARREL")
                || type.name().contains("SHULKER") || type.name().contains("HOPPER")
                || type.name().contains("DROPPER") || type.name().contains("DISPENSER")
                || type.name().contains("DOOR") || type.name().contains("GATE")
                || type.name().contains("TRAPDOOR") || type.name().contains("LEVER")
                || type.name().contains("BUTTON") || type.name().contains("FURNACE")
                || type.name().contains("ANVIL") || type.name().contains("TABLE")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!gameManager.isJoined(player.getUniqueId())) return;
        if (gameManager.getState() == GameState.LOBBY || gameManager.getState() == GameState.STARTING) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    // ===== VAMPIRE SCOUTING PHASE PROTECTION =====

    @EventHandler(priority = EventPriority.LOWEST)
    public void onVampireInteractDuringScout(PlayerInteractEvent event) {
        if (gameManager.getState() != GameState.ACTIVE) return;
        if (gameManager.isVampiresReleased()) return;
        if (!gameManager.isVampire(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onVampirePickupDuringScout(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (gameManager.getState() != GameState.ACTIVE) return;
        if (gameManager.isVampiresReleased()) return;
        if (!gameManager.isVampire(player.getUniqueId())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onVampireDamageDuringScout(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (gameManager.getState() != GameState.ACTIVE) return;
        if (gameManager.isVampiresReleased()) return;
        if (!gameManager.isVampire(player.getUniqueId())) return;
        event.setCancelled(true);
    }

    // ===== MOB SPAWNING =====

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Allow plugin-spawned mobs (from perks like Undead Horde, Wolf Pack, etc.)
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) return;

        // Block natural mob spawning in the arena world (check ArenaManager first, fallback to humanSpawn)
        if (!isArenaWorld(event.getEntity().getWorld())) return;

        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL
                || event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER
                || event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.REINFORCEMENTS
                || event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.PATROL
                || event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.RAID
                || event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.VILLAGE_INVASION
                || event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.JOCKEY
                || event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CHUNK_GEN
                || event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.MOUNT) {
            event.setCancelled(true);
        }
    }

    // ===== MOB GRIEFING PROTECTION =====

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (isArenaWorld(event.getEntity().getWorld())) {
            event.blockList().clear(); // Allow damage but no block destruction
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Player) return;
        if (isArenaWorld(event.getEntity().getWorld())) {
            event.setCancelled(true); // Block endermen, withers, etc. from changing blocks
        }
    }

    // ===== HELPER =====

    private boolean isArenaWorld(org.bukkit.World world) {
        ArenaManager arenaManager = gameManager.getArenaManager();
        if (arenaManager != null && arenaManager.getArenaWorld() != null) {
            return world.equals(arenaManager.getArenaWorld());
        }
        // Fallback: check against humanSpawn world
        org.bukkit.Location humanSpawn = gameManager.getHumanSpawn();
        return humanSpawn != null && world.equals(humanSpawn.getWorld());
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            return (Player) event.getDamager();
        }
        if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                return (Player) projectile.getShooter();
            }
        }
        return null;
    }
}
