package waterfall.onewire.HttpClient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import waterfall.onewire.busmaster.Logger;

import java.util.List;

/**
 * Created by dwaterfa on 8/23/16.
 */
public class ListBusMastersCmdResult extends BaseCmdResult {
    public ListBusMastersCmdResult() {
        super();
    };

    public ListBusMastersCmdResult(PostErrors e) {
        super(e);
        this.resultList = null;
    }

    public ListBusMastersCmdResult(ControllerErrors e) {
        super(e);
        this.resultList = null;
    }

    public ListBusMastersCmdResult(List<String> bmList, Logger optLogger) {
        super();
        this.resultList = bmList;
        super.setLogger(optLogger);
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
