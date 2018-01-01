package waterfall.onewire.busmasters.HA7S;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.testng.Assert;
import org.testng.annotations.Test;
import waterfall.onewire.Convert;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.WriteScratchpadCmd;
import waterfall.onewire.device.DS18B20Scratchpad;

/**
 * Created by dwaterfa on 12/17/17.
 */
public class WriteScratchpadCmdTests extends TestBase {

  @Test
  public void testWriteScratchpadCmd() {

    DSAddress dsAddr = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);
    final long cmdWriteCTM = 5;
    final long cmdReadCRCTM = 6;
    final DS18B20Scratchpad data = new DS18B20Scratchpad().setResolution((byte)0);
    Assert.assertTrue(data.checkValid());
    Assert.assertEquals(data.getResolution(), (byte)0);
    final byte[] writeData = data.getWriteScratchpadBytes();
    final byte[] read_data = new byte[] {'4', 'E',
        Convert.fourBitsToHex(writeData[0] >> 4),
        Convert.fourBitsToHex(writeData[0] & 0xf),
        Convert.fourBitsToHex(writeData[1] >> 4),
        Convert.fourBitsToHex(writeData[1] & 0xf),
        Convert.fourBitsToHex(writeData[2] >> 4),
        Convert.fourBitsToHex(writeData[2] & 0xf) };

    HA7SSerial mockSerial = getStartedMockSerial();
    HA7S ha7s = new HA7S(mockSerial);

    WriteScratchpadCmd cmd = ha7s.queryWriteScratchpadCmd(dsAddr, writeData);
    Assert.assertNotNull(cmd);

    // 0x4B 0x46 0x1F   4E4B461F

    try {

      when(mockSerial.writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
          .thenAnswer(makeAnswerForAddress(3L, 4L))
          .thenAnswer(makeAnswerForReadResult(
              new HA7SSerial.ReadResult(read_data.length, cmdWriteCTM, cmdReadCRCTM), read_data));

      Assert.assertEquals(cmd.execute(), WriteScratchpadCmd.Result.success);
      Assert.assertEquals(cmd.getResultWriteCTM(), cmdWriteCTM);

      ha7s.stopBus(null);

      WriteScratchpadCmd.Result result = cmd.execute();
      Assert.assertEquals(result, WriteScratchpadCmd.Result.busFault);

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
