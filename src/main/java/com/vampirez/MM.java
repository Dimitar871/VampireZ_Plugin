package com.vampirez;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/** Central MiniMessage / Adventure text utilities. */
public final class MM {
    private static final MiniMessage MM_INSTANCE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMP = LegacyComponentSerializer.legacyAmpersand();

    private MM() {}

    /** Parse a MiniMessage-formatted string to a Component. */
    public static Component parse(String miniMessage) {
        return MM_INSTANCE.deserialize(miniMessage);
    }

    /**
     * Convert a legacy {@code &}-code string (e.g. from config.yml) to a Component.
     * Use this only for config values that haven't been migrated to MiniMessage yet.
     */
    public static Component legacy(String ampersandString) {
        return LEGACY_AMP.deserialize(ampersandString);
    }
}
