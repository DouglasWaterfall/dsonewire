package waterfall.onewire.HttpClient;

/**
 * Created by dwaterfa on 1/14/17.
 */
public class WaitForEventPostError implements WaitForEventResult {
    private PostErrors postError;

    public WaitForEventPostError(PostErrors postError) {
        this.postError = postError;
    }

    @Override
    public boolean hasPostError() {
        return (postError != null);
    }

    @Override
    public PostErrors getPostError() {
        return postError;
    }

    public void setPostError(PostErrors postError) {
        this.postError = postError;
    }

    @Override
    public boolean hasControllerError() {
        return false;
    }

    @Override
    public ControllerErrors getControllerError() {
        return null;
    }

    @Override
    public boolean hasBMListChanged() {
        return false;
    }

    @Override
    public BMListChangedData getBMListChangedData() {
        return null;
    }

    @Override
    public boolean hasBMSearchData() {
        return false;
    }

    @Override
    public BMSearchData[] getBMSearchData() {
        return null;
    }

    @Override
    public boolean hasBMAlarmSearchData() {
        return false;
    }

    @Override
    public BMSearchData[] getBMAlarmSearchData() {
        return null;
    }

}
