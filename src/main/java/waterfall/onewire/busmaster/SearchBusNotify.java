package waterfall.onewire.busmaster;

import java.util.List;

/**
 * Created by dwaterfa on 11/20/16.
 */
public interface SearchBusNotify {

    /**
     * Notify of a successful SeachBusCmd.
     * @param bm The BusMaster the successful search had be exectuted on.
     * @param searchResultList The list of String addresses found
     */
    public void notify(BusMaster bm, List<String> searchResultList);

}
