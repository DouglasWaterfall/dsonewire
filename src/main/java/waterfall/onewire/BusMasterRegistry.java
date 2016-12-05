package waterfall.onewire;

import java.util.*;

import waterfall.onewire.busmaster.BusMaster;

/**
 * This class is simply a registry of BusMasters which have been started.
 */
public class BusMasterRegistry extends Observable {

    private HashMap<String, BusMaster> bmMap = new HashMap<String, BusMaster>();

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
     *
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

}

