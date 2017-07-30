package waterfall.onewire.busmaster;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;

/**
 * Created by dwaterfa on 7/29/17.
 */
public class StartBusCmdTest {

    public static class TestStartBusCmd extends StartBusCmd {
        private Result execute_internal_result = null;
        private RuntimeException execute_internal_exception = null;

        public TestStartBusCmd(BusMaster bm, LogLevel logLevel ) {
            super(bm, logLevel);
        }

        protected Result execute_internal() {
            if (execute_internal_exception != null) {
                throw execute_internal_exception;
            }
            return execute_internal_result;
        };

        public void setResult(Result result) {
            this.result = result;
        }

        public void setExecuteResult(Result result) {
            this.execute_internal_result = result;
        }

        public void setExecuteException(RuntimeException exception) {
            this.execute_internal_exception = exception;
        }

    }

    @Test
    public void testConstructorDefaults() {
        BusMaster mockBM = mock(BusMaster.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        TestStartBusCmd cmd = new TestStartBusCmd(mockBM, mockLogLevel);
        Assert.assertNull(cmd.getResult());
    }

    @Test(expectedExceptions = NoResultException.class)
    public void testExecuteBusy() {
        BusMaster mockBM = mock(BusMaster.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        TestStartBusCmd cmd = new TestStartBusCmd(mockBM, mockLogLevel);
        cmd.setResult(StartBusCmd.Result.busy);
        cmd.execute();

        Assert.fail("should have thrown exception");
    }

    @Test
    public void testExecuteInternalException() {
        BusMaster mockBM = mock(BusMaster.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        TestStartBusCmd cmd = new TestStartBusCmd(mockBM, mockLogLevel);
        cmd.setExecuteException(new RuntimeException("foo"));
        StartBusCmd.Result r = cmd.execute();
        Assert.assertEquals(r, StartBusCmd.Result.communication_error);
    }

    @Test(dataProvider="createExecuteInternalResultData")
    public void testExecuteInternalResult(StartBusCmd.Result setResult) {
        BusMaster mockBM = mock(BusMaster.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        TestStartBusCmd cmd = new TestStartBusCmd(mockBM, mockLogLevel);
        cmd.setExecuteResult(setResult);
        StartBusCmd.Result r = cmd.execute();
        Assert.assertEquals(r, setResult);
    }

    @DataProvider
    public Object[][] createExecuteInternalResultData() {
        return new Object[][] {
                { StartBusCmd.Result.already_started },
                { StartBusCmd.Result.bus_not_found },
                { StartBusCmd.Result.communication_error },
                { StartBusCmd.Result.started },
        };
    }

}
