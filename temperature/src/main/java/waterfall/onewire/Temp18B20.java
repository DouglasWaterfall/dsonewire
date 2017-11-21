package waterfall.onewire;

import com.dalsemi.onewire.utils.CRC8;
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

  private final DSAddress dsAddress;
  private final PrecisionBits precisionBits;
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
   * Construct and instance around a specific dsAddress.
   */
  public Temp18B20(DSAddress dsAddress, PrecisionBits precisionBits, byte tempHAlarm,
      byte tempLAlarm) {
    if (dsAddress == null) {
      throw new IllegalArgumentException("dsAddress");
    }
    this.dsAddress = dsAddress;
    if (precisionBits == null) {
      throw new IllegalArgumentException("precisionBits");
    }
    this.precisionBits = precisionBits;
    this.tempHAlarm = tempHAlarm;
    this.tempLAlarm = tempLAlarm;

    this.waitForDeviceByAddress = null;
    this.bm = null;

    this.convertTCmd = null;
    this.readScratchpadCmd = null;

    initState = InitializationState.noBusMaster;

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
   * +125C to -55C. The amount of frational bits has a direct influence on the time it takes to
   * calculate the temperature.
   *
   * @return The number of temperature bits
   */
  public PrecisionBits getPrecisionBits() {
    return precisionBits;
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
   */
  public synchronized void setBusMaster(BusMaster bm) {
    if (this.bm != null) {
      throw new IllegalArgumentException("dup bm");
    }
    this.bm = bm;
  }

  /**
   * This is the second way for the instance to find out - by being passed in the BusMasterRegistry
   * then it is expected to find it itself. It will immediately launch a search. Once found it will
   * stop searching and then call setBusMaster() when it is found.
   *
   * @param bMR The BusMasterRegistry where the busmasters may be found
   */
  public synchronized void setBusMasterRegistry(BusMasterRegistry bMR) {
    if (this.waitForDeviceByAddress != null) {
      throw new IllegalArgumentException("dup bMR");
    }
    this.waitForDeviceByAddress = new WaitForDeviceByAddress(bMR, false,
        (TimeUnit.SECONDS.toMillis(15)));
    this.waitForDeviceByAddress.addAddress(new myWFDBAC(this), new String[]{dsAddress.toString()});
  }

  /**
   * Read the temperature of the device.
   *
   * @param withinMSec If non-null, if the instance has already read the temperature and time it was
   * taken is equal to or greater than this argument then the previously read value will be
   * returned. To force an update of the temperature just pass in null for this value.
   * @return ReadingError instance if the BusMaster has not been located, or if the withinMSec time
   * exceeds the currentTimeMSec().
   */
  public synchronized Reading getTemperature(Long withinMSec) {
    if (withinMSec != null) {
      if (withinMSec > System.currentTimeMillis()) {
        return new ReadingError("withinMSec exceeds current time");
      }

      // Maybe we already have the time we want.
      if ((lastReading != null) && (!(lastReading instanceof ReadingError)) && (
          lastReading.getTimeMSec() <= withinMSec)) {
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
    while (true) {
      switch (initState) {
        case noBusMaster: {
          if (bm != null) {
            convertTCmd = bm.queryConvertTCmd(dsAddress);
            readScratchpadCmd = bm.queryReadScratchpadCmd(dsAddress, (short) 9);
            initState = InitializationState.findDevice;
            break;
          }
          return new ReadingError(ERR_NO_BUSMASTER);
        }

        case findDevice: {
          // find out what the current configuration setting is
          ReadScratchpadCmd.Result rResult = readScratchpadCmd.execute();
          if (rResult != ReadScratchpadCmd.Result.success) {
            return new ReadingError(ERR_READSCRATCHPAD_RESULT + rResult.name());
          }

          // If the device is present it will pull at least some of the bits low to return data. So we if get back
          // all FFs then we know the device is not there.
          byte[] resultData = readScratchpadCmd.getResultData();
          boolean notAllFFs = false;
          for (byte b : resultData) {
            if (b != 0xff) {
              notAllFFs = true;
              break;
            }
          }
          if (!notAllFFs) {
            // Not found.
            return new ReadingError(ERR_DEVICE_NOT_FOUND);
          }

          // check configuration to see if it is what we wanted.
          DS18B20Scratchpad scratchpadData = new DS18B20Scratchpad(resultData);
          if (!scratchpadData.checkValid()) {
            // Not quite what we thought it was - perhaps wrong device of our code.
            return new ReadingError(ERR_SCRATCHPAD_DATA_NOT_VALID);
          }

          if ((scratchpadData.getResolution() != precisionBits.ordinal()) ||
              (scratchpadData.getTempHAlarm() != tempHAlarm) ||
              (scratchpadData.getTempLAlarm() != tempLAlarm)) {
            initState = InitializationState.needInitialization;
          } else {
            initState = InitializationState.initialized;
          }
          break;
        }

        case needInitialization: {
          //
          // We need to write to the device scratchpad to set the configuration.
          //
          DS18B20Scratchpad scratchpadData = new DS18B20Scratchpad();
          scratchpadData.setTempHAlarm((byte) tempHAlarm);
          scratchpadData.setTempLAlarm((byte) tempLAlarm);
          scratchpadData.setResolution((byte) precisionBits.ordinal());

          // write scratchpad cmd

          // Not yet.
          initState = InitializationState.initialized;
          // FALLTHROUGH
        }

        case initialized:
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

          byte[] resultData = null;

          for (int retryCount = 0; retryCount < 5; retryCount++) {
            // Read out the temp.
            ReadScratchpadCmd.Result rResult = readScratchpadCmd.execute();
            if (rResult != ReadScratchpadCmd.Result.success) {
              return new ReadingError(ERR_READSCRATCHPAD_RESULT + rResult.name());
            }

            resultData = readScratchpadCmd.getResultData();

            // check that the data was transferred correctly (otherwise re-read it)
            if (CRC8.compute(resultData) == 0) {
              break;
            }

            if (retryCount == 4) {
              return new ReadingError(ERR_SCRATCHPAD_DATA_CRC);
            }
          }

          DS18B20Scratchpad data = new DS18B20Scratchpad(resultData);

          // check that the device has the right setting - if not then there has been a reset and we should clear the
          // state to be needs-init and redo it. "check init" is different from "do the init"
          byte resolution = data.getResolution();
          if (resolution != precisionBits.ordinal()) {
            initState = InitializationState.needInitialization;
            break;
          }

          // calculate the value
          float tempC = data.getTempC();
          return new ReadingData(tempC, cWriteCTM);
      }
    }
  }

  /**
   * The time in MS we need to wait is dependent on the precision we are asking for.
   */
  private long calculateWaitDelayMSec() {
    switch (precisionBits) {
      case Nine:
        return 94L;
      case Ten:
        return 188L;
      case Eleven:
        return 375L;
      case Twelve:
      default:
        return 750L;
    }
  }

  /**
   * Temperature is in C and we always have 8 bits of integer portion
   */
  public enum PrecisionBits {
    /**
     * 1 bit fractional => 0.5C
     */
    Nine,

    /**
     * 2 bits fractional => 0.25C
     */
    Ten,

    /**
     * 3 bits fractional => 0.125C
     */
    Eleven,

    /**
     * 4 bits fractional => 0.0625C
     */
    Twelve
  }

  private enum InitializationState {
    /**
     * No busmaster for the device has been determined.
     */
    noBusMaster,

    /**
     * The device is in an unknown state and needs to be checked to see if it needs initialization
     */
    findDevice,

    /**
     * The device has been determined to need initialization.
     */
    needInitialization,

    /**
     * The device has been initialized.
     */
    initialized
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

  public class ReadingData extends Reading {

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


