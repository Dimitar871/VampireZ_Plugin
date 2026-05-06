package com.vampirez.engine.condition;

import com.vampirez.engine.HookContext;
import com.vampirez.engine.HookType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * CooldownCondition.test() consults Perk.getEffectiveCooldown(player, base) which calls
 * Bukkit.getPluginManager().getPlugin(...). To keep tests pure unit, we stub Bukkit.
 */
class CooldownConditionTest {

    private MockedStatic<Bukkit> bukkitMock;

    @BeforeEach
    void setUp() {
        bukkitMock = mockStatic(Bukkit.class);
        // No VampireZ plugin registered → getEffectiveCooldown returns the base value unchanged.
        PluginManager pm = mock(PluginManager.class);
        when(pm.getPlugin("VampireZ")).thenReturn(null);
        bukkitMock.when(Bukkit::getPluginManager).thenReturn(pm);
    }

    @AfterEach
    void tearDown() {
        bukkitMock.close();
    }

    @Test
    void firstCallPasses_subsequentCallsBlockedUntilCooldownExpires() {
        Player owner = mock(Player.class);
        when(owner.getUniqueId()).thenReturn(UUID.randomUUID());

        HookContext ctx = HookContext.builder(HookType.ON_DAMAGE_DEALT).owner(owner).build();
        CooldownCondition cd = new CooldownCondition(50_000);

        assertTrue(cd.test(ctx), "first call should pass and stamp the timestamp");
        assertFalse(cd.test(ctx), "second call within window should be blocked");
        assertFalse(cd.test(ctx), "third call within window should still be blocked");
    }

    @Test
    void differentPlayersHaveIndependentCooldowns() {
        Player a = mock(Player.class);
        when(a.getUniqueId()).thenReturn(UUID.randomUUID());
        Player b = mock(Player.class);
        when(b.getUniqueId()).thenReturn(UUID.randomUUID());

        CooldownCondition cd = new CooldownCondition(50_000);

        assertTrue(cd.test(HookContext.builder(HookType.ON_DAMAGE_DEALT).owner(a).build()));
        assertTrue(cd.test(HookContext.builder(HookType.ON_DAMAGE_DEALT).owner(b).build()),
                "player B's cooldown should not be affected by player A's call");
        assertFalse(cd.test(HookContext.builder(HookType.ON_DAMAGE_DEALT).owner(a).build()));
    }

    @Test
    void noOwnerInContext_fails() {
        HookContext ctx = HookContext.builder(HookType.ON_DAMAGE_DEALT).build();
        assertFalse(new CooldownCondition(1000).test(ctx));
    }
}
