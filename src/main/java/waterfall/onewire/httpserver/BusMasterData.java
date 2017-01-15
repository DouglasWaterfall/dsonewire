package waterfall.onewire.httpserver;

import waterfall.onewire.HttpClient.WaitForEventResult;
import waterfall.onewire.busmaster.AlarmSearchBusCmdNotifyResult;
import waterfall.onewire.busmaster.BusMaster;
import waterfall.onewire.busmaster.SearchBusCmd;
import waterfall.onewire.busmaster.SearchBusCmdNotifyResult;

import java.util.List;

/**
 * Created by dwaterfa on 1/14/17.
 * This data represents information we know about the various BusMasters from the BusMasterRegistry and how we
 * manage them on behalf of the client. This data is only populated on behalf of the client registering for bus
 * master events.
 */
public class BusMasterData {

    public final BusMaster bm;
    public final String bmIdent;

    public boolean searchCancelled;

    public class SearchResultData {
        public long notifyTimestampMSec;
        public List<String> notifyDSList;
        public long notifyDSListCRC32;

        public SearchResultData(long timestampMSec, List<String> dsList, long dsListCRC32) {
            notifyTimestampMSec = timestampMSec;
            notifyDSList = dsList;
            notifyDSListCRC32 = dsListCRC32;
        }

        public WaitForEventResult.BMSearchData getBMSearchDataFor(Long lastSearchNotifyTimestampMSec) {
            if ((lastSearchNotifyTimestampMSec == null) ||
                (lastSearchNotifyTimestampMSec < notifyTimestampMSec)) {
                return new WaitForEventResult.BMSearchData(BusMasterData.this.bmIdent, notifyTimestampMSec, notifyDSList, notifyDSListCRC32);
            }

            return null;
        }
    }

    public SearchBusCmdNotifyResult searchNotifyObj;
    public SearchResultData searchNotifyResultData;

    public AlarmSearchBusCmdNotifyResult alarmSearchNotifyObj;
    public SearchResultData alarmSearchNotifyResultData;

    public BusMasterData(BusMaster bm, String bmIdent) {
        this.bm = bm;
        this.bmIdent = bmIdent;

        this.searchCancelled = true;

        this.searchNotifyObj = null;
        this.searchNotifyResultData = null;

        this.alarmSearchNotifyObj = null;
        this.alarmSearchNotifyResultData = null;
    }

    // returns true if an update was done
    public boolean updateSearchData(SearchBusCmdNotifyResult notifyResult, SearchBusCmd.ResultData searchResultData) {
        // be cautious
        if (searchNotifyObj == notifyResult) {
            searchNotifyResultData = new SearchResultData(System.currentTimeMillis(), searchResultData.getList(), searchResultData.getListCRC32());
            return true;
        }

        return false;
    }

    public WaitForEventResult.BMSearchData getSearchDataRelativeTo(Long lastSearchNotifyTimestampMSec) {
        if (searchNotifyResultData != null) {
            return searchNotifyResultData.getBMSearchDataFor(lastSearchNotifyTimestampMSec);
        }

        return null;
    }

    // returns true if an update was done
    public boolean updateAlarmSearchData(AlarmSearchBusCmdNotifyResult notifyResult, SearchBusCmd.ResultData searchResultData) {
        // be cautious
        if (alarmSearchNotifyObj == notifyResult) {
            alarmSearchNotifyResultData = new SearchResultData(System.currentTimeMillis(), searchResultData.getList(), searchResultData.getListCRC32());
            return true;
        }

        return false;
    }

    public WaitForEventResult.BMSearchData getAlarmSearchDataRelativeTo(Long lastAlarmSearchNotifyTimestampMSec) {
        if (alarmSearchNotifyResultData != null) {
            return alarmSearchNotifyResultData.getBMSearchDataFor(lastAlarmSearchNotifyTimestampMSec);
        }

        return null;
    }

}
