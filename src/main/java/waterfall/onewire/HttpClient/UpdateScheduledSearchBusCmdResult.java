package waterfall.onewire.HttpClient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import waterfall.onewire.busmaster.BusMaster;

/**
 * Created by dwaterfa on 1/16/17.
 */
public class UpdateScheduledSearchBusCmdResult extends BaseCmdResult {
    @JsonProperty("result")
    public String result;

    public UpdateScheduledSearchBusCmdResult() {
        super();
    };

    public UpdateScheduledSearchBusCmdResult(PostErrors e) {
        super(e);
        this.result = null;
    }

    public UpdateScheduledSearchBusCmdResult(BaseCmdResult.ControllerErrors e) {
        super(e);
        this.result = null;
    }

    public UpdateScheduledSearchBusCmdResult(BusMaster.UpdateScheduledNotifySearchBusCmdResult result) {
        super();
        this.result = result.toString();
    }

    @JsonIgnore
    public BusMaster.UpdateScheduledNotifySearchBusCmdResult getResult() {
        if (result == null) {
            return null;
        }
        try {
            return Enum.valueOf(BusMaster.UpdateScheduledNotifySearchBusCmdResult.class, result);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonIgnore
    public String toString() {
        StringBuffer sb = new StringBuffer();
        String s = super.toString();
        if (s != null) {
            sb.append(s + "\n");
        }

        if (result != null) {
            sb.append("result:" + result + "\n");
        }

        if (sb.length() > 0) {
            return sb.toString();
        }
        return null;
    }

}
