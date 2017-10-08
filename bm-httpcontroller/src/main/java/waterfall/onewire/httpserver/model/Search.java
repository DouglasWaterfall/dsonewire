package waterfall.onewire.httpserver.model;

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
public class Search implements NotifySearchBusCmdResult {

    private final BusMaster bm;
    private final SearchBusCmd.ResultData[] resultData;
    private final boolean searchActive[];

    public Search(BusMaster bm) {
        this.bm = bm;
        resultData = new SearchBusCmd.ResultData[]{null, null};
        searchActive = new boolean[]{false, false};
    }

    public synchronized boolean updateSearch(boolean byAlarm, long minPeriodMSec) {

        if (!searchActive[byAlarm ? 1 : 0]) {
            BusMaster.ScheduleNotifySearchBusCmdResult result = bm.scheduleNotifySearchBusCmd(this, byAlarm, minPeriodMSec);
            if (result == BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success) {
                searchActive[byAlarm ? 1 : 0] = true;
                return true;
            } else {
                System.err.println("schedule failure:" + result.name());
                return false;
            }
        } else {
            BusMaster.UpdateScheduledNotifySearchBusCmdResult result = bm.updateScheduledNotifySearchBusCmd(this, byAlarm, minPeriodMSec);
            if (result == BusMaster.UpdateScheduledNotifySearchBusCmdResult.USNSBC_Success) {
                return true;
            } else {
                System.err.println("update failure:" + result.name());
                return false;
            }

        }
    }

    public synchronized void cancelSearches() {
        for (int i = 0; i < searchActive.length; i++) {
            if (searchActive[i]) {
                BusMaster.CancelScheduledNotifySearchBusCmdResult result = bm.cancelScheduledNotifySearchBusCmd(this, (i == 1));
                if (result != BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_Success) {
                    System.err.println("cancel failure:" + result.name());
                }

                searchActive[i] = false;
            }
        }
    }

    /**
     * This method is for the NotifySearchCmdBusResult when a BusMaster has a search result
     *
     * @param bm
     * @param byAlarm true if by the SearchBusCmd was by Alarm, false if it was a general search.
     * @param searchResultData
     */
    public synchronized void notify(final BusMaster bm, final boolean byAlarm, final SearchBusCmd.ResultData searchResultData) {
        if (searchActive[byAlarm ? 1 : 0]) {
            resultData[byAlarm ? 1 : 0] = searchResultData;
        }
        else {
            System.err.println("notify when not active");
        }
    }


}

    /*

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
*/
