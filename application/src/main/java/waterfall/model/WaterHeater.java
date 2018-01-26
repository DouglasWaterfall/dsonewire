package waterfall.model;

import com.dalsemi.onewire.utils.Convert;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import waterfall.onewire.BusMasterRegistry;
import waterfall.onewire.DSAddress;
import waterfall.onewire.Temp18B20;
import waterfall.onewire.Temp18B20.Reading;
import waterfall.onewire.Temp18B20.ReadingError;
import waterfall.onewire.device.DS18B20Scratchpad;

/**
 * Created by dwaterfa on 1/12/18.
 */
@SpringBootConfiguration
public class WaterHeater implements Runnable {

  // This could be passed in by a property file
  private static DSAddress whAddress = DSAddress.fromUncheckedHex("260000065BE22D28");

  /**
   * This is the temperature point where when crossing we think we are either leaving the flat state
   * when burning or arriving back to it when cooling.
   */
  private static final float tempTrigger = 85.0F;

  /**
   * How often we sample.
   */
  private static final int sampleTimeSec = 10;

  /**
   * The number of samples we take in order to compare the oldest with the most recent. A window of
   * 5 gives us (5 + 1) sample range to compare to
   */
  private static final int windowSize = 5;
  private final float[] window;
  private int wIndex;

  /**
   * These are the states for our WaterHeater
   */
  enum State {
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
     * back below the tempTrigger.
     */
    cooling
  }

  /**
   * Our state.
   */
  private State state;

  /**
   * When the current burn time started, -1 if not currently burning.
   */
  private long burnStartTimeMSec;

  @Autowired
  private BusMasterRegistry bmRegistry;

  /**
   * This is filled in on start, needs the BusMasterRegistry
   */
  private Temp18B20 temp18B20;

  private Thread pushThread;

  private Current current;

  public WaterHeater() {
    this.window = new float[windowSize];
    this.wIndex = 0;
    this.state = State.unknown;
    this.burnStartTimeMSec = -1;

    this.temp18B20 = null;
    this.pushThread = null;
    this.current = null;
  }

  @PostConstruct
  public void start() {
    this.temp18B20 = new Temp18B20(whAddress, (byte) 1, DS18B20Scratchpad.DEFAULT_HALARM,
        DS18B20Scratchpad.DEFAULT_LALARM)
        .setBusMasterRegistry(bmRegistry);

    pushThread = new Thread(this);
    pushThread.setDaemon(true);
    pushThread.start();
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
          if ((now < tempTrigger) && (slopeTempPerMinute < 1.0) && (slopeTempPerMinute > -1.0)) {
            setStateFlat(reading);
          } else if (now >= tempTrigger) {
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
            if ((now < tempTrigger) && (slopeTempPerMinute > -0.5)) {
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
    String date = toDateString(System.currentTimeMillis());

    current = new Current(date, state, error.getError());

    StringBuffer sb = new StringBuffer();
    sb.append(date);
    sb.append('\t');
    sb.append(state.name());
    if (error != null) {
      sb.append('\t');
      sb.append(error.getError());
    }
    sb.append('\n');
    System.out.print(sb.toString());
    System.out.flush();
  }

  private void setStateFlat(Reading reading) {
    state = State.flat;

    logStateChange(state, (float) Convert.toFahrenheit(reading.getTempC()),
        reading.getTimeMSec(), null);
  }

  private void setStateBurning(Reading reading) {
    burnStartTimeMSec = reading.getTimeMSec();
    state = State.burning;

    logStateChange(state, (float) Convert.toFahrenheit(reading.getTempC()),
        reading.getTimeMSec(), null);
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

    logStateChange(state, (float) Convert.toFahrenheit(reading.getTempC()),
        reading.getTimeMSec(), msg);
  }

  private void logStateChange(State newState, float tempF, long timeMSec, String addMessage) {
    String date = toDateString(timeMSec);

    current = new Current(date, state, tempF);

    StringBuffer sb = new StringBuffer();
    sb.append(date);
    sb.append('\t');
    sb.append(newState.name());
    sb.append('\t');
    sb.append(tempF);
    if (addMessage != null) {
      sb.append('\t');
      sb.append(addMessage);
    }
    sb.append('\n');
    System.out.print(sb.toString());
    System.out.flush();
  }

  private static String toDateString(long timeMSec) {
    GregorianCalendar calendar = new GregorianCalendar();
    calendar.setTimeInMillis(timeMSec);
    return String.format("%d/%d/%d %d:%d:%d",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH),
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        calendar.get(Calendar.SECOND));
  }

  public static class Current {

    public String date;
    public String state;
    public Float tempF;
    public String error;

    public Current(String date, State s, float tempF) {
      this.date = date;
      this.state = s.name();
      this.tempF = tempF;
      this.error = null;
    }

    public Current(String date, State s, String error) {
      this.date = date;
      this.state = s.name();
      this.tempF = null;
      this.error = error;

    }
  }

  public Current getCurrent() {
    return current;
  }

}
