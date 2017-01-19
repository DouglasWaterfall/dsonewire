package waterfall.onewire.httpserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import waterfall.onewire.BusMasterRegistry;
import waterfall.onewire.HA7SBusMasterManager;
import waterfall.onewire.busmaster.*;


@SpringBootApplication
@EnableScheduling
public class Application {

    // The Controller autowires the BusMasterRegistry and will automatically see any of the BusMasters the
    // HA7SBusMasterManager registers.
    @Autowired
    Controller  controller;

    @Autowired
    BusMasterRegistry busMasterRegistry;

    // This knows how to find HA7S BusMasters and autowires the BusMasterRegistry and will register any that it finds
    // and successfully starts.
    @Autowired
    HA7SBusMasterManager ha7sBMManager;

    // We just need to start the HA7S manager with the arguments from the command line who will register any that it
    // finds with the BusMasterRegistry.
    private void startTTY(String ha7sTTY) {
        ha7sBMManager.startTTY(ha7sTTY, Logger.LogLevel.CmdOnlyLevel());
    }

    private void startDummy() {
        ha7sBMManager.startDummy(Logger.LogLevel.CmdOnlyLevel());
    }

    @Bean
    public BusMasterRegistry getBusMasterRegistry() {
        return new BusMasterRegistry();
    }

    @Bean
    public HA7SBusMasterManager getHa7sBMManager() {
        return new HA7SBusMasterManager(busMasterRegistry);
    }

    public static void main(String[] args) {
        PropertySource ps = new SimpleCommandLinePropertySource(args);
        String ha7sTTY = (String)ps.getProperty("ha7sTTY");
        if ((ha7sTTY == null) || ("".equals(ha7sTTY))) {
            // such as /dev/ttyAMA0 or /dev/ttyS0
            System.err.println("Error, must specify --ha7sTTY={path_to_serial_port}{,path,...}");
            System.exit(1);
        }

        ApplicationContext ctx = SpringApplication.run(Application.class, args);
        Application app = (Application) ctx.getBean(Application.class);
        if (false) {
            app.startTTY(ha7sTTY);
        }
        else {
            app.startDummy();
        }

        // It does not matter if the this thread exists, the other threads created by the various Objects will continue.
    }

}

