package waterfall.onewire.HttpClient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by dwaterfa on 9/7/16.
 */
public class TimeDiffResult extends BaseCmdResult {

    @JsonProperty("sentTimeMSec")
    public Long clientSentTimeMSec;

    @JsonProperty("serverReceivedTimeMSec")
    public Long serverReceivedTimeMSec;

    @JsonIgnore
    public Long clientReceivedTimeMSec;

    public TimeDiffResult () {
        super();
    };

    public TimeDiffResult(PostErrors e) {
        super(e);
        this.clientSentTimeMSec = null;
        this.serverReceivedTimeMSec = null;
        this.clientReceivedTimeMSec = null;
    }

    public TimeDiffResult(ControllerErrors e) {
        super(e);
        this.clientSentTimeMSec = null;
        this.serverReceivedTimeMSec = null;
        this.clientReceivedTimeMSec = null;
    }

    public TimeDiffResult(Long clientSentTimeMSec, Long serverReceivedTimeMSec) {
        super();
        assert (clientSentTimeMSec != null);
        assert (serverReceivedTimeMSec != null);
        this.clientSentTimeMSec = clientSentTimeMSec;
        this.serverReceivedTimeMSec = serverReceivedTimeMSec;
        this.clientReceivedTimeMSec = null;
    }

    public void setClientReceivedTimeMSec(Long clientReceivedTimeMSec) {
        assert (postError == null);
        assert (controllerError == null);
        assert (clientSentTimeMSec != null);
        assert (serverReceivedTimeMSec != null);
        assert (clientReceivedTimeMSec != null);
        assert (this.clientReceivedTimeMSec == null);
        this.clientReceivedTimeMSec = clientReceivedTimeMSec;
    }
}

