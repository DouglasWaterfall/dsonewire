package waterfall.onewire.busmasters.Http.Client;

/**
 * Created by dwaterfa on 8/8/16.
 */
public class StopBusCmd extends waterfall.onewire.busmaster.StopBusCmd {

    public StopBusCmd(Client client, LogLevel logLevel) {
        super(client, logLevel);
    }

    protected Result execute_internal() {
        assert (result == Result.busy);

        return ((Client) busMaster).StopBusCmd(getLogger());
    }
}
