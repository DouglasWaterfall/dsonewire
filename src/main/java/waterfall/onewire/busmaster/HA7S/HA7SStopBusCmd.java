package waterfall.onewire.busmaster.HA7S;

import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.StopBusCmd;

/**
 * Created by dwaterfa on 6/11/16.
 */
public class HA7SStopBusCmd extends StopBusCmd {

    public HA7SStopBusCmd(HA7S ha7s, boolean log) {
        super(ha7s, log);
    }

    protected Result execute_internal() {
        assert (result == Result.busy);

        return ((HA7S) busMaster).executeStopBusCmd(this);
    }

}
