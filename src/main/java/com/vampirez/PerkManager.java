
package com.vampirez;

import com.vampirez.api.event.PlayerPerkGainedEvent;
import com.vampirez.api.event.PlayerPerkLostEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PerkManager {

    private final Map<String, Perk> perkRegistry = new LinkedHashMap<>();
    private final Map<UUID, List<Perk>> playerPerks = new HashMap<>();
    private int maxPerks = 10;
    private Set<String> disabledPerks = new HashSet<>();

    public void setMaxPerks(int max) {
        this.maxPerks = max;
    }

    public void setDisabledPerks(java.util.Collection<String> ids) {
        this.disabledPerks = new HashSet<>(ids);
    }

    public boolean isDisabled(String perkId) {
        return disabledPerks.contains(perkId);
    }

    public void registerPerk(Perk perk) {
        perkRegistry.put(perk.getId(), perk);
    }

    public List<Perk> getRandomPerks(PerkTier tier, PerkTeam playerTeam, int count, UUID playerUUID) {
        List<Perk> owned = getPlayerPerks(playerUUID);
        Set<String> ownedIds = owned.stream().map(Perk::getId).collect(Collectors.toSet());

        List<Perk> pool = perkRegistry.values().stream()
                .filter(p -> p.getTier() == tier)
                .filter(p -> p.getTeam() == playerTeam || p.getTeam() == PerkTeam.BOTH)
                .filter(p -> !ownedIds.contains(p.getId()))
                .filter(p -> !disabledPerks.contains(p.getId()))
                .collect(Collectors.toList());

        Collections.shuffle(pool);
        return pool.subList(0, Math.min(count, pool.size()));
    }

    public boolean addPerkToPlayer(UUID uuid, Perk perk) {
        return addPerkToPlayer(uuid, perk, PlayerPerkGainedEvent.Source.INTERNAL);
    }

    public boolean addPerkToPlayer(UUID uuid, Perk perk, PlayerPerkGainedEvent.Source source) {
        return addPerkToPlayer(uuid, perk, source, true);
    }

    /**
     * @param applyNow pass false when a later bulk {@link #reapplyPerks} will run apply()
     *                 (reconnect auto-assign, scouting phase) — applying here too would
     *                 double-grant items from item-granting perks
     */
    public boolean addPerkToPlayer(UUID uuid, Perk perk, PlayerPerkGainedEvent.Source source, boolean applyNow) {
        List<Perk> perks = playerPerks.computeIfAbsent(uuid, k -> new ArrayList<>());
        if (perks.size() >= maxPerks) return false;

        PlayerPerkGainedEvent event = new PlayerPerkGainedEvent(uuid, perk.getId(), source);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        perks.add(perk);
        if (applyNow) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && !player.isDead()) {
                perk.apply(player);
            }
            // If dead, perk is tracked but not applied — reapplyPerks() will handle it on respawn
        }
        return true;
    }

    /**
     * Adds a perk bypassing the max perk limit. Used by Lucky Roll Prismatic.
     */
    public void forceAddPerkToPlayer(UUID uuid, Perk perk) {
        forceAddPerkToPlayer(uuid, perk, PlayerPerkGainedEvent.Source.INTERNAL);
    }

    public boolean forceAddPerkToPlayer(UUID uuid, Perk perk, PlayerPerkGainedEvent.Source source) {
        PlayerPerkGainedEvent event = new PlayerPerkGainedEvent(uuid, perk.getId(), source);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        List<Perk> perks = playerPerks.computeIfAbsent(uuid, k -> new ArrayList<>());
        perks.add(perk);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && !player.isDead()) {
            perk.apply(player);
        }
        return true;
    }

    public void removePerk(UUID uuid, Perk perk) {
        List<Perk> perks = playerPerks.get(uuid);
        if (perks != null) {
            if (perks.remove(perk)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    perk.remove(player);
                }
                Bukkit.getPluginManager().callEvent(new PlayerPerkLostEvent(uuid, perk.getId()));
            }
        }
    }

    public List<Perk> removeTeamSpecificPerks(UUID uuid, PerkTeam oldTeam) {
        List<Perk> perks = playerPerks.get(uuid);
        if (perks == null) return Collections.emptyList();

        List<Perk> removed = new ArrayList<>();
        Iterator<Perk> it = perks.iterator();
        while (it.hasNext()) {
            Perk perk = it.next();
            if (perk.getTeam() == oldTeam) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    perk.remove(player);
                }
                removed.add(perk);
                it.remove();
                Bukkit.getPluginManager().callEvent(new PlayerPerkLostEvent(uuid, perk.getId()));
            }
        }
        return removed;
    }

    public void removeAllPerks(UUID uuid) {
        List<Perk> perks = playerPerks.remove(uuid);
        if (perks != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                for (Perk perk : perks) {
                    perk.remove(player);
                }
            }
            for (Perk perk : perks) {
                Bukkit.getPluginManager().callEvent(new PlayerPerkLostEvent(uuid, perk.getId()));
            }
        }
    }

    public void reapplyPerks(UUID uuid) {
        List<Perk> perks = playerPerks.get(uuid);
        if (perks == null) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            for (Perk perk : perks) {
                perk.apply(player);
            }
        }
    }

    public List<Perk> getPlayerPerks(UUID uuid) {
        return playerPerks.getOrDefault(uuid, Collections.emptyList());
    }

    public int getPlayerPerkCount(UUID uuid) {
        return playerPerks.getOrDefault(uuid, Collections.emptyList()).size();
    }

    public int getMaxPerks() { return maxPerks; }

    public void resetAll() {
        for (Map.Entry<UUID, List<Perk>> entry : playerPerks.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                for (Perk perk : entry.getValue()) {
                    perk.remove(player);
                }
            }
        }
        playerPerks.clear();
        // remove(player) above only reaches online owners — stale cross-player
        // state (cleaver stacks, curses, bleeds, tasks) must be wiped explicitly.
        for (Perk perk : perkRegistry.values()) {
            perk.clearGlobalState();
        }
    }

    /**
     * Replaces every registered {@link com.vampirez.engine.DataDrivenPerk} with the
     * freshly loaded set — the heart of perks.yml hot reload. Call in LOBBY only.
     *
     * <p>Stale data-driven instances are first detached from any player still holding
     * them (admin test perks), so no player list keeps a perk the registry no longer
     * knows. Boot order registers YAML perks before Java perks, so a YAML id that
     * collides with a Java perk loses; this method preserves that precedence by
     * skipping such ids instead of overwriting.
     *
     * @return ids that were skipped because a Java perk already owns them
     */
    public List<String> reloadDataDrivenPerks(List<? extends Perk> fresh) {
        for (UUID uuid : new ArrayList<>(playerPerks.keySet())) {
            for (Perk perk : new ArrayList<>(playerPerks.get(uuid))) {
                if (perk instanceof com.vampirez.engine.DataDrivenPerk) {
                    removePerk(uuid, perk);
                }
            }
        }

        perkRegistry.values().removeIf(p -> p instanceof com.vampirez.engine.DataDrivenPerk);

        List<String> skipped = new ArrayList<>();
        for (Perk perk : fresh) {
            if (perkRegistry.containsKey(perk.getId())) {
                skipped.add(perk.getId());
                continue;
            }
            perkRegistry.put(perk.getId(), perk);
        }
        return skipped;
    }

    public boolean hasPerk(UUID uuid, String perkId) {
        List<Perk> perks = playerPerks.get(uuid);
        if (perks == null) return false;
        for (Perk p : perks) {
            if (p.getId().equals(perkId)) return true;
        }
        return false;
    }

    public Perk getPerkById(String id) {
        return perkRegistry.get(id);
    }

    public Collection<Perk> getAllPerks() {
        return perkRegistry.values();
    }
}
