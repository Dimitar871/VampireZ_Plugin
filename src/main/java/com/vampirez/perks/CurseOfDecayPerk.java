package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CurseOfDecayPerk extends Perk {

    // Static so PerkListener can access it for healing reduction
    public static final Map<UUID, Long> cursedPlayers = new ConcurrentHashMap<>();
    private static final long CURSE_DURATION_MS = 6000;
    public static final double HEALING_REDUCTION = 0.50;

    public CurseOfDecayPerk() {
        super("curse_of_decay", "Curse of Decay", PerkTier.PRISMATIC, PerkTeam.VAMPIRE,
                Material.WITHER_SKELETON_SKULL,
                "Attacks curse enemies: 50% reduced healing",
                "for 6s. Refreshes on hit.");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player target)) return;

        cursedPlayers.put(target.getUniqueId(), System.currentTimeMillis());
        target.getWorld().spawnParticle(Particle.WITCH, target.getLocation().add(0, 1.5, 0), 25, 0.3, 0.3, 0.3, 0.02);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1.5, 0), 30, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(48, 0, 48), 1.5f));
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_HUSK_AMBIENT, 0.5f, 0.8f);
        incrementStat(attacker.getUniqueId(), "curses_applied");
        target.sendMessage(ChatColor.DARK_GRAY + "Curse of Decay! Your healing is reduced by 50%!");
    }

    @Override
    public void onTick(Player player) {
        // Cleanup expired curses
        long now = System.currentTimeMillis();
        cursedPlayers.entrySet().removeIf(e -> (now - e.getValue()) > CURSE_DURATION_MS);
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("curses_applied", "Curses Applied");
        return labels;
    }

    public static boolean isCursed(UUID uuid) {
        Long timestamp = cursedPlayers.get(uuid);
        if (timestamp == null) return false;
        if ((System.currentTimeMillis() - timestamp) > CURSE_DURATION_MS) {
            cursedPlayers.remove(uuid);
            return false;
        }
        return true;
    }
}
