package waterfall.onewire;

/**
 * Created by dwaterfa on 7/23/17.
 */

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.Test;
import waterfall.onewire.busmaster.BusMaster;
import waterfall.onewire.busmaster.NotifySearchBusCmdResult;
import waterfall.onewire.busmaster.SearchBusCmd;


public class WaitForDeviceByAddressTest {

  private static String MOCK_BM_NAME = "mock_bm_name";

  @Test(expectedExceptions = {
      IllegalArgumentException.class}, expectedExceptionsMessageRegExp = "bmRegistry null")
  public void testWaitDeviceByAddressNullRegistry() {

    boolean typeByAlarm = false;

    new WaitForDeviceByAddress(null, typeByAlarm, 250);

    Assert.fail("expected exception");
  }

  @Test(expectedExceptions = {
      IllegalArgumentException.class}, expectedExceptionsMessageRegExp = "bmSearchPeriod less than 1")
  public void testWaitDeviceByAddressBadTimeout() {

    boolean typeByAlarm = false;

    new WaitForDeviceByAddress(new BusMasterRegistry(), typeByAlarm, -1);

    Assert.fail("expected exception");
  }

  @Test(expectedExceptions = {
      IllegalArgumentException.class}, expectedExceptionsMessageRegExp = "callback")
  public void testWaitDeviceByAddressNullCallback() {

    boolean typeByAlarm = false;

    new WaitForDeviceByAddress(new BusMasterRegistry(), typeByAlarm, 1)
        .addAddress(null, new DSAddress[0]);

    Assert.fail("expected exception");
  }

  @Test(expectedExceptions = {
      IllegalArgumentException.class}, expectedExceptionsMessageRegExp = "dsAddresses")
  public void testWaitDeviceByAddressNullAddresses() {

    boolean typeByAlarm = false;

    new WaitForDeviceByAddress(new BusMasterRegistry(), typeByAlarm, 1)
        .addAddress(new myCallback(), null);

    Assert.fail("expected exception");
  }

  @Test(expectedExceptions = {
      IllegalArgumentException.class}, expectedExceptionsMessageRegExp = "dup .*")
  public void testWaitDeviceByAddressDuplicateAddresses() {

    boolean typeByAlarm = false;

    new WaitForDeviceByAddress(new BusMasterRegistry(), typeByAlarm, 1)
        .addAddress(new myCallback(),
            new DSAddress[] {
                DSAddress.fromUncheckedHex(DSAddress.goodHexAddress1),
                DSAddress.fromUncheckedHex(DSAddress.goodHexAddress1)
            });

    Assert.fail("expected exception");
  }

  // ToDo
  // have another thread do the waiting and actually have there be a delay so to check the threading handoff
  // check for cancelling of the request after successfully finding it.
  // waitForDeviceByAddress with BM, bad value of for bmSearchPeriodMSec (exception) - THIS DELAYED until we get a BM. That seems bad - we need to check on the call.
  // waitForDeviceByAddress no BMs
  // waitForDeviceByAddress BMs added
  // waitForDeviceByAddress BMs removed

  @Test
  public void testWaitDeviceByAddressImmediateResult() {
    BusMasterRegistry bmR = new BusMasterRegistry();

    BusMaster mockBM = mock(BusMaster.class);

    when(mockBM.getName()).thenReturn(MOCK_BM_NAME);
    when(mockBM.getIsStarted()).thenReturn(true);
    when(mockBM.toString()).thenReturn(MOCK_BM_NAME);

    Assert.assertNotNull(bmR.getBusMasters());
    Assert.assertEquals(bmR.getBusMasters().length, 0);
    Assert.assertFalse(bmR.hasBusMasterByName(MOCK_BM_NAME));

    bmR.addBusMaster(mockBM);

    Assert.assertFalse(((Observable) bmR).hasChanged());

    Assert.assertNotNull(bmR.getBusMasters());
    Assert.assertEquals(bmR.getBusMasters().length, 1);
    Assert.assertEquals(bmR.getBusMasters()[0].getName(), MOCK_BM_NAME);
    Assert.assertTrue(bmR.hasBusMasterByName(MOCK_BM_NAME));

    DSAddress SEARCH_ADDRESS = DSAddress.fromUncheckedHex(DSAddress.goodHexAddress1);
    DSAddress SEARCH_ADDRESS_2 = DSAddress.fromUncheckedHex(DSAddress.goodHexAddress2);

    // this will call right away
    Answer<BusMaster.ScheduleNotifySearchBusCmdResult> answer = makeSearchBusCmdAnswerFor(mockBM,
        new DSAddress[]{SEARCH_ADDRESS, SEARCH_ADDRESS_2}, 0);

    when(mockBM.scheduleNotifySearchBusCmd(any(NotifySearchBusCmdResult.class), any(Boolean.class),
        any(Long.class))).thenAnswer(answer);

    // we will NOT cancel the search for each device after it is returned
    myCallback myCallback = new myCallback();

    WaitForDeviceByAddress wfdby = new WaitForDeviceByAddress(bmR, true, 250);
    wfdby.addAddress(myCallback, new DSAddress[]{SEARCH_ADDRESS, SEARCH_ADDRESS_2});

    BusMaster foundBM = myCallback.waitFor(SEARCH_ADDRESS);
    Assert.assertEquals(foundBM, mockBM);

    foundBM = myCallback.waitFor(SEARCH_ADDRESS_2);
    Assert.assertEquals(foundBM, mockBM);

    // the searches should still be in effect as we hae not cancelled
    verify(mockBM, times(0))
        .cancelScheduledNotifySearchBusCmd(any(NotifySearchBusCmdResult.class), any(Boolean.class));
  }

