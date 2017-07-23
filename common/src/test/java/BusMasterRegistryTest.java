/**
 * Created by dwaterfa on 7/22/17.
 *
 *
 * public void addBusMaster(BusMaster bm)

 public synchronized BusMaster[] getBusMasters()
 public synchronized boolean hasBusMasterByName(String bmName)

 public class BusMasterAdded

 public void removeBusMaster(BusMaster bm)

 public synchronized BusMaster[] getBusMasters()
 public synchronized boolean hasBusMasterByName(String bmName)

 public class BusMasterRemoved

 * Add an Observer to the Registry. We will update it with all the known BusMasters.
 public void addObserver(Observer o)

 BusMaster waitForDeviceByAddress(String address, long bmSearchPeriodMSec)
 */
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

import waterfall.onewire.BusMasterRegistry;
import waterfall.onewire.busmaster.BusMaster;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.NotifySearchBusCmdResult;
import waterfall.onewire.busmaster.SearchBusCmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.zip.CRC32;

public class BusMasterRegistryTest {

    private static String MOCK_BM_NAME = "mock_bm_name";

    @Test
    public void testNoBusMasters() {
        BusMasterRegistry bmR = new BusMasterRegistry();

        BusMaster[] bmList = bmR.getBusMasters();

        Assert.assertNotNull(bmList);
        Assert.assertEquals(bmList.length, 0);

        Assert.assertFalse(bmR.hasBusMasterByName("foo"));
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void testAddNullBusMaster() {
        BusMasterRegistry bmR = new BusMasterRegistry();

        bmR.addBusMaster(null);

        Assert.fail("should have thrown exception");
    }

    @Test (expectedExceptions = { IllegalArgumentException.class }, expectedExceptionsMessageRegExp = "bm does not have a name")
    public void testAddNoNameBusMaster() {
        BusMasterRegistry bmR = new BusMasterRegistry();

        BusMaster mockBM = mock(BusMaster.class);

        bmR.addBusMaster(mockBM);

        Assert.fail("should have thrown exception");
    }

    @Test (expectedExceptions = { IllegalArgumentException.class }, expectedExceptionsMessageRegExp = "bm not started")
    public void testAddNotStartedBusMaster() {
        BusMasterRegistry bmR = new BusMasterRegistry();

        BusMaster mockBM = mock(BusMaster.class);

        when(mockBM.getName()).thenReturn(MOCK_BM_NAME);
        when(mockBM.getIsStarted()).thenReturn(false);

        bmR.addBusMaster(mockBM);

        Assert.fail("should have thrown exception");
    }

    @Test (expectedExceptions = { IllegalArgumentException.class }, expectedExceptionsMessageRegExp = "bm already known to the registry")
    public void testAddNotDuplicateBusMaster() {
        BusMasterRegistry bmR = new BusMasterRegistry();

        BusMaster mockBM = mock(BusMaster.class);

        when(mockBM.getName()).thenReturn(MOCK_BM_NAME);
        when(mockBM.getIsStarted()).thenReturn(true);

        bmR.addBusMaster(mockBM);

        Assert.assertNotNull(bmR.getBusMasters());
        Assert.assertEquals(bmR.getBusMasters().length, 1);
        Assert.assertTrue(bmR.hasBusMasterByName(MOCK_BM_NAME));

        bmR.addBusMaster(mockBM);

        Assert.fail("should have thrown exception");
    }

    @Test
    public void testAddBusMasterFound() {
        BusMasterRegistry bmR = new BusMasterRegistry();

        BusMaster mockBM = mock(BusMaster.class);

        when(mockBM.getName()).thenReturn(MOCK_BM_NAME);
        when(mockBM.getIsStarted()).thenReturn(true);

        Assert.assertFalse(((Observable)bmR).hasChanged());

        Assert.assertNotNull(bmR.getBusMasters());
        Assert.assertEquals(bmR.getBusMasters().length, 0);
        Assert.assertFalse(bmR.hasBusMasterByName(MOCK_BM_NAME));

        bmR.addBusMaster(mockBM);

        Assert.assertFalse(((Observable)bmR).hasChanged());

        Assert.assertNotNull(bmR.getBusMasters());
        Assert.assertEquals(bmR.getBusMasters().length, 1);
        Assert.assertEquals(bmR.getBusMasters()[0].getName(), MOCK_BM_NAME);
        Assert.assertTrue(bmR.hasBusMasterByName(MOCK_BM_NAME));
    }

    @Test
    public void testAddBusMasterNotifiesIfRegisteredBefore() {
        BusMasterRegistry bmR = new BusMasterRegistry();

        BusMaster mockBM = mock(BusMaster.class);

        when(mockBM.getName()).thenReturn(MOCK_BM_NAME);
        when(mockBM.getIsStarted()).thenReturn(true);

        Assert.assertNotNull(bmR.getBusMasters());
        Assert.assertEquals(bmR.getBusMasters().length, 0);

        Observer mockO = mock(Observer.class);
        ArgumentCaptor<Observable> arg1 = ArgumentCaptor.forClass(Observable.class);
        ArgumentCaptor<Object> arg2 = ArgumentCaptor.forClass(Object.class);

        bmR.addObserver(mockO);

        bmR.addBusMaster(mockBM);

        Assert.assertFalse(((Observable)bmR).hasChanged());

        Assert.assertNotNull(bmR.getBusMasters());
        Assert.assertEquals(bmR.getBusMasters().length, 1);
        Assert.assertEquals(bmR.getBusMasters()[0].getName(), MOCK_BM_NAME);
        Assert.assertTrue(bmR.hasBusMasterByName(MOCK_BM_NAME));

        verify(mockO).update(arg1.capture(), arg2.capture());
        Assert.assertEquals(arg1.getValue(), bmR);
        Assert.assertTrue(arg2.getValue() instanceof BusMasterRegistry.BusMasterAdded);
    }

    @Test
    public void testAddBusMasterNotifiesIfRegisteredAfter() {
        BusMasterRegistry bmR = new BusMasterRegistry();

        BusMaster mockBM = mock(BusMaster.class);

        when(mockBM.getName()).thenReturn(MOCK_BM_NAME);
        when(mockBM.getIsStarted()).thenReturn(true);

        Assert.assertNotNull(bmR.getBusMasters());
        Assert.assertEquals(bmR.getBusMasters().length, 0);

        bmR.addBusMaster(mockBM);

        Assert.assertFalse(((Observable)bmR).hasChanged());

        Assert.assertNotNull(bmR.getBusMasters());
        Assert.assertEquals(bmR.getBusMasters().length, 1);
        Assert.assertEquals(bmR.getBusMasters()[0].getName(), MOCK_BM_NAME);
        Assert.assertTrue(bmR.hasBusMasterByName(MOCK_BM_NAME));

        Observer mockO = mock(Observer.class);
        ArgumentCaptor<Observable> arg1 = ArgumentCaptor.forClass(Observable.class);
        ArgumentCaptor<Object> arg2 = ArgumentCaptor.forClass(Object.class);

        bmR.addObserver(mockO);

        BusMaster mockBM_2 = mock(BusMaster.class);

        when(mockBM_2.getName()).thenReturn(MOCK_BM_NAME + 2);
        when(mockBM_2.getIsStarted()).thenReturn(true);

        bmR.addBusMaster(mockBM_2);

        Assert.assertNotNull(bmR.getBusMasters());
        Assert.assertEquals(bmR.getBusMasters().length, 2);
        Assert.assertTrue(bmR.hasBusMasterByName(MOCK_BM_NAME));
        Assert.assertTrue(bmR.hasBusMasterByName(MOCK_BM_NAME + 2));

        verify(mockO, times(2)).update(arg1.capture(), arg2.capture());
        Assert.assertEquals(arg1.getAllValues().size(), 2);
        Assert.assertEquals(arg2.getAllValues().size(), 2);
        Assert.assertEquals(arg1.getAllValues().get(0), bmR);
        Assert.assertTrue(arg2.getAllValues().get(0) instanceof BusMasterRegistry.BusMasterAdded);
        Assert.assertEquals(((BusMasterRegistry.BusMasterAdded)arg2.getAllValues().get(0)).getBusMaster(), mockBM);
        Assert.assertEquals(arg1.getAllValues().get(1), bmR);
        Assert.assertTrue(arg2.getAllValues().get(1) instanceof BusMasterRegistry.BusMasterAdded);
        Assert.assertEquals(((BusMasterRegistry.BusMasterAdded)arg2.getAllValues().get(1)).getBusMaster(), mockBM_2);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void testRemoveNullBusMaster() {
        BusMasterRegistry bmR = new BusMasterRegistry();

        bmR.removeBusMaster(null);

        Assert.fail("should have thrown exception");
    }

    @Test (expectedExceptions = { IllegalArgumentException.class }, expectedExceptionsMessageRegExp = "bm not known to the registry")
    public void testRemoveUnknownBusMaster() {
        BusMasterRegistry bmR = new BusMasterRegistry();

        BusMaster mockBM = mock(BusMaster.class);

        when(mockBM.getName()).thenReturn(MOCK_BM_NAME);
        when(mockBM.getIsStarted()).thenReturn(true);

        Assert.assertNotNull(bmR.getBusMasters());
        Assert.assertEquals(bmR.getBusMasters().length, 0);
        Assert.assertFalse(bmR.hasBusMasterByName(MOCK_BM_NAME));

        bmR.removeBusMaster(mockBM);

        Assert.fail("should have thrown exception");
    }

    @Test
    public void testRemoveBusMaster() {
        BusMasterRegistry bmR = new BusMasterRegistry();

        BusMaster mockBM = mock(BusMaster.class);

        when(mockBM.getName()).thenReturn(MOCK_BM_NAME);
        when(mockBM.getIsStarted()).thenReturn(true);

        Assert.assertNotNull(bmR.getBusMasters());
        Assert.assertEquals(bmR.getBusMasters().length, 0);
        Assert.assertFalse(bmR.hasBusMasterByName(MOCK_BM_NAME));

        bmR.addBusMaster(mockBM);

        Assert.assertFalse(((Observable) bmR).hasChanged());

        Assert.assertNotNull(bmR.getBusMasters());
        Assert.assertEquals(bmR.getBusMasters().length, 1);
        Assert.assertEquals(bmR.getBusMasters()[0].getName(), MOCK_BM_NAME);
        Assert.assertTrue(bmR.hasBusMasterByName(MOCK_BM_NAME));

        BusMaster mockBM_2 = mock(BusMaster.class);

        when(mockBM_2.getName()).thenReturn(MOCK_BM_NAME + 2);
        when(mockBM_2.getIsStarted()).thenReturn(true);

        bmR.addBusMaster(mockBM_2);

        Assert.assertNotNull(bmR.getBusMasters());
        Assert.assertEquals(bmR.getBusMasters().length, 2);
        Assert.assertTrue(bmR.hasBusMasterByName(MOCK_BM_NAME));
        Assert.assertTrue(bmR.hasBusMasterByName(MOCK_BM_NAME + 2));

        bmR.removeBusMaster(mockBM);

        Assert.assertNotNull(bmR.getBusMasters());
        Assert.assertEquals(bmR.getBusMasters().length, 1);
        Assert.assertTrue(bmR.hasBusMasterByName(MOCK_BM_NAME + 2));

        bmR.removeBusMaster(mockBM_2);

        Assert.assertNotNull(bmR.getBusMasters());
        Assert.assertEquals(bmR.getBusMasters().length, 0);
    }

    private static class mySearchBusCmd extends SearchBusCmd {
        public mySearchBusCmd(BusMaster bm, List<String> resultList) {
            super(bm, false, null);
            result = Result.success;
            this.resultData = new ResultData(resultList, 123456);
        }

        protected Result execute_internal() { return null; }
        protected void setResultData(long resultWriteCTM, List<String> resultList) { }
    }

    @Test
    public void testWaitDeviceByAddress() {
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

        Answer<BusMaster.ScheduleNotifySearchBusCmdResult> answer = new Answer<BusMaster.ScheduleNotifySearchBusCmdResult>() {
            @Override
            public BusMaster.ScheduleNotifySearchBusCmdResult answer(final InvocationOnMock invocation) throws Throwable {
                NotifySearchBusCmdResult obj = (NotifySearchBusCmdResult) (invocation.getArguments())[0];
                Boolean typeByAlarm = (Boolean) (invocation.getArguments())[1];
                Long minPeriodMSec = (Long) (invocation.getArguments())[2];

                ArrayList<String> list = new ArrayList<String>();
                list.add(SEARCH_ADDRESS);
                list.add(SEARCH_ADDRESS_2);

                obj.notify(mockBM, typeByAlarm, new mySearchBusCmd(mockBM, list).getResultData());

                return BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success;
            }
        };

        when(mockBM.scheduleNotifySearchBusCmd(any(NotifySearchBusCmdResult.class), any(Boolean.class), any(Long.class))).thenAnswer(answer);

        BusMaster foundBM = bmR.waitForDeviceByAddress(SEARCH_ADDRESS, 250);
        Assert.assertEquals(foundBM, mockBM);

        foundBM = bmR.waitForDeviceByAddress(SEARCH_ADDRESS_2, 250);
        Assert.assertEquals(foundBM, mockBM);
    }

}
