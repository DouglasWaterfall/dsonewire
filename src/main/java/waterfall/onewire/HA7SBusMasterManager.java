package waterfall.onewire;

import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.StartBusCmd;
import waterfall.onewire.busmaster.StopBusCmd;
import waterfall.onewire.busmasters.HA7S.HA7S;
import waterfall.onewire.busmasters.HA7S.HA7SSerial;
import waterfall.onewire.busmasters.HA7S.JSSC;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * Created by dwaterfa on 12/10/16.
 */
public class HA7SBusMasterManager {
    private final BusMasterRegistry bmRegistry;
    private ArrayList<HA7S> bmList = new ArrayList<>();

    public HA7SBusMasterManager(BusMasterRegistry bmRegistry) {
        if (bmRegistry == null) {
            throw new IllegalArgumentException("bmRegistry");
        }
        this.bmRegistry = bmRegistry;
    }

    private String[] checkTTYArg(String ttyArg) {
        final String exceptionStr = "ha7sTTYArg";
        String[] bmPaths = null;

        if ((ttyArg == null) || ((bmPaths = ttyArg.split(",")).length == 0)) {
            throw new IllegalArgumentException(exceptionStr);
        }

        for (String s: bmPaths) {
            if (s.isEmpty()) {
                throw new IllegalArgumentException(exceptionStr);
            }
        }

        return bmPaths;
    }

    public synchronized HA7S[] start(String ha7sTTYArg, Class ttyClass, Logger.LogLevel logLevel) throws IllegalArgumentException, NoSuchMethodException {

        String[] bmPaths = checkTTYArg(ha7sTTYArg);

        if ((ttyClass == null) || (ttyClass.isAssignableFrom(HA7SSerial.class))) {
            throw new IllegalArgumentException("ttyClass");
        }

        if (logLevel == null) {
            throw new IllegalArgumentException("logLevel");
        }

        Constructor c = null;
        try {
            c = ttyClass.getConstructor(java.lang.String.class);
        } catch (NoSuchMethodException e) {
            System.err.println("Class " + ttyClass.toString() + " does not have String constructor");
            throw e;
        }

        ArrayList<HA7S> addedList = new ArrayList<>();

        for (String s : bmPaths) {

            HA7S ha7s = null;

            try {
                ha7s = new HA7S((HA7SSerial) c.newInstance(s));
            } catch (IllegalAccessException e) {
                System.err.println(e);
            } catch (InstantiationException e) {
                System.err.println(e);
            } catch (InvocationTargetException e) {
                System.err.println(e);
            }

            if (ha7s == null) {
                throw new IllegalArgumentException("cannot instantiate");
            }

            StartBusCmd startCmd = ha7s.queryStartBusCmd(logLevel);
            StartBusCmd.Result startResult = startCmd.execute();

            // dumpLog(startCmd.getLogger());

            if (startResult == StartBusCmd.Result.started) {
                bmRegistry.addBusMaster(ha7s);
                addedList.add(ha7s);
            } else {
                System.err.println("Failed on " + ha7s.getName() + ": " + startResult.name());
            }
        }

        return addedList.toArray(new HA7S[addedList.size()]);
    }

    public synchronized void stop(HA7S ha7s) {
        if (ha7s == null) {
            throw new IllegalArgumentException("ha7s");
        }

        if (ha7s == null) {
            throw new IllegalArgumentException("ha7s is not started");
        }

        bmRegistry.removeBusMaster(ha7s);

        StopBusCmd stopCmd = ha7s.queryStopBusCmd(Logger.LogLevel.CmdOnlyLevel());
        StopBusCmd.Result stopResult = stopCmd.execute();

        if (stopResult != StopBusCmd.Result.stopped) {
            System.err.println("Stop failed result:" + stopResult.name());
        }
    }

}

