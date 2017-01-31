package waterfall.onewire;

import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.StartBusCmd;
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
        this.bmRegistry = bmRegistry;
    }

    public void startTTY(String ha7sTTYArg, Class ttyClass, Logger.LogLevel logLevel) {
        Constructor c = null;
        try {
            c = ttyClass.getConstructor(java.lang.String.class);
        }
        catch (NoSuchMethodException e) {
            System.err.println("Class " + ttyClass.toString() + " does not have String constructor");
            return;
        }

        String[] bmPaths = ha7sTTYArg.split(",");

        for (String s : bmPaths) {
            try {
                HA7S ha7s = new HA7S((HA7SSerial)c.newInstance(s));

                startAndRegister(ha7s, logLevel);
            }
            catch (IllegalAccessException e) {
                System.err.println(e);
                return;
            }
            catch (InstantiationException e) {
                System.err.println(e);
                return;
            }
            catch (InvocationTargetException e) {
                System.err.println(e);
                return;
            }
        }
    }

    private void startAndRegister(HA7S ha7s, Logger.LogLevel logLevel) {
        System.out.println("Starting dsonewire-busmasterserver on " + ha7s.getName());

        StartBusCmd startCmd = ha7s.queryStartBusCmd(logLevel);
        StartBusCmd.Result startResult = startCmd.execute();

        // dumpLog(startCmd.getLogger());

        if (startResult == StartBusCmd.Result.started) {
            bmRegistry.addBusMaster(ha7s);
        }
        else {
            System.out.println("Failed on " + ha7s.getName() + ": " + startResult.name());
        }
    }
}

