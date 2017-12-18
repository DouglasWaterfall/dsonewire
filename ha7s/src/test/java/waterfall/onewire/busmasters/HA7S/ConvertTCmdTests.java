package waterfall.onewire.busmasters.HA7S;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.testng.Assert;
import org.testng.annotations.Test;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.BusMaster.StartBusResult;
import waterfall.onewire.busmaster.ConvertTCmd;
import waterfall.onewire.busmaster.ConvertTCmd.Result;
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

    HA7SSerial mockSerial = getReadyToStartMockSerial();
    HA7S ha7s = new HA7S(mockSerial);

    ConvertTCmd cmd = ha7s.queryConvertTCmd(dsAddr);
    Assert.assertNotNull(cmd);

    try {
      ConvertTCmd.Result result = cmd.execute();
      Assert.assertEquals(result, ConvertTCmd.Result.busFault);
    } catch (Exception e) {
      Assert.fail("Unexpected exception:" + e);
    }

    try {
      Assert.assertEquals(ha7s.startBus(null).getCode(), StartBusResult.Code.started);
      Assert.assertTrue(ha7s.getIsStarted());
    } catch (Exception e) {
      Assert.fail("Unexpected exception:" + e);
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
      Assert.fail("Unexpected exception:" + e);
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
      Assert.fail("Unexpected exception:" + e);
    }

    // bus failure?
  }

}
