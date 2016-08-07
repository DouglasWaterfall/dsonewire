package waterfall.onewire.HttpClient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import waterfall.onewire.busmaster.ReadScratchpadCmd;

/**
 * Created by dwaterfa on 8/6/16.
 */
public class ReadScratchpadCmdResult extends BaseCmdResult {
    public ReadScratchpadCmdResult() {
        super();
    };

    public ReadScratchpadCmdResult(ControllerErrors e) {
        super(e);
        this.result = null;
        this.resultHexData = null;
    }

    public ReadScratchpadCmdResult(ReadScratchpadCmd cmd, Float tempF, Float tempC) {
        super(null);
        this.result = cmd.getResult().name();
        if (cmd.getResult() == ReadScratchpadCmd.Result.success) {
            this.resultHexData = cmd.getResultHexData();
            this.tempF = tempF;
            this.tempC = tempC;
        }
        super.setLogger(cmd.getLogger());
    }

    @JsonProperty("result")
    public String result;

    @JsonIgnore
    public ReadScratchpadCmd.Result getResult() {
        if (result == null) {
            return null;
        }
        try {
            return Enum.valueOf(ReadScratchpadCmd.Result.class, result);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonIgnore
    public byte[] resultData;

    @JsonProperty("resultHexData")
    public byte[] resultHexData;

    @JsonProperty("tempF")
    public Float tempF;

    @JsonProperty("tempC")
    public Float tempC;

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

        if (resultHexData != null) {
            sb.append("resultHexData:" + resultHexData.toString() + "\n");
        }

        if (tempF != null) {
            sb.append("tempF:" + tempF.toString() + "\n");
        }

        if (tempC != null) {
            sb.append("tempF:" + tempC.toString() + "\n");
        }

        if (sb.length() > 0) {
            return sb.toString();
        }
        return null;
    }

}
