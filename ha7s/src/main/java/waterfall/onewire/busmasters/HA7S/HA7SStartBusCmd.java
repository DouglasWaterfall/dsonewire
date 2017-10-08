package waterfall.onewire.busmasters.HA7S;

import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.StartBusCmd;

/**
 * Created by dwaterfa on 6/11/16.
 */
public class HA7SStartBusCmd extends StartBusCmd {

    public HA7SStartBusCmd(HA7S ha7s) {
        super(ha7s);
    }

    protected StartBusCmd.Result execute_internal() {
        assert (result == StartBusCmd.Result.busy);

        return ((HA7S) busMaster).executeStartBusCmd(this);
    }

    public Logger getDeviceLevelLogger() {
        if ((getLogger() != null) && (getLogLevel().isLevelDevice())) {
            return getLogger();
        }
        return null;
    }

    public void logErrorInternal(String str) {
        if ((getLogger() != null) && (getLogLevel().isLevelDevice())) {
            getLogger().logError(this.getClass().getSimpleName(), str);
        }
    }

}
