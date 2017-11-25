package waterfall.onewire.busmasters.HA7S;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.StartBusCmd;
import waterfall.onewire.busmaster.StopBusCmd;


/**
 * Created by dwaterfa on 8/6/17.
 */
public class HA7SStopBusCmdTest {

  @Test
  public void testNotStarted() {

    HA7SSerial mockSerial = mock(HA7SSerial.class);
    HA7S ha7s = new HA7S(mockSerial);
    StopBusCmd stopCmd = ha7s.queryStopBusCmd();
    Assert.assertEquals(stopCmd.execute(), StopBusCmd.Result.not_started);
  }

  @Test(dataProvider = "createSerialPortStopNegativeCases")
  public void testSerialPortStopNegative(HA7SSerial.StopResult serialStopResult,
      StopBusCmd.Result expectedResult) {

    HA7SSerial mockSerial = mock(HA7SSerial.class);
    when(mockSerial.start(any(Logger.class))).thenReturn(HA7SSerial.StartResult.SR_Success);

    HA7SSerial.ReadResult readResult = new HA7SSerial.ReadResult(0, 1, 2);

    when(mockSerial
        .writeReadTilCR(any(byte[].class), any(byte[].class), any(Long.TYPE), any(Logger.class)))
        .thenReturn(readResult);

    HA7S ha7s = new HA7S(mockSerial);
    Assert.assertFalse(ha7s.getIsStarted());

    StartBusCmd startCmd = ha7s.queryStartBusCmd();
    Assert.assertEquals(startCmd.execute(), StartBusCmd.Result.started);

    when(mockSerial.stop(any(Logger.class))).thenReturn(serialStopResult);

    StopBusCmd stopCmd = ha7s.queryStopBusCmd();
    Assert.assertEquals(stopCmd.execute(), expectedResult);
  }

  @DataProvider
  public Object[][] createSerialPortStopNegativeCases() {
    return new Object[][]{
        {HA7SSerial.StopResult.SR_Error, StopBusCmd.Result.communication_error}
    };
  }

  @Test
  public void testSerialPortStopPositive() {

    HA7SSerial mockSerial = mock(HA7SSerial.class);
    when(mockSerial.start(any(Logger.class))).thenReturn(HA7SSerial.StartResult.SR_Success);

    HA7SSerial.ReadResult readResult = new HA7SSerial.ReadResult(0, 1, 2);

    when(mockSerial
        .writeReadTilCR(any(byte[].class), any(byte[].class), any(Long.TYPE), any(Logger.class)))
        .thenReturn(readResult);

    HA7S ha7s = new HA7S(mockSerial);
    Assert.assertFalse(ha7s.getIsStarted());

    StartBusCmd startCmd = ha7s.queryStartBusCmd();
    Assert.assertEquals(startCmd.execute(), StartBusCmd.Result.started);

    when(mockSerial.stop(any(Logger.class))).thenReturn(HA7SSerial.StopResult.SR_Success);

    StopBusCmd stopCmd = ha7s.queryStopBusCmd();
    Assert.assertEquals(stopCmd.execute(), StopBusCmd.Result.stopped);
  }

}
