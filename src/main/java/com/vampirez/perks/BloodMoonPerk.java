package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class BloodMoonPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> activePlayers = new HashSet<>();
    private final Map<UUID, Long> activeExpiry = new HashMap<>();
    private static final long COOLDOWN_MS = 90000;
    private static final long DURATION_MS = 8000;
    private static final double LIFESTEAL_PERCENT = 0.20;

    public BloodMoonPerk() {
        super("blood_moon", "Blood Moon", PerkTier.PRISMATIC, PerkTeam.VAMPIRE,
                Material.NETHER_STAR,
                "Right-click: allies within 15 blocks get",
                "Speed I + 20% lifesteal for 8s (90s cd)");
    }

    @Override
    public void apply(Player player) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_RED + "Blood Moon" + ChatColor.GRAY + " (Right-Click)");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Empower nearby vampires", ChatColor.YELLOW + "Cooldown: 90s"));
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
    }

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        cooldowns.remove(uuid);
        activePlayers.remove(uuid);
        activeExpiry.remove(uuid);
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.NETHER_STAR && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Blood Moon")) {
                player.getInventory().remove(item);
            }
        }
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.NETHER_STAR || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Blood Moon")) return;

        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(player, COOLDOWN_MS)) {
            player.sendMessage(ChatColor.RED + "Blood Moon on cooldown! " + ((getEffectiveCooldown(player, COOLDOWN_MS) - (now - last)) / 1000 + 1) + "s");
            return;
        }
        cooldowns.put(uuid, now);

        // Dark red particle burst + flames
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 2, 0), 120, 3, 2, 3,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(139, 0, 0), 2.5f));
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 2, 0), 40, 3, 2, 3, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.5f);

        // Buff self
        long expiry = now + DURATION_MS;
        activePlayers.add(uuid);
        activeExpiry.put(uuid, expiry);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 0, false, true));

        // Buff nearby allies
        int alliesBuffed = 0;
        for (Entity entity : player.getNearbyEntities(15, 15, 15)) {
            if (!(entity instanceof Player ally)) continue;
            if (!isSameTeam(player, ally)) continue;

            activePlayers.add(ally.getUniqueId());
            activeExpiry.put(ally.getUniqueId(), expiry);
            ally.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 0, false, true));
            ally.sendMessage(ChatColor.DARK_RED + "Blood Moon! " + player.getName() + " empowers you!");
            alliesBuffed++;
        }

        incrementStat(uuid, "activations");
        addStat(uuid, "allies_buffed", alliesBuffed);

        player.sendMessage(ChatColor.DARK_RED + "Blood Moon activated! You and nearby allies are empowered!");
    }

    @Override
    public void onTick(Player player) {
        // Clean up expired buffs
        long now = System.currentTimeMillis();
        activeExpiry.entrySet().removeIf(e -> {
            if (now > e.getValue()) {
                activePlayers.remove(e.getKey());
                return true;
            }
            return false;
        });
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player)) return;
        if (!activePlayers.contains(attacker.getUniqueId())) return;

        // 20% lifesteal
        double healAmount = event.getDamage() * LIFESTEAL_PERCENT;
        double maxHealth = attacker.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
        double currentHealth = attacker.getHealth();
        double actualHeal = Math.min(healAmount, maxHealth - currentHealth);
        attacker.setHealth(Math.min(currentHealth + healAmount, maxHealth));
        if (actualHeal > 0) {
            addStat(attacker.getUniqueId(), "health_stolen", actualHeal);
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("activations", "Activations");
        labels.put("allies_buffed", "Allies Buffed");
        labels.put("health_stolen", "Health Stolen");
        return labels;
    }
}
