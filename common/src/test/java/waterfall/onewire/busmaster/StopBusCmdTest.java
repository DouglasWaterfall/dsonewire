package waterfall.onewire.busmaster;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;

/**
 * Created by dwaterfa on 7/29/17.
 */
public class StopBusCmdTest {

    public static class TestStopBusCmd extends StopBusCmd {
        private Result execute_internal_result = null;
        private RuntimeException execute_internal_exception = null;

        public TestStopBusCmd(BusMaster bm) {
            super(bm);
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

        TestStopBusCmd cmd = new TestStopBusCmd(mockBM);
        Assert.assertNull(cmd.getResult());
    }

    @Test(expectedExceptions = NoResultException.class)
    public void testExecuteBusy() {
        BusMaster mockBM = mock(BusMaster.class);

        TestStopBusCmd cmd = new TestStopBusCmd(mockBM);
        cmd.setResult(StopBusCmd.Result.busy);
        cmd.execute();

        Assert.fail("should have thrown exception");
    }

    @Test
    public void testExecuteInternalException() {
        BusMaster mockBM = mock(BusMaster.class);

        TestStopBusCmd cmd = new TestStopBusCmd(mockBM);
        cmd.setExecuteException(new RuntimeException("foo"));
        StopBusCmd.Result r = cmd.execute();
        Assert.assertEquals(r, StopBusCmd.Result.communication_error);
    }

    @Test(dataProvider="createExecuteInternalResultData")
    public void testExecuteInternalResult(StopBusCmd.Result setResult) {
        BusMaster mockBM = mock(BusMaster.class);

        TestStopBusCmd cmd = new TestStopBusCmd(mockBM);
        cmd.setExecuteResult(setResult);
        StopBusCmd.Result r = cmd.execute();
        Assert.assertEquals(r, setResult);
    }

    @DataProvider
    public Object[][] createExecuteInternalResultData() {
        return new Object[][] {
                { StopBusCmd.Result.not_started },
                { StopBusCmd.Result.communication_error },
                { StopBusCmd.Result.stopped },
        };
    }

}
