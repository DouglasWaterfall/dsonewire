package waterfall.onewire;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Created by dwaterfa on 11/30/17.
 */
public class DSAddressTest {

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

  @Test(dataProvider = "takeUnCRCCheckedHexNegativeCases")
  public void testTakeUnCRCCheckedHexNegative(byte[] hexAddr, Exception expectedException) {
    try {
      DSAddress.takeUnCRCCheckedHex(hexAddr);
      Assert.fail("expected exception");
    } catch (Exception e) {
      Assert.assertEquals(e.getClass(), expectedException.getClass());
      Assert.assertEquals(e.getMessage(), expectedException.getMessage());
    }
  }

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

  @DataProvider
  public Object[][] getFamilyCodeCases() {
    return new Object[][] {
        { DSAddress.fromUncheckedHex(DSAddress._5F0000065CCD1A28), (short)0x28},
        { DSAddress.fromUncheckedHex(DSAddress._7B0000063B759F27), (short)0x27},
        { DSAddress.fromUncheckedHex(DSAddress._30000000C369CC12), (short)0x12}
    };
  }

  @Test(dataProvider = "getFamilyCodeCases")
  public void testGetFamilyCode(DSAddress dsAddress, short expectedFamilyCode) {
    Assert.assertEquals(dsAddress.getFamilyCode(), expectedFamilyCode);
  }

  @DataProvider
  public Object[][] getCopyRawBytesToCases() {

    byte[] _5F0000065CCD1A28 = new byte[] { 0x28, 0x1A, (byte)0xCD, 0x5C, 0x06, 0x00, 0x00, 0x5F };
    byte[] _ED0000063BC00428 = new byte[] { 0x28, 0x04, (byte)0xC0, 0x3B, 0x06, 0x00, 0x00, (byte)0xED };

    return new Object[][] {
        { DSAddress.fromUncheckedHex(DSAddress._5F0000065CCD1A28), 0, _5F0000065CCD1A28 },
        { DSAddress.fromUncheckedHex(DSAddress._5F0000065CCD1A28), 1, _5F0000065CCD1A28 },
        { DSAddress.fromUncheckedHex(DSAddress._ED0000063BC00428), 0, _ED0000063BC00428 },
        { DSAddress.fromUncheckedHex(DSAddress._ED0000063BC00428), 1, _ED0000063BC00428 }
    };
  }

  @Test(dataProvider = "getCopyRawBytesToCases")
  public void testCopyRawBytesToCases(DSAddress dsAddress, int offset, byte[] expected) {

    byte[] pad = new byte[] { (byte)0xff, 0x00 };
    byte[] b = null;
    int last = offset + expected.length;

    for (int j = 0; j < 2; j++) {
      b = new byte[expected.length + offset + 1];
      for (int i = 0; i < offset; i++) {
        b[i] = pad[j];
      }
      b[last] = pad[j];

      byte[] bc = dsAddress.copyRawBytesTo(b, offset);
      Assert.assertTrue(bc == b);
      ;
      for (int i = 0; i < offset; i++) {
        Assert.assertEquals(b[i], pad[j]);
      }
      Assert.assertEquals(b[last], pad[j]);

      for (int i = 0; i < 8; i++) {
        Assert.assertEquals(b[offset + i], expected[i]);
      }
    }
  }

  @DataProvider
  public Object[][] getCopyHexBytesToCases() {

    return new Object[][] {
        { DSAddress.fromUncheckedHex(DSAddress._5F0000065CCD1A28), 0, DSAddress._5F0000065CCD1A28 },
        { DSAddress.fromUncheckedHex(DSAddress._5F0000065CCD1A28), 1, DSAddress._5F0000065CCD1A28 },
        { DSAddress.fromUncheckedHex(DSAddress._ED0000063BC00428), 0, DSAddress._ED0000063BC00428 },
        { DSAddress.fromUncheckedHex(DSAddress._ED0000063BC00428), 1, DSAddress._ED0000063BC00428 }
    };
  }

  @Test(dataProvider = "getCopyHexBytesToCases")
  public void testCopyHexBytesToCases(DSAddress dsAddress, int offset, String expected) {

    byte[] pad = new byte[] { (byte)0xf, 0x0 };
    byte[] b = null;
    int last = offset + expected.length();

    for (int j = 0; j < 2; j++) {
      b = new byte[expected.length() + offset + 1];
      for (int i = 0; i < offset; i++) {
        b[i] = pad[j];
      }
      b[last] = pad[j];

      byte[] bc = dsAddress.copyHexBytesTo(b, offset);
      Assert.assertTrue(bc == b);
      ;
      for (int i = 0; i < offset; i++) {
        Assert.assertEquals(b[i], pad[j]);
      }
      Assert.assertEquals(b[last], pad[j]);

      for (int i = 0; i < 8; i++) {
        Assert.assertEquals(b[offset + i], expected.charAt(i));
      }
    }
  }

  @DataProvider
  public Object[][] getToStringCases() {
    return new Object[][] {
        { DSAddress.fromUncheckedHex(DSAddress._5F0000065CCD1A28), DSAddress._5F0000065CCD1A28 },
        { DSAddress.fromUncheckedHex(DSAddress._ED0000063BC00428), DSAddress._ED0000063BC00428 },
        { DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28), DSAddress._EE0000065BC0AE28 },
        { DSAddress.fromUncheckedHex(DSAddress._7B0000063B759F27), DSAddress._7B0000063B759F27 },
        { DSAddress.fromUncheckedHex(DSAddress._30000000C369CC12), DSAddress._30000000C369CC12 }
    };
  }

  @Test(dataProvider = "getToStringCases")
  public void testToStringCases(DSAddress dsAddress, String expected) {
    Assert.assertEquals(dsAddress.toString(), expected);
  }

  @Test
  public void testEquals() {
    DSAddress v1 = DSAddress.fromUncheckedHex(DSAddress._5F0000065CCD1A28);
    DSAddress v1a = DSAddress.fromUncheckedHex(DSAddress._5F0000065CCD1A28);
    DSAddress v2 = DSAddress.fromUncheckedHex(DSAddress._7B0000063B759F27);

    Assert.assertTrue(v1.equals(v1));
    Assert.assertTrue(v1.equals(v1a));
    Assert.assertTrue(v1a.equals(v1));
    Assert.assertFalse(v1.equals(v2));
    Assert.assertFalse(v2.equals(v1));
  }

}
