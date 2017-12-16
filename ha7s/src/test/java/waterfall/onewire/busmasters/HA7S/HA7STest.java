package waterfall.onewire.busmasters.HA7S;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import waterfall.onewire.Convert;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.BusMaster;
import waterfall.onewire.busmaster.BusMaster.StartBusResult;
import waterfall.onewire.busmaster.BusMaster.StopBusResult;
import waterfall.onewire.busmaster.BusMaster.StopBusResult.Code;
import waterfall.onewire.busmaster.ConvertTCmd;
import waterfall.onewire.busmaster.ConvertTCmd.Result;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.NotifySearchBusCmdResult;
import waterfall.onewire.busmaster.ReadPowerSupplyCmd;
import waterfall.onewire.busmaster.ReadScratchpadCmd;
import waterfall.onewire.busmaster.SearchBusCmd;
import waterfall.onewire.busmasters.HA7S.HA7SSerial.ReadResult;
import waterfall.onewire.busmasters.HA7S.part.DS18B20;

/**
 * Created by dwaterfa on 1/29/17.
 *
 * Here is helpful site with which to fabricate addresses:
 * http://www.datastat.com/sysadminjournal/maximcrc.cgi
 *
 * The trick is when calculating the crc8 you need to do it in reverse order, that is from right
 * to left of the address as we maintain it. If you do the 14 bytes what will be left will be a
 * two byte CRC value. This is what can then be the FIRST two nibbles of the full address.
 *
 * "EE0000065BC0AE28",
 * "090000065BD53528",
 * "5F0000065CCD1A28",
 * "260000065BE22D28",
 * "7C0000063BB13028",
 * "5A0000063B7AF528",
 * "AA0000063BF51928",
 * "390000063B759F28",
 * "7F0000063BA12F28"
 */
public class HA7STest {

  public static final long periodNegativeOneMSec = -1;
  public static final long periodZeroMSec = 0;
  public static final long periodOneMSec = 1;
  public static final long period250MSec = 250;
  public static final long period500MSec = 500;
  public static final long period750MSec = 750;

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConstructorNullSerial() {
    new HA7S(null);
  }

  @Test
  public void testConstructorDefaults() {
    String portName = "foo";

    HA7SSerial mockSerial = mock(HA7SSerial.class);
    when(mockSerial.getPortName()).thenReturn(portName);

    HA7S ha7s = new HA7S(mockSerial);
    Assert.assertFalse(ha7s.getIsStarted());
    Assert.assertTrue(ha7s.getCurrentTimeMillis() > 0);
    Assert.assertEquals(ha7s.getName(), "HA7S on " + portName);
  }

  @Test(dataProvider = "createSerialPortStartNegativeCases")
  public void testSerialPortStartNegative(HA7SSerial.StartResult serialStartResult,
      StartBusResult.Code expectedStartResultCode) {

    HA7SSerial mockSerial = mock(HA7SSerial.class);
    when(mockSerial.start(any(Logger.class))).thenReturn(serialStartResult);

    HA7S ha7s = new HA7S(mockSerial);
    Assert.assertFalse(ha7s.getIsStarted());

    Assert.assertEquals(ha7s.startBus(null).getCode(), expectedStartResultCode);
  }

  @DataProvider
  public Object[][] createSerialPortStartNegativeCases() {
    return new Object[][]{
        {HA7SSerial.StartResult.SR_NoPortName, StartBusResult.Code.communication_error},
        {HA7SSerial.StartResult.SR_Busy, StartBusResult.Code.communication_error},
        {HA7SSerial.StartResult.SR_Error, StartBusResult.Code.communication_error}
    };
  }

