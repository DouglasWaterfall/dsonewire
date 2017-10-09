package waterfall.onewire;

import waterfall.onewire.busmaster.BusMaster;

/**
 * Created by dwaterfa on 10/8/17.
 */
public interface WaitForDeviceByAddressCallback {

    /**
     * This is the notification side of WaitForDeviceByAddress. The notification is done on the Search callback thread
     * under a synchronized lock on the WaitForDeviceByAddress instance.
     *
     * The callee is free to call cancelAddress() from this method if it wishes, alternately it can return true from
     * the method which will do the same thing.
     *
     * @param bm
     * @param dsAddress
     * @param typeByAlarm this comes from an alarm search
     * @return true if the device wait should be cancelled, false otherwise. This is the same as calling cancel()
     */
    boolean deviceFound(BusMaster bm, String dsAddress, boolean typeByAlarm);

}
