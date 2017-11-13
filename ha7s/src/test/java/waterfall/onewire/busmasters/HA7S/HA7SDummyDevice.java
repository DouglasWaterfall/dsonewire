package waterfall.onewire.busmasters.HA7S;

import waterfall.onewire.DSAddress;

/**
 * Created by dwaterfa on 11/8/17.
 */
public interface HA7SDummyDevice {

    /**
     * Returns the DSAddress of the device.
     * @return The DSAddress of the device
     */
    public DSAddress getDSAddress();

    /**
     * Indicates that device has an active alarm during search.
     * @return true if the device is a search alarm state, false otherwise.
     */
    public boolean hasAlarm();

    /**
     * Send and read bytes to a Device. The HA7S will copy the written data unchanged to the data range and the
     * callee must modify it as appropriate based on the read data of the device.
     *
     * @param data The bytes written and read from the device
     * @param start index where the data begin (expected to be the DS command byte)
     * @param end index where the data ends, where the count is (end - start)
     */
    public void writeBlock(byte[] data, short start, short end);

}
