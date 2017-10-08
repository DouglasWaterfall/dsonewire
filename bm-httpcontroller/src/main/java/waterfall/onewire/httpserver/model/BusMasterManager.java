package waterfall.onewire.httpserver.model;

import org.springframework.beans.factory.annotation.Autowired;
import waterfall.onewire.BusMasterRegistry;
import waterfall.onewire.HttpClient.WaitForEventCmdData;
import waterfall.onewire.HttpClient.WaitForEventCmdResult;
import waterfall.onewire.busmaster.BusMaster;
import waterfall.onewire.busmaster.NotifySearchBusCmdResult;
import waterfall.onewire.busmaster.SearchBusCmd;
import waterfall.onewire.httpserver.BusMasterData;
import waterfall.onewire.httpserver.BusMasterTracker;

import java.util.*;

/**
 * Created by dwaterfa on 1/12/17.
 */

public class BusMasterManager {

    private final BusMasterRegistry bmRegistry;
    private final Base64.Encoder encoder;

    private final BusMasterTracker bmUpdateData;

    @Autowired
    public BusMasterManager(BusMasterRegistry bmRegistry) {
        this.bmRegistry = bmRegistry;
        this.bmUpdateData = new BusMasterTracker();
        this.encoder = Base64.getUrlEncoder();

        // we want to find out about BusMasters added to the BusMasterRegistry.
        bmRegistry.addObserver(new myBMSearchResult());
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
     * This method is called from the BusMasterRegistry when the BusMaster list changes.
     */
    private class myBMSearchResult implements Observer {
        @Override
        public void update(Observable o, Object arg) {
            if ((o instanceof BusMasterRegistry) && (arg instanceof BusMaster)) {
                RegistryObserver.this.bmRegistryUpdate((BusMasterRegistry) o, (BusMaster) arg);
            }
        }
    }

    private synchronized void bmRegistryUpdate(BusMasterRegistry bmRegistry, BusMaster bm) {
        String bmIdent = encoder.encodeToString(bm.getName().getBytes());

        if (bmUpdateData.addBusMaster(bm, bmIdent)) {
            waitingThreadData.notifyWaitingThreadIfPresent();
        }
    }

    /**
     * This method is called from the BusMasterRegistry when the BusMaster list changes.
     */
    private class myNotifySearchBusCmdResult implements NotifySearchBusCmdResult {

        @Override
        public void notify(BusMaster bm, boolean byAlarm, SearchBusCmd.ResultData searchResultData) {
            RegistryObserver.this.searchBusNotify(this, bm, byAlarm, searchResultData);
        }
    }

    private synchronized void searchBusNotify(myNotifySearchBusCmdResult notifyResult, BusMaster bm, boolean byAlarm,
                                              SearchBusCmd.ResultData searchResultData) {
        BusMasterData bmData = bmUpdateData.findBusMasterDataFor(bm);
        // be cautious
        if ((bmData != null) && (bmData.updateSearchData(notifyResult, byAlarm ? BusMasterData.SearchType.ByAlarm : BusMasterData.SearchType.General, searchResultData))) {
            waitingThreadData.notifyWaitingThreadIfPresent();
        }
    }

}
