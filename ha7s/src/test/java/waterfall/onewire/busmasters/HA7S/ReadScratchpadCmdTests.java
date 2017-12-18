package waterfall.onewire.busmasters.HA7S;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.testng.Assert;
import org.testng.annotations.Test;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.BusMaster.StartBusResult;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.ReadScratchpadCmd;

/**
 * Created by dwaterfa on 12/17/17.
 */
public class ReadScratchpadCmdTests extends TestBase {

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
            new HA7SSerial.ReadResult(read_data.length, cmdWriteCTM, cmdReadCRCTM), read_data));

    try {
      Assert.assertEquals(cmd.execute(), ReadScratchpadCmd.Result.success);
      Assert.assertEquals(cmd.getResultWriteCTM(), cmdWriteCTM);
      Assert.assertTrue(Arrays.equals(cmd.getResultHexData(),
          Arrays.copyOfRange(read_data, 2, read_data.length)));
    } catch (Exception e) {
      Assert.fail("Unexpected exception:" + e);
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

}
