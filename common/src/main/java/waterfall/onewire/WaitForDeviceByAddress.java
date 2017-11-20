/**
 * Created by dwaterfa on 7/23/17.
 */
package waterfall.onewire;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import waterfall.onewire.busmaster.BusMaster;
import waterfall.onewire.busmaster.NotifySearchBusCmdResult;
import waterfall.onewire.busmaster.SearchBusCmd;

/**
 * Implements the waiting for Address scheme. There will be one instance of this for a specified
 * Address. It will be registered here as an Observer, which will maintain its lifetime, and will
 * get notified of new bus masters AND removed busmasters so it can scheduled and stop scheduling
 * searches as required.
 */
public class WaitForDeviceByAddress implements Observer, NotifySearchBusCmdResult {

  private final BusMasterRegistry bmRegistry;
  private final boolean typeByAlarm;
  private final long bmSearchPeriodMSec;
  private final ArrayList<BusMaster> bmScheduledList;
  private final HashMap<String, WaitForDeviceByAddressCallback> waitMap;

  public WaitForDeviceByAddress(BusMasterRegistry bmRegistry, boolean typeByAlarm,
      long bmSearchPeriodMSec) {
    if (bmRegistry == null) {
      throw new IllegalArgumentException("bmRegistry null");
    }
    if (bmSearchPeriodMSec < 1) {
      throw new IllegalArgumentException("bmSearchPeriod less than 1");
    }
    this.bmRegistry = bmRegistry;
    this.typeByAlarm = typeByAlarm;
    this.bmSearchPeriodMSec = bmSearchPeriodMSec;
    this.bmScheduledList = new ArrayList<>();
    this.waitMap = new HashMap<>();
  }

  // We are called from the BusMasterRegistry with a BusMaster and we will schedule a search on it.
  @Override // Observer
  public void update(Observable o, Object arg) {
    boolean added = false;
    boolean removed = false;
    BusMaster bm = null;

    synchronized (this) {
      if ((o instanceof BusMasterRegistry) && (arg != null)) {
        if ((arg instanceof BusMasterRegistry.BusMasterAdded) && (!waitMap.isEmpty())) {
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
        result = bm.scheduleNotifySearchBusCmd(this, typeByAlarm, bmSearchPeriodMSec);
      } catch (Exception e) {
        System.err
            .println(bm.getName() + ": WaitForDeviceAddress() scheduleNotifySearchBusCmd:" + e);
      }
      if (result != BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success) {
        throw new IllegalArgumentException(
            bm.getName() + ": scheduleNotifySearchBusCmd returned " + result.name());
      }
    } else if (removed) {
      BusMaster.CancelScheduledNotifySearchBusCmdResult result = null;
      try {
        result = bm.cancelScheduledNotifySearchBusCmd(this, typeByAlarm);
      } catch (Exception e) {
        System.err.println(
            bm.getName() + ": WaitForDeviceAddress() cancelScheduledNotifySearchBusCmd:" + e);
      }
      if (result != BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_Success) {
        throw new IllegalArgumentException(
            bm.getName() + ": cancelScheduledNotifySearchBusCmd returned " + result.name());
      }
    }
  }

  // Called from NotifySearchBusCmdResult with the results of any searches. If we find the Address we want then
  // we know which BusMaster controls it and we can cancel all of the scheduled searches.
  @Override // NotifySearchBusCmdResult
  public synchronized void notify(BusMaster bm, boolean byAlarm,
      SearchBusCmd.ResultData searchResultData) {
    for (String addr : searchResultData.getList()) {
      if (waitMap.isEmpty()) {
        break;
      }

      WaitForDeviceByAddressCallback callback = waitMap.get(addr);
      if (callback != null) {
        try {
          if (callback.deviceFound(bm, addr, typeByAlarm)) {
            cancelAddress(callback, addr);
          }
        } catch (Exception e) {
          System.err.println("Exception from " + callback.getClass().getSimpleName() + ":" + e);
        }
      }
    }
  }

  public void addAddress(WaitForDeviceByAddressCallback callback, String[] addresses) {
    if (callback == null) {
      throw new IllegalArgumentException("callback");
    }

    if (addresses == null) {
      throw new IllegalArgumentException("addresses");
    }

    boolean mapWasEmpty, mapNowEmpty;

    synchronized (this) {
      mapWasEmpty = waitMap.isEmpty();

      for (String address : addresses) {
        if (waitMap.containsKey(address)) {
          throw new IllegalArgumentException("dup " + address);
        }

        waitMap.put(address, callback);
      }

      mapNowEmpty = waitMap.isEmpty();
    }

    if ((mapWasEmpty) && (!mapNowEmpty)) {
      // schedule searches. This may instantly callback with the BMs already known
      bmRegistry.addObserver(this);
    }
  }

  public void cancelAddress(WaitForDeviceByAddressCallback callback, String address) {
    if (callback == null) {
      throw new IllegalArgumentException("callback");
    }

    if (address == null) {
      throw new IllegalArgumentException("address");
    }

    synchronized (this) {
      WaitForDeviceByAddressCallback t_callback = waitMap.get(address);
      if (t_callback == null) {
        throw new IllegalArgumentException("address not found");
      }
      if (t_callback != callback) {
        throw new IllegalArgumentException("not your address");
      }
      waitMap.remove(address);

      if (waitMap.isEmpty()) {
        bmRegistry.deleteObserver(this);
        for (BusMaster t_bm : bmScheduledList) {
          t_bm.cancelScheduledNotifySearchBusCmd(this, typeByAlarm);
        }
        bmScheduledList.clear();
      }
    }
  }

}

