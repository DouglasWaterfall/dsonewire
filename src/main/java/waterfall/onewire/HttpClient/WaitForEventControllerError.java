package waterfall.onewire.HttpClient;

/**
 * Created by dwaterfa on 1/14/17.
 */
public class WaitForEventControllerError implements WaitForEventResult {
    private ControllerErrors controllerError;

    public WaitForEventControllerError(ControllerErrors controllerError) {
        this.controllerError = controllerError;
    }

    @Override
    public boolean hasPostError() {
        return false;
    }

    @Override
    public PostErrors getPostError() { return null; }

    @Override
    public boolean hasControllerError() {
        return true;
    }

    @Override
    public ControllerErrors getControllerError() {
        return controllerError;
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
