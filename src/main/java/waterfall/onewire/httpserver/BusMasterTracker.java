package waterfall.onewire.httpserver;

import waterfall.onewire.HttpClient.WaitForEventResult;
import waterfall.onewire.busmaster.BusMaster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dwaterfa on 1/16/17.
 */
public class BusMasterTracker {
    private long timestampMSec;
    private HashMap<String, BusMasterData> bmIdentMap;

    public BusMasterTracker() {
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

    public BusMasterData getBusMasterDataByIdent(String bmIdent) {
        BusMasterData bmData = bmIdentMap.get(bmIdent);
        if (bmData != null) {
            return bmData;
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

    public boolean addBusMaster(BusMaster bm, String bmIdent) {

        if (!bmIdentMap.containsKey(bmIdent)) {
            System.out.println("Adding busMaster" + bm.getName());
            bmIdentMap.put(bmIdent, new BusMasterData(bm, bmIdent));
            timestampMSec = System.currentTimeMillis();
            return true;
        } else if (bmIdentMap.get(bmIdent) == bm) {
            System.err.println("Duplicate add of busMaster" + bm.getName());
        } else {
            System.err.println("name encoding collision of busMaster:" + bm.getName() + " and " + bmIdentMap.get(bmIdent).bm.getName());
        }
        return false;
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

    public ArrayList<WaitForEventResult.BMSearchData> getSearchDataRelativeTo(boolean byAlarm, Map<String, Long> searchNotifyTimestampMSec) {
        ArrayList<WaitForEventResult.BMSearchData> list = null;

        for (BusMasterData bmData : bmIdentMap.values()) {
            // The event thready may not know about searches which have been scheduled after it went to sleep
            // so finding an empty reference in the command data is equivalent to never having seen the current
            // search result.

            WaitForEventResult.BMSearchData bmSearchData = null;

            if ((bmSearchData = bmData.getSearchDataRelativeTo(byAlarm, searchNotifyTimestampMSec.get(bmData.bmIdent))) != null) {
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
            if (bmData.hasActiveSearches()) {
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
            if (bmData.hasActiveSearches()) {
                bmData.cancelSearch(false);
                bmData.cancelSearch(true);
                bmData.searchCancelled = true;
            }
        }
    }

}
