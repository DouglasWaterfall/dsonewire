package waterfall.onewire.busmasters.Http.Client;

import waterfall.onewire.HttpClient.SearchBusCmdResult;
import waterfall.onewire.busmasters.Util;

import java.util.List;

/**
 * Created by dwaterfa on 8/17/16.
 */
public class SearchBusCmd extends waterfall.onewire.busmaster.SearchBusCmd {
    private String suffix = null;

    public SearchBusCmd(Client client, boolean byAlarm, LogLevel logLevel) {
        super(client, byAlarm, logLevel);
        this.resultData = null;
    }

    public SearchBusCmd(Client client, short familyCode, LogLevel logLevel) {
        super(client, familyCode, logLevel);
        this.resultData = null;
    }

    protected Result execute_internal() {
        assert (result == Result.busy);
        assert (resultData == null);

        String logLevelParam = Util.computeLogLevelParam(getLogger());
        final String suffix = "searchBusCmd/" + ((Client)busMaster).getBmIdent() + ((logLevelParam != null) ? logLevelParam : "");

        SearchBusCmdResult postResult = (SearchBusCmdResult) ((Client) busMaster).postURLDataWithAuthorization(suffix, SearchBusCmdResult.class);

        if (postResult.hasPostError()) {
            logErrorInternal("postError:" + postResult.getPostError().name());
            return Result.communication_error;
        }

        if (postResult.controllerError != null) {
            logErrorInternal(" controllerError:" + postResult.controllerError);
            return Result.communication_error;
        }

        try {
            Result result = Enum.valueOf(Result.class, postResult.result);

            if (result == Result.success) {
                setResultData(postResult.resultWriteCTM + ((Client)busMaster).getRemoteTimeDiffMSec(), postResult.resultList);
            }

            return result;
        }
        catch (IllegalArgumentException e) {
            logErrorInternal(" bad result enum:" + postResult.result);
            return Result.communication_error;
        }
    }

    public void setResultData(long resultWriteCTM, List<String> resultList) {
        assert (result == Result.busy);
        this.resultData = new ResultData(resultList, resultWriteCTM);
    }

    private void logErrorInternal(String str) {
        if ((getLogger() != null) && (getLogLevel().isLevelComm())) {
            final String logContext = this.getClass().getSimpleName() + " bmIdent:" + ((Client)busMaster).getBmIdent() + " ";
            getLogger().logError(logContext, str);
        }
    }

}