/*
    final static long fifteen_seconds = (1000 * 15);
    final static long thirty_seconds = (1000 * 30);

    class mySearchBusNotify implements NotifySearchBusCmdResult {
        private int countToDeregister;
        private long start;
        private SearchBusCmd.ResultData resultData;

        public mySearchBusNotify(int countToDeregister) {
            reset(countToDeregister);
        }

        public int getCountToDeregister() { return countToDeregister; }

        public SearchBusCmd.ResultData getNotifyResultData() { return resultData; }

        public void reset(int countToDeregister) {
            this.countToDeregister = countToDeregister;
            this.start = System.currentTimeMillis();
            this.resultData = null;
        }

        @Override
        public void notify(final BusMaster bm, final SearchBusCmd.ResultData resultData) {
            System.out.println("Notify! delta:" + (System.currentTimeMillis() - start) + " devices:" + resultData.getList().size());

            this.resultData = resultData;

            if (--countToDeregister <= 0) {
                if (!bm.cancelSearchNotifyFor(this)) {
                    System.err.println("mySearchBusNotify - cancel failed!");
                }
                else {
                    System.err.println("mySearchBusNotify - cancelled");
                }
            }
        }
    }

    private static boolean startBM(BusMaster busmaster, Logger.LogLevel logLevel) {
        if (busmaster.getIsStarted()) {
            System.out.println("start(" + busmaster.getName() + "): already started");
            return true;
        }

        StartBusCmd startCmd = busmaster.queryStartBusCmd(logLevel);
        StartBusCmd.Result startResult = startCmd.execute();

        // dumpLog(startCmd.getLogger());

        if ((startResult == StartBusCmd.Result.started) || (startResult == StartBusCmd.Result.already_started)) {
            return true;
        }

        return false;
    }


// 1. Try to register with a negative time period
System.out.println("test 1");
        mySearchBusNotify t1 = new mySearchBusNotify(1);
        BusMaster.ScheduleNotifySearchBusCmdResult result = bm.scheduleNotifySearchBusCmd(t1, -1);
        if (result != BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_MinPeriodInvalid) {
        System.err.println("wrong error:" + result.name());
        return;
        }

        // 2. Try to register with a zero time period
        System.out.println("test 2");
        result = bm.scheduleNotifySearchBusCmd(t1, 0);
        if (result != BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_MinPeriodInvalid) {
        System.err.println("wrong error:" + result.name());
        return;
        }

        // 3. Try to register with a null Notify Object
        System.out.println("test 3");
        result = bm.scheduleNotifySearchBusCmd(null, thirty_seconds);
        if (result != BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_NotifyObjNull) {
        System.err.println("wrong error:" + result.name());
        return;
        }

        // 4. Try to register without the BM being started
        System.out.println("test 4");
        result = bm.scheduleNotifySearchBusCmd(t1, thirty_seconds);
        if (result != BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_BusMasterNotStarted) {
        System.err.println("wrong error:" + result.name());
        return;
        }

        System.out.println("starting bm");
        if (!startBM(bm, Logger.LogLevel.CmdOnlyLevel())) {
        System.err.println(bm.getName() + " failed to start");
        return;
        }

        // 5. Try to register same object more than once
        System.out.println("test 5");
        result = bm.scheduleNotifySearchBusCmd(t1, thirty_seconds);
        if (result != BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success) {
        System.err.println("5a:wrong error:" + result.name());
        return;
        }
        result = bm.scheduleNotifySearchBusCmd(t1, fifteen_seconds);
        if (result != BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_NotifyObjAlreadyScheduled) {
        System.err.println("5b:wrong error:" + result.name());
        return;
        }
        // we will get one event and then it will deregister.
        int timeoutCount = 10;
        while (t1.getCountToDeregister() > 0) {
        if (--timeoutCount == 0) {
        System.err.println("5:gave up waiting for notify");
        return;
        }
        try {
        Thread.sleep(1000);
        } catch (Exception e) {
        ;
        }
        }

        // 6. See that a later request will re-use the same data if it is in the time period
        System.out.println("test 6");
        long saveWriteCTM = t1.getNotifyResultData().getWriteCTM();
        t1.reset(1);
        result = bm.scheduleNotifySearchBusCmd(t1, thirty_seconds);
        if (result != BusMaster.ScheduleNotifySearchBusCmdResult.SNSBCR_Success) {
        System.err.println("6a: wrong error:" + result.name());
        return;
        }
        // we will get one event and then it will deregister.
        timeoutCount = 10;
        while (t1.getCountToDeregister() > 0) {
        if (--timeoutCount == 0) {
        System.err.println("6:gave up waiting for notify");
        return;
        }
        try {
        Thread.sleep(1000);
        } catch (Exception e) {
        ;
        }
        }
        if (t1.getNotifyResultData().getWriteCTM() != saveWriteCTM) {
        System.err.println("6: Result writeCTM did not match");
        return;
        }

        System.out.println("Done");

     * Some ideas for Tests:
     * Negative Tests:
     *
     * Positive Tests
     * Start the BM.
     * 1. Register a NotifyObject with time period of 1 minute
     *      expect a call back in less than a minute (expected started right away)
     *      deregister the NotifyObject immediately (so before the 1 minute repeated)
     *      wait for 30 seconds
     *      register and expect a call back immediately with the same writeCTM as before.
     *      deregister the NotifyObject immediately.
     *      wait for 90 seconds
     *      register and expect a call back with writeCTM after our register (so no using the old data)
     *      deregister the NotifyObject immediately.
     *
     * - test rate changes
     * - test removing someone and rate changes
     * - test removing someone and the rate continues.
     * - test a search which occurs by an outside agent leads to a callback
*/
