package waterfall.onewire.busmaster;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Created by dwaterfa on 8/6/17.
 */
public class SearchPusherByBusCmdTest {

  @Test(dataProvider = "createPushByCorrectCmdCases")
  public void testPushByCorrectCmd(boolean isByAlarm) {

    BusMaster mockBM = mock(BusMaster.class);
    when(mockBM.getIsStarted()).thenReturn(true);

    SearchBusCmd mockByAlarmCmd = mock(SearchBusCmd.class);
    SearchBusCmd mockNotByAlarmCmd = mock(SearchBusCmd.class);

    when(mockBM.querySearchBusByAlarmCmd()).thenReturn(mockByAlarmCmd);
    when(mockBM.querySearchBusCmd()).thenReturn(mockNotByAlarmCmd);

    SearchPusherByBusCmd pusher = new SearchPusherByBusCmd(mockBM, isByAlarm);
    Assert.assertTrue(pusher.adjustPeriod(250));
    delayForPush(25);

    verify(mockBM, times(isByAlarm ? 1 : 0)).querySearchBusByAlarmCmd();
    verify(mockBM, times(isByAlarm ? 0 : 1)).querySearchBusCmd();
    verify(mockByAlarmCmd, times(isByAlarm ? 1 : 0)).execute();
    verify(mockNotByAlarmCmd, times(isByAlarm ? 0 : 1)).execute();

    Assert.assertFalse(pusher.adjustPeriod(Long.MAX_VALUE));
  }

  @DataProvider
  public Object[][] createPushByCorrectCmdCases() {
    return new Object[][]{
        {true},
        {false}
    };
  }

  // this just lets the push thread get a chance to run
  private void delayForPush(long msec) {
    try {
      Thread.sleep(msec);
    } catch (InterruptedException e) {
      ;
    }
  }

  public static class TestSearchBusCmd extends SearchBusCmd {

    public TestSearchBusCmd(BusMaster bm, boolean byAlarm) {
      super(bm, byAlarm);
    }

    protected Result execute_internal() {
      setResultData(0, null);
      return Result.success;
    }

    ;

    protected void setResultData(long resultWriteCTM, List<String> resultList) {
      this.resultData = new ResultData(resultList, resultWriteCTM);
    }

  }


}
