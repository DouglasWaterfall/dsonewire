package waterfall.onewire.busmasters.HA7S;

import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.StopBusCmd;

/**
 * Created by dwaterfa on 6/11/16.
 */
public class HA7SStopBusCmd extends StopBusCmd {

    public HA7SStopBusCmd(HA7S ha7s) {
        super(ha7s);
    }

    protected Result execute_internal() {
        assert (result == Result.busy);

        return ((HA7S) busMaster).executeStopBusCmd(this);
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
