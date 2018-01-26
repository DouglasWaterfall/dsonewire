package waterfall.onewire.device;

import com.dalsemi.onewire.utils.CRC8;
import java.util.Arrays;

/**
 * Created by dwaterfa on 11/17/17.
 */
public class DS18B20Scratchpad {

  public static final byte RESOLUTION_9 = 0;
  public static final byte RESOLUTION_10 = 1;
  public static final byte RESOLUTION_11 = 2;
  public static final byte RESOLUTION_12 = 3;

  public static final byte DEFAULT_HALARM = 75;
  public static final byte DEFAULT_LALARM = 70;
  public static final byte DEFAULT_RESOLUTION = RESOLUTION_12;

  /**
   * The format of the configuration byte is { 0 R1 R0 1 1 1 1 1 }
   */
  public static final byte MASK_BYTE_4 = (byte) 0x1f;
  public static final byte RESERVED_BYTE_5 = (byte) 0xff;
  // byte 6 is reserved, but value not specified
  public static final byte RESERVED_BYTE_7 = (byte) 0x10;

  private static final byte[] powerOnDefault = getFactoryDefault();
  public final byte[] data;

  public DS18B20Scratchpad() {
    data = Arrays.copyOf(powerOnDefault, powerOnDefault.length);
  }

  public DS18B20Scratchpad(byte[] data) {
    if (data == null) {
      throw new NullPointerException("data");
    }
    if (data.length != 9) {
      throw new IllegalArgumentException("data must be 9 bytes");
    }
    // We intentionally do NOT check for validity here.
    this.data = data;
  }

  private static byte[] getFactoryDefault() {
    byte[] data = new byte[9];
    data[0] = 0x10; // 1 C
    data[1] = 0x00;
    data[2] = DEFAULT_HALARM;
    data[3] = DEFAULT_LALARM;
    data[4] = MASK_BYTE_4 + (DEFAULT_RESOLUTION << 5);   // 12 bits
    data[5] = RESERVED_BYTE_5;
    data[6] = 0;
    data[7] = RESERVED_BYTE_7;
    data[8] = (byte) CRC8.compute(data, 0, 8);
    return data;
  }

  public float getTempC() {
    // mask out the undefined bits based on resolution
    byte lsb = data[0];
    switch (getResolution()) {
      case 0: // 9
        lsb &= 0xf8;    // 1 fractional bit
        break;
      case 1: // 10
        lsb &= 0xfc;    // 2 fractional bits
        break;
      case 2: // 11
        lsb &= 0xfe;    // 3 fractional bits
        break;
      case 3: // 12
      default:
        // 12 bits - no masking - 4 fractional bits
        ;
    }

    byte msb = data[1];

    return (float)((((int)(msb << 4) | ((lsb >> 4) & 0xf)) << 4) | (lsb & 0xf)) / (float)16.0;
  }

  public DS18B20Scratchpad setTempC(float tempC) {
    int v = (int)(tempC * 16.0);

    // 6 bytes
    if (((v >> 4) > 127) || ((v >> 4) < -127)) {
      throw new IllegalArgumentException("tempC exceeds limits");
    }

    // possible loss of precision here...
    data[0] = (byte)(v & 0xff);
    data[1] = (byte)(v >> 8);
    data[8] = (byte) CRC8.compute(data, 0, 8);

    return this;
  }

  public byte getTempHAlarm() {
    return data[2];
  }

  public DS18B20Scratchpad setTempHAlarm(byte tempC) {
    data[2] = tempC;
    data[8] = (byte) CRC8.compute(data, 0, 8);
    return this;
  }

  public byte getTempLAlarm() {
    return data[3];
  }

  public DS18B20Scratchpad setTempLAlarm(byte tempC) {
    data[3] = tempC;
    data[8] = (byte) CRC8.compute(data, 0, 8);
    return this;
  }

  /**
   * 0 = 9 1 = 10 2 = 11 3 = 12
   */
  public byte getResolution() {
    return (byte) ((data[4] >> 5) & 0x3);
  }

  public DS18B20Scratchpad setResolution(byte resolution) {
    if ((resolution < 0) || (resolution > 3)) {
      throw new IllegalArgumentException("resolution not valid");
    }
    data[4] = (byte) (MASK_BYTE_4 | (resolution << 5));
    data[8] = (byte) CRC8.compute(data, 0, 8);
    return this;
  }

  public boolean checkAllFFs() {
    for (byte b : data) {
      if (b != (byte) 0xff) {
        return false;
      }
    }
    return true;
  }

  public boolean checkValid() {
    return ((CRC8.compute(data) == 0) &&
        ((data[4] & 0x9f) == (int) MASK_BYTE_4) &&
        (data[5] == (int) RESERVED_BYTE_5) &&
        // [6] is also reserved but no constant is specified.
        (data[7] == (int) RESERVED_BYTE_7));
  }

  public byte[] getWriteScratchpadBytes() {
    return new byte[]{data[2], data[3], data[4]};
  }

  public byte[] getRawBytes() {
    return data;
  }

}
