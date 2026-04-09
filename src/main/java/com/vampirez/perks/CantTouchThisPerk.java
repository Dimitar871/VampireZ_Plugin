package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class CantTouchThisPerk extends Perk {

    private final Map<UUID, Integer> tickCounters = new HashMap<>();
    private final Map<UUID, BukkitTask> particleTasks = new HashMap<>();
    private static final int COOLDOWN_TICKS = 30; // 30 seconds (called every 20 ticks)

    public CantTouchThisPerk() {
        super("cant_touch_this", "Can't Touch This", PerkTier.PRISMATIC, PerkTeam.HUMAN,
                Material.TOTEM_OF_UNDYING,
                "8s invulnerability every 30s",
                "White particles when active");
    }

    @Override
    public void apply(Player player) {
        tickCounters.put(player.getUniqueId(), 0);
    }

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        tickCounters.remove(uuid);
        BukkitTask task = particleTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        int count = tickCounters.getOrDefault(uuid, 0) + 1;
        tickCounters.put(uuid, count);

        if (count >= COOLDOWN_TICKS) {
            tickCounters.put(uuid, 0);
            // Grant invulnerability (Resistance V = near-immune) for 8 seconds
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 160, 4, false, true));
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 80, 1.5, 2, 1.5, 0,
                    new Particle.DustOptions(Color.WHITE, 2.0f));
            player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.5f, 1.5f);

            // Start particle ring task (every 5 ticks = 4x per second)
            BukkitTask oldTask = particleTasks.remove(uuid);
            if (oldTask != null) oldTask.cancel();

            BukkitTask task = Bukkit.getScheduler().runTaskTimer(getPlugin(), () -> {
                if (!player.isOnline()) return;
                double radius = 1.5;
                for (int i = 0; i < 12; i++) {
                    double angle = (2 * Math.PI / 12) * i;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    player.getWorld().spawnParticle(Particle.DUST,
                            player.getLocation().add(x, 0.5, z), 2, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.WHITE, 1.5f));
                }
            }, 0L, 5L);
            particleTasks.put(uuid, task);

            // Cancel particle task after 160 ticks (8s)
            Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
                BukkitTask t = particleTasks.remove(uuid);
                if (t != null) t.cancel();
            }, 160L);

            incrementStat(uuid, "activations");
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("activations", "Times Activated");
        return labels;
    }
}
