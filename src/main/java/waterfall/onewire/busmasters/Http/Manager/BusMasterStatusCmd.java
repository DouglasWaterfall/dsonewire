package waterfall.onewire.busmasters.Http.Manager;

import waterfall.onewire.HttpClient.StatusCmdResult;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmasters.Http.Client.Client;

/**
 * Created by dwaterfa on 1/20/17.
 */
public class BusMasterStatusCmd {

    public static StatusCmdResult go(String endpoint, String bmIdent, Logger optLogger) {

        final String logContext = (optLogger != null) ? Client.class.getSimpleName() + ".BusMasterStatusCmd bmIdent:" + bmIdent + " " : "";
        final String statusSuffix = "busStatusCmd/" + bmIdent;

        StatusCmdResult statusPostResult = (StatusCmdResult) Client.postURLDataNoAuthorization(endpoint, statusSuffix, StatusCmdResult.class);

        if (statusPostResult.postError != null) {
            Client.logErrorCommLevel(optLogger, logContext, " postError:" + statusPostResult.postError.name());
        } else if (statusPostResult.controllerError != null) {
            Client.logErrorCommLevel(optLogger, logContext, " controllerError:" + statusPostResult.controllerError);
        }

        return statusPostResult;
    }
}
