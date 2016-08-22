package waterfall.onewire.HttpClient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.ConvertTCmd;

/**
 * Created by dwaterfa on 8/6/16.
 */
public class ConvertTCmdResult extends BaseCmdResult {
    public ConvertTCmdResult() {
        super();
    };

    public ConvertTCmdResult(PostErrors e) {
        super(e);
        this.result = null;
    }

    public ConvertTCmdResult(ControllerErrors e) {
        super(e);
        this.result = null;
    }

    public ConvertTCmdResult(ConvertTCmd cmd) {
        super();
        this.result = cmd.getResult().name();
        super.setLogger(cmd.getLogger());
    }

    @JsonProperty("result")
    public String result;

    @JsonIgnore
    public ConvertTCmd.Result getResult() {
        if (result == null) {
            return null;
        }
        try {
            return Enum.valueOf(ConvertTCmd.Result.class, result);
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
