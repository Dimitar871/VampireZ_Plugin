package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HarmingPotionsPerk extends Perk {

    private final Map<UUID, Long> lastRegen = new HashMap<>();
    private static final long REGEN_INTERVAL_MS = 120000;

    public HarmingPotionsPerk() {
        super("harming_potions", "Harming Potions", PerkTier.GOLD, PerkTeam.VAMPIRE,
                Material.SPLASH_POTION,
                "Start with 3 Splash Harming potions",
                "Regenerate 1 every 2 minutes");
    }

    private ItemStack createPotion() {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta != null) {
            meta.setBasePotionData(new PotionData(PotionType.HARMING, false, false));
            potion.setItemMeta(meta);
        }
        return potion;
    }

    @Override
    public void apply(Player player) {
        for (int i = 0; i < 3; i++) {
            player.getInventory().addItem(createPotion());
        }
    }

    @Override
    public void remove(Player player) {
        lastRegen.remove(player.getUniqueId());
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastRegen.get(uuid);
        if (last == null) { lastRegen.put(uuid, now); return; }
        if ((now - last) >= REGEN_INTERVAL_MS) {
            lastRegen.put(uuid, now);
            player.getInventory().addItem(createPotion());
            player.sendMessage(ChatColor.LIGHT_PURPLE + "A harming potion has regenerated!");
        }
    }
}
