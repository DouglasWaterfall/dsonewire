package waterfall.onewire.busmasters.HA7S;

import org.testng.Assert;
import org.testng.annotations.Test;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmasters.HA7S.part.DS18B20;

/**
 * Created by dwaterfa on 12/17/17.
 */
public class ScheduleNotifySearchBusCmdTests extends TestBase {

  @Test
  public void testScheduleNotifySearchBusCmd() {
    HA7SSerialDummy serialDummy = new HA7SSerialDummy("port");

    HA7S ha7s = new HA7S(serialDummy);

    final boolean notByAlarm = false;

    try {
      ha7s.cancelScheduledNotifySearchBusCmd(null, notByAlarm);
      Assert.fail("exception expected");
    } catch (IllegalArgumentException e) {
      Assert.assertEquals(e.getMessage(), "CSNSBC_BusMasterNotStarted");
    }

    try {
      ha7s.cancelScheduledNotifySearchBusCmd(null, notByAlarm);
      Assert.fail("Expected exception");
    } catch (IllegalArgumentException e) {
      Assert.assertEquals(e.getMessage(), "CSNSBC_NotifyObjNotAlreadyScheduled");
    }

    try {
      ha7s.scheduleNotifySearchBusCmd(null, notByAlarm, periodOneMSec);
      Assert.fail("Expected exception");
    } catch (IllegalArgumentException e) {
      Assert.assertEquals(e.getMessage(), "SNSBCR_NotifyObjNull");
    }

    try {
      ha7s.scheduleNotifySearchBusCmd(new myNotifySearchBusCmdResult(), notByAlarm,
          periodNegativeOneMSec);
      Assert.fail("Expected exception");
    } catch (IllegalArgumentException e) {
      Assert.assertEquals(e.getMessage(), "SNSBCR_MinPeriodInvalid");
    }

    try {
      ha7s.scheduleNotifySearchBusCmd(new myNotifySearchBusCmdResult(), notByAlarm, periodZeroMSec);
      Assert.fail("Expected exception");
    } catch (IllegalArgumentException e) {
      Assert.assertEquals(e.getMessage(), "SNSBCR_MinPeriodInvalid");
    }

    myNotifySearchBusCmdResult callback = new myNotifySearchBusCmdResult();
    Assert.assertNull(callback.getData());

    try {
      ha7s.scheduleNotifySearchBusCmd(callback, notByAlarm, period250MSec);
    } catch (IllegalArgumentException e) {
      Assert.fail("Unexpected exception:" + e);
    }

    try {
      ha7s.scheduleNotifySearchBusCmd(callback, notByAlarm, period250MSec);
    } catch (IllegalArgumentException e) {
      Assert.assertEquals(e.getMessage(), "SNSBCR_NotifyObjAlreadyScheduled");
    }

    boolean waitResult = callback.wait500MSecForNotifyChange(-1);
    Assert.assertTrue(waitResult);

    myNotifySearchBusCmdResult.Data notifyData = callback.getData();
    Assert.assertNotNull(notifyData);
    Assert.assertTrue(notifyData.notifyCount > -1);
    Assert.assertEquals(notifyData.bm, ha7s);
    Assert.assertEquals((boolean) notifyData.byAlarm, notByAlarm);
    Assert.assertNotNull(notifyData.searchResultData);
    Assert.assertNotNull(notifyData.searchResultData.getList());
    Assert.assertEquals(notifyData.searchResultData.getList().size(), 0);

    // do it again - this is expected to FAIL the wait because we are not re-notified of the same
    // result based on the CRC of the last notify.
    waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
    Assert.assertFalse(waitResult);

    // add a device to the bus
    final DSAddress dev_A = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);

    serialDummy.addDevice(new DS18B20(dev_A));

    for (int i = 0; i < 2; i++) {
      waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
      if (!waitResult) {
        // We got some old data before new device was recognized, go wait again until the next notify
        Assert.assertEquals(i, 0);
      } else {
        break;
      }
    }
    notifyData = callback.getData();
    Assert.assertNotNull(notifyData);
    Assert.assertEquals(notifyData.bm, ha7s);
    Assert.assertEquals((boolean) notifyData.byAlarm, notByAlarm);
    Assert.assertNotNull(notifyData.searchResultData);
    Assert.assertNotNull(notifyData.searchResultData.getList());
    Assert.assertEquals(notifyData.searchResultData.getList().size(), 1);
    Assert.assertTrue(notifyData.searchResultData.getList().contains(dev_A));

    // add more devices to the bus
    final DSAddress dev_B = DSAddress.fromUncheckedHex(DSAddress._090000065BD53528);
    final DSAddress dev_C = DSAddress.fromUncheckedHex(DSAddress._5F0000065CCD1A28);

    serialDummy.addDevice(new DS18B20(dev_B));
    serialDummy.addDevice(new DS18B20(dev_C));

