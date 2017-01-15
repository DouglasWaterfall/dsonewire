package waterfall.onewire;

import java.util.*;

import waterfall.onewire.busmaster.BusMaster;
import waterfall.onewire.busmaster.SearchBusCmd;
import waterfall.onewire.busmaster.SearchBusCmdNotifyResult;

/**
 * This class is simply a registry of BusMasters which have been started.
 */
public class BusMasterRegistry extends Observable {

    private HashMap<String, BusMaster> bmMap = new HashMap<String, BusMaster>();

    public BusMasterRegistry() { }

    /**
     * Find the list of BusMasters currently known to the Registry
     *
     * @return
     */
    public synchronized BusMaster[] getBusMasters() {
        return bmMap.values().toArray(new BusMaster[bmMap.size()]);
    }

    /**
     * Returns true if we know about the BusMaster by its name.
     */
    public synchronized boolean hasBusMasterByName(String bmName) {
        return (bmMap.containsKey(bmName));
    }

    /**
     * Add a BusMaster to the registry. Must must be started, may be a duplicate.
     *
     * @param bm
     */
    public void addBusMaster(BusMaster bm) {
        if (bm == null) {
            throw new NullPointerException();
        }

        if (!bm.getIsStarted()) {
            throw new IllegalArgumentException("bm not started");
        }

        synchronized (this) {
            if (!bmMap.containsKey(bm.getName())) {
                bmMap.put(bm.getName(), bm);
                setChanged();
                notifyObservers(bm);
            }
        }
    }

    /**
     * Add an Observer to the Registry. We will update it with all the known BusMasters.
     *
     * @param o
     */
    @Override
    public void addObserver(Observer o) {
        super.addObserver(o);

        BusMaster[] bms = getBusMasters();
        for (BusMaster bm : bms) {
            o.update(this, bm);
        }
    }

    /**
     * Wait for the specified Address to be found on a BusMaster and return it.
     * @param address
     * @param bmSearchPeriodMSec
     * @return
     */
    public BusMaster waitForDeviceByAddress(String address, long bmSearchPeriodMSec) {
        return new WaitForDeviceByAddress(address, bmSearchPeriodMSec).waitForBM();
    }

    /**
     * Implements the waiting for Address scheme.
     */
    private class WaitForDeviceByAddress implements Observer,SearchBusCmdNotifyResult {
        private final String address;
        private final long bmSearchPeriodMSec;
        private final ArrayList<BusMaster> bmList;
        private BusMaster foundBM;
        private Thread waitingThread;

        public WaitForDeviceByAddress(String address, long bmSearchPeriodMSec) {
            this.address = address;
            this.bmSearchPeriodMSec = bmSearchPeriodMSec;
            this.bmList = new ArrayList<>();
            this.foundBM = null;
            this.waitingThread = null;

            // Start with registering ourselves to track BusMasters. When these are found we will be called back on
            // the update() method.
            BusMasterRegistry.this.addObserver(this);
        }

        // We are called from the Registry with a BusMaster and we will schedule a search on it.
        @Override // Observer
        public void update(Observable o, Object arg) {
            boolean added = false;

            synchronized (this) {
                if ((o instanceof BusMasterRegistry) &&
                        (arg != null) &&
                        (arg instanceof BusMaster) &&
                        (foundBM == null) &&
                        (!bmList.contains((BusMaster) arg))) {
                    bmList.add((BusMaster) arg);
                    added = true;
                }
            }

            if (added) {
                try {
                    ((BusMaster) arg).scheduleSearchNotifyFor(this, bmSearchPeriodMSec);
                }
                catch (Exception e) {
                    System.err.println("WaitForDeviceAddress(" + address + ") scheduleSearchNotifyFor:" + e);
                }
            }
        }

        // Called from SearchBusCmdNotifyResult with the results of any searches. If we find the Address we want then
        // we know which BusMaster and we can deregister everything.
        @Override // SearchBusCmdNotifyResult
        public synchronized void notify(BusMaster bm, SearchBusCmd.ResultData searchResultData) {
            if (this.foundBM == null) {
                for (String addr: searchResultData.getList()) {
                    if (addr.equals(address)) {
                        this.foundBM = bm;
                        for (BusMaster t_bm: bmList) {
                            t_bm.cancelSearchNotifyFor(this);
                        }
                        bmList.clear();
                        BusMasterRegistry.this.deleteObserver(this);
                        if (waitingThread != null) {
                            this.notify();
                        }
                        break;
                    }
                }
            }
        }

        public synchronized BusMaster waitForBM() {
            while (this.foundBM == null) {
                waitingThread = Thread.currentThread();
                try {
                    wait();
                }
                catch (InterruptedException e) {
                    ;
                }
            }
            waitingThread = null;

            // We are not responsible for tearing everything down.
            return this.foundBM;
        }

    }
}

