package waterfall.onewire;

import com.dalsemi.onewire.utils.CRC8;
import java.util.Arrays;

/**
 * Created by dwaterfa on 6/15/16.
 */
public class DSAddress extends Object {

  public static String goodHexAddress1 = "EE0000065BC0AE28";
  public static String goodHexAddress2 = "090000065BD53528";
  public static String goodHexAddress3 = "5F0000065CCD1A28";

  private final byte[] addr;

  /**
   * Construct from a String of unchecked data. Must be 16 upper case hex characters in MSB to LSB
   * order, and pass CRC8.
   *
   * @param uncheckedHexAddr the callee may take and keep the argument for itself without fear of it
   * being changed.
   * @return DSAddress
   */
  public static DSAddress fromUncheckedHex(String uncheckedHexAddr) {
    if (uncheckedHexAddr == null) {
      throw new NullPointerException("uncheckedHexAddr");
    }
    if (uncheckedHexAddr.length() != 16) {
      throw new IllegalArgumentException("uncheckedHexAddr bad length");
    }
    if (!uncheckedHexAddr.matches("[0-9A-F]*")) {
      throw new IllegalArgumentException("uncheckedHexAddr bad chars");
    }
    byte[] addr = new byte[8];
    for (int i = 0; i < 8; i++) {
      int hexCharIndex = ((7 - i) * 2);
      addr[i] = (byte) Convert.hexTo8bits((byte) uncheckedHexAddr.charAt(hexCharIndex),
          (byte) uncheckedHexAddr.charAt(hexCharIndex + 1));
    }
    if (CRC8.compute(addr) != 0) {
      throw new IllegalArgumentException("uncheckedHexAddr bad CRC8 " + uncheckedHexAddr);
    }
    return new DSAddress(addr);
  }

  /**
   * Construct from a byte[] of unchecked data. Must be 16 upper case hex characters in MSB to LSB
   * order, and pass CRC8.
   *
   * @param uncheckedHexAddr the callee may take and keep the argument for itself without fear of it
   * being changed.
   * @return DSAddress
   */
  public static DSAddress takeUncheckedHex(byte[] uncheckedHexAddr) {
    if (uncheckedHexAddr == null) {
      throw new NullPointerException("uncheckedHexAddr");
    }
    if (uncheckedHexAddr.length != 16) {
      throw new IllegalArgumentException("uncheckedHexAddr bad length");
    }
    byte[] addr = new byte[8];
    for (int i = 0; i < 8; i++) {
      int hexCharIndex = ((7 - i) * 2);
      addr[i] = (byte) Convert.hexTo8bits(uncheckedHexAddr[hexCharIndex],
          uncheckedHexAddr[hexCharIndex + 1]);
    }
    if (CRC8.compute(addr) != 0) {
      throw new IllegalArgumentException("uncheckedHexAddr bad CRC8");
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
    int hexDataIndex = 0 + index;
    for (int i = 7; i >= 0; i--) {
      to[hexDataIndex++] = Convert.fourBitsToHex(((int) addr[i] & 0xff) >> 4);
      to[hexDataIndex++] = Convert.fourBitsToHex(((int) addr[i] & 0xff) & 0xf);
    }
    return to;
  }

  /**
   * @return The upper case hex characters in MSB to LSB order.
   */
  @Override
  public String toString() {
    return new String(copyHexBytesTo(new byte[16], 0));
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

}
