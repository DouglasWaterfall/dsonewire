package waterfall.onewire;

import java.util.concurrent.TimeUnit;
import waterfall.onewire.busmaster.BusMaster;
import waterfall.onewire.busmaster.ConvertTCmd;
import waterfall.onewire.busmaster.ReadScratchpadCmd;
import waterfall.onewire.device.DS18B20Scratchpad;

/**
 * Created by dwaterfa on 10/19/17.
 */
public class Temp18B20 {

  public static String ERR_NO_BUSMASTER = "No BusMaster";
  public static String ERR_READSCRATCHPAD_RESULT = "ReadScratchpadCmd.Result."; // starts with
  public static String ERR_DEVICE_NOT_FOUND = "Device not found";
  public static String ERR_SCRATCHPAD_DATA_NOT_VALID = "Scratchpad data not valid";
  public static String ERR_SCRATCHPAD_DATA_CRC = "Scratchpad data crc error";
  public static String ERR_CONVERTT_RESULT = "ConvertTCmd.Result.";
  public static String ERR_REINITIALIZE_CYCLE = "Reinitialize cycle";
  public static String ERR_UNEXPECTED_INITSTATE = "Unexpected initState:"; // starts with

  private final DSAddress dsAddress;
  private final byte resolution;
  private final byte tempHAlarm;
  private final byte tempLAlarm;
  private BusMaster bm;
  private WaitForDeviceByAddress waitForDeviceByAddress;
  private ConvertTCmd convertTCmd;
  private ReadScratchpadCmd readScratchpadCmd;
  private InitializationState initState;
  private Reading lastReading;
  private Thread pushingThread;

  /**
   * Construct an instance around a specific dsAddress.
   *
   * @param resolution Must be within 0-3
   */
  public Temp18B20(DSAddress dsAddress, byte resolution, byte tempHAlarm, byte tempLAlarm) {
    if (dsAddress == null) {
      throw new IllegalArgumentException("dsAddress");
    }
    if ((resolution < 0) || (resolution > 3)) {
      throw new IllegalArgumentException("resolution");
    }
    this.dsAddress = dsAddress;
    this.resolution = resolution;
    this.tempHAlarm = tempHAlarm;
    this.tempLAlarm = tempLAlarm;

    this.waitForDeviceByAddress = null;
    this.bm = null;

    this.convertTCmd = null;
    this.readScratchpadCmd = null;

    initState = InitializationState.WaitingForBusMaster;

    this.lastReading = null;
    this.pushingThread = null;
  }

  /**
   * @return The DSAddress assigned to this instance
   */
  public DSAddress getDSAddress() {
    return dsAddress;
  }

  /**
   * The temperature precision is based on 7 integer bits and 1-4 fractional bits. This provides a
   * theoretical range of +127 15/16 to -127 15/16 Celcius but the datasheet claims only a range of
   * +125C to -55C. The amount of fractional bits has a direct influence on the time it takes to
   * calculate the temperature.
   *
   * @return The number of temperature bits
   */
  public byte getResolution() {
    return resolution;
  }

  /**
   * The temperature, in Celcius, at or above where the device will respond to a conditional
   * search.
   *
   * @return high alarm temperature
   */
  public byte getTempHAlarm() {
    return tempHAlarm;
  }

  /**
   * The temperature, in Celcius, at or below where the device will respond to a conditional
   * search.
   *
   * @return low alarm temperature
   */
  public byte getTempLAlarm() {
    return tempLAlarm;
  }

  /**
   * The BusMaster where this device has being controller from, or null if not defined.
   *
   * @return BusMaster, or null if not assigned.
   */
  public BusMaster getBusMaster() {
    return bm;
  }

  /**
   * This is one of the two ways for the instance to find which BusMaster is exists on - here we
   * explicitly set it.
   *
   * @param bm The BusMaster where the device is managed
   * @return this
   */
  public synchronized Temp18B20 setBusMaster(BusMaster bm) {
    if (this.bm != null) {
      throw new IllegalArgumentException("dup bm");
    }
    this.bm = bm;
    return this;
  }

  /**
   * This is the second way for the instance to find out - by being passed in the BusMasterRegistry
   * then it is expected to find it itself. It will immediately launch a search. Once found it will
   * stop searching and then call setBusMaster() when it is found.
   *
   * @param bMR The BusMasterRegistry where the busmasters may be found
   * @return this
   */
  public synchronized Temp18B20 setBusMasterRegistry(BusMasterRegistry bMR) {
    if (this.waitForDeviceByAddress != null) {
      throw new IllegalArgumentException("dup bMR");
    }
    this.waitForDeviceByAddress = new WaitForDeviceByAddress(bMR, false,
        (TimeUnit.SECONDS.toMillis(15)));
    this.waitForDeviceByAddress.addAddress(new myWFDBAC(this), new String[]{dsAddress.toString()});
    return this;
  }

