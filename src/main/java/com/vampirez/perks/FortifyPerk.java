package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedHashMap;
import java.util.Map;

public class FortifyPerk extends Perk {

    public FortifyPerk() {
        super("fortify", "Fortify", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.IRON_CHESTPLATE,
                "Crouching grants Resistance I",
                "Stand up to remove the effect");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        player.removePotionEffect(PotionEffectType.RESISTANCE);
    }

    @Override
    public void onTick(Player player) {
        if (player.isSneaking()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 30, 0, false, true), true);
            incrementStat(player.getUniqueId(), "ticks_fortified");
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("ticks_fortified", "Ticks Fortified");
        return labels;
    }
}
