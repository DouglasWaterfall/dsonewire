package waterfall.onewire.busmasters.Http;

import waterfall.onewire.DSAddress;
import waterfall.onewire.HttpClient.ConvertTCmdResult;
import waterfall.onewire.busmaster.ConvertTCmd;

/**
 * Created by dwaterfa on 8/7/16.
 */
public class HttpConvertTCmd extends ConvertTCmd {

    private String suffix = null;

    public HttpConvertTCmd(Client client, DSAddress dsAddr, boolean log) {
        super(client, dsAddr, log);
    }

    public void setResultWriteCTM(long resultWriteCTM) {
        assert (result == Result.busy);
        this.resultWriteCTM = resultWriteCTM;
    }

    protected Result execute_internal() {
        assert (result == Result.busy);
        assert (resultWriteCTM == 0);

        if (suffix == null) {
            suffix = "convertTCmd/" + ((Client)busMaster).getBmIdent() + "/" + dsAddr.toString() + ((getLogger() != null) ? "?log=true" : "");

        }

        ConvertTCmdResult postResult = (ConvertTCmdResult) ((Client) busMaster).postURLDataWithAuthorization(suffix, ConvertTCmdResult.class);

        if (postResult.postError != null) {

            if (getLogger() != null) {
                getLogger().logError(this.getClass().getSimpleName(),  " postError:" + postResult.postError.name());
            }

            return Result.communication_error;
        }

        if (postResult.controllerError != null) {

            if (getLogger() != null) {
                getLogger().logError(this.getClass().getSimpleName(), " controllerError:" + postResult.controllerError);
            }

            return Result.communication_error;
        }

        try {
            return Enum.valueOf(Result.class, postResult.result);
        }
        catch (IllegalArgumentException e) {
            if (getLogger() != null) {
                getLogger().logError(this.getClass().getSimpleName(), " bad result enum:" + postResult.result);
            }

            return Result.communication_error;
        }
    }
}
