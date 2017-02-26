package waterfall.onewire.HttpClient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import waterfall.onewire.busmaster.Logger;

import java.util.Iterator;

/**
 * Created by dwaterfa on 1/21/17.
 */
public class BaseCmdPostResult {

    protected BaseCmdPostResult() {
        postError = null;
    }

    protected BaseCmdPostResult(PostErrors pe) {
        postError = pe;
    }

    public boolean hasPostError() {
        return (postError != null);
    }

    public PostErrors getPostError() {
        return postError;
    }

    public void setPostError(PostErrors postError) {
        this.postError = postError;
    }

    @JsonIgnore
    protected PostErrors postError;

    @JsonProperty("log")
    protected String[] log;

    @JsonIgnore
    protected void setLogger(Logger optLogger) {
        if ((optLogger != null) && (optLogger.getLogSize() > 0)) {
            log = new String[optLogger.getLogSize()];
            int i = 0;
            for (Iterator<String> iter = optLogger.getLogIter(); iter.hasNext(); ) {
                log[i++] = iter.next();
            }
        }
    }
}

