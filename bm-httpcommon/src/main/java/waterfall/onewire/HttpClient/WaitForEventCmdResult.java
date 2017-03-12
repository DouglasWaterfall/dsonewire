package waterfall.onewire.HttpClient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import waterfall.onewire.busmaster.Logger;

import java.util.List;

/**
 * Created by dwaterfa on 8/23/16.
 */
public class WaitForEventCmdResult extends BaseCmdPostResult {

    /**
     * The Errors possible from the controller for this call
     */
    public enum ControllerErrors {
        // a waiting thread already exists
        InternalError_ThreadAlreadyWaiting,

        /**
         * This will only be returned if the client and controller have already agreed on the bmChanged notification
         * timestamp but the search(s) have separately specified a bmIdent which is not known to the controller.
         */
        SearchBMIdentUnknown,
        AlarmSearchBMIdentUnknown,

        /**
         * When active BM searches are outstanding on behalf of the client it is important that the server stays in sync
         * with the client.
         *
         * A flag will be internally set for all new BusMasters which are added to the controller, and when the dead
         * man timer trigger the cancellation of outstanding search requests.
         *
         * This will only be returned if the calling thread specifies search timestamps for one or more bmIdents
         * representing that it expects searches to be pending.
         *
         * The internal flag will be cleared when any search for the bmIdent scheduled.
         */
        SearchesCancelled,

        /**
         * The wait completed but has nothing to report.
         */
        WaitTimeout
    }

    @JsonProperty("controllerError")
    public ControllerErrors controllerError;

    /**
     * This class is valid when the serverTimestampMSec value passed into the waitForEvent() call does not match what the
     * server has or the BusMaster list held by the server has been changed since the last timestamp passed into the
     * waitForEvent() call. The caller should be careful to check the currentServerTimestampMsec against what it thought
     * in order to detect when the server has been reset.
     */
    public static class BMListChangedData {
        public long currentServerTimestampMSec;
        public long bmUpdateTimestampMSec;
        public String[] bmIdents;

        public BMListChangedData(long currentServerTimestampMSec, long bmUpdateTimestampMSec, String[] bmIdents) {
            this.currentServerTimestampMSec = currentServerTimestampMSec;
            this.bmUpdateTimestampMSec = bmUpdateTimestampMSec;
            this.bmIdents = bmIdents;
        }
    }

    @JsonProperty("bmListChangedData")
    private BMListChangedData bmListChangedData;

    /**
     * This class holds the notification information for an active search (whole buss or byAlarm only). This will be
     * returned if
     */
    public static class BMSearchData {
        public String bmIdent;
        public long notifyTimestampMSec;
        public List<String> dsList;
        public long dsListCRC32;

        public BMSearchData(String bmIdent, long notifyTimestampMSec, List<String> dsList, long dsListCRC32) {
            this.bmIdent = bmIdent;
            this.notifyTimestampMSec = notifyTimestampMSec;
            this.dsList = dsList;
            this.dsListCRC32 = dsListCRC32;
        }
    }

    private BMSearchData[] bmSearchData;

    private BMSearchData[] bmSearchByAlarmData;

    public WaitForEventCmdResult() {
        super();
        controllerError = null;
        this.bmListChangedData = null;
        this.bmSearchData = null;
        this.bmSearchByAlarmData = null;
    }

    public WaitForEventCmdResult(PostErrors pe) {
        super(pe);
        controllerError = null;
        this.bmListChangedData = null;
        this.bmSearchData = null;
        this.bmSearchByAlarmData = null;
    }

    public WaitForEventCmdResult(ControllerErrors ce) {
        super();
        controllerError = ce;
        this.bmListChangedData = null;
        this.bmSearchData = null;
        this.bmSearchByAlarmData = null;
    }

    public WaitForEventCmdResult(BMListChangedData data) {
        super();
        controllerError = null;
        this.bmListChangedData = data;
        this.bmSearchData = null;
        this.bmSearchByAlarmData = null;
    }

    public WaitForEventCmdResult(BMSearchData[] data, BMSearchData[] alarmData) {
        super();
        controllerError = null;
        this.bmListChangedData = null;
        this.bmSearchData = data;
        this.bmSearchByAlarmData = alarmData;
    }

    /**
     * @return true if there was an error from the controller on the server.
     */
    public boolean hasControllerError() {
        return (controllerError != null);
    }

    /**
     * @return the ControllerError, or null if there was no error from the controller on the server.
     */
    public ControllerErrors getControllerError() {
        return controllerError;
    }

    /**
     *
     * @param controllerError
     */
    public void setControllerError(ControllerErrors controllerError) {
        this.controllerError = controllerError;
    }

    /**
     * A successful waitForEvent() call can return one or more of the below events in any combination. However, if
     * the currentServerTimestampMsec value was changed then that will be the only event data available and it will imply
     * that any outstanding searches on behalf of the client have been cancelled.
     *
     * @return true if there is a mismatched currentServerTimestampMSec or the BM list has changed since the last
     * timestamp provided in the waitForEvent()
     */
    public boolean hasBMListChanged() {
        return (bmListChangedData != null);
    }

    /**
     * @return the event data, or null if there has been no change to the BMList for this event
     */
    public BMListChangedData getBMListChangedData(){
        return bmListChangedData;
    }

    /**
     *
     * @param data
     */
    public void setBMListChangedData(BMListChangedData data){
        this.bmListChangedData = bmListChangedData;
    }

    /**
     * @return true if one or more BMs had a (general bus) search notification relative to notifyTimestampMSec value for the BM(s).
     */
    public boolean hasBMSearchData() {
        return (bmSearchData != null);
    }

    /**
     * @return BMSearchData if there is search notify data, otherwise null
     */
    public BMSearchData[] getBMSearchData() {
        return bmSearchData;
    }

    /**
     *
     * @param data
     */
    public void setBMSearchData(BMSearchData[] data) {
        bmSearchData = data;
    }

    /**
     * @return true if one or more BMs had a search by alarm notification relative to notifyTimestampMSec value for the BM(s).
     */
    public boolean hasBMAlarmSearchData(){
        return (bmSearchByAlarmData != null);
    }

    /**
     * @return BMSearchData if there is search by alarm notify data, otherwise null
     */
    public BMSearchData[] getBMAlarmSearchData() {
        return bmSearchByAlarmData;
    }

    /**
     *
     * @param data
     */
    public void setBMSearchByAlarmData(BMSearchData[] data) {
        bmSearchByAlarmData = data;
    }

}


