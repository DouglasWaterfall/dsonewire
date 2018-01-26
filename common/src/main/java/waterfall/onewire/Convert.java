package waterfall.onewire;

/**
 * Created by dwaterfa on 8/3/16.
 */
public class Convert {
  //
  // char-hex conversion static methods
  //
  // returns new value for toffset.
  //

  private static final byte[] toHexTable = {
      '0', '1', '2', '3', '4', '5', '6', '7',
      '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
  };

  /**
   * Convert a subset of a normal byte[] to a hex encoded byte[]
   *
   * @param from byte array with full bytes
   * @param from_offset offset in the from array to start reading
   * @param from_count count of bytes in the from array to convert
   * @param tbuf byte array to be filled with HEX 0-9A-B byte values
   * @param toffset offset in the tbuf array to start writing (2:1 to the from array)
   * @return number of bytes written into the tbuf array
   */
  private static int byteToHex(final byte[] from, int from_offset, int from_count,
      final byte[] tbuf, int toffset) {
    int f_index = from_offset;
    int t_idx = toffset;

    for (int i = 0; i < from_count; i++) {
      tbuf[t_idx++] = fourBitsToHex(((int) from[f_index] & 0xff) >> 4);
      tbuf[t_idx++] = fourBitsToHex(((int) from[f_index++] & 0xff) & 0xf);
    }

    return t_idx;
  }

  /**
   * Convert from a normal byte[] to a hex encoded byte[]
   *
   * @param from byte array with full bytes to convert
   * @param tbuf byte array to be filled with HEX 0-9A-B byte values
   * @param toffset offset in the tbuf array to start writing (2:1 to the from array)
   * @return number of bytes written into the tbuf array
   */
  private static int byteToHex(final byte[] from, final byte[] tbuf, int toffset) {
    return byteToHex(from, 0, from.length, tbuf, toffset);
  }

  /**
   * Fill a hex encoded byte[] with a normal byte value
   *
   * @param fill_char full byte value to fill with
   * @param fill_count number of full byte value to fill
   * @param tbuf byte array to be filled with HEX 0-9A-B byte values
   * @param toffset offset in the tbuf array to start writing (2:1 to the from array)
   * @return number of bytes written into the tbuf array
   */
  private static int byteToHex(byte fill_char, int fill_count, final byte[] tbuf, int toffset) {
    int t_idx = toffset;
    byte b1 = fourBitsToHex(((int) fill_char & 0xff) >> 4);
    byte b2 = fourBitsToHex(((int) fill_char & 0xff) & 0xf);

    for (int i = 0; i < fill_count; i++) {
      tbuf[t_idx++] = b1;
      tbuf[t_idx++] = b2;
    }

    return t_idx;
  };

  /**
   * Converts the byte array to and array of two hex characters per byte.
   * @param from
   * @return
   */
  public static byte[] byteToHex(byte from[]) {
    byte[] ret = new byte[from.length * 2];
    byteToHex(from, 0, from.length, ret, 0);
    return ret;
  }

  /**
   * Convert a hex encoded byte[] to a full byte[]
   *
   * @param from hex encoded byte[] to read from
   * @param findex offset in the from[] to start reading from
   * @param fcount count of hex bytes to read in the from[] array, expected to be an even number
   * @param tbuf full byte[] to write to
   * @param toffset offset in the full byte[] array to start writing to
   * @return the number of full bytes written
   */
  public static int hexToByte(final byte[] from, int findex, int fcount, byte[] tbuf, int toffset) {
    int idx = toffset;

    for (int i = 0; i < fcount; i += 2) {
      tbuf[idx++] = (byte) ((hexToFourBits(from[findex + i]) << 4) + hexToFourBits(
          from[findex + i + 1]));
    }

    return (fcount / 2);
  }

  /**
   * Convert a hex encoded byte[] to a new full byte[]
   *
   * @param from hex encoded byte[] to read from
   * @return new byte[] with the full data
   */
  public static byte[] hexToByte(final byte[] from) {
    int fromLen = from.length;

    byte[] ret = new byte[fromLen / 2];

    hexToByte(from, 0, fromLen, ret, 0);

    return ret;
  }

  public static byte fourBitsToHex(int fourBits) {
    return toHexTable[fourBits];
  }

  public static byte hexToFourBits(byte hex) {
    if ((hex >= '0') && (hex <= '9')) {
      return (byte) (hex - '0');
    }

    if ((hex >= 'A') && (hex <= 'F')) {
      return (byte) (10 + (hex - 'A'));
    }

    throw new IllegalArgumentException("hex is not upper case hex chars");
  }

  public static int hexTo8bits(byte msbHex, byte lsbHex) {
    return (hexToFourBits(msbHex) << 4) | hexToFourBits(lsbHex);
  }

}
