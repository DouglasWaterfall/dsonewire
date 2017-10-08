package waterfall.onewire.HttpClient;

/**
 * Created by dwaterfa on 8/19/17.
 */
public abstract class WaitForEventCmdResult {

    public boolean isThreadAlreadyWaiting() {
        return false;
    }

    public boolean isServerTimestampChanged() {
        return false;
    }

    public boolean isBMListChanged() {
        return false;
    }

    public boolean isInvalidBMSearchData() {
        return false;
    }

    public boolean isBMSearchChanged() {
        return false;
    }

    public boolean isWaitTimeout() {
        return false;
    }

}
