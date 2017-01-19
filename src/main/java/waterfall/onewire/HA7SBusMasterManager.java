package waterfall.onewire;

import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.StartBusCmd;
import waterfall.onewire.busmasters.HA7S.HA7S;
import waterfall.onewire.busmasters.HA7S.HA7SSerialDummy;
import waterfall.onewire.busmasters.HA7S.JSSC;

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

    public void startTTY(String ha7sTTYArg, Logger.LogLevel logLevel) {
        String[] bmPaths = ha7sTTYArg.split(",");

        for (String s : bmPaths) {
            HA7S ha7s = new HA7S(new JSSC(s));

            startAndRegister(ha7s, logLevel);
        }
    }

    public void startDummy(Logger.LogLevel logLevel) {
        HA7S ha7s = new HA7S(new HA7SSerialDummy());

        startAndRegister(ha7s, logLevel);
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

