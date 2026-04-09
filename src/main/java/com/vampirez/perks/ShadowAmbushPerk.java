package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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

public class ShadowAmbushPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> stealthActive = new HashSet<>();
    private static final long COOLDOWN_MS = 30000;

    public ShadowAmbushPerk() {
        super("shadow_ambush", "Shadow Ambush", PerkTier.GOLD, PerkTeam.VAMPIRE,
                Material.FLINT,
                "Right-click flint for 3s invisibility.",
                "Next melee hit deals +50% damage",
                "and breaks invis. 30s cooldown.");
    }

    @Override
    public void apply(Player player) {
        ItemStack item = new ItemStack(Material.FLINT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_GRAY + "Shadow Ambush" + ChatColor.GRAY + " (Right-Click)");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "3s Invisibility + Bonus Damage", ChatColor.YELLOW + "Cooldown: 30s"));
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
    }

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        cooldowns.remove(uuid);
        stealthActive.remove(uuid);
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.FLINT && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Shadow Ambush")) {
                player.getInventory().remove(item);
            }
        }
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.FLINT || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Shadow Ambush")) return;

        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(player, COOLDOWN_MS)) {
            player.sendMessage(ChatColor.RED + "Shadow Ambush on cooldown! " + ((getEffectiveCooldown(player, COOLDOWN_MS) - (now - last)) / 1000 + 1) + "s");
            return;
        }
        cooldowns.put(uuid, now);
        stealthActive.add(uuid);

        // 3s invisibility = 60 ticks
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0, false, false));
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.02);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.5f);
        player.sendMessage(ChatColor.DARK_GRAY + "You vanish into the shadows...");
        incrementStat(uuid, "activations");

        // Auto-remove stealth flag after 3s if not broken by attack
        org.bukkit.Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            stealthActive.remove(uuid);
        }, 60L);
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        UUID uuid = attacker.getUniqueId();
        if (!stealthActive.contains(uuid)) return;

        // Only melee (damager is the player directly, not a projectile)
        if (event.getDamager() != attacker) return;

        // +50% damage
        event.setDamage(event.getDamage() * 1.5);
        stealthActive.remove(uuid);
        attacker.removePotionEffect(PotionEffectType.INVISIBILITY);

        attacker.getWorld().spawnParticle(Particle.ENCHANTED_HIT, victim.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.1);
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.8f);
        attacker.sendMessage(ChatColor.DARK_GRAY + "Shadow Ambush! +50% damage!");
        incrementStat(uuid, "ambush_hits");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("activations", "Times Activated");
        labels.put("ambush_hits", "Ambush Hits");
        return labels;
    }
}
