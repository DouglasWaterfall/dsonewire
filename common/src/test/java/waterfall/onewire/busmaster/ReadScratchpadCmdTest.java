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
public class ReadScratchpadCmdTest {

    public static class TestReadScratchpadCmd extends ReadScratchpadCmd {
        private Result execute_internal_result = null;
        private long execute_internal_resultWriteCTM = 0;
        private byte[] execute_internal_resultData = null;
        private byte[] execute_internal_resultHexData = null;
        private RuntimeException execute_internal_exception = null;

        public TestReadScratchpadCmd(BusMaster bm, DSAddress dsAddr, short requestByteCount, LogLevel logLevel ) {
            super(bm, dsAddr, requestByteCount, logLevel);
        }

        protected Result execute_internal() {
            if (execute_internal_exception != null) {
                throw execute_internal_exception;
            }
            setResultData(execute_internal_resultWriteCTM, execute_internal_resultData, execute_internal_resultHexData);
            return execute_internal_result;
        };

        protected void setResultData(long resultWriteCTM, byte[] resultData, byte[] resultHexData) {
            this.resultWriteCTM = resultWriteCTM;
            this.resultData = resultData;
            this.resultHexData = resultHexData;
        }

        public void setResult(Result result) {
            this.result = result;
        }

        public void setExecuteResult(Result result, long resultWriteCTM, byte[] resultData, byte[] resultHexData) {
            this.execute_internal_result = result;
            this.execute_internal_resultWriteCTM = resultWriteCTM;
            this.execute_internal_resultData = resultData;
            this.execute_internal_resultHexData = resultHexData;
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

        TestReadScratchpadCmd cmd = new TestReadScratchpadCmd(mockBM, mockAddr, (short)5, mockLogLevel);
        Assert.assertNull(cmd.getResult());

        try {
            long v = cmd.getResultWriteCTM();
            Assert.fail("exception expected");
        }
        catch(NoResultException e) {
           ;
        }
    }

