package waterfall.onewire.busmasters.HA7S;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.SearchBusCmd;
import waterfall.onewire.busmaster.StartBusCmd;


/**
 * Created by dwaterfa on 8/6/17.
 */
public class HA7SSearchBusCmdTest {

  @Test(dataProvider = "createPositiveCases")
  public void testNotByAlarmPositive(DSAddress[] deviceAddresses) {

    Assert.assertNotNull(deviceAddresses);
    Assert.assertTrue(deviceAddresses.length <= 2);

    HA7S mockHA7S = mock(HA7S.class);
    when(mockHA7S.getIsStarted()).thenReturn(true);

    SearchBusCmd cmd = new HA7SSearchBusCmd(mockHA7S);
    Assert.assertFalse(cmd.isByAlarm());
    Assert.assertFalse(cmd.isByFamilyCode());

    long firstWriteCTM = 5L;
    long secondWriteCTM = 6L;

    if (deviceAddresses.length == 0) {
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, null, firstWriteCTM);
      when(mockHA7S.cmdSearchROM(any(ArrayList.class), any(Logger.class))).thenAnswer(answer);

    } else if (deviceAddresses.length == 1) {
      ArrayList<byte[]> ret = new ArrayList<byte[]>();
      ret.add(deviceAddresses[0].copyHexBytesTo(new byte[16], 0));
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, ret, firstWriteCTM);
      when(mockHA7S.cmdSearchROM(any(ArrayList.class), any(Logger.class))).thenAnswer(answer);

    } else if (deviceAddresses.length == 2) {
      ArrayList<byte[]> ret = new ArrayList<byte[]>();
      ret.add(deviceAddresses[0].copyHexBytesTo(new byte[16], 0));
      ret.add(deviceAddresses[1].copyHexBytesTo(new byte[16], 0));
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, ret, firstWriteCTM);
      when(mockHA7S.cmdSearchROM(any(ArrayList.class), any(Logger.class))).thenAnswer(answer);

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

  @Test(dataProvider = "createPositiveCases")
  public void testByAlarmPositive(DSAddress[] deviceAddresses) {

    Assert.assertNotNull(deviceAddresses);
    Assert.assertTrue(deviceAddresses.length <= 2);

    HA7S mockHA7S = mock(HA7S.class);
    when(mockHA7S.getIsStarted()).thenReturn(true);

    SearchBusCmd cmd = new HA7SSearchBusCmd(mockHA7S, true);
    Assert.assertTrue(cmd.isByAlarm());
    Assert.assertFalse(cmd.isByFamilyCode());

    long firstWriteCTM = 5L;
    long secondWriteCTM = 6L;

    if (deviceAddresses.length == 0) {
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, null, firstWriteCTM);
      when(mockHA7S.cmdConditionalSearch(any(ArrayList.class), any(Logger.class)))
          .thenAnswer(answer);

    } else if (deviceAddresses.length == 1) {
      ArrayList<byte[]> ret = new ArrayList<byte[]>();
      ret.add(deviceAddresses[0].copyHexBytesTo(new byte[16], 0));
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, ret, firstWriteCTM);
      when(mockHA7S.cmdConditionalSearch(any(ArrayList.class), any(Logger.class)))
          .thenAnswer(answer);

    } else if (deviceAddresses.length == 2) {
      ArrayList<byte[]> ret = new ArrayList<byte[]>();
      ret.add(deviceAddresses[0].copyHexBytesTo(new byte[16], 0));
      ret.add(deviceAddresses[1].copyHexBytesTo(new byte[16], 0));
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, ret, firstWriteCTM);
      when(mockHA7S.cmdConditionalSearch(any(ArrayList.class), any(Logger.class)))
          .thenAnswer(answer);

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

  @Test(dataProvider = "createPositiveCases")
  public void testByFamilyCodePositive(DSAddress[] deviceAddresses) {

    Assert.assertNotNull(deviceAddresses);
    Assert.assertTrue(deviceAddresses.length <= 2);

    HA7S mockHA7S = mock(HA7S.class);
    when(mockHA7S.getIsStarted()).thenReturn(true);

    short familyCode = 54;
    SearchBusCmd cmd = new HA7SSearchBusCmd(mockHA7S, familyCode);
    Assert.assertFalse(cmd.isByAlarm());
    Assert.assertTrue(cmd.isByFamilyCode());
    Assert.assertEquals(((HA7SSearchBusCmd) cmd).getFamilyCode(), familyCode);

    long firstWriteCTM = 5L;
    long secondWriteCTM = 6L;

    if (deviceAddresses.length == 0) {
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, null, firstWriteCTM);
      when(mockHA7S.cmdFamilySearch(any(byte.class), any(ArrayList.class), any(Logger.class)))
          .thenAnswer(answer);

    } else if (deviceAddresses.length == 1) {
      ArrayList<byte[]> ret = new ArrayList<byte[]>();
      ret.add(deviceAddresses[0].copyHexBytesTo(new byte[16], 0));
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, ret, firstWriteCTM);
      when(mockHA7S.cmdFamilySearch(any(byte.class), any(ArrayList.class), any(Logger.class)))
          .thenAnswer(answer);

    } else if (deviceAddresses.length == 2) {
      ArrayList<byte[]> ret = new ArrayList<byte[]>();
      ret.add(deviceAddresses[0].copyHexBytesTo(new byte[16], 0));
      ret.add(deviceAddresses[1].copyHexBytesTo(new byte[16], 0));
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, ret, firstWriteCTM);
      when(mockHA7S.cmdFamilySearch(any(byte.class), any(ArrayList.class), any(Logger.class)))
          .thenAnswer(answer);
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

  @Test(dataProvider = "createNegativeCases")
  public void testSearchNegative(HA7SSerial.ReadResult readResult, byte[] rbuf_data,
      SearchBusCmd.Result expectedResult) {

    HA7SSerial mockSerial = mock(HA7SSerial.class);
    when(mockSerial.start(any(Logger.class))).thenReturn(HA7SSerial.StartResult.SR_Success);

    HA7S mockHA7S = new HA7S(mockSerial);
    Assert.assertFalse(mockHA7S.getIsStarted());

    // for starting
    when(mockSerial
        .writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
        .thenReturn(new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Success));

    StartBusCmd startCmd = mockHA7S.queryStartBusCmd();
    Assert.assertEquals(startCmd.execute(), StartBusCmd.Result.started);

    // searching
    SearchBusCmd searchCmd = new HA7SSearchBusCmd(mockHA7S);
    Assert.assertFalse(searchCmd.isByAlarm());
    Assert.assertFalse(searchCmd.isByFamilyCode());

    when(mockSerial
        .writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
        .thenAnswer(HA7SSerialTest.makeWriteReadTilCRResult(
            new HA7SSerialTest.TestHA7SSerial(readResult, rbuf_data, 5L)));

    Assert.assertEquals(searchCmd.execute(), expectedResult);
  }

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
