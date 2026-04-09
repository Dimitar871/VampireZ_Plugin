package com.vampirez.perks;

import com.vampirez.EconomyManager;
import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import com.vampirez.VampireZPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;

public class BountyHunterPerk extends Perk {

    public BountyHunterPerk() {
        super("bounty_hunter", "Bounty Hunter", PerkTier.GOLD, PerkTeam.BOTH,
                Material.GOLD_INGOT,
                "Kills grant +15 bonus gold",
                "on top of the normal kill reward");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onKill(Player killer, Player victim) {
        EconomyManager econ = ((VampireZPlugin) getPlugin()).getGameManager().getEconomyManager();
        econ.addGold(killer.getUniqueId(), 15);
        killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        killer.sendMessage(ChatColor.GOLD + "+15 bonus gold (Bounty Hunter)!");
        addStat(killer.getUniqueId(), "bonus_gold", 15);
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("bonus_gold", "Bonus Gold");
        return labels;
    }
}
