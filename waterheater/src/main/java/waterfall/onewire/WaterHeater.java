/**
 * Created by dwaterfa on 1/12/18.
 */
package waterfall.onewire;

import com.dalsemi.onewire.utils.Convert;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
      int windowSize, String dsAddress) {
    this.triggerTemp = triggerTemp;
    this.sampleTimeSec = sampleTimeSec;
    this.window = new float[windowSize];
    this.wIndex = 0;
    this.state = State.unknown;
    this.burnStartTimeMSec = -1;
    this.current = null;
    
    this.whAddress = DSAddress.fromUncheckedHex(dsAddress);
    this.temp18B20 = new Temp18B20(whAddress, (byte) 1, DS18B20Scratchpad.DEFAULT_HALARM,
        DS18B20Scratchpad.DEFAULT_LALARM)
        .setBusMasterRegistry(bmRegistry);
    this.pushThread = new Thread(this);
    this.pushThread.setDaemon(true);
    this.pushThread.start();
  }

  private void logStateChange(Current current, String addMessage) {
    StringBuffer sb = new StringBuffer();
    sb.append(toDateString(current.stateStartMSec));
    sb.append('\t');
    sb.append(current.state.name());
    if (current.tempF != null) {
      sb.append('\t');
      sb.append(current.tempF);
    }
    else if (current.error != null) {
      sb.append('\t');
      sb.append(current.error);
    }
    if (addMessage != null) {
      sb.append('\t');
      sb.append(addMessage);
    }
    sb.append('\n');
    logger.info(sb.toString());
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
      e.printStackTrace();
    }
  }

  private void nextReading(Reading reading) {

    if (reading instanceof ReadingError) {
      setStateUnknown((ReadingError) reading);
      wIndex = 0;
    } else {
      float now = (float) Convert.toFahrenheit(reading.getTempC());

      if (wIndex >= window.length) {
        float start = window[(wIndex - window.length) % window.length];

        float slopeTempPerMinute = (now - start) / (window.length * sampleTimeSec / (float) 60.0);

        if (state == State.unknown) {
          if ((now < triggerTemp) && (slopeTempPerMinute < 1.0) && (slopeTempPerMinute > -1.0)) {
            setStateFlat(reading);
          } else if (now >= triggerTemp) {
            if (slopeTempPerMinute >= 2.0) {
              setStateBurning(reading);
            } else if (slopeTempPerMinute <= -2.0) {
              setStateCooling(reading);
            }
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

          case unknown:
          default:
        }
      }
      window[wIndex++ % window.length] = now;
    }
  }

  private void setStateUnknown(ReadingError error) {
    state = State.unknown;
    current = new Current(System.currentTimeMillis(), state, error.getError(), current);

    logStateChange(current, null);
  }

  private void setStateFlat(Reading reading) {
    state = State.flat;
    current = new Current(reading.getTimeMSec(), state,
        (float)Convert.toFahrenheit(reading.getTempC()), current);

    logStateChange(current, null);
  }

  private void setStateBurning(Reading reading) {
    burnStartTimeMSec = reading.getTimeMSec();
    state = State.burning;
    current = new Current(reading.getTimeMSec(), state,
        (float)Convert.toFahrenheit(reading.getTempC()), current);

    logStateChange(current, null);
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
    current = new Current(reading.getTimeMSec(), state,
        (float)Convert.toFahrenheit(reading.getTempC()), current);

    logStateChange(current, null);
  }

  public Current getCurrent() {
    return current;
  }

  /**
   * These are the states for our WaterHeater
   */
  public enum State {
    /**
     * We are not able, or have not enough data, to determine if the WaterHeater is in any of the
     * other three states.
     */
    unknown,

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

  public static class Current {

    @JsonIgnore
    public final long stateStartMSec;

    public final State state;
    public final String stateStart;
    public final Float tempF;
    public final String error;
    public final Burn[] burns;

    public Current(long stateStartMSec, State s, float tempF, Current prevCurrent) {
      this.stateStartMSec = stateStartMSec;
      this.state = s;
      this.stateStart = toDateString(stateStartMSec);
      this.tempF = tempF;
      this.error = null;

      if (prevCurrent != null) {
        if ((s == State.cooling) && (prevCurrent.state == State.burning) &&
            (prevCurrent.error == null)) {
          float burnTimeSec = ((float)(stateStartMSec - prevCurrent.stateStartMSec)) / 1000;
          this.burns = addBurn(prevCurrent.burns,
              toDateString(prevCurrent.stateStartMSec), burnTimeSec);
        }
        else {
          this.burns = prevCurrent.burns;
        }
      }
      else {
        this.burns = null;
      }
    }

    public Current(long atStartMSec, State s, String error, Current prevCurrent) {
      this.stateStartMSec = atStartMSec;
      this.state = s;
      this.stateStart = toDateString(stateStartMSec);
      this.tempF = null;
      this.error = error;

      if (prevCurrent != null) {
        this.burns = prevCurrent.burns;
      }
      else {
        this.burns = null;
      }
    }

    private Burn[] addBurn(Burn[] prevBurns, String date, float timeSecs) {
      Burn newBurn = new Burn(date, timeSecs);
      Burn[] newBurns = null;

      if (prevBurns != null) {
        newBurns = new Burn[Math.min(10, prevBurns.length + 1)];
        int toCopy = Math.min(9, prevBurns.length);
        for (int i = 0; i < toCopy; i++) {
          newBurns[1 + i] = prevBurns[i];
        }
      }
      else {
        newBurns = new Burn[1];
      }

      newBurns[0] = newBurn;
      return newBurns;
    }
  }

  public static class Burn {

    public String date;
    public float timeSecs;

    public Burn(String date, float timeSec) {
      this.date = date;
      this.timeSecs = timeSec;
    }
  }

  private static String toDateString(long timeMSec) {
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
