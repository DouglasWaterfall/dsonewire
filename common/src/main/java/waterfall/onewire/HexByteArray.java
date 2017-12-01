package waterfall.onewire;

import java.util.Arrays;

/**
 * Created by dwaterfa on 11/11/17.
 */
public class HexByteArray {

  private final byte[] data;

  /**
   * @param rawHex Must be even number of upper case hex characters as bytes.
   */
  public HexByteArray(byte[] rawHex) {
    if (rawHex == null) {
      throw new NullPointerException("rawHex");
    }
    if ((rawHex.length % 1) != 0) {
      throw new IllegalArgumentException("rawHex.length must be even number of bytes");
    }
    if (rawHex.length > (32 * 2)) {
      throw new IllegalArgumentException("Max rawHex.length is 64");
    }
    this.data = rawHex;
    checkHexData();
  }

  /**
   * @param hexByteCount The number of bytes to represent using the two byte hex data.
   */
  public HexByteArray(int hexByteCount) {
    if (hexByteCount < 0) {
      throw new IllegalArgumentException("byteCount < 0");
    }
    if (hexByteCount > 32) {
      throw new IllegalArgumentException("byteCount > 32");
    }
    this.data = new byte[hexByteCount * 2];
    Arrays.fill(this.data, (byte) '0');
    checkHexData();
  }

  /**
   * @param copy HexByteArray to duplicate.
   */
  public HexByteArray(HexByteArray copy) {
    if (copy == null) {
      throw new NullPointerException("copy");
    }
    this.data = Arrays.copyOf(copy.getRaw(), copy.getRaw().length);
    checkHexData();
  }

  /**
   * Translates from two character hex bytes to the equivalent byte value.
   * @param hexByteIndex Must be >= 0 and < size()
   * @return The value of the hexByte at the specified index
   */
  public byte get(short hexByteIndex) {
    short index = (short) (hexByteIndex * 2);

    if ((index < 0) || (index >= data.length)) {
      throw new ArrayIndexOutOfBoundsException("hexByteIndex not good");
    }

    return (byte)Convert.hexTo8bits(data[index], data[index + 1]);
  }

  /**
   * @param hexByteIndex Must be >= 0 and < size()
   * @param unsignedS Must be >= 0 and <= 255
   */
  public void set(short hexByteIndex, short unsignedS) {
    short index = (short) (hexByteIndex * 2);

    if ((index < 0) || (index >= data.length)) {
      throw new ArrayIndexOutOfBoundsException("hexByteIndex not good");
    }
    if ((unsignedS < 0) || (unsignedS > 255)) {
      throw new IllegalArgumentException("unsignedS exceeds unsigned byte range");
    }
    data[index] = Convert.fourBitsToHex(unsignedS >> 4);
    data[index + 1] = Convert.fourBitsToHex(unsignedS & 0x0f);
  }

  /**
   * @return Count of HexData bytes.
   */
  public short size() {
    return (short)data.length;
  }

  /**
   * @return The underlying byte array containing the hex characters as bytes.
   */
  public byte[] getRaw() {
    return data;
  }

  /**
   * Throws exception if any of the raw data is not a hex character 0-9A-F.
   */
  public void checkHexData() {
    for (int i = 0; i < data.length; i++) {
      byte b = data[i];
      if (!(((b >= '0') && (b <= '9')) || ((b >= 'A') && (b <= 'F')))) {
        throw new IllegalArgumentException("checkHexData() data[" + i + "] not hex character");
      }
    }
  }

  @Override
  public boolean equals(Object other) {
    return (super.equals(other) &&
        (other instanceof HexByteArray) &&
        (((((HexByteArray) other).data == null) && (data == null)) ||
            ((((HexByteArray) other).data != null) && (data != null) && (((HexByteArray) other).data
                .equals(data)))));
  }
}

