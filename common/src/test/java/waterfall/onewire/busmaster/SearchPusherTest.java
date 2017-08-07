package waterfall.onewire.busmaster;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Created by dwaterfa on 8/6/17.
 */
public class SearchPusherTest {

    @Test(dataProvider = "createConstructorCases")
    public void testConstructor(boolean isByAlarm) {
        SearchPusher pusher = new mySearchPusher(isByAlarm);
        Assert.assertEquals(pusher.isAlarmSearch(), isByAlarm);
        Assert.assertFalse(pusher.isTimerActive());
        Assert.assertFalse(pusher.adjustPeriod(Long.MAX_VALUE));
    }

    @DataProvider
    public Object[][] createConstructorCases() {
        return new Object[][] {
                { true },
                { false }
        };
    }

    @Test
    public void testAdjustPeriodCancel() {
        SearchPusher pusher = new mySearchPusher(true);
        Assert.assertFalse(pusher.isTimerActive());

        Assert.assertTrue(pusher.adjustPeriod(5000));
        Assert.assertTrue(pusher.isTimerActive());

        Assert.assertFalse(pusher.adjustPeriod(Long.MAX_VALUE));
        Assert.assertFalse(pusher.isTimerActive());
    }

    @Test
    public void testAdjustPeriodSame() {
        SearchPusher pusher = new mySearchPusher(true);
        Assert.assertFalse(pusher.isTimerActive());

        Assert.assertTrue(pusher.adjustPeriod(5000));
        Assert.assertTrue(pusher.isTimerActive());

        Assert.assertFalse(pusher.adjustPeriod(5000));
        Assert.assertTrue(pusher.isTimerActive());

        Assert.assertFalse(pusher.adjustPeriod(Long.MAX_VALUE));
        Assert.assertFalse(pusher.isTimerActive());
    }

    @Test
    public void testAdjustPeriodCausesPushes() {
        mySearchPusher pusher = new mySearchPusher(true);
        Assert.assertFalse(pusher.isTimerActive());

        Assert.assertTrue(pusher.adjustPeriod(50));
        Assert.assertEquals(pusher.getCurrentPeriodMSec(), 50);
        long start = System.currentTimeMillis();
        Assert.assertTrue(pusher.isTimerActive());

        delayForPush(60);
        long first = System.currentTimeMillis();
        long firstPushTimeMSec = pusher.getLastPushTimeMSec();
        Assert.assertTrue(firstPushTimeMSec > start);
        Assert.assertTrue(firstPushTimeMSec < first);

        delayForPush(60);
        long second = System.currentTimeMillis();
        long secondPushTimeMSec = pusher.getLastPushTimeMSec();
        Assert.assertTrue(secondPushTimeMSec > first);
        Assert.assertTrue(secondPushTimeMSec > first);
        Assert.assertTrue(secondPushTimeMSec > firstPushTimeMSec);

        long diffMSec = (secondPushTimeMSec - firstPushTimeMSec);
        Assert.assertTrue((diffMSec >= 50) && (diffMSec < 60));

        Assert.assertFalse(pusher.adjustPeriod(Long.MAX_VALUE));
        Assert.assertEquals(pusher.getCurrentPeriodMSec(), Long.MAX_VALUE);
        Assert.assertFalse(pusher.isTimerActive());
    }

