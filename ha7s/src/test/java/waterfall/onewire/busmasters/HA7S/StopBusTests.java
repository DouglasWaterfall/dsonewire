package waterfall.onewire.busmasters.HA7S;

import org.testng.Assert;
import org.testng.annotations.Test;
import waterfall.onewire.busmasters.HA7S.HA7SSerial.StartResult;

/**
 * Created by dwaterfa on 12/17/17.
 */
public class StopBusTests extends TestBase {

  @Test
  public void testStopCmd() {
    HA7SSerial serial = new HA7SSerialDummy("port");
    Assert.assertEquals(serial.start(null), StartResult.SR_Success);
    HA7S ha7s = new HA7S(serial);

    String name = ha7s.getName();

    try {
      ha7s.stopBus(null);
      Assert.assertNotEquals(ha7s.getName(), name);

      // 2nd call makes no difference
      ha7s.stopBus(null);
    } catch (Exception e) {
      Assert.fail("Unexpected exception:" + e);
    }
  }

}
