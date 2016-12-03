package waterfall.onewire.busmaster;

import java.util.List;

/**
 * Created by dwaterfa on 11/20/16.
 */
public interface SearchBusCmdNotifyResult {

    /**
     * Notify of a successful SearchBusCmd.
     *
     * @param bm
     * @param searchResultData
     */
    public void notify(final BusMaster bm, final SearchBusCmd.ResultData searchResultData);

}