    @Test
    public void testAdjustPeriodShorter() {
        mySearchPusher pusher = new mySearchPusher(true);
        Assert.assertFalse(pusher.isTimerActive());

        Assert.assertTrue(pusher.adjustPeriod(100)); // no delay
        Assert.assertEquals(pusher.getCurrentPeriodMSec(), 100);
        long start = System.currentTimeMillis();
        Assert.assertTrue(pusher.isTimerActive());

        delayForPush(110);
        long first = System.currentTimeMillis();
        long firstPushTimeMSec = pusher.getLastPushTimeMSec();
        Assert.assertTrue(firstPushTimeMSec > start);
        Assert.assertTrue(firstPushTimeMSec < first);

        delayForPush(110);
        long second = System.currentTimeMillis();
        long secondPushTimeMSec = pusher.getLastPushTimeMSec();
        Assert.assertTrue(secondPushTimeMSec > first);
        Assert.assertTrue(secondPushTimeMSec > first);
        Assert.assertTrue(secondPushTimeMSec > firstPushTimeMSec);

        long diffMSec = (secondPushTimeMSec - firstPushTimeMSec);
        Assert.assertTrue((diffMSec >= 100) && (diffMSec < 110));

        // we just pushed less than 25msec ago so there will be a delay
        Assert.assertFalse(pusher.adjustPeriod(50));
        Assert.assertEquals(pusher.getCurrentPeriodMSec(), 50);
        delayForPush(60);

        long thirdPushTimeMSec = pusher.getLastPushTimeMSec();
        Assert.assertTrue(thirdPushTimeMSec > second);
        Assert.assertTrue(thirdPushTimeMSec > second);
        Assert.assertTrue(thirdPushTimeMSec > secondPushTimeMSec);

        diffMSec = (thirdPushTimeMSec - secondPushTimeMSec);
        Assert.assertTrue((diffMSec >= 50) && (diffMSec < 60));

        Assert.assertFalse(pusher.adjustPeriod(Long.MAX_VALUE));
        Assert.assertEquals(pusher.getCurrentPeriodMSec(), Long.MAX_VALUE);
        Assert.assertFalse(pusher.isTimerActive());
    }

    @Test
    public void testAdjustPeriodLonger() {
        mySearchPusher pusher = new mySearchPusher(true);
        Assert.assertFalse(pusher.isTimerActive());

        Assert.assertTrue(pusher.adjustPeriod(50)); // no delay
        Assert.assertEquals(pusher.getCurrentPeriodMSec(), 50);
        long start = System.currentTimeMillis();
        Assert.assertTrue(pusher.isTimerActive());

        delayForPush(60);
        long first = System.currentTimeMillis();
        long firstPushTimeMSec = pusher.getLastPushTimeMSec();
        Assert.assertTrue(firstPushTimeMSec > start);
        Assert.assertTrue(firstPushTimeMSec < first);

        delayForPush(60);
        long second = System.currentTimeMillis();
        long secondPushTimeMSec = pusher.getLastPushTimeMSec();
        Assert.assertTrue(secondPushTimeMSec > first);
        Assert.assertTrue(secondPushTimeMSec > first);
        Assert.assertTrue(secondPushTimeMSec > firstPushTimeMSec);

        long diffMSec = (secondPushTimeMSec - firstPushTimeMSec);
        Assert.assertTrue((diffMSec >= 50) && (diffMSec < 60));

        // we just pushed less than 25msec ago so there will be a delay
        Assert.assertFalse(pusher.adjustPeriod(100));
        Assert.assertEquals(pusher.getCurrentPeriodMSec(), 100);
        delayForPush(50);
        Assert.assertEquals(pusher.getLastPushTimeMSec(), secondPushTimeMSec);
        delayForPush(60);

        long thirdPushTimeMSec = pusher.getLastPushTimeMSec();
        Assert.assertTrue(thirdPushTimeMSec > second);
        Assert.assertTrue(thirdPushTimeMSec > second);
        Assert.assertTrue(thirdPushTimeMSec > secondPushTimeMSec);

        diffMSec = (thirdPushTimeMSec - secondPushTimeMSec);
        Assert.assertTrue((diffMSec >= 100) && (diffMSec < 110));

        Assert.assertFalse(pusher.adjustPeriod(Long.MAX_VALUE));
        Assert.assertEquals(pusher.getCurrentPeriodMSec(), Long.MAX_VALUE);
        Assert.assertFalse(pusher.isTimerActive());
    }

    public class mySearchPusher extends SearchPusher {

        public mySearchPusher(boolean isAlarmSearch) {
            super(isAlarmSearch);
        }

        protected void push() {
        }

        public boolean isTimerActive() {
            return super.isTimerActive();
        }

        // for our testing
        public long getCurrentPeriodMSec() {
            return this.currentPeriodMSec;
        }

        public long getLastPushTimeMSec() {
            return this.lastPushTimeMSec;
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
