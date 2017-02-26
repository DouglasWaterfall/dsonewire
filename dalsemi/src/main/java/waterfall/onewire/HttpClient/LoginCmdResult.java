package waterfall.onewire.HttpClient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import waterfall.onewire.busmaster.Logger;

import java.util.Iterator;

/**
 * Created by dwaterfa on 1/21/17.
 */
public class LoginCmdResult extends BaseCmdPostResult {

    protected LoginCmdResult() {
        super();
        authorization = null;
    }

    protected LoginCmdResult(PostErrors pe) {
        super(pe);
        authorization = null;
    }

    protected LoginCmdResult(String authorization) {
        super();
        this.authorization = authorization;
    }

    @JsonProperty("authorization")
    public String authorization;

}
