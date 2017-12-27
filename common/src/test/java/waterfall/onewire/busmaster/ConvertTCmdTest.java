package waterfall.onewire.busmaster;

import static org.mockito.Mockito.mock;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import waterfall.onewire.DSAddress;

/**
 * Created by dwaterfa on 7/29/17.
 */
public class ConvertTCmdTest {

  @Test
  public void testConstructorDefaults() {
    BusMaster mockBM = mock(BusMaster.class);
    DSAddress mockAddr = mock(DSAddress.class);

    TestConvertTCmd cmd = new TestConvertTCmd(mockBM, mockAddr);
    Assert.assertNull(cmd.getResult());

    try {
      long v = cmd.getResultWriteCTM();
      Assert.fail("exception expected");
    } catch (NoResultException e) {
      ;
    }
  }

  @Test(expectedExceptions = NoResultException.class)
  public void testGetResultWriteCTMBusy() {
    BusMaster mockBM = mock(BusMaster.class);
    DSAddress mockAddr = mock(DSAddress.class);

    TestConvertTCmd cmd = new TestConvertTCmd(mockBM, mockAddr);
    cmd.setResult(ConvertTCmd.Result.cmdBusy);
    cmd.getResultWriteCTM();
    Assert.fail("exception expected");
  }

  @Test(expectedExceptions = NoResultException.class)
  public void testExecuteBusy() {
    BusMaster mockBM = mock(BusMaster.class);
    DSAddress mockAddr = mock(DSAddress.class);

    TestConvertTCmd cmd = new TestConvertTCmd(mockBM, mockAddr);
    cmd.setResult(ConvertTCmd.Result.cmdBusy);
    cmd.execute();

    Assert.fail("should have thrown exception");
  }

  @Test
  public void testExecuteInternalException() {
    BusMaster mockBM = mock(BusMaster.class);
    DSAddress mockAddr = mock(DSAddress.class);

    TestConvertTCmd cmd = new TestConvertTCmd(mockBM, mockAddr);
    cmd.setExecuteException(new RuntimeException("foo"));
    ConvertTCmd.Result r = cmd.execute();
    Assert.assertEquals(r, ConvertTCmd.Result.deviceFault);
  }

  @Test(dataProvider = "createExecuteInternalResultData")
  public void testExecuteInternalResult(ConvertTCmd.Result setResult, long setResultWriteCTM) {
    BusMaster mockBM = mock(BusMaster.class);
    DSAddress mockAddr = mock(DSAddress.class);

    TestConvertTCmd cmd = new TestConvertTCmd(mockBM, mockAddr);
    cmd.setExecuteResult(setResult, setResultWriteCTM);
    ConvertTCmd.Result r = cmd.execute();
    Assert.assertEquals(r, setResult);
    if (r == ConvertTCmd.Result.success) {
      Assert.assertEquals(cmd.getResultWriteCTM(), setResultWriteCTM);
    }
  }

  @DataProvider
  public Object[][] createExecuteInternalResultData() {
    return new Object[][]{
        {ConvertTCmd.Result.busFault, -1},
        {ConvertTCmd.Result.deviceFault, -1},
        {ConvertTCmd.Result.success, -1},
        {ConvertTCmd.Result.success, 0},
        {ConvertTCmd.Result.success, 1},
    };
  }

  public static class TestConvertTCmd extends ConvertTCmd {

    private Result execute_internal_result = null;
    private long execute_internal_resultWriteCTM = 0;
    private RuntimeException execute_internal_exception = null;

    public TestConvertTCmd(BusMaster bm, DSAddress dsAddr) {
      super(bm, dsAddr);
    }

    protected Result execute_internal() {
      if (execute_internal_exception != null) {
        throw execute_internal_exception;
      }
      setResultData(execute_internal_resultWriteCTM);
      return execute_internal_result;
    }

    protected void setResultData(long resultWriteCTM) {
      this.resultWriteCTM = resultWriteCTM;
    }

    public void setResult(Result result) {
      this.result = result;
    }

    public void setExecuteResult(Result result, long resultWriteCTM) {
      this.execute_internal_result = result;
      this.execute_internal_resultWriteCTM = resultWriteCTM;
    }

    public void setExecuteException(RuntimeException exception) {
      this.execute_internal_exception = exception;
    }

  }

}
