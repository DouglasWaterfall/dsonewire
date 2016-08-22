package waterfall.onewire.HttpClient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.ReadPowerSupplyCmd;

/**
 * Created by dwaterfa on 8/6/16.
 */
public class ReadPowerSupplyCmdResult extends BaseCmdResult {

    public ReadPowerSupplyCmdResult() {
        super();
    };

    public ReadPowerSupplyCmdResult(PostErrors e) {
        super(e);
        this.result = null;
        this.isParasitic = null;
    }

    public ReadPowerSupplyCmdResult(ControllerErrors e) {
        super(e);
        this.result = null;
        this.isParasitic = null;
    }

    public ReadPowerSupplyCmdResult(ReadPowerSupplyCmd cmd) {
        super();
        this.result = cmd.getResult().name();
        if (cmd.getResult() == ReadPowerSupplyCmd.Result.success) {
            this.isParasitic = cmd.getResultIsParasitic();
        }
        super.setLogger(cmd.getLogger());
    }

    @JsonProperty("result")
    public String result;

    @JsonIgnore
    public ReadPowerSupplyCmd.Result getResult() {
        if (result == null) {
            return null;
        }
        try {
            return Enum.valueOf(ReadPowerSupplyCmd.Result.class, result);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonProperty("isParasitic")
    public Boolean isParasitic;

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

        if (isParasitic != null) {
            sb.append("isParasitic:" + isParasitic.toString() + "\n");
        }

        if (sb.length() > 0) {
            return sb.toString();
        }
        return null;
    }

}
