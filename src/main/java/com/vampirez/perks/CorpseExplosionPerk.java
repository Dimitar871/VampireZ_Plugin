package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

public class CorpseExplosionPerk extends Perk {

    public CorpseExplosionPerk() {
        super("corpse_explosion", "Corpse Explosion", PerkTier.GOLD, PerkTeam.VAMPIRE,
                Material.TNT,
                "On kill, victim explodes after 1s",
                "3 hearts AoE within 5 blocks to enemies");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onKill(Player killer, Player victim) {
        Location deathLoc = victim.getLocation().clone();
        UUID killerUUID = killer.getUniqueId();

        incrementStat(killerUUID, "explosions");

        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            deathLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, deathLoc, 1, 0, 0, 0, 0);
            deathLoc.getWorld().spawnParticle(Particle.FLAME, deathLoc, 60, 2, 1, 2, 0.1);
            deathLoc.getWorld().spawnParticle(Particle.DUST, deathLoc, 40, 2, 1, 2, 0,
                    new Particle.DustOptions(org.bukkit.Color.RED, 1.5f));
            deathLoc.getWorld().playSound(deathLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

            Player killerPlayer = Bukkit.getPlayer(killerUUID);
            int playersHit = 0;
            for (Entity entity : deathLoc.getWorld().getNearbyEntities(deathLoc, 5, 5, 5)) {
                if (!(entity instanceof Player target)) continue;
                if (target.getUniqueId().equals(killerUUID)) continue;
                if (killerPlayer != null && isSameTeam(killerPlayer, target)) continue;

                target.damage(6.0); // 3 hearts
                target.sendMessage(ChatColor.RED + "Corpse Explosion!");
                playersHit++;
            }
            if (playersHit > 0) {
                addStat(killerUUID, "players_hit", playersHit);
            }
        }, 20L);
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("explosions", "Explosions");
        labels.put("players_hit", "Players Hit");
        return labels;
    }
}
