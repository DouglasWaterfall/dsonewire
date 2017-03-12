package waterfall.onewire.busmaster;

import java.util.List;

public interface AlarmSearchBusCmdNotifyResult {

    /**
     * Notify of a successful SearchBusCmd.
     *
     * @param bm
     * @param searchResultData
     */
    public void notify(final BusMaster bm, final SearchBusCmd.ResultData searchResultData);


}

