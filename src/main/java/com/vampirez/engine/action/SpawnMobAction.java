package com.vampirez.engine.action;

import com.vampirez.GameManager;
import com.vampirez.VampireZPlugin;
import com.vampirez.engine.HookContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;
import java.util.UUID;

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
    private final NamedTextColor nameColor;
    private final String team;
    private final boolean noBaby;
    private final boolean angry;
    private final boolean tamed;
    private final Material equipmentMainHand;
    private final Material equipmentHelmet;
    private final double spreadX;

    public SpawnMobAction(EntityType entityType, int count, long lifetimeTicks,
                          String customName, NamedTextColor nameColor, String team,
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
                Component name = Component.text(owner.getName() + "'s " + customName)
                        .color(nameColor != null ? nameColor : NamedTextColor.WHITE);
                entity.customName(name);
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
                mob.setRemoveWhenFarAway(false);
                // Force immediate aggression — vanilla AI scanning can take 1-3s otherwise,
                // wasting a chunk of the mob's lifetime standing idle.
                aggroNearestEnemy(mob, owner, plugin);
            }

            if (lifetimeTicks > 0) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!entity.isDead()) entity.remove();
                }, lifetimeTicks);
            }
        }
    }

    /**
     * Force the mob to attack the nearest enemy player of the owner's opposing team.
     * Searches within 24 blocks; falls back to no-op if no enemy is online or in range.
     * Also briefly buffs Speed I so the mob actually closes the distance fast.
     *
     * <p>Public + static so other spawnable perks (e.g. {@code SummonerPerk}) can reuse it.
     */
    public static void aggroNearestEnemy(Mob mob, Player owner, JavaPlugin plugin) {
        if (!(plugin instanceof VampireZPlugin vzPlugin)) return;
        GameManager gm = vzPlugin.getGameManager();
        if (gm == null) return;

        boolean ownerIsVampire = gm.isVampire(owner.getUniqueId());
        Set<UUID> enemyTeam = ownerIsVampire ? gm.getHumanTeam() : gm.getVampireTeam();
        if (enemyTeam == null || enemyTeam.isEmpty()) return;

        Player nearest = null;
        double nearestDistSq = 24 * 24; // max aggro range
        Location mobLoc = mob.getLocation();
        for (UUID uuid : enemyTeam) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) continue;
            if (!p.getWorld().equals(mob.getWorld())) continue;
            double distSq = p.getLocation().distanceSquared(mobLoc);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = p;
            }
        }

        if (nearest != null) {
            mob.setTarget(nearest);
            // Brief Speed I so the mob actually closes — Zombies/Skeletons walk slowly.
            mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0, false, false));
        }
    }
}