  @Test
  public void testWaitDeviceByAddressImmediateResultImmediateCancel() {
    BusMasterRegistry bmR = new BusMasterRegistry();

    BusMaster mockBM = mock(BusMaster.class);

    when(mockBM.getName()).thenReturn(MOCK_BM_NAME);
    when(mockBM.getIsStarted()).thenReturn(true);
    when(mockBM.toString()).thenReturn(MOCK_BM_NAME);

    Assert.assertNotNull(bmR.getBusMasters());
    Assert.assertEquals(bmR.getBusMasters().length, 0);
    Assert.assertFalse(bmR.hasBusMasterByName(MOCK_BM_NAME));

    bmR.addBusMaster(mockBM);

    Assert.assertFalse(((Observable) bmR).hasChanged());

    Assert.assertNotNull(bmR.getBusMasters());
    Assert.assertEquals(bmR.getBusMasters().length, 1);
    Assert.assertEquals(bmR.getBusMasters()[0].getName(), MOCK_BM_NAME);
    Assert.assertTrue(bmR.hasBusMasterByName(MOCK_BM_NAME));

    DSAddress SEARCH_ADDRESS = DSAddress.fromUncheckedHex(DSAddress.goodHexAddress1);
    DSAddress SEARCH_ADDRESS_2 = DSAddress.fromUncheckedHex(DSAddress.goodHexAddress2);

    // this will call right away
    Answer<BusMaster.ScheduleNotifySearchBusCmdResult> answer = makeSearchBusCmdAnswerFor(mockBM,
        new DSAddress[]{SEARCH_ADDRESS, SEARCH_ADDRESS_2}, 0);

    when(mockBM.scheduleNotifySearchBusCmd(any(NotifySearchBusCmdResult.class), any(Boolean.class),
        any(Long.class))).thenAnswer(answer);

    // we will cancel the search for each device after it is returned
    myCallback myCallback = new myCallback(true);

    WaitForDeviceByAddress wfdby = new WaitForDeviceByAddress(bmR, true, 250);
    wfdby.addAddress(myCallback, new DSAddress[]{SEARCH_ADDRESS, SEARCH_ADDRESS_2});

    BusMaster foundBM = myCallback.waitFor(SEARCH_ADDRESS);
    Assert.assertEquals(foundBM, mockBM);

    foundBM = myCallback.waitFor(SEARCH_ADDRESS_2);
    Assert.assertEquals(foundBM, mockBM);

    // therefore there should be no searches going on
    verify(mockBM, times(1))
        .cancelScheduledNotifySearchBusCmd(any(NotifySearchBusCmdResult.class), any(Boolean.class));
  }

