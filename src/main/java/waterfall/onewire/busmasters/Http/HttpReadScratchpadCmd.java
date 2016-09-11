package waterfall.onewire.busmasters.Http;

import waterfall.onewire.Convert;
import waterfall.onewire.DSAddress;
import waterfall.onewire.HttpClient.ReadScratchpadCmdResult;
import waterfall.onewire.busmaster.ReadScratchpadCmd;

/**
 * Created by dwaterfa on 8/7/16.
 */
public class HttpReadScratchpadCmd extends ReadScratchpadCmd {

    private String suffix = null;

    public HttpReadScratchpadCmd(Client client, DSAddress dsAddr, short requestCount, LogLevel logLevel) {
        super(client, dsAddr, requestCount, logLevel);
    }

    public void setResultData(long resultWriteCTM, byte[] resultData, byte[] resultHexData) {
        assert (result == Result.busy);
        this.resultWriteCTM = resultWriteCTM;
        this.resultData = resultData;
        this.resultHexData = resultHexData;
    }

    protected Result execute_internal() {
        assert (result == Result.busy);
        assert (resultWriteCTM == 0);

        if (suffix == null) {
            String logLevelParam = ((Client)busMaster).computeLogLevelParam(getLogger());
            suffix = "readScratchpadCmd/" + ((Client)busMaster).getBmIdent() + "/" + dsAddr.toString() + "/" + requestByteCount + ((logLevelParam != null) ? logLevelParam : "");
        }

        ReadScratchpadCmdResult postResult = (ReadScratchpadCmdResult) ((Client) busMaster).postURLDataWithAuthorization(suffix, ReadScratchpadCmdResult.class);

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
                setResultData(postResult.resultWriteCTM + ((Client)busMaster).getRemoteTimeDiffMSec(), Convert.hexToByte(postResult.resultHexData), postResult.resultHexData);
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
