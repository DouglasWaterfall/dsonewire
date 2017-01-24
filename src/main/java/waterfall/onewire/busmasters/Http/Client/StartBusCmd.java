package waterfall.onewire.busmasters.Http.Client;

/**
 * Created by dwaterfa on 8/7/16.
 */
public class StartBusCmd extends waterfall.onewire.busmaster.StartBusCmd {

    public StartBusCmd(Client client, LogLevel logLevel) {
        super(client, logLevel);
    }

    protected Result execute_internal() {
        assert (result == Result.busy);

        return ((Client) busMaster).StartBusCmd(getLogger());
    }
}
