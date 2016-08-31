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

    public HttpReadScratchpadCmd(Client client, DSAddress dsAddr, short requestCount, boolean log) {
        super(client, dsAddr, requestCount, log);
    }

    public void setResultData(byte[] resultData, byte[] resultHexData) {
        assert (result == Result.busy);
        this.resultData = resultData;
        this.resultHexData = resultHexData;
    }

    public void setResultWriteCTM(long resultWriteCTM) {
        assert (result == Result.busy);
        this.resultWriteCTM = resultWriteCTM;
    }

    protected Result execute_internal() {
        assert (result == Result.busy);
        assert (resultWriteCTM == 0);

        if (suffix == null) {
            suffix = "readScratchpadCmd/" + ((Client)busMaster).getBmIdent() + "/" + dsAddr.toString() + "/" + requestByteCount + ((getLogger() != null) ? "?log=true" : "");
        }

        ReadScratchpadCmdResult postResult = (ReadScratchpadCmdResult) ((Client) busMaster).postURLDataWithAuthorization(suffix, ReadScratchpadCmdResult.class);

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
                setResultData(Convert.hexToByte(postResult.resultHexData), postResult.resultHexData);
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
