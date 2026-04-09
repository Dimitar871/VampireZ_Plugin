package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedHashMap;
import java.util.Map;

public class MartyrPerk extends Perk {

    public MartyrPerk() {
        super("martyr", "Martyr", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.TOTEM_OF_UNDYING,
                "When you die, allies within 10 blocks",
                "heal 4 hearts + Absorption II for 5s");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDeath(Player player, PlayerDeathEvent event) {
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 100, 1, 1, 1, 0.3);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0,
                new Particle.DustOptions(Color.fromRGB(255, 215, 0), 2.0f));
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.8f, 1.2f);

        int alliesHealed = 0;
        for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
            if (!(entity instanceof Player ally)) continue;
            if (!isSameTeam(player, ally)) continue;

            double maxHealth = ally.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
            ally.setHealth(Math.min(ally.getHealth() + 8.0, maxHealth));
            ally.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1, false, true));
            ally.sendMessage(ChatColor.GOLD + "Martyr! " + player.getName() + "'s sacrifice heals you!");
            alliesHealed++;
        }
        incrementStat(player.getUniqueId(), "activations");
        addStat(player.getUniqueId(), "allies_healed", alliesHealed);
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("activations", "Activations");
        labels.put("allies_healed", "Allies Healed");
        return labels;
    }
}
