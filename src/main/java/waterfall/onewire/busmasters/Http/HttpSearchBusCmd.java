package waterfall.onewire.busmasters.Http;

import waterfall.onewire.HttpClient.SearchBusCmdResult;
import waterfall.onewire.busmaster.SearchBusCmd;

import java.util.List;

/**
 * Created by dwaterfa on 8/17/16.
 */
public class HttpSearchBusCmd extends SearchBusCmd {
    private String suffix = null;

    public HttpSearchBusCmd(Client client, boolean byAlarm, boolean log) {
        super(client, byAlarm, log);
        this.resultList = null;
    }

    public HttpSearchBusCmd(Client client, short familyCode, boolean log) {
        super(client, familyCode, log);
        this.resultList = null;
    }

    protected Result execute_internal() {
        assert (result == Result.busy);
        assert (resultList == null);
        assert (resultWriteCTM == 0);

        final String logContext = (getLogger() != null) ? this.getClass().getSimpleName() + " bmIdent:" + ((Client)busMaster).getBmIdent() + " " : "";
        final String suffix = "searchBusCmd/" + ((Client)busMaster).getBmIdent() + ((getLogger() != null) ? "?log=true" : "");

        SearchBusCmdResult postResult = (SearchBusCmdResult) ((Client) busMaster).postURLDataWithAuthorization(suffix, SearchBusCmdResult.class);

        if (postResult.postError != null) {

            if (getLogger() != null) {
                getLogger().logError(logContext,  "postError:" + postResult.postError.name());
            }

            return Result.communication_error;
        }

        if (postResult.controllerError != null) {

            if (getLogger() != null) {
                getLogger().logError(logContext, " controllerError:" + postResult.controllerError);
            }

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
            if (getLogger() != null) {
                getLogger().logError(logContext, " bad result enum:" + postResult.result);
            }

            return Result.communication_error;
        }
    }

    public void setResultData(long resultWriteCTM, List<String> resultList) {
        assert (result == Result.busy);
        this.resultWriteCTM = resultWriteCTM;
        this.resultList = resultList;
    }

}
