package waterfall.onewire.HttpClient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import waterfall.onewire.busmaster.BusMaster;

/**
 * Created by dwaterfa on 1/16/17.
 */
public class ScheduleSearchBusCmdResult extends BaseCmdResult {
    @JsonProperty("result")
    public String result;

    public ScheduleSearchBusCmdResult() {
        super();
    };

    public ScheduleSearchBusCmdResult(PostErrors e) {
        super(e);
        this.result = null;
    }

    public ScheduleSearchBusCmdResult(ControllerErrors e) {
        super(e);
        this.result = null;
    }

    public ScheduleSearchBusCmdResult(BusMaster.ScheduleNotifySearchBusCmdResult result) {
        super();
        this.result = result.toString();
    }

    @JsonIgnore
    public BusMaster.ScheduleNotifySearchBusCmdResult getResult() {
        if (result == null) {
            return null;
        }
        try {
            return Enum.valueOf(BusMaster.ScheduleNotifySearchBusCmdResult.class, result);
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
