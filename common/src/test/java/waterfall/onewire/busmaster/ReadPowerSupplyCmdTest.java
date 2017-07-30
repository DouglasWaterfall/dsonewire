package waterfall.onewire.busmaster;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import waterfall.onewire.DSAddress;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by dwaterfa on 7/29/17.
 */
public class ReadPowerSupplyCmdTest {

    public static class TestReadPowerSupplyCmd extends ReadPowerSupplyCmd {
        private Result execute_internal_result = null;
        private long execute_internal_resultWriteCTM = 0;
        private boolean execute_internal_resultIsParasitic = false;
        private RuntimeException execute_internal_exception = null;

        public TestReadPowerSupplyCmd(BusMaster bm, DSAddress dsAddr, LogLevel logLevel ) {
            super(bm, dsAddr, logLevel);
        }

        protected Result execute_internal() {
            if (execute_internal_exception != null) {
                throw execute_internal_exception;
            }
            setResultData(execute_internal_resultWriteCTM, execute_internal_resultIsParasitic);
            return execute_internal_result;
        };

        protected void setResultData(long resultWriteCTM, boolean isParasitic) {
            this.resultWriteCTM = resultWriteCTM;
            this.resultIsParasitic = isParasitic;
        }

        public void setResult(Result result) {
            this.result = result;
        }

        public void setExecuteResult(Result result, long resultWriteCTM, boolean resultIsParasitic) {
            this.execute_internal_result = result;
            this.execute_internal_resultWriteCTM = resultWriteCTM;
            this.execute_internal_resultIsParasitic = resultIsParasitic;
        }

        public void setExecuteException(RuntimeException exception) {
            this.execute_internal_exception = exception;
        }

    }

    @Test
    public void testConstructorDefaults() {
        BusMaster mockBM = mock(BusMaster.class);
        DSAddress mockAddr = mock(DSAddress.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        TestReadPowerSupplyCmd cmd = new TestReadPowerSupplyCmd(mockBM, mockAddr, mockLogLevel);
        Assert.assertNull(cmd.getResult());

        try {
            long v = cmd.getResultWriteCTM();
            Assert.fail("exception expected");
        }
        catch(NoResultException e) {
           ;
        }
    }

    @Test(expectedExceptions = NoResultException.class)
    public void testGetResultWriteCTMBusy() {
        BusMaster mockBM = mock(BusMaster.class);
        DSAddress mockAddr = mock(DSAddress.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        TestReadPowerSupplyCmd cmd = new TestReadPowerSupplyCmd(mockBM, mockAddr, mockLogLevel);
        cmd.setResult(ReadPowerSupplyCmd.Result.busy);
        cmd.getResultWriteCTM();
        Assert.fail("exception expected");
    }

    @Test(expectedExceptions = NoResultException.class)
    public void testGetResultIsParasiticBusy() {
        BusMaster mockBM = mock(BusMaster.class);
        DSAddress mockAddr = mock(DSAddress.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        TestReadPowerSupplyCmd cmd = new TestReadPowerSupplyCmd(mockBM, mockAddr, mockLogLevel);
        cmd.setResult(ReadPowerSupplyCmd.Result.busy);
        cmd.getResultIsParasitic();
        Assert.fail("exception expected");
    }

    @Test(expectedExceptions = NoResultDataException.class)
    public void testGetResultIsParasiticNotSuccess() {
        BusMaster mockBM = mock(BusMaster.class);
        DSAddress mockAddr = mock(DSAddress.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        TestReadPowerSupplyCmd cmd = new TestReadPowerSupplyCmd(mockBM, mockAddr, mockLogLevel);
        cmd.setResult(ReadPowerSupplyCmd.Result.communication_error);
        cmd.getResultIsParasitic();
        Assert.fail("exception expected");
    }

    @Test(expectedExceptions = NoResultException.class)
    public void testExecuteBusy() {
        BusMaster mockBM = mock(BusMaster.class);
        DSAddress mockAddr = mock(DSAddress.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        TestReadPowerSupplyCmd cmd = new TestReadPowerSupplyCmd(mockBM, mockAddr, mockLogLevel);
        cmd.setResult(ReadPowerSupplyCmd.Result.busy);
        cmd.execute();

        Assert.fail("should have thrown exception");
    }

    @Test
    public void testBusNotStarted() {
        BusMaster mockBM = mock(BusMaster.class);
        when(mockBM.getIsStarted()).thenReturn(false);
        DSAddress mockAddr = mock(DSAddress.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        TestReadPowerSupplyCmd cmd = new TestReadPowerSupplyCmd(mockBM, mockAddr, mockLogLevel);
        Assert.assertEquals(cmd.execute(), ReadPowerSupplyCmd.Result.bus_not_started);
    }

    @Test
    public void testExecuteInternalException() {
        BusMaster mockBM = mock(BusMaster.class);
        when(mockBM.getIsStarted()).thenReturn(true);
        DSAddress mockAddr = mock(DSAddress.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        TestReadPowerSupplyCmd cmd = new TestReadPowerSupplyCmd(mockBM, mockAddr, mockLogLevel);
        cmd.setExecuteException(new RuntimeException("foo"));
        ReadPowerSupplyCmd.Result r = cmd.execute();
        Assert.assertEquals(r, ReadPowerSupplyCmd.Result.communication_error);
    }

    @Test(dataProvider="createExecuteInternalResultData")
    public void testExecuteInternalResult(ReadPowerSupplyCmd.Result setResult, long setResultWriteCTM,
                                          boolean setResultIsParasitic) {
        BusMaster mockBM = mock(BusMaster.class);
        when(mockBM.getIsStarted()).thenReturn(true);
        DSAddress mockAddr = mock(DSAddress.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        TestReadPowerSupplyCmd cmd = new TestReadPowerSupplyCmd(mockBM, mockAddr, mockLogLevel);
        cmd.setExecuteResult(setResult, setResultWriteCTM, setResultIsParasitic);
        ReadPowerSupplyCmd.Result r = cmd.execute();
        Assert.assertEquals(r, setResult);
        if (r == ReadPowerSupplyCmd.Result.success) {
            Assert.assertEquals(cmd.getResultWriteCTM(), setResultWriteCTM);
            Assert.assertEquals(cmd.getResultIsParasitic(), setResultIsParasitic);
        }
    }

    @DataProvider
    public Object[][] createExecuteInternalResultData() {
        return new Object[][] {
                { ReadPowerSupplyCmd.Result.bus_not_started, -1, false },
                { ReadPowerSupplyCmd.Result.communication_error, -1, false },
                { ReadPowerSupplyCmd.Result.device_not_found, -1, false },
                { ReadPowerSupplyCmd.Result.device_error, -1, false },
                { ReadPowerSupplyCmd.Result.success, -1, false },
                { ReadPowerSupplyCmd.Result.success, 0, true },
                { ReadPowerSupplyCmd.Result.success, 1, true },
        };
    }

}