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
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.*;

public class ComboShieldPerk extends Perk {

    private final Map<UUID, Integer> hitStreaks = new HashMap<>();

    public ComboShieldPerk() {
        super("combo_shield", "Combo Shield", PerkTier.SILVER, PerkTeam.BOTH,
                Material.SHIELD,
                "Hit enemies 4 times without",
                "being hit back to gain",
                "3 hearts of absorption shield.");
    }

    @Override
    public void apply(Player player) {
        hitStreaks.put(player.getUniqueId(), 0);
    }

    @Override
    public void remove(Player player) {
        hitStreaks.remove(player.getUniqueId());
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player)) return;

        UUID uuid = attacker.getUniqueId();
        int streak = hitStreaks.getOrDefault(uuid, 0) + 1;

        if (streak >= 4) {
            hitStreaks.put(uuid, 0);

            // Grant 3 hearts (6 HP) absorption
            attacker.setAbsorptionAmount(attacker.getAbsorptionAmount() + 6.0);

            attacker.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, attacker.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.2);
            attacker.playSound(attacker.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.8f, 1.5f);
            attacker.sendMessage(ChatColor.GOLD + "Combo Shield! " + ChatColor.WHITE + "+3 absorption hearts!");
            incrementStat(uuid, "shields_granted");
        } else {
            hitStreaks.put(uuid, streak);
        }
    }

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        // Reset streak when hit
        UUID uuid = victim.getUniqueId();
        int prev = hitStreaks.getOrDefault(uuid, 0);
        if (prev > 0) {
            hitStreaks.put(uuid, 0);
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("shields_granted", "Shields Granted");
        return labels;
    }
}
