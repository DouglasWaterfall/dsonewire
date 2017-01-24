package waterfall.onewire.HttpClient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import waterfall.onewire.busmaster.Logger;

import java.util.Iterator;

/**
 * Created by dwaterfa on 8/6/16.
 */
public class BaseCmdResult extends BaseCmdPostResult {

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
        Invalid_rCount,
        Invalid_minPeriodMSec
    }

    protected BaseCmdResult() {
        super();
        controllerError = null;
    }

    protected BaseCmdResult(PostErrors pe) {
        super(pe);
        controllerError = null;
    }

    protected BaseCmdResult(ControllerErrors ce) {
        super();
        controllerError = (ce != null) ? ce.name() : null;
    }

    @JsonProperty("controllerError")
    public String controllerError;

}
