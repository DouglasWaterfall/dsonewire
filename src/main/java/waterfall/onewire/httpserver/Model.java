package waterfall.onewire.httpserver;

import org.springframework.beans.factory.annotation.Autowired;
import waterfall.onewire.BusMasterRegistry;
import waterfall.onewire.HttpClient.*;
import waterfall.onewire.busmaster.BusMaster;
import waterfall.onewire.busmaster.AlarmSearchBusCmdNotifyResult;
import waterfall.onewire.busmaster.SearchBusCmd;
import waterfall.onewire.busmaster.SearchBusCmdNotifyResult;

import java.util.*;

/**
 * Created by dwaterfa on 1/12/17.
 */
public class Model {

    private final BusMasterRegistry bmRegistry;
    private final Base64.Encoder encoder;

    // This is essentially a timestamp which helps the client keep in sync with the server. If the server restarts
    // it will change this value which will tell the client that it needs to throw away its state and resychronize
    // with the server here.
    private final long startTimestampMSec;

    // TODO - this is still evolving
    // we would like to provide some sort of login/authentication relative to the client system so it could know that
    // is in control. For now it is just an encoding of the start time and all methods on the controller require that
    // this value be provided in the rest calls.
    private String currentAuthenticationValue;

    // The current thread which is sitting in waitForEvent()
    private WaitingThreadData waitingThreadData;

    private class BusMasterUpdateData {
        private long timestampMSec;
        private HashMap<String, BusMasterData> bmIdentMap;

        public BusMasterUpdateData() {
            this.timestampMSec = 0;
            this.bmIdentMap = new HashMap<String, BusMasterData>();
        }

        public BusMaster getBusMasterByIdent(String bmIdent) {
            BusMasterData bmData = bmIdentMap.get(bmIdent);
            if (bmData != null) {
                return bmData.bm;
            }
            return null;
        }

        public BusMasterData findBusMasterDataFor(BusMaster bm) {
            for (BusMasterData bmData : bmIdentMap.values()) {
                if (bmData.bm == bm) {
                    return bmData;
                }
            }

            return null;
        }

        public void addBusMaster(BusMaster bm) {
            String bmIdent = encoder.encodeToString(bm.getName().getBytes());

            if (!bmIdentMap.containsKey(bmIdent)) {
                System.out.println("Adding busMaster" + bm.getName());
                bmIdentMap.put(bmIdent, new BusMasterData(bm, bmIdent));
                timestampMSec = System.currentTimeMillis();
                waitingThreadData.notifyWaitingThreadIfPresent();
            } else if (bmIdentMap.get(bmIdent) == bm) {
                System.err.println("Duplicate add of busMaster" + bm.getName());
            } else {
                System.err.println("name encoding collision of busMaster:" + bm.getName() + " and " + bmIdentMap.get(bmIdent).bm.getName());
            }
        }

        // public void removeBusMaster(BusMaster bm)

        // returns an Error enum if the notifyTimestamps are for BusMasters which are not known of the searches have
        // been cancelled for the BusMaster
        public WaitForEventResult.ControllerErrors validateSearchTimestamps(Map<String, Long> searchNotifyTimestampMSec) {
            if (searchNotifyTimestampMSec != null) {
                for (String dsAddr : searchNotifyTimestampMSec.keySet()) {
                    BusMasterData bmData = bmIdentMap.get(dsAddr);
                    if (bmData == null) {
                        return WaitForEventResult.ControllerErrors.SearchBMIdentUnknown;
                    } else if (bmData.searchCancelled) {
                        return WaitForEventResult.ControllerErrors.SearchesCancelled;
                    }
                }
            }

            return null;
        }

        public ArrayList<WaitForEventResult.BMSearchData> getSearchDataRelativeTo(Map<String, Long> searchNotifyTimestampMSec) {
            ArrayList<WaitForEventResult.BMSearchData> list = null;

            for (BusMasterData bmData : bmIdentMap.values()) {
                // The event thready may not know about searches which have been scheduled after it went to sleep
                // so finding an empty reference in the command data is equivalent to never having seen the current
                // search result.

                WaitForEventResult.BMSearchData bmSearchData = null;

                if ((bmData.searchNotifyObj != null) &&
                        ((bmSearchData = bmData.getSearchDataRelativeTo(searchNotifyTimestampMSec.get(bmData.bmIdent))) != null)) {
                    if (list == null) {
                        list = new ArrayList<>();
                    }
                    list.add(bmSearchData);
                }
            }

            return list;
        }

