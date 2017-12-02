package waterfall.onewire.busmasters.HA7S;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.ReadPowerSupplyCmd;


/**
 * Created by dwaterfa on 8/6/17.
 */
public class HA7SReadPowerSupplyCmdTest {

  @Test(dataProvider = "createAddressSelectNegativeCases")
  public void testAddressSelectNegative(HA7S.cmdResult cmdResult,
      ReadPowerSupplyCmd.Result expectedResult) {

    HA7S mockHA7S = mock(HA7S.class);
    DSAddress dsAddr = DSAddress.fromUncheckedHex(DSAddress._5F0000065CCD1A28);

    HA7S.cmdReturn cmdReturn = new HA7S.cmdReturn(cmdResult);

    if (cmdResult == HA7S.cmdResult.NotStarted) {
      when(mockHA7S.getIsStarted()).thenReturn(false);
    } else {
      when(mockHA7S.getIsStarted()).thenReturn(true);
    }
    when(mockHA7S.cmdAddressSelect(any(DSAddress.class), any(Logger.class))).thenReturn(cmdReturn);

    HA7SReadPowerSupplyCmd cmd = new HA7SReadPowerSupplyCmd(mockHA7S, dsAddr);
    Assert.assertEquals(cmd.execute(), expectedResult);
  }

  @DataProvider
  public Object[][] createAddressSelectNegativeCases() {
    return new Object[][]{
        {HA7S.cmdResult.NotStarted, ReadPowerSupplyCmd.Result.bus_not_started},
        {HA7S.cmdResult.DeviceNotFound, ReadPowerSupplyCmd.Result.device_not_found},
        {HA7S.cmdResult.ReadTimeout, ReadPowerSupplyCmd.Result.communication_error},
        {HA7S.cmdResult.ReadOverrun, ReadPowerSupplyCmd.Result.communication_error},
        {HA7S.cmdResult.ReadError, ReadPowerSupplyCmd.Result.communication_error}
    };

  }

  @Test(dataProvider = "createCmdWriteBlockCases")
  public void testCmdWriteBlock(HA7S.cmdResult cmdResult,
      byte[] rbuf_data,
      long writeCTM,
      ReadPowerSupplyCmd.Result expectedResult,
      Boolean parasitic) {
    HA7S mockHA7S = mock(HA7S.class);
    DSAddress dsAddr = DSAddress.fromUncheckedHex(DSAddress._5F0000065CCD1A28);

    when(mockHA7S.getIsStarted()).thenReturn(true);
    when(mockHA7S.cmdAddressSelect(any(DSAddress.class), any(Logger.class)))
        .thenReturn(new HA7S.cmdReturn(HA7S.cmdResult.Success));

    Answer<HA7S.cmdReturn> answer = HA7STest
        .makeWriteBlockCmdReturnAnswer(cmdResult, rbuf_data, writeCTM);
    when(mockHA7S.cmdWriteBlock(any(byte[].class), any(byte[].class), any(Logger.class)))
        .thenAnswer(answer);

    HA7SReadPowerSupplyCmd cmd = new HA7SReadPowerSupplyCmd(mockHA7S, dsAddr);
    Assert.assertEquals(cmd.execute(), expectedResult);
    if (parasitic != null) {
      Assert.assertEquals(cmd.getResultIsParasitic(), parasitic.booleanValue());
    }
  }

  @DataProvider
  public Object[][] createCmdWriteBlockCases() {

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

}
