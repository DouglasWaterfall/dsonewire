package waterfall.onewire.busmasters.Http;

import waterfall.onewire.DSAddress;
import waterfall.onewire.HttpClient.ReadPowerSupplyCmdResult;
import waterfall.onewire.busmaster.ReadPowerSupplyCmd;

/**
 * Created by dwaterfa on 7/27/16.
 */
public class HttpReadPowerSupplyCmd extends ReadPowerSupplyCmd {

    private String suffix = null;

    public HttpReadPowerSupplyCmd(Client client, DSAddress dsAddr, boolean log) {
        super(client, dsAddr, log);
    }

    public void setResultIsParasitic(boolean isParasitic) {
        assert (result == Result.busy);
        this.resultIsParasitic = isParasitic;
    }

    public void setResultWriteCTM(long resultWriteCTM) {
        assert (result == Result.busy);
        this.resultWriteCTM = resultWriteCTM;
    }

    protected Result execute_internal() {
        assert (result == Result.busy);
        assert (resultWriteCTM == 0);

        if (suffix == null) {
            suffix = "readPowerSupplyCmd/" + ((Client)busMaster).getBmIdent() + "/" + dsAddr.toString() + ((getLogger() != null) ? "?log=true" : "");
        }

        ReadPowerSupplyCmdResult postResult = (ReadPowerSupplyCmdResult) ((Client) busMaster).postURLDataWithAuthorization(suffix, ReadPowerSupplyCmdResult.class);

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
            Result result = Enum.valueOf(Result.class, postResult.result);

            if (result == Result.success) {
                setResultIsParasitic(postResult.isParasitic);
            }

            return result;
        }
        catch (IllegalArgumentException e) {
            if (getLogger() != null) {
                getLogger().logError(this.getClass().getSimpleName(), " bad result enum:" + postResult.result);
            }

            return Result.communication_error;
        }
    }

}