        public ArrayList<WaitForEventResult.BMSearchData> getAlarmSearchDataRelativeTo(Map<String, Long> alarmSearchNotifyTimestampMSec) {
            ArrayList<WaitForEventResult.BMSearchData> list = null;

            for (BusMasterData bmData : bmIdentMap.values()) {
                // The event thready may not know about searches which have been scheduled after it went to sleep
                // so finding an empty reference in the command data is equivalent to never having seen the current
                // search result.

                WaitForEventResult.BMSearchData bmSearchData = null;

                if ((bmData.alarmSearchNotifyObj != null) &&
                        ((bmSearchData = bmData.getAlarmSearchDataRelativeTo(alarmSearchNotifyTimestampMSec.get(bmData.bmIdent))) != null)) {
                    if (list == null) {
                        list = new ArrayList<>();
                    }
                    list.add(bmSearchData);
                }
            }

            return list;
        }

        public boolean hasActiveSearches() {
            for (BusMasterData bmData : bmIdentMap.values()) {
                if ((!bmData.searchCancelled) && ((bmData.searchNotifyObj != null) ||
                        (bmData.alarmSearchNotifyObj != null))) {
                    return true;
                }
            }
            return false;
        }


        /**
         * Cancel any searches outstanding for all BMs and reset the searchCancelled flag.
         * <p>
         * Called under lock
         */
        public void cancelAllSearches() {
            for (BusMasterData bmData : bmIdentMap.values()) {
                if (!bmData.searchCancelled) {
                    if (bmData.searchNotifyObj != null) {
                        cancelSearchFor(bmData);
                    }
                    if (bmData.alarmSearchNotifyObj != null) {
                        cancelAlarmSearchFor(bmData);
                    }

                    bmData.searchCancelled = true;
                }
            }
        }

   }

    private final BusMasterUpdateData bmUpdateData;

    @Autowired
    public Model(BusMasterRegistry bmRegistry) {
        this.bmRegistry = bmRegistry;
        this.bmUpdateData = new BusMasterUpdateData();
        this.encoder = Base64.getUrlEncoder();
        this.startTimestampMSec = System.currentTimeMillis();
        this.currentAuthenticationValue = Base64.getUrlEncoder().encodeToString(new Date(startTimestampMSec).toString().getBytes());

        // we want to find out about BusMasters added to the BusMasterRegistry.
        bmRegistry.addObserver(new myBMSearchResult());
    }

    public String getCurrentAuthenticationValue() {
        return currentAuthenticationValue;
    }

    /**
     * Checks that the authentication string matches the currentAuthenticationValue or throws an exception
     *
     * @param authentication
     * @throws AuthException
     */
    public void checkAuthenticationHeader(String authentication) throws AuthException {
        if ((authentication == null) ||
                (currentAuthenticationValue == null) ||
                (!authentication.equals(currentAuthenticationValue))) {
            throw new AuthException();
        }
    }

    /**
     * Returns a BusMaster for a bmIdent
     *
     * @param bmIdent
     * @return the BusMaster, or null if the bmIdent is not known to the Model.
     */
    public BusMaster getBusMasterByIdent(String bmIdent) {
        return bmUpdateData.getBusMasterByIdent(bmIdent);
    }

    /**
     * Schedule a search bus using the specified interval for the given bus
     *
     * @param bmData
     * @param minPeriodMSec
     * @return BusMaster.ScheduleSearchResult
     */
    public synchronized BusMaster.ScheduleSearchResult scheduleSearchFor(BusMasterData bmData, long minPeriodMSec) {
        if (bmData.searchNotifyObj != null) {
            return BusMaster.ScheduleSearchResult.SSR_NotifyObjAlreadyScheduled;
        }

        mySearchBusCmdNotifyResult t = new mySearchBusCmdNotifyResult();
        BusMaster.ScheduleSearchResult result = bmData.bm.scheduleSearchNotifyFor(t, minPeriodMSec);
        if (result == BusMaster.ScheduleSearchResult.SSR_Success) {
            bmData.searchCancelled = false;
            bmData.searchNotifyObj = t;
            if (!waitingThreadData.hasWaitingThread()) {
                waitingThreadData.scheduleDeadManTimerTask(new DeadManTimerTask());
            }
        }

        return result;
    }

