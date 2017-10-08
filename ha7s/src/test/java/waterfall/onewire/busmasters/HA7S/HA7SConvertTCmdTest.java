package waterfall.onewire.busmasters.HA7S;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.ConvertTCmd;
import waterfall.onewire.busmaster.Logger;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Created by dwaterfa on 8/6/17.
 */
public class HA7SConvertTCmdTest {

    @Test(dataProvider = "createAddressSelectNegativeCases")
    public void testAddressSelectNegative(HA7S.cmdResult cmdResult,
                                  ConvertTCmd.Result expectedResult) {

        HA7S mockHA7S = mock(HA7S.class);
        DSAddress dsAddr = new DSAddress("AAAAAAAAAA");

        HA7S.cmdReturn cmdReturn = new HA7S.cmdReturn(cmdResult);

        if (cmdResult == HA7S.cmdResult.NotStarted) {
            when(mockHA7S.getIsStarted()).thenReturn(false);
        }
        else {
            when(mockHA7S.getIsStarted()).thenReturn(true);
        }
        when(mockHA7S.cmdAddressSelect(any(DSAddress.class), any(Logger.class))).thenReturn(cmdReturn);

        HA7SConvertTCmd cmd = new HA7SConvertTCmd(mockHA7S, dsAddr);
        Assert.assertEquals(cmd.execute(), expectedResult);
    }

    @DataProvider
    public Object[][] createAddressSelectNegativeCases() {
        return new Object[][] {
                { HA7S.cmdResult.NotStarted, ConvertTCmd.Result.bus_not_started },
                { HA7S.cmdResult.DeviceNotFound, ConvertTCmd.Result.device_not_found },
                { HA7S.cmdResult.ReadTimeout, ConvertTCmd.Result.communication_error },
                { HA7S.cmdResult.ReadOverrun, ConvertTCmd.Result.communication_error },
                { HA7S.cmdResult.ReadError, ConvertTCmd.Result.communication_error }
        };

    }

    @Test(dataProvider = "createCmdWriteBlockCases")
    public void testCmdWriteBlock(HA7S.cmdReturn cmdReturn,
                                          ConvertTCmd.Result expectedResult) {
        HA7S mockHA7S = mock(HA7S.class);
        DSAddress dsAddr = new DSAddress("AAAAAAAAAA");

        when(mockHA7S.getIsStarted()).thenReturn(true);
        when(mockHA7S.cmdAddressSelect(any(DSAddress.class), any(Logger.class))).thenReturn(new HA7S.cmdReturn(HA7S.cmdResult.Success));
        when(mockHA7S.cmdWriteBlock(any(byte[].class), any(byte[].class), any(Logger.class))).thenReturn(cmdReturn);

        HA7SConvertTCmd cmd = new HA7SConvertTCmd(mockHA7S, dsAddr);
        Assert.assertEquals(cmd.execute(), expectedResult);
    }

    @DataProvider
    public Object[][] createCmdWriteBlockCases() {
        return new Object[][]{
                {new HA7S.cmdReturn(HA7S.cmdResult.NotStarted), ConvertTCmd.Result.communication_error},
                {new HA7S.cmdReturn(HA7S.cmdResult.DeviceNotFound), ConvertTCmd.Result.communication_error},
                {new HA7S.cmdReturn(HA7S.cmdResult.ReadTimeout), ConvertTCmd.Result.communication_error},
                {new HA7S.cmdReturn(HA7S.cmdResult.ReadOverrun), ConvertTCmd.Result.communication_error},
                {new HA7S.cmdReturn(HA7S.cmdResult.ReadError), ConvertTCmd.Result.communication_error},
                {new HA7S.cmdReturn(HA7S.cmdResult.ReadError), ConvertTCmd.Result.communication_error},
                {new HA7S.cmdReturn(1, 1), ConvertTCmd.Result.communication_error},
                {new HA7S.cmdReturn(2, 2), ConvertTCmd.Result.success}
        };
    }

}
