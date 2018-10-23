package waterfall.onewire.waterheater;

/**
 * These are the states for our WaterHeater
 */
public enum State {
  /*
   * We are waiting for the device to make its appearance or recover from a ReadError.
   */
  deviceSync,

  /**
   * We are getting temperatures from the device but yet able to determine if the WaterHeater is in
   * any of the * other three states.
   */
  sync,

  /**
   * The burner is off and only the pilot light is keeping the burner housing warm at a fairly
   * constant rate.
   */
  flat,

  /**
   * The burner is on and heating up the burner housing.
   */
  burning,

  /**
   * We have finished burning and the temperature of the burner housing is cooling off but still
   * back below the triggerTemp.
   */
  cooling
}