    /**
     * Cancel a search for the given bus.
     *
     * @param bmData
     * @return true if a search existed and was cancelled, false otherwise
     */
    public synchronized boolean cancelSearchFor(BusMasterData bmData) {
        if ((!bmData.searchCancelled) && (bmData.searchNotifyObj != null)) {
            bmData.bm.cancelSearchNotifyFor(bmData.searchNotifyObj);
            bmData.searchNotifyObj = null;
            bmData.searchNotifyResultData = null;
            return true;
        }

        return false;
    }

    /**
     * Schedule an alarm search bus using the specified interval for the given bus
     *
     * @param bmData
     * @param minPeriodMSec
     * @return BusMaster.ScheduleSearchResult
     */
    public synchronized BusMaster.ScheduleSearchResult scheduleAlarmSearchFor(BusMasterData bmData, long minPeriodMSec) {
        if (bmData.alarmSearchNotifyObj != null) {
            return BusMaster.ScheduleSearchResult.SSR_NotifyObjAlreadyScheduled;
        }

        myAlarmSearchBusCmdNotifyResult t = new myAlarmSearchBusCmdNotifyResult();
        BusMaster.ScheduleSearchResult result = bmData.bm.scheduleAlarmSearchNotifyFor(t, minPeriodMSec);
        if (result == BusMaster.ScheduleSearchResult.SSR_Success) {
            bmData.searchCancelled = false;
            bmData.alarmSearchNotifyObj = t;
            if (!waitingThreadData.hasWaitingThread()) {
                waitingThreadData.scheduleDeadManTimerTask(new DeadManTimerTask());
            }
        }

        return result;
    }

    /**
     * Cancel an alarm search for the given bus.
     *
     * @param bmData
     * @return true if a search existed and was cancelled, false otherwise
     */
    public synchronized boolean cancelAlarmSearchFor(BusMasterData bmData) {
        if ((!bmData.searchCancelled) && (bmData.alarmSearchNotifyObj != null)) {
            bmData.bm.cancelAlarmSearchNotifyFor(bmData.alarmSearchNotifyObj);
            bmData.alarmSearchNotifyObj = null;
            bmData.alarmSearchNotifyResultData = null;
            return true;
        }

        return false;
    }

    /**
     * @param cmd
     * @return
     */
    public synchronized WaitForEventResult waitForEvent(WaitForEventCmd cmd) {
        if (waitingThreadData.hasWaitingThread()) {
            return new WaitForEventControllerError(WaitForEventResult.ControllerErrors.InternalError_ThreadAlreadyWaiting);
        }

        boolean checkedSearchArgsValid = false;

        try {
            waitingThreadData.setWaitingThread(cmd);

            if (cmd.getServerTimestampMSec() != startTimestampMSec) {
                bmUpdateData.cancelAllSearches();
                return new WaitForEventBMChanged(new WaitForEventResult.BMListChangedData(startTimestampMSec, bmUpdateTimestampMSec, bmUpdateList));
            }

            while (true) {
                // make sure the bmlists are in sync
                if ((bmUpdateList != null) && (cmd.getBMListChangedNotifyTimestampMSec() != bmUpdateTimestampMSec)) {
                    return new WaitForEventBMChanged(new WaitForEventResult.BMListChangedData(startTimestampMSec, bmUpdateTimestampMSec, bmUpdateList));
                }

                // We only do this once after we are sure that the bm lists are at least in sync. There are race
                // conditions here between what we are called with and someone adding or cancelling a search so it is
                // difficult for us to really validate the data other than making sure we are not asking about a bm
                // which does not exist.
                if (!checkedSearchArgsValid) {
                    WaitForEventResult.ControllerErrors controllerError = null;

                    if (((controllerError = bmUpdateData.validateSearchTimestamps(cmd.getBMSearchNotifyTimestampMSec())) != null) ||
                            ((controllerError = bmUpdateData.validateSearchTimestamps(cmd.getBMAlarmSearchNotifyTimestampMSec())) != null)) {
                      return new WaitForEventControllerError(controllerError);
                    }

                    checkedSearchArgsValid = true;
                }

                ArrayList<WaitForEventResult.BMSearchData> bmSearchDataList = bmUpdateData.getSearchDataRelativeTo(cmd.getBMSearchNotifyTimestampMSec());

                ArrayList<WaitForEventResult.BMSearchData> bmAlarmSearchDataList = bmUpdateData.getAlarmSearchDataRelativeTo(cmd.getBMAlarmSearchNotifyTimestampMSec());

                if ((bmSearchDataList != null) || (bmAlarmSearchDataList != null)) {
                    return new WaitForEventSearchNotify(bmSearchDataList != null ? bmSearchDataList.toArray(new WaitForEventResult.BMSearchData[bmSearchDataList.size()]) : null,
                            bmAlarmSearchDataList != null ? bmAlarmSearchDataList.toArray(new WaitForEventResult.BMSearchData[bmAlarmSearchDataList.size()]) : null);
                }

                // if the timeout time has passed at the start of this method then it returns true and we can return.
                // otherwise we woke up either due to a notify or a timeout so loop around to see if there is an event
                // to pickup and we will return back here and detect the timeout case then.
                if (waitingThreadData.waitUntilTimeout()) {
                    return new WaitForEventControllerError(WaitForEventResult.ControllerErrors.WaitTimeout);
                }
            }
        } finally {
            waitingThreadData.clearWaitingThread();

            if (bmUpdateData.hasActiveSearches()) {
                waitingThreadData.scheduleDeadManTimerTask(new DeadManTimerTask());
            }
        }
    }

