package waterfall.onewire.HttpClient;

/**
 * Created by dwaterfa on 1/14/17.
 */
public class WaitForEventBMChanged implements WaitForEventResult {
    private BMListChangedData bmListChangedData;

    public WaitForEventBMChanged(BMListChangedData data) {
        this.bmListChangedData = data;
    }

    @Override
    public boolean hasPostError() {
        return false;
    }

    @Override
    public PostErrors getPostError() {
        return null;
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
        return true;
    }

    @Override
    public BMListChangedData getBMListChangedData() {
        return bmListChangedData;
    }

    public void setBmListChangedData(BMListChangedData data) {
        this.bmListChangedData = data;
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
