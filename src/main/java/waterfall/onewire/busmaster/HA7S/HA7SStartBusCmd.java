package waterfall.onewire.busmaster.HA7S;

import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.StartBusCmd;

/**
 * Created by dwaterfa on 6/11/16.
 */
public class HA7SStartBusCmd extends StartBusCmd {

    public HA7SStartBusCmd(HA7S ha7s, Logger optLogger) {
        super(ha7s, optLogger);
    }

    protected StartBusCmd.Result execute_internal() {
        assert (result == StartBusCmd.Result.busy);

        return ((HA7S) busMaster).executeStartBusCmd(this);
    }

}
