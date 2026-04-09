package com.vampirez;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PerkManager {

    private final Map<String, Perk> perkRegistry = new LinkedHashMap<>();
    private final Map<UUID, List<Perk>> playerPerks = new HashMap<>();
    private int maxPerks = 4;

    public void setMaxPerks(int max) {
        this.maxPerks = max;
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
                .collect(Collectors.toList());

        Collections.shuffle(pool);
        return pool.subList(0, Math.min(count, pool.size()));
    }

    public boolean addPerkToPlayer(UUID uuid, Perk perk) {
        List<Perk> perks = playerPerks.computeIfAbsent(uuid, k -> new ArrayList<>());
        if (perks.size() >= maxPerks) return false;
        perks.add(perk);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && !player.isDead()) {
            perk.apply(player);
        }
        // If dead, perk is tracked but not applied — reapplyPerks() will handle it on respawn
        return true;
    }

    public void removePerk(UUID uuid, Perk perk) {
        List<Perk> perks = playerPerks.get(uuid);
        if (perks != null) {
            perks.remove(perk);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                perk.remove(player);
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
