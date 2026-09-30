package com.vampirez;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

/** Tags on plugin-spawned mobs (team ownership, war-horse owner). */
public final class MobTags {

    private static final NamespacedKey TEAM = new NamespacedKey(ArenaPaths.NAMESPACE, "team");
    private static final NamespacedKey WAR_HORSE_OWNER = new NamespacedKey(ArenaPaths.NAMESPACE, "war_horse_owner");

    private MobTags() {}

    public static void setTeam(Entity entity, String team) {
        entity.getPersistentDataContainer().set(TEAM, PersistentDataType.STRING, team);
    }

    /** @return the team tag, or null for mobs the plugin did not spawn */
    public static String team(Entity entity) {
        return entity.getPersistentDataContainer().get(TEAM, PersistentDataType.STRING);
    }

    public static void setWarHorseOwner(Entity horse, String ownerUuid) {
        horse.getPersistentDataContainer().set(WAR_HORSE_OWNER, PersistentDataType.STRING, ownerUuid);
    }

    public static boolean isWarHorse(Entity entity) {
        return entity.getPersistentDataContainer().has(WAR_HORSE_OWNER, PersistentDataType.STRING);
    }
}
