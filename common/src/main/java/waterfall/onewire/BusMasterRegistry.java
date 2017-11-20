package waterfall.onewire;

import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import waterfall.onewire.busmaster.BusMaster;

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
   */
  @Override
  public void addObserver(Observer o) {
    super.addObserver(o);

    BusMaster[] bms = getBusMasters();
    for (BusMaster bm : bms) {
      o.update(this, new BusMasterAdded(bm));
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

