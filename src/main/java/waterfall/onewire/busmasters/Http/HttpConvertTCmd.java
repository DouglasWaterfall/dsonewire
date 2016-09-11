package waterfall.onewire.busmasters.Http;

import waterfall.onewire.DSAddress;
import waterfall.onewire.HttpClient.ConvertTCmdResult;
import waterfall.onewire.busmaster.ConvertTCmd;
import waterfall.onewire.busmaster.Logger;

/**
 * Created by dwaterfa on 8/7/16.
 */
public class HttpConvertTCmd extends ConvertTCmd {

    private String suffix = null;

    public HttpConvertTCmd(Client client, DSAddress dsAddr, LogLevel logLevel) {
        super(client, dsAddr, logLevel);
    }

    public void setResultData(long resultWriteCTM) {
        assert (result == Result.busy);
        this.resultWriteCTM = resultWriteCTM;
    }

    protected Result execute_internal() {
        assert (result == Result.busy);
        assert (resultWriteCTM == 0);

        if (suffix == null) {
            String logLevelParam = ((Client)busMaster).computeLogLevelParam(getLogger());
            suffix = "convertTCmd/" + ((Client)busMaster).getBmIdent() + "/" + dsAddr.toString() + ((logLevelParam != null) ? logLevelParam : "");
        }

        ConvertTCmdResult postResult = (ConvertTCmdResult) ((Client) busMaster).postURLDataWithAuthorization(suffix, ConvertTCmdResult.class);

        if (postResult.postError != null) {
            logErrorInternal(" postError:" + postResult.postError.name());
            return Result.communication_error;
        }

        if (postResult.controllerError != null) {
            logErrorInternal(" controllerError:" + postResult.controllerError);
            return Result.communication_error;
        }

        try {
            Result result = Enum.valueOf(Result.class, postResult.result);

            if (result == Result.success) {
                setResultData(postResult.resultWriteCTM + ((Client)busMaster).getRemoteTimeDiffMSec());
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
