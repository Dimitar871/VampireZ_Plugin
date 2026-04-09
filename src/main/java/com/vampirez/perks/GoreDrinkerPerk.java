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
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

public class GoreDrinkerPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 20000;
    private static final double AOE_RADIUS = 5.0;
    private static final double DAMAGE_PER_HIT = 2.0; // 1 heart
    private static final double HEAL_PER_HIT = 3.0;   // 1.5 hearts

    public GoreDrinkerPerk() {
        super("gore_drinker", "Gore Drinker", PerkTier.GOLD, PerkTeam.BOTH,
                Material.NETHERITE_SWORD,
                "Right-click your sword: slash all enemies",
                "within 5 blocks for 1 heart each.",
                "Heal 1.5 hearts per enemy hit. (20s cd)");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        // Must be holding a sword
        Material held = player.getInventory().getItemInMainHand().getType();
        if (!held.name().contains("SWORD")) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        long effectiveCd = getEffectiveCooldown(player, COOLDOWN_MS);
        if (last != null && (now - last) < effectiveCd) {
            long remaining = (effectiveCd - (now - last)) / 1000 + 1;
            player.sendMessage(ChatColor.RED + "Gore Drinker on cooldown! " + remaining + "s");
            return;
        }
        cooldowns.put(uuid, now);

        // Blood vortex AoE slash
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 20, 2.5, 0.3, 2.5, 0);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 40, 2.5, 0.8, 2.5, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(180, 0, 0), 1.8f));
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 0.5, 0), 25, 2.0, 0.3, 2.0, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(100, 0, 0), 1.2f));
        player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 0.8, 0), 8, 2.0, 0.3, 2.0, 0.01);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);

        int hits = 0;
        for (Entity entity : player.getNearbyEntities(AOE_RADIUS, AOE_RADIUS, AOE_RADIUS)) {
            if (!(entity instanceof Player target)) continue;
            if (isSameTeam(player, target)) continue;

            target.damage(DAMAGE_PER_HIT);
            hits++;
        }

        if (hits > 0) {
            double heal = hits * HEAL_PER_HIT;
            double newHealth = Math.min(player.getHealth() + heal, player.getMaxHealth());
            player.setHealth(newHealth);
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), hits * 2, 0.5, 0.3, 0.5, 0);
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.8f, 1.2f);
        }

        incrementStat(uuid, "activations");
        addStat(uuid, "hits", hits);
        addStat(uuid, "healing", hits * HEAL_PER_HIT);
        player.sendMessage(ChatColor.DARK_RED + "Gore Drinker! Hit " + hits + " enemies, healed " + String.format("%.1f", hits * HEAL_PER_HIT / 2.0) + " hearts!");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("activations", "Activations");
        labels.put("hits", "Enemies Hit");
        labels.put("healing", "HP Healed");
        return labels;
    }
}
