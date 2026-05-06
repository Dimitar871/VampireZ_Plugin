package com.vampirez.engine;

import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * End-to-end loader tests: write the real pilot YAML to a temp file, parse it via
 * PerkConfigLoader, and verify both metadata and behavior of the resulting perks.
 *
 * Behavior verification fires onDamageDealt/onDamageTaken with a mocked damage event
 * and asserts setDamage is (or isn't) called. This catches both schema typos and
 * miswired action/condition factories.
 */
class PerkConfigLoaderTest {

    @TempDir
    Path tempDir;

    private MockedStatic<Bukkit> bukkitMock;

    @BeforeEach
    void setUp() {
        // CooldownCondition.test() reaches into Bukkit.getPluginManager() via Perk.getEffectiveCooldown.
        // Stub it to a manager with no VampireZ plugin so the base cooldown is used unchanged.
        bukkitMock = mockStatic(Bukkit.class);
        PluginManager pm = mock(PluginManager.class);
        when(pm.getPlugin("VampireZ")).thenReturn(null);
        bukkitMock.when(Bukkit::getPluginManager).thenReturn(pm);
    }

    @AfterEach
    void tearDown() {
        bukkitMock.close();
    }

    private File writeYaml(String content) throws Exception {
        Path file = tempDir.resolve("perks.yml");
        Files.writeString(file, content);
        return file.toFile();
    }

    private List<DataDrivenPerk> load(String yaml) throws Exception {
        File f = writeYaml(yaml);
        return new PerkConfigLoader(Logger.getLogger("test")).loadAll(f);
    }

    private DataDrivenPerk findById(List<DataDrivenPerk> perks, String id) {
        return perks.stream().filter(p -> p.getId().equals(id)).findFirst().orElseThrow();
    }

    private static final String PILOT_YAML = """
            blunt_force:
              name: "Blunt Force"
              tier: SILVER
              team: BOTH
              icon: IRON_INGOT
              description:
                - "+20% melee damage"
              hooks:
                on_damage_dealt:
                  - actions:
                      - type: multiply_damage
                        factor: 1.2

            tough_skin:
              name: "Tough Skin"
              tier: SILVER
              team: BOTH
              icon: IRON_INGOT
              description:
                - "-10% damage taken"
              hooks:
                on_damage_taken:
                  - actions:
                      - type: multiply_damage
                        factor: 0.9

            backstab:
              name: "Backstab"
              tier: SILVER
              team: VAMPIRE
              icon: IRON_SWORD
              description:
                - "Deal 30% more damage when hitting from behind."
              hooks:
                on_damage_dealt:
                  - conditions:
                      - type: from_behind
                    actions:
                      - type: multiply_damage
                        factor: 1.3

            feral_charge:
              name: "Feral Charge"
              tier: SILVER
              team: VAMPIRE
              icon: RABBIT_FOOT
              description:
                - "Sprinting attacks deal +30% damage"
              hooks:
                on_damage_dealt:
                  - conditions:
                      - type: is_sprinting
                        target: attacker
                      - type: cooldown
                        ms: 6000
                    actions:
                      - type: multiply_damage
                        factor: 1.3

            executioner:
              name: "Executioner"
              tier: GOLD
              team: BOTH
              icon: DIAMOND_AXE
              description:
                - "+30% damage to targets below 40% HP"
              hooks:
                on_damage_dealt:
                  - conditions:
                      - type: hp_below_absolute
                        target: victim
                        threshold: 3.0
                    actions:
                      - type: set_damage
                        value: 100.0
                  - conditions:
                      - type: hp_below_percent
                        target: victim
                        threshold: 0.4
                    actions:
                      - type: multiply_damage
                        factor: 1.3
            """;

    @Test
    void loadsAllFivePilotPerks() throws Exception {
        List<DataDrivenPerk> perks = load(PILOT_YAML);
        assertEquals(5, perks.size());
    }

    @Test
    void parsesMetadataCorrectlyForEachPerk() throws Exception {
        List<DataDrivenPerk> perks = load(PILOT_YAML);

        DataDrivenPerk blunt = findById(perks, "blunt_force");
        assertEquals("Blunt Force", blunt.getDisplayName());
        assertEquals(PerkTier.SILVER, blunt.getTier());
        assertEquals(PerkTeam.BOTH, blunt.getTeam());
        assertEquals(Material.IRON_INGOT, blunt.getIcon());

        DataDrivenPerk backstab = findById(perks, "backstab");
        assertEquals(PerkTeam.VAMPIRE, backstab.getTeam());
        assertEquals(Material.IRON_SWORD, backstab.getIcon());

        DataDrivenPerk exec = findById(perks, "executioner");
        assertEquals(PerkTier.GOLD, exec.getTier());
        assertEquals(Material.DIAMOND_AXE, exec.getIcon());
    }

    @Test
    void bluntForce_multipliesDamageOnHit() throws Exception {
        DataDrivenPerk perk = findById(load(PILOT_YAML), "blunt_force");

        Player attacker = mock(Player.class);
        Player victim = mock(Player.class);
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamage()).thenReturn(5.0);

        perk.onDamageDealt(attacker, victim, event);

        verify(event).setDamage(6.0);
    }

    @Test
    void toughSkin_reducesDamageOnTaken() throws Exception {
        DataDrivenPerk perk = findById(load(PILOT_YAML), "tough_skin");

        Player victim = mock(Player.class);
        Player attacker = mock(Player.class);
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamage()).thenReturn(10.0);

        perk.onDamageTaken(victim, attacker, event);

        verify(event).setDamage(9.0);
    }

    @Test
    void backstab_onlyFiresWhenHittingFromBehind() throws Exception {
        DataDrivenPerk perk = findById(load(PILOT_YAML), "backstab");

        // Victim faces +Z (yaw=0). Attacker at -Z is behind.
        org.bukkit.World world = mock(org.bukkit.World.class);
        Player victim = mock(Player.class);
        when(victim.getLocation()).thenReturn(new org.bukkit.Location(world, 0, 64, 0, 0f, 0f));

        Player frontAttacker = mock(Player.class);
        when(frontAttacker.getLocation()).thenReturn(new org.bukkit.Location(world, 0, 64, 3, 0f, 0f));
        EntityDamageByEntityEvent frontEvent = mock(EntityDamageByEntityEvent.class);
        when(frontEvent.getDamage()).thenReturn(5.0);

        perk.onDamageDealt(frontAttacker, victim, frontEvent);
        verify(frontEvent, never()).setDamage(anyDouble());

        Player rearAttacker = mock(Player.class);
        when(rearAttacker.getLocation()).thenReturn(new org.bukkit.Location(world, 0, 64, -3, 0f, 0f));
        EntityDamageByEntityEvent rearEvent = mock(EntityDamageByEntityEvent.class);
        when(rearEvent.getDamage()).thenReturn(5.0);

        perk.onDamageDealt(rearAttacker, victim, rearEvent);
        verify(rearEvent).setDamage(6.5);
    }

    @Test
    void feralCharge_requiresSprintAndRespectsCooldown() throws Exception {
        DataDrivenPerk perk = findById(load(PILOT_YAML), "feral_charge");

        Player attacker = mock(Player.class);
        when(attacker.getUniqueId()).thenReturn(UUID.randomUUID());
        Player victim = mock(Player.class);

        // Not sprinting → no boost
        when(attacker.isSprinting()).thenReturn(false);
        EntityDamageByEntityEvent walkEvent = mock(EntityDamageByEntityEvent.class);
        when(walkEvent.getDamage()).thenReturn(5.0);
        perk.onDamageDealt(attacker, victim, walkEvent);
        verify(walkEvent, never()).setDamage(anyDouble());

        // Sprinting first hit → boosted
        when(attacker.isSprinting()).thenReturn(true);
        EntityDamageByEntityEvent firstSprintEvent = mock(EntityDamageByEntityEvent.class);
        when(firstSprintEvent.getDamage()).thenReturn(5.0);
        perk.onDamageDealt(attacker, victim, firstSprintEvent);
        verify(firstSprintEvent).setDamage(6.5);

        // Sprinting second hit within cooldown → no boost
        EntityDamageByEntityEvent secondSprintEvent = mock(EntityDamageByEntityEvent.class);
        when(secondSprintEvent.getDamage()).thenReturn(5.0);
        perk.onDamageDealt(attacker, victim, secondSprintEvent);
        verify(secondSprintEvent, never()).setDamage(anyDouble());
    }

    @Test
    void executioner_executesBelowAbsoluteThresholdOtherwiseMultiplies() throws Exception {
        DataDrivenPerk perk = findById(load(PILOT_YAML), "executioner");

        Player attacker = mock(Player.class);
        Player victim = mock(Player.class);
        when(victim.getMaxHealth()).thenReturn(20.0);

        // Victim at 2 HP (below 3.0 absolute) → set damage to 100.0
        when(victim.getHealth()).thenReturn(2.0);
        EntityDamageByEntityEvent execEvent = mock(EntityDamageByEntityEvent.class);
        when(execEvent.getDamage()).thenReturn(5.0);
        perk.onDamageDealt(attacker, victim, execEvent);
        verify(execEvent).setDamage(100.0);

        // Victim at 6 HP (30% of 20, below 40%, but above 3.0 absolute) → multiply by 1.3
        when(victim.getHealth()).thenReturn(6.0);
        EntityDamageByEntityEvent multEvent = mock(EntityDamageByEntityEvent.class);
        when(multEvent.getDamage()).thenReturn(5.0);
        perk.onDamageDealt(attacker, victim, multEvent);
        verify(multEvent).setDamage(6.5);

        // Victim at 18 HP (90%) → no boost
        when(victim.getHealth()).thenReturn(18.0);
        EntityDamageByEntityEvent fullEvent = mock(EntityDamageByEntityEvent.class);
        when(fullEvent.getDamage()).thenReturn(5.0);
        perk.onDamageDealt(attacker, victim, fullEvent);
        verify(fullEvent, never()).setDamage(anyDouble());
    }

    @Test
    void unknownActionType_loggedAndSkipped_perkStillLoadsWithoutCrash() throws Exception {
        String yaml = """
                broken:
                  name: "Broken"
                  tier: SILVER
                  team: BOTH
                  icon: STONE
                  description:
                    - "broken"
                  hooks:
                    on_damage_dealt:
                      - actions:
                          - type: nonexistent_action
                          - type: multiply_damage
                            factor: 2.0
                """;
        DataDrivenPerk perk = findById(load(yaml), "broken");

        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamage()).thenReturn(5.0);
        perk.onDamageDealt(mock(Player.class), mock(Player.class), event);

        // The known action still ran.
        verify(event, atLeastOnce()).setDamage(10.0);
    }

    @Test
    void emptyYamlReturnsEmptyList() throws Exception {
        List<DataDrivenPerk> perks = load("");
        assertEquals(0, perks.size());
    }

    @Test
    void missingFileReturnsEmptyListWithoutThrowing() {
        File missing = tempDir.resolve("does-not-exist.yml").toFile();
        List<DataDrivenPerk> perks = new PerkConfigLoader(Logger.getLogger("test")).loadAll(missing);
        assertNotNull(perks);
        assertTrue(perks.isEmpty());
    }
}
