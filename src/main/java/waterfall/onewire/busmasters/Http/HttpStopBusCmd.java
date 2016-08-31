package waterfall.onewire.busmasters.Http;

import waterfall.onewire.busmaster.StopBusCmd;

/**
 * Created by dwaterfa on 8/8/16.
 */
public class HttpStopBusCmd extends StopBusCmd {

    public HttpStopBusCmd(Client client, boolean log) {
        super(client, log);
    }

    protected Result execute_internal() {
        assert (result == Result.busy);

        return ((Client) busMaster).StopBusCmd(getLogger());
    }
}