  @Test
  public void testWaitDeviceByAddressImmediateResultImmediateCancelStartOver() {
    BusMasterRegistry bmR = new BusMasterRegistry();

    BusMaster mockBM = mock(BusMaster.class);

    when(mockBM.getName()).thenReturn(MOCK_BM_NAME);
    when(mockBM.getIsStarted()).thenReturn(true);
    when(mockBM.toString()).thenReturn(MOCK_BM_NAME);

    Assert.assertNotNull(bmR.getBusMasters());
    Assert.assertEquals(bmR.getBusMasters().length, 0);
    Assert.assertFalse(bmR.hasBusMasterByName(MOCK_BM_NAME));

    bmR.addBusMaster(mockBM);

    Assert.assertFalse(((Observable) bmR).hasChanged());

    Assert.assertNotNull(bmR.getBusMasters());
    Assert.assertEquals(bmR.getBusMasters().length, 1);
    Assert.assertEquals(bmR.getBusMasters()[0].getName(), MOCK_BM_NAME);
    Assert.assertTrue(bmR.hasBusMasterByName(MOCK_BM_NAME));

    DSAddress SEARCH_ADDRESS = DSAddress.fromUncheckedHex(DSAddress.goodHexAddress1);
    DSAddress SEARCH_ADDRESS_2 = DSAddress.fromUncheckedHex(DSAddress.goodHexAddress2);

    // this will call right away
    Answer<BusMaster.ScheduleNotifySearchBusCmdResult> answer = makeSearchBusCmdAnswerFor(mockBM,
        new DSAddress[]{SEARCH_ADDRESS, SEARCH_ADDRESS_2}, 0);

    when(mockBM.scheduleNotifySearchBusCmd(any(NotifySearchBusCmdResult.class), any(Boolean.class),
        any(Long.class))).thenAnswer(answer);

    // we will cancel the search for each device after it is returned
    myCallback myCallback = new myCallback(true);

    WaitForDeviceByAddress wfdby = new WaitForDeviceByAddress(bmR, true, 250);
    wfdby.addAddress(myCallback, new DSAddress[]{SEARCH_ADDRESS, SEARCH_ADDRESS_2});

    BusMaster foundBM = myCallback.waitFor(SEARCH_ADDRESS);
    Assert.assertEquals(foundBM, mockBM);

    foundBM = myCallback.waitFor(SEARCH_ADDRESS_2);
    Assert.assertEquals(foundBM, mockBM);

    // therefore there should be no searches going on
    verify(mockBM, times(1))
        .cancelScheduledNotifySearchBusCmd(any(NotifySearchBusCmdResult.class), any(Boolean.class));

    // Now start another one
    myCallback.clear();

    wfdby.addAddress(myCallback, new DSAddress[]{SEARCH_ADDRESS, SEARCH_ADDRESS_2});

    foundBM = myCallback.waitFor(SEARCH_ADDRESS);
    Assert.assertEquals(foundBM, mockBM);

    foundBM = myCallback.waitFor(SEARCH_ADDRESS_2);
    Assert.assertEquals(foundBM, mockBM);
  }


  // With the bus master already registered, we wait for a device to show up, which will happen after three
  // successive updates.
  @Test
  public void testWaitDeviceByAddressDelayResult() {
    BusMasterRegistry bmR = new BusMasterRegistry();

    BusMaster mockBM = mock(BusMaster.class);

    when(mockBM.getName()).thenReturn(MOCK_BM_NAME);
    when(mockBM.getIsStarted()).thenReturn(true);
    when(mockBM.toString()).thenReturn(MOCK_BM_NAME);

    Assert.assertNotNull(bmR.getBusMasters());
    Assert.assertEquals(bmR.getBusMasters().length, 0);
    Assert.assertFalse(bmR.hasBusMasterByName(MOCK_BM_NAME));

    bmR.addBusMaster(mockBM);

    Assert.assertFalse(((Observable) bmR).hasChanged());

    Assert.assertNotNull(bmR.getBusMasters());
    Assert.assertEquals(bmR.getBusMasters().length, 1);
    Assert.assertEquals(bmR.getBusMasters()[0].getName(), MOCK_BM_NAME);
    Assert.assertTrue(bmR.hasBusMasterByName(MOCK_BM_NAME));

    DSAddress SEARCH_ADDRESS = DSAddress.fromUncheckedHex(DSAddress.goodHexAddress1);
    DSAddress SEARCH_ADDRESS_2 = DSAddress.fromUncheckedHex(DSAddress.goodHexAddress2);
    DSAddress SEARCH_ADDRESS_3 = DSAddress.fromUncheckedHex(DSAddress.goodHexAddress3);

    // this will call back with one additional address every 100ms
    Answer<BusMaster.ScheduleNotifySearchBusCmdResult> answer = makeSearchBusCmdAnswerFor(mockBM,
        new DSAddress[]{SEARCH_ADDRESS, SEARCH_ADDRESS_2, SEARCH_ADDRESS_3}, 100);

    when(mockBM.scheduleNotifySearchBusCmd(any(NotifySearchBusCmdResult.class),
        any(Boolean.class), any(Long.class))).thenAnswer(answer);

    myCallback myCallback = new myCallback();

    // This will wait for this third address to show up.
    WaitForDeviceByAddress wfdby = new WaitForDeviceByAddress(bmR, false, 50);
    wfdby.addAddress(myCallback, new DSAddress[]{SEARCH_ADDRESS_3});

    BusMaster foundBM = myCallback.waitFor(SEARCH_ADDRESS_3);
    Assert.assertEquals(foundBM, mockBM);

    wfdby.cancelAddress(myCallback, SEARCH_ADDRESS_3);

    verify(mockBM, times(1))
        .cancelScheduledNotifySearchBusCmd(any(NotifySearchBusCmdResult.class), any(Boolean.class));
  }

