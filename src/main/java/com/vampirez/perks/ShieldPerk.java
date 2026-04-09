package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class ShieldPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 45000;

    public ShieldPerk() {
        super("shield", "Shield", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.SHIELD,
                "Right-click sword for 3 absorption",
                "hearts (4s). 45s cooldown.");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Material hand = player.getInventory().getItemInMainHand().getType();
        if (!hand.name().endsWith("_SWORD")) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(player, COOLDOWN_MS)) {
            player.sendMessage(ChatColor.RED + "Shield on cooldown! " + ((getEffectiveCooldown(player, COOLDOWN_MS) - (now - last)) / 1000 + 1) + "s");
            return;
        }
        cooldowns.put(uuid, now);

        // 3 absorption hearts = 6 HP absorption, 80 ticks = 4 seconds
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 80, 1, false, true));
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);
        player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.2f);
        player.sendMessage(ChatColor.GOLD + "Shield activated! Absorption hearts granted.");
        incrementStat(uuid, "activations");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("activations", "Times Activated");
        return labels;
    }
}
