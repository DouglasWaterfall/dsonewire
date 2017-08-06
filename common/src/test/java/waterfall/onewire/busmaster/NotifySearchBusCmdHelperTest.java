package waterfall.onewire.busmaster;

import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import static org.mockito.Mockito.*;

/**
 * Created by dwaterfa on 7/31/17.
 */
public class NotifySearchBusCmdHelperTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testConstructorNullSearhPusher() {
        new NotifySearchBusCmdHelper(null, mock(BusMaster.class));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testConstructorNullBusMaster() {
        new NotifySearchBusCmdHelper(mock(SearchPusher.class), null);
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

        SearchPusher mockSearchPusher = mock(SearchPusher.class);
        when(mockSearchPusher.adjustPeriod(any(Long.class))).thenReturn(true);

        NotifySearchBusCmdHelper nsbch = new NotifySearchBusCmdHelper(mockSearchPusher, mockBM);

        if (addTwice) {
            BusMaster.ScheduleNotifySearchBusCmdResult result = nsbch.scheduleSearchNotifyFor(nsbcr, minPeriodMSec);
            Assert.assertEquals(BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success, result);
            verify(mockSearchPusher, times(1)).adjustPeriod(eq(minPeriodMSec));
        }

        BusMaster.ScheduleNotifySearchBusCmdResult result = nsbch.scheduleSearchNotifyFor(nsbcr, minPeriodMSec);
        Assert.assertEquals(result, expectedResult);

        nsbch.cancelAllScheduledSearchNotifyFor();
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

    @Test
    public void testScheduleNotifySearchBusCmdPositiveCases() {
        BusMaster mockBM = mock(BusMaster.class);
        when(mockBM.getIsStarted()).thenReturn(true);

        SearchPusher mockSearchPusher = mock(SearchPusher.class);
        when(mockSearchPusher.adjustPeriod(any(Long.class))).thenReturn(true);

        NotifySearchBusCmdHelper nsbch = new NotifySearchBusCmdHelper(mockSearchPusher, mockBM);

        NotifySearchBusCmdResult mockNSBCR = mock(NotifySearchBusCmdResult.class);

        BusMaster.ScheduleNotifySearchBusCmdResult s_result = nsbch.scheduleSearchNotifyFor(mockNSBCR, 100);
        Assert.assertEquals(s_result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success);
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(100L));

        Assert.assertTrue(nsbch.cancelAllScheduledSearchNotifyFor());
    }

    // TODO - do the two cases where we return adjustPeriod as true AND adjustPeriod as false

    @Test(dataProvider = "updateScheduledSearchNotifyForNegativeTestsProvider")
    public void testUpdateScheduledSearchNotifyForNegativeTests(BusMaster.UpdateScheduledNotifySearchBusCmdResult expectedResult,
                                                                NotifySearchBusCmdResult scheduleNSBCR,
                                                                long schedule_minPeriodMSec,
                                                                NotifySearchBusCmdResult updateNSBCR,
                                                                long update_minPeriodMSec) {
        BusMaster mockBM = mock(BusMaster.class);
        when(mockBM.getIsStarted()).thenReturn(true);

        SearchPusher mockSearchPusher = mock(SearchPusher.class);
        when(mockSearchPusher.adjustPeriod(any(Long.class))).thenReturn(true);

        NotifySearchBusCmdHelper nsbch = new NotifySearchBusCmdHelper(mockSearchPusher, mockBM);

        if (scheduleNSBCR != null) {
            BusMaster.ScheduleNotifySearchBusCmdResult s_result = nsbch.scheduleSearchNotifyFor(scheduleNSBCR, schedule_minPeriodMSec);
            Assert.assertEquals(s_result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success);
        }

        BusMaster.UpdateScheduledNotifySearchBusCmdResult result = nsbch.updateScheduledSearchNotifyFor(updateNSBCR, update_minPeriodMSec);
        Assert.assertEquals(result, expectedResult);
    }

