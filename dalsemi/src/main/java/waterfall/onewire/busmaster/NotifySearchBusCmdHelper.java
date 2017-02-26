package waterfall.onewire.busmaster;

import java.util.*;

/**
 * Created by dwaterfa on 11/30/16.
 */
public class NotifySearchBusCmdHelper {

    private BusMaster bm;
    private final boolean isAlarmSearch;
    private SearchPusher searchPusher;
    private HashMap<Object, Long> notifyMap;

    private SearchBusCmd.ResultData lastNotifySearchResultData;

    public NotifySearchBusCmdHelper(BusMaster bm, boolean isAlarmSearch) {
        this.bm = bm;
        this.isAlarmSearch = isAlarmSearch;
        searchPusher = newSearchPusher();
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

        if ((notifyMap != null) && (notifyMap.get(obj) != null)) {
            return BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_NotifyObjAlreadyScheduled;
        }

        if (!bm.getIsStarted()) {
            return BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_BusMasterNotStarted;
        }

        if (notifyMap == null) {
            notifyMap = new HashMap<Object, Long>();
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
            NotifyHelper notifyHelper = new NotifyHelper(bm, obj, lastNotifySearchResultData);
        }

        return BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success;
    }

    /**
     * Called from the owning BusMaster to update the period of an existing search push.
     */
    public synchronized BusMaster.UpdateScheduledNotifySearchBusCmdResult updateScheduledSearchNotifyFor(NotifySearchBusCmdResult obj, long minPeriodMSec) {

        if (minPeriodMSec <= 0) {
            return BusMaster.UpdateScheduledNotifySearchBusCmdResult.USNSBC_MinPeriodInvalid;
        }

        if ((obj == null) || (notifyMap != null) && (notifyMap.get(obj) == null)) {
            return BusMaster.UpdateScheduledNotifySearchBusCmdResult.USNSBC_NotifyObjNotAlreadyScheduled;
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

        if (obj == null) {
            return BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_NotifyObjNotAlreadyScheduled;
        }

        if ((notifyMap != null) && (notifyMap.get(obj) == null)) {
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
     */
    public synchronized void cancelAllScheduledSearchNotifyFor() {
        if ((notifyMap != null) && (notifyMap.size() > 0)) {
            notifyMap.clear();
            searchPusher.adjustPeriod(Long.MAX_VALUE);
        }
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
            NotifyHelper notifyHelper = new NotifyHelper(bm, notifyMap, searchResultData);
        }
    }

    /**
     * Calculates the current min period from the specified map.
     * @param notifyMap
     * @return
     */
    private static long calculateMinPeriodMSecFromMap(Map<Object, Long> notifyMap) {
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
     * This nested abstract class is responsible for pushing search requests to the bus though an appropriate means and
     * thus triggering a resulting callback on BusMaster.searchBusCmdExecuteCallback() which will then be routed
     * here to the method notifySearchResult().
     * <p>
     * How it does this is why it is abstract.
     * <p>
     * The caller of these methods will be the NotifySearchBusCmdHelper and will always be done with a lock held on it so
     * it is important to avoid situations where we deadlock. One way to avoid that is to ensure that all actual push
     * operations are done on a different thread and these methods simply manage the lifetime of that thread.
     * <p>
     * This is a nested class so the members of the NotifySearchBusCmdHelper are available to us here.
     */
    protected abstract class SearchPusher {

        /**
         * This method is used to start, change the period and stop, all through the value passed in.
         *
         * If the value is Long.MAX_VALUE then it will be stopped.
         * If the value is the same or greater than the last set value, then no change to the period will occur.
         * If the value is less than the last set value, or a value has never been set, then it will start.
         *
         * @param minPeriodMSec the new minimum search period for this Helper
         * @return true if an immediate SearchBusCmd has been triggered by this call, or false otherwise.
         */
        public abstract boolean adjustPeriod(long minPeriodMSec);

    }

    /**
     * This overridable method returns a SearchPusher instance appropriate to this class. By default it returns a
     * conventional pusher which uses the Timer to exectute a SearchBusCmd against the BusMaster.
     *
     * @return SearchPusher instance for this class
     */
    protected SearchPusher newSearchPusher() {
        return new SearchPusherByBusCmd();
    }

    /**
     * This private class does the dirty work of calling back by a different thread the registered Objects with the
     * result of a search
     */
    private class NotifyHelper implements Runnable {
        final private Object[] objs;
        final private BusMaster bm;
        final private SearchBusCmd.ResultData searchResultData;
        final private Thread thread;

        public NotifyHelper(final BusMaster bm, final Object obj, final SearchBusCmd.ResultData searchResultData) {
            this.bm = bm;
            this.objs = new Object[]{obj};
            this.searchResultData = searchResultData;
            this.thread = new Thread(this);
            this.thread.setDaemon(true);
            this.thread.start();
        }

        public NotifyHelper(final BusMaster bm, final Map<Object, Long> notifyMap, final SearchBusCmd.ResultData searchResultData) {
            this.bm = bm;
            this.objs = new Object[notifyMap.size()];
            int i = 0;
            for (Object obj : notifyMap.keySet()) {
                objs[i++] = obj;
            }
            this.searchResultData = searchResultData;
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

    /**
     * This nested class implements the SearchPusher through a Timer and a call to a conventional SearchBusCmd on
     * the BusMaster.
     */
    protected class SearchPusherByBusCmd extends SearchPusher {
        private Timer timer = null;
        private long currentPeriodMSec;

        private Thread searchBusCmdThread;
        private long lastPushTimeMSec;
        private SearchBusCmd searchBusCmd;

        public SearchPusherByBusCmd() {
            super();
            this.timer = null;
            this.currentPeriodMSec = Long.MAX_VALUE;
            this.searchBusCmdThread = null;
            this.lastPushTimeMSec = 0;
            this.searchBusCmd = null;
        }

        // This implementation is smart enough to distinguish first start from a true adjustment.
        public synchronized boolean adjustPeriod(long minPeriodMSec) {

            if (minPeriodMSec == currentPeriodMSec) {
                // same value nothing to do.
                return false;
            }

            if (minPeriodMSec == Long.MAX_VALUE) {
                // shutting down
                timer.cancel();
                timer = null;
                currentPeriodMSec = Long.MAX_VALUE;
                return false;
            }

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
         * Called as TimerTask from our Timer. Because of the design of Timer each TimerTask instance can only be used
         * once so we have to make a new one of these whenever the Timer period changes.
         */
        public class PushTimerTask extends TimerTask {

            public void run() {
                synchronized (SearchPusherByBusCmd.this) {
                    if (SearchPusherByBusCmd.this.searchBusCmdThread != null) {
                        return;
                    }
                    SearchPusherByBusCmd.this.searchBusCmdThread = Thread.currentThread();
                    SearchPusherByBusCmd.this.lastPushTimeMSec = System.currentTimeMillis();
                }

                // now that ourselves are marked as the search thread, we can get on with it
                try {
                    if (SearchPusherByBusCmd.this.searchBusCmd == null) {
                        SearchPusherByBusCmd.this.searchBusCmd = (NotifySearchBusCmdHelper.this.isAlarmSearch ?
                                NotifySearchBusCmdHelper.this.bm.querySearchBusByAlarmCmd(Logger.LogLevel.CmdOnlyLevel())
                                : NotifySearchBusCmdHelper.this.bm.querySearchBusCmd(Logger.LogLevel.CmdOnlyLevel()));
                    }

                    // The command will internally call back to this class when it is successful, so
                    // all we need to do here is just push.
                    SearchPusherByBusCmd.this.searchBusCmd.execute();

                } finally {
                    synchronized (SearchPusherByBusCmd.this) {
                        SearchPusherByBusCmd.this.searchBusCmdThread = null;
                        if (SearchPusherByBusCmd.this.timer == null) {
                            SearchPusherByBusCmd.this.searchBusCmd = null;
                        }
                    }
                }
            }
        }
    }

}