  // With the bus master not already registered, we wait for a device to show up, which will happen after two
  // successive updates.
  @Test
  public void testWaitDeviceByAddressNoBusMasterFirst() {
    BusMasterRegistry bmR = new BusMasterRegistry();

    BusMaster mockBM = mock(BusMaster.class);

    when(mockBM.getName()).thenReturn(MOCK_BM_NAME);
    when(mockBM.getIsStarted()).thenReturn(true);
    when(mockBM.toString()).thenReturn(MOCK_BM_NAME);

    Assert.assertNotNull(bmR.getBusMasters());
    Assert.assertEquals(bmR.getBusMasters().length, 0);
    Assert.assertFalse(bmR.hasBusMasterByName(MOCK_BM_NAME));

    // We will wait tenth of second to add the busmaster
    new SleepAddBusMasterToRegistry(bmR, mockBM, 100L);

    DSAddress SEARCH_ADDRESS = DSAddress.fromUncheckedHex(DSAddress.goodHexAddress1);
    DSAddress SEARCH_ADDRESS_2 = DSAddress.fromUncheckedHex(DSAddress.goodHexAddress2);
    DSAddress SEARCH_ADDRESS_3 = DSAddress.fromUncheckedHex(DSAddress.goodHexAddress3);

    // this will call back with one additional address every 100ms
    Answer<BusMaster.ScheduleNotifySearchBusCmdResult> answer = makeSearchBusCmdAnswerFor(mockBM,
        new DSAddress[]{SEARCH_ADDRESS, SEARCH_ADDRESS_2, SEARCH_ADDRESS_3}, 100);

    when(mockBM.scheduleNotifySearchBusCmd(any(NotifySearchBusCmdResult.class), any(Boolean.class),
        any(Long.class))).thenAnswer(answer);

    myCallback myCallback = new myCallback();

    // This will wait for this third address to show up.
    WaitForDeviceByAddress wfdby = new WaitForDeviceByAddress(bmR, false, 50);
    wfdby.addAddress(myCallback, new DSAddress[]{SEARCH_ADDRESS_2});

    BusMaster foundBM = myCallback.waitFor(SEARCH_ADDRESS_2);
    Assert.assertEquals(foundBM, mockBM);

    wfdby.cancelAddress(myCallback, SEARCH_ADDRESS_2);

    verify(mockBM, times(1))
        .cancelScheduledNotifySearchBusCmd(any(NotifySearchBusCmdResult.class), any(Boolean.class));

    // And we assume the busmaster showed up
    Assert.assertFalse(((Observable) bmR).hasChanged());
    Assert.assertNotNull(bmR.getBusMasters());
    Assert.assertEquals(bmR.getBusMasters().length, 1);
    Assert.assertEquals(bmR.getBusMasters()[0].getName(), MOCK_BM_NAME);
    Assert.assertTrue(bmR.hasBusMasterByName(MOCK_BM_NAME));
  }

  //
  // Utility functions
  //

  //
  // This method will schedule search command returns, one for each resultAddress provided with an increment between
  // equal to the delayTimeMSec. Each return will incremently add one address without removing any of the others.
  //
  private Answer<BusMaster.ScheduleNotifySearchBusCmdResult> makeSearchBusCmdAnswerFor(BusMaster bm,
      DSAddress[] resultAddresses,
      long delayTimeMSec) {
    return new Answer<BusMaster.ScheduleNotifySearchBusCmdResult>() {
      @Override
      public BusMaster.ScheduleNotifySearchBusCmdResult answer(final InvocationOnMock invocation) {
        NotifySearchBusCmdResult obj = (NotifySearchBusCmdResult) (invocation.getArguments())[0];
        Boolean typeByAlarm = (Boolean) (invocation.getArguments())[1];
        Long minPeriodMSec = (Long) (invocation.getArguments())[2];

        for (int i = 0; i < resultAddresses.length; i++) {
          ArrayList<DSAddress> list = new ArrayList(i + 1);
          for (int j = 0; j <= i; j++) {
            list.add(resultAddresses[j]);
          }
          new SleepNotifySearchBusCmdResult(obj, bm, typeByAlarm,
              new mySearchBusCmd(bm, list).getResultData(), delayTimeMSec);
        }

        return BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success;
      }
    };
  }

