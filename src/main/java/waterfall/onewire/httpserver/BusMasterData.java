package waterfall.onewire.httpserver;

import waterfall.onewire.HttpClient.WaitForEventCmdResult;
import waterfall.onewire.busmaster.BusMaster;
import waterfall.onewire.busmaster.NotifySearchBusCmdResult;
import waterfall.onewire.busmaster.SearchBusCmd;

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

    public enum SearchType {
        General,
        ByAlarm
    }

    public class SearchData {
        NotifySearchBusCmdResult notifyObj;

        public class ResultData {
            public long notifyTimestampMSec;
            public List<String> notifyDSList;
            public long notifyDSListCRC32;

            public ResultData(long timestampMSec, List<String> dsList, long dsListCRC32) {
                notifyTimestampMSec = timestampMSec;
                notifyDSList = dsList;
                notifyDSListCRC32 = dsListCRC32;
            }

            public WaitForEventCmdResult.BMSearchData getBMSearchDataFor(Long lastSearchNotifyTimestampMSec) {
                if ((lastSearchNotifyTimestampMSec == null) ||
                        (lastSearchNotifyTimestampMSec < notifyTimestampMSec)) {
                    return new WaitForEventCmdResult.BMSearchData(BusMasterData.this.bmIdent, notifyTimestampMSec, notifyDSList, notifyDSListCRC32);
                }

                return null;
            }
        }

        ResultData resultData;

        public SearchData() {
            notifyObj = null;
            resultData = null;
        }

        public boolean updateResultData(Object notifyResult, SearchBusCmd.ResultData resultData) {
            // be cautious
            if (notifyObj == notifyResult) {
                this.resultData = new ResultData(System.currentTimeMillis(), resultData.getList(), resultData.getListCRC32());
                return true;
            }

            return false;
        }

        public WaitForEventCmdResult.BMSearchData getResultDataRelativeTo(Long lastSearchNotifyTimestampMSec) {
            if (resultData != null) {
                return resultData.getBMSearchDataFor(lastSearchNotifyTimestampMSec);
            }

            return null;
        }
    }

    SearchData[] searchData;

    public BusMasterData(BusMaster bm, String bmIdent) {
        this.bm = bm;
        this.bmIdent = bmIdent;

        this.searchCancelled = true;

        this.searchData = new SearchData[2];
        this.searchData[0] = new SearchData();
        this.searchData[1] = new SearchData();
    }

    public BusMaster.ScheduleNotifySearchBusCmdResult scheduleSearch(NotifySearchBusCmdResult notifyResult, SearchType sType, long minPeriodMSec) {
        SearchData sData = searchData[(sType == SearchType.ByAlarm) ? 1 : 0];

        if (sData.notifyObj != null) {
            return BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_NotifyObjAlreadyScheduled;
        }

        BusMaster.ScheduleNotifySearchBusCmdResult result = bm.scheduleNotifySearchBusCmd(notifyResult, (sType == SearchType.ByAlarm), minPeriodMSec);
        if (result == BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success) {
            searchCancelled = false;
            sData.notifyObj = notifyResult;
            sData.resultData = null;
        }

        return result;
    }

    public BusMaster.UpdateScheduledNotifySearchBusCmdResult updateScheduledSearch(SearchType sType, long minPeriodMSec) {
        SearchData sData = searchData[(sType == SearchType.ByAlarm) ? 1 : 0];
        return bm.updateScheduledNotifySearchBusCmd(sData.notifyObj, (sType == SearchType.ByAlarm), minPeriodMSec);
    }

    public boolean updateSearchData(NotifySearchBusCmdResult notifyResult, SearchType sType, SearchBusCmd.ResultData searchResultData) {
        SearchData sData = searchData[(sType == SearchType.ByAlarm) ? 1 : 0];

        return sData.updateResultData(notifyResult, searchResultData);
    }

    public WaitForEventCmdResult.BMSearchData getSearchDataRelativeTo(SearchType sType, Long lastSearchNotifyTimestampMSec) {
        SearchData sData = searchData[(sType == SearchType.ByAlarm) ? 1 : 0];

        return sData.getResultDataRelativeTo(lastSearchNotifyTimestampMSec);
    }

    public BusMaster.CancelScheduledNotifySearchBusCmdResult cancelScheduledSearch(SearchType sType) {
        SearchData sData = searchData[(sType == SearchType.ByAlarm) ? 1 : 0];

        BusMaster.CancelScheduledNotifySearchBusCmdResult result = bm.cancelScheduledNotifySearchBusCmd((NotifySearchBusCmdResult)sData.notifyObj, (sType == SearchType.ByAlarm));
        if (sData.notifyObj != null) {
            sData.notifyObj = null;
            sData.resultData = null;
        }

        return result;
    }

    public boolean hasActiveSearches() {
        return ((!searchCancelled) && ((searchData[0].notifyObj != null) || (searchData[1].notifyObj != null)));
    }

}
