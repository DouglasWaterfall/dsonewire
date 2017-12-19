package waterfall.onewire.busmasters.HA7S;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import waterfall.onewire.Convert;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.SearchBusCmd;

/**
 * Created by dwaterfa on 12/17/17.
 */
public class SearchCmdTests extends TestBase {

  //
  // Search
  //
  @Test
  public void testSearchCmd() {

    final long cmdFirstWriteCTM = 3;
    final long cmdFirstReadCRCTM = 4;
    final DSAddress dev_A = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);
    final DSAddress dev_B = DSAddress.fromUncheckedHex(DSAddress._090000065BD53528);
    final DSAddress dev_C = DSAddress.fromUncheckedHex(DSAddress._5F0000065CCD1A28);

    HA7SSerial mockSerial = getStartedMockSerial();
    HA7S ha7s = new HA7S(mockSerial);

    SearchBusCmd searchBusCmd = ha7s.querySearchBusCmd();
    Assert.assertNotNull(searchBusCmd);
    Assert.assertFalse(searchBusCmd.isByAlarm());
    Assert.assertFalse(searchBusCmd.isByFamilyCode());
    Assert.assertNull(searchBusCmd.getResult());

    try {

      when(mockSerial.writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
          .thenAnswer(makeAnswerForSearch(new byte[]{'S'}, dev_A.copyHexBytesTo(new byte[16], 0),
              cmdFirstWriteCTM,
              cmdFirstReadCRCTM))
          .thenAnswer(
              makeAnswerForSearch(new byte[]{'s'}, dev_B.copyHexBytesTo(new byte[16], 0), 5L, 6L))
          .thenAnswer(
              makeAnswerForSearch(new byte[]{'s'}, dev_C.copyHexBytesTo(new byte[16], 0), 7L, 8L))
          .thenAnswer(makeAnswerForSearch(new byte[]{'s'}, null, 9L, 10L));

      SearchBusCmd.Result result = searchBusCmd.execute();
      Assert.assertEquals(result, SearchBusCmd.Result.success);

      Assert.assertEquals(searchBusCmd.getResult(), SearchBusCmd.Result.success);
      Assert.assertNotNull(searchBusCmd.getResultList());
      Assert.assertEquals(searchBusCmd.getResultList().size(), 3);
      Assert.assertTrue(searchBusCmd.getResultListCRC32() != 0);
      Assert.assertEquals(searchBusCmd.getResultWriteCTM(), cmdFirstWriteCTM);
      Assert.assertTrue(searchBusCmd.getResultList().contains(dev_A) &&
          searchBusCmd.getResultList().contains(dev_B) &&
          searchBusCmd.getResultList().contains(dev_C));

      ha7s.stopBus(null);

      searchBusCmd.execute();
      Assert.assertEquals(result, SearchBusCmd.Result.bus_not_started);

    } catch (Exception e) {
      Assert.fail("Unexpected exception:" + e);
    }

  }

  @Test
  public void testAlarmSearchCmd() {

    final long cmdFirstWriteCTM = 3;
    final long cmdFirstReadCRCTM = 4;
    final DSAddress dev_A = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);
    final DSAddress dev_B = DSAddress.fromUncheckedHex(DSAddress._090000065BD53528);
    final DSAddress dev_C = DSAddress.fromUncheckedHex(DSAddress._5F0000065CCD1A28);

    HA7SSerial mockSerial = getStartedMockSerial();
    HA7S ha7s = new HA7S(mockSerial);

    SearchBusCmd searchBusCmd = ha7s.querySearchBusByAlarmCmd();
    Assert.assertNotNull(searchBusCmd);
    Assert.assertTrue(searchBusCmd.isByAlarm());
    Assert.assertFalse(searchBusCmd.isByFamilyCode());
    Assert.assertNull(searchBusCmd.getResult());

