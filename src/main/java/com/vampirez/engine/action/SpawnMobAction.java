package com.vampirez.engine.action;

import com.vampirez.engine.HookContext;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Spawns N mobs around the owner, with optional name, team metadata, equipment, taming, and
 * a scheduled removal after the lifetime expires.
 *
 * Spread: mobs alternate left/right of the player by {@code spreadX} blocks.
 */
public class SpawnMobAction implements Action {

    private final EntityType entityType;
    private final int count;
    private final long lifetimeTicks;
    private final String customName;
    private final ChatColor nameColor;
    private final String team;
    private final boolean noBaby;
    private final boolean angry;
    private final boolean tamed;
    private final Material equipmentMainHand;
    private final Material equipmentHelmet;
    private final double spreadX;

    public SpawnMobAction(EntityType entityType, int count, long lifetimeTicks,
                          String customName, ChatColor nameColor, String team,
                          boolean noBaby, boolean angry, boolean tamed,
                          Material equipmentMainHand, Material equipmentHelmet, double spreadX) {
        this.entityType = entityType;
        this.count = count;
        this.lifetimeTicks = lifetimeTicks;
        this.customName = customName;
        this.nameColor = nameColor;
        this.team = team;
        this.noBaby = noBaby;
        this.angry = angry;
        this.tamed = tamed;
        this.equipmentMainHand = equipmentMainHand;
        this.equipmentHelmet = equipmentHelmet;
        this.spreadX = spreadX;
    }

    @Override
    public void run(HookContext ctx) {
        Player owner = ctx.owner;
        if (owner == null || entityType == null) return;
        JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin("VampireZ");
        if (plugin == null) return;

        for (int i = 0; i < count; i++) {
            double offsetX = (i % 2 == 0 ? spreadX : -spreadX);
            Location loc = owner.getLocation().add(offsetX, 0, 0);
            Entity spawned = owner.getWorld().spawnEntity(loc, entityType);
            if (!(spawned instanceof LivingEntity)) continue;
            LivingEntity entity = (LivingEntity) spawned;

            if (customName != null) {
                entity.setCustomName((nameColor != null ? nameColor.toString() : "")
                        + owner.getName() + "'s " + customName);
                entity.setCustomNameVisible(true);
            }
            if (team != null) {
                entity.setMetadata("vampirez_team", new FixedMetadataValue(plugin, team));
            }
            if (noBaby && entity instanceof Zombie z) z.setBaby(false);
            if (entity instanceof Wolf w) {
                if (tamed) {
                    w.setTamed(true);
                    w.setOwner(owner);
                }
                if (angry) w.setAngry(true);
            }
            if (entity.getEquipment() != null) {
                if (equipmentMainHand != null) {
                    entity.getEquipment().setItemInMainHand(new ItemStack(equipmentMainHand));
                    entity.getEquipment().setItemInMainHandDropChance(0);
                }
                if (equipmentHelmet != null) {
                    entity.getEquipment().setHelmet(new ItemStack(equipmentHelmet));
                    entity.getEquipment().setHelmetDropChance(0);
                }
            }
            // Make the mob target enemies of owner's team. Mobs use vampirez_team metadata to
            // decide friend/foe via existing GameListener mob-targeting logic.
            if (entity instanceof Mob mob) {
                // No explicit target — GameListener already wires mob aggression by team metadata.
                // Just leave the mob to behave naturally.
                mob.setRemoveWhenFarAway(false);
            }

            if (lifetimeTicks > 0) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!entity.isDead()) entity.remove();
                }, lifetimeTicks);
            }
        }
    }
}
