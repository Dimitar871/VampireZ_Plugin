package com.vampirez;

import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class GameTimerManagerTest {

    private GameTimerManager tm;

    @BeforeEach
    void setUp() {
        tm = new GameTimerManager();
    }

    // ===== remainingSeconds =====

    @Test
    void remainingSeconds_setterAndGetter_roundTrip() {
        tm.setRemainingSeconds(1500);
        assertEquals(1500, tm.getRemainingSeconds());
    }

    @Test
    void decrementRemainingSeconds_returnsNewValueAfterDecrement() {
        tm.setRemainingSeconds(10);
        assertEquals(9, tm.decrementRemainingSeconds());
        assertEquals(9, tm.getRemainingSeconds());
        assertEquals(8, tm.decrementRemainingSeconds());
    }

    // ===== autoStartCountdown =====

    @Test
    void autoStartCountdown_defaultsToMinusOne() {
        assertEquals(-1, tm.getAutoStartCountdown());
    }

    @Test
    void autoStartCountdown_setterAndGetter() {
        tm.setAutoStartCountdown(30);
        assertEquals(30, tm.getAutoStartCountdown());
    }

    // ===== activeStartedAtMs / getActiveElapsedSeconds =====

    @Test
    void getActiveElapsedSeconds_returnsZero_whenNotStarted() {
        assertEquals(0, tm.getActiveElapsedSeconds());
    }

    @Test
    void getActiveElapsedSeconds_reflectsWallClockSinceStart() {
        long oneMinuteAgo = System.currentTimeMillis() - 60_000L;
        tm.setActiveStartedAtMs(oneMinuteAgo);

        int elapsed = tm.getActiveElapsedSeconds();
        // Allow ±1s slack for test execution variance.
        assertTrue(elapsed >= 59 && elapsed <= 61,
                "Expected ~60 seconds elapsed, got " + elapsed);
    }

    // ===== firedTimedMilestones =====

    @Test
    void firedTimedMilestones_startsEmpty() {
        assertFalse(tm.hasFiredTimedMilestone(300));
        assertEquals(0, tm.getFiredTimedMilestones().size());
    }

    @Test
    void firedTimedMilestones_addAndQuery() {
        tm.getFiredTimedMilestones().add(300);
        tm.getFiredTimedMilestones().add(600);

        assertTrue(tm.hasFiredTimedMilestone(300));
        assertTrue(tm.hasFiredTimedMilestone(600));
        assertFalse(tm.hasFiredTimedMilestone(900));
    }

    // ===== Task lifecycle =====

    @Test
    void cancelAllTasks_cancelsEveryNonNullTask_andNullsThemOut() {
        BukkitTask t1 = Mockito.mock(BukkitTask.class);
        BukkitTask t2 = Mockito.mock(BukkitTask.class);
        BukkitTask t3 = Mockito.mock(BukkitTask.class);
        BukkitTask t4 = Mockito.mock(BukkitTask.class);
        BukkitTask t5 = Mockito.mock(BukkitTask.class);

        tm.timerTask = t1;
        tm.scoreboardTask = t2;
        tm.countdownTask = t3;
        tm.vampireReleaseTask = t4;
        tm.autoStartTask = t5;

        tm.cancelAllTasks();

        verify(t1, times(1)).cancel();
        verify(t2, times(1)).cancel();
        verify(t3, times(1)).cancel();
        verify(t4, times(1)).cancel();
        verify(t5, times(1)).cancel();

        assertNull(tm.timerTask);
        assertNull(tm.scoreboardTask);
        assertNull(tm.countdownTask);
        assertNull(tm.vampireReleaseTask);
        assertNull(tm.autoStartTask);
    }

    @Test
    void cancelAllTasks_isIdempotent_safeOnNullFields() {
        // All fields start null — should not throw.
        tm.cancelAllTasks();
        tm.cancelAllTasks();
    }

    @Test
    void cancelAllTasks_skipsAlreadyNullTasks() {
        BukkitTask onlyTimer = Mockito.mock(BukkitTask.class);
        tm.timerTask = onlyTimer;
        // Other 4 fields are null.

        tm.cancelAllTasks();

        verify(onlyTimer, times(1)).cancel();
        // No NPE means the null-skip logic worked.
    }

    @Test
    void cancelAutoStartTask_cancelsOnlyAutoStart_andResetsCountdown() {
        BukkitTask asTask = Mockito.mock(BukkitTask.class);
        BukkitTask other = Mockito.mock(BukkitTask.class);
        tm.autoStartTask = asTask;
        tm.timerTask = other;
        tm.setAutoStartCountdown(15);

        tm.cancelAutoStartTask();

        verify(asTask, times(1)).cancel();
        verify(other, never()).cancel();
        assertNull(tm.autoStartTask);
        assertEquals(-1, tm.getAutoStartCountdown(),
                "cancelAutoStartTask must reset countdown to -1 sentinel");
    }
}