    public class AuthException extends RuntimeException {
        public AuthException() {
            super();
        }
    }

    // We maintain a dead man timer for the event thread so that when it does not call back within a set period of
    // time we will throw away the state we have maintained on its behalf. We will need an instance of this for every
    // timer we schedule.
    private class DeadManTimerTask extends TimerTask {

        // Called when the timer associated with this Task fires off.
        public void run() {
            Model.this.deadManTimerTaskNotify(this);
        }

    }

    private synchronized void deadManTimerTaskNotify(DeadManTimerTask task) {
        // There is a possible race condition between the event firing off and a waiting thread getting the
        // lock first and clearing the dead man timer. When the thread leaves the timer task instance gets the
        // lock and tries to cancel. So we check to see if the tasks match.
        if (waitingThreadData.isCurrentDeadManTimerTask(task)) {
            waitingThreadData.cancelDeadManTimerTask();
            bmUpdateData.cancelAllSearches();
        }
    }

    private class myBMSearchResult implements Observer {
        // Called from the BusMasterRegistry when the BusMaster list changes.
        @Override
        public void update(Observable o, Object arg) {
            if ((o instanceof BusMasterRegistry) && (arg instanceof BusMaster)) {
                Model.this.bmRegistryUpdate((BusMasterRegistry) o, (BusMaster) arg);
            }
        }
    }

    private synchronized void bmRegistryUpdate(BusMasterRegistry bmRegistry, BusMaster bm) {
        bmUpdateData.addBusMaster(bm);
    }

    private class mySearchBusCmdNotifyResult implements SearchBusCmdNotifyResult {

        @Override
        public void notify(BusMaster bm, SearchBusCmd.ResultData searchResultData) {
            Model.this.searchBusNotify(this, bm, searchResultData);
        }
    }

    private synchronized void searchBusNotify(mySearchBusCmdNotifyResult notifyResult, BusMaster bm,
                                              SearchBusCmd.ResultData searchResultData) {
        BusMasterData bmData = bmUpdateData.findBusMasterDataFor(bm);
        // be cautious
        if ((bmData != null) && (bmData.updateSearchData(notifyResult, searchResultData))) {
            waitingThreadData.notifyWaitingThreadIfPresent();
        }
    }

    private class myAlarmSearchBusCmdNotifyResult implements AlarmSearchBusCmdNotifyResult {

        @Override
        public void notify(BusMaster bm, SearchBusCmd.ResultData searchResultData) {
            Model.this.alarmSearchBusNotify(this, bm, searchResultData);
        }
    }

    private synchronized void alarmSearchBusNotify(myAlarmSearchBusCmdNotifyResult notifyResult, BusMaster bm,
                                                   SearchBusCmd.ResultData searchResultData) {
        BusMasterData bmData = bmUpdateData.findBusMasterDataFor(bm);
        // be cautious
        if ((bmData != null) && (bmData.updateAlarmSearchData(notifyResult, searchResultData))) {
            waitingThreadData.notifyWaitingThreadIfPresent();
        }
    }

}
