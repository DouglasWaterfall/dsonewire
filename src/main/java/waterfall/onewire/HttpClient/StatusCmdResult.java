package waterfall.onewire.HttpClient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import waterfall.onewire.busmaster.BusMaster;

/**
 * Created by dwaterfa on 8/7/16.
 */
public class StatusCmdResult extends BaseCmdResult {

    public StatusCmdResult() {
        super();
    }

    public StatusCmdResult(PostErrors e) {
        super(e);
        this.ident = null;
        this.authorization = null;
    }

    public StatusCmdResult(ControllerErrors e) {
        super(e);
        this.ident = null;
        this.authorization = null;
    }

    public StatusCmdResult(String bmIdent, BusMaster bm, String authorization) {
        super();

        assert (bmIdent != null);
        assert (!bmIdent.isEmpty());
        assert (bm != null);
        assert (authorization != null);
        assert (!authorization.isEmpty());

        this.ident = bmIdent;
        this.name = bm.getName();
        this.started = bm.getIsStarted();
        this.authorization = authorization;
    }

    @JsonProperty("ident")
    public String ident;

    @JsonProperty("name")
    public String name;

    @JsonProperty("started")
    public Boolean started;

    @JsonProperty("authorization")
    public String authorization;

    @JsonIgnore
    public String toString() {
        StringBuffer sb = new StringBuffer();

        if (ident != null) {
            sb.append("ident:" + ident + " ");
        }

        if (name != null) {
            sb.append("name:" + name + " ");
        }

        if (started != null) {
            sb.append("started:" + started);
        }

        if (authorization != null) {
            sb.append("authorization:" + authorization);
        }

        return sb.toString();
    }

}
