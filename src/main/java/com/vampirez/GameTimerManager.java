package com.vampirez;

import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;

/**
 * Owns the five {@link BukkitTask} handles plus the per-game timer state
 * ({@code remainingSeconds}, {@code autoStartCountdown}, {@code activeStartedAtMs},
 * {@code firedTimedMilestones}). The methods that schedule these tasks still live on
 * {@link GameManager} for now — this class is the data home + cancel coordinator.
 */
public class GameTimerManager {

    BukkitTask timerTask;
    BukkitTask scoreboardTask;
    BukkitTask countdownTask;
    BukkitTask vampireReleaseTask;
    BukkitTask autoStartTask;

    private int remainingSeconds;
    private int autoStartCountdown = -1;
    private long activeStartedAtMs = 0L;
    private final Set<Integer> firedTimedMilestones = new HashSet<>();

    public int getRemainingSeconds() { return remainingSeconds; }
    public void setRemainingSeconds(int s) { this.remainingSeconds = s; }
    public int decrementRemainingSeconds() { return --remainingSeconds; }

    public int getAutoStartCountdown() { return autoStartCountdown; }
    public void setAutoStartCountdown(int v) { this.autoStartCountdown = v; }

    public long getActiveStartedAtMs() { return activeStartedAtMs; }
    public void setActiveStartedAtMs(long ms) { this.activeStartedAtMs = ms; }

    public int getActiveElapsedSeconds() {
        if (activeStartedAtMs == 0L) return 0;
        return (int) ((System.currentTimeMillis() - activeStartedAtMs) / 1000);
    }

    public Set<Integer> getFiredTimedMilestones() { return firedTimedMilestones; }
    public boolean hasFiredTimedMilestone(int sec) { return firedTimedMilestones.contains(sec); }

    /** Cancel every running task. Idempotent — safe to call from end/stop/reset paths. */
    public void cancelAllTasks() {
        if (timerTask != null) { timerTask.cancel(); timerTask = null; }
        if (scoreboardTask != null) { scoreboardTask.cancel(); scoreboardTask = null; }
        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        if (vampireReleaseTask != null) { vampireReleaseTask.cancel(); vampireReleaseTask = null; }
        if (autoStartTask != null) { autoStartTask.cancel(); autoStartTask = null; }
    }

    public void cancelAutoStartTask() {
        if (autoStartTask != null) { autoStartTask.cancel(); autoStartTask = null; }
        autoStartCountdown = -1;
    }
}
