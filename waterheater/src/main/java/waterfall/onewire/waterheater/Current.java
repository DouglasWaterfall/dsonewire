package waterfall.onewire.waterheater;

public class Current {

    public final long stateStartMSec;

    public final State state;
    public final String stateStart;
    public final Float tempF;
    public final String error;

    public Current(long stateStartMSec, State s, float tempF) {
        this.stateStartMSec = stateStartMSec;
        this.state = s;
        this.stateStart = WaterHeater.toDateString(stateStartMSec);
        this.tempF = tempF;
        this.error = null;
    }

    public Current(long atStartMSec, State s, String error) {
        this.stateStartMSec = atStartMSec;
        this.state = s;
        this.stateStart = WaterHeater.toDateString(stateStartMSec);
        this.tempF = null;
        this.error = error;
    }
}
