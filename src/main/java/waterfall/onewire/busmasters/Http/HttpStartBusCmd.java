package waterfall.onewire.busmasters.Http;

import waterfall.onewire.busmaster.StartBusCmd;

/**
 * Created by dwaterfa on 8/7/16.
 */
public class HttpStartBusCmd extends StartBusCmd {

    public HttpStartBusCmd(Client client, LogLevel logLevel) {
        super(client, logLevel);
    }

    protected Result execute_internal() {
        assert (result == Result.busy);

        return ((Client) busMaster).StartBusCmd(getLogger());
    }
}