    try {

      when(mockSerial.writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
          .thenAnswer(makeAnswerForSearch(new byte[]{'C'}, dev_A.copyHexBytesTo(new byte[16], 0),
              cmdFirstWriteCTM,
              cmdFirstReadCRCTM))
          .thenAnswer(
              makeAnswerForSearch(new byte[]{'c'}, dev_B.copyHexBytesTo(new byte[16], 0), 5L, 6L))
          .thenAnswer(
              makeAnswerForSearch(new byte[]{'c'}, dev_C.copyHexBytesTo(new byte[16], 0), 7L, 8L))
          .thenAnswer(makeAnswerForSearch(new byte[]{'c'}, null, 9L, 10L));

      SearchBusCmd.Result result = searchBusCmd.execute();
      Assert.assertEquals(result, SearchBusCmd.Result.success);
      Assert.assertEquals(searchBusCmd.getResult(), SearchBusCmd.Result.success);
      Assert.assertNotNull(searchBusCmd.getResultList());
      Assert.assertEquals(searchBusCmd.getResultList().size(), 3);
      Assert.assertTrue(searchBusCmd.getResultListCRC32() != 0);
      Assert.assertEquals(searchBusCmd.getResultWriteCTM(), cmdFirstWriteCTM);
      Assert.assertTrue(searchBusCmd.getResultList().contains(dev_A) &&
          searchBusCmd.getResultList().contains(dev_B) &&
          searchBusCmd.getResultList().contains(dev_C));

      ha7s.stopBus(null);

      result = searchBusCmd.execute();
      Assert.assertEquals(result, SearchBusCmd.Result.bus_not_started);

    } catch (Exception e) {
      Assert.fail("Unexpected exception:" + e);
    }
  }

  @Test
  public void testSearchFamilyCmd() {

    final long cmdFirstWriteCTM = 3;
    final long cmdFirstReadCRCTM = 4;
    final DSAddress dev_A = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);
    final DSAddress dev_B = DSAddress.fromUncheckedHex(DSAddress._090000065BD53528);
    final DSAddress dev_C = DSAddress.fromUncheckedHex(DSAddress._5F0000065CCD1A28);

    HA7SSerial mockSerial = getStartedMockSerial();
    HA7S ha7s = new HA7S(mockSerial);

    short familyCode = 0x28;

    SearchBusCmd searchBusCmd = ha7s.querySearchBusByFamilyCmd(familyCode);
    Assert.assertNotNull(searchBusCmd);
    Assert.assertFalse(searchBusCmd.isByAlarm());
    Assert.assertTrue(searchBusCmd.isByFamilyCode());
    Assert.assertNull(searchBusCmd.getResult());

    try {
      when(mockSerial.writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
          .thenAnswer(makeAnswerForSearch(new byte[]{'F', Convert.fourBitsToHex(familyCode >> 4),
                  Convert.fourBitsToHex(familyCode & 0xf)},
              dev_A.copyHexBytesTo(new byte[16], 0), cmdFirstWriteCTM, cmdFirstReadCRCTM))
          .thenAnswer(
              makeAnswerForSearch(new byte[]{'f'}, dev_B.copyHexBytesTo(new byte[16], 0), 5L, 6L))
          .thenAnswer(
              makeAnswerForSearch(new byte[]{'f'}, dev_C.copyHexBytesTo(new byte[16], 0), 7L, 8L))
          .thenAnswer(makeAnswerForSearch(new byte[]{'f'}, null, 9L, 10L));

      SearchBusCmd.Result result = searchBusCmd.execute();
      Assert.assertEquals(result, SearchBusCmd.Result.success);

      Assert.assertEquals(searchBusCmd.getResult(), SearchBusCmd.Result.success);
      Assert.assertNotNull(searchBusCmd.getResultList());
      Assert.assertEquals(searchBusCmd.getResultList().size(), 3);
      Assert.assertTrue(searchBusCmd.getResultListCRC32() != 0);
      Assert.assertEquals(searchBusCmd.getResultWriteCTM(), cmdFirstWriteCTM);
      Assert.assertTrue(searchBusCmd.getResultList().contains(dev_A) &&
          searchBusCmd.getResultList().contains(dev_B) &&
          searchBusCmd.getResultList().contains(dev_C));

      ha7s.stopBus(null);

      result = searchBusCmd.execute();
      Assert.assertEquals(result, SearchBusCmd.Result.bus_not_started);

    } catch (Exception e) {
      Assert.fail("Unexpected exception:" + e);
    }

  }