  /**
   * Read the temperature of the device. The device will only actually be sampled if the time
   * request, if specified, is
   *
   * @param withinTimeMSec If non-null, then the time the last temperature was taken must be equal
   * or AFTER the specified time for it to be returned, otherwise a new temperature will be taken.
   * For example, if you wanted to get the temperature within 5 minutes of the current time, you
   * would pass in the value for 5 minutes LESS than the current time which would mean that the
   * last temperature read must have been taken between 5 minutes ago and now for it to be returned,
   * otherwise a new temperature will be taken.
   * @return ReadingError instance if the BusMaster has not been located, or if the withinMSec time
   * exceeds the currentTimeMSec().
   */
  public synchronized Reading getTemperature(Long withinTimeMSec) {
    if (withinTimeMSec != null) {
      if (withinTimeMSec > System.currentTimeMillis()) {
        return new ReadingError("withinTimeMSec exceeds current time");
      }

      // Maybe we already have the time we want.
      if ((lastReading != null) && (lastReading instanceof ReadingData) &&
          (lastReading.getTimeMSec() >= withinTimeMSec)) {
        return lastReading;
      }
    }

    // We need to generate an upto date reading.

    if (pushingThread == null) {
      // we are going to be the pushing thread
      try {
        pushingThread = Thread.currentThread();

        // this call will handle initialization if required.
        lastReading = pushRead();

      } finally {
        pushingThread = null;
      }
    } else {
      // we need to wait until the thread finishes. Since someone IS pushing that whatever happens
      // with them we will take the result since they will update the lastReading value one way or
      // another.
      Reading saveLastReading = lastReading;

      do {
        try {
          wait(TimeUnit.SECONDS.toMillis(2));
        } catch (InterruptedException e) {
          return new ReadingError("wait timeout");
        }
      }
      while (lastReading == saveLastReading);
    }

    return lastReading;
  }

  /**
   * @return true if the we have some result, even an error. Otherwise false which means the device
   * state changed.
   */
  private Reading pushRead() {

    boolean wasReInitalize = false;

    if (initState == InitializationState.WaitingForBusMaster) {
      if (bm == null) {
        return new ReadingError(ERR_NO_BUSMASTER);
      }

      convertTCmd = bm.queryConvertTCmd(dsAddress);
      readScratchpadCmd = bm.queryReadScratchpadCmd(dsAddress, (short) 9);
      initState = InitializationState.Initialize;
    }

    if (initState == InitializationState.ReInitialize) {
      // We don't want to do this more than once to avoid recursion.
      wasReInitalize = true;
      initState = InitializationState.Initialize;
    }

    if (initState == InitializationState.Initialize) {
      // find out what the current configuration setting is
      ReadScratchpadCmd.Result rResult = readScratchpadCmd.execute();
      if (rResult != ReadScratchpadCmd.Result.success) {
        initState = InitializationState.Initialize;
        return new ReadingError(ERR_READSCRATCHPAD_RESULT + rResult.name());
      }

      // If the device is present it will pull at least some of the bits low to return data.
      // So we if get back all FFs then we know the device is not there.
      DS18B20Scratchpad scratchpadData = new DS18B20Scratchpad(readScratchpadCmd.getResultData());
      if (scratchpadData.checkAllFFs()) {
        initState = InitializationState.Initialize;
        return new ReadingError(ERR_DEVICE_NOT_FOUND);
      }

      // Check to see if this is a DS18B20.
      if (!scratchpadData.checkValid()) {
        initState = InitializationState.Initialize;
        return new ReadingError(ERR_SCRATCHPAD_DATA_NOT_VALID);
      }

      // Looks okay. Now check configuration to see if it is the state we wanted.
      if ((scratchpadData.getResolution() != resolution) ||
          (scratchpadData.getTempHAlarm() != tempHAlarm) ||
          (scratchpadData.getTempLAlarm() != tempLAlarm)) {
        ReadingError readingError = Initialize();
        if (readingError != null) {
          return readingError;
        }
      }

      initState = InitializationState.Ready;
    }

    if (initState == InitializationState.ReInitialize) {
      ReadingError readingError = Initialize();
      if (readingError != null) {
        initState = InitializationState.Initialize;
        return readingError;
      }

      initState = InitializationState.Ready;
    }

    if (initState != InitializationState.Ready) {
      return new ReadingError(ERR_UNEXPECTED_INITSTATE + initState.name());
    }

    ConvertTCmd.Result cResult = convertTCmd.execute();
    if (cResult != ConvertTCmd.Result.success) {
      return new ReadingError(ERR_CONVERTT_RESULT + cResult.name());
    }

    long cWriteCTM = convertTCmd.getResultWriteCTM();
    long delayMSec = calculateWaitDelayMSec();
    long waitUntilMSec = System.currentTimeMillis() + delayMSec;

    do {
      try {
        wait(delayMSec);
      } catch (InterruptedException e) {
        ;
      }
    }
    while (System.currentTimeMillis() < waitUntilMSec);

    DS18B20Scratchpad data = null;

    for (int retryCount = 0; retryCount < 5; retryCount++) {
      // Read out the temp.
      ReadScratchpadCmd.Result rResult = readScratchpadCmd.execute();
      if (rResult != ReadScratchpadCmd.Result.success) {
        return new ReadingError(ERR_READSCRATCHPAD_RESULT + rResult.name());
      }

      data = new DS18B20Scratchpad(readScratchpadCmd.getResultData());

      if (data.checkAllFFs()) {
        // The device did not respond
        initState = InitializationState.Initialize;
        return new ReadingError(ERR_DEVICE_NOT_FOUND);
      }

      // check that the data was transferred correctly (otherwise re-read it)
      if (data.checkValid()) {
        break;
      }

      if (retryCount == 4) {
        return new ReadingError(ERR_SCRATCHPAD_DATA_CRC);
      }
    }

    // check that the device has the right setting - if not then there has been a reset and we
    // should clear the state to be needs-init and redo it. "check init" is different from
    // "do the init"
    if ((data.getResolution() != resolution) ||
        (data.getTempHAlarm() != tempHAlarm) ||
        (data.getTempLAlarm() != tempLAlarm)) {

      // It is tempting to theoretically be able to use the temperature that we just asked for
      // but the problem is that we might not have waited long enough for it to be generated and
      // so it is suspect. Better to just start over.

      // we wish to avoid recursion
      if (wasReInitalize) {
        // this is odd - we did the init, and then convert and then read and we are unhappy? We
        // do not want to cycle forever so we fail this and force it to try again from scratch.
        initState = InitializationState.Initialize;
        return new ReadingError(ERR_REINITIALIZE_CYCLE);
      }

      initState = InitializationState.ReInitialize;
      return pushRead();
    }

    // calculate the value
    float tempC = data.getTempC();
    return new ReadingData(tempC, cWriteCTM);
  }