    for (int i = 0; i < 2; i++) {
      waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
      if (!waitResult) {
        // We got some old data before new device was recognized, go wait again until the next notify
        Assert.assertEquals(i, 0);
      } else {
        break;
      }
    }
    notifyData = callback.getData();
    Assert.assertNotNull(notifyData);
    Assert.assertEquals(notifyData.bm, ha7s);
    Assert.assertEquals((boolean) notifyData.byAlarm, notByAlarm);
    Assert.assertNotNull(notifyData.searchResultData);
    Assert.assertNotNull(notifyData.searchResultData.getList());
    Assert.assertEquals(notifyData.searchResultData.getList().size(), 3);
    Assert.assertTrue(notifyData.searchResultData.getList().contains(dev_A) &&
        notifyData.searchResultData.getList().contains(dev_B) &&
        notifyData.searchResultData.getList().contains(dev_C));

    // bit of a race condition, but I think we can cancel before the next callback at 250ms
    try {
      ha7s.cancelScheduledNotifySearchBusCmd(callback, notByAlarm);
    } catch (IllegalArgumentException e) {
      Assert.fail("Unexpected exception:" + e);
    }

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
    serialDummy.start(null);

    HA7S ha7s = new HA7S(serialDummy);

    final boolean byAlarm = true;

    try {
      ha7s.cancelScheduledNotifySearchBusCmd(null, byAlarm);
    } catch (IllegalArgumentException e) {
      Assert.assertEquals(e.getMessage(), "CSNSBC_NotifyObjNotAlreadyScheduled");
    }

    try {
      ha7s.scheduleNotifySearchBusCmd(null, byAlarm, periodOneMSec);
    } catch (IllegalArgumentException e) {
      Assert.assertEquals(e.getMessage(), "SNSBCR_NotifyObjNull");
    }

    try {
      ha7s.scheduleNotifySearchBusCmd(new myNotifySearchBusCmdResult(), byAlarm,
          periodNegativeOneMSec);
    } catch (IllegalArgumentException e) {
      Assert.assertEquals(e.getMessage(), "SNSBCR_MinPeriodInvalid");
    }

    try {
      ha7s.scheduleNotifySearchBusCmd(new myNotifySearchBusCmdResult(), byAlarm, periodZeroMSec);
    } catch (IllegalArgumentException e) {
      Assert.assertEquals(e.getMessage(), "SNSBCR_MinPeriodInvalid");
    }

    myNotifySearchBusCmdResult callback = new myNotifySearchBusCmdResult();
    Assert.assertNull(callback.getData());

    try {
      ha7s.scheduleNotifySearchBusCmd(callback, byAlarm, period250MSec);
    } catch (IllegalArgumentException e) {
      Assert.fail("Unexpected exception:" + e);
    }

    try {
      ha7s.scheduleNotifySearchBusCmd(callback, byAlarm, period250MSec);
    } catch (IllegalArgumentException e) {
      Assert.assertEquals(e.getMessage(), "SNSBCR_NotifyObjAlreadyScheduled");
    }

    boolean waitResult = callback.wait500MSecForNotifyChange(-1);
    Assert.assertTrue(waitResult);

    myNotifySearchBusCmdResult.Data notifyData = callback.getData();
    Assert.assertNotNull(notifyData);
    Assert.assertTrue(notifyData.notifyCount > -1);
    Assert.assertEquals(notifyData.bm, ha7s);
    Assert.assertEquals((boolean) notifyData.byAlarm, byAlarm);
    Assert.assertNotNull(notifyData.searchResultData);
    Assert.assertNotNull(notifyData.searchResultData.getList());
    Assert.assertEquals(notifyData.searchResultData.getList().size(), 0);

    // do it again - this is expected to FAIL the wait because we are not re-notified of the same
    // result based on the CRC of the last notify.
    waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
    Assert.assertFalse(waitResult);

