package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class DecoyPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 120000;
    private final Random random = new Random();

    public DecoyPerk() {
        super("decoy", "Decoy", PerkTier.PRISMATIC, PerkTeam.BOTH,
                Material.ARMOR_STAND,
                "At 20% HP: spawn 4 clones + go",
                "fully invisible 2s (120s cd).");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        UUID uuid = victim.getUniqueId();

        // Check HP after damage
        double healthAfter = victim.getHealth() - event.getFinalDamage();
        double maxHealth = victim.getAttribute(Attribute.MAX_HEALTH).getValue();

        if (healthAfter > maxHealth * 0.2) return;
        if (healthAfter <= 0) return;

        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(victim, COOLDOWN_MS)) return;

        cooldowns.put(uuid, now);

        // Spawn 4 decoy armor stands with player's armor
        List<ArmorStand> decoys = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            ArmorStand decoy = victim.getWorld().spawn(victim.getLocation(), ArmorStand.class);
            decoy.setCustomName(victim.getDisplayName());
            decoy.setCustomNameVisible(true);
            decoy.setArms(true);
            decoy.setBasePlate(false);

            // Copy armor to decoy
            if (victim.getInventory().getHelmet() != null)
                decoy.getEquipment().setHelmet(victim.getInventory().getHelmet().clone());
            if (victim.getInventory().getChestplate() != null)
                decoy.getEquipment().setChestplate(victim.getInventory().getChestplate().clone());
            if (victim.getInventory().getLeggings() != null)
                decoy.getEquipment().setLeggings(victim.getInventory().getLeggings().clone());
            if (victim.getInventory().getBoots() != null)
                decoy.getEquipment().setBoots(victim.getInventory().getBoots().clone());

            // Give main hand item
            ItemStack mainHand = victim.getInventory().getItemInMainHand();
            if (mainHand.getType() != Material.AIR) {
                decoy.getEquipment().setItemInMainHand(mainHand.clone());
            }

            // Launch decoy in evenly spread direction
            double angle = (2 * Math.PI / 4) * i + random.nextDouble() * 0.5;
            decoy.setVelocity(new Vector(Math.cos(angle) * 0.5, 0.3, Math.sin(angle) * 0.5));
            decoys.add(decoy);
        }

        // Make player fully invisible: potion + temporarily hide armor
        victim.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0, false, false), true);

        // Snapshot current armor/toughness values before removing armor pieces
        AttributeInstance armorAttr = victim.getAttribute(Attribute.ARMOR);
        AttributeInstance toughnessAttr = victim.getAttribute(Attribute.ARMOR_TOUGHNESS);
        double armorValue = armorAttr != null ? armorAttr.getValue() : 0;
        double toughnessValue = toughnessAttr != null ? toughnessAttr.getValue() : 0;

        // Store and remove armor so it's not visible during invisibility
        ItemStack savedHelmet = victim.getInventory().getHelmet();
        ItemStack savedChest = victim.getInventory().getChestplate();
        ItemStack savedLegs = victim.getInventory().getLeggings();
        ItemStack savedBoots = victim.getInventory().getBoots();
        victim.getInventory().setHelmet(null);
        victim.getInventory().setChestplate(null);
        victim.getInventory().setLeggings(null);
        victim.getInventory().setBoots(null);

        // Add attribute modifiers to keep the same defense without visible armor
        UUID armorModId = UUID.randomUUID();
        UUID toughnessModId = UUID.randomUUID();
        AttributeModifier armorMod = new AttributeModifier(armorModId, "decoy_armor", armorValue,
                AttributeModifier.Operation.ADD_NUMBER);
        AttributeModifier toughnessMod = new AttributeModifier(toughnessModId, "decoy_toughness", toughnessValue,
                AttributeModifier.Operation.ADD_NUMBER);
        if (armorAttr != null) armorAttr.addModifier(armorMod);
        if (toughnessAttr != null) toughnessAttr.addModifier(toughnessMod);

        // Effects
        victim.getWorld().spawnParticle(Particle.CLOUD, victim.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        victim.playSound(victim.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f);
        victim.sendMessage(ChatColor.LIGHT_PURPLE + "4 Decoys deployed! You're fully invisible for 2s!");

        // Restore armor + remove attribute modifiers after 2 seconds (40 ticks)
        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Remove temporary defense modifiers
                AttributeInstance a = player.getAttribute(Attribute.ARMOR);
                AttributeInstance t = player.getAttribute(Attribute.ARMOR_TOUGHNESS);
                if (a != null) a.removeModifier(armorMod);
                if (t != null) t.removeModifier(toughnessMod);
                // Restore armor pieces
                player.getInventory().setHelmet(savedHelmet);
                player.getInventory().setChestplate(savedChest);
                player.getInventory().setLeggings(savedLegs);
                player.getInventory().setBoots(savedBoots);
            }
        }, 40L);

        // Remove decoys after 3 seconds
        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            for (ArmorStand decoy : decoys) {
                if (!decoy.isDead()) {
                    decoy.getWorld().spawnParticle(Particle.LARGE_SMOKE, decoy.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.02);
                    decoy.remove();
                }
            }
        }, 60L);

        incrementStat(uuid, "decoys");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("decoys", "Decoys Deployed");
        return labels;
    }
}
