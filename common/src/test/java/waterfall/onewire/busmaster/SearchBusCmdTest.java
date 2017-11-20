package waterfall.onewire.busmaster;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Created by dwaterfa on 7/29/17.
 */
public class SearchBusCmdTest {

  @Test(dataProvider = "constructorDefaultsProvider")
  public void testConstructorDefaults(SearchBusCmd cmd, boolean isByAlarm, boolean isByFamilyCode) {
    Assert.assertEquals(cmd.isByAlarm(), isByAlarm);
    Assert.assertEquals(cmd.isByFamilyCode(), isByFamilyCode);

    Assert.assertNull(cmd.getResult());

    try {
      cmd.getResultData();
      Assert.fail("exception expected");
    } catch (NoResultException e) {
      ;
    }

    try {
      cmd.getResultList();
      Assert.fail("exception expected");
    } catch (NoResultException e) {
      ;
    }

    try {
      cmd.getResultListCRC32();
      Assert.fail("exception expected");
    } catch (NoResultException e) {
      ;
    }

    try {
      cmd.getResultWriteCTM();
      Assert.fail("exception expected");
    } catch (NoResultException e) {
      ;
    }
  }

  @DataProvider
  public Object[][] constructorDefaultsProvider() {
    BusMaster mockBM = mock(BusMaster.class);

    // SearchBusCmd, isByAlarm, isByFamilyCode
    return new Object[][]{
        // byAlarm, not byFamilyCode
        {new TestSearchBusCmd(mockBM, true), true, false},
        // not byAlarm, not byFamilyCode
        {new TestSearchBusCmd(mockBM, false), false, false},
        // not byAlarm, byFamilyCode
        {new TestSearchBusCmd(mockBM, (short) 5), false, true}

    };

  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBadRequestNegativeFamilyCode() {
    BusMaster mockBM = mock(BusMaster.class);

    new TestSearchBusCmd(mockBM, (short) -1);
    Assert.fail("exception expected");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBadRequestBigFamilyCode() {
    BusMaster mockBM = mock(BusMaster.class);

    new TestSearchBusCmd(mockBM, (short) 256);
    Assert.fail("exception expected");
  }

  @Test(expectedExceptions = NoResultException.class)
  public void testGetResulDataBusy() {
    BusMaster mockBM = mock(BusMaster.class);

    TestSearchBusCmd cmd = new TestSearchBusCmd(mockBM, (short) 5);
    cmd.setResult(SearchBusCmd.Result.busy);
    cmd.getResultData();
    Assert.fail("exception expected");
  }

  @Test(expectedExceptions = NoResultDataException.class)
  public void testGetResulDataNoSuccess() {
    BusMaster mockBM = mock(BusMaster.class);

    TestSearchBusCmd cmd = new TestSearchBusCmd(mockBM, (short) 5);
    cmd.setResult(SearchBusCmd.Result.communication_error);
    cmd.getResultData();
    Assert.fail("exception expected");
  }

  @Test(expectedExceptions = NoResultException.class)
  public void testExecuteBusy() {
    BusMaster mockBM = mock(BusMaster.class);

    TestSearchBusCmd cmd = new TestSearchBusCmd(mockBM, (short) 5);
    cmd.setResult(SearchBusCmd.Result.busy);
    cmd.execute();

    Assert.fail("should have thrown exception");
  }

  @Test
  public void testBusNotStarted() {
    BusMaster mockBM = mock(BusMaster.class);
    when(mockBM.getIsStarted()).thenReturn(false);

    TestSearchBusCmd cmd = new TestSearchBusCmd(mockBM, (short) 5);
    Assert.assertEquals(cmd.execute(), SearchBusCmd.Result.bus_not_started);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testResultDataListNull() {
    BusMaster mockBM = mock(BusMaster.class);

    TestSearchBusCmd cmd = new TestSearchBusCmd(mockBM, (short) 5);
    new SearchBusCmd.ResultData(null, 1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testResultDataBadWriteCTM() {
    BusMaster mockBM = mock(BusMaster.class);

    TestSearchBusCmd cmd = new TestSearchBusCmd(mockBM, (short) 5);
    new SearchBusCmd.ResultData(new ArrayList<String>(), 0);
  }

  @Test
  public void testExecuteInternalException() {
    BusMaster mockBM = mock(BusMaster.class);
    when(mockBM.getIsStarted()).thenReturn(true);

    TestSearchBusCmd cmd = new TestSearchBusCmd(mockBM, (short) 5);
    cmd.setExecuteException(new RuntimeException("foo"));
    SearchBusCmd.Result r = cmd.execute();
    Assert.assertEquals(r, SearchBusCmd.Result.communication_error);
  }

  @Test(dataProvider = "createExecuteInternalResultData")
  public void testExecuteInternalResult(SearchBusCmd.Result setResult, List<String> setResultList,
      long setResultWriteCTM) {
    BusMaster mockBM = mock(BusMaster.class);
    when(mockBM.getIsStarted()).thenReturn(true);

    TestSearchBusCmd cmd = new TestSearchBusCmd(mockBM, false);
    cmd.setExecuteResult(setResult, setResultWriteCTM, setResultList);
    SearchBusCmd.Result r = cmd.execute();
    Assert.assertEquals(r, setResult);
    if (r == SearchBusCmd.Result.success) {
      Assert.assertEquals(cmd.getResultWriteCTM(), setResultWriteCTM);
      Assert.assertEquals(cmd.getResultList(), setResultList);
    }
  }

  @DataProvider
  public Object[][] createExecuteInternalResultData() {
    List<String> list = new ArrayList<String>();

    return new Object[][]{
        {SearchBusCmd.Result.bus_not_started, list, 1},
        {SearchBusCmd.Result.communication_error, list, 1},
        {SearchBusCmd.Result.success, list, 1},
        {SearchBusCmd.Result.success, list, 2}
    };
  }

  public static class TestSearchBusCmd extends SearchBusCmd {

    private Result execute_internal_result = null;
    private long execute_internal_resultWriteCTM = 0;
    private List<String> execute_internal_resultList = null;
    private RuntimeException execute_internal_exception = null;

    public TestSearchBusCmd(BusMaster bm, short familyCode) {
      super(bm, familyCode);
    }

    public TestSearchBusCmd(BusMaster bm, boolean byAlarm) {
      super(bm, byAlarm);
    }

    protected Result execute_internal() {
      if (execute_internal_exception != null) {
        throw execute_internal_exception;
      }
      setResultData(execute_internal_resultWriteCTM, execute_internal_resultList);
      return execute_internal_result;
    }

    ;

    protected void setResultData(long resultWriteCTM, List<String> resultList) {
      this.resultData = new ResultData(resultList, resultWriteCTM);
    }

    public void setResult(Result result) {
      this.result = result;
    }

    public void setExecuteResult(Result result, long resultWriteCTM, List<String> resultList) {
      this.execute_internal_result = result;
      this.execute_internal_resultWriteCTM = resultWriteCTM;
      this.execute_internal_resultList = resultList;
    }

    public void setExecuteException(RuntimeException exception) {
      this.execute_internal_exception = exception;
    }

  }

}
