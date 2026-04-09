package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedHashMap;
import java.util.Map;

public class RallyCryPerk extends Perk {

    public RallyCryPerk() {
        super("rally_cry", "Rally Cry", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.GOAT_HORN,
                "On kill, allies within 8 blocks",
                "get Speed I + Regen I for 4s");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onKill(Player killer, Player victim) {
        killer.getWorld().playSound(killer.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_0, 1.0f, 1.2f);
        killer.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, killer.getLocation().add(0, 2, 0), 40, 2, 1, 2, 0.1);
        killer.getWorld().spawnParticle(Particle.DUST, killer.getLocation().add(0, 2, 0), 30, 2, 1, 2, 0,
                new Particle.DustOptions(Color.GREEN, 1.5f));

        int alliesBuffed = 0;
        for (Entity entity : killer.getNearbyEntities(8, 8, 8)) {
            if (!(entity instanceof Player ally)) continue;
            if (!isSameTeam(killer, ally)) continue;

            ally.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 80, 0, false, true));
            ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 80, 0, false, true));
            ally.sendMessage(ChatColor.GREEN + "Rally Cry! " + killer.getName() + " inspires you!");
            alliesBuffed++;
        }
        incrementStat(killer.getUniqueId(), "activations");
        addStat(killer.getUniqueId(), "allies_buffed", alliesBuffed);
        killer.sendMessage(ChatColor.GREEN + "Rally Cry! Your kill inspires nearby allies!");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("activations", "Activations");
        labels.put("allies_buffed", "Allies Buffed");
        return labels;
    }
}
