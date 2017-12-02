package waterfall.onewire;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Created by dwaterfa on 11/30/17.
 */
public class DSAddressTest {

  @Test(dataProvider = "fromUncheckedHexNegativeCases")
  public void testFromUncheckedHexNegative(String hexAddr, Exception expectedException) {
    try {
      DSAddress.fromUncheckedHex(hexAddr);
      Assert.fail("expected exception");
    } catch (Exception e) {
      Assert.assertEquals(e.getClass(), expectedException.getClass());
      Assert.assertEquals(e.getMessage(), expectedException.getMessage());
    }
  }

  @DataProvider
  public Object[][] fromUncheckedHexNegativeCases() {
    return new Object[][]{
        {null, new NullPointerException("uncheckedHexAddr")},
        {"foofoofoofoofoo", new IllegalArgumentException("uncheckedHexAddr bad length")},
        {"foofoofoofoofoofo", new IllegalArgumentException("uncheckedHexAddr bad length")},
        {"FFFFFFFFFFFFFFFG", new IllegalArgumentException("uncheckedHexAddr not hex")},
        {"AbCdEf0123x56789", new IllegalArgumentException("uncheckedHexAddr not hex")},
        {"ABCDEF0123456789",
            new IllegalArgumentException("uncheckedHexAddr bad CRC8 ABCDEF0123456789")},
    };
  }

  @Test(dataProvider = "takeUnCRCCheckedHexNegativeCases")
  public void testTakeUncheckedHexNegative(byte[] hexAddr, Exception expectedException) {
    try {
      DSAddress.takeUnCRCCheckedHex(hexAddr);
      Assert.fail("expected exception");
    } catch (Exception e) {
      Assert.assertEquals(e.getClass(), expectedException.getClass());
      Assert.assertEquals(e.getMessage(), expectedException.getMessage());
    }
  }

  @DataProvider
  public Object[][] takeUnCRCCheckedHexNegativeCases() {
    byte[] shortLength = new byte[]{'f', 'o', 'o', 'f', 'o', 'o', 'f', 'o', 'o', 'f', 'o', 'o', 'f',
        'o', 'o'};
    byte[] longLength = new byte[]{'f', 'o', 'o', 'f', 'o', 'o', 'f', 'o', 'o', 'f', 'o', 'o', 'f',
        'o', 'o', 'f', 'o'};
    byte[] badHex = new byte[]{'F', 'A', '0', '1', '2', 'C', 'E', 'D', '5', '7', '6', '8', '9',
        'B', 'f', '4'};
    byte[] badCRC = new byte[]{'F', 'A', '0', '1', '2', 'C', 'E', 'D', '5', '7', '6', '8', '9',
        'B', 'F', '4'};
    return new Object[][]{
        {null, new NullPointerException("takeUnCRCCheckedHexAddr")},
        {shortLength, new IllegalArgumentException("takeUnCRCCheckedHexAddr bad length")},
        {longLength, new IllegalArgumentException("takeUnCRCCheckedHexAddr bad length")},
        {badHex, new IllegalArgumentException("hex is not upper case hex chars")},
        {badCRC, new IllegalArgumentException("takeUnCRCCheckedHexAddr bad CRC8")}
    };
  }

  /*
  throw new NullPointerException("checkedAddrToTake");
if (checkedAddrToTake.length != 8) {
    throw new IllegalArgumentException("checkedAddrToTake bad length");
    */

  @DataProvider
  public Object[][] takeCheckedNegativeCases() {
    return new Object[][]{
        {null, new NullPointerException("checkedAddrToTake")},
        {new byte[7], new IllegalArgumentException("checkedAddrToTake bad length")},
        {new byte[9], new IllegalArgumentException("checkedAddrToTake bad length")}
    };
  }

  @Test(dataProvider = "takeCheckedNegativeCases")
  public void testTakeCheckedNegative(byte[] checkedAddrToTake, Exception expectedException) {
    try {
      DSAddress.takeChecked(checkedAddrToTake);
      Assert.fail("expected exception");
    } catch (Exception e) {
      Assert.assertEquals(e.getClass(), expectedException.getClass());
      Assert.assertEquals(e.getMessage(), expectedException.getMessage());
    }
  }

}
