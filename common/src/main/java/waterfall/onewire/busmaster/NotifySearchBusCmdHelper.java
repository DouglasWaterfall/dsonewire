package waterfall.onewire.busmaster;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dwaterfa on 11/30/16.
 */
public class NotifySearchBusCmdHelper {

  private final SearchPusher searchPusher;
  private final BusMaster bm;
  private final HashMap<Object, Long> notifyMap;
  private SearchBusCmd.ResultData lastNotifySearchResultData;

  public NotifySearchBusCmdHelper(SearchPusher searchPusher, BusMaster bm) {
    if (searchPusher == null) {
      throw new IllegalArgumentException("searchPusher");
    }
    this.searchPusher = searchPusher;
    if (bm == null) {
      throw new IllegalArgumentException("bm");
    }
    this.bm = bm;
    notifyMap = new HashMap<Object, Long>();
    lastNotifySearchResultData = null;
  }

  /**
   * Calculates the current min period from the specified map.
   */
  public static long calculateMinPeriodMSecFromMap(Map<Object, Long> notifyMap) {
    // Calculate what the new period needs to be.
    long newMinPeriodMSec = Long.MAX_VALUE;
    for (Long l : notifyMap.values()) {
      if (l < newMinPeriodMSec) {
        newMinPeriodMSec = l.longValue();
      }
    }
    return newMinPeriodMSec;
  }

  /**
   * Called from the owning BusMaster to schedule a search push.
   */
  public synchronized void scheduleSearchNotifyFor(NotifySearchBusCmdResult obj,
      long minPeriodMSec) throws IllegalArgumentException {
    if (obj == null) {
      throw new IllegalArgumentException("SNSBCR_NotifyObjNull");
    }

    if (minPeriodMSec <= 0) {
      throw new IllegalArgumentException("SNSBCR_MinPeriodInvalid");
    }

    if ((notifyMap != null) && (notifyMap.get(obj) != null)) {
      throw new IllegalArgumentException("SNSBCR_NotifyObjAlreadyScheduled");
    }

    notifyMap.put(obj, new Long(minPeriodMSec));

    boolean pushStarted = searchPusher.adjustPeriod(calculateMinPeriodMSecFromMap(notifyMap));

    // A push might start immediately, or one might already be in progress, if the last time we pushed exceeded the
    // desired period.
    //
    // If nothing is known to start immediately and we have data sitting around which is valid withing the desired
    // time period then we can notify the Object with that data.
    //
    // Essentially we are saying that if a push started we will wait for that new data to come along rather than
    // use old, but still potentially within the time period, data.
    //
    // The callee is free to call back here and deregister themselves after the notification if they found what
    // they were looking for.
    if (pushStarted) {
      // we do this because if we've started a new search for a new caller and the result is the exactly the
      // same as the previous value then we will not update anyone...including the new caller. The other waiters
      // will be notified again but they have to tolerate that.
      lastNotifySearchResultData = null;
    } else if ((lastNotifySearchResultData != null) &&
        ((bm.getCurrentTimeMillis() - lastNotifySearchResultData.getWriteCTM()) <= minPeriodMSec)) {
      // this will occur on a new thread
      NotifyHelper notifyHelper = new NotifyHelper(bm, obj, lastNotifySearchResultData,
          searchPusher.isAlarmSearch());
    }
  }

  /**
   * Called from the owning BusMaster to update the period of an existing search push.
   */
  public synchronized void updateScheduledSearchNotifyFor(NotifySearchBusCmdResult obj,
      long minPeriodMSec) throws IllegalArgumentException {

    if ((obj == null) || (notifyMap != null) && (notifyMap.get(obj) == null)) {
      throw new IllegalArgumentException("USNSBC_NotifyObjNotAlreadyScheduled");
    }

    if (minPeriodMSec <= 0) {
      throw new IllegalArgumentException("USNSBC_MinPeriodInvalid");
    }

    Long currentMinPeriodMSec = notifyMap.get(obj);
    if (currentMinPeriodMSec == minPeriodMSec) {
      throw new IllegalArgumentException("USNSBC_MinPeriodUnchanged");
    }

    notifyMap.put(obj, minPeriodMSec);

    searchPusher.adjustPeriod(calculateMinPeriodMSecFromMap(notifyMap));
  }

  /**
   * Called from the owning BusMaster to cancel an existing search push.
   */
  public synchronized void cancelScheduledSearchNotifyFor(NotifySearchBusCmdResult obj)
      throws IllegalArgumentException {

    if ((obj == null) || (notifyMap != null) && (notifyMap.get(obj) == null)) {
      throw new IllegalArgumentException("CSNSBC_NotifyObjNotAlreadyScheduled");
    }

    notifyMap.remove(obj);

    if (notifyMap.size() == 0) {
      // We are the last one out.
      searchPusher.adjustPeriod(Long.MAX_VALUE);
    } else {
      searchPusher.adjustPeriod(calculateMinPeriodMSecFromMap(notifyMap));
    }
  }

  /**
   * Called from the owning BusMaster to cancel all searches.
   *
   * @return true if any active searches were cancelled
   */
  public synchronized boolean cancelAllScheduledSearchNotifyFor() {
    if ((notifyMap != null) && (notifyMap.size() > 0)) {
      notifyMap.clear();
      searchPusher.adjustPeriod(Long.MAX_VALUE);
      return true;
    }
    return false;
  }

  /**
   * This method is called from the owning BusMaster when the SearchCmd successfully completes.
   */
  public synchronized void notifySearchResult(final SearchBusCmd.ResultData searchResultData) {
    if (searchResultData == null) {
      throw new IllegalArgumentException("searchResultData null");
    }

    boolean crc32Changed = ((lastNotifySearchResultData == null) ||
        (searchResultData.getListCRC32() != lastNotifySearchResultData.getListCRC32()));

    lastNotifySearchResultData = searchResultData;

    if ((notifyMap.size() > 0) && (crc32Changed)) {
      // this will occur on a new thread
      NotifyHelper notifyHelper = new NotifyHelper(bm, notifyMap, searchResultData,
          searchPusher.isAlarmSearch());
    }
  }

  /**
   * This private class does the dirty work of calling back by a different thread the registered
   * Objects with the result of a search
   */
  private class NotifyHelper implements Runnable {

    private final BusMaster bm;
    private final Object[] objs;
    private final SearchBusCmd.ResultData searchResultData;
    private final boolean isAlarmSearch;
    private final Thread thread;

    public NotifyHelper(final BusMaster bm, final Object obj,
        final SearchBusCmd.ResultData searchResultData, boolean isAlarmSearch) {
      this.bm = bm;
      this.objs = new Object[]{obj};
      this.searchResultData = searchResultData;
      this.isAlarmSearch = isAlarmSearch;
      this.thread = new Thread(this);
      this.thread.setDaemon(true);
      this.thread.start();
    }

    public NotifyHelper(final BusMaster bm, final Map<Object, Long> notifyMap,
        final SearchBusCmd.ResultData searchResultData, boolean isAlarmSearch) {
      this.bm = bm;
      this.objs = new Object[notifyMap.size()];
      int i = 0;
      for (Object obj : notifyMap.keySet()) {
        objs[i++] = obj;
      }
      this.searchResultData = searchResultData;
      this.isAlarmSearch = isAlarmSearch;
      this.thread = new Thread(this);
      this.thread.setDaemon(true);
      this.thread.start();
    }

    public void run() {
      for (Object obj : objs) {
        try {
          NotifySearchBusCmdResult.class.cast(obj).notify(bm, isAlarmSearch, searchResultData);
        } catch (Exception e) {
          ;
        }
      }
    }
  }

}
