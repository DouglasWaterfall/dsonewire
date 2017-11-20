package waterfall.onewire.busmaster;

import java.util.Timer;
import java.util.TimerTask;

/**
 * This nested abstract class is responsible for pushing search requests to the bus though an
 * appropriate means and thus triggering a resulting callback on BusMaster.searchBusCmdExecuteCallback()
 * which will then be routed here to the method notifySearchResult(). <p> How it does this is why it
 * is abstract. <p> The caller of these methods will be the NotifySearchBusCmdHelper and will always
 * be done with a lock held on it so it is important to avoid situations where we deadlock. One way
 * to avoid that is to ensure that all actual push operations are done on a different thread and
 * these methods simply manage the lifetime of that thread. <p> This is a nested class so the
 * members of the NotifySearchBusCmdHelper are available to us here.
 */
public abstract class SearchPusher {

  private final boolean isAlarmSearch;
  protected long currentPeriodMSec;
  protected long lastPushTimeMSec;
  private Timer timer;

  protected SearchPusher(boolean isAlarmSearch) {
    this.isAlarmSearch = isAlarmSearch;
    this.timer = null;
    this.currentPeriodMSec = Long.MAX_VALUE;
    this.lastPushTimeMSec = 0;
  }

  /**
   *
   * @return
   */
  public boolean isAlarmSearch() {
    return isAlarmSearch;
  }

  /**
   * This method is used to start, change the period and stop, all through the value passed in. <p>
   * If the value is Long.MAX_VALUE then it will be stopped. If the value is the same or greater
   * than the last set value, then no change to the period will occur. If the value is less than the
   * last set value, or a value has never been set, then it will start. <p> This implementation is
   * smart enough to distinguish first start from a true adjustment.
   *
   * @param minPeriodMSec the new minimum search period for this Helper
   * @return true if an immediate SearchBusCmd has been triggered by this call, or false otherwise.
   */
  public synchronized boolean adjustPeriod(long minPeriodMSec) {

    if (minPeriodMSec == currentPeriodMSec) {
      // same value nothing to do.
      return false;
    }

    if (minPeriodMSec == Long.MAX_VALUE) {
      // shutting down
      if (timer != null) {
        timer.cancel();
        timer = null;
        currentPeriodMSec = Long.MAX_VALUE;
      }
      return false;
    }

    // new period is different than current so we'll need to start a new timer.
    currentPeriodMSec = minPeriodMSec;

    if (timer != null) {
      timer.cancel();
    }
    timer = new Timer(true);

    // if it has been a long time then the data is stale and we need to get on with it.
    long delay = minPeriodMSec - (System.currentTimeMillis() - lastPushTimeMSec);
    if (delay < 0) {
      delay = 0;
    }

    timer.schedule(new PushTimerTask(), delay, currentPeriodMSec);

    return (delay == 0);
  }

  /**
   * This method is called by the PushTimerTask when it is time to launch a new search
   */
  protected abstract void push();

  /**
   *
   * @return
   */
  protected boolean isTimerActive() {
    return (timer != null);
  }

  /**
   *
   */
  protected class PushTimerTask extends TimerTask {

    public void run() {
      synchronized (SearchPusher.this) {
        lastPushTimeMSec = System.currentTimeMillis();
      }
      SearchPusher.this.push();
    }
  }

}
