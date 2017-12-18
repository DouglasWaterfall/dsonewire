package waterfall.onewire.busmasters.HA7S;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.testng.Assert;
import org.testng.annotations.Test;
import waterfall.onewire.busmaster.BusMaster.StartBusResult;
import waterfall.onewire.busmaster.Logger;

/**
 * Created by dwaterfa on 12/17/17.
 */
public class StopBusTests extends TestBase {

  @Test
  public void testStopCmd() {
    HA7S ha7s = new HA7S(new HA7SSerialDummy("port"));

    Assert.assertFalse(ha7s.getIsStarted());

    try {
      Assert.assertEquals(ha7s.startBus(null).getCode(), StartBusResult.Code.started);
    } catch (Exception e) {
      Assert.fail("Unexpected exception:" + e);
    }

    Assert.assertTrue(ha7s.getIsStarted());

    try {
      ha7s.stopBus(null);
      Assert.assertFalse(ha7s.getIsStarted());
    } catch (Exception e) {
      Assert.fail("Unexpected exception:" + e);
    }

    try {
      ha7s.stopBus(null);
      Assert.assertFalse(ha7s.getIsStarted());
    } catch (Exception e) {
      Assert.fail("Unexpected exception:" + e);
    }
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
    Assert.assertTrue(ha7s.getIsStarted());
    ha7s.stopBus(null);
    Assert.assertFalse(ha7s.getIsStarted());
  }

}
