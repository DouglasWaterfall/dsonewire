package waterfall.onewire.busmasters.HA7S;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.ReadPowerSupplyCmd;

/**
 * Created by dwaterfa on 12/17/17.
 */
public class ReadPowerSupplyCmdTests extends TestBase {

  @Test(dataProvider = "createReadPowerSupplyCmdPositiveCases")
  public void testReadPowerSupplyCmdPositive(byte[] rbuf_data, boolean expectedIsParasitic) {
    Assert.assertNotNull(rbuf_data);
    Assert.assertEquals(rbuf_data.length, 4);

    DSAddress dsAddr = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);
    final long cmdWriteCTM = 5;
    final long cmdReadCRCTM = 6;

    HA7SSerial mockSerial = getStartedMockSerial();
    HA7S ha7s = new HA7S(mockSerial);

    ReadPowerSupplyCmd cmd = ha7s.queryReadPowerSupplyCmd(dsAddr);
    Assert.assertNotNull(cmd);

    try {

      when(mockSerial.writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
          .thenAnswer(makeAnswerForAddress(3L, 4L))
          .thenAnswer(makeAnswerForReadResult(
              new HA7SSerial.ReadResult(4, cmdWriteCTM, cmdReadCRCTM), rbuf_data))
          .thenAnswer(makeAnswerForReadZero(78, 6L));

      Assert.assertEquals(cmd.execute(), ReadPowerSupplyCmd.Result.success);
      Assert.assertEquals(cmd.getResultWriteCTM(), cmdWriteCTM);
      Assert.assertEquals(cmd.getResultIsParasitic(), expectedIsParasitic);

      ha7s.stopBus(null);

      ReadPowerSupplyCmd.Result result = cmd.execute();
      Assert.assertEquals(result, ReadPowerSupplyCmd.Result.busFault);

    } catch (Exception e) {
      Assert.fail("Unexpected exception:" + e);
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

}
