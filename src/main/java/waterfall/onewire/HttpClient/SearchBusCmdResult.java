package waterfall.onewire.HttpClient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.SearchBusCmd;

import java.util.List;

/**
 * Created by dwaterfa on 8/6/16.
 */
public class SearchBusCmdResult extends BaseCmdResult {

    public SearchBusCmdResult() {
        super();
    };

    public SearchBusCmdResult(PostErrors e) {
        super(e);
        this.result = null;
        this.resultList = null;
    }

    public SearchBusCmdResult(ControllerErrors e) {
        super(e);
        this.result = null;
        this.resultList = null;
    }

    public SearchBusCmdResult(SearchBusCmd cmd) {
        super();
        this.result = cmd.getResult().name();
        if (cmd.getResult() == SearchBusCmd.Result.success) {
            this.resultList = cmd.getResultList();
        }
        super.setLogger(cmd.getLogger());
    }

    @JsonProperty("result")
    public String result;

    @JsonIgnore
    public SearchBusCmd.Result getResult() {
        if (result == null) {
            return null;
        }
        try {
            return Enum.valueOf(SearchBusCmd.Result.class, result);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonProperty("resultList")
    public List<String> resultList;

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

        if (resultList != null) {
            sb.append("resultList[" + resultList.size() + "]:\n");
            for (String ss: resultList) {
                sb.append(ss + "\n");
            }
        }

        if (sb.length() > 0) {
            return sb.toString();
        }
        return null;
    }
}
