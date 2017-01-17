package waterfall.onewire.httpserver;

import org.springframework.beans.factory.annotation.Autowired;
import waterfall.onewire.BusMasterRegistry;
import waterfall.onewire.HttpClient.*;
import waterfall.onewire.busmaster.BusMaster;
import waterfall.onewire.busmaster.AlarmSearchBusCmdNotifyResult;
import waterfall.onewire.busmaster.NotifySearchBusCmdResult;
import waterfall.onewire.busmaster.SearchBusCmd;

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

    private final BusMasterTracker bmUpdateData;

    @Autowired
    public Model(BusMasterRegistry bmRegistry) {
        this.bmRegistry = bmRegistry;
        this.bmUpdateData = new BusMasterTracker();
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
     * Returns the BusMasterData managing the BusMaster
     *
     * @param bmIdent
     * @return
     */
    public synchronized BusMasterData getBusMasterDataByIdent(String bmIdent) {
        return bmUpdateData.getBusMasterDataByIdent(bmIdent);
    }

    /**
     * Returns a BusMaster for a bmIdent
     *
     * @param bmIdent
     * @return the BusMaster, or null if the bmIdent is not known to the Model.
     */
    public BusMaster getBusMasterByIdent(String bmIdent) {
        BusMasterData bmData = getBusMasterDataByIdent(bmIdent);
        if (bmData != null) {
            return bmData.bm;
        }
        return null;
    }

    /**
     *
     */
    public boolean isBMIdentValid(String bmIdent) {
        return (getBusMasterDataByIdent(bmIdent) != null);
    }

    /**
     * Schedule a search bus using the specified interval for the given bus
     *
     * @param bmIdent
     * @param byAlarm true if this for Alarm searches, false if general search
     * @param minPeriodMSec
     * @return BusMaster.ScheduleNotifySearchBusCmdResult
     */
    public synchronized BusMaster.ScheduleNotifySearchBusCmdResult scheduleSearch(String bmIdent, boolean byAlarm, long minPeriodMSec) {
        BusMaster.ScheduleNotifySearchBusCmdResult result = bmUpdateData.getBusMasterDataByIdent(bmIdent).scheduleSearch(new myNotifySearchBusCmdResult(), byAlarm, minPeriodMSec);
        if (result == BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success) {
            if (!waitingThreadData.hasWaitingThread()) {
                waitingThreadData.scheduleDeadManTimerTask(new DeadManTimerTask());
            }
        }
        return result;
    }

    /**
     * Update a scheduled notify search bus using the specified interval for the given bus
     *
     * @param bmIdent
     * @param byAlarm true if this for Alarm searches, false if general search
     * @param minPeriodMSec
     * @return UpdateScheduledNotifySearchBusCmdResult
     */
    public synchronized BusMaster.UpdateScheduledNotifySearchBusCmdResult updateScheduledSearch(String bmIdent, boolean byAlarm, long minPeriodMSec) {
        return bmUpdateData.getBusMasterDataByIdent(bmIdent).updateScheduledSearch(byAlarm, minPeriodMSec);
    }

    /**
     * Cancel an alarm search for the given bus.
     *
     * @param bmIdent
     * @param byAlarm true if this for Alarm searches, false if general search
     * @return true if a search existed and was cancelled, false otherwise
     */
    public synchronized BusMaster.CancelScheduledNotifySearchBusCmdResult cancelScheduledSearch(String bmIdent, boolean byAlarm) {
        return bmUpdateData.getBusMasterDataByIdent(bmIdent).cancelScheduledSearch(byAlarm);
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

                ArrayList<WaitForEventResult.BMSearchData> bmSearchDataList = bmUpdateData.getSearchDataRelativeTo(false, cmd.getBMSearchNotifyTimestampMSec());

                ArrayList<WaitForEventResult.BMSearchData> bmAlarmSearchDataList = bmUpdateData.getSearchDataRelativeTo(true, cmd.getBMAlarmSearchNotifyTimestampMSec());

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
        String bmIdent = encoder.encodeToString(bm.getName().getBytes());

        if (bmUpdateData.addBusMaster(bm, bmIdent)) {
            waitingThreadData.notifyWaitingThreadIfPresent();
        }
    }

    private class myNotifySearchBusCmdResult implements NotifySearchBusCmdResult {

        @Override
        public void notify(BusMaster bm, boolean byAlarm, SearchBusCmd.ResultData searchResultData) {
            Model.this.searchBusNotify(this, bm, byAlarm, searchResultData);
        }
    }

    private synchronized void searchBusNotify(myNotifySearchBusCmdResult notifyResult, BusMaster bm, boolean byAlarm,
                                              SearchBusCmd.ResultData searchResultData) {
        BusMasterData bmData = bmUpdateData.findBusMasterDataFor(bm);
        // be cautious
        if ((bmData != null) && (bmData.updateSearchData(notifyResult, byAlarm, searchResultData))) {
            waitingThreadData.notifyWaitingThreadIfPresent();
        }
    }

}