  @Test(dataProvider = "createSerialPortReadTilCRNegativeCases")
  public void testSerialPortWriteReadTilCRNegative(HA7SSerial.ReadResult first_serialReadResult,
      byte[] first_rbuf_data,
      HA7SSerial.ReadResult second_serialReadResult, byte[] second_rbuf_data,
      StartBusResult.Code expectedStartResultCode) {

    HA7SSerial mockSerial = mock(HA7SSerial.class);
    when(mockSerial.start(any(Logger.class))).thenReturn(HA7SSerial.StartResult.SR_Success);

    Answer<HA7SSerial.ReadResult> firstAnswer = makeAnswerForReadResult(first_serialReadResult,
        first_rbuf_data);

    if (second_serialReadResult != null) {
      Answer<HA7SSerial.ReadResult> secondAnswer = makeAnswerForReadResult(second_serialReadResult,
          second_rbuf_data);
      when(mockSerial
          .writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
          .thenAnswer(firstAnswer)
          .thenAnswer(secondAnswer);
    } else {
      when(mockSerial
          .writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
          .thenAnswer(firstAnswer);
    }

    when(mockSerial.stop(any(Logger.class))).thenReturn(HA7SSerial.StopResult.SR_Success);

    HA7S ha7s = new HA7S(mockSerial);
    Assert.assertFalse(ha7s.getIsStarted());

    Assert.assertEquals(ha7s.startBus(null).getCode(), expectedStartResultCode);
  }

  @DataProvider
  public Object[][] createSerialPortReadTilCRNegativeCases() {

    return new Object[][]{
        // generic read overrun
        {new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadOverrun), null,
            null, null,
            StartBusResult.Code.communication_error},
        // generic read timeout
        {new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadTimeout), null,
            null, null,
            StartBusResult.Code.communication_error},
        // generic error
        {new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Error), null,
            null, null,
            StartBusResult.Code.communication_error},
        // reset failure followed by error
        {new HA7SSerial.ReadResult(1, 5, 7),
            new byte[]{0x7},
            new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Error), null,
            StartBusResult.Code.communication_error},
    };
  }

  @Test
  public void testSerialPortStartPositive() {

    HA7SSerial mockSerial = mock(HA7SSerial.class);
    when(mockSerial.start(any(Logger.class))).thenReturn(HA7SSerial.StartResult.SR_Success);

    HA7SSerial.ReadResult readResult = new HA7SSerial.ReadResult(0, 1, 3);

    when(mockSerial
        .writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
        .thenReturn(readResult);

    HA7S ha7s = new HA7S(mockSerial);
    Assert.assertFalse(ha7s.getIsStarted());

    Assert.assertEquals(ha7s.startBus(null).getCode(), StartBusResult.Code.started);
    Assert.assertEquals(ha7s.startBus(null).getCode(), StartBusResult.Code.already_started);
  }

  @Test
  public void testConstructorDummySerial() {
    HA7S ha7s = null;

    try {
      ha7s = new HA7S(new HA7SSerialDummy("port"));
    } catch (Exception e) {
      Assert.fail("Exception not expected");
    }

    Assert.assertNotNull(ha7s);
    Assert.assertNotNull(ha7s.getName());
    Assert.assertTrue(ha7s.getName().startsWith("HA7S on "));

    long ctm = ha7s.getCurrentTimeMillis();
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Assert.fail("Unexpected exception");
    }
    Assert.assertTrue(ctm < ha7s.getCurrentTimeMillis());

    Assert.assertFalse(ha7s.getIsStarted());
  }

  //
  // Start
  //
  @Test
  public void testStartCmd() {
    HA7S ha7s = new HA7S(new HA7SSerialDummy("port"));

    Assert.assertFalse(ha7s.getIsStarted());

    try {
      Assert.assertEquals(ha7s.startBus(null).getCode(), StartBusResult.Code.started);
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    Assert.assertTrue(ha7s.getIsStarted());

    try {
      Assert.assertEquals(ha7s.startBus(null).getCode(), StartBusResult.Code.already_started);
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }
  }

  //
  // Stop
  //
  @Test
  public void testStopCmd() {
    HA7S ha7s = new HA7S(new HA7SSerialDummy("port"));

    Assert.assertFalse(ha7s.getIsStarted());

    try {
      Assert.assertEquals(ha7s.stopBus(null).getCode(), StopBusResult.Code.not_started);
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    try {
      Assert.assertEquals(ha7s.startBus(null).getCode(), StartBusResult.Code.started);
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    Assert.assertTrue(ha7s.getIsStarted());

    try {
      Assert.assertEquals(ha7s.stopBus(null).getCode(), StopBusResult.Code.stopped);
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    try {
      Assert.assertEquals(ha7s.stopBus(null).getCode(), StopBusResult.Code.not_started);
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }
  }

  @Test
  public void testNotStarted() {

    HA7SSerial mockSerial = mock(HA7SSerial.class);

    HA7S ha7s = new HA7S(mockSerial);
    Assert.assertEquals(ha7s.stopBus(null).getCode(), Code.not_started);
  }

  @Test(dataProvider = "createSerialPortStopNegativeCases")
  public void testSerialPortStopNegative(HA7SSerial.StopResult serialStopResult,
      StopBusResult.Code expectedStopResultCode) {

    HA7SSerial mockSerial = mock(HA7SSerial.class);
    when(mockSerial.start(any(Logger.class))).thenReturn(HA7SSerial.StartResult.SR_Success);
    HA7SSerial.ReadResult readResult = new HA7SSerial.ReadResult(0, 1, 2);
    when(mockSerial.writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
        .thenReturn(readResult);
    when(mockSerial.stop(any(Logger.class))).thenReturn(serialStopResult);

    HA7S ha7s = new HA7S(mockSerial);
    Assert.assertFalse(ha7s.getIsStarted());
    Assert.assertEquals(ha7s.startBus(null).getCode(), StartBusResult.Code.started);

    Assert.assertEquals(ha7s.stopBus(null).getCode(), expectedStopResultCode);
  }

  @DataProvider
  public Object[][] createSerialPortStopNegativeCases() {
    return new Object[][]{
        {HA7SSerial.StopResult.SR_Error, StopBusResult.Code.communication_error}
    };
  }

  @Test
  public void testSerialPortStopPositive() {

    HA7SSerial mockSerial = mock(HA7SSerial.class);
    when(mockSerial.start(any(Logger.class))).thenReturn(HA7SSerial.StartResult.SR_Success);
    HA7SSerial.ReadResult readResult = new HA7SSerial.ReadResult(0, 1, 2);
    when(mockSerial.writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
        .thenReturn(readResult);
    when(mockSerial.stop(any(Logger.class))).thenReturn(HA7SSerial.StopResult.SR_Success);

    HA7S ha7s = new HA7S(mockSerial);
    Assert.assertFalse(ha7s.getIsStarted());
    Assert.assertEquals(ha7s.startBus(null).getCode(), StartBusResult.Code.started);

    Assert.assertEquals(ha7s.stopBus(null).getCode(), StopBusResult.Code.stopped);
  }

  //
  // ReadPowerSupply
  //
  @Test(dataProvider = "createReadPowerSupplyCmdCases")
  public void testReadPowerSupplyCmdPositive(byte[] rbuf_data, boolean expectedIsParasitic) {
    Assert.assertNotNull(rbuf_data);
    Assert.assertEquals(rbuf_data.length, 4);

    DSAddress dsAddr = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);
    final long cmdWriteCTM = 5;
    final long cmdReadCRCTM = 6;

    HA7SSerial mockSerial = getReadyToStartMockSerial();
    HA7S ha7s = new HA7S(mockSerial);

    ReadPowerSupplyCmd cmd = ha7s.queryReadPowerSupplyCmd(dsAddr);
    Assert.assertNotNull(cmd);

    try {
      ReadPowerSupplyCmd.Result result = cmd.execute();
      Assert.assertEquals(result, ReadPowerSupplyCmd.Result.busFault);
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    try {
      Assert.assertEquals(ha7s.startBus(null).getCode(), StartBusResult.Code.started);
      Assert.assertTrue(ha7s.getIsStarted());
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    when(mockSerial.writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
        .thenAnswer(makeAnswerForAddress(3L, 4L))
        .thenAnswer(makeAnswerForReadResult(
            new HA7SSerial.ReadResult(4, cmdWriteCTM, cmdReadCRCTM), rbuf_data));

    try {
      Assert.assertEquals(cmd.execute(), ReadPowerSupplyCmd.Result.success);
      Assert.assertEquals(cmd.getResultWriteCTM(), cmdWriteCTM);
      Assert.assertEquals(cmd.getResultIsParasitic(), expectedIsParasitic);
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    // bus failure?
  }

  @DataProvider
  public Object[][] createReadPowerSupplyCmdPositiveCases() {
    return new Object[][]{
        {new byte[]{'B', '4', '0', '0'}, true},
        {new byte[]{'B', '4', 'F', 'F'}, false}
    };
  }

  /*
  @DataProvider
  public Object[][] createReadPowerSupplyCmdWriteBlockCases() {

    byte[] parasitic = {'0', '0', '0', '0'};

    byte[] not_parasitic = {'0', '0', '0', '1'};

    return new Object[][]{
        {HA7S.cmdResult.NotStarted, null, -1, ReadPowerSupplyCmd.Result.communication_error, null},
        {HA7S.cmdResult.DeviceNotFound, null, -1, ReadPowerSupplyCmd.Result.communication_error,
            null},
        {HA7S.cmdResult.ReadTimeout, null, -1, ReadPowerSupplyCmd.Result.communication_error, null},
        {HA7S.cmdResult.ReadOverrun, null, -1, ReadPowerSupplyCmd.Result.communication_error, null},
        {HA7S.cmdResult.ReadError, null, -1, ReadPowerSupplyCmd.Result.communication_error, null},
        {HA7S.cmdResult.ReadError, null, -1, ReadPowerSupplyCmd.Result.communication_error, null},

        // no data returned
        {HA7S.cmdResult.Success, null, 1, ReadPowerSupplyCmd.Result.communication_error, null},
        // no data returned
        {HA7S.cmdResult.Success, new byte[0], 1, ReadPowerSupplyCmd.Result.communication_error,
            null},
        // parasitic data returned
        {HA7S.cmdResult.Success, parasitic, 4, ReadPowerSupplyCmd.Result.success,
            new Boolean(true)},
        // non-parasitic data returned
        {HA7S.cmdResult.Success, not_parasitic, 4, ReadPowerSupplyCmd.Result.success,
            new Boolean(false)}
    };
  }
  */

  //
  // ConvertT
  //
  @Test
  public void testConvertTCmd() {

    DSAddress dsAddr = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);
    final long cmdWriteCTM = 5;
    final long cmdReadCRCTM = 6;

    HA7SSerial mockSerial = getReadyToStartMockSerial();
    HA7S ha7s = new HA7S(mockSerial);

    ConvertTCmd cmd = ha7s.queryConvertTCmd(dsAddr);
    Assert.assertNotNull(cmd);

    try {
      ConvertTCmd.Result result = cmd.execute();
      Assert.assertEquals(result, ConvertTCmd.Result.busFault);
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    try {
      Assert.assertEquals(ha7s.startBus(null).getCode(), StartBusResult.Code.started);
      Assert.assertTrue(ha7s.getIsStarted());
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    when(mockSerial.writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
        .thenAnswer(makeAnswerForAddress(3L, 4L))
        .thenAnswer(makeAnswerForReadResult(
            new HA7SSerial.ReadResult(2, cmdWriteCTM, cmdReadCRCTM), new byte[]{'4', '4'}))
        .thenAnswer(makeAnswerForReadResult(
            new HA7SSerial.ReadResult(1, 5L, 6L), new byte[]{'0'}))
        .thenAnswer(makeAnswerForReadZero(7L, 8L));

    try {
      Assert.assertEquals(cmd.execute(), ConvertTCmd.Result.success);
      Assert.assertEquals(cmd.getResultWriteCTM(), cmdWriteCTM);
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    when(mockSerial.writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
        .thenAnswer(makeAnswerForAddress(3L, 4L))
        .thenAnswer(makeAnswerForReadResult(
            new HA7SSerial.ReadResult(2, cmdWriteCTM, cmdReadCRCTM), new byte[]{'4', '4'}))
        .thenAnswer(makeAnswerForReadResult(
            new HA7SSerial.ReadResult(1, 5L, 6L), new byte[]{'1'}))
        .thenAnswer(makeAnswerForReadZero(7L, 8L));

    try {
      Assert.assertEquals(cmd.execute(), Result.deviceNotFound);
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    // bus failure?
  }

  //
  // ReadScratchpadCmd
  //
  @Test
  public void testReadScratchpadCmd() {

    DSAddress dsAddr = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);
    final long cmdWriteCTM = 5;
    final long cmdReadCRCTM = 6;
    byte[] read_data = new byte[]{'B', 'E',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', // 5
        '0', 'A', 'F', '0', '1', '2', '3', '4'};         // 4

    HA7SSerial mockSerial = getReadyToStartMockSerial();
    HA7S ha7s = new HA7S(mockSerial);

    ReadScratchpadCmd cmd = ha7s.queryReadScratchpadCmd(dsAddr, (short) 9);
    Assert.assertNotNull(cmd);

    try {
      ReadScratchpadCmd.Result result = cmd.execute();
      Assert.assertEquals(result, ReadScratchpadCmd.Result.busFault);
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    try {
      Assert.assertEquals(ha7s.startBus(null).getCode(), StartBusResult.Code.started);
      Assert.assertTrue(ha7s.getIsStarted());
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    when(mockSerial.writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
        .thenAnswer(makeAnswerForAddress(3L, 4L))
        .thenAnswer(makeAnswerForReadResult(
            new HA7SSerial.ReadResult(read_data.length, cmdWriteCTM, cmdReadCRCTM), read_data));

    try {
      Assert.assertEquals(cmd.execute(), ReadScratchpadCmd.Result.success);
      Assert.assertEquals(cmd.getResultWriteCTM(), cmdWriteCTM);
      Assert.assertTrue(Arrays.equals(cmd.getResultHexData(),
          Arrays.copyOfRange(read_data, 2, read_data.length)));
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    // bus fault
    // CRC error (but only if CRC is selected?)
    // All FFs (device missing) - this is device specific too.
    //  Having each application be responsible for retries seems a little heavy handled. It seems
    //  that they could pass in some sort of policy object which would tell what to do.
    //  check CRC, FFF means device missing, etc.
    //  What about ConverT - it seems like our first read bit is our indication of alive?

    // Reset after ConvertT?
  }

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

    HA7SSerial mockSerial = getReadyToStartMockSerial();
    HA7S ha7s = new HA7S(mockSerial);

    SearchBusCmd searchBusCmd = ha7s.querySearchBusCmd();
    Assert.assertNotNull(searchBusCmd);
    Assert.assertFalse(searchBusCmd.isByAlarm());
    Assert.assertFalse(searchBusCmd.isByFamilyCode());
    Assert.assertNull(searchBusCmd.getResult());

    try {
      SearchBusCmd.Result result = searchBusCmd.execute();
      Assert.assertEquals(result, SearchBusCmd.Result.bus_not_started);
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    try {
      Assert.assertEquals(ha7s.startBus(null).getCode(), StartBusResult.Code.started);
      Assert.assertTrue(ha7s.getIsStarted());
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    when(mockSerial.writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
        .thenAnswer(makeAnswerForSearch(new byte[] { 'S' }, dev_A.copyHexBytesTo(new byte[16], 0), cmdFirstWriteCTM,
            cmdFirstReadCRCTM))
        .thenAnswer(makeAnswerForSearch(new byte[] { 's' }, dev_B.copyHexBytesTo(new byte[16], 0), 5L, 6L))
        .thenAnswer(makeAnswerForSearch(new byte[] { 's' }, dev_C.copyHexBytesTo(new byte[16], 0), 7L, 8L))
        .thenAnswer(makeAnswerForSearch(new byte[] { 's' }, null, 9L, 10L));

    try {
      SearchBusCmd.Result result = searchBusCmd.execute();
      Assert.assertEquals(result, SearchBusCmd.Result.success);
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    Assert.assertEquals(searchBusCmd.getResult(), SearchBusCmd.Result.success);
    Assert.assertNotNull(searchBusCmd.getResultList());
    Assert.assertEquals(searchBusCmd.getResultList().size(), 3);
    Assert.assertTrue(searchBusCmd.getResultListCRC32() != 0);
    Assert.assertEquals(searchBusCmd.getResultWriteCTM(), cmdFirstWriteCTM);
    Assert.assertTrue(searchBusCmd.getResultList().contains(dev_A) &&
        searchBusCmd.getResultList().contains(dev_B) &&
        searchBusCmd.getResultList().contains(dev_C));
  }

  @Test
  public void testAlarmSearchCmd() {

    final long cmdFirstWriteCTM = 3;
    final long cmdFirstReadCRCTM = 4;
    final DSAddress dev_A = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);
    final DSAddress dev_B = DSAddress.fromUncheckedHex(DSAddress._090000065BD53528);
    final DSAddress dev_C = DSAddress.fromUncheckedHex(DSAddress._5F0000065CCD1A28);

    HA7SSerial mockSerial = getReadyToStartMockSerial();
    HA7S ha7s = new HA7S(mockSerial);

    SearchBusCmd searchBusCmd = ha7s.querySearchBusByAlarmCmd();
    Assert.assertNotNull(searchBusCmd);
    Assert.assertTrue(searchBusCmd.isByAlarm());
    Assert.assertFalse(searchBusCmd.isByFamilyCode());
    Assert.assertNull(searchBusCmd.getResult());

    try {
      SearchBusCmd.Result result = searchBusCmd.execute();
      Assert.assertEquals(result, SearchBusCmd.Result.bus_not_started);
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    try {
      Assert.assertEquals(ha7s.startBus(null).getCode(), StartBusResult.Code.started);
      Assert.assertTrue(ha7s.getIsStarted());
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    when(mockSerial.writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
        .thenAnswer(makeAnswerForSearch(new byte[] { 'C' }, dev_A.copyHexBytesTo(new byte[16], 0), cmdFirstWriteCTM,
            cmdFirstReadCRCTM))
        .thenAnswer(makeAnswerForSearch(new byte[] { 'c' }, dev_B.copyHexBytesTo(new byte[16], 0), 5L, 6L))
        .thenAnswer(makeAnswerForSearch(new byte[] { 'c' }, dev_C.copyHexBytesTo(new byte[16], 0), 7L, 8L))
        .thenAnswer(makeAnswerForSearch(new byte[] { 'c' }, null, 9L, 10L));

    try {
      SearchBusCmd.Result result = searchBusCmd.execute();
      Assert.assertEquals(result, SearchBusCmd.Result.success);
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    Assert.assertEquals(searchBusCmd.getResult(), SearchBusCmd.Result.success);
    Assert.assertNotNull(searchBusCmd.getResultList());
    Assert.assertEquals(searchBusCmd.getResultList().size(), 3);
    Assert.assertTrue(searchBusCmd.getResultListCRC32() != 0);
    Assert.assertEquals(searchBusCmd.getResultWriteCTM(), cmdFirstWriteCTM);
    Assert.assertTrue(searchBusCmd.getResultList().contains(dev_A) &&
        searchBusCmd.getResultList().contains(dev_B) &&
        searchBusCmd.getResultList().contains(dev_C));
  }

  @Test
  public void testSearchFamilyCmd() {

    final long cmdFirstWriteCTM = 3;
    final long cmdFirstReadCRCTM = 4;
    final DSAddress dev_A = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);
    final DSAddress dev_B = DSAddress.fromUncheckedHex(DSAddress._090000065BD53528);
    final DSAddress dev_C = DSAddress.fromUncheckedHex(DSAddress._5F0000065CCD1A28);

    HA7SSerial mockSerial = getReadyToStartMockSerial();
    HA7S ha7s = new HA7S(mockSerial);

    short familyCode = 0x28;

    SearchBusCmd searchBusCmd = ha7s.querySearchBusByFamilyCmd(familyCode);
    Assert.assertNotNull(searchBusCmd);
    Assert.assertFalse(searchBusCmd.isByAlarm());
    Assert.assertTrue(searchBusCmd.isByFamilyCode());
    Assert.assertNull(searchBusCmd.getResult());

    try {
      SearchBusCmd.Result result = searchBusCmd.execute();
      Assert.assertEquals(result, SearchBusCmd.Result.bus_not_started);
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    try {
      Assert.assertEquals(ha7s.startBus(null).getCode(), StartBusResult.Code.started);
      Assert.assertTrue(ha7s.getIsStarted());
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    when(mockSerial.writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
        .thenAnswer(makeAnswerForSearch(new byte[] { 'F', Convert.fourBitsToHex(familyCode >> 4),
            Convert.fourBitsToHex(familyCode & 0xf) },
            dev_A.copyHexBytesTo(new byte[16], 0), cmdFirstWriteCTM, cmdFirstReadCRCTM))
        .thenAnswer(makeAnswerForSearch(new byte[] { 'f' }, dev_B.copyHexBytesTo(new byte[16], 0), 5L, 6L))
        .thenAnswer(makeAnswerForSearch(new byte[] { 'f' }, dev_C.copyHexBytesTo(new byte[16], 0), 7L, 8L))
        .thenAnswer(makeAnswerForSearch(new byte[] { 'f' }, null, 9L, 10L));

    try {
      SearchBusCmd.Result result = searchBusCmd.execute();
      Assert.assertEquals(result, SearchBusCmd.Result.success);
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    Assert.assertEquals(searchBusCmd.getResult(), SearchBusCmd.Result.success);
    Assert.assertNotNull(searchBusCmd.getResultList());
    Assert.assertEquals(searchBusCmd.getResultList().size(), 3);
    Assert.assertTrue(searchBusCmd.getResultListCRC32() != 0);
    Assert.assertEquals(searchBusCmd.getResultWriteCTM(), cmdFirstWriteCTM);
    Assert.assertTrue(searchBusCmd.getResultList().contains(dev_A) &&
        searchBusCmd.getResultList().contains(dev_B) &&
        searchBusCmd.getResultList().contains(dev_C));
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

  @Test
  public void testScheduleNotifySearchBusCmd() {
    HA7SSerialDummy serialDummy = new HA7SSerialDummy("port");

    HA7S ha7s = new HA7S(serialDummy);

    final boolean notByAlarm = false;

    BusMaster.CancelScheduledNotifySearchBusCmdResult cancelResult = ha7s
        .cancelScheduledNotifySearchBusCmd(null, notByAlarm);
    Assert.assertEquals(cancelResult,
        BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_BusMasterNotStarted);

    BusMaster.ScheduleNotifySearchBusCmdResult result = ha7s
        .scheduleNotifySearchBusCmd(null, notByAlarm, periodOneMSec);
    Assert.assertEquals(result,
        BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_BusMasterNotStarted);

    try {
      Assert.assertEquals(ha7s.startBus(null).getCode(), StartBusResult.Code.started);
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    Assert.assertTrue(ha7s.getIsStarted());

    cancelResult = ha7s.cancelScheduledNotifySearchBusCmd(null, notByAlarm);
    Assert.assertEquals(cancelResult,
        BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_NotifyObjNotAlreadyScheduled);

    result = ha7s.scheduleNotifySearchBusCmd(null, notByAlarm, periodOneMSec);
    Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_NotifyObjNull);

    result = ha7s.scheduleNotifySearchBusCmd(new myNotifySearchBusCmdResult(), notByAlarm,
        periodNegativeOneMSec);
    Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_MinPeriodInvalid);

    result = ha7s
        .scheduleNotifySearchBusCmd(new myNotifySearchBusCmdResult(), notByAlarm, periodZeroMSec);
    Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_MinPeriodInvalid);

    myNotifySearchBusCmdResult callback = new myNotifySearchBusCmdResult();
    Assert.assertNull(callback.getData());

    result = ha7s.scheduleNotifySearchBusCmd(callback, notByAlarm, period250MSec);
    Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success);

    result = ha7s.scheduleNotifySearchBusCmd(callback, notByAlarm, period250MSec);
    Assert.assertEquals(result,
        BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_NotifyObjAlreadyScheduled);

    boolean waitResult = callback.wait500MSecForNotifyChange(-1);
    Assert.assertTrue(waitResult);

    myNotifySearchBusCmdResult.Data notifyData = callback.getData();
    Assert.assertNotNull(notifyData);
    Assert.assertTrue(notifyData.notifyCount > -1);
    Assert.assertEquals(notifyData.bm, ha7s);
    Assert.assertEquals((boolean) notifyData.byAlarm, notByAlarm);
    Assert.assertNotNull(notifyData.searchResultData);
    Assert.assertNotNull(notifyData.searchResultData.getList());
    Assert.assertEquals(notifyData.searchResultData.getList().size(), 0);

    // do it again - this is expected to FAIL the wait because we are not re-notified of the same
    // result based on the CRC of the last notify.
    waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
    Assert.assertFalse(waitResult);

    // add a device to the bus
    final DSAddress dev_A = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);

    serialDummy.addDevice(new DS18B20(dev_A));

    for (int i = 0; i < 2; i++) {
      waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
      if (!waitResult) {
        // We got some old data before new device was recognized, go wait again until the next notify
        Assert.assertEquals(i, 0);
      } else {
        break;
      }
    }
    notifyData = callback.getData();
    Assert.assertNotNull(notifyData);
    Assert.assertEquals(notifyData.bm, ha7s);
    Assert.assertEquals((boolean) notifyData.byAlarm, notByAlarm);
    Assert.assertNotNull(notifyData.searchResultData);
    Assert.assertNotNull(notifyData.searchResultData.getList());
    Assert.assertEquals(notifyData.searchResultData.getList().size(), 1);
    Assert.assertTrue(notifyData.searchResultData.getList().contains(dev_A));

    // add more devices to the bus
    final DSAddress dev_B = DSAddress.fromUncheckedHex(DSAddress._090000065BD53528);
    final DSAddress dev_C = DSAddress.fromUncheckedHex(DSAddress._5F0000065CCD1A28);

    serialDummy.addDevice(new DS18B20(dev_B));
    serialDummy.addDevice(new DS18B20(dev_C));

    for (int i = 0; i < 2; i++) {
      waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
      if (!waitResult) {
        // We got some old data before new device was recognized, go wait again until the next notify
        Assert.assertEquals(i, 0);
      } else {
        break;
      }
    }
    notifyData = callback.getData();
    Assert.assertNotNull(notifyData);
    Assert.assertEquals(notifyData.bm, ha7s);
    Assert.assertEquals((boolean) notifyData.byAlarm, notByAlarm);
    Assert.assertNotNull(notifyData.searchResultData);
    Assert.assertNotNull(notifyData.searchResultData.getList());
    Assert.assertEquals(notifyData.searchResultData.getList().size(), 3);
    Assert.assertTrue(notifyData.searchResultData.getList().contains(dev_A) &&
        notifyData.searchResultData.getList().contains(dev_B) &&
        notifyData.searchResultData.getList().contains(dev_C));

    // bit of a race condition, but I think we can cancel before the next callback at 250ms
    cancelResult = ha7s.cancelScheduledNotifySearchBusCmd(callback, notByAlarm);
    Assert.assertEquals(cancelResult,
        BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_Success);

    for (int i = 0; i < 2; i++) {
      waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
      if (waitResult) {
        Assert.fail("We should not get any more notifications");
        break;
      }
    }
    Assert.assertEquals(callback.getData().notifyCount, notifyData.notifyCount);
  }

  @Test
  public void testScheduleNotifySearchBusCmdTiming() {
    internal_testScheduleNotifySearchBusCmdTiming(false);
  }

  @Test
  public void testScheduleNotifySearchBusByAlarmCmd() {
    HA7SSerialDummy serialDummy = new HA7SSerialDummy("port");

    HA7S ha7s = new HA7S(serialDummy);

    final boolean byAlarm = true;

    BusMaster.CancelScheduledNotifySearchBusCmdResult cancelResult = ha7s
        .cancelScheduledNotifySearchBusCmd(null, byAlarm);
    Assert.assertEquals(cancelResult,
        BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_BusMasterNotStarted);

    BusMaster.ScheduleNotifySearchBusCmdResult result = ha7s
        .scheduleNotifySearchBusCmd(null, byAlarm, periodOneMSec);
    Assert.assertEquals(result,
        BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_BusMasterNotStarted);

    try {
      Assert.assertEquals(ha7s.startBus(null).getCode(), StartBusResult.Code.started);
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    Assert.assertTrue(ha7s.getIsStarted());

    cancelResult = ha7s.cancelScheduledNotifySearchBusCmd(null, byAlarm);
    Assert.assertEquals(cancelResult,
        BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_NotifyObjNotAlreadyScheduled);

    result = ha7s.scheduleNotifySearchBusCmd(null, byAlarm, periodOneMSec);
    Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_NotifyObjNull);

    result = ha7s.scheduleNotifySearchBusCmd(new myNotifySearchBusCmdResult(), byAlarm,
        periodNegativeOneMSec);
    Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_MinPeriodInvalid);

    result = ha7s
        .scheduleNotifySearchBusCmd(new myNotifySearchBusCmdResult(), byAlarm, periodZeroMSec);
    Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_MinPeriodInvalid);

    myNotifySearchBusCmdResult callback = new myNotifySearchBusCmdResult();
    Assert.assertNull(callback.getData());

    result = ha7s.scheduleNotifySearchBusCmd(callback, byAlarm, period250MSec);
    Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success);

    result = ha7s.scheduleNotifySearchBusCmd(callback, byAlarm, period250MSec);
    Assert.assertEquals(result,
        BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_NotifyObjAlreadyScheduled);

    boolean waitResult = callback.wait500MSecForNotifyChange(-1);
    Assert.assertTrue(waitResult);

    myNotifySearchBusCmdResult.Data notifyData = callback.getData();
    Assert.assertNotNull(notifyData);
    Assert.assertTrue(notifyData.notifyCount > -1);
    Assert.assertEquals(notifyData.bm, ha7s);
    Assert.assertEquals((boolean) notifyData.byAlarm, byAlarm);
    Assert.assertNotNull(notifyData.searchResultData);
    Assert.assertNotNull(notifyData.searchResultData.getList());
    Assert.assertEquals(notifyData.searchResultData.getList().size(), 0);

    // do it again - this is expected to FAIL the wait because we are not re-notified of the same
    // result based on the CRC of the last notify.
    waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
    Assert.assertFalse(waitResult);

    // add a device to the bus
    final DSAddress dev_A = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);

    serialDummy.addDevice(new DS18B20(dev_A).setHasAlarm(true));

    for (int i = 0; i < 2; i++) {
      waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
      if (!waitResult) {
        // We got some old data before new device was recognized, go wait again until the next notify
        Assert.assertEquals(i, 0);
      } else {
        break;
      }
    }
    notifyData = callback.getData();
    Assert.assertNotNull(notifyData);
    Assert.assertEquals(notifyData.bm, ha7s);
    Assert.assertEquals((boolean) notifyData.byAlarm, byAlarm);
    Assert.assertNotNull(notifyData.searchResultData);
    Assert.assertNotNull(notifyData.searchResultData.getList());
    Assert.assertEquals(notifyData.searchResultData.getList().size(), 1);
    Assert.assertTrue(notifyData.searchResultData.getList().contains(dev_A));

    // add more devices to the bus
    final DSAddress dev_B = DSAddress.fromUncheckedHex(DSAddress._090000065BD53528);
    final DSAddress dev_C = DSAddress.fromUncheckedHex(DSAddress._5F0000065CCD1A28);

    serialDummy.addDevice(new DS18B20(dev_B));
    serialDummy.addDevice(new DS18B20(dev_C).setHasAlarm(true));

    for (int i = 0; i < 2; i++) {
      waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
      if (!waitResult) {
        // We got some old data before new device was recognized, go wait again until the next notify
        Assert.assertEquals(i, 0);
      } else {
        break;
      }
    }
    notifyData = callback.getData();
    Assert.assertNotNull(notifyData);
    Assert.assertEquals(notifyData.bm, ha7s);
    Assert.assertEquals((boolean) notifyData.byAlarm, byAlarm);
    Assert.assertNotNull(notifyData.searchResultData);
    Assert.assertNotNull(notifyData.searchResultData.getList());
    Assert.assertEquals(notifyData.searchResultData.getList().size(), 2);
    Assert.assertTrue(notifyData.searchResultData.getList().contains(dev_A) &&
        notifyData.searchResultData.getList().contains(dev_C));

    // bit of a race condition, but I think we can cancel before the next callback at 250ms
    cancelResult = ha7s.cancelScheduledNotifySearchBusCmd(callback, byAlarm);
    Assert.assertEquals(cancelResult,
        BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_Success);

    for (int i = 0; i < 2; i++) {
      waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
      if (waitResult) {
        Assert.fail("We should not get any more notifications");
        break;
      }
    }
    Assert.assertEquals(callback.getData().notifyCount, notifyData.notifyCount);
  }

  @Test
  public void testScheduleNotifySearchBusByAlarmCmdTiming() {
    internal_testScheduleNotifySearchBusCmdTiming(true);
  }

  private void internal_testScheduleNotifySearchBusCmdTiming(boolean byAlarm) {
    HA7SSerialDummy serialDummy = new HA7SSerialDummy("port");

    HA7S ha7s = new HA7S(serialDummy);

    try {
      Assert.assertEquals(ha7s.startBus(null).getCode(), StartBusResult.Code.started);
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    Assert.assertTrue(ha7s.getIsStarted());

    myNotifySearchBusCmdResult callback = new myNotifySearchBusCmdResult();
    Assert.assertNull(callback.getData());

    final DSAddress dev_A = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);

    // this will ensure that we get a callback on every search by adding/removing a device to the search list
    callback.setAddRemoveEveryNotify(serialDummy, dev_A, byAlarm);

    BusMaster.ScheduleNotifySearchBusCmdResult scheduleResult = ha7s
        .scheduleNotifySearchBusCmd(callback, byAlarm, period250MSec);
    Assert.assertEquals(scheduleResult, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success);

    // wait for something to change
    for (int i = 0; i < 2; i++) {
      boolean waitResult = callback.wait500MSecForNotifyChange(-1);
      if (!waitResult) {
        // We got some old data before new device was recognized, go wait again until the next notify
        Assert.assertEquals(i, 0);
      } else {
        break;
      }
    }
    myNotifySearchBusCmdResult.Data notifyData = callback.getData();
    Assert.assertNotNull(notifyData);
    Assert.assertNotEquals(notifyData.notifyCount, -1);
    Assert.assertEquals((boolean) notifyData.byAlarm, byAlarm);

    // wait for something to change again
    for (int i = 0; i < 2; i++) {
      boolean waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
      if (!waitResult) {
        // We got some old data before new device was recognized, go wait again until the next notify
        Assert.assertEquals(i, 0);
      } else {
        break;
      }
    }
    myNotifySearchBusCmdResult.Data notifyData2 = callback.getData();
    Assert.assertNotNull(notifyData2);
    Assert.assertEquals((boolean) notifyData2.byAlarm, byAlarm);
    Assert.assertEquals((notifyData.notifyCount + 1), notifyData2.notifyCount);
    long delta = (notifyData2.searchResultData.getWriteCTM() - notifyData.searchResultData
        .getWriteCTM());
    Assert.assertTrue(delta >= period250MSec);
    Assert.assertTrue(delta < period500MSec);

    // let's change the rate to something slower
    BusMaster.UpdateScheduledNotifySearchBusCmdResult updateResult = ha7s
        .updateScheduledNotifySearchBusCmd(callback, byAlarm, period500MSec);
    Assert.assertEquals(updateResult,
        BusMaster.UpdateScheduledNotifySearchBusCmdResult.USNSBC_Success);

    // wait for something to change again
    for (int i = 0; i < 2; i++) {
      boolean waitResult = callback.wait1000MSecForNotifyChange(notifyData2.notifyCount);
      if (!waitResult) {
        // We got some old data before new device was recognized, go wait again until the next notify
        Assert.assertEquals(i, 0);
      } else {
        break;
      }
    }
    myNotifySearchBusCmdResult.Data notifyData3 = callback.getData();
    Assert.assertNotNull(notifyData3);
    Assert.assertEquals((boolean) notifyData3.byAlarm, byAlarm);
    Assert.assertEquals((notifyData2.notifyCount + 1), notifyData3.notifyCount);
    delta = (notifyData3.searchResultData.getWriteCTM() - notifyData2.searchResultData
        .getWriteCTM());
    Assert.assertTrue(delta >= period500MSec);
    Assert.assertTrue(delta < period750MSec);

    // let's change the rate to something faster by adding another object waiting. We do not need
    // to wait on it since its new rate will be effective for all waiters.
    myNotifySearchBusCmdResult callback2 = new myNotifySearchBusCmdResult();
    Assert.assertNull(callback2.getData());
    scheduleResult = ha7s.scheduleNotifySearchBusCmd(callback2, byAlarm, period250MSec);
    Assert.assertEquals(scheduleResult, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success);

    // wait for something to change again
    for (int i = 0; i < 2; i++) {
      boolean waitResult = callback.wait1000MSecForNotifyChange(notifyData3.notifyCount);
      if (!waitResult) {
        // We got some old data before new device was recognized, go wait again until the next notify
        Assert.assertEquals(i, 0);
      } else {
        break;
      }
    }
    myNotifySearchBusCmdResult.Data notifyData4 = callback.getData();
    Assert.assertNotNull(notifyData4);
    Assert.assertEquals((boolean) notifyData4.byAlarm, byAlarm);
    Assert.assertEquals((notifyData3.notifyCount + 1), notifyData4.notifyCount);
    delta = (notifyData4.searchResultData.getWriteCTM() - notifyData3.searchResultData
        .getWriteCTM());
    Assert.assertTrue(delta >= period250MSec);
    Assert.assertTrue(delta < period500MSec);

    // cancel the new rate which will slow things back to the 500 rate
    BusMaster.CancelScheduledNotifySearchBusCmdResult cancelResult = ha7s
        .cancelScheduledNotifySearchBusCmd(callback2, byAlarm);
    Assert.assertEquals(cancelResult,
        BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_Success);

    // wait for something to change again
    for (int i = 0; i < 2; i++) {
      boolean waitResult = callback.wait1000MSecForNotifyChange(notifyData4.notifyCount);
      if (!waitResult) {
        // We got some old data before new device was recognized, go wait again until the next notify
        Assert.assertEquals(i, 0);
      } else {
        break;
      }
    }
    myNotifySearchBusCmdResult.Data notifyData5 = callback.getData();
    Assert.assertNotNull(notifyData5);
    Assert.assertEquals((boolean) notifyData5.byAlarm, byAlarm);
    Assert.assertEquals((notifyData4.notifyCount + 1), notifyData5.notifyCount);
    delta = (notifyData5.searchResultData.getWriteCTM() - notifyData4.searchResultData
        .getWriteCTM());
    Assert.assertTrue(delta >= period500MSec);
    Assert.assertTrue(delta < period750MSec);

    // Stop the busmaster to cancel everything.
    try {
      Assert.assertEquals(ha7s.stopBus(null).getCode(), StopBusResult.Code.stopped);
    } catch (Exception e) {
      Assert.fail("Unexpected exception");
    }

    Assert.assertFalse(ha7s.getIsStarted());

    // no more events expected
    for (int i = 0; i < 2; i++) {
      boolean waitResult = callback.wait500MSecForNotifyChange(notifyData5.notifyCount);
      if (waitResult) {
        Assert.fail("We should not get any more notifications");
        break;
      }
    }
    Assert.assertEquals(callback.getData().notifyCount, notifyData5.notifyCount);

    // cancelling our schedule will return a different error.
    cancelResult = ha7s.cancelScheduledNotifySearchBusCmd(callback, byAlarm);
    Assert.assertEquals(cancelResult,
        BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_BusMasterNotStarted);
  }

  public void testScheduleNotifySearchBusCmdTimer() {
    // We want to prove that the time period we are called back matches what we asked for
    // We want to test updating the search period rate
    // We want to test update ...

    // It would be useful to consider negative tests for the APIs, though there are not that many
  }

  //
  // Utils
  //
  private HA7SSerial getReadyToStartMockSerial() {
    HA7SSerial mockSerial = mock(HA7SSerial.class);

    when(mockSerial.start(any(Logger.class))).thenReturn(HA7SSerial.StartResult.SR_Success);

    // We are actually writing a Reset Bus 'R' cmd here
    when(mockSerial.writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
        .thenAnswer(makeAnswerForReadZero(1L, 2L));

    return mockSerial;
  }

  public static Answer<HA7S.cmdReturn> makeWriteBlockCmdReturnAnswer(HA7S.cmdResult result,
      byte[] rbuf_data, long writeCTM) {
    return new Answer<HA7S.cmdReturn>() {
      @Override
      public HA7S.cmdReturn answer(final InvocationOnMock invocation) {
        byte[] wbuf = (byte[]) (invocation.getArguments())[0];
        byte[] rbuf = (byte[]) (invocation.getArguments())[1];
        Logger logger = (Logger) (invocation.getArguments())[2];

        if (result == HA7S.cmdResult.Success) {
          int read_count = 0;
          if (rbuf_data != null) {
            read_count = rbuf_data.length;
            for (int i = 0; i < read_count; i++) {
              rbuf[i] = rbuf_data[i];
            }
          }
          return new HA7S.cmdReturn(read_count, writeCTM);
        }
        return new HA7S.cmdReturn(result);
      }
    };
  }

  public static Answer<HA7S.cmdReturn> makeSearchCmdReturnAnswer(HA7S.cmdResult result,
      ArrayList<byte[]> hexByteArrayList_data, long writeCTM) {
    return new Answer<HA7S.cmdReturn>() {
      @Override
      public HA7S.cmdReturn answer(final InvocationOnMock invocation) {
        Byte familyCode = null;
        ArrayList<byte[]> hexByteArrayList = null;
        Logger logger = null;
        if (invocation.getArguments().length == 2) {
          hexByteArrayList = (ArrayList<byte[]>) (invocation.getArguments())[0];
          logger = (Logger) (invocation.getArguments())[1];
        } else {
          familyCode = (Byte) (invocation.getArguments())[0];
          hexByteArrayList = (ArrayList<byte[]>) (invocation.getArguments())[1];
          logger = (Logger) (invocation.getArguments())[2];
        }

        if (result == HA7S.cmdResult.Success) {
          int read_count = 0;
          if (hexByteArrayList_data != null) {
            read_count = hexByteArrayList_data.size();
            for (int i = 0; i < read_count; i++) {
              hexByteArrayList.add(hexByteArrayList_data.get(i));
            }
          }
          return new HA7S.cmdReturn(read_count, writeCTM);
        }
        return new HA7S.cmdReturn(result);
      }
    };
  }

  private Answer<HA7SSerial.ReadResult> makeAnswerForReadResult(
      HA7SSerial.ReadResult serialReadResult, byte[] rbuf_data) {
    return new Answer<HA7SSerial.ReadResult>() {
      @Override
      public HA7SSerial.ReadResult answer(final InvocationOnMock invocation) {
        byte[] wbuf = (byte[]) (invocation.getArguments())[0];
        byte[] rbuf = (byte[]) (invocation.getArguments())[1];
        Logger logger = (Logger) (invocation.getArguments())[2];

        if (rbuf_data != null) {
          for (int i = 0; i < rbuf_data.length; i++) {
            rbuf[i] = rbuf_data[i];
          }
        }

        return serialReadResult;
      }
    };
  }

  private Answer<HA7SSerial.ReadResult> makeAnswerForReadZero(long writeCTM, long readCRCTM) {
    return makeAnswerForReadResult(new HA7SSerial.ReadResult(0, writeCTM, readCRCTM), null);
  }

  private Answer<HA7SSerial.ReadResult> makeAnswerForAddress(long writeCTM, long readCRCTM) {
    return new Answer<HA7SSerial.ReadResult>() {
      @Override
      public HA7SSerial.ReadResult answer(final InvocationOnMock invocation) {
        byte[] wbuf = (byte[]) (invocation.getArguments())[0];
        byte[] rbuf = (byte[]) (invocation.getArguments())[1];
        Logger logger = (Logger) (invocation.getArguments())[2];

        Assert.assertNotNull(wbuf);
        Assert.assertEquals(wbuf.length, 18);
        Assert.assertEquals(wbuf[0], 'A');
        Assert.assertEquals(wbuf.length, (1 + 16 + 1));
        Assert.assertNotNull(rbuf);
        Assert.assertTrue(rbuf.length >= 16);

        for (int i = 0; i < 16; i++) {
          rbuf[i] = wbuf[1 + i];
        }

        return new ReadResult(16, writeCTM, readCRCTM);
      }
    };
  }

  private Answer<HA7SSerial.ReadResult> makeAnswerForSearch(byte[] cmdHex, byte[] rbuf_data,
      long writeCTM, long readCRCTM) {
    return new Answer<HA7SSerial.ReadResult>() {
      @Override
      public HA7SSerial.ReadResult answer(final InvocationOnMock invocation) {
        byte[] wbuf = (byte[]) (invocation.getArguments())[0];
        byte[] rbuf = (byte[]) (invocation.getArguments())[1];
        Logger logger = (Logger) (invocation.getArguments())[2];

        Assert.assertNotNull(wbuf);
        Assert.assertTrue(wbuf.length >= 1);
        for (int i = 0; i < cmdHex.length; i++) {
          Assert.assertEquals(wbuf[i], cmdHex[i]);
        }
        Assert.assertNotNull(rbuf);
        Assert.assertTrue(rbuf.length >= 16);
        Assert
            .assertTrue((rbuf_data == null) || (rbuf_data.length == 0) || (rbuf_data.length == 16));

        int readCount = 0;
        if (rbuf_data != null) {
          for (int i = 0; i < rbuf_data.length; i++) {
            rbuf[i] = rbuf_data[i];
          }
          readCount = rbuf_data.length;
        }

        return new ReadResult(readCount, writeCTM, readCRCTM);
      }
    };
  }

  public class myNotifySearchBusCmdResult implements NotifySearchBusCmdResult {

    private int notifyCount;
    private Data data;
    private HA7SSerialDummy serialDummy;
    private DSAddress updateOnNotifyDev;
    private boolean updateOnNotifyAlarm;
    private boolean updateOnNotifyDoAdd;

    public myNotifySearchBusCmdResult() {
      notifyCount = -1;
      data = null;
      serialDummy = null;
      updateOnNotifyDev = null;
      updateOnNotifyAlarm = false;
      updateOnNotifyDoAdd = false;
    }

    public synchronized void notify(BusMaster bm, boolean byAlarm,
        SearchBusCmd.ResultData searchResultData) {
      data = new Data(++notifyCount, bm, byAlarm, searchResultData);

      if (serialDummy != null) {
        if (updateOnNotifyDoAdd) {
          serialDummy.addDevice(new DS18B20(updateOnNotifyDev).setHasAlarm(updateOnNotifyAlarm));
        } else {
          serialDummy.removeDevice(updateOnNotifyDev);
        }
        updateOnNotifyDoAdd = !updateOnNotifyDoAdd;
      }
    }

    public synchronized Data getData() {
      return data;
    }

    public boolean wait500MSecForNotifyChange(int afterNotifyCount) {
      return internalWaitForNotifyChange(5, 100, afterNotifyCount);
    }

    public boolean wait1000MSecForNotifyChange(int afterNotifyCount) {
      return internalWaitForNotifyChange(5, 200, afterNotifyCount);
    }

    private boolean internalWaitForNotifyChange(int count, long timeMSec, int afterNotifyCount) {
      for (int i = 0; i < count; i++) {
        Data t_data = getData();
        if ((t_data != null) && (t_data.notifyCount != afterNotifyCount)) {
          return true;
        }
        try {
          Thread.sleep(timeMSec);
        } catch (InterruptedException e) {

        }
      }
      return false;
    }

    public synchronized void setAddRemoveEveryNotify(HA7SSerialDummy serialDummy, DSAddress dev,
        boolean activeAlarm) {
      if (this.serialDummy != null) {
        throw new IllegalArgumentException("already registered");
      }

      this.serialDummy = serialDummy;
      updateOnNotifyDev = dev;
      updateOnNotifyAlarm = activeAlarm;
      updateOnNotifyDoAdd = true;
    }

    public synchronized void clearAddRemoveEveryNotify() {
      if (serialDummy != null) {
        if (!updateOnNotifyDoAdd) {
          serialDummy.removeDevice(updateOnNotifyDev);
        }
        serialDummy = null;
        updateOnNotifyDev = null;
      }
    }

    public class Data {

      public int notifyCount;
      public BusMaster bm;
      public Boolean byAlarm;
      public SearchBusCmd.ResultData searchResultData;

      public Data(int notifyCount, BusMaster bm, boolean byAlarm,
          SearchBusCmd.ResultData searchResultData) {
        this.notifyCount = notifyCount;
        this.bm = bm;
        this.byAlarm = byAlarm;
        this.searchResultData = searchResultData;
      }
    }
  }

}
