package waterfall.onewire.busmasters.HA7S;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.SearchBusCmd;
import waterfall.onewire.busmaster.StartBusCmd;


/**
 * Created by dwaterfa on 8/6/17.
 */
public class HA7SSearchBusCmdTest {

  @Test(dataProvider = "createPositiveCases")
  public void testNotByAlarmPositive(String[] deviceAddresses) {

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
      when(mockHA7S.cmdSearchROM(any(byte[].class), any(Logger.class))).thenAnswer(answer);
    } else if (deviceAddresses.length == 1) {
      byte[] addr1 = deviceAddresses[0].getBytes();
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, addr1, firstWriteCTM);
      when(mockHA7S.cmdSearchROM(any(byte[].class), any(Logger.class))).thenAnswer(answer);

      Answer<HA7S.cmdReturn> answer2 = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, null, secondWriteCTM);
      when(mockHA7S.cmdNextSearchROM(any(byte[].class), any(Logger.class))).thenAnswer(answer2);
    } else if (deviceAddresses.length == 2) {
      byte[] addr1 = deviceAddresses[0].getBytes();
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, addr1, firstWriteCTM);
      when(mockHA7S.cmdSearchROM(any(byte[].class), any(Logger.class))).thenAnswer(answer);

      byte[] addr2 = deviceAddresses[1].getBytes();
      Answer<HA7S.cmdReturn> answer2 = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, addr2, secondWriteCTM);
      Answer<HA7S.cmdReturn> answer3 = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, null, secondWriteCTM);

      when(mockHA7S.cmdNextSearchROM(any(byte[].class), any(Logger.class)))
          .thenAnswer(answer2)
          .thenAnswer(answer3);
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
  public void testByAlarmPositive(String[] deviceAddresses) {

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
      when(mockHA7S.cmdConditionalSearch(any(byte[].class), any(Logger.class))).thenAnswer(answer);
    } else if (deviceAddresses.length == 1) {
      byte[] addr1 = deviceAddresses[0].getBytes();
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, addr1, firstWriteCTM);
      when(mockHA7S.cmdConditionalSearch(any(byte[].class), any(Logger.class))).thenAnswer(answer);

      Answer<HA7S.cmdReturn> answer2 = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, null, secondWriteCTM);
      when(mockHA7S.cmdNextConditionalSearch(any(byte[].class), any(Logger.class)))
          .thenAnswer(answer2);
    } else if (deviceAddresses.length == 2) {
      byte[] addr1 = deviceAddresses[0].getBytes();
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, addr1, firstWriteCTM);
      when(mockHA7S.cmdConditionalSearch(any(byte[].class), any(Logger.class))).thenAnswer(answer);

      byte[] addr2 = deviceAddresses[1].getBytes();
      Answer<HA7S.cmdReturn> answer2 = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, addr2, secondWriteCTM);
      Answer<HA7S.cmdReturn> answer3 = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, null, secondWriteCTM);

      when(mockHA7S.cmdNextConditionalSearch(any(byte[].class), any(Logger.class)))
          .thenAnswer(answer2)
          .thenAnswer(answer3);
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
  public void testByFamilyCodePositive(String[] deviceAddresses) {

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
      when(mockHA7S.cmdFamilySearch(any(byte.class), any(byte[].class), any(Logger.class)))
          .thenAnswer(answer);
    } else if (deviceAddresses.length == 1) {
      byte[] addr1 = deviceAddresses[0].getBytes();
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, addr1, firstWriteCTM);
      when(mockHA7S.cmdFamilySearch(any(byte.class), any(byte[].class), any(Logger.class)))
          .thenAnswer(answer);

      Answer<HA7S.cmdReturn> answer2 = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, null, secondWriteCTM);
      when(mockHA7S.cmdNextFamilySearch(any(byte[].class), any(Logger.class))).thenAnswer(answer2);
    } else if (deviceAddresses.length == 2) {
      byte[] addr1 = deviceAddresses[0].getBytes();
      Answer<HA7S.cmdReturn> answer = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, addr1, firstWriteCTM);
      when(mockHA7S.cmdFamilySearch(any(byte.class), any(byte[].class), any(Logger.class)))
          .thenAnswer(answer);

      byte[] addr2 = deviceAddresses[1].getBytes();
      Answer<HA7S.cmdReturn> answer2 = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, addr2, secondWriteCTM);
      Answer<HA7S.cmdReturn> answer3 = HA7STest
          .makeSearchCmdReturnAnswer(HA7S.cmdResult.Success, null, secondWriteCTM);

      when(mockHA7S.cmdNextFamilySearch(any(byte[].class), any(Logger.class)))
          .thenAnswer(answer2)
          .thenAnswer(answer3);
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
    String one = "0123456789ABCDEF";
    String two = "FEDCBA9876543210";

    String[] nothingFound = {};
    String[] oneFound = {one};
    String[] twoFound = {one, two};

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
        .writeReadTilCR(any(byte[].class), any(byte[].class), any(Long.TYPE), any(Logger.class)))
        .thenReturn(new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Success));

    StartBusCmd startCmd = mockHA7S.queryStartBusCmd();
    Assert.assertEquals(startCmd.execute(), StartBusCmd.Result.started);

    // searching
    SearchBusCmd searchCmd = new HA7SSearchBusCmd(mockHA7S);
    Assert.assertFalse(searchCmd.isByAlarm());
    Assert.assertFalse(searchCmd.isByFamilyCode());

    when(mockSerial
        .writeReadTilCR(any(byte[].class), any(byte[].class), any(Long.TYPE), any(Logger.class)))
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