  /*
  @Test(dataProvider = "createPositiveCases")
  public void testNotByAlarmPositive(DSAddress[] deviceAddresses) {

    Assert.assertNotNull(deviceAddresses);
    Assert.assertTrue(deviceAddresses.length <= 2);

    HA7S spyHA7S = Mockito.spy(new HA7S(mock(HA7SSerial.class)));
    Mockito.doReturn(true).when(spyHA7S).getIsStarted();

    SearchBusCmd cmd = spyHA7S.querySearchBusCmd();
    Assert.assertFalse(cmd.isByAlarm());
    Assert.assertFalse(cmd.isByFamilyCode());

    long firstWriteCTM = 5L;
    long secondWriteCTM = 6L;

    if (deviceAddresses.length == 0) {
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, null, firstWriteCTM);
      Mockito.doAnswer(answer).when(spyHA7S).cmdSearchROM(any(ArrayList.class), any(Logger.class));

    } else if (deviceAddresses.length == 1) {
      ArrayList<byte[]> ret = new ArrayList<byte[]>();
      ret.add(deviceAddresses[0].copyHexBytesTo(new byte[16], 0));
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, ret, firstWriteCTM);
      Mockito.doAnswer(answer).when(spyHA7S).cmdSearchROM(any(ArrayList.class), any(Logger.class));

    } else if (deviceAddresses.length == 2) {
      ArrayList<byte[]> ret = new ArrayList<byte[]>();
      ret.add(deviceAddresses[0].copyHexBytesTo(new byte[16], 0));
      ret.add(deviceAddresses[1].copyHexBytesTo(new byte[16], 0));
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, ret, firstWriteCTM);
      Mockito.doAnswer(answer).when(spyHA7S).cmdSearchROM(any(ArrayList.class), any(Logger.class));

    }

    Assert.assertEquals(cmd.execute(), SearchBusCmd.Result.success);
    Assert.assertEquals(cmd.getResult(), SearchBusCmd.Result.success);
    Assert.assertNotNull(cmd.getResultList());
    Assert.assertEquals(cmd.getResultList().size(), deviceAddresses.length);
    if (deviceAddresses.length == 1) {
      Assert.assertEquals(cmd.getResultList().get(0), deviceAddresses[0]);
    } else if (deviceAddresses.length == 2) {
      Assert.assertEquals(cmd.getResultList().get(1), deviceAddresses[1]);
    }
    Assert.assertEquals(cmd.getResultWriteCTM(), firstWriteCTM);
  }
  */

  /*
  @Test(dataProvider = "createPositiveCases")
  public void testByAlarmPositive(DSAddress[] deviceAddresses) {

    Assert.assertNotNull(deviceAddresses);
    Assert.assertTrue(deviceAddresses.length <= 2);

    HA7S spyHA7S = Mockito.spy(new HA7S(mock(HA7SSerial.class)));
    Mockito.doReturn(true).when(spyHA7S).getIsStarted();

    SearchBusCmd cmd = spyHA7S.querySearchBusByAlarmCmd();
    Assert.assertTrue(cmd.isByAlarm());
    Assert.assertFalse(cmd.isByFamilyCode());

    long firstWriteCTM = 5L;
    long secondWriteCTM = 6L;

    if (deviceAddresses.length == 0) {
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, null, firstWriteCTM);
      Mockito.doAnswer(answer).when(spyHA7S).cmdConditionalSearch(any(ArrayList.class), any(Logger.class));

    } else if (deviceAddresses.length == 1) {
      ArrayList<byte[]> ret = new ArrayList<byte[]>();
      ret.add(deviceAddresses[0].copyHexBytesTo(new byte[16], 0));
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, ret, firstWriteCTM);
      Mockito.doAnswer(answer).when(spyHA7S).cmdConditionalSearch(any(ArrayList.class), any(Logger.class));

    } else if (deviceAddresses.length == 2) {
      ArrayList<byte[]> ret = new ArrayList<byte[]>();
      ret.add(deviceAddresses[0].copyHexBytesTo(new byte[16], 0));
      ret.add(deviceAddresses[1].copyHexBytesTo(new byte[16], 0));
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, ret, firstWriteCTM);
      Mockito.doAnswer(answer).when(spyHA7S).cmdConditionalSearch(any(ArrayList.class), any(Logger.class));
    }

    Assert.assertEquals(cmd.execute(), SearchBusCmd.Result.success);
    Assert.assertEquals(cmd.getResult(), SearchBusCmd.Result.success);
    Assert.assertNotNull(cmd.getResultList());
    Assert.assertEquals(cmd.getResultList().size(), deviceAddresses.length);
    if (deviceAddresses.length == 1) {
      Assert.assertEquals(cmd.getResultList().get(0), deviceAddresses[0]);
    } else if (deviceAddresses.length == 2) {
      Assert.assertEquals(cmd.getResultList().get(1), deviceAddresses[1]);
    }
    Assert.assertEquals(cmd.getResultWriteCTM(), firstWriteCTM);
  }
  */