    @Test(expectedExceptions = AssertionError.class )
    public void testBadRequestByteCount() {
        BusMaster mockBM = mock(BusMaster.class);
        DSAddress mockAddr = mock(DSAddress.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        new TestReadScratchpadCmd(mockBM, mockAddr, (short)0, mockLogLevel);
        Assert.fail("exception expected");
    }

    @Test(expectedExceptions = NoResultException.class)
    public void testGetResultWriteCTMBusy() {
        BusMaster mockBM = mock(BusMaster.class);
        DSAddress mockAddr = mock(DSAddress.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        TestReadScratchpadCmd cmd = new TestReadScratchpadCmd(mockBM, mockAddr, (short)5, mockLogLevel);
        cmd.setResult(ReadScratchpadCmd.Result.busy);
        cmd.getResultWriteCTM();
        Assert.fail("exception expected");
    }

    @Test(expectedExceptions = NoResultDataException.class)
    public void testGetResulWriteCTMNotSuccess() {
        BusMaster mockBM = mock(BusMaster.class);
        DSAddress mockAddr = mock(DSAddress.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        TestReadScratchpadCmd cmd = new TestReadScratchpadCmd(mockBM, mockAddr, (short)5, mockLogLevel);
        cmd.setResult(ReadScratchpadCmd.Result.communication_error);
        cmd.getResultWriteCTM();
        Assert.fail("exception expected");
    }

    @Test(expectedExceptions = NoResultException.class)
    public void testGetResultDataBusy() {
        BusMaster mockBM = mock(BusMaster.class);
        DSAddress mockAddr = mock(DSAddress.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        TestReadScratchpadCmd cmd = new TestReadScratchpadCmd(mockBM, mockAddr, (short)5, mockLogLevel);
        cmd.setResult(ReadScratchpadCmd.Result.busy);
        cmd.getResultData();
        Assert.fail("exception expected");
    }

    @Test(expectedExceptions = NoResultDataException.class)
    public void testGetResultDataNotSuccess() {
        BusMaster mockBM = mock(BusMaster.class);
        DSAddress mockAddr = mock(DSAddress.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        TestReadScratchpadCmd cmd = new TestReadScratchpadCmd(mockBM, mockAddr, (short)5, mockLogLevel);
        cmd.setResult(ReadScratchpadCmd.Result.communication_error);
        cmd.getResultData();
        Assert.fail("exception expected");
    }

    @Test(expectedExceptions = NoResultException.class)
    public void testGetResultHexDataBusy() {
        BusMaster mockBM = mock(BusMaster.class);
        DSAddress mockAddr = mock(DSAddress.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        TestReadScratchpadCmd cmd = new TestReadScratchpadCmd(mockBM, mockAddr, (short)5, mockLogLevel);
        cmd.setResult(ReadScratchpadCmd.Result.busy);
        cmd.getResultHexData();
        Assert.fail("exception expected");
    }

    @Test(expectedExceptions = NoResultDataException.class)
    public void testGetResultHexDataNotSuccess() {
        BusMaster mockBM = mock(BusMaster.class);
        DSAddress mockAddr = mock(DSAddress.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        TestReadScratchpadCmd cmd = new TestReadScratchpadCmd(mockBM, mockAddr, (short)5, mockLogLevel);
        cmd.setResult(ReadScratchpadCmd.Result.communication_error);
        cmd.getResultHexData();
        Assert.fail("exception expected");
    }

    @Test(expectedExceptions = NoResultException.class)
    public void testExecuteBusy() {
        BusMaster mockBM = mock(BusMaster.class);
        DSAddress mockAddr = mock(DSAddress.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        TestReadScratchpadCmd cmd = new TestReadScratchpadCmd(mockBM, mockAddr, (short)5, mockLogLevel);
        cmd.setResult(ReadScratchpadCmd.Result.busy);
        cmd.execute();

        Assert.fail("should have thrown exception");
    }

    @Test
    public void testBusNotStarted() {
        BusMaster mockBM = mock(BusMaster.class);
        when(mockBM.getIsStarted()).thenReturn(false);
        DSAddress mockAddr = mock(DSAddress.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        TestReadScratchpadCmd cmd = new TestReadScratchpadCmd(mockBM, mockAddr, (short)5, mockLogLevel);
        Assert.assertEquals(cmd.execute(), ReadScratchpadCmd.Result.bus_not_started);
    }

    @Test
    public void testExecuteInternalException() {
        BusMaster mockBM = mock(BusMaster.class);
        when(mockBM.getIsStarted()).thenReturn(true);
        DSAddress mockAddr = mock(DSAddress.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        TestReadScratchpadCmd cmd = new TestReadScratchpadCmd(mockBM, mockAddr, (short)5, mockLogLevel);
        cmd.setExecuteException(new RuntimeException("foo"));
        ReadScratchpadCmd.Result r = cmd.execute();
        Assert.assertEquals(r, ReadScratchpadCmd.Result.communication_error);
    }

    @Test(dataProvider="createExecuteInternalResultData")
    public void testExecuteInternalResult(ReadScratchpadCmd.Result setResult, short setRequestByteCount, long setResultWriteCTM,
                                          byte[] setResultData, byte[] setResultHexData) {
        BusMaster mockBM = mock(BusMaster.class);
        when(mockBM.getIsStarted()).thenReturn(true);
        DSAddress mockAddr = mock(DSAddress.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        TestReadScratchpadCmd cmd = new TestReadScratchpadCmd(mockBM, mockAddr, setRequestByteCount, mockLogLevel);
        cmd.setExecuteResult(setResult, setResultWriteCTM, setResultData, setResultHexData);
        ReadScratchpadCmd.Result r = cmd.execute();
        Assert.assertEquals(r, setResult);
        if (r == ReadScratchpadCmd.Result.success) {
            Assert.assertEquals(cmd.getResultWriteCTM(), setResultWriteCTM);
            Assert.assertEquals(cmd.getResultData(), setResultData);
            Assert.assertEquals(cmd.getResultHexData(), setResultHexData);
        }
    }

    @DataProvider
    public Object[][] createExecuteInternalResultData() {
        byte[] twoData = { 0, 0 };
        byte[] twoHexData = { 0, 0, 0, 0 };

        return new Object[][] {
                { ReadScratchpadCmd.Result.bus_not_started, (short)2, -1, twoData, twoHexData },
                { ReadScratchpadCmd.Result.communication_error, (short)2, -1, twoData, twoHexData },
                { ReadScratchpadCmd.Result.device_not_found, (short)2, -1, twoData, twoHexData },
                { ReadScratchpadCmd.Result.device_error, (short)2, -1, twoData, twoHexData },
                { ReadScratchpadCmd.Result.success, (short)2, -1, twoData, twoHexData },
                { ReadScratchpadCmd.Result.success, (short)2, 0, twoData, twoHexData },
                { ReadScratchpadCmd.Result.success, (short)2, 1, twoData, twoHexData },
        };
    }

}