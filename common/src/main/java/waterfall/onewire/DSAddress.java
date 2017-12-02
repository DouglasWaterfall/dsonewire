package waterfall.onewire;

import com.dalsemi.onewire.utils.CRC8;
import java.util.Arrays;

/**
 * Created by dwaterfa on 6/15/16.
 */
public class DSAddress extends Object {

  public static String _EE0000065BC0AE28 = "EE0000065BC0AE28";
  public static String _ED0000063BC00428 = "ED0000063BC00428";
  public static String _090000065BD53528 = "090000065BD53528";
  public static String _410000063C088028 = "410000063C088028";
  public static String _5F0000065CCD1A28 = "5F0000065CCD1A28";
  public static String _30000000C369CC12 = "30000000C369CC12";
  public static String _7B0000063B759F27 = "7B0000063B759F27";

  private final byte[] addr;
  private byte[] hexBytes = null;

  /**
   * Construct from a String of unchecked data. Must be 16 upper case hex characters in MSB to LSB
   * order, and pass CRC8.
   *
   * @param uncheckedHexAddr the callee may take and keep the argument for itself without fear of it
   * being changed.
   * @return DSAddress
   * @throws IllegalArgumentException for bad length, or failed CRC check
   */
  public static DSAddress fromUncheckedHex(String uncheckedHexAddr) {
    final String notHexMsg = "uncheckedHexAddr not hex";

    if (uncheckedHexAddr == null) {
      throw new NullPointerException("uncheckedHexAddr");
    }
    if (uncheckedHexAddr.length() != 16) {
      throw new IllegalArgumentException("uncheckedHexAddr bad length");
    }
    byte[] addr = new byte[8];
    for (int i = 0; i < 8; i++) {
      int hexCharIndex = ((7 - i) * 2);
      byte msb = checkUpperCaseHex((byte)uncheckedHexAddr.charAt(hexCharIndex), notHexMsg);
      byte lsb = checkUpperCaseHex((byte)uncheckedHexAddr.charAt(hexCharIndex + 1), notHexMsg);
      addr[i] = (byte) Convert.hexTo8bits(msb, lsb);
    }
    if (CRC8.compute(addr) != 0) {
      throw new IllegalArgumentException("uncheckedHexAddr bad CRC8 " + uncheckedHexAddr);
    }
    return new DSAddress(addr);
  }

  /**
   * Construct from a byte[] 16 bytes long, upper case hex bytes in MSB to LSB order, but not yet
   * checked to pass CRC8.
   *
   * @param takeUnCRCCheckedHexAddr the callee may take and keep the argument for itself without
   * fear of it being changed.
   * @return DSAddress
   * @throws IllegalArgumentException for bad length, or failed CRC check
   */
  public static DSAddress takeUnCRCCheckedHex(byte[] takeUnCRCCheckedHexAddr) {
    if (takeUnCRCCheckedHexAddr == null) {
      throw new NullPointerException("takeUnCRCCheckedHexAddr");
    }
    if (takeUnCRCCheckedHexAddr.length != 16) {
      throw new IllegalArgumentException("takeUnCRCCheckedHexAddr bad length");
    }
    byte[] addr = new byte[8];
    for (int i = 0; i < 8; i++) {
      int hexCharIndex = ((7 - i) * 2);
      addr[i] = (byte) Convert.hexTo8bits(takeUnCRCCheckedHexAddr[hexCharIndex],
          takeUnCRCCheckedHexAddr[hexCharIndex + 1]);
    }
    if (CRC8.compute(addr) != 0) {
      throw new IllegalArgumentException("takeUnCRCCheckedHexAddr bad CRC8");
    }
    return new DSAddress(addr);
  }

  /**
   * Construct by taking over a byte[] of previously checked, NOT hex, data. Must be 8 bytes long.
   * Assumed to already have passed CRC8, which requires LSB to MSB order.
   *
   * @param checkedAddrToTake the callee may take and keep the argument for itself without fear of
   * it being changed.
   * @return DSAddress
   */
  public static DSAddress takeChecked(byte[] checkedAddrToTake) {
    if (checkedAddrToTake == null) {
      throw new NullPointerException("checkedAddrToTake");
    }
    if (checkedAddrToTake.length != 8) {
      throw new IllegalArgumentException("checkedAddrToTake bad length");
    }
    return new DSAddress(checkedAddrToTake);
  }

  /**
   * The family code is one byte which declares the device type family. This can be used in the
   * family search.
   * @return the family code
   */
  public short getFamilyCode() {
    return (short) addr[0];
  }

  /**
   * Fill the specified array with the raw bytes. This api avoids exposing the underlying array.
   *
   * @param to array to fill with the 8 bytes of raw data, lsb to msb
   * @param index in the to array to start filling
   * @return the array passed in
   */
  public byte[] copyRawBytesTo(byte[] to, int index) {
    for (int i = 0; i < 8; i++) {
      to[index + i] = addr[i];
    }
    return to;
  }

  /**
   * Fill the specified array with the hex data. This api avoids exposing the underlying array.
   * @param to array to fill with the 16 bytes of hex data, msb to lsb
   * @param index in the to array to start filling
   * @return the array passed in
   */
  public byte[] copyHexBytesTo(byte[] to, int index) {
    byte[] t = getHexBytes();
    for (int i = 0; i < t.length; i++) {
      to[index + i] = t[i];
    }
    return to;
  }

  /**
   * @return The upper case hex characters in MSB to LSB order.
   */
  @Override
  public String toString() {
    return new String(getHexBytes());
  }

  @Override
  public boolean equals(Object other) {
    return ((other instanceof DSAddress) &&
        ((other == this) || (Arrays.equals(addr, ((DSAddress) other).addr))));
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(addr);
  }

  private DSAddress(byte[] checkedAddrToTake) {
    this.addr = checkedAddrToTake;
  }

  private byte[] getHexBytes() {
    if (hexBytes == null) {
      byte[] new_hexBytes = new byte[16];
      int hexDataIndex = 0;
      for (int i = 7; i >= 0; i--) {
        new_hexBytes[hexDataIndex++] = Convert.fourBitsToHex(((int) addr[i] & 0xff) >> 4);
        new_hexBytes[hexDataIndex++] = Convert.fourBitsToHex(((int) addr[i] & 0xff) & 0xf);
      }
      hexBytes = new_hexBytes;
    }
    return hexBytes;
  }

  private static byte checkUpperCaseHex(byte b, String message) {
    if (!(((b >= '0') && (b <= '9')) || ((b >= 'A') && (b <= 'F')))) {
      throw new IllegalArgumentException(message);
    }
    return b;
  }

}
