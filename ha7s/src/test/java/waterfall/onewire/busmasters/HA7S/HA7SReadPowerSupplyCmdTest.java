package waterfall.onewire.busmasters.HA7S;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.*;

import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.Mockito.*;


/**
 * Created by dwaterfa on 8/6/17.
 */
public class HA7SReadPowerSupplyCmdTest {

    @Test(dataProvider = "createAddressSelectNegativeCases")
    public void testAddressSelectNegative(HA7S.cmdResult cmdResult,
                                          ReadPowerSupplyCmd.Result expectedResult) {

        HA7S mockHA7S = mock(HA7S.class);
        DSAddress dsAddr = new DSAddress("AAAAAAAAAA");
        Logger.LogLevel logLevel = Logger.LogLevel.DeviceOnlyLevel();

        HA7S.cmdReturn cmdReturn = new HA7S.cmdReturn(cmdResult);

        if (cmdResult == HA7S.cmdResult.NotStarted) {
            when(mockHA7S.getIsStarted()).thenReturn(false);
        } else {
            when(mockHA7S.getIsStarted()).thenReturn(true);
        }
        when(mockHA7S.cmdAddressSelect(any(DSAddress.class), any(Logger.class))).thenReturn(cmdReturn);

        HA7SReadPowerSupplyCmd cmd = new HA7SReadPowerSupplyCmd(mockHA7S, dsAddr, logLevel);
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
    public void testCmdWriteBlock(HA7S.cmdReturn cmdReturn,
                                  ReadPowerSupplyCmd.Result expectedResult,
                                  byte[] rbuf_data,
                                  Boolean parasitic) {
        HA7S mockHA7S = mock(HA7S.class);
        DSAddress dsAddr = new DSAddress("AAAAAAAAAA");
        Logger.LogLevel logLevel = Logger.LogLevel.DeviceOnlyLevel();

        when(mockHA7S.getIsStarted()).thenReturn(true);
        when(mockHA7S.cmdAddressSelect(any(DSAddress.class), any(Logger.class))).thenReturn(new HA7S.cmdReturn(HA7S.cmdResult.Success));

        Answer<HA7S.cmdReturn> answer = new Answer<HA7S.cmdReturn>() {
            @Override
            public HA7S.cmdReturn answer(final InvocationOnMock invocation) {
                byte[] wbuf = (byte[]) (invocation.getArguments())[0];
                byte[] rbuf = (byte[]) (invocation.getArguments())[1];
                Logger logger = (Logger) (invocation.getArguments())[2];

                if (rbuf_data != null) {
                    for (int i = 0; i < rbuf_data.length; i++) {
                        rbuf[i] = rbuf_data[i];
                    }
                }

                return cmdReturn;
            }
        };

        when(mockHA7S.cmdWriteBlock(any(byte[].class), any(byte[].class), any(Logger.class))).thenAnswer(answer);

        HA7SReadPowerSupplyCmd cmd = new HA7SReadPowerSupplyCmd(mockHA7S, dsAddr, logLevel);
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
                {new HA7S.cmdReturn(HA7S.cmdResult.NotStarted), ReadPowerSupplyCmd.Result.communication_error, null, null},
                {new HA7S.cmdReturn(HA7S.cmdResult.DeviceNotFound), ReadPowerSupplyCmd.Result.communication_error, null, null},
                {new HA7S.cmdReturn(HA7S.cmdResult.ReadTimeout), ReadPowerSupplyCmd.Result.communication_error, null, null},
                {new HA7S.cmdReturn(HA7S.cmdResult.ReadOverrun), ReadPowerSupplyCmd.Result.communication_error, null, null},
                {new HA7S.cmdReturn(HA7S.cmdResult.ReadError), ReadPowerSupplyCmd.Result.communication_error, null, null},
                {new HA7S.cmdReturn(HA7S.cmdResult.ReadError), ReadPowerSupplyCmd.Result.communication_error, null, null},
                {new HA7S.cmdReturn(1, 1), ReadPowerSupplyCmd.Result.communication_error, null, null},
                {new HA7S.cmdReturn(4, 4), ReadPowerSupplyCmd.Result.success, parasitic, new Boolean(true)},
                {new HA7S.cmdReturn(4, 4), ReadPowerSupplyCmd.Result.success, not_parasitic, new Boolean(false)}
        };
    }

}
