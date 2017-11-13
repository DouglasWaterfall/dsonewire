package waterfall.onewire.busmasters.HA7S;

import waterfall.onewire.Convert;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.Command;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Created by dwaterfa on 11/8/17.
 */
public class HA7SDummyDS18B20 implements HA7SDummyDevice {

    private final DSAddress dsAddress;
    private boolean isParasitic = false;
    private boolean hasAlarm;
    private short readIndex;
    private byte[][] readData;

    public HA7SDummyDS18B20(DSAddress dsAddress) {
        this.dsAddress = dsAddress;
        this.isParasitic = isParasitic;
        hasAlarm = false;
        readIndex = Short.MAX_VALUE;
        readData = null;
    }

    @Override
    public DSAddress getDSAddress() {
        return dsAddress;
    }

    @Override
    public boolean hasAlarm() {
        return hasAlarm;
    }

    @Override
    public void writeBlock(byte[] data, short start, short end) {
        if ((data == null) || (data.length < 1)) {
            throw new IllegalArgumentException("in is null or empty");
        }
        if (start < 0) {
            throw new IllegalArgumentException("start < 0");
        }
        if (start >= data.length) {
            throw new IllegalArgumentException("start past length");
        }
        if (end <= start) {
            throw new IllegalArgumentException("end <= start");
        }
        if (end > data.length) {
            throw new IllegalArgumentException("end > length");
        }

        switch ((byte)Convert.hexTo8bits(data[start], data[start + 1])) {
            case Command.CONVERT_T:
                if ((end - start) > 1) {
                    throw new IllegalArgumentException("CONVERT_T extra data");
                }

                break;

            case Command.READ_POWER_SUPPLY:
                if ((end - start) > 2) {
                    throw new IllegalArgumentException("READ_POWER_SUPPLY extra data");
                }

                // we only drive the LSB bit low, otherwise leave alone
                // Note: the device only drives one byte low - need to check what happens with the rest of the byte.
                if ((end > start) && (isParasitic)) {
                    data[start + 1] &= (byte)0xfe;
                }
                break;

            case Command.READ_SCRATCHPAD:
                if ((end - start) > ((9 + 1) * 2)) {
                    throw new IllegalArgumentException("READ_SCRATCHPATH extra data");
                }
                if (readData == null) {
                    throw new IllegalArgumentException("READ_SCRATCHPAD no read data");
                }
                int rIndex = readIndex;
                if (readIndex < readData.length) {
                    readIndex++;
                }
                int y = 0;
                for (int i = (start + 2); i < end; i += 2) {
                    int b = Convert.hexTo8bits(data[i], data[i + 1]);
                    b &= readData[rIndex][y++];
                    data[i] = Convert.fourBitsToHex(b >> 4);
                    data[i + 1] = Convert.fourBitsToHex(b & 0xf);
                }
                break;

            default:
                throw new IllegalArgumentException(String.format("Unknown cmd:0x%02x", data[start]));
        }
    }

    // Our private methods.
    public HA7SDummyDS18B20 setHasAlarm(boolean hasAlarm) {
        this.hasAlarm = hasAlarm;
        return this;
    }

    public boolean getParasitic() {
        return isParasitic;
    }

    public HA7SDummyDS18B20 setParasitic(boolean isParasitic) {
        this.isParasitic = isParasitic;
        return this;
    }

    public byte[][] getScratchpadData() {
        return this.readData;
    }

    public void setScratchPadData(byte[][] data) {
        if ((data == null) || (data.length < 1)) {
            throw new IllegalArgumentException("data is null or empty");
        }
        for (byte[] rd: data) {
            if ((rd == null) || (rd.length != 9)) {
                throw new IllegalArgumentException("data element lengths must be 9");
            }
        }
        readData = data;
        readIndex = 0;
    }

}

