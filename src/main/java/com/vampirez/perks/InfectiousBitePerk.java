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
import org.bukkit.entity.Player;

import java.util.*;

public class InfectiousBitePerk extends Perk {

    private static final double HEAL_AMOUNT = 8.0; // 4 hearts
    private static final double RADIUS = 8.0;

    public InfectiousBitePerk() {
        super("infectious_bite", "Infectious Bite", PerkTier.SILVER, PerkTeam.VAMPIRE,
                Material.SPIDER_EYE,
                "On kill, heal all nearby vampires",
                "(8 block radius) for 4 hearts.");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onKill(Player killer, Player victim) {
        UUID uuid = killer.getUniqueId();
        int healed = 0;

        for (Player nearby : Bukkit.getOnlinePlayers()) {
            if (nearby.getUniqueId().equals(uuid)) continue;
            if (!isSameTeam(killer, nearby)) continue;
            if (nearby.getLocation().distance(killer.getLocation()) > RADIUS) continue;

            double maxHealth = nearby.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
            nearby.setHealth(Math.min(nearby.getHealth() + HEAL_AMOUNT, maxHealth));
            nearby.getWorld().spawnParticle(Particle.HEART, nearby.getLocation().add(0, 2, 0), 5, 0.3, 0.3, 0.3, 0);
            nearby.playSound(nearby.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
            nearby.sendMessage(ChatColor.DARK_RED + killer.getName() + "'s kill healed you for 4 hearts!");
            healed++;
        }

        if (healed > 0) {
            killer.sendMessage(ChatColor.DARK_RED + "Infectious Bite: Healed " + healed + " nearby vampire" + (healed > 1 ? "s" : "") + "!");
            addStat(uuid, "vampires_healed", healed);
        }
        incrementStat(uuid, "kills");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("kills", "Kills");
        labels.put("vampires_healed", "Vampires Healed");
        return labels;
    }
}
