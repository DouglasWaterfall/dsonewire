package waterfall.onewire.busmasters.HA7S;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.testng.Assert;
import org.testng.annotations.Test;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.ConvertTCmd;
import waterfall.onewire.busmaster.Logger;

/**
 * Created by dwaterfa on 12/17/17.
 */
public class ConvertTCmdTests extends TestBase {

  @Test
  public void testConvertTCmd() {

    DSAddress dsAddr = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);
    final long cmdWriteCTM = 5;
    final long cmdReadCRCTM = 6;

    HA7SSerial mockSerial = getStartedMockSerial();
    HA7S ha7s = new HA7S(mockSerial);

    ConvertTCmd cmd = ha7s.queryConvertTCmd(dsAddr);
    Assert.assertNotNull(cmd);

    try {
      when(mockSerial.writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
          .thenAnswer(makeAnswerForAddress(3L, 4L))
          .thenAnswer(makeAnswerForReadResult(
              new HA7SSerial.ReadResult(2, cmdWriteCTM, cmdReadCRCTM), new byte[]{'4', '4'}))
          .thenAnswer(makeAnswerForReadResult(
              new HA7SSerial.ReadResult(1, 5L, 6L), new byte[]{'0'}))
          .thenAnswer(makeAnswerForReadZero(7L, 8L));

      Assert.assertEquals(cmd.execute(), ConvertTCmd.Result.success);
      Assert.assertEquals(cmd.getResultWriteCTM(), cmdWriteCTM);

      /*
      >> Some devices are not handling the read bit call correctly, not sure why
      >> Disable this for now.
      when(mockSerial.writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
          .thenAnswer(makeAnswerForAddress(3L, 4L))
          .thenAnswer(makeAnswerForReadResult(
              new HA7SSerial.ReadResult(2, cmdWriteCTM, cmdReadCRCTM), new byte[]{'4', '4'}))
          .thenAnswer(makeAnswerForReadResult(
              new HA7SSerial.ReadResult(1, 5L, 6L), new byte[]{'1'}))
          .thenAnswer(makeAnswerForReadZero(7L, 8L));

      Assert.assertEquals(cmd.execute(), Result.deviceNotFound);
      */

      ha7s.stopBus(null);

      ConvertTCmd.Result result = cmd.execute();
      Assert.assertEquals(result, ConvertTCmd.Result.busFault);

    } catch (Exception e) {
      Assert.fail("Unexpected exception:" + e);
    }

    // bus failure?
  }

}
