package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import com.vampirez.VampireZPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class IronGuardianPerk extends Perk {

    public IronGuardianPerk() {
        super("iron_guardian", "Iron Guardian", PerkTier.PRISMATIC, PerkTeam.HUMAN,
                Material.IRON_BLOCK,
                "On vampire kill: summon an Iron Golem",
                "that fights for you (12s)");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onKill(Player killer, Player victim) {
        incrementStat(killer.getUniqueId(), "summons");

        IronGolem golem = (IronGolem) killer.getWorld().spawnEntity(
                killer.getLocation().add(2, 0, 0), EntityType.IRON_GOLEM);
        golem.setCustomName(ChatColor.GREEN + killer.getName() + "'s Golem");
        golem.setCustomNameVisible(true);
        golem.setMetadata("vampirez_team", new FixedMetadataValue(getPlugin(), "HUMAN"));

        // Periodically retarget nearest vampire (iron golems don't naturally attack players)
        VampireZPlugin vPlugin = (VampireZPlugin) getPlugin();
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks += 40;
                if (golem.isDead() || ticks > 240) {
                    if (!golem.isDead()) {
                        golem.getWorld().spawnParticle(Particle.BLOCK, golem.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0,
                                Material.IRON_BLOCK.createBlockData());
                        golem.remove();
                    }
                    cancel();
                    return;
                }
                // Find nearest vampire player and set as target
                Player nearest = null;
                double nearestDist = Double.MAX_VALUE;
                for (UUID vampUUID : vPlugin.getGameManager().getVampireTeam()) {
                    Player vamp = Bukkit.getPlayer(vampUUID);
                    if (vamp != null && vamp.isOnline() && vamp.getWorld().equals(golem.getWorld())) {
                        double dist = vamp.getLocation().distanceSquared(golem.getLocation());
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearest = vamp;
                        }
                    }
                }
                if (nearest != null) {
                    golem.setTarget(nearest);
                }
            }
        }.runTaskTimer(getPlugin(), 1L, 40L);

        killer.getWorld().spawnParticle(Particle.EXPLOSION, golem.getLocation().add(0, 1, 0), 3, 0.5, 0.5, 0.5, 0);
        killer.playSound(killer.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, 1.0f, 0.8f);
        killer.sendMessage(ChatColor.GREEN + "Iron Guardian summoned!");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("summons", "Summons");
        return labels;
    }
}