  /*
  @Test(dataProvider = "createPositiveCases")
  public void testByFamilyCodePositive(DSAddress[] deviceAddresses) {

    Assert.assertNotNull(deviceAddresses);
    Assert.assertTrue(deviceAddresses.length <= 2);

    HA7S spyHA7S = Mockito.spy(new HA7S(mock(HA7SSerial.class)));
    Mockito.doReturn(true).when(spyHA7S).getIsStarted();

    short familyCode = 54;
    SearchBusCmd cmd = spyHA7S.querySearchBusByFamilyCmd(familyCode);
    Assert.assertFalse(cmd.isByAlarm());
    Assert.assertTrue(cmd.isByFamilyCode());

    long firstWriteCTM = 5L;
    long secondWriteCTM = 6L;

    if (deviceAddresses.length == 0) {
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, null, firstWriteCTM);
      Mockito.doAnswer(answer).when(spyHA7S).cmdFamilySearch(any(byte.class), any(ArrayList.class),
          any(Logger.class));

    } else if (deviceAddresses.length == 1) {
      ArrayList<byte[]> ret = new ArrayList<byte[]>();
      ret.add(deviceAddresses[0].copyHexBytesTo(new byte[16], 0));
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, ret, firstWriteCTM);
      Mockito.doAnswer(answer).when(spyHA7S).cmdFamilySearch(any(byte.class), any(ArrayList.class),
          any(Logger.class));

    } else if (deviceAddresses.length == 2) {
      ArrayList<byte[]> ret = new ArrayList<byte[]>();
      ret.add(deviceAddresses[0].copyHexBytesTo(new byte[16], 0));
      ret.add(deviceAddresses[1].copyHexBytesTo(new byte[16], 0));
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, ret, firstWriteCTM);
      Mockito.doAnswer(answer).when(spyHA7S).cmdFamilySearch(any(byte.class), any(ArrayList.class),
          any(Logger.class));

    }

    Assert.assertEquals(cmd.execute(), SearchBusCmd.Result.success);
    Assert.assertEquals(cmd.getResult(), SearchBusCmd.Result.success);
    Assert.assertNotNull(cmd.getResultList());
    Assert.assertEquals(cmd.getResultList().size(), deviceAddresses.length);
    if (deviceAddresses.length == 1) {
      Assert.assertEquals(cmd.getResultList().get(0), deviceAddresses[0]);
    } else if (deviceAddresses.length == 2) {
      Assert.assertEquals(cmd.getResultList().get(1), deviceAddresses[1]);
    }
    Assert.assertEquals(cmd.getResultWriteCTM(), firstWriteCTM);
  }
  */

  @DataProvider
  public Object[][] createPositiveCases() {
    DSAddress one = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);
    DSAddress two = DSAddress.fromUncheckedHex(DSAddress._090000065BD53528);

    DSAddress[] nothingFound = {};
    DSAddress[] oneFound = {one};
    DSAddress[] twoFound = {one, two};

    return new Object[][]{
        {nothingFound},
        {oneFound},
        {twoFound}
    };
  }

  /*
  @Test(dataProvider = "createNegativeCases")
  public void testSearchNegative(HA7SSerial.ReadResult readResult, byte[] rbuf_data,
      SearchBusCmd.Result expectedResult) {

    HA7SSerial mockSerial = mock(HA7SSerial.class);
    when(mockSerial.start(any(Logger.class))).thenReturn(HA7SSerial.StartResult.SR_Success);
    // for starting
    when(mockSerial.writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
        .thenReturn(new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Success));

    HA7S spyHA7S = Mockito.spy(new HA7S(mockSerial));
    Assert.assertFalse(spyHA7S.getIsStarted());
    Assert.assertEquals(spyHA7S.startBus(null).getCode(), StartBusResult.Code.started);

    // searching
    SearchBusCmd searchCmd = spyHA7S.querySearchBusCmd();
    Assert.assertFalse(searchCmd.isByAlarm());
    Assert.assertFalse(searchCmd.isByFamilyCode());

    when(mockSerial.writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
        .thenAnswer(HA7SSerialTest.makeWriteReadTilCRResult(
            new HA7SSerialTest.TestHA7SSerial(readResult, rbuf_data, 5L)));

    Assert.assertEquals(searchCmd.execute(), expectedResult);
  }
  */

  @DataProvider
  public Object[][] createNegativeCases() {

    String invalidAddress = "0123456789ABCDEF";

    return new Object[][]{
        {new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadOverrun), null,
            SearchBusCmd.Result.communication_error},
        {new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadTimeout), null,
            SearchBusCmd.Result.communication_error},
        {new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Error), null,
            SearchBusCmd.Result.communication_error},
        // invalid read count
        {new HA7SSerial.ReadResult(1, 2L, 4L), null,
            SearchBusCmd.Result.communication_error},
        // invalid address
        {new HA7SSerial.ReadResult(16, 2L, 5L),
            invalidAddress.getBytes(), SearchBusCmd.Result.communication_error}
    };
  }

}
