package waterfall.onewire.busmasters.Http.Client;

import waterfall.onewire.DSAddress;
import waterfall.onewire.HttpClient.ReadPowerSupplyCmdResult;
import waterfall.onewire.busmasters.Util;

/**
 * Created by dwaterfa on 7/27/16.
 */
public class ReadPowerSupplyCmd extends waterfall.onewire.busmaster.ReadPowerSupplyCmd {

    private String suffix = null;

    public ReadPowerSupplyCmd(Client client, DSAddress dsAddr, LogLevel logLevel) {
        super(client, dsAddr, logLevel);
    }

    public void setResultData(long resultWriteCTM, boolean isParasitic) {
        assert (result == Result.busy);
        this.resultWriteCTM = resultWriteCTM;
        this.resultIsParasitic = isParasitic;
    }

    protected Result execute_internal() {
        assert (result == Result.busy);
        assert (resultWriteCTM == 0);

        if (suffix == null) {
            String logLevelParam = Util.computeLogLevelParam(getLogger());
            suffix = "readPowerSupplyCmd/" + ((Client)busMaster).getBmIdent() + "/" + dsAddr.toString() + ((logLevelParam != null) ? logLevelParam : "");
        }

        ReadPowerSupplyCmdResult postResult = (ReadPowerSupplyCmdResult) ((Client) busMaster).postURLDataWithAuthorization(suffix, ReadPowerSupplyCmdResult.class);

        if (postResult.hasPostError()) {
            logErrorInternal(" postError:" + postResult.getPostError().name());
            return Result.communication_error;
        }

        if (postResult.controllerError != null) {
            logErrorInternal(" controllerError:" + postResult.controllerError);
            return Result.communication_error;
        }

        try {
            Result result = Enum.valueOf(Result.class, postResult.result);

            if (result == Result.success) {
                setResultData(postResult.resultWriteCTM + ((Client)busMaster).getRemoteTimeDiffMSec(), postResult.isParasitic);
            }

            return result;
        }
        catch (IllegalArgumentException e) {
            logErrorInternal(" bad result enum:" + postResult.result);
            return Result.communication_error;
        }
    }

    private void logErrorInternal(String str) {
        if ((getLogger() != null) && (getLogLevel().isLevelComm())) {
            getLogger().logError(this.getClass().getSimpleName(), str);
        }
    }
}

