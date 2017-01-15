package waterfall.onewire.httpserver;

import waterfall.onewire.HttpClient.WaitForEventCmd;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by dwaterfa on 1/14/17.
 */
public class WaitingThreadData {
    private Thread thread;
    private long waitEndTimeMSec;
    private Timer deadManTimer = null;
    private TimerTask deadManTimerTask = null;

    public WaitingThreadData() {
        thread = null;
        deadManTimer = null;
    }

    public boolean hasWaitingThread() {
        return (thread != null);
    }

    public void setWaitingThread(WaitForEventCmd cmd) {
        cancelDeadManTimerTask();
        thread = Thread.currentThread();
        waitEndTimeMSec = System.currentTimeMillis() + cmd.getWaitTimeoutMSec();
    }

    // returns true if at the start of the call the current time exceeds the timeout end point.
    public boolean waitUntilTimeout() {
        long timeRemainingMSec = (waitEndTimeMSec - System.currentTimeMillis());

        if (timeRemainingMSec <= 0) {
            return true;
        }

        try {
            thread.wait(timeRemainingMSec);
        } catch (InterruptedException e) {
            // we have woken up due to the timeout or because someone has notified us.
        }

        return false;
    }

    public void notifyWaitingThreadIfPresent() {
        if (thread != null) {
            thread.notify();
        }
    }

    public void clearWaitingThread() {
        thread = null;
    }

    private final static long fifteen_seconds_in_msec = (1000 * 15);

    public void scheduleDeadManTimerTask(TimerTask task) {
        deadManTimer = new Timer();
        deadManTimerTask = task;
        deadManTimer.schedule(deadManTimerTask, fifteen_seconds_in_msec);
    }

    public boolean isCurrentDeadManTimerTask(TimerTask task) {
        return ((task != null) && (deadManTimerTask == task));
    }

    public void cancelDeadManTimerTask() {
        if (deadManTimer != null) {
            deadManTimerTask = null;
            deadManTimer.cancel();
            deadManTimer = null;
        }
    }

}
