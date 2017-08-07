package waterfall.onewire.busmaster;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.Mockito.*;

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

        when(mockBM.querySearchBusByAlarmCmd(any(Logger.LogLevel.class))).thenReturn(mockByAlarmCmd);
        when(mockBM.querySearchBusCmd(any(Logger.LogLevel.class))).thenReturn(mockNotByAlarmCmd);

        SearchPusherByBusCmd pusher = new SearchPusherByBusCmd(mockBM, isByAlarm);
        Assert.assertTrue(pusher.adjustPeriod(250));
        delayForPush(25);

        verify(mockBM, times(isByAlarm ? 1 : 0)).querySearchBusByAlarmCmd(any(Logger.LogLevel.class));
        verify(mockBM, times(isByAlarm ? 0 : 1)).querySearchBusCmd(any(Logger.LogLevel.class));
        verify(mockByAlarmCmd, times(isByAlarm ? 1 : 0)).execute();
        verify(mockNotByAlarmCmd, times(isByAlarm ? 0 : 1)).execute();

        Assert.assertFalse(pusher.adjustPeriod(Long.MAX_VALUE));
    }

    @DataProvider
    public Object[][] createPushByCorrectCmdCases() {
        return new Object[][] {
                { true },
                { false }
        };
    }

    public static class TestSearchBusCmd extends SearchBusCmd {

        public TestSearchBusCmd(BusMaster bm, boolean byAlarm, LogLevel logLevel) {
            super(bm, byAlarm, logLevel);
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

    // this just lets the push thread get a chance to run
    private void delayForPush(long msec) {
        try {
            Thread.sleep(msec);
        }
        catch (InterruptedException e) {
            ;
        }
    }



}
