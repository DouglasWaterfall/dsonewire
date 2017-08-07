package waterfall.onewire.busmaster;

/**
 * Created by dwaterfa on 11/20/16.
 */
public interface NotifySearchBusCmdResult {

    /**
     * Notify of a successful SearchBusCmd. Not supported for family search.
     *
     * @param bm
     * @param byAlarm true if by the SearchBusCmd was by Alarm, false if it was a general search.
     * @param searchResultData
     */
    public void notify(final BusMaster bm, final boolean byAlarm, final SearchBusCmd.ResultData searchResultData);

}
