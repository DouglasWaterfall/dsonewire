/**
 * Created by dwaterfa on 1/12/18.
 */
package waterfall.onewire.waterheater;

import com.dalsemi.onewire.utils.Convert;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import waterfall.onewire.BusMasterRegistry;
import waterfall.onewire.DSAddress;
import waterfall.onewire.Temp18B20;
import waterfall.onewire.Temp18B20.Reading;
import waterfall.onewire.Temp18B20.ReadingError;
import waterfall.onewire.device.DS18B20Scratchpad;


public class WaterHeater implements Runnable {

  private final float triggerTemp;
  private final int sampleTimeSec;
  private final DSAddress whAddress;
  private final StateChangeNotifier stateChangeNotifier;
  private final LoggerNotifier loggerNotifier;
  private final float[] window;
  private final Temp18B20 temp18B20;
  private final Thread pushThread;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * 
   */
  private int wIndex;
  
  /**
   * Our state.
   */
  private State state;
  
  /**
   * When the current burn time started, -1 if not currently burning.
   */
  private long burnStartTimeMSec;
  
  /**
   * This is filled in on start, needs the BusMasterRegistry
   */
  private Current current;

  /**
   * 
   * @param triggerTemp This is the temperature point where when crossing we think we are either leaving the flat state when burning or arriving back to it when cooling.
   * @param sampleTimeSec How often we sample.
   * @param windowSize The number of samples we take in order to compare the oldest with the most recent. A window of 5 gives us (5 + 1) sample range to compare to.
   * @param dsAddress The DSAddress of the sensor clipped to the burner housing.
   */
  public WaterHeater(BusMasterRegistry bmRegistry, float triggerTemp, int sampleTimeSec,
                     int windowSize, String dsAddress, StateChangeNotifier stateChangeNotifier) {
    this.triggerTemp = triggerTemp;
    this.sampleTimeSec = sampleTimeSec;
    this.window = new float[windowSize];
    this.wIndex = 0;
    this.state = State.sync;
    this.burnStartTimeMSec = -1;
    this.current = null;

    this.whAddress = DSAddress.fromUncheckedHex(dsAddress);
    this.loggerNotifier = new LoggerNotifier(logger);
    this.stateChangeNotifier = stateChangeNotifier;
    this.temp18B20 = new Temp18B20(whAddress, (byte) 1, DS18B20Scratchpad.DEFAULT_HALARM,
        DS18B20Scratchpad.DEFAULT_LALARM)
        .setBusMasterRegistry(bmRegistry);
    this.pushThread = new Thread(this);
    this.pushThread.setDaemon(true);
    this.pushThread.start();
  }

  public void start() {
  }

  public void run() {
    try {
      for (; ; ) {
        nextReading(temp18B20.getTemperature(null));

        try {
          Thread.sleep(TimeUnit.SECONDS.toMillis(sampleTimeSec));
        } catch (InterruptedException e) {
          ;
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace(); }
  }

  private void nextReading(Reading reading) {

    if (reading instanceof ReadingError) {
      setStateDeviceSync((ReadingError) reading);
      wIndex = 0;
    } else {
      float now = (float) Convert.toFahrenheit(reading.getTempC());

      if (wIndex >= window.length) {
        float start = window[(wIndex - window.length) % window.length];

        float slopeTempPerMinute = (now - start) / (window.length * sampleTimeSec / (float) 60.0);

        if ((state == State.deviceSync) || (state == State.sync)) {
          if ((now < triggerTemp) && (slopeTempPerMinute < 1.0) && (slopeTempPerMinute > -1.0)) {
            setStateFlat(reading);
          } else if (now >= triggerTemp) {
            if (slopeTempPerMinute >= 2.0) {
              setStateBurning(reading);
            } else if (slopeTempPerMinute <= -2.0) {
              setStateCooling(reading);
            }
            else {
              setStateSync(reading, slopeTempPerMinute);
            }
          }
          else {
            setStateSync(reading, slopeTempPerMinute);
          }
        }

        switch (state) {
          case flat:
            if (slopeTempPerMinute >= 2.0) {
              setStateBurning(reading);
            }
            break;

          case burning:
            if (slopeTempPerMinute <= -0.5) {
              setStateCooling(reading);
            }
            break;

          case cooling:
            if ((now < triggerTemp) && (slopeTempPerMinute > -0.5)) {
              setStateFlat(reading);
            }
            break;

          case deviceSync:
          case sync:
          default:
        }
      }
      window[wIndex++ % window.length] = now;
    }
  }

  private void setStateDeviceSync(ReadingError error) {
    state = State.deviceSync;
    Current prevCurrent = current;
    current = new Current(System.currentTimeMillis(), state, error.getError());

    loggerNotifier.stateChanged(prevCurrent, current);
    if (stateChangeNotifier != null) {
      stateChangeNotifier.stateChanged(prevCurrent, current);
    }
  }

  private void setStateSync(Reading reading, float slopeTempPerMinute) {
    state = State.sync;
    Current prevCurrent = current;
    float tempF = (float)Convert.toFahrenheit(reading.getTempC());
    String error = String.format("tempF:%f slopeTempPerMinute:%f", tempF, slopeTempPerMinute);
    current = new Current(System.currentTimeMillis(), state, error);

    loggerNotifier.stateChanged(prevCurrent, current);
    if (stateChangeNotifier != null) {
      stateChangeNotifier.stateChanged(prevCurrent, current);
    }
  }

  private void setStateFlat(Reading reading) {
    state = State.flat;
    Current prevCurrent = current;
    current = new Current(reading.getTimeMSec(), state, (float)Convert.toFahrenheit(reading.getTempC()));

    if (stateChangeNotifier != null) {
      stateChangeNotifier.stateChanged(prevCurrent, current);
    }
  }

  private void setStateBurning(Reading reading) {
    burnStartTimeMSec = reading.getTimeMSec();
    state = State.burning;
    Current prevCurrent = current;
    current = new Current(reading.getTimeMSec(), state, (float)Convert.toFahrenheit(reading.getTempC()));

    if (stateChangeNotifier != null) {
      stateChangeNotifier.stateChanged(prevCurrent, current);
    }
  }

  private void setStateCooling(Reading reading) {
    String msg = null;

    if (state == State.burning) {
      if (burnStartTimeMSec != -1) {
        float burnTimeMins = ((reading.getTimeMSec() - burnStartTimeMSec) / 1000 / 60.0F);
        msg = "burn time:" + burnTimeMins + " mins";
      }
      burnStartTimeMSec = -1;
    }

    state = State.cooling;
    Current prevCurrent = current;
    current = new Current(reading.getTimeMSec(), state, (float)Convert.toFahrenheit(reading.getTempC()));

    if (stateChangeNotifier != null) {
      stateChangeNotifier.stateChanged(prevCurrent, current);
    }
  }

  public Current getCurrent() {
    return current;
  }

  public static String toDateString(long timeMSec) {
    GregorianCalendar calendar = new GregorianCalendar();
    calendar.setTimeInMillis(timeMSec);
    return String.format("%d/%02d/%02d %02d:%02d:%02d",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH),
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        calendar.get(Calendar.SECOND));
  }

}

