/**
 * Created by dwaterfa on 7/23/17.
 */
package waterfall.onewire;

import waterfall.onewire.busmaster.BusMaster;
import waterfall.onewire.busmaster.NotifySearchBusCmdResult;
import waterfall.onewire.busmaster.SearchBusCmd;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

/**
 * Implements the waiting for Address scheme. There will be one instance of this for a specified Address.
 * It will be registered here as an Observer, which will maintain its lifetime, and will get notified of new
 * bus masters AND removed busmasters so it can scheduled and stop scheduling searches as required.
 */
public class WaitForDeviceByAddress implements Observer, NotifySearchBusCmdResult {
    private final BusMasterRegistry bmRegistry;
    private final long bmSearchPeriodMSec;
    private final ArrayList<BusMaster> bmScheduledList;
    private String address;
    private BusMaster foundBM;
    private Thread waitingThread;

    public WaitForDeviceByAddress(BusMasterRegistry bmRegistry, long bmSearchPeriodMSec) {
        if (bmRegistry == null) {
            throw new IllegalArgumentException("bmRegistry null");
        }
        if (bmSearchPeriodMSec < 1) {
            throw new IllegalArgumentException("bmSearchPeriod less than 1");
        }
        this.bmRegistry = bmRegistry;
        this.bmSearchPeriodMSec = bmSearchPeriodMSec;
        this.bmScheduledList = new ArrayList<>();
        this.address = null;
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
                if ((arg instanceof BusMasterRegistry.BusMasterAdded) && (foundBM == null)) {
                    bm = ((BusMasterRegistry.BusMasterAdded) arg).getBusMaster();
                    if (!bmScheduledList.contains(bm)) {
                        bmScheduledList.add(bm);
                        added = true;
                    }
                } else if (arg instanceof BusMasterRegistry.BusMasterRemoved) {
                    bm = ((BusMasterRegistry.BusMasterRemoved) arg).getBusMaster();
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

    /**
     * Wait for the specified Address to be found on any BusMaster and return it. We will wait forever for this to occur
     * and BusMasters can come and go and we will be patient.
     * @return BusMaster
     */
    public synchronized BusMaster waitForBM(String address) {
        if ((address == null) || (address.isEmpty())) {
            throw new IllegalArgumentException("address null or empty");
        }

        this.address = address;

        try {
            bmRegistry.addObserver(this);

            while (this.foundBM == null) {
                waitingThread = Thread.currentThread();
                try {
                    wait();
                } catch (InterruptedException e) {
                    ;
                }
            }
            waitingThread = null;

        } finally {
            // Start with registering ourselves to track the lifetime of the BusMasters. When these are found we will
            // be called back on the update() method.
            bmRegistry.deleteObserver(this);
            this.address = null;
        }

        // We are not responsible for tearing everything down.
        return this.foundBM;
    }

}
