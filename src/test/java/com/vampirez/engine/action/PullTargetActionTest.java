package com.vampirez.engine.action;

import com.vampirez.engine.HookContext;
import com.vampirez.engine.HookType;
import com.vampirez.engine.condition.Target;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PullTargetActionTest {

    private static Player playerAt(double x, double y, double z) {
        Player p = mock(Player.class);
        when(p.getLocation()).thenReturn(new Location(null, x, y, z));
        return p;
    }

    @Test
    void pullsVictimTowardTheOwner() {
        Player owner = playerAt(0, 0, 0);
        Player victim = playerAt(10, 0, 0); // due east of owner

        new PullTargetAction(Target.VICTIM, 1.5, 0.0).run(HookContext.builder(HookType.ON_DAMAGE_DEALT)
                .owner(owner).attacker(owner).victim(victim).victimPlayer(victim).build());

        ArgumentCaptor<Vector> vel = ArgumentCaptor.forClass(Vector.class);
        verify(victim).setVelocity(vel.capture());
        assertEquals(-1.5, vel.getValue().getX(), 1e-9, "pull points from victim toward owner, scaled by strength");
        assertEquals(0.0, vel.getValue().getZ(), 1e-9);
    }

    @Test
    void liftIsAppliedAsAMinimumUpwardComponent() {
        Player owner = playerAt(0, 0, 0);
        Player victim = playerAt(5, 0, 0); // level with owner → raw Y component is 0

        new PullTargetAction(Target.VICTIM, 1.0, 0.4).run(HookContext.builder(HookType.ON_DAMAGE_DEALT)
                .owner(owner).victim(victim).victimPlayer(victim).build());

        ArgumentCaptor<Vector> vel = ArgumentCaptor.forClass(Vector.class);
        verify(victim).setVelocity(vel.capture());
        assertEquals(0.4, vel.getValue().getY(), 1e-9, "level targets get popped up by lift");
    }

    @Test
    void downwardPullKeepsLiftFloor() {
        Player owner = playerAt(0, 0, 0);
        Player victim = playerAt(0, 10, 0); // victim above owner → raw pull points down

        new PullTargetAction(Target.VICTIM, 1.0, 0.2).run(HookContext.builder(HookType.ON_DAMAGE_DEALT)
                .owner(owner).victim(victim).victimPlayer(victim).build());

        ArgumentCaptor<Vector> vel = ArgumentCaptor.forClass(Vector.class);
        verify(victim).setVelocity(vel.capture());
        assertTrue(vel.getValue().getY() >= 0.2, "lift floor overrides a downward pull");
    }

    @Test
    void noOpWithoutOwnerOrTargetOrWhenSelfTargeted() {
        Player owner = playerAt(0, 0, 0);

        // no target
        new PullTargetAction(Target.VICTIM, 1.0, 0.2).run(
                HookContext.builder(HookType.ON_DAMAGE_DEALT).owner(owner).build());
        // self-targeted (owner resolves as both) — must not yank yourself
        new PullTargetAction(Target.OWNER, 1.0, 0.2).run(
                HookContext.builder(HookType.ON_DAMAGE_DEALT).owner(owner).build());

        verify(owner, never()).setVelocity(org.mockito.ArgumentMatchers.any());
    }
}
