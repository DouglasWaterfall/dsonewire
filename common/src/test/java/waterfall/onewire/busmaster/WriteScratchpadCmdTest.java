package waterfall.onewire.busmaster;

import static org.mockito.Mockito.mock;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.WriteScratchpadCmd.Result;

/**
 * Created by dwaterfa on 7/29/17.
 */
public class WriteScratchpadCmdTest {

  @Test
  public void testConstructorDefaults() {
    BusMaster mockBM = mock(BusMaster.class);
    DSAddress mockAddr = mock(DSAddress.class);

    TestWriteScratchpadCmd cmd = new TestWriteScratchpadCmd(mockBM, mockAddr, new byte[1]);
    Assert.assertNull(cmd.getResult());

    try {
      long v = cmd.getResultWriteCTM();
      Assert.fail("exception expected");
    } catch (NoResultException e) {
      ;
    }
  }

  @DataProvider
  public Object[][] getWriteDataNegativeCases() {
    return new Object[][] {
        { null },
        { new byte[0] }
    };
  }

  @Test(dataProvider = "getWriteDataNegativeCases", expectedExceptions = IllegalArgumentException.class)
  public void testWriteDataNegativeCases(byte[] writeData) {
    BusMaster mockBM = mock(BusMaster.class);
    DSAddress mockAddr = mock(DSAddress.class);

    new TestWriteScratchpadCmd(mockBM, mockAddr, writeData);
    Assert.fail("exception expected");
  }

  @Test(expectedExceptions = NoResultException.class)
  public void testGetResultWriteCTMBusy() {
    BusMaster mockBM = mock(BusMaster.class);
    DSAddress mockAddr = mock(DSAddress.class);

    TestWriteScratchpadCmd cmd = new TestWriteScratchpadCmd(mockBM, mockAddr, new byte[1]);
    cmd.setResult(Result.cmdBusy);
    cmd.getResultWriteCTM();
    Assert.fail("exception expected");
  }

  @Test(expectedExceptions = NoResultDataException.class)
  public void testGetResultWriteCTMNotSuccess() {
    BusMaster mockBM = mock(BusMaster.class);
    DSAddress mockAddr = mock(DSAddress.class);

    TestWriteScratchpadCmd cmd = new TestWriteScratchpadCmd(mockBM, mockAddr, new byte[1]);
    cmd.setResult(Result.deviceFault);
    cmd.getResultWriteCTM();
    Assert.fail("exception expected");
  }

  @Test(expectedExceptions = NoResultException.class)
  public void testExecuteBusy() {
    BusMaster mockBM = mock(BusMaster.class);
    DSAddress mockAddr = mock(DSAddress.class);

    TestWriteScratchpadCmd cmd = new TestWriteScratchpadCmd(mockBM, mockAddr, new byte[1]);
    cmd.setResult(Result.cmdBusy);
    cmd.execute();

    Assert.fail("should have thrown exception");
  }

  @Test
  public void testExecuteInternalException() {
    BusMaster mockBM = mock(BusMaster.class);
    DSAddress mockAddr = mock(DSAddress.class);

    TestWriteScratchpadCmd cmd = new TestWriteScratchpadCmd(mockBM, mockAddr, new byte[1]);
    cmd.setExecuteException(new RuntimeException("foo"));
    Result r = cmd.execute();
    Assert.assertEquals(r, Result.deviceFault);
  }

  @Test(dataProvider = "createExecuteInternalResultData")
  public void testExecuteInternalResult(Result setResult, byte[] setWriteData, long setResultWriteCTM) {
    BusMaster mockBM = mock(BusMaster.class);
    DSAddress mockAddr = mock(DSAddress.class);

    TestWriteScratchpadCmd cmd = new TestWriteScratchpadCmd(mockBM, mockAddr, setWriteData);
    cmd.setExecuteResult(setResult, setResultWriteCTM);
    Result r = cmd.execute();
    Assert.assertEquals(r, setResult);
    if (r == Result.success) {
      Assert.assertEquals(cmd.getResultWriteCTM(), setResultWriteCTM);
    }
  }

  @DataProvider
  public Object[][] createExecuteInternalResultData() {
    byte[] writeData = { 0 };

    return new Object[][]{
        {Result.busFault, writeData, -1},
        {Result.deviceFault, writeData, -1},
        {Result.success, writeData, -1},
        {Result.success, writeData, 0},
        {Result.success, writeData, 1},
    };
  }

  public static class TestWriteScratchpadCmd extends WriteScratchpadCmd {

    private Result execute_internal_result = null;
    private long execute_internal_resultWriteCTM = 0;
    private RuntimeException execute_internal_exception = null;

    public TestWriteScratchpadCmd(BusMaster bm, DSAddress dsAddr, byte[] writeData) {
      super(bm, dsAddr, writeData);
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

