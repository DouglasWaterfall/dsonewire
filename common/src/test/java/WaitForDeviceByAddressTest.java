/**
 * Created by dwaterfa on 7/23/17.
 */

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.Test;
import waterfall.onewire.BusMasterRegistry;
import waterfall.onewire.WaitForDeviceByAddress;
import waterfall.onewire.busmaster.BusMaster;
import waterfall.onewire.busmaster.NotifySearchBusCmdResult;
import waterfall.onewire.busmaster.SearchBusCmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class WaitForDeviceByAddressTest {

    private static String MOCK_BM_NAME = "mock_bm_name";

    @Test(expectedExceptions = {IllegalArgumentException.class}, expectedExceptionsMessageRegExp = "bmRegistry null")
    public void testWaitDeviceByAddressNullRegistry() {

        new WaitForDeviceByAddress(null, 250);

        Assert.fail("expected exception");
    }

    @Test(expectedExceptions = {IllegalArgumentException.class}, expectedExceptionsMessageRegExp = "bmSearchPeriod less than 1")
    public void testWaitDeviceByAddressBadTimeout() {

        new WaitForDeviceByAddress(new BusMasterRegistry(), -1);

        Assert.fail("expected exception");
    }

    @Test(expectedExceptions = {IllegalArgumentException.class}, expectedExceptionsMessageRegExp = "address null or empty")
    public void testWaitDeviceByAddressNullAddress() {

        new WaitForDeviceByAddress(new BusMasterRegistry(), 1).waitForBM(null);

        Assert.fail("expected exception");
    }

    @Test(expectedExceptions = {IllegalArgumentException.class}, expectedExceptionsMessageRegExp = "address null or empty")
    public void testWaitDeviceByAddressEmptyAddress() {

        new WaitForDeviceByAddress(new BusMasterRegistry(), 1).waitForBM("");

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

        String SEARCH_ADDRESS = "MyAddress";
        String SEARCH_ADDRESS_2 = "MyAddress_2";

        // this will call right away
        Answer<BusMaster.ScheduleNotifySearchBusCmdResult> answer = makeAnswerFor(mockBM, new String[] { SEARCH_ADDRESS, SEARCH_ADDRESS_2 }, 0);

        when(mockBM.scheduleNotifySearchBusCmd(any(NotifySearchBusCmdResult.class), any(Boolean.class), any(Long.class))).thenAnswer(answer);

        WaitForDeviceByAddress wfdby = new WaitForDeviceByAddress(bmR, 250);

        BusMaster foundBM = wfdby.waitForBM(SEARCH_ADDRESS);
        Assert.assertEquals(foundBM, mockBM);

        foundBM = wfdby.waitForBM(SEARCH_ADDRESS_2);
        Assert.assertEquals(foundBM, mockBM);
    }

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

        String SEARCH_ADDRESS = "MyAddress";
        String SEARCH_ADDRESS_2 = "MyAddress_2";

        // this will call 100ms
        Answer<BusMaster.ScheduleNotifySearchBusCmdResult> answer = makeAnswerFor(mockBM, new String[] { SEARCH_ADDRESS, SEARCH_ADDRESS_2 }, 100);

        when(mockBM.scheduleNotifySearchBusCmd(any(NotifySearchBusCmdResult.class), any(Boolean.class), any(Long.class))).thenAnswer(answer);

        WaitForDeviceByAddress wfdby = new WaitForDeviceByAddress(bmR, 50);

        BusMaster foundBM = wfdby.waitForBM(SEARCH_ADDRESS);
        Assert.assertEquals(foundBM, mockBM);


        // Stopped - this path has the BM already registered so as soon we do the wait we add the observer and that
        // triggers a request for search.
        // The other test is to be waiting and then add the BM
        // Then we need to remove the BM and have that cancel the scheduled
        //
        // We should create some utility methods which would do the following:
        //  schedule NotifySearchBusCmd
        //  schedule AddBusMaster (To Registry)
        //  schedule RemoveBusMaster (To Registry)
        //  if the time delay is zero then it could do it right away and thus stay on the same thread.
        //
        // These would let us test the threading where someone is waiting for the new busmaster or the new devices

        foundBM = wfdby.waitForBM(SEARCH_ADDRESS_2);
        Assert.assertEquals(foundBM, mockBM);
    }

    //
    // Utility functions
    //
    private Answer<BusMaster.ScheduleNotifySearchBusCmdResult> makeAnswerFor(BusMaster bm, String[] resultAddresses,
                                                                     long delayTimeMSec) {
        return new Answer<BusMaster.ScheduleNotifySearchBusCmdResult>() {
            @Override
            public BusMaster.ScheduleNotifySearchBusCmdResult answer(final InvocationOnMock invocation) {
                NotifySearchBusCmdResult obj = (NotifySearchBusCmdResult) (invocation.getArguments())[0];
                Boolean typeByAlarm = (Boolean) (invocation.getArguments())[1];
                Long minPeriodMSec = (Long) (invocation.getArguments())[2];

                // call right away
                new SleepNotifySearchBusCmdResult(obj, bm, typeByAlarm, new mySearchBusCmd(bm, Arrays.asList(resultAddresses)).getResultData(), delayTimeMSec);

                return BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success;
            }
        };
    }

    private static class mySearchBusCmd extends SearchBusCmd {
        public mySearchBusCmd(BusMaster bm, List<String> resultList) {
            super(bm, false, null);
            result = Result.success;
            this.resultData = new ResultData(resultList, 123456);
        }

        protected Result execute_internal() {
            return null;
        }

        protected void setResultData(long resultWriteCTM, List<String> resultList) {
        }
    }

    private static class SleepAddBusMasterToRegistry implements Runnable {
        private final BusMasterRegistry bmRegistry;
        private final BusMaster bmToAdd;
        private final long sleepBeforeMSec;

        public SleepAddBusMasterToRegistry(BusMasterRegistry bmRegistry, BusMaster bmToAdd, long sleepBeforeMSec) {
            this.bmRegistry = bmRegistry;
            this.bmToAdd = bmToAdd;
            this.sleepBeforeMSec = sleepBeforeMSec;

            if (sleepBeforeMSec > 0) {
                new Thread(this).start();
            }
            else {
                run();
            }
        }

        public void run() {
            try {
                Thread.sleep(sleepBeforeMSec);
            }
            catch (InterruptedException e) {
                ;
            }

            bmRegistry.addBusMaster(bmToAdd);
        }

    }

    private static class SleepRemoveBusMasterFromRegistry implements Runnable {
        private final BusMasterRegistry bmRegistry;
        private final BusMaster bmToRemove;
        private final long sleepBeforeMSec;

        public SleepRemoveBusMasterFromRegistry(BusMasterRegistry bmRegistry, BusMaster bmToRemove, long sleepBeforeMSec) {
            this.bmRegistry = bmRegistry;
            this.bmToRemove = bmToRemove;
            this.sleepBeforeMSec = sleepBeforeMSec;

            if (sleepBeforeMSec > 0) {
                new Thread(this).start();
            }
            else {
                run();
            }
        }

        public void run() {
            try {
                Thread.sleep(sleepBeforeMSec);
            }
            catch (InterruptedException e) {
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

        public SleepNotifySearchBusCmdResult(NotifySearchBusCmdResult nsbcr, BusMaster bm, boolean byAlarm,
                                             SearchBusCmd.ResultData resultData, long sleepBeforeMSec) {
            this.nsbcr = nsbcr;
            this.bm = bm;
            this.byAlarm = byAlarm;
            this.resultData = resultData;
            this.sleepBeforeMSec = sleepBeforeMSec;

            if (sleepBeforeMSec > 0) {
                new Thread(this).start();
            }
            else {
                run();
            }
        }

        public void run() {

            if (sleepBeforeMSec > 0) {
                try {
                    Thread.sleep(sleepBeforeMSec);
                }
                catch (InterruptedException e) {
                    ;
                }
            }

            nsbcr.notify(bm, byAlarm, resultData);
        }
    }


}
