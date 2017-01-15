package waterfall.onewire.HttpClient;

/**
 * Created by dwaterfa on 1/14/17.
 */
public class WaitForEventSearchNotify implements WaitForEventResult {

    private BMSearchData[] bmSearchData;

    private BMSearchData[] bmSearchByAlarmData;

    public WaitForEventSearchNotify(BMSearchData[] bmSearchData, BMSearchData[] bmSearchByAlarmData) {
        this.bmSearchData = bmSearchData;
        this.bmSearchByAlarmData = bmSearchByAlarmData;
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
        return false;
    }

    @Override
    public BMListChangedData getBMListChangedData() {
        return null;
    }

    @Override
    public boolean hasBMSearchData() {
        return (bmSearchData != null);
    }

    @Override
    public BMSearchData[] getBMSearchData() {
        return bmSearchData;
    }

    public void setBmSearchData(BMSearchData[] bmSearchData) {
        this.bmSearchData = bmSearchData;
    }

    @Override
    public boolean hasBMAlarmSearchData() {
        return (bmSearchByAlarmData != null);
    }

    @Override
    public BMSearchData[] getBMAlarmSearchData() {
        return bmSearchByAlarmData;
    }

    public void setBmSearchByAlarmData(BMSearchData[] bmSearchByAlarmData) {
        this.bmSearchByAlarmData = bmSearchByAlarmData;
    }

}
