package waterfall.onewire.busmasters.HA7S;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.Test;
import waterfall.onewire.busmaster.*;

/**
 * Created by dwaterfa on 1/29/17.
 */
public class HA7STest {

    public static final long periodNegativeOneMSec = -1;
    public static final long periodZeroMSec = 0;
    public static final long periodOneMSec = 1;
    public static final long period250MSec = 250;
    public static final long period500MSec = 500;
    public static final long period750MSec = 750;

    @Test
    public void testConstructorNullSerial() {
        HA7S ha7s = null;

        try {
            ha7s = new HA7S(null);
            Assert.fail("Exception expected");
        }
        catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testConstructorDummySerial() {
        HA7S ha7s = null;

        try {
            ha7s = new HA7S(new HA7SSerialDummy("port"));
        }
        catch (Exception e) {
            Assert.fail("Exception not expected");
        }

        Assert.assertNotNull(ha7s);
        Assert.assertNotNull(ha7s.getName());
        Assert.assertTrue(ha7s.getName().startsWith("HA7S on "));

        long ctm = ha7s.getCurrentTimeMillis();
        try {
            Thread.sleep(100);
        }
        catch (InterruptedException e) {
            Assert.fail("Unexpected exception");
        }
        Assert.assertTrue(ctm < ha7s.getCurrentTimeMillis());

        Assert.assertFalse(ha7s.getIsStarted());
    }

    @Test
    public void testStartCmd() {
        HA7S ha7s = new HA7S(new HA7SSerialDummy("port"));

        Assert.assertFalse(ha7s.getIsStarted());

        StartBusCmd startCmd = ha7s.queryStartBusCmd();
        Assert.assertNotNull(startCmd);

        try {
            StartBusCmd.Result startResult = startCmd.execute();
            Assert.assertEquals(startResult, StartBusCmd.Result.started);
        }
        catch (Exception e) {
            Assert.fail("Unexpected exception");
        }

        Assert.assertTrue(ha7s.getIsStarted());

        try {
            StartBusCmd.Result startResult = startCmd.execute();
            Assert.assertEquals(startResult, StartBusCmd.Result.already_started);
        }
        catch (Exception e) {
            Assert.fail("Unexpected exception");
        }
    }

    @Test
    public void testStopCmd() {
        HA7S ha7s = new HA7S(new HA7SSerialDummy("port"));

        Assert.assertFalse(ha7s.getIsStarted());

        StopBusCmd stopBusCmd = ha7s.queryStopBusCmd();

        Assert.assertNotNull(stopBusCmd);

        try {
            StopBusCmd.Result result = stopBusCmd.execute();
            Assert.assertEquals(result, StopBusCmd.Result.not_started);
        }
        catch (Exception e) {
            Assert.fail("Unexpected exception");
        }

        {
            StartBusCmd startCmd = ha7s.queryStartBusCmd();
            Assert.assertNotNull(startCmd);

            try {
                StartBusCmd.Result startResult = startCmd.execute();
                Assert.assertEquals(startResult, StartBusCmd.Result.started);
            } catch (Exception e) {
                Assert.fail("Unexpected exception");
            }

            Assert.assertTrue(ha7s.getIsStarted());
        }

        try {
            StopBusCmd.Result result = stopBusCmd.execute();
            Assert.assertEquals(result, StopBusCmd.Result.stopped);
        }
        catch (Exception e) {
            Assert.fail("Unexpected exception");
        }

        try {
            StopBusCmd.Result result = stopBusCmd.execute();
            Assert.assertEquals(result, StopBusCmd.Result.not_started);
        }
        catch (Exception e) {
            Assert.fail("Unexpected exception");
        }
    }

    @Test
    public void testSearchCmd() {
        HA7SSerialDummy serialDummy = new HA7SSerialDummy("port");

        HA7S ha7s = new HA7S(serialDummy);

        SearchBusCmd searchBusCmd = ha7s.querySearchBusCmd();
        Assert.assertNotNull(searchBusCmd);
        Assert.assertFalse(searchBusCmd.isByAlarm());
        Assert.assertFalse(searchBusCmd.isByFamilyCode());
        Assert.assertNull(searchBusCmd.getResult());

        try {
            SearchBusCmd.Result result = searchBusCmd.execute();
            Assert.assertEquals(result, SearchBusCmd.Result.bus_not_started);
        } catch (Exception e) {
            Assert.fail("Unexpected exception");
        }

        {
            StartBusCmd startCmd = ha7s.queryStartBusCmd();
            Assert.assertNotNull(startCmd);

            try {
                StartBusCmd.Result startResult = startCmd.execute();
                Assert.assertEquals(startResult, StartBusCmd.Result.started);
            } catch (Exception e) {
                Assert.fail("Unexpected exception");
            }

            Assert.assertTrue(ha7s.getIsStarted());
        }

        // By default the dummy has no entries.
        try {
            SearchBusCmd.Result result = searchBusCmd.execute();
            Assert.assertEquals(result, SearchBusCmd.Result.success);
        } catch (Exception e) {
            Assert.fail("Unexpected exception");
        }

        Assert.assertEquals(searchBusCmd.getResult(), SearchBusCmd.Result.success);
        Assert.assertNotNull(searchBusCmd.getResultList());
        Assert.assertEquals(searchBusCmd.getResultList().size(), 0);
        Assert.assertEquals(searchBusCmd.getResultListCRC32(), 0);
        Assert.assertTrue(searchBusCmd.getResultWriteCTM() < ha7s.getCurrentTimeMillis());

        long lastResultWriteCTM = searchBusCmd.getResultWriteCTM();

        final String dev_A = "EE0000065BC0AE28";
        final String dev_B = "090000065BD53528";
        final String dev_C = "5F0000065CCD1A28";

        serialDummy.addDevice(dev_A, false);
        serialDummy.addDevice(dev_B, false);
        serialDummy.addDevice(dev_C, false);

        try {
            SearchBusCmd.Result result = searchBusCmd.execute();
            Assert.assertEquals(result, SearchBusCmd.Result.success);
        } catch (Exception e) {
            Assert.fail("Unexpected exception");
        }

        Assert.assertEquals(searchBusCmd.getResult(), SearchBusCmd.Result.success);
        Assert.assertNotNull(searchBusCmd.getResultList());
        Assert.assertEquals(searchBusCmd.getResultList().size(), 3);
        Assert.assertTrue(searchBusCmd.getResultListCRC32() != 0);
        Assert.assertTrue(searchBusCmd.getResultWriteCTM() > lastResultWriteCTM);
        Assert.assertTrue(searchBusCmd.getResultList().contains(dev_A) &&
                            searchBusCmd.getResultList().contains(dev_B) &&
                            searchBusCmd.getResultList().contains(dev_C));

        lastResultWriteCTM = searchBusCmd.getResultWriteCTM();

        serialDummy.removeDevice(dev_B);

        try {
            SearchBusCmd.Result result = searchBusCmd.execute();
            Assert.assertEquals(result, SearchBusCmd.Result.success);
        } catch (Exception e) {
            Assert.fail("Unexpected exception");
        }

        Assert.assertEquals(searchBusCmd.getResult(), SearchBusCmd.Result.success);
        Assert.assertNotNull(searchBusCmd.getResultList());
        Assert.assertEquals(searchBusCmd.getResultList().size(), 2);
        Assert.assertTrue(searchBusCmd.getResultListCRC32() != 0);
        Assert.assertTrue(searchBusCmd.getResultWriteCTM() > lastResultWriteCTM);
        Assert.assertTrue(searchBusCmd.getResultList().contains(dev_A) &&
                searchBusCmd.getResultList().contains(dev_C));

        lastResultWriteCTM = searchBusCmd.getResultWriteCTM();

        serialDummy.removeAllDevices();

        try {
            SearchBusCmd.Result result = searchBusCmd.execute();
            Assert.assertEquals(result, SearchBusCmd.Result.success);
        } catch (Exception e) {
            Assert.fail("Unexpected exception");
        }

        Assert.assertEquals(searchBusCmd.getResult(), SearchBusCmd.Result.success);
        Assert.assertNotNull(searchBusCmd.getResultList());
        Assert.assertEquals(searchBusCmd.getResultList().size(), 0);
        Assert.assertEquals(searchBusCmd.getResultListCRC32(), 0);
        Assert.assertTrue(searchBusCmd.getResultWriteCTM() > lastResultWriteCTM);
    }

    @Test
    public void testAlarmSearchCmd() {
        HA7SSerialDummy serialDummy = new HA7SSerialDummy("port");

        HA7S ha7s = new HA7S(serialDummy);

        SearchBusCmd searchBusCmd = ha7s.querySearchBusByAlarmCmd();
        Assert.assertNotNull(searchBusCmd);
        Assert.assertTrue(searchBusCmd.isByAlarm());
        Assert.assertFalse(searchBusCmd.isByFamilyCode());
        Assert.assertNull(searchBusCmd.getResult());

        try {
            SearchBusCmd.Result result = searchBusCmd.execute();
            Assert.assertEquals(result, SearchBusCmd.Result.bus_not_started);
        } catch (Exception e) {
            Assert.fail("Unexpected exception");
        }

        {
            StartBusCmd startCmd = ha7s.queryStartBusCmd();
            Assert.assertNotNull(startCmd);

            try {
                StartBusCmd.Result startResult = startCmd.execute();
                Assert.assertEquals(startResult, StartBusCmd.Result.started);
            } catch (Exception e) {
                Assert.fail("Unexpected exception");
            }

            Assert.assertTrue(ha7s.getIsStarted());
        }

        // By default the dummy has no entries.
        try {
            SearchBusCmd.Result result = searchBusCmd.execute();
            Assert.assertEquals(result, SearchBusCmd.Result.success);
        } catch (Exception e) {
            Assert.fail("Unexpected exception");
        }

        Assert.assertEquals(searchBusCmd.getResult(), SearchBusCmd.Result.success);
        Assert.assertNotNull(searchBusCmd.getResultList());
        Assert.assertEquals(searchBusCmd.getResultList().size(), 0);
        Assert.assertEquals(searchBusCmd.getResultListCRC32(), 0);
        Assert.assertTrue(searchBusCmd.getResultWriteCTM() < ha7s.getCurrentTimeMillis());

        long lastResultWriteCTM = searchBusCmd.getResultWriteCTM();

        final String dev_A = "EE0000065BC0AE28";
        final String dev_B = "090000065BD53528";
        final String dev_C = "5F0000065CCD1A28";

        serialDummy.addDevice(dev_A, false);
        serialDummy.addDevice(dev_B, true);
        serialDummy.addDevice(dev_C, true);

        try {
            SearchBusCmd.Result result = searchBusCmd.execute();
            Assert.assertEquals(result, SearchBusCmd.Result.success);
        } catch (Exception e) {
            Assert.fail("Unexpected exception");
        }

        Assert.assertEquals(searchBusCmd.getResult(), SearchBusCmd.Result.success);
        Assert.assertNotNull(searchBusCmd.getResultList());
        Assert.assertEquals(searchBusCmd.getResultList().size(), 2);
        Assert.assertTrue(searchBusCmd.getResultListCRC32() != 0);
        Assert.assertTrue(searchBusCmd.getResultWriteCTM() > lastResultWriteCTM);
        Assert.assertTrue(searchBusCmd.getResultList().contains(dev_B) &&
                searchBusCmd.getResultList().contains(dev_C));

        lastResultWriteCTM = searchBusCmd.getResultWriteCTM();

        // dev_A false
        serialDummy.removeDevice(dev_B);
        // dev_C true

        try {
            SearchBusCmd.Result result = searchBusCmd.execute();
            Assert.assertEquals(result, SearchBusCmd.Result.success);
        } catch (Exception e) {
            Assert.fail("Unexpected exception");
        }

        Assert.assertEquals(searchBusCmd.getResult(), SearchBusCmd.Result.success);
        Assert.assertNotNull(searchBusCmd.getResultList());
        Assert.assertEquals(searchBusCmd.getResultList().size(), 1);
        Assert.assertTrue(searchBusCmd.getResultListCRC32() != 0);
        Assert.assertTrue(searchBusCmd.getResultWriteCTM() > lastResultWriteCTM);
        Assert.assertTrue(searchBusCmd.getResultList().contains(dev_C));

        lastResultWriteCTM = searchBusCmd.getResultWriteCTM();

        serialDummy.removeDevice(dev_A);
        serialDummy.addDevice(dev_A, true);
        serialDummy.addDevice(dev_B, false);
        // dev_C true

        try {
            SearchBusCmd.Result result = searchBusCmd.execute();
            Assert.assertEquals(result, SearchBusCmd.Result.success);
        } catch (Exception e) {
            Assert.fail("Unexpected exception");
        }

        Assert.assertEquals(searchBusCmd.getResult(), SearchBusCmd.Result.success);
        Assert.assertNotNull(searchBusCmd.getResultList());
        Assert.assertEquals(searchBusCmd.getResultList().size(), 2);
        Assert.assertTrue(searchBusCmd.getResultListCRC32() != 0);
        Assert.assertTrue(searchBusCmd.getResultWriteCTM() > lastResultWriteCTM);
        Assert.assertTrue(searchBusCmd.getResultList().contains(dev_A) &&
                            searchBusCmd.getResultList().contains(dev_C));

        lastResultWriteCTM = searchBusCmd.getResultWriteCTM();

        serialDummy.removeDevice(dev_A);
        // dev_B false
        serialDummy.removeDevice(dev_C);

        try {
            SearchBusCmd.Result result = searchBusCmd.execute();
            Assert.assertEquals(result, SearchBusCmd.Result.success);
        } catch (Exception e) {
            Assert.fail("Unexpected exception");
        }

        Assert.assertEquals(searchBusCmd.getResult(), SearchBusCmd.Result.success);
        Assert.assertNotNull(searchBusCmd.getResultList());
        Assert.assertEquals(searchBusCmd.getResultList().size(), 0);
        Assert.assertEquals(searchBusCmd.getResultListCRC32(), 0);
        Assert.assertTrue(searchBusCmd.getResultWriteCTM() > lastResultWriteCTM);

        try {
            SearchBusCmd.Result result = searchBusCmd.execute();
            Assert.assertEquals(result, SearchBusCmd.Result.success);
        } catch (Exception e) {
            Assert.fail("Unexpected exception");
        }

        lastResultWriteCTM = searchBusCmd.getResultWriteCTM();

        serialDummy.removeAllDevices();

        try {
            SearchBusCmd.Result result = searchBusCmd.execute();
            Assert.assertEquals(result, SearchBusCmd.Result.success);
        } catch (Exception e) {
            Assert.fail("Unexpected exception");
        }

        Assert.assertEquals(searchBusCmd.getResult(), SearchBusCmd.Result.success);
        Assert.assertNotNull(searchBusCmd.getResultList());
        Assert.assertEquals(searchBusCmd.getResultList().size(), 0);
        Assert.assertEquals(searchBusCmd.getResultListCRC32(), 0);
        Assert.assertTrue(searchBusCmd.getResultWriteCTM() > lastResultWriteCTM);
    }

    @Test
    public void testSearchFamilyCmd() {
        HA7SSerialDummy serialDummy = new HA7SSerialDummy("port");

        HA7S ha7s = new HA7S(serialDummy);

        short familyCode = 0x27;

        SearchBusCmd searchBusCmd = ha7s.querySearchBusByFamilyCmd(familyCode);
        Assert.assertNotNull(searchBusCmd);
        Assert.assertFalse(searchBusCmd.isByAlarm());
        Assert.assertTrue(searchBusCmd.isByFamilyCode());
        Assert.assertNull(searchBusCmd.getResult());

        try {
            SearchBusCmd.Result result = searchBusCmd.execute();
            Assert.assertEquals(result, SearchBusCmd.Result.bus_not_started);
        } catch (Exception e) {
            Assert.fail("Unexpected exception");
        }

        {
            StartBusCmd startCmd = ha7s.queryStartBusCmd();
            Assert.assertNotNull(startCmd);

            try {
                StartBusCmd.Result startResult = startCmd.execute();
                Assert.assertEquals(startResult, StartBusCmd.Result.started);
            } catch (Exception e) {
                Assert.fail("Unexpected exception");
            }

            Assert.assertTrue(ha7s.getIsStarted());
        }

        // By default the dummy has no entries.
        try {
            SearchBusCmd.Result result = searchBusCmd.execute();
            Assert.assertEquals(result, SearchBusCmd.Result.success);
        } catch (Exception e) {
            Assert.fail("Unexpected exception");
        }

        Assert.assertEquals(searchBusCmd.getResult(), SearchBusCmd.Result.success);
        Assert.assertNotNull(searchBusCmd.getResultList());
        Assert.assertEquals(searchBusCmd.getResultList().size(), 0);
        Assert.assertEquals(searchBusCmd.getResultListCRC32(), 0);
        Assert.assertTrue(searchBusCmd.getResultWriteCTM() < ha7s.getCurrentTimeMillis());

        long lastResultWriteCTM = searchBusCmd.getResultWriteCTM();

        final String dev_A = "EE0000065BC0AE28";
        final String dev_B = "090000065BD53528";
        final String dev_C = "5F0000065CCD1A28";

        serialDummy.addDevice(dev_A, false);
        serialDummy.addDevice(dev_B, false);
        serialDummy.addDevice(dev_C, false);

        try {
            SearchBusCmd.Result result = searchBusCmd.execute();
            Assert.assertEquals(result, SearchBusCmd.Result.success);
        } catch (Exception e) {
            Assert.fail("Unexpected exception");
        }

        Assert.assertEquals(searchBusCmd.getResult(), SearchBusCmd.Result.success);
        Assert.assertNotNull(searchBusCmd.getResultList());
        Assert.assertEquals(searchBusCmd.getResultList().size(), 0);
        Assert.assertEquals(searchBusCmd.getResultListCRC32(), 0);
        Assert.assertTrue(searchBusCmd.getResultWriteCTM() > lastResultWriteCTM);

        lastResultWriteCTM = searchBusCmd.getResultWriteCTM();

        final String dev_D = "7B0000063B759F27";
        serialDummy.addDevice(dev_D, false);

        try {
            SearchBusCmd.Result result = searchBusCmd.execute();
            Assert.assertEquals(result, SearchBusCmd.Result.success);
        } catch (Exception e) {
            Assert.fail("Unexpected exception");
        }

        Assert.assertEquals(searchBusCmd.getResult(), SearchBusCmd.Result.success);
        Assert.assertNotNull(searchBusCmd.getResultList());
        Assert.assertEquals(searchBusCmd.getResultList().size(), 1);
        Assert.assertTrue(searchBusCmd.getResultListCRC32() != 0);
        Assert.assertTrue(searchBusCmd.getResultWriteCTM() > lastResultWriteCTM);
        Assert.assertTrue(searchBusCmd.getResultList().contains(dev_D));

        lastResultWriteCTM = searchBusCmd.getResultWriteCTM();

        serialDummy.removeAllDevices();

        try {
            SearchBusCmd.Result result = searchBusCmd.execute();
            Assert.assertEquals(result, SearchBusCmd.Result.success);
        } catch (Exception e) {
            Assert.fail("Unexpected exception");
        }

        Assert.assertEquals(searchBusCmd.getResult(), SearchBusCmd.Result.success);
        Assert.assertNotNull(searchBusCmd.getResultList());
        Assert.assertEquals(searchBusCmd.getResultList().size(), 0);
        Assert.assertEquals(searchBusCmd.getResultListCRC32(), 0);
        Assert.assertTrue(searchBusCmd.getResultWriteCTM() > lastResultWriteCTM);
    }

    /*
     * Here is helpful site with which to fabricate addresses:
     * http://www.datastat.com/sysadminjournal/maximcrc.cgi
     *
     * The trick is when calculating the crc8 you need to do it in reverse order, that is from right
     * to left of the address as we maintain it. If you do the 14 bytes what will be left will be a
     * two byte CRC value. This is what can then be the FIRST two nibbles of the full address.

    "EE0000065BC0AE28",
            "090000065BD53528",
            "5F0000065CCD1A28",
            "260000065BE22D28",
            "7C0000063BB13028",
            "5A0000063B7AF528",
            "AA0000063BF51928",
            "390000063B759F28",
            "7F0000063BA12F28"

    */
    
    public class myNotifySearchBusCmdResult implements NotifySearchBusCmdResult {
        public class Data {

            public Data(int notifyCount, BusMaster bm, boolean byAlarm, SearchBusCmd.ResultData searchResultData) {
                this.notifyCount = notifyCount;
                this.bm = bm;
                this.byAlarm = byAlarm;
                this.searchResultData = searchResultData;
            }

            public int notifyCount;
            public BusMaster bm;
            public Boolean byAlarm;
            public SearchBusCmd.ResultData searchResultData;
        }

        private int notifyCount;
        private Data data;

        private HA7SSerialDummy serialDummy;
        private String updateOnNotifyDev;
        private boolean updateOnNotifyAlarm;
        private boolean updateOnNotifyDoAdd;

        public myNotifySearchBusCmdResult() {
            notifyCount = -1;
            data = null;
            serialDummy = null;
            updateOnNotifyDev = null;
            updateOnNotifyAlarm = false;
            updateOnNotifyDoAdd = false;
        }

        public synchronized void notify(BusMaster bm, boolean byAlarm, SearchBusCmd.ResultData searchResultData) {
            data = new Data(++notifyCount, bm, byAlarm, searchResultData);

            if (serialDummy != null) {
                if (updateOnNotifyDoAdd) {
                    serialDummy.addDevice(updateOnNotifyDev, updateOnNotifyAlarm);
                }
                else {
                    serialDummy.removeDevice(updateOnNotifyDev);
                }
                updateOnNotifyDoAdd = !updateOnNotifyDoAdd;
            }
        }

        public synchronized Data getData() {
            return data;
        }

        public boolean wait500MSecForNotifyChange(int afterNotifyCount) {
            return internalWaitForNotifyChange(5, 100, afterNotifyCount);
        }

        public boolean wait1000MSecForNotifyChange(int afterNotifyCount) {
            return internalWaitForNotifyChange(5, 200, afterNotifyCount);
        }

        private boolean internalWaitForNotifyChange(int count, long timeMSec, int afterNotifyCount) {
            for (int i = 0; i < count; i++) {
                Data t_data = getData();
                if ((t_data != null) && (t_data.notifyCount != afterNotifyCount)) {
                    return true;
                }
                try {
                    Thread.sleep(timeMSec);
                } catch (InterruptedException e) {

                }
            }
            return false;
        }

        public synchronized void setAddRemoveEveryNotify(HA7SSerialDummy serialDummy, String dev, boolean activeAlarm) {
            if (this.serialDummy != null) {
                throw new IllegalArgumentException("already registered");
            }

            this.serialDummy = serialDummy;
            updateOnNotifyDev = dev;
            updateOnNotifyAlarm = activeAlarm;
            updateOnNotifyDoAdd = true;
        }

        public synchronized void clearAddRemoveEveryNotify() {
            if (serialDummy != null) {
                if (!updateOnNotifyDoAdd) {
                    serialDummy.removeDevice(updateOnNotifyDev);
                }
                serialDummy = null;
                updateOnNotifyDev = null;
            }
        }
    }

    @Test
    public void testScheduleNotifySearchBusCmd() {
        HA7SSerialDummy serialDummy = new HA7SSerialDummy("port");
        
        HA7S ha7s = new HA7S(serialDummy);
        
        final boolean notByAlarm = false;

        BusMaster.CancelScheduledNotifySearchBusCmdResult cancelResult = ha7s.cancelScheduledNotifySearchBusCmd(null, notByAlarm);
        Assert.assertEquals(cancelResult, BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_BusMasterNotStarted);

        BusMaster.ScheduleNotifySearchBusCmdResult result = ha7s.scheduleNotifySearchBusCmd(null, notByAlarm, periodOneMSec);
        Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_BusMasterNotStarted);

        {
            StartBusCmd startCmd = ha7s.queryStartBusCmd();
            Assert.assertNotNull(startCmd);

            try {
                StartBusCmd.Result startResult = startCmd.execute();
                Assert.assertEquals(startResult, StartBusCmd.Result.started);
            } catch (Exception e) {
                Assert.fail("Unexpected exception");
            }

            Assert.assertTrue(ha7s.getIsStarted());
        }

        cancelResult = ha7s.cancelScheduledNotifySearchBusCmd(null, notByAlarm);
        Assert.assertEquals(cancelResult, BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_NotifyObjNotAlreadyScheduled);

        result = ha7s.scheduleNotifySearchBusCmd(null, notByAlarm, periodOneMSec);
        Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_NotifyObjNull);

