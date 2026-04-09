package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import com.vampirez.VampireZPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class BardPerk extends Perk {

    private static final double AURA_RADIUS = 20.0;
    private static final Map<UUID, Set<UUID>> damageBoostedByBard = new HashMap<>();

    // Track which allies currently have bard buffs so we can remove them when out of range / bard switches
    private final Map<UUID, Set<UUID>> regenBuffedAllies = new HashMap<>();
    private final Map<UUID, Set<UUID>> strengthBuffedAllies = new HashMap<>();
    private final Map<UUID, String> lastHeldAura = new HashMap<>();

    public static boolean isDamageBoosted(UUID uuid) {
        for (Set<UUID> boosted : damageBoostedByBard.values()) {
            if (boosted.contains(uuid)) return true;
        }
        return false;
    }

    public BardPerk() {
        super("bard", "Bard", PerkTier.PRISMATIC, PerkTeam.HUMAN,
                Material.NOTE_BLOCK,
                "Weakness IV (nearly useless in melee).",
                "Hold Melon: Regen I to allies within 20 blocks.",
                "Hold Blaze Rod: Strength I to allies within 20 blocks.",
                "Hold Iron Nugget: Jump Boost IV + Speed II (self).",
                "Only one aura active at a time (main hand).");
    }

    @Override
    public void apply(Player player) {
        // Apply Weakness IV (amplifier 3 = level IV)
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, Integer.MAX_VALUE, 3, true, false, false));

        // Give held items
        ItemStack healingAura = new ItemStack(Material.GLISTERING_MELON_SLICE);
        ItemMeta healMeta = healingAura.getItemMeta();
        if (healMeta != null) {
            healMeta.setDisplayName(ChatColor.GREEN + "Healing Aura");
            healMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Hold in main hand to grant",
                    ChatColor.GRAY + "Regeneration I to nearby allies"
            ));
            healingAura.setItemMeta(healMeta);
        }

        ItemStack damageAura = new ItemStack(Material.BLAZE_ROD);
        ItemMeta dmgMeta = damageAura.getItemMeta();
        if (dmgMeta != null) {
            dmgMeta.setDisplayName(ChatColor.RED + "Damage Aura");
            dmgMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Hold in main hand to grant",
                    ChatColor.GRAY + "Strength I to nearby allies"
            ));
            damageAura.setItemMeta(dmgMeta);
        }

        ItemStack escapeItem = new ItemStack(Material.IRON_NUGGET);
        ItemMeta escMeta = escapeItem.getItemMeta();
        if (escMeta != null) {
            escMeta.setDisplayName(ChatColor.AQUA + "Wind Runner");
            escMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Hold in main hand for",
                    ChatColor.GRAY + "Jump Boost IV + Speed II (self only)",
                    ChatColor.YELLOW + "Use to escape dangerous situations!"
            ));
            escapeItem.setItemMeta(escMeta);
        }

        player.getInventory().addItem(healingAura);
        player.getInventory().addItem(damageAura);
        player.getInventory().addItem(escapeItem);
    }

    @Override
    public void remove(Player player) {
        UUID bardUUID = player.getUniqueId();
        player.removePotionEffect(PotionEffectType.WEAKNESS);

        // Remove self buffs
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        player.removePotionEffect(PotionEffectType.SPEED);

        // Remove bard items
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String name = item.getItemMeta().getDisplayName();
                if (name.contains("Healing Aura") || name.contains("Damage Aura") || name.contains("Wind Runner")) {
                    player.getInventory().remove(item);
                }
            }
        }

        // Remove buffs from all allies
        clearAllyBuffs(bardUUID, regenBuffedAllies, PotionEffectType.REGENERATION);
        clearAllyBuffs(bardUUID, strengthBuffedAllies, PotionEffectType.STRENGTH);

        damageBoostedByBard.remove(bardUUID);
        lastHeldAura.remove(bardUUID);
    }

    @Override
    public void onTick(Player player) {
        VampireZPlugin plugin = (VampireZPlugin) getPlugin();
        if (plugin.getGameManager() == null) return;

        UUID bardUUID = player.getUniqueId();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        String heldName = "";
        if (mainHand != null && mainHand.hasItemMeta() && mainHand.getItemMeta().hasDisplayName()) {
            heldName = mainHand.getItemMeta().getDisplayName();
        }

        String previousAura = lastHeldAura.getOrDefault(bardUUID, "");
        List<Player> nearbyHumans = getNearbyHumans(player, plugin);
        Set<UUID> nearbyUUIDs = new HashSet<>();
        for (Player h : nearbyHumans) nearbyUUIDs.add(h.getUniqueId());

        if (heldName.contains("Healing Aura")) {
            // Switched away from other auras — clean up
            if (!previousAura.equals("healing")) {
                clearAllyBuffs(bardUUID, strengthBuffedAllies, PotionEffectType.STRENGTH);
                damageBoostedByBard.remove(bardUUID);
                removeSelfEscapeBuffs(player);
            }
            lastHeldAura.put(bardUUID, "healing");

            // Apply Regen I to nearby allies, remove from out-of-range allies
            Set<UUID> currentBuffed = regenBuffedAllies.computeIfAbsent(bardUUID, k -> new HashSet<>());

            // Remove buff from allies no longer in range
            Iterator<UUID> it = currentBuffed.iterator();
            while (it.hasNext()) {
                UUID allyUUID = it.next();
                if (!nearbyUUIDs.contains(allyUUID)) {
                    Player ally = Bukkit.getPlayer(allyUUID);
                    if (ally != null && ally.isOnline()) {
                        ally.removePotionEffect(PotionEffectType.REGENERATION);
                    }
                    it.remove();
                }
            }

            // Apply buff to allies in range
            for (Player human : nearbyHumans) {
                PotionEffect regen = human.getPotionEffect(PotionEffectType.REGENERATION);
                if (regen == null || regen.getDuration() < 30) {
                    human.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, false, false));
                }
                currentBuffed.add(human.getUniqueId());
            }

            if (!nearbyHumans.isEmpty()) {
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1.5, 0), 5, 1.0, 0.5, 1.0, 0);
            }

        } else if (heldName.contains("Damage Aura")) {
            if (!previousAura.equals("damage")) {
                clearAllyBuffs(bardUUID, regenBuffedAllies, PotionEffectType.REGENERATION);
                removeSelfEscapeBuffs(player);
            }
            lastHeldAura.put(bardUUID, "damage");

            // Apply Strength I to nearby allies
            Set<UUID> currentBuffed = strengthBuffedAllies.computeIfAbsent(bardUUID, k -> new HashSet<>());

            // Remove buff from allies no longer in range
            Iterator<UUID> it = currentBuffed.iterator();
            while (it.hasNext()) {
                UUID allyUUID = it.next();
                if (!nearbyUUIDs.contains(allyUUID)) {
                    Player ally = Bukkit.getPlayer(allyUUID);
                    if (ally != null && ally.isOnline()) {
                        ally.removePotionEffect(PotionEffectType.STRENGTH);
                    }
                    it.remove();
                }
            }

            // Apply buff to allies in range
            for (Player human : nearbyHumans) {
                PotionEffect str = human.getPotionEffect(PotionEffectType.STRENGTH);
                if (str == null || str.getDuration() < 30) {
                    human.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 0, false, false));
                }
                currentBuffed.add(human.getUniqueId());
            }

            // Also keep the old damage boost tracking for PerkListener compatibility
            Set<UUID> boosted = new HashSet<>(nearbyUUIDs);
            damageBoostedByBard.put(bardUUID, boosted);

            if (!nearbyHumans.isEmpty()) {
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1.5, 0), 3, 0.5, 0.3, 0.5, 0.01);
            }

        } else if (heldName.contains("Wind Runner")) {
            if (!previousAura.equals("escape")) {
                clearAllyBuffs(bardUUID, regenBuffedAllies, PotionEffectType.REGENERATION);
                clearAllyBuffs(bardUUID, strengthBuffedAllies, PotionEffectType.STRENGTH);
                damageBoostedByBard.remove(bardUUID);
            }
            lastHeldAura.put(bardUUID, "escape");

            // Self buffs: Jump Boost IV (amplifier 3) + Speed II (amplifier 1)
            PotionEffect jump = player.getPotionEffect(PotionEffectType.JUMP_BOOST);
            if (jump == null || jump.getDuration() < 30) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, 3, false, false));
            }
            PotionEffect speed = player.getPotionEffect(PotionEffectType.SPEED);
            if (speed == null || speed.getDuration() < 30) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1, false, false));
            }

            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 3, 0.3, 0.1, 0.3, 0.02);

        } else {
            // Not holding any bard item — clear everything
            if (!previousAura.isEmpty()) {
                clearAllyBuffs(bardUUID, regenBuffedAllies, PotionEffectType.REGENERATION);
                clearAllyBuffs(bardUUID, strengthBuffedAllies, PotionEffectType.STRENGTH);
                damageBoostedByBard.remove(bardUUID);
                removeSelfEscapeBuffs(player);
            }
            lastHeldAura.put(bardUUID, "");
        }
    }

    private void clearAllyBuffs(UUID bardUUID, Map<UUID, Set<UUID>> buffMap, PotionEffectType effectType) {
        Set<UUID> buffed = buffMap.remove(bardUUID);
        if (buffed != null) {
            for (UUID allyUUID : buffed) {
                Player ally = Bukkit.getPlayer(allyUUID);
                if (ally != null && ally.isOnline()) {
                    ally.removePotionEffect(effectType);
                }
            }
        }
    }

    private void removeSelfEscapeBuffs(Player player) {
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        player.removePotionEffect(PotionEffectType.SPEED);
    }

    private List<Player> getNearbyHumans(Player bard, VampireZPlugin plugin) {
        UUID bardUUID = bard.getUniqueId();
        List<Player> result = new ArrayList<>();
        for (UUID humanUUID : plugin.getGameManager().getHumanTeam()) {
            if (humanUUID.equals(bardUUID)) continue;
            Player human = Bukkit.getPlayer(humanUUID);
            if (human != null && human.isOnline() && human.getWorld().equals(bard.getWorld())) {
                if (human.getLocation().distance(bard.getLocation()) <= AURA_RADIUS) {
                    result.add(human);
                }
            }
        }
        return result;
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("heals", "Heal Ticks");
        return labels;
    }
}
