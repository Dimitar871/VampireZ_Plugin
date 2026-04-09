package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedHashMap;
import java.util.Map;

public class PlagueDoctorPerk extends Perk {

    private static final PotionEffectType[] NEGATIVE_EFFECTS = {
            PotionEffectType.POISON,
            PotionEffectType.WITHER,
            PotionEffectType.SLOWNESS,
            PotionEffectType.WEAKNESS,
            PotionEffectType.BLINDNESS,
            PotionEffectType.HUNGER,
            PotionEffectType.MINING_FATIGUE,
            PotionEffectType.NAUSEA,
            PotionEffectType.LEVITATION,
            PotionEffectType.BAD_OMEN,
            PotionEffectType.DARKNESS,
            PotionEffectType.UNLUCK,
            PotionEffectType.INSTANT_DAMAGE
    };

    public PlagueDoctorPerk() {
        super("plague_doctor", "Plague Doctor", PerkTier.PRISMATIC, PerkTeam.BOTH,
                Material.BREWING_STAND,
                "Immune to all negative effects",
                "(poison, wither, slow, weakness, blindness,",
                "hunger, fatigue, nausea, levitation, darkness)");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onTick(Player player) {
        for (PotionEffectType effect : NEGATIVE_EFFECTS) {
            if (player.hasPotionEffect(effect)) {
                player.removePotionEffect(effect);
                incrementStat(player.getUniqueId(), "effects_cleansed");
            }
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("effects_cleansed", "Effects Cleansed");
        return labels;
    }
}