        result = ha7s.scheduleNotifySearchBusCmd(new myNotifySearchBusCmdResult(), notByAlarm, periodNegativeOneMSec);
        Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_MinPeriodInvalid);

        result = ha7s.scheduleNotifySearchBusCmd(new myNotifySearchBusCmdResult(), notByAlarm, periodZeroMSec);
        Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_MinPeriodInvalid);

        myNotifySearchBusCmdResult callback = new myNotifySearchBusCmdResult();
        Assert.assertNull(callback.getData());

        result = ha7s.scheduleNotifySearchBusCmd(callback, notByAlarm, period250MSec);
        Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success);

        result = ha7s.scheduleNotifySearchBusCmd(callback, notByAlarm, period250MSec);
        Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_NotifyObjAlreadyScheduled);

        boolean waitResult = callback.wait500MSecForNotifyChange(-1);
        Assert.assertTrue(waitResult);

        myNotifySearchBusCmdResult.Data notifyData = callback.getData();
        Assert.assertNotNull(notifyData);
        Assert.assertTrue(notifyData.notifyCount > -1);
        Assert.assertEquals(notifyData.bm, ha7s);
        Assert.assertEquals((boolean)notifyData.byAlarm, notByAlarm);
        Assert.assertNotNull(notifyData.searchResultData);
        Assert.assertNotNull(notifyData.searchResultData.getList());
        Assert.assertEquals(notifyData.searchResultData.getList().size(), 0);

        // do it again - this is expected to FAIL the wait because we are not re-notified of the same
        // result based on the CRC of the last notify.
        waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
        Assert.assertFalse(waitResult);

        // add a device to the bus
        final String dev_A = "EE0000065BC0AE28";

        serialDummy.addDevice(dev_A, false);

        for (int i = 0; i < 2; i++) {
            waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
            if (!waitResult) {
                // We got some old data before new device was recognized, go wait again until the next notify
                Assert.assertEquals(i, 0);
            }
            else {
                break;
            }
        }
        notifyData = callback.getData();
        Assert.assertNotNull(notifyData);
        Assert.assertEquals(notifyData.bm, ha7s);
        Assert.assertEquals((boolean)notifyData.byAlarm, notByAlarm);
        Assert.assertNotNull(notifyData.searchResultData);
        Assert.assertNotNull(notifyData.searchResultData.getList());
        Assert.assertEquals(notifyData.searchResultData.getList().size(), 1);
        Assert.assertTrue(notifyData.searchResultData.getList().contains(dev_A));

        // add more devices to the bus
        final String dev_B = "090000065BD53528";
        final String dev_C = "5F0000065CCD1A28";

        serialDummy.addDevice(dev_B, false);
        serialDummy.addDevice(dev_C, false);

        for (int i = 0; i < 2; i++) {
            waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
            if (!waitResult) {
                // We got some old data before new device was recognized, go wait again until the next notify
                Assert.assertEquals(i, 0);
            }
            else {
                break;
            }
        }
        notifyData = callback.getData();
        Assert.assertNotNull(notifyData);
        Assert.assertEquals(notifyData.bm, ha7s);
        Assert.assertEquals((boolean)notifyData.byAlarm, notByAlarm);
        Assert.assertNotNull(notifyData.searchResultData);
        Assert.assertNotNull(notifyData.searchResultData.getList());
        Assert.assertEquals(notifyData.searchResultData.getList().size(), 3);
        Assert.assertTrue(notifyData.searchResultData.getList().contains(dev_A) &&
                notifyData.searchResultData.getList().contains(dev_B) &&
                notifyData.searchResultData.getList().contains(dev_C));

        // bit of a race condition, but I think we can cancel before the next callback at 250ms
        cancelResult = ha7s.cancelScheduledNotifySearchBusCmd(callback, notByAlarm);
        Assert.assertEquals(cancelResult, BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_Success);

        for (int i = 0; i < 2; i++) {
            waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
            if (waitResult) {
                Assert.fail("We should not get any more notifications");
                break;
            }
        }
        Assert.assertEquals(callback.getData().notifyCount, notifyData.notifyCount);
    }
    
    @Test
    public void testScheduleNotifySearchBusCmdTiming() {
        internal_testScheduleNotifySearchBusCmdTiming(false);
    }

    @Test
    public void testScheduleNotifySearchBusByAlarmCmd() {
        HA7SSerialDummy serialDummy = new HA7SSerialDummy("port");

        HA7S ha7s = new HA7S(serialDummy);

        final boolean byAlarm = true;

        BusMaster.CancelScheduledNotifySearchBusCmdResult cancelResult = ha7s.cancelScheduledNotifySearchBusCmd(null, byAlarm);
        Assert.assertEquals(cancelResult, BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_BusMasterNotStarted);

        BusMaster.ScheduleNotifySearchBusCmdResult result = ha7s.scheduleNotifySearchBusCmd(null, byAlarm, periodOneMSec);
        Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_BusMasterNotStarted);

        {
            StartBusCmd startCmd = ha7s.queryStartBusCmd();
            Assert.assertNotNull(startCmd);

            try {
                StartBusCmd.Result startResult = startCmd.execute();
                Assert.assertEquals(startResult, StartBusCmd.Result.started);
            } catch (Exception e) {
                Assert.fail("Unexpected exception");
            }

            Assert.assertTrue(ha7s.getIsStarted());
        }

        cancelResult = ha7s.cancelScheduledNotifySearchBusCmd(null, byAlarm);
        Assert.assertEquals(cancelResult, BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_NotifyObjNotAlreadyScheduled);

        result = ha7s.scheduleNotifySearchBusCmd(null, byAlarm, periodOneMSec);
        Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_NotifyObjNull);

        result = ha7s.scheduleNotifySearchBusCmd(new myNotifySearchBusCmdResult(), byAlarm, periodNegativeOneMSec);
        Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_MinPeriodInvalid);

        result = ha7s.scheduleNotifySearchBusCmd(new myNotifySearchBusCmdResult(), byAlarm, periodZeroMSec);
        Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_MinPeriodInvalid);

        myNotifySearchBusCmdResult callback = new myNotifySearchBusCmdResult();
        Assert.assertNull(callback.getData());

        result = ha7s.scheduleNotifySearchBusCmd(callback, byAlarm, period250MSec);
        Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success);

        result = ha7s.scheduleNotifySearchBusCmd(callback, byAlarm, period250MSec);
        Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_NotifyObjAlreadyScheduled);

        boolean waitResult = callback.wait500MSecForNotifyChange(-1);
        Assert.assertTrue(waitResult);

        myNotifySearchBusCmdResult.Data notifyData = callback.getData();
        Assert.assertNotNull(notifyData);
        Assert.assertTrue(notifyData.notifyCount > -1);
        Assert.assertEquals(notifyData.bm, ha7s);
        Assert.assertEquals((boolean)notifyData.byAlarm, byAlarm);
        Assert.assertNotNull(notifyData.searchResultData);
        Assert.assertNotNull(notifyData.searchResultData.getList());
        Assert.assertEquals(notifyData.searchResultData.getList().size(), 0);

        // do it again - this is expected to FAIL the wait because we are not re-notified of the same
        // result based on the CRC of the last notify.
        waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
        Assert.assertFalse(waitResult);

        // add a device to the bus
        final String dev_A = "EE0000065BC0AE28";

        serialDummy.addDevice(dev_A, true);

        for (int i = 0; i < 2; i++) {
            waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
            if (!waitResult) {
                // We got some old data before new device was recognized, go wait again until the next notify
                Assert.assertEquals(i, 0);
            }
            else {
                break;
            }
        }
        notifyData = callback.getData();
        Assert.assertNotNull(notifyData);
        Assert.assertEquals(notifyData.bm, ha7s);
        Assert.assertEquals((boolean)notifyData.byAlarm, byAlarm);
        Assert.assertNotNull(notifyData.searchResultData);
        Assert.assertNotNull(notifyData.searchResultData.getList());
        Assert.assertEquals(notifyData.searchResultData.getList().size(), 1);
        Assert.assertTrue(notifyData.searchResultData.getList().contains(dev_A));

        // add more devices to the bus
        final String dev_B = "090000065BD53528";
        final String dev_C = "5F0000065CCD1A28";

        serialDummy.addDevice(dev_B, false);
        serialDummy.addDevice(dev_C, true);

        for (int i = 0; i < 2; i++) {
            waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
            if (!waitResult) {
                // We got some old data before new device was recognized, go wait again until the next notify
                Assert.assertEquals(i, 0);
            }
            else {
                break;
            }
        }
        notifyData = callback.getData();
        Assert.assertNotNull(notifyData);
        Assert.assertEquals(notifyData.bm, ha7s);
        Assert.assertEquals((boolean)notifyData.byAlarm, byAlarm);
        Assert.assertNotNull(notifyData.searchResultData);
        Assert.assertNotNull(notifyData.searchResultData.getList());
        Assert.assertEquals(notifyData.searchResultData.getList().size(), 2);
        Assert.assertTrue(notifyData.searchResultData.getList().contains(dev_A) &&
                notifyData.searchResultData.getList().contains(dev_C));

        // bit of a race condition, but I think we can cancel before the next callback at 250ms
        cancelResult = ha7s.cancelScheduledNotifySearchBusCmd(callback, byAlarm);
        Assert.assertEquals(cancelResult, BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_Success);

        for (int i = 0; i < 2; i++) {
            waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
            if (waitResult) {
                Assert.fail("We should not get any more notifications");
                break;
            }
        }
        Assert.assertEquals(callback.getData().notifyCount, notifyData.notifyCount);
    }

    @Test
    public void testScheduleNotifySearchBusByAlarmCmdTiming() {
        internal_testScheduleNotifySearchBusCmdTiming(true);
    }

    private void internal_testScheduleNotifySearchBusCmdTiming(boolean byAlarm) {
        HA7SSerialDummy serialDummy = new HA7SSerialDummy("port");

        HA7S ha7s = new HA7S(serialDummy);

        {
            StartBusCmd startCmd = ha7s.queryStartBusCmd();
            Assert.assertNotNull(startCmd);

            try {
                StartBusCmd.Result startResult = startCmd.execute();
                Assert.assertEquals(startResult, StartBusCmd.Result.started);
            } catch (Exception e) {
                Assert.fail("Unexpected exception");
            }

            Assert.assertTrue(ha7s.getIsStarted());
        }

        myNotifySearchBusCmdResult callback = new myNotifySearchBusCmdResult();
        Assert.assertNull(callback.getData());

        final String dev_A = "EE0000065BC0AE28";

        // this will ensure that we get a callback on every search by adding/removing a device to the search list
        callback.setAddRemoveEveryNotify(serialDummy, dev_A, byAlarm);

        BusMaster.ScheduleNotifySearchBusCmdResult scheduleResult = ha7s.scheduleNotifySearchBusCmd(callback, byAlarm, period250MSec);
        Assert.assertEquals(scheduleResult, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success);

        // wait for something to change
        for (int i = 0; i < 2; i++) {
            boolean waitResult = callback.wait500MSecForNotifyChange(-1);
            if (!waitResult) {
                // We got some old data before new device was recognized, go wait again until the next notify
                Assert.assertEquals(i, 0);
            } else {
                break;
            }
        }
        myNotifySearchBusCmdResult.Data notifyData = callback.getData();
        Assert.assertNotNull(notifyData);
        Assert.assertNotEquals(notifyData.notifyCount, -1);
        Assert.assertEquals((boolean)notifyData.byAlarm, byAlarm);

        // wait for something to change again
        for (int i = 0; i < 2; i++) {
            boolean waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
            if (!waitResult) {
                // We got some old data before new device was recognized, go wait again until the next notify
                Assert.assertEquals(i, 0);
            } else {
                break;
            }
        }
        myNotifySearchBusCmdResult.Data notifyData2 = callback.getData();
        Assert.assertNotNull(notifyData2);
        Assert.assertEquals((boolean)notifyData2.byAlarm, byAlarm);
        Assert.assertEquals((notifyData.notifyCount + 1), notifyData2.notifyCount);
        long delta = (notifyData2.searchResultData.getWriteCTM() - notifyData.searchResultData.getWriteCTM());
        Assert.assertTrue(delta >= period250MSec);
        Assert.assertTrue(delta < period500MSec);

        // let's change the rate to something slower
        BusMaster.UpdateScheduledNotifySearchBusCmdResult updateResult = ha7s.updateScheduledNotifySearchBusCmd(callback, byAlarm, period500MSec);
        Assert.assertEquals(updateResult, BusMaster.UpdateScheduledNotifySearchBusCmdResult.USNSBC_Success);

        // wait for something to change again
        for (int i = 0; i < 2; i++) {
            boolean waitResult = callback.wait1000MSecForNotifyChange(notifyData2.notifyCount);
            if (!waitResult) {
                // We got some old data before new device was recognized, go wait again until the next notify
                Assert.assertEquals(i, 0);
            } else {
                break;
            }
        }
        myNotifySearchBusCmdResult.Data notifyData3 = callback.getData();
        Assert.assertNotNull(notifyData3);
        Assert.assertEquals((boolean)notifyData3.byAlarm, byAlarm);
        Assert.assertEquals((notifyData2.notifyCount + 1), notifyData3.notifyCount);
        delta = (notifyData3.searchResultData.getWriteCTM() - notifyData2.searchResultData.getWriteCTM());
        Assert.assertTrue(delta >= period500MSec);
        Assert.assertTrue(delta < period750MSec);

        // let's change the rate to something faster by adding another object waiting. We do not need
        // to wait on it since its new rate will be effective for all waiters.
        myNotifySearchBusCmdResult callback2 = new myNotifySearchBusCmdResult();
        Assert.assertNull(callback2.getData());
        scheduleResult = ha7s.scheduleNotifySearchBusCmd(callback2, byAlarm, period250MSec);
        Assert.assertEquals(scheduleResult, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success);

        // wait for something to change again
        for (int i = 0; i < 2; i++) {
            boolean waitResult = callback.wait1000MSecForNotifyChange(notifyData3.notifyCount);
            if (!waitResult) {
                // We got some old data before new device was recognized, go wait again until the next notify
                Assert.assertEquals(i, 0);
            } else {
                break;
            }
        }
        myNotifySearchBusCmdResult.Data notifyData4 = callback.getData();
        Assert.assertNotNull(notifyData4);
        Assert.assertEquals((boolean)notifyData4.byAlarm, byAlarm);
        Assert.assertEquals((notifyData3.notifyCount + 1), notifyData4.notifyCount);
        delta = (notifyData4.searchResultData.getWriteCTM() - notifyData3.searchResultData.getWriteCTM());
        Assert.assertTrue(delta >= period250MSec);
        Assert.assertTrue(delta < period500MSec);

        // cancel the new rate which will slow things back to the 500 rate
        BusMaster.CancelScheduledNotifySearchBusCmdResult cancelResult = ha7s.cancelScheduledNotifySearchBusCmd(callback2, byAlarm);
        Assert.assertEquals(cancelResult, BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_Success);

        // wait for something to change again
        for (int i = 0; i < 2; i++) {
            boolean waitResult = callback.wait1000MSecForNotifyChange(notifyData4.notifyCount);
            if (!waitResult) {
                // We got some old data before new device was recognized, go wait again until the next notify
                Assert.assertEquals(i, 0);
            } else {
                break;
            }
        }
        myNotifySearchBusCmdResult.Data notifyData5 = callback.getData();
        Assert.assertNotNull(notifyData5);
        Assert.assertEquals((boolean)notifyData5.byAlarm, byAlarm);
        Assert.assertEquals((notifyData4.notifyCount + 1), notifyData5.notifyCount);
        delta = (notifyData5.searchResultData.getWriteCTM() - notifyData4.searchResultData.getWriteCTM());
        Assert.assertTrue(delta >= period500MSec);
        Assert.assertTrue(delta < period750MSec);

        // Stop the busmaster to cancel everything.
        {
            StopBusCmd stopCmd = ha7s.queryStopBusCmd();
            Assert.assertNotNull(stopCmd);

            try {
                StopBusCmd.Result startResult = stopCmd.execute();
                Assert.assertEquals(startResult, StopBusCmd.Result.stopped);
            } catch (Exception e) {
                Assert.fail("Unexpected exception");
            }

            Assert.assertFalse(ha7s.getIsStarted());
        }

        // no more events expected
        for (int i = 0; i < 2; i++) {
            boolean waitResult = callback.wait500MSecForNotifyChange(notifyData5.notifyCount);
            if (waitResult) {
                Assert.fail("We should not get any more notifications");
                break;
            }
        }
        Assert.assertEquals(callback.getData().notifyCount, notifyData5.notifyCount);

        // cancelling our schedule will return a different error.
        cancelResult = ha7s.cancelScheduledNotifySearchBusCmd(callback, byAlarm);
        Assert.assertEquals(cancelResult, BusMaster.CancelScheduledNotifySearchBusCmdResult.CSNSBC_BusMasterNotStarted);
    }

    public void testScheduleNotifySearchBusCmdTimer() {
        // We want to prove that the time period we are called back matches what we asked for
        // We want to test updating the search period rate
        // We want to test update ...

        // It would be useful to consider negative tests for the APIs, though there are not that many
    }

    // Utils
    public static Answer<HA7S.cmdReturn> makeWriteBlockCmdReturnAnswer(HA7S.cmdResult result, byte[] rbuf_data, long writeCTM) {
        return new Answer<HA7S.cmdReturn>() {
            @Override
            public HA7S.cmdReturn answer(final InvocationOnMock invocation) {
                byte[] wbuf = (byte[]) (invocation.getArguments())[0];
                byte[] rbuf = (byte[]) (invocation.getArguments())[1];
                Logger logger = (Logger) (invocation.getArguments())[2];

                if (result == HA7S.cmdResult.Success) {
                    int read_count = 0;
                    if (rbuf_data != null) {
                        read_count = rbuf_data.length;
                        for (int i = 0; i < read_count; i++) {
                            rbuf[i] = rbuf_data[i];
                        }
                    }
                    return new HA7S.cmdReturn(read_count, writeCTM);
                }
                return new HA7S.cmdReturn(result);
            }
        };
    }

    public static Answer<HA7S.cmdReturn> makeSearchCmdReturnAnswer(HA7S.cmdResult result, byte[] rbuf_data, long writeCTM) {
        return new Answer<HA7S.cmdReturn>() {
            @Override
            public HA7S.cmdReturn answer(final InvocationOnMock invocation) {
                Byte familyCode = null;
                byte[] rbuf = null;
                Logger logger = null;
                if (invocation.getArguments().length == 2) {
                    rbuf = (byte[]) (invocation.getArguments())[0];
                    logger = (Logger) (invocation.getArguments())[1];
                }
                else {
                    familyCode = (Byte)(invocation.getArguments())[0];
                    rbuf = (byte[]) (invocation.getArguments())[1];
                    logger = (Logger) (invocation.getArguments())[2];
                }

                if (result == HA7S.cmdResult.Success) {
                    int read_count = 0;
                    if (rbuf_data != null) {
                        read_count = rbuf_data.length;
                        for (int i = 0; i < read_count; i++) {
                            rbuf[i] = rbuf_data[i];
                        }
                    }
                    return new HA7S.cmdReturn(read_count, writeCTM);
                }
                return new HA7S.cmdReturn(result);
            }
        };
    }

}
