package waterfall.onewire.httpserver.model;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by dwaterfa on 1/14/17.
 */
public class WaitingThread {
    private Thread thread;
    private long waitEndTimeMSec;
    private Timer deadManTimer = null;
    private TimerTask deadManTimerTask = null;

    public WaitingThread() {
        thread = null;
        deadManTimer = null;
    }

    public synchronized boolean setWaitingThread(long waitTimeoutMSec) {
        if (thread == null) {
            thread = Thread.currentThread();
            waitEndTimeMSec = System.currentTimeMillis() + waitTimeoutMSec;
            cancelDeadManTimerTask();
            return true;
        }
        return false;
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

    public void scheduleDeadManTimerTask(BusMasterManager bmManager) {
        deadManTimer = new Timer();
        deadManTimerTask = new DeadManTimerTask(bmManager);
        deadManTimer.schedule(deadManTimerTask, fifteen_seconds_in_msec);
    }

    public boolean isCurrentDeadManTimerTask(TimerTask task) {
        return ((task != null) && (deadManTimerTask == task));
    }

    public synchronized void cancelDeadManTimerTask() {
        if (deadManTimer != null) {
            deadManTimerTask = null;
            deadManTimer.cancel();
            deadManTimer = null;
        }
    }

    // We maintain a dead man timer for the event thread so that when it does not call back within a set period of
    // time we will throw away the state we have maintained on its behalf. We will need an instance of this for every
    // timer we schedule.
    private class DeadManTimerTask extends TimerTask {
        private final BusMasterManager bmManager;

        public DeadManTimerTask(BusMasterManager bmManager) {
            this.bmManager = bmManager;
        }

        // Called when the timer associated with this Task fires off.
        public void run() {
            synchronized(WaitingThread.this) {
            // There is a possible race condition between the event firing off and a waiting thread getting the
            // lock first and clearing the dead man timer. When the thread leaves the timer task instance gets the
            // lock and tries to cancel. So we check to see if the tasks match.
            if (isCurrentDeadManTimerTask(this)) {
                cancelDeadManTimerTask();
                bmManager.cancelAllSearches();
            }
        }

    }

}
