package waterfall.onewire.busmasters.HA7S.part;

import waterfall.onewire.Convert;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.Command;
import waterfall.onewire.busmasters.HA7S.HA7SDummyDevice;

/**
 * Created by dwaterfa on 11/8/17.
 */
public class DS18B20 implements HA7SDummyDevice {

  private final DSAddress dsAddress;
  private boolean isParasitic = false;
  private boolean hasAlarm;
  private short readIndex;
  private byte[][] readData;

  public DS18B20(DSAddress dsAddress) {
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

    switch ((byte) Convert.hexTo8bits(data[start], data[start + 1])) {
      case Command.CONVERT_T:
        convertT(data, start, end);
        break;

      case Command.READ_POWER_SUPPLY:
        readPowerSupply(data, start, end);
        break;

      case Command.READ_SCRATCHPAD:
        readScratchpad(data, start, end);
        break;

      default:
        throw new IllegalArgumentException(String.format("Unknown cmd:0x%02x", data[start]));
    }
  }

  public void readBit(byte[] data) {
    // Expected to be 0 if the device is busy Converting
    data[0] = '1';
  }

  //
  // Our private methods.
  //
  public void convertT(byte[] data, short start, short end) {
    if ((end - start) > 2) { // hex for cmd
      throw new IllegalArgumentException("CONVERT_T extra data");
    }

    // At one point we had the Convert be the way things were advanced, that turned out to be
    // problematic when simulating CRC errors. So we have to advance each time we read.
  }

  public void readPowerSupply(byte[] data, short start, short end) {
    if ((end - start) > 2) { // hex for cmd.
      throw new IllegalArgumentException("READ_POWER_SUPPLY extra data");
    }

    // we only drive the LSB bit low, otherwise leave alone
    // Note: the device only drives one byte low - need to check what happens with the rest of the byte.
    if ((end > start) && (isParasitic)) {
      data[start + 1] &= (byte) 0xfe;
    }
  }

  public void readScratchpad(byte[] data, short start, short end) {
    if ((end - start) > ((9 + 1) * 2)) {
      throw new IllegalArgumentException("READ_SCRATCHPATH extra data");
    }
    if (readData == null) {
      throw new IllegalArgumentException("READ_SCRATCHPAD no read data");
    }

    // ConvertT is the only way we advance the data.

    int y = 0;
    for (int i = (start + 2); i < end; i += 2) {
      int b = Convert.hexTo8bits(data[i], data[i + 1]);
      b &= readData[readIndex][y++];
      data[i] = Convert.fourBitsToHex(b >> 4);
      data[i + 1] = Convert.fourBitsToHex(b & 0xf);
    }

    // We advance on every read if we have something to advance to.
    if ((readIndex + 1) < readData.length) {
      readIndex++;
    }

    data.toString();
  }

  public DS18B20 setHasAlarm(boolean hasAlarm) {
    this.hasAlarm = hasAlarm;
    return this;
  }

  public boolean getParasitic() {
    return isParasitic;
  }

  public DS18B20 setParasitic(boolean isParasitic) {
    this.isParasitic = isParasitic;
    return this;
  }

  public byte[][] getScratchpadData() {
    return this.readData;
  }

  public DS18B20 setScratchPadData(byte[][] data) {
    if ((data == null) || (data.length < 1)) {
      throw new IllegalArgumentException("data is null or empty");
    }
    for (byte[] rd : data) {
      if ((rd == null) || (rd.length != 9)) {
        throw new IllegalArgumentException("data element lengths must be 9");
      }
    }
    readData = data;
    readIndex = 0;
    return this;
  }

}

