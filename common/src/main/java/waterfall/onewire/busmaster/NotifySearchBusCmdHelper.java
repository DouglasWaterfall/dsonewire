package waterfall.onewire.busmaster;

import java.util.*;

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
     * Called from the owning BusMaster to schedule a search push.
     */
    public synchronized BusMaster.ScheduleNotifySearchBusCmdResult scheduleSearchNotifyFor(NotifySearchBusCmdResult obj, long minPeriodMSec) {
        if (obj == null) {
            return BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_NotifyObjNull;
        }

        if (minPeriodMSec <= 0) {
            return BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_MinPeriodInvalid;
        }

        if (!bm.getIsStarted()) {
            return BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_BusMasterNotStarted;
        }

        if ((notifyMap != null) && (notifyMap.get(obj) != null)) {
            return BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_NotifyObjAlreadyScheduled;
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
        if ((!pushStarted) && (lastNotifySearchResultData != null) &&
                ((bm.getCurrentTimeMillis() - lastNotifySearchResultData.getWriteCTM()) <= minPeriodMSec)) {
            // this will occur on a new thread
            NotifyHelper notifyHelper = new NotifyHelper(bm, obj, lastNotifySearchResultData, searchPusher.isAlarmSearch());
        }

        return BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success;
    }

    /**
     * Called from the owning BusMaster to update the period of an existing search push.
     */
    public synchronized BusMaster.UpdateScheduledNotifySearchBusCmdResult updateScheduledSearchNotifyFor(NotifySearchBusCmdResult obj, long minPeriodMSec) {

        if ((obj == null) || (notifyMap != null) && (notifyMap.get(obj) == null)) {
            return BusMaster.UpdateScheduledNotifySearchBusCmdResult.USNSBC_NotifyObjNotAlreadyScheduled;
        }

        if (minPeriodMSec <= 0) {
            return BusMaster.UpdateScheduledNotifySearchBusCmdResult.USNSBC_MinPeriodInvalid;
        }

        Long currentMinPeriodMSec = notifyMap.get(obj);
        if (currentMinPeriodMSec == minPeriodMSec) {
            return BusMaster.UpdateScheduledNotifySearchBusCmdResult.USNSBC_MinPeriodUnchanged;
        }

        notifyMap.put(obj, minPeriodMSec);

        searchPusher.adjustPeriod(calculateMinPeriodMSecFromMap(notifyMap));

        return BusMaster.UpdateScheduledNotifySearchBusCmdResult.USNSBC_Success;
    }

    /**
     * Called from the owning BusMaster to cancel an existing search push.
     */
    public synchronized BusMaster.CancelScheduledNotifySearchBusCmdResult cancelScheduledSearchNotifyFor(NotifySearchBusCmdResult obj) {

        if ((obj == null) || (notifyMap != null) && (notifyMap.get(obj) == null)) {
            return BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_NotifyObjNotAlreadyScheduled;
        }

        notifyMap.remove(obj);

        if (notifyMap.size() == 0) {
            // We are the last one out.
            searchPusher.adjustPeriod(Long.MAX_VALUE);
        } else {
            searchPusher.adjustPeriod(calculateMinPeriodMSecFromMap(notifyMap));
        }

        return BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_Success;
    }

    /**
     * Called from the owning BusMaster to cancel all searches.
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
     *
     * @param searchResultData
     */
    public synchronized void notifySearchResult(final SearchBusCmd.ResultData searchResultData) {
        boolean crc32Changed = ((lastNotifySearchResultData == null) ||
                (searchResultData.getListCRC32() != lastNotifySearchResultData.getListCRC32()));

        lastNotifySearchResultData = searchResultData;

        if ((notifyMap.size() > 0) && (crc32Changed)) {
            // this will occur on a new thread
            NotifyHelper notifyHelper = new NotifyHelper(bm, notifyMap, searchResultData, searchPusher.isAlarmSearch());
        }
    }

    /**
     * Calculates the current min period from the specified map.
     * @param notifyMap
     * @return
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
     * This private class does the dirty work of calling back by a different thread the registered Objects with the
     * result of a search
     */
    private class NotifyHelper implements Runnable {
        private final BusMaster bm;
        private final Object[] objs;
        private final SearchBusCmd.ResultData searchResultData;
        private final boolean isAlarmSearch;
        private final Thread thread;

        public NotifyHelper(final BusMaster bm, final Object obj, final SearchBusCmd.ResultData searchResultData, boolean isAlarmSearch) {
            this.bm = bm;
            this.objs = new Object[]{obj};
            this.searchResultData = searchResultData;
            this.isAlarmSearch = isAlarmSearch;
            this.thread = new Thread(this);
            this.thread.setDaemon(true);
            this.thread.start();
        }

        public NotifyHelper(final BusMaster bm, final Map<Object, Long> notifyMap, final SearchBusCmd.ResultData searchResultData, boolean isAlarmSearch) {
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
