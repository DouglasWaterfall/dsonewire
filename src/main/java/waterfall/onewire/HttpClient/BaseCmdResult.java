package waterfall.onewire.HttpClient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import waterfall.onewire.busmaster.Logger;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by dwaterfa on 8/6/16.
 */
public class BaseCmdResult {

    public enum PostErrors {
        ConnectException,
        MethodNotAllowed,
        ReadTimeout,
        ParseException,
        ServerError, // 500
        UnknownError
    }

    public enum ControllerErrors {
        Unknown_bmIdent,
        BM_not_started,
        Bad_parm_logLevel,
        Bad_parm_byAlarm_not_true_false,
        Bad_parm_only_one_byAlarm_or_byFamilyCode_allowed,
        Bad_parm_byFamilyCode_not_a_number,
        Bad_parm_byFamilyCode_must_be_unsigned_byte,
        Bad_parm_clientSentTimeMSec,
        Invalid_dsAddr,
        Invalid_rCount
    }

    protected BaseCmdResult() {
        postError = null;
        controllerError = null;
    }

    protected BaseCmdResult(PostErrors pe) {
        postError = pe;
        controllerError = null;
    }

    protected BaseCmdResult(ControllerErrors ce) {
        postError = null;
        controllerError = (ce != null) ? ce.name() : null;
    }

    @JsonIgnore
    public PostErrors postError;

    @JsonProperty("controllerError")
    public String controllerError;

    @JsonProperty("log")
    public String[] log;

    @JsonIgnore
    protected void setLogger(Logger optLogger) {
        if ((optLogger != null) && (optLogger.getLogSize() > 0)) {
            log = new String[optLogger.getLogSize()];
            int i = 0;
            for (Iterator<String> iter = optLogger.getLogIter(); iter.hasNext(); ) {
                log[i++] = iter.next();
            }
        }
    }

}
