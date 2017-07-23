package waterfall.onewire;

import java.util.*;

import waterfall.onewire.busmaster.BusMaster;
import waterfall.onewire.busmaster.SearchBusCmd;
import waterfall.onewire.busmaster.NotifySearchBusCmdResult;

/**
 * This class is simply a registry of BusMasters which have been started.
 */
public class BusMasterRegistry extends Observable {

    private final HashMap<String, BusMaster> bmMap;

    public BusMasterRegistry() {
        bmMap = new HashMap<>();
    }

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

        if ((bm.getName() == null) || (bm.getName().isEmpty())) {
            throw new IllegalArgumentException("bm does not have a name");
        }

        if (!bm.getIsStarted()) {
            throw new IllegalArgumentException("bm not started");
        }

        synchronized (this) {
            if (!bmMap.containsKey(bm.getName())) {
                bmMap.put(bm.getName(), bm);
                setChanged();
                notifyObservers(new BusMasterAdded(bm));
            } else {
                throw new IllegalArgumentException("bm already known to the registry");
            }
        }
    }

    /**
     * Remove a BusMaster from the registry. Must be known.
     *
     * @param bm
     */
    public void removeBusMaster(BusMaster bm) {
        if (bm == null) {
            throw new NullPointerException();
        }

        synchronized (this) {
            if (bmMap.containsKey(bm.getName())) {
                bmMap.remove(bm.getName());
                setChanged();
                notifyObservers(new BusMasterRemoved(bm));
            } else {
                throw new IllegalArgumentException("bm not known to the registry");
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
            o.update(this, new BusMasterAdded(bm));
        }
    }

    /**
     * Wait for the specified Address to be found on a BusMaster and return it. We will wait forever for this to occur
     * and BusMasters can come and go and we will be patient.
     *
     * @param address
     * @param bmSearchPeriodMSec
     * @return
     */
    public BusMaster waitForDeviceByAddress(String address, long bmSearchPeriodMSec) {
        WaitForDeviceByAddress wfdbaObj = new WaitForDeviceByAddress(address, bmSearchPeriodMSec);
        BusMaster bm = null;
        try {
            addObserver(wfdbaObj);
            bm = wfdbaObj.waitForBM();
        } finally {
            // Start with registering ourselves to track the lifetime of the BusMasters. When these are found we will
            // be called back on the update() method.
            deleteObserver(wfdbaObj);
        }

        return bm;
    }

    /**
     * Implements the waiting for Address scheme. There will be one instance of this for a specified Address.
     * It will be registered here as an Observer, which will maintain its lifetime, and will get notified of new
     * bus masters AND removed busmasters so it can scheduled and stop scheduling searches as required.
     */
    private class WaitForDeviceByAddress implements Observer, NotifySearchBusCmdResult {
        private final String address;
        private final long bmSearchPeriodMSec;
        private final ArrayList<BusMaster> bmScheduledList;
        private BusMaster foundBM;
        private Thread waitingThread;

        public WaitForDeviceByAddress(String address, long bmSearchPeriodMSec) {
            this.address = address;
            this.bmSearchPeriodMSec = bmSearchPeriodMSec;
            this.bmScheduledList = new ArrayList<>();
            this.foundBM = null;
            this.waitingThread = null;
        }

        // We are called from the BusMasterRegistry with a BusMaster and we will schedule a search on it.
        @Override // Observer
        public void update(Observable o, Object arg) {
            boolean added = false;
            boolean removed = false;
            BusMaster bm = null;

            synchronized (this) {
                if ((o instanceof BusMasterRegistry) && (arg != null)) {
                    if ((arg instanceof BusMasterAdded) && (foundBM == null)) {
                        bm = ((BusMasterAdded) arg).getBusMaster();
                        if (!bmScheduledList.contains(bm)) {
                            bmScheduledList.add(bm);
                            added = true;
                        }
                    } else if (arg instanceof BusMasterRemoved) {
                        bm = ((BusMasterRemoved) arg).getBusMaster();
                        if (bmScheduledList.contains(bm)) {
                            bmScheduledList.remove(bm);
                            removed = true;
                        }
                    }
                }
            }

            if (added) {
                BusMaster.ScheduleNotifySearchBusCmdResult result = null;
                try {
                    result = bm.scheduleNotifySearchBusCmd(this, false, bmSearchPeriodMSec);
                } catch (Exception e) {
                    System.err.println(bm.getName() + ": WaitForDeviceAddress(" + address + ") scheduleNotifySearchBusCmd:" + e);
                }
                if (result != BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success) {
                    throw new IllegalArgumentException(bm.getName() + ": scheduleNotifySearchBusCmd returned " + result.name());
                }
            } else if (removed) {
                BusMaster.CancelScheduledNotifySearchBusCmdResult result = null;
                try {
                    result = bm.cancelScheduledNotifySearchBusCmd(this, false);
                } catch (Exception e) {
                    System.err.println(bm.getName() + ": WaitForDeviceAddress(" + address + ") cancelScheduledNotifySearchBusCmd:" + e);
                }
                if (result != BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_Success) {
                    throw new IllegalArgumentException(bm.getName() + ": cancelScheduledNotifySearchBusCmd returned " + result.name());
                }
            }
        }

        // Called from NotifySearchBusCmdResult with the results of any searches. If we find the Address we want then
        // we know which BusMaster controls it and we can cancel all of the scheduled searches.
        @Override // NotifySearchBusCmdResult
        public synchronized void notify(BusMaster bm, boolean byAlarm, SearchBusCmd.ResultData searchResultData) {
            if (this.foundBM == null) {
                for (String addr : searchResultData.getList()) {
                    if (addr.equals(address)) {
                        this.foundBM = bm;
                        for (BusMaster t_bm : bmScheduledList) {
                            t_bm.cancelScheduledNotifySearchBusCmd(this, byAlarm);
                        }
                        bmScheduledList.clear();
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
                } catch (InterruptedException e) {
                    ;
                }
            }
            waitingThread = null;

            // We are not responsible for tearing everything down.
            return this.foundBM;
        }

    }

    // When an instance of this Object is passed to Observers the started BusMaster has already been added to the registry.
    public class BusMasterAdded {
        private BusMaster bm;

        public BusMasterAdded(BusMaster bm) {
            this.bm = bm;
        }

        public BusMaster getBusMaster() {
            return bm;
        }
    }

    // When an instance of this Object is passed to Observers the BusMaster has already been removed from the registry.
    // We have no control over whether the BusMaster is stopped or not, nor do we have any ability to prevent the
    // BusMaster from being still used, we just forget about it.
    public class BusMasterRemoved {
        private BusMaster bm;

        public BusMasterRemoved(BusMaster bm) {
            this.bm = bm;
        }

        public BusMaster getBusMaster() {
            return bm;
        }
    }

}

