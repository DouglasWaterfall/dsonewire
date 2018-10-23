package waterfall.onewire.waterheater;

public abstract class StateChangeNotifier {
    protected StateChangeNotifier() { }

    /**
     * The WaterHeater will call this when it determines that the logical state has changed. In general one
     * is not called with the same state twice except in the case of a ReadingError.
     * @param prevCurrent may be null in no previous Current.
     * @param newCurrent
     */
    public abstract void stateChanged(Current prevCurrent, Current newCurrent);

}

