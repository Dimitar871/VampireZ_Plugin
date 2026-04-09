package com.vampirez.perks;

import com.vampirez.EconomyManager;
import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import com.vampirez.VampireZPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GoldenGuardPerk extends Perk {

    private final Set<UUID> used = new HashSet<>();

    public GoldenGuardPerk() {
        super("golden_guard", "Golden Guard", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.GOLD_BLOCK,
                "Fatal damage: spend 30 gold to",
                "survive at 2 hearts (once per life)");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        used.remove(player.getUniqueId());
    }

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        UUID uuid = victim.getUniqueId();
        if (used.contains(uuid)) return;

        double healthAfter = victim.getHealth() - event.getFinalDamage();
        if (healthAfter <= 0) {
            EconomyManager econ = ((VampireZPlugin) getPlugin()).getGameManager().getEconomyManager();
            if (econ.removeGold(uuid, 30)) {
                event.setCancelled(true);
                used.add(uuid);
                victim.setHealth(4.0);
                victim.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, victim.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0.2);
                victim.playSound(victim.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
                victim.sendMessage(ChatColor.GOLD + "Golden Guard! Spent 30 gold to survive!");
                incrementStat(uuid, "saves");
            }
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("saves", "Saves");
        return labels;
    }
}
