package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class RegenerativePerk extends Perk {

    private final Map<UUID, List<BukkitTask>> activeTasks = new HashMap<>();

    public RegenerativePerk() {
        super("regenerative", "Regenerative", PerkTier.SILVER, PerkTeam.BOTH,
                Material.GHAST_TEAR,
                "When damaged, regenerate 30%",
                "of the damage taken over 10 seconds.");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        List<BukkitTask> tasks = activeTasks.remove(uuid);
        if (tasks != null) {
            tasks.forEach(BukkitTask::cancel);
        }
    }

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        UUID uuid = victim.getUniqueId();
        double totalHeal = event.getDamage() * 0.30;

        // Heal in 5 pulses over 10 seconds (every 2 seconds = 40 ticks)
        int pulses = 5;
        double healPerPulse = totalHeal / pulses;
        int[] count = {0};

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(getPlugin(), () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline() || player.isDead()) {
                count[0] = pulses; // force stop
            }

            if (count[0] < pulses && player != null) {
                double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
                if (player.getHealth() < maxHealth) {
                    player.setHealth(Math.min(player.getHealth() + healPerPulse, maxHealth));
                    player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 2, 0.3, 0.2, 0.3, 0);
                    addStat(uuid, "health_regenerated", healPerPulse);
                }
            }

            count[0]++;
        }, 40L, 40L);

        // Auto-cancel after all pulses via a delayed task
        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            task.cancel();
            List<BukkitTask> tasks = activeTasks.get(uuid);
            if (tasks != null) tasks.remove(task);
        }, 40L * (pulses + 1));

        activeTasks.computeIfAbsent(uuid, k -> new ArrayList<>()).add(task);
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("health_regenerated", "HP Regenerated");
        return labels;
    }
}
