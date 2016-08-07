package waterfall.onewire.HttpClient;

import com.fasterxml.jackson.annotation.JsonProperty;
import waterfall.onewire.busmaster.Logger;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by dwaterfa on 8/6/16.
 */
public class BaseCmdResult {

    public enum ControllerErrors {
        ConnectException,
        MethodNotAllowed,
        ReadTimeout,
        ParseException,
        ServerError, // 500
        UnknownError,

        Unknown_bmIdent,
        BM_not_started,
        Bad_parm_log_not_true_or_false,
        Bad_parm_byAlarm_not_true_false,
        Bad_parm_only_one_byAlarm_or_byFamilyCode_allowed,
        Bad_parm_byFamilyCode_not_a_number,
        Bad_parm_byFamilyCode_must_be_unsigned_byte,
        No_BM_for_dsAddr,
        Invalid_dsAddr,
        Invalid_rCount
    }

    protected BaseCmdResult() {
    }

    protected BaseCmdResult(ControllerErrors e) {
        error = (e != null) ? e.name() : null;
    }

    @JsonProperty("error")
    public String error;

    @JsonProperty("log")
    public String[] log;

    protected void setLogger(Logger optLogger) {
        if ((optLogger != null) && (optLogger.getLogSize() > 0)) {
            log = new String[optLogger.getLogSize()];
            int i = 0;
            for (Iterator<String> iter = optLogger.getLogIter(); iter.hasNext(); ) {
                log[i++] = iter.next();
            }
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        if (error != null) {
            sb.append("error:" + error);
            sb.append('\n');
        }

        if (log != null) {
            sb.append("log[" + log.length + "]:\n");
            for (String s : log) {
                sb.append(s + "\n");
            }
        }

        return sb.toString();
    }
}
