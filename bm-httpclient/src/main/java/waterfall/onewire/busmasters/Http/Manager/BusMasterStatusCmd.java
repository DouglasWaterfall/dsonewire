package waterfall.onewire.busmasters.Http.Manager;

import waterfall.onewire.HttpClient.StatusCmdResult;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmasters.Http.Client.Client;
import waterfall.onewire.busmasters.Util;

/**
 * Created by dwaterfa on 1/20/17.
 */
public class BusMasterStatusCmd {

    public static StatusCmdResult go(String endpoint, String bmIdent, Logger optLogger) {

        final String logContext = (optLogger != null) ? Client.class.getSimpleName() + ".BusMasterStatusCmd bmIdent:" + bmIdent + " " : "";
        final String statusSuffix = "busStatusCmd/" + bmIdent;

        StatusCmdResult statusPostResult = (StatusCmdResult) Util.postURLDataNoAuthorization(endpoint, statusSuffix, StatusCmdResult.class);

        if (statusPostResult.hasPostError()) {
            Util.logErrorCommLevel(optLogger, logContext, " postError:" + statusPostResult.getPostError().name());
        } else if (statusPostResult.controllerError != null) {
            Util.logErrorCommLevel(optLogger, logContext, " controllerError:" + statusPostResult.controllerError);
        }

        return statusPostResult;
    }
}
