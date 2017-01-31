package waterfall.onewire.busmasters.HA7S;

import javafx.scene.paint.Stop;
import org.testng.Assert;
import org.testng.annotations.Test;
import waterfall.onewire.busmaster.*;

import java.util.Iterator;

/**
 * Created by dwaterfa on 1/29/17.
 */
public class HA7STest {

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
            ha7s = new HA7S(new HA7SSerialDummy());
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
        HA7S ha7s = new HA7S(new HA7SSerialDummy());

        Assert.assertFalse(ha7s.getIsStarted());

        StartBusCmd startCmd = ha7s.queryStartBusCmd(Logger.LogLevel.CmdOnlyLevel());
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
        HA7S ha7s = new HA7S(new HA7SSerialDummy());

        Assert.assertFalse(ha7s.getIsStarted());

        StopBusCmd stopBusCmd = ha7s.queryStopBusCmd(Logger.LogLevel.CmdOnlyLevel());

        Assert.assertNotNull(stopBusCmd);

        try {
            StopBusCmd.Result result = stopBusCmd.execute();
            Assert.assertEquals(result, StopBusCmd.Result.not_started);
        }
        catch (Exception e) {
            Assert.fail("Unexpected exception");
        }

        {
            StartBusCmd startCmd = ha7s.queryStartBusCmd(Logger.LogLevel.CmdOnlyLevel());
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
        HA7SSerialDummy serialDummy = new HA7SSerialDummy();

        HA7S ha7s = new HA7S(serialDummy);

        SearchBusCmd searchBusCmd = ha7s.querySearchBusCmd(Logger.LogLevel.CmdOnlyLevel());
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
            StartBusCmd startCmd = ha7s.queryStartBusCmd(Logger.LogLevel.CmdOnlyLevel());
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
        HA7SSerialDummy serialDummy = new HA7SSerialDummy();

        HA7S ha7s = new HA7S(serialDummy);

        SearchBusCmd searchBusCmd = ha7s.querySearchBusByAlarmCmd(Logger.LogLevel.CmdOnlyLevel());
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
            StartBusCmd startCmd = ha7s.queryStartBusCmd(Logger.LogLevel.CmdOnlyLevel());
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
        HA7SSerialDummy serialDummy = new HA7SSerialDummy();

        HA7S ha7s = new HA7S(serialDummy);

        short familyCode = 0x27;

        SearchBusCmd searchBusCmd = ha7s.querySearchBusByFamilyCmd(familyCode, Logger.LogLevel.CmdOnlyLevel());
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
            StartBusCmd startCmd = ha7s.queryStartBusCmd(Logger.LogLevel.CmdOnlyLevel());
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

        public boolean waitForNotifyChange(int afterNotifyCount) {
            for (int i = 0; i < 5; i++) {
                Data t_data = getData();
                if ((t_data != null) && (t_data.notifyCount != afterNotifyCount)) {
                    return true;
                }
                try {
                    Thread.sleep(100);
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
        HA7SSerialDummy serialDummy = new HA7SSerialDummy();
        
        HA7S ha7s = new HA7S(serialDummy);
        
        final boolean notByAlarm = false;
        final long periodNegativeOneMSec = -1;
        final long periodZeroMSec = 0;
        final long periodOneMSec = 1;
        final long period250MSec = 250;

        BusMaster.ScheduleNotifySearchBusCmdResult result = ha7s.scheduleNotifySearchBusCmd(null, notByAlarm, periodOneMSec);
        Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_NotifyObjNull);
        
        result = ha7s.scheduleNotifySearchBusCmd(new myNotifySearchBusCmdResult(), notByAlarm, periodNegativeOneMSec);
        Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_MinPeriodInvalid);
        result = ha7s.scheduleNotifySearchBusCmd(new myNotifySearchBusCmdResult(), notByAlarm, periodZeroMSec);
        Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_MinPeriodInvalid);
        
        result = ha7s.scheduleNotifySearchBusCmd(new myNotifySearchBusCmdResult(), notByAlarm, period250MSec);
        Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_BusMasterNotStarted);

        {
            StartBusCmd startCmd = ha7s.queryStartBusCmd(Logger.LogLevel.CmdOnlyLevel());
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

        result = ha7s.scheduleNotifySearchBusCmd(callback, notByAlarm, period250MSec);
        Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success);

        result = ha7s.scheduleNotifySearchBusCmd(callback, notByAlarm, period250MSec);
        Assert.assertEquals(result, BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_NotifyObjAlreadyScheduled);

        boolean waitResult = callback.waitForNotifyChange(-1);
        Assert.assertTrue(waitResult);

        myNotifySearchBusCmdResult.Data notifyData = callback.getData();
        Assert.assertNotNull(notifyData);
        Assert.assertTrue(notifyData.notifyCount > -1);
        Assert.assertEquals(notifyData.bm, ha7s);
        Assert.assertFalse(notifyData.byAlarm);
        Assert.assertNotNull(notifyData.searchResultData);
        Assert.assertNotNull(notifyData.searchResultData.getList());
        Assert.assertEquals(notifyData.searchResultData.getList().size(), 0);

        // do it again - this is expected to FAIL the wait because we are not re-notified of the same
        // result based on the CRC of the last notify.
        waitResult = callback.waitForNotifyChange(notifyData.notifyCount);
        Assert.assertFalse(waitResult);

        // add a device to the bus
        final String dev_A = "EE0000065BC0AE28";

        serialDummy.addDevice(dev_A, false);

        for (int i = 0; i < 2; i++) {
            waitResult = callback.waitForNotifyChange(notifyData.notifyCount);
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
        Assert.assertFalse(notifyData.byAlarm);
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
            waitResult = callback.waitForNotifyChange(notifyData.notifyCount);
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
        Assert.assertFalse(notifyData.byAlarm);
        Assert.assertNotNull(notifyData.searchResultData);
        Assert.assertNotNull(notifyData.searchResultData.getList());
        Assert.assertEquals(notifyData.searchResultData.getList().size(), 3);
        Assert.assertTrue(notifyData.searchResultData.getList().contains(dev_A) &&
                notifyData.searchResultData.getList().contains(dev_B) &&
                notifyData.searchResultData.getList().contains(dev_C));
    }

    @Test
    public void testScheduleNotifySearchBusCmdTimer() {
        // We want to prove that the time period we are called back matches what we asked for
        // we want to prove that cancelling will stop the notifications
        // we want to prove that cancelling without actually scheduling returns the right error
        // We want to test updating the search period rate
        // We want to test update ...

        // It would be useful to consider negative tests for the APIs, though there are not that many
    }

}