    // add a device to the bus
    final DSAddress dev_A = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);

    serialDummy.addDevice(new DS18B20(dev_A).setHasAlarm(true));

    for (int i = 0; i < 2; i++) {
      waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
      if (!waitResult) {
        // We got some old data before new device was recognized, go wait again until the next notify
        Assert.assertEquals(i, 0);
      } else {
        break;
      }
    }
    notifyData = callback.getData();
    Assert.assertNotNull(notifyData);
    Assert.assertEquals(notifyData.bm, ha7s);
    Assert.assertEquals((boolean) notifyData.byAlarm, byAlarm);
    Assert.assertNotNull(notifyData.searchResultData);
    Assert.assertNotNull(notifyData.searchResultData.getList());
    Assert.assertEquals(notifyData.searchResultData.getList().size(), 1);
    Assert.assertTrue(notifyData.searchResultData.getList().contains(dev_A));

    // add more devices to the bus
    final DSAddress dev_B = DSAddress.fromUncheckedHex(DSAddress._090000065BD53528);
    final DSAddress dev_C = DSAddress.fromUncheckedHex(DSAddress._5F0000065CCD1A28);

    serialDummy.addDevice(new DS18B20(dev_B));
    serialDummy.addDevice(new DS18B20(dev_C).setHasAlarm(true));

    for (int i = 0; i < 2; i++) {
      waitResult = callback.wait500MSecForNotifyChange(notifyData.notifyCount);
      if (!waitResult) {
        // We got some old data before new device was recognized, go wait again until the next notify
        Assert.assertEquals(i, 0);
      } else {
        break;
      }
    }
    notifyData = callback.getData();
    Assert.assertNotNull(notifyData);
    Assert.assertEquals(notifyData.bm, ha7s);
    Assert.assertEquals((boolean) notifyData.byAlarm, byAlarm);
    Assert.assertNotNull(notifyData.searchResultData);
    Assert.assertNotNull(notifyData.searchResultData.getList());
    Assert.assertEquals(notifyData.searchResultData.getList().size(), 2);
    Assert.assertTrue(notifyData.searchResultData.getList().contains(dev_A) &&
        notifyData.searchResultData.getList().contains(dev_C));

    // bit of a race condition, but I think we can cancel before the next callback at 250ms
    try {
      ha7s.cancelScheduledNotifySearchBusCmd(callback, byAlarm);
    } catch (IllegalArgumentException e) {
      Assert.fail("Unexpected exception:" + e);
    }

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
    serialDummy.start(null);

    HA7S ha7s = new HA7S(serialDummy);

    myNotifySearchBusCmdResult callback = new myNotifySearchBusCmdResult();
    Assert.assertNull(callback.getData());

    final DSAddress dev_A = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);

    // this will ensure that we get a callback on every search by adding/removing a device to the search list
    callback.setAddRemoveEveryNotify(serialDummy, dev_A, byAlarm);

    try {
      ha7s.scheduleNotifySearchBusCmd(callback, byAlarm, period250MSec);
    } catch (IllegalArgumentException e) {
      Assert.fail("Unexpected exception:" + e);
    }

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
    Assert.assertEquals((boolean) notifyData.byAlarm, byAlarm);

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
    Assert.assertEquals((boolean) notifyData2.byAlarm, byAlarm);
    Assert.assertEquals((notifyData.notifyCount + 1), notifyData2.notifyCount);
    long delta = (notifyData2.searchResultData.getWriteCTM() - notifyData.searchResultData
        .getWriteCTM());
    Assert.assertTrue(delta >= period250MSec);
    Assert.assertTrue(delta < period500MSec);

    // let's change the rate to something slower
    try {
      ha7s.updateScheduledNotifySearchBusCmd(callback, byAlarm, period500MSec);
    } catch (IllegalArgumentException e) {
      Assert.fail("Unexpected exception:" + e);
    }

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
    Assert.assertEquals((boolean) notifyData3.byAlarm, byAlarm);
    Assert.assertEquals((notifyData2.notifyCount + 1), notifyData3.notifyCount);
    delta = (notifyData3.searchResultData.getWriteCTM() - notifyData2.searchResultData
        .getWriteCTM());
    Assert.assertTrue(delta >= period500MSec);
    Assert.assertTrue(delta < period750MSec);

    // let's change the rate to something faster by adding another object waiting. We do not need
    // to wait on it since its new rate will be effective for all waiters.
    myNotifySearchBusCmdResult callback2 = new myNotifySearchBusCmdResult();
    Assert.assertNull(callback2.getData());
    try {
      ha7s.scheduleNotifySearchBusCmd(callback2, byAlarm, period250MSec);
    } catch (IllegalArgumentException e) {
      Assert.fail("Unexpected exception:" + e);
    }

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
    Assert.assertEquals((boolean) notifyData4.byAlarm, byAlarm);
    Assert.assertEquals((notifyData3.notifyCount + 1), notifyData4.notifyCount);
    delta = (notifyData4.searchResultData.getWriteCTM() - notifyData3.searchResultData
        .getWriteCTM());
    Assert.assertTrue(delta >= period250MSec);
    Assert.assertTrue(delta < period500MSec);

    // cancel the new rate which will slow things back to the 500 rate
    try {
      ha7s.cancelScheduledNotifySearchBusCmd(callback2, byAlarm);
    } catch (IllegalArgumentException e) {
      Assert.fail("Unexpected exception:" + e);
    }

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
    Assert.assertEquals((boolean) notifyData5.byAlarm, byAlarm);
    Assert.assertEquals((notifyData4.notifyCount + 1), notifyData5.notifyCount);
    delta = (notifyData5.searchResultData.getWriteCTM() - notifyData4.searchResultData
        .getWriteCTM());
    Assert.assertTrue(delta >= period500MSec);
    Assert.assertTrue(delta < period750MSec);

    // Stop the busmaster to cancel everything.
    try {
      ha7s.stopBus(null);
    } catch (Exception e) {
      Assert.fail("Unexpected exception:" + e);
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
    try {
      ha7s.cancelScheduledNotifySearchBusCmd(callback, byAlarm);
    } catch (IllegalArgumentException e) {
      Assert.assertEquals(e.getMessage(), "CSNSBC_BusMasterNotStarted");
    }
  }

  public void testScheduleNotifySearchBusCmdTimer() {
    // We want to prove that the time period we are called back matches what we asked for
    // We want to test updating the search period rate
    // We want to test update ...

    // It would be useful to consider negative tests for the APIs, though there are not that many
  }


}