  private ReadingError Initialize() {

    //
    // We need to write to the device scratchpad to set the configuration.
    //
    DS18B20Scratchpad scratchpadData = new DS18B20Scratchpad();
    scratchpadData.setTempHAlarm((byte) tempHAlarm);
    scratchpadData.setTempLAlarm((byte) tempLAlarm);
    scratchpadData.setResolution((byte) resolution);

    // write scratchpad cmd

    // Not yet.

    // Success - not returning an ReadingError
    return null;
  }

  /**
   * The time in MS we need to wait is dependent on the precision we are asking for.
   */
  private long calculateWaitDelayMSec() {
    switch (resolution) {
      case 0:
        return 94L;
      case 1:
        return 188L;
      case 2:
        return 375L;
      case 3:
      default:
        return 750L;
    }
  }

  private enum InitializationState {
    /**
     * No busmaster for the device has been determined.
     */
    WaitingForBusMaster,

    /**
     * The device needs to be found and/or initialized.
     */
    Initialize,

    /**
     * The device has been initialized.
     */
    Ready,

    /**
     * The device was ready and we found that it was not what we wanted. We attempt to re-init once
     * and then go back to Ready.
     */
    ReInitialize
  }

  /**
   * Our internal instance which deals with finding the device.
   */
  private static class myWFDBAC implements WaitForDeviceByAddressCallback {

    private final Temp18B20 _this;

    public myWFDBAC(Temp18B20 _this) {
      this._this = _this;
    }

    @Override
    public boolean deviceFound(BusMaster bm, String dsAddress, boolean typeByAlarm) {
      _this.setBusMaster(bm);
      return true; // cancel the search
    }
  }

  public static abstract class Reading {

    protected final long timeMSec;

    protected Reading(long timeMSec) {
      this.timeMSec = timeMSec;
    }

    /**
     * This is the timestamp in systemtime when the temperature was sampled OR it is the time the
     * error was generated.
     *
     * @return Time in msec systemtime
     */
    public long getTimeMSec() {
      return timeMSec;
    }

    public String getError() {
      throw new IllegalArgumentException("no error");
    }

    public float getTempC() {
      throw new IllegalArgumentException("no temp");
    }

  }

  public static class ReadingError extends Reading {

    private final String error;

    public ReadingError(String error) {
      super(System.currentTimeMillis());
      this.error = error;
    }

    public String getError() {
      return error;
    }
  }

  public static class ReadingData extends Reading {

    private final float tempC;

    public ReadingData(float tempC, long timeMSec) {
      super(timeMSec);
      this.tempC = tempC;
    }

    public float getTempC() {
      return tempC;
    }
  }

}

