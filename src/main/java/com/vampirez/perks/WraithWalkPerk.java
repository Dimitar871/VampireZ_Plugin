package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
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

public class WraithWalkPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> ghostPlayers = new HashSet<>();
    private final Map<UUID, ItemStack[]> storedArmor = new HashMap<>();
    private static final long COOLDOWN_MS = 35000;
    private static final long GHOST_DURATION_TICKS = 80L; // 4 seconds

    public WraithWalkPerk() {
        super("wraith_walk", "Wraith Walk", PerkTier.PRISMATIC, PerkTeam.VAMPIRE,
                Material.ECHO_SHARD,
                "Right-click: 4s ghost form (invulnerable, can't attack)",
                "On expiry: AoE Blindness + Slow (35s cd)");
    }

    @Override
    public void apply(Player player) {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_GRAY + "Wraith Walk" + ChatColor.GRAY + " (Right-Click)");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Ghost form for 4s", ChatColor.YELLOW + "Cooldown: 35s"));
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
    }

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        cooldowns.remove(uuid);
        ghostPlayers.remove(uuid);
        // Restore armor if stored
        if (storedArmor.containsKey(uuid)) {
            player.getInventory().setArmorContents(storedArmor.remove(uuid));
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.ECHO_SHARD && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Wraith Walk")) {
                player.getInventory().remove(item);
            }
        }
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.ECHO_SHARD || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Wraith Walk")) return;

        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(player, COOLDOWN_MS)) {
            player.sendMessage(ChatColor.RED + "Wraith Walk on cooldown! " + ((getEffectiveCooldown(player, COOLDOWN_MS) - (now - last)) / 1000 + 1) + "s");
            return;
        }
        cooldowns.put(uuid, now);
        ghostPlayers.add(uuid);

        // Store and hide armor
        storedArmor.put(uuid, player.getInventory().getArmorContents().clone());
        player.getInventory().setArmorContents(new ItemStack[4]);

        // Apply ghost form effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, (int)GHOST_DURATION_TICKS, 0, false, false));
        player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0), 60, 0.5, 1, 0.5, 0.05);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 40, 0.5, 1, 0.5, 0,
                new Particle.DustOptions(Color.GRAY, 1.5f));
        player.playSound(player.getLocation(), Sound.ENTITY_VEX_AMBIENT, 1.0f, 0.5f);
        player.sendMessage(ChatColor.DARK_GRAY + "Wraith Walk! You become a ghost...");

        incrementStat(uuid, "activations");

        // Schedule expiry AoE + armor restore
        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            ghostPlayers.remove(uuid);

            // Restore armor
            if (storedArmor.containsKey(uuid)) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    p.getInventory().setArmorContents(storedArmor.remove(uuid));
                } else {
                    storedArmor.remove(uuid);
                    return;
                }
            }

            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) return;

            p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, p.getLocation().add(0, 1, 0), 80, 2, 1, 2, 0.05);
            p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 50, 2, 1, 2, 0,
                    new Particle.DustOptions(Color.PURPLE, 1.5f));
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.8f, 1.5f);

            int enemiesHit = 0;
            for (Entity entity : p.getNearbyEntities(5, 5, 5)) {
                if (!(entity instanceof Player target)) continue;
                if (isSameTeam(p, target)) continue;

                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, true));
                target.sendMessage(ChatColor.DARK_GRAY + "A wraith haunts you!");
                enemiesHit++;
            }
            addStat(uuid, "enemies_debuffed", enemiesHit);
            p.sendMessage(ChatColor.DARK_GRAY + "Wraith Walk expires with a chilling blast!");
        }, GHOST_DURATION_TICKS);
    }

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        if (ghostPlayers.contains(victim.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (ghostPlayers.contains(attacker.getUniqueId())) {
            event.setCancelled(true);
            attacker.sendMessage(ChatColor.GRAY + "You can't attack in ghost form!");
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("activations", "Times Activated");
        labels.put("enemies_debuffed", "Enemies Debuffed");
        return labels;
    }
}