    @DataProvider
    public Object[][] updateScheduledSearchNotifyForNegativeTestsProvider() {

        NotifySearchBusCmdResult mockNSBCR = mock(NotifySearchBusCmdResult.class);
        NotifySearchBusCmdResult mockNSBCR_2 = mock(NotifySearchBusCmdResult.class);

        // result, schedule object, schedule value, update object, update value
        return new Object[][]{
                {BusMaster.UpdateScheduledNotifySearchBusCmdResult.USNSBC_NotifyObjNotAlreadyScheduled, null, 100, null, 1},
                {BusMaster.UpdateScheduledNotifySearchBusCmdResult.USNSBC_NotifyObjNotAlreadyScheduled, null, 100, mockNSBCR, 1},

                {BusMaster.UpdateScheduledNotifySearchBusCmdResult.USNSBC_MinPeriodInvalid, mockNSBCR, 100, mockNSBCR, -1},

                {BusMaster.UpdateScheduledNotifySearchBusCmdResult.USNSBC_MinPeriodUnchanged, mockNSBCR, 100, mockNSBCR, 100}
        };
    }

    @Test
    public void testUpdateScheduledSearchNotifyForPositiveCases() {
        BusMaster mockBM = mock(BusMaster.class);
        when(mockBM.getIsStarted()).thenReturn(true);

        SearchPusher mockSearchPusher = mock(SearchPusher.class);
        when(mockSearchPusher.adjustPeriod(any(Long.class))).thenReturn(true);

        NotifySearchBusCmdHelper nsbch = new NotifySearchBusCmdHelper(mockSearchPusher, mockBM);

        NotifySearchBusCmdResult mockNSBCR = mock(NotifySearchBusCmdResult.class);
        long mockNSBCR_period = 100;

        NotifySearchBusCmdResult mockNSBCR_2 = mock(NotifySearchBusCmdResult.class);
        long mockNSBCR_2_period = 50;

        BusMaster.ScheduleNotifySearchBusCmdResult s_result = nsbch.scheduleSearchNotifyFor(mockNSBCR, mockNSBCR_period);
        Assert.assertEquals(s_result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success);
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(mockNSBCR_period));

