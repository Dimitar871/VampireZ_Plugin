package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkManager;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import com.vampirez.VampireZPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

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
                "Immune to ALL negative effects",
                "(potion effects, fire, bleed, curse,",
                "armor shred, and freeze)");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        boolean hasGoliath = hasPlayerPerk(uuid, "goliath");

        // 1. Remove negative potion effects
        for (PotionEffectType effect : NEGATIVE_EFFECTS) {
            if (player.hasPotionEffect(effect)) {
                // Skip removing Slowness if player has Goliath (it's a self-imposed tradeoff)
                if (effect.equals(PotionEffectType.SLOWNESS) && hasGoliath) {
                    continue;
                }
                player.removePotionEffect(effect);
                incrementStat(uuid, "effects_cleansed");
            }
        }

        // 2. Remove high-amplifier Jump Boost (used by Temporal Shield as a freeze mechanic)
        PotionEffect jumpBoost = player.getPotionEffect(PotionEffectType.JUMP_BOOST);
        if (jumpBoost != null && jumpBoost.getAmplifier() > 10) {
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            incrementStat(uuid, "effects_cleansed");
        }

        // 3. Remove fire ticks (e.g. from Firebrand, Fire Aspect)
        if (player.getFireTicks() > 0) {
            player.setFireTicks(0);
            incrementStat(uuid, "effects_cleansed");
        }

        // 4. Clear Hemophilia bleeding (custom DoT, not a potion effect)
        if (HemophiliaPerk.clearBleedsOn(uuid)) {
            incrementStat(uuid, "effects_cleansed");
        }

        // 5. Clear Curse of Decay (healing reduction, not a potion effect)
        if (CurseOfDecayPerk.cursedPlayers.remove(uuid) != null) {
            incrementStat(uuid, "effects_cleansed");
        }

        // 6. Clear Black Cleaver armor shred stacks
        if (BlackCleaverPerk.cleavedTargets.remove(uuid) != null) {
            BlackCleaverPerk.cleavedExpiry.remove(uuid);
            incrementStat(uuid, "effects_cleansed");
        }
    }

    private boolean hasPlayerPerk(UUID uuid, String perkId) {
        VampireZPlugin plugin = (VampireZPlugin) Bukkit.getPluginManager().getPlugin("VampireZ");
        if (plugin != null && plugin.getGameManager() != null) {
            PerkManager pm = plugin.getGameManager().getPerkManager();
            return pm.hasPerk(uuid, perkId);
        }
        return false;
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("effects_cleansed", "Effects Cleansed");
        return labels;
    }
}
