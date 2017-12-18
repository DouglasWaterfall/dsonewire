package waterfall.onewire.busmasters.HA7S;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import waterfall.onewire.busmaster.BusMaster.StartBusResult;
import waterfall.onewire.busmaster.BusMaster.StartBusResult.Code;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmasters.HA7S.HA7SSerial.ReadResult;

/**
 * Created by dwaterfa on 12/17/17.
 */
public class StartBusTests extends TestBase {

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
        {HA7SSerial.StartResult.SR_NoPortName, Code.deviceFault},
        {HA7SSerial.StartResult.SR_Busy, Code.deviceFault},
        {HA7SSerial.StartResult.SR_Error, Code.deviceFault}
    };
  }

  @Test(dataProvider = "createSerialPortReadTilCRNegativeCases")
  public void testSerialPortWriteReadTilCRNegative(HA7SSerial.ReadResult first_serialReadResult,
      byte[] first_rbuf_data,
      HA7SSerial.ReadResult second_serialReadResult, byte[] second_rbuf_data,
      StartBusResult.Code expectedStartResultCode) {

    HA7SSerial mockSerial = mock(HA7SSerial.class);
    when(mockSerial.start(any(Logger.class))).thenReturn(HA7SSerial.StartResult.SR_Success);

    Answer<ReadResult> firstAnswer = makeAnswerForReadResult(first_serialReadResult,
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
            Code.busFault},
        // generic read timeout
        {new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadTimeout), null,
            null, null,
            Code.busFault},
        // generic error
        {new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Error), null,
            null, null,
            Code.busFault},
        // reset failure followed by error
        {new HA7SSerial.ReadResult(1, 5, 7),
            new byte[]{0x7},
            new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Error), null,
            Code.busFault},
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
    Assert.assertEquals(ha7s.startBus(null).getCode(), StartBusResult.Code.started);
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
      Assert.fail("Unexpected exception:" + e);
    }

    Assert.assertTrue(ha7s.getIsStarted());

    try {
      Assert.assertEquals(ha7s.startBus(null).getCode(), StartBusResult.Code.started);
    } catch (Exception e) {
      Assert.fail("Unexpected exception:" + e);
    }
  }


}