        s_result = nsbch.scheduleSearchNotifyFor(mockNSBCR_2, mockNSBCR_2_period);
        Assert.assertEquals(s_result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success);
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(mockNSBCR_period));
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(mockNSBCR_2_period));

        long mockNSBCR_2_shorter_period = 25;

        BusMaster.UpdateScheduledNotifySearchBusCmdResult u_result = nsbch.updateScheduledSearchNotifyFor(mockNSBCR_2, mockNSBCR_2_shorter_period);
        Assert.assertEquals(u_result, BusMaster.UpdateScheduledNotifySearchBusCmdResult.USNSBC_Success);
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(mockNSBCR_period));
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(mockNSBCR_2_period));
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(mockNSBCR_2_shorter_period));

        long mockNSBCR_2_longer_period = 150;

        u_result = nsbch.updateScheduledSearchNotifyFor(mockNSBCR_2, mockNSBCR_2_longer_period);
        Assert.assertEquals(u_result, BusMaster.UpdateScheduledNotifySearchBusCmdResult.USNSBC_Success);
        verify(mockSearchPusher, times(2)).adjustPeriod(eq(mockNSBCR_period));
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(mockNSBCR_2_period));
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(mockNSBCR_2_shorter_period));

        // cancel them all
        Assert.assertTrue(nsbch.cancelAllScheduledSearchNotifyFor());
        verify(mockSearchPusher, times(2)).adjustPeriod(eq(mockNSBCR_period));
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(mockNSBCR_2_period));
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(mockNSBCR_2_shorter_period));
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(Long.MAX_VALUE));
    }

    @Test
    public void testCancelScheduledSearchNotifyFor() {
        BusMaster mockBM = mock(BusMaster.class);
        when(mockBM.getIsStarted()).thenReturn(true);

        SearchPusher mockSearchPusher = mock(SearchPusher.class);
        when(mockSearchPusher.adjustPeriod(any(Long.class))).thenReturn(true);

        NotifySearchBusCmdHelper nsbch = new NotifySearchBusCmdHelper(mockSearchPusher, mockBM);

        // null
        BusMaster.CancelScheduledNotifySearchBusCmdResult result = nsbch.cancelScheduledSearchNotifyFor(null);
        Assert.assertEquals(result, BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_NotifyObjNotAlreadyScheduled);

        // not previously scheduled
        NotifySearchBusCmdResult mockNSBCR = mock(NotifySearchBusCmdResult.class);
        result = nsbch.cancelScheduledSearchNotifyFor(mockNSBCR);
        Assert.assertEquals(result, BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_NotifyObjNotAlreadyScheduled);

        // schedule and then remove
        BusMaster.ScheduleNotifySearchBusCmdResult s_result = nsbch.scheduleSearchNotifyFor(mockNSBCR, 100);
        Assert.assertEquals(s_result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success);
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(100L));
        result = nsbch.cancelScheduledSearchNotifyFor(mockNSBCR);
        Assert.assertEquals(result, BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_Success);
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(Long.MAX_VALUE));
        Assert.assertFalse(nsbch.cancelAllScheduledSearchNotifyFor());

        // schedule two and watch the adjustments
        reset(mockSearchPusher);
        s_result = nsbch.scheduleSearchNotifyFor(mockNSBCR, 100);
        Assert.assertEquals(s_result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success);
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(100L));
        NotifySearchBusCmdResult mockNSBCR_2 = mock(NotifySearchBusCmdResult.class);
        s_result = nsbch.scheduleSearchNotifyFor(mockNSBCR_2, 50);
        Assert.assertEquals(s_result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success);
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(100L));
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(50L));
        NotifySearchBusCmdResult mockNSBCR_3 = mock(NotifySearchBusCmdResult.class);
        s_result = nsbch.scheduleSearchNotifyFor(mockNSBCR_3, 150);
        Assert.assertEquals(s_result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success);
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(100L));
        verify(mockSearchPusher, times(2)).adjustPeriod(eq(50L));

        // now cancel and watch them adjust again
        reset(mockSearchPusher);
        result = nsbch.cancelScheduledSearchNotifyFor(mockNSBCR); // 100
        Assert.assertEquals(result, BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_Success);
        verify(mockSearchPusher, times(0)).adjustPeriod(eq(100L));
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(50L));
        result = nsbch.cancelScheduledSearchNotifyFor(mockNSBCR_2); // 50
        Assert.assertEquals(result, BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_Success);
        verify(mockSearchPusher, times(0)).adjustPeriod(eq(100L));
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(50L));
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(150L));
        result = nsbch.cancelScheduledSearchNotifyFor(mockNSBCR_3); // 150
        Assert.assertEquals(result, BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_Success);
        verify(mockSearchPusher, times(0)).adjustPeriod(eq(100L));
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(50L));
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(150L));
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(Long.MAX_VALUE));
        Assert.assertFalse(nsbch.cancelAllScheduledSearchNotifyFor());
    }

    @Test
    public void testCancelAllScheduledSearchNotifyFor() {
        BusMaster mockBM = mock(BusMaster.class);
        when(mockBM.getIsStarted()).thenReturn(true);

        SearchPusher mockSearchPusher = mock(SearchPusher.class);
        when(mockSearchPusher.adjustPeriod(any(Long.class))).thenReturn(true);

        NotifySearchBusCmdHelper nsbch = new NotifySearchBusCmdHelper(mockSearchPusher, mockBM);

        // nothing was scheduled
        Assert.assertFalse(nsbch.cancelAllScheduledSearchNotifyFor());
        verify(mockSearchPusher, times(0)).adjustPeriod(any(Long.class));

        // schedule something then cancel
        NotifySearchBusCmdResult mockNSBCR = mock(NotifySearchBusCmdResult.class);
        BusMaster.ScheduleNotifySearchBusCmdResult s_result = nsbch.scheduleSearchNotifyFor(mockNSBCR, 100);
        Assert.assertEquals(s_result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success);
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(100L));

        Assert.assertTrue(nsbch.cancelAllScheduledSearchNotifyFor());
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(100L));
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(Long.MAX_VALUE));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNotifySearchResultBadArguments() {
        BusMaster mockBM = mock(BusMaster.class);
        when(mockBM.getIsStarted()).thenReturn(true);

        SearchPusher mockSearchPusher = mock(SearchPusher.class);
        when(mockSearchPusher.adjustPeriod(any(Long.class))).thenReturn(true);

        NotifySearchBusCmdHelper nsbch = new NotifySearchBusCmdHelper(mockSearchPusher, mockBM);
        nsbch.notifySearchResult(null);
    }

    @Test
    public void testNotifySearchResult() {
        BusMaster mockBM = mock(BusMaster.class);
        when(mockBM.getIsStarted()).thenReturn(true);

        SearchPusher mockSearchPusher = mock(SearchPusher.class);

        NotifySearchBusCmdHelper nsbch = new NotifySearchBusCmdHelper(mockSearchPusher, mockBM);

        boolean isByAlarm = true;
        when(mockSearchPusher.isAlarmSearch()).thenReturn(isByAlarm);

        String oneDevice = "123456";
        List<String> resultList = new ArrayList<String>();
        resultList.add(oneDevice);
        long writeCTM = 54321;

        mySearchBusCmd mySearchBusCmd = new mySearchBusCmd(mockBM, isByAlarm);
        mySearchBusCmd.setExpectedReturn(SearchBusCmd.Result.success, resultList, writeCTM);
        mySearchBusCmd.execute(); // each execute will generate a new ResultData

        nsbch.notifySearchResult(mySearchBusCmd.getResultData());
        // nothing to verify here, really, since nobody is actually registered

        // the schedule will NOT notify the caller of the last update because of the mock on the SearchPusher
        // which will return that a new push was started. This will also clear the previously known state.
        when(mockSearchPusher.adjustPeriod(any(Long.class))).thenReturn(true);
        NotifySearchBusCmdResult mockNSBCR = mock(NotifySearchBusCmdResult.class);
        BusMaster.ScheduleNotifySearchBusCmdResult s_result = nsbch.scheduleSearchNotifyFor(mockNSBCR, 100);
        Assert.assertEquals(s_result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success);
        delayForPush();
        verify(mockNSBCR, times(0)).notify(any(BusMaster.class), any(Boolean.class), any(SearchBusCmd.ResultData.class));

        // another notify, we expect a new callback to this mock even though it is the same result data.
        nsbch.notifySearchResult(mySearchBusCmd.getResultData());
        delayForPush();
        {
            ArgumentCaptor<SearchBusCmd.ResultData> resultDataArgs2 = ArgumentCaptor.forClass(SearchBusCmd.ResultData.class);
            verify(mockNSBCR, times(1)).notify(eq(mockBM), eq(isByAlarm), resultDataArgs2.capture());
            Assert.assertEquals(resultDataArgs2.getAllValues().size(), 1);
            Assert.assertEquals(resultDataArgs2.getAllValues().get(0).getWriteCTM(), writeCTM);
        }

        // now let us change the data a bit which will trigger a new instance of
        String twoDevice = "78910";
        resultList.add(twoDevice);
        long writeCTM_update = 654321;
        mySearchBusCmd.setExpectedReturn(SearchBusCmd.Result.success, resultList, writeCTM_update);
        mySearchBusCmd.execute();

        // and push again
        nsbch.notifySearchResult(mySearchBusCmd.getResultData());
        delayForPush();
        {
            ArgumentCaptor<SearchBusCmd.ResultData> resultDataArgs3 = ArgumentCaptor.forClass(SearchBusCmd.ResultData.class);
            verify(mockNSBCR, times(2)).notify(eq(mockBM), eq(isByAlarm), resultDataArgs3.capture());
            Assert.assertEquals(resultDataArgs3.getAllValues().size(), 2);
            Assert.assertEquals(resultDataArgs3.getAllValues().get(0).getWriteCTM(), writeCTM);
            Assert.assertEquals(resultDataArgs3.getAllValues().get(1).getWriteCTM(), writeCTM_update);
        }
    }

    // TODO: calculateMinPeriodMSecForMap needs some tests.

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

    // this just lets the push thread get a chance to run
    private void delayForPush() {
        try {
            Thread.sleep(5);
        }
        catch (InterruptedException e) {
            ;
        }
    }

}
