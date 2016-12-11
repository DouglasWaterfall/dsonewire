package waterfall.onewire;

import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.StartBusCmd;
import waterfall.onewire.busmasters.HA7S.HA7S;

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

    public void start(String ha7sTTYArg, Logger.LogLevel logLevel) {
        String[] bmPaths = ha7sTTYArg.split(",");

        for (String s : bmPaths) {
            HA7S bm = new HA7S(s);

            System.out.println("Starting dsonewire-busmasterserver on " + s);

            StartBusCmd startCmd = bm.queryStartBusCmd(logLevel);
            StartBusCmd.Result startResult = startCmd.execute();

            // dumpLog(startCmd.getLogger());

            if (startResult == StartBusCmd.Result.started) {
                bmRegistry.addBusMaster(bm);
            }
            else {
                System.out.println("Failed on " + s + ": " + startResult.name());
            }
        }
    }
}