  public static class myCallback implements WaitForDeviceByAddressCallback {

    private final WaitForDeviceByAddress wfdba;
    private final boolean cancelOnDeviceFound;
    private final HashMap<DSAddress, BusMaster> addrMap = new HashMap<>();

    public myCallback() {
      this.wfdba = null;
      this.cancelOnDeviceFound = false;
    }

    public myCallback(boolean cancelOnDeviceFound) {
      this.wfdba = null;
      this.cancelOnDeviceFound = cancelOnDeviceFound;
    }

    @Override
    public synchronized boolean deviceFound(BusMaster bm, DSAddress dsAddress,
        boolean typeByAlarm) {

      BusMaster existingBM = addrMap.get(dsAddress);
      Assert.assertTrue((existingBM == null) || (existingBM == bm));

      addrMap.put(dsAddress, bm);

      notify();

      if (wfdba != null) {
        wfdba.cancelAddress(this, dsAddress);
      }

      return cancelOnDeviceFound;
    }

    //
    // Our own
    //
    public void clear() {
      addrMap.clear();
    }

    public synchronized BusMaster waitFor(DSAddress dsAddress) {
      try {
        if (!addrMap.containsKey(dsAddress)) {
          wait(500);
        }
      } catch (InterruptedException e) {
        ;
      }

      return addrMap.get(dsAddress);
    }
  }

  private static class mySearchBusCmd extends SearchBusCmd {

    public mySearchBusCmd(BusMaster bm, List<DSAddress> resultList) {
      super(bm, false);
      result = Result.success;
      this.resultData = new ResultData(resultList, 123456);
    }

    protected Result execute_internal() {
      return null;
    }

    protected void setResultData(long resultWriteCTM, List<DSAddress> resultList) {
    }
  }

  private static class SleepAddBusMasterToRegistry implements Runnable {

    private final BusMasterRegistry bmRegistry;
    private final BusMaster bmToAdd;
    private final long sleepBeforeMSec;

    public SleepAddBusMasterToRegistry(BusMasterRegistry bmRegistry, BusMaster bmToAdd,
        long sleepBeforeMSec) {
      this.bmRegistry = bmRegistry;
      this.bmToAdd = bmToAdd;
      this.sleepBeforeMSec = sleepBeforeMSec;

      if (sleepBeforeMSec > 0) {
        new Thread(this).start();
      } else {
        run();
      }
    }

    public void run() {
      try {
        Thread.sleep(sleepBeforeMSec);
      } catch (InterruptedException e) {
        ;
      }

      bmRegistry.addBusMaster(bmToAdd);
    }

  }

  private static class SleepRemoveBusMasterFromRegistry implements Runnable {

    private final BusMasterRegistry bmRegistry;
    private final BusMaster bmToRemove;
    private final long sleepBeforeMSec;

    public SleepRemoveBusMasterFromRegistry(BusMasterRegistry bmRegistry, BusMaster bmToRemove,
        long sleepBeforeMSec) {
      this.bmRegistry = bmRegistry;
      this.bmToRemove = bmToRemove;
      this.sleepBeforeMSec = sleepBeforeMSec;

      if (sleepBeforeMSec > 0) {
        new Thread(this).start();
      } else {
        run();
      }
    }

    public void run() {
      try {
        Thread.sleep(sleepBeforeMSec);
      } catch (InterruptedException e) {
        ;
      }

      bmRegistry.removeBusMaster(bmToRemove);
    }

  }

  private static class SleepNotifySearchBusCmdResult implements Runnable {

    private final NotifySearchBusCmdResult nsbcr;
    private final BusMaster bm;
    private final boolean byAlarm;
    private final SearchBusCmd.ResultData resultData;
    private final long sleepBeforeMSec;

    public SleepNotifySearchBusCmdResult(NotifySearchBusCmdResult nsbcr, BusMaster bm,
        boolean byAlarm,
        SearchBusCmd.ResultData resultData, long sleepBeforeMSec) {
      this.nsbcr = nsbcr;
      this.bm = bm;
      this.byAlarm = byAlarm;
      this.resultData = resultData;
      this.sleepBeforeMSec = sleepBeforeMSec;

      if (sleepBeforeMSec > 0) {
        new Thread(this).start();
      } else {
        run();
      }
    }

    public void run() {

      if (sleepBeforeMSec > 0) {
        try {
          Thread.sleep(sleepBeforeMSec);
        } catch (InterruptedException e) {
          ;
        }
      }

      nsbcr.notify(bm, byAlarm, resultData);
    }
  }

}
