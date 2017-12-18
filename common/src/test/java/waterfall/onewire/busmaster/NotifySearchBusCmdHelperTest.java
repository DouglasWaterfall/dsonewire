package waterfall.onewire.busmaster;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.mockito.ArgumentCaptor;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import waterfall.onewire.DSAddress;

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
  public void testScheduleNotifySearchBusCmdNegativeTests(String expectedExceptionMessage,
      NotifySearchBusCmdResult nsbcr, boolean isStarted, boolean byAlarm, boolean addTwice,
      long minPeriodMSec) {
    BusMaster mockBM = mock(BusMaster.class);
    if (isStarted) {
      when(mockBM.getIsStarted()).thenReturn(true);
      mySearchBusCmd mySearchBusCmd = new mySearchBusCmd(mockBM, byAlarm);
      when(mockBM.querySearchBusByAlarmCmd()).thenReturn(mySearchBusCmd);
    }

    SearchPusher mockSearchPusher = mock(SearchPusher.class);
    when(mockSearchPusher.adjustPeriod(any(Long.class))).thenReturn(true);

    NotifySearchBusCmdHelper nsbch = new NotifySearchBusCmdHelper(mockSearchPusher, mockBM);

    if (addTwice) {
      try {
        nsbch.scheduleSearchNotifyFor(nsbcr, minPeriodMSec);
        verify(mockSearchPusher, times(1)).adjustPeriod(eq(minPeriodMSec));
      } catch (IllegalArgumentException e) {
        Assert.fail("Unexpected exception:" + e);
      }
    }

    try {
      nsbch.scheduleSearchNotifyFor(nsbcr, minPeriodMSec);
      Assert.fail("Expected exception");
    } catch (IllegalArgumentException e) {
      Assert.assertEquals(e.getMessage(), expectedExceptionMessage);
    }

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
        {"SNSBCR_NotifyObjNull", null, notStarted, byAlarm,
            addOnce, ignoredMinPeriodMSec},
        {"SNSBCR_NotifyObjNull", null, notStarted,
            notByAlarm, addOnce, ignoredMinPeriodMSec},

        {"SNSBCR_MinPeriodInvalid", mockNSBCR, notStarted,
            byAlarm, addOnce, invalidMinPeriodMSec},
        {"SNSBCR_MinPeriodInvalid", mockNSBCR, notStarted,
            notByAlarm, addOnce, invalidMinPeriodMSec},

        {"SNSBCR_BusMasterNotStarted", mockNSBCR,
            notStarted, byAlarm, addOnce, ignoredMinPeriodMSec},
        {"SNSBCR_BusMasterNotStarted", mockNSBCR,
            notStarted, notByAlarm, addOnce, ignoredMinPeriodMSec},

        {"SNSBCR_NotifyObjAlreadyScheduled", mockNSBCR,
            started, byAlarm, addTwice, ignoredMinPeriodMSec},
        {"SNSBCR_NotifyObjAlreadyScheduled", mockNSBCR,
            started, notByAlarm, addTwice, ignoredMinPeriodMSec}
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

    try {
      nsbch.scheduleSearchNotifyFor(mockNSBCR, 100);
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(100L));
    } catch (IllegalArgumentException e) {
      Assert.fail("Unexpected exception:" + e);
    }

    Assert.assertTrue(nsbch.cancelAllScheduledSearchNotifyFor());
  }

  // TODO - do the two cases where we return adjustPeriod as true AND adjustPeriod as false

  @Test(dataProvider = "updateScheduledSearchNotifyForNegativeTestsProvider")
  public void testUpdateScheduledSearchNotifyForNegativeTests(
      String expectedExceptionMessage,
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
      try {
        nsbch.scheduleSearchNotifyFor(scheduleNSBCR, schedule_minPeriodMSec);
      } catch (IllegalArgumentException e) {
        Assert.fail("Unexpected excpetion");
      }
    }

    try {
      nsbch.updateScheduledSearchNotifyFor(updateNSBCR, update_minPeriodMSec);
    } catch (IllegalArgumentException e) {
      Assert.assertEquals(e.getMessage(), expectedExceptionMessage);
    }
  }

  @DataProvider
  public Object[][] updateScheduledSearchNotifyForNegativeTestsProvider() {

    NotifySearchBusCmdResult mockNSBCR = mock(NotifySearchBusCmdResult.class);
    NotifySearchBusCmdResult mockNSBCR_2 = mock(NotifySearchBusCmdResult.class);

    // result, schedule object, schedule value, update object, update value
    return new Object[][]{
        {"USNSBC_NotifyObjNotAlreadyScheduled", null, 100, null, 1},
        {"USNSBC_NotifyObjNotAlreadyScheduled", null, 100, mockNSBCR, 1},
        {"USNSBC_MinPeriodInvalid", mockNSBCR, 100, mockNSBCR, -1},
        {"USNSBC_MinPeriodUnchanged", mockNSBCR, 100, mockNSBCR, 100}
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

    try {
      nsbch.scheduleSearchNotifyFor(mockNSBCR, mockNSBCR_period);
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(mockNSBCR_period));

      nsbch.scheduleSearchNotifyFor(mockNSBCR_2, mockNSBCR_2_period);
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(mockNSBCR_period));
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(mockNSBCR_2_period));

      long mockNSBCR_2_shorter_period = 25;

      nsbch.updateScheduledSearchNotifyFor(mockNSBCR_2, mockNSBCR_2_shorter_period);
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(mockNSBCR_period));
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(mockNSBCR_2_period));
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(mockNSBCR_2_shorter_period));

      long mockNSBCR_2_longer_period = 150;

      nsbch.updateScheduledSearchNotifyFor(mockNSBCR_2, mockNSBCR_2_longer_period);
      verify(mockSearchPusher, times(2)).adjustPeriod(eq(mockNSBCR_period));
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(mockNSBCR_2_period));
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(mockNSBCR_2_shorter_period));
      // cancel them all
      Assert.assertTrue(nsbch.cancelAllScheduledSearchNotifyFor());
      verify(mockSearchPusher, times(2)).adjustPeriod(eq(mockNSBCR_period));
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(mockNSBCR_2_period));
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(mockNSBCR_2_shorter_period));
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(Long.MAX_VALUE));
    } catch (IllegalArgumentException e) {
      Assert.fail("Unexpected exception:" + e);
    }
  }

  @Test
  public void testCancelScheduledSearchNotifyFor() {
    BusMaster mockBM = mock(BusMaster.class);
    when(mockBM.getIsStarted()).thenReturn(true);

    SearchPusher mockSearchPusher = mock(SearchPusher.class);
    when(mockSearchPusher.adjustPeriod(any(Long.class))).thenReturn(true);

    NotifySearchBusCmdHelper nsbch = new NotifySearchBusCmdHelper(mockSearchPusher, mockBM);

    // null
    try {
      nsbch.cancelScheduledSearchNotifyFor(null);
    } catch (IllegalArgumentException e) {
      Assert.assertEquals(e.getMessage(), "CSNSBC_NotifyObjNotAlreadyScheduled");
    }

    // not previously scheduled
    NotifySearchBusCmdResult mockNSBCR = mock(NotifySearchBusCmdResult.class);
    try {
      nsbch.cancelScheduledSearchNotifyFor(mockNSBCR);
    } catch (IllegalArgumentException e) {
      Assert.assertEquals(e.getMessage(), "CSNSBC_NotifyObjNotAlreadyScheduled");
    }

    // schedule and then remove
    try {
      nsbch.scheduleSearchNotifyFor(mockNSBCR, 100);
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(100L));
      nsbch.cancelScheduledSearchNotifyFor(mockNSBCR);
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(Long.MAX_VALUE));
      Assert.assertFalse(nsbch.cancelAllScheduledSearchNotifyFor());

      // schedule two and watch the adjustments
      reset(mockSearchPusher);

      nsbch.scheduleSearchNotifyFor(mockNSBCR, 100);
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(100L));
      NotifySearchBusCmdResult mockNSBCR_2 = mock(NotifySearchBusCmdResult.class);
      nsbch.scheduleSearchNotifyFor(mockNSBCR_2, 50);
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(100L));
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(50L));
      NotifySearchBusCmdResult mockNSBCR_3 = mock(NotifySearchBusCmdResult.class);
      nsbch.scheduleSearchNotifyFor(mockNSBCR_3, 150);
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(100L));
      verify(mockSearchPusher, times(2)).adjustPeriod(eq(50L));

      // now cancel and watch them adjust again
      reset(mockSearchPusher);
      nsbch.cancelScheduledSearchNotifyFor(mockNSBCR); // 100
      verify(mockSearchPusher, times(0)).adjustPeriod(eq(100L));
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(50L));
      nsbch.cancelScheduledSearchNotifyFor(mockNSBCR_2); // 50
      verify(mockSearchPusher, times(0)).adjustPeriod(eq(100L));
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(50L));
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(150L));
      nsbch.cancelScheduledSearchNotifyFor(mockNSBCR_3); // 150
      verify(mockSearchPusher, times(0)).adjustPeriod(eq(100L));
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(50L));
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(150L));
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(Long.MAX_VALUE));
      Assert.assertFalse(nsbch.cancelAllScheduledSearchNotifyFor());
    } catch (IllegalArgumentException e) {
      Assert.fail("Unexpected exception:" + e);
    }
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
    try {
      NotifySearchBusCmdResult mockNSBCR = mock(NotifySearchBusCmdResult.class);
      nsbch.scheduleSearchNotifyFor(mockNSBCR, 100);
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(100L));

      Assert.assertTrue(nsbch.cancelAllScheduledSearchNotifyFor());
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(100L));
      verify(mockSearchPusher, times(1)).adjustPeriod(eq(Long.MAX_VALUE));
    } catch (IllegalArgumentException e) {
      Assert.fail("Unexpected exception:" + e);
    }
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

    List<DSAddress> resultList = new ArrayList<>();
    resultList.add(DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28));
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
    try {
      nsbch.scheduleSearchNotifyFor(mockNSBCR, 100);
    } catch (IllegalArgumentException e) {
      Assert.fail("Unexpected exception:" + e);
    }
    delayForPush();
    verify(mockNSBCR, times(0))
        .notify(any(BusMaster.class), any(Boolean.class), any(SearchBusCmd.ResultData.class));

    // another notify, we expect a new callback to this mock even though it is the same result data.
    nsbch.notifySearchResult(mySearchBusCmd.getResultData());
    delayForPush();
    {
      ArgumentCaptor<SearchBusCmd.ResultData> resultDataArgs2 = ArgumentCaptor
          .forClass(SearchBusCmd.ResultData.class);
      verify(mockNSBCR, times(1)).notify(eq(mockBM), eq(isByAlarm), resultDataArgs2.capture());
      Assert.assertEquals(resultDataArgs2.getAllValues().size(), 1);
      Assert.assertEquals(resultDataArgs2.getAllValues().get(0).getWriteCTM(), writeCTM);
    }

    // now let us change the data a bit which will trigger a new instance of
    resultList.add(DSAddress.fromUncheckedHex(DSAddress._090000065BD53528));
    long writeCTM_update = 654321;
    mySearchBusCmd.setExpectedReturn(SearchBusCmd.Result.success, resultList, writeCTM_update);
    mySearchBusCmd.execute();

    // and push again
    nsbch.notifySearchResult(mySearchBusCmd.getResultData());
    delayForPush();
    {
      ArgumentCaptor<SearchBusCmd.ResultData> resultDataArgs3 = ArgumentCaptor
          .forClass(SearchBusCmd.ResultData.class);
      verify(mockNSBCR, times(2)).notify(eq(mockBM), eq(isByAlarm), resultDataArgs3.capture());
      Assert.assertEquals(resultDataArgs3.getAllValues().size(), 2);
      Assert.assertEquals(resultDataArgs3.getAllValues().get(0).getWriteCTM(), writeCTM);
      Assert.assertEquals(resultDataArgs3.getAllValues().get(1).getWriteCTM(), writeCTM_update);
    }
  }

  // TODO: calculateMinPeriodMSecForMap needs some tests.

  // this just lets the push thread get a chance to run
  private void delayForPush() {
    try {
      Thread.sleep(5);
    } catch (InterruptedException e) {
      ;
    }
  }

  private class mySearchBusCmd extends SearchBusCmd {

    SearchBusCmd.Result expected_result = null;
    List<DSAddress> expected_resultList = null;
    long expected_writeCTM = 0;

    public mySearchBusCmd(BusMaster bm, boolean byAlarm) {
      super(bm, byAlarm);
    }

    public mySearchBusCmd(BusMaster bm, short familyCode) {
      super(bm, familyCode);
    }

    public SearchBusCmd.Result execute_internal() {
      result = expected_result;
      if (result == Result.success) {
        setResultData(new ResultData(expected_resultList, expected_writeCTM));
      }
      return result;
    }

    public void setResultData(ResultData resultData) {
      this.resultData = resultData;
    }

    // class specific
    public void setExpectedReturn(SearchBusCmd.Result expected_result,
        List<DSAddress> expected_resultList,
        long expected_writeCTM) {
      this.expected_result = expected_result;
      this.expected_resultList = expected_resultList;
      this.expected_writeCTM = expected_writeCTM;
    }

  }

}
