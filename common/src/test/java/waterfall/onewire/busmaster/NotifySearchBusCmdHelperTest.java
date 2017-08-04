package waterfall.onewire.busmaster;

import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by dwaterfa on 7/31/17.
 */
public class NotifySearchBusCmdHelperTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testConstructorArgs() {
        new NotifySearchBusCmdHelper(null, false);
    }

    @Test(dataProvider = "scheduleNotifySearchBusCmdNegativeTestsProvider")
    /*
    {BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_NotifyObjNull, notStarted, byAlarm, null, addOnce, ignoredMinPeriodMSec},
    {BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_NotifyObjNull, notStarted, notByAlarm, null, addOnce, ignoredMinPeriodMSec},
    */
    public void testScheduleNotifySearchBusCmdNegativeTests(BusMaster.ScheduleNotifySearchBusCmdResult expectedResult,
                                                            NotifySearchBusCmdResult nsbcr, boolean isStarted,
                                                            boolean byAlarm, boolean addTwice, long minPeriodMSec) {
        BusMaster mockBM = mock(BusMaster.class);
        if (isStarted) {
            when(mockBM.getIsStarted()).thenReturn(true);
            mySearchBusCmd mySearchBusCmd = new mySearchBusCmd(mockBM, byAlarm);
            when(mockBM.querySearchBusByAlarmCmd(any(Logger.LogLevel.class))).thenReturn(mySearchBusCmd);
        }

        NotifySearchBusCmdHelper nsbch = new NotifySearchBusCmdHelper(mockBM, byAlarm);

        if (addTwice) {
            BusMaster.ScheduleNotifySearchBusCmdResult result = nsbch.scheduleSearchNotifyFor(nsbcr, minPeriodMSec);
            Assert.assertEquals(BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success, result);
        }

        BusMaster.ScheduleNotifySearchBusCmdResult result = nsbch.scheduleSearchNotifyFor(nsbcr, minPeriodMSec);
        Assert.assertEquals(result, expectedResult);

        nsbch.cancelAllScheduledSearchNotifyFor();
    }

    @Test(dataProvider = "updateScheduledSearchNotifyForNegativeTestsProvider")
    public void testUpdateScheduledSearchNotifyForNegativeTests(BusMaster.UpdateScheduledNotifySearchBusCmdResult expectedResult,
                                                                NotifySearchBusCmdResult nsbcr, BusMaster bm, boolean byAlarm, long minPeriodMSec) {
        NotifySearchBusCmdHelper nsbch = new NotifySearchBusCmdHelper(bm, byAlarm);
        BusMaster.UpdateScheduledNotifySearchBusCmdResult result = nsbch.updateScheduledSearchNotifyFor(nsbcr, minPeriodMSec);
        Assert.assertEquals(result, expectedResult);
    }

    private class mySearchBusCmd extends SearchBusCmd {
        SearchBusCmd.Result expected_result = null;
        List<String> expected_resultList = null;
        long expected_writeCTM = 0;

        public mySearchBusCmd(BusMaster bm, boolean byAlarm) {
            super(bm, byAlarm, null);
        }

        public mySearchBusCmd(BusMaster bm, short familyCode) {
            super(bm, familyCode, null);
        }

        public SearchBusCmd.Result execute_internal() {
            result = expected_result;
            if (result == Result.success) {
                setResultData(expected_writeCTM, expected_resultList);
            }
            return result;
        }

        public void setResultData(long writeCTM, List<String> list) {
            resultData = new ResultData(expected_resultList, expected_writeCTM);
        }

        // class specific
        public void setExpectedReturn(SearchBusCmd.Result expected_result,
                                      List<String> expected_resultList,
                                      long expected_writeCTM) {
            this.expected_result = expected_result;
            this.expected_resultList = expected_resultList;
            this.expected_writeCTM = expected_writeCTM;
        }

    }

    @DataProvider
    public Object[][] scheduleNotifySearchBusCmdNegativeTestsProvider() {

        NotifySearchBusCmdResult mockNSBCR = mock(NotifySearchBusCmdResult.class);

        boolean notStarted = false;
        boolean started = true;

        boolean notByAlarm = false;
        boolean byAlarm = true;

        boolean addOnce = false;
        boolean addTwice = true;

        long invalidMinPeriodMSec = -1;
        long ignoredMinPeriodMSec = 1;

        return new Object[][]{
                {BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_NotifyObjNull, null, notStarted, byAlarm, addOnce, ignoredMinPeriodMSec},
                {BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_NotifyObjNull, null, notStarted, notByAlarm, addOnce, ignoredMinPeriodMSec},

                {BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_MinPeriodInvalid, mockNSBCR, notStarted, byAlarm, addOnce, invalidMinPeriodMSec},
                {BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_MinPeriodInvalid, mockNSBCR, notStarted, notByAlarm, addOnce, invalidMinPeriodMSec},

                {BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_BusMasterNotStarted, mockNSBCR, notStarted, byAlarm, addOnce, ignoredMinPeriodMSec},
                {BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_BusMasterNotStarted, mockNSBCR, notStarted, notByAlarm, addOnce, ignoredMinPeriodMSec},

                {BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_NotifyObjAlreadyScheduled, mockNSBCR, started, byAlarm, addTwice, ignoredMinPeriodMSec },
                {BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_NotifyObjAlreadyScheduled, mockNSBCR, started, notByAlarm, addTwice, ignoredMinPeriodMSec }
        };
    }

    @DataProvider
    public Object[][] updateScheduledSearchNotifyForNegativeTestsProvider() {
        BusMaster mockBM_notStarted = mock(BusMaster.class);
        when(mockBM_notStarted.getIsStarted()).thenReturn(false);

        BusMaster mockBM_started = mock(BusMaster.class);
        when(mockBM_started.getIsStarted()).thenReturn(true);

        NotifySearchBusCmdResult mockNSBCR = mock(NotifySearchBusCmdResult.class);

        return new Object[][]{
                {BusMaster.UpdateScheduledNotifySearchBusCmdResult.USNSBC_MinPeriodInvalid, mockNSBCR, mockBM_notStarted, false, -1},
                {BusMaster.UpdateScheduledNotifySearchBusCmdResult.USNSBC_NotifyObjNotAlreadyScheduled, null, mockBM_notStarted, false, 1},
        };
    }

    // STOPPED:
    // need CancelScheduledNegative - check cancel all found nothing
    // need UpdateScheduled Positive showing timing change, by alarm and not
    // Positive should demonstrate:
    //  callbacks occur at declared rate
    //  byAlarm flag is properly carried through
    //  the schedule rate told to the BM is what we told the NSBCR
    //  time writeCTM advances
    //  feed in different values for the returned values
    //  Have the crc32 NOT change on an update and show that we are not called a second time until it changes
    //
    // ? do we simulate errors on the BusMaster? Does that change the pusher?
    //
    // Simulate with two busmasters showing interleaving rates
    // Make calculateMinPeriodMSecFromMap public and have a full range of cases.

    @Test
    public void testScheduleNotifySearchBusCmdPositiveCases() {
        //
        // The NotifySearchBusCmdHelper will call to the BusMaster to query for a SearchBusCmd which will be executed
        // and expect to callback to the notifySearchResult() api which will then notify those NotifySearchCmdResult
        // instances.
        //
        BusMaster mockBM = mock(BusMaster.class);
        when(mockBM.getIsStarted()).thenReturn(true);

        mySearchBusCmd myCmdByAlarm = new mySearchBusCmd(mockBM, true);
        when(mockBM.querySearchBusByAlarmCmd(any(Logger.LogLevel.class))).thenReturn(myCmdByAlarm);

        ArrayList<String> resultList = new ArrayList<>();
        resultList.add("123456789");
        myCmdByAlarm.setExpectedReturn(SearchBusCmd.Result.success, resultList, 1);

        NotifySearchBusCmdHelper nsbch = new NotifySearchBusCmdHelper(mockBM, true);

        doNothing().when(mockBM).searchBusCmdExecuteCallback((SearchBusCmd) argThat(new ArgumentMatcher<SearchBusCmd>() {
            @Override
            public boolean matches(Object obj) {
                nsbch.notifySearchResult(((SearchBusCmd) obj).getResultData());
                return true;
            }

        }));

        NotifySearchBusCmdResult mockNSBCR = mock(NotifySearchBusCmdResult.class);

        BusMaster.ScheduleNotifySearchBusCmdResult result = nsbch.scheduleSearchNotifyFor(mockNSBCR, 50);
        Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success);


        verify(mockNSBCR, times(1)).notify(eq(mockBM), eq(true), any(SearchBusCmd.ResultData.class));

        Assert.assertTrue(nsbch.cancelAllScheduledSearchNotifyFor());
    }

    /*
    public void testExecuteBusy() {
        BusMaster mockBM = mock(BusMaster.class);
        Logger.LogLevel mockLogLevel = mock(Logger.LogLevel.class);

        TestStartBusCmd cmd = new TestStartBusCmd(mockBM, mockLogLevel);
        cmd.setResult(StartBusCmd.Result.busy);
        cmd.execute();

        Assert.fail("should have thrown exception");
    }
    */

    /*
    @DataProvider
    public Object[][] createExecuteInternalResultData() {
        return new Object[][] {
                { StartBusCmd.Result.already_started },
                { StartBusCmd.Result.bus_not_found },
                { StartBusCmd.Result.communication_error },
                { StartBusCmd.Result.started },
        };
    }
    */

}
