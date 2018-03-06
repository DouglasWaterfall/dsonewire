package waterfall.controller;

import com.dalsemi.onewire.utils.Convert;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import waterfall.config.TemperatureConfig;
import waterfall.config.TemperatureConfig.Temperature;
import waterfall.onewire.BusMasterRegistry;
import waterfall.onewire.DSAddress;
import waterfall.onewire.Temp18B20;
import waterfall.onewire.Temp18B20.Reading;
import waterfall.onewire.Temp18B20.ReadingError;
import waterfall.onewire.device.DS18B20Scratchpad;

/**
 * Created by dwaterfa on 1/14/18.
 */
@RestController
@EnableAutoConfiguration
public class TemperatureController {

  @Autowired
  BusMasterRegistry bmRegistry;

  @Autowired
  TemperatureConfig config;

  private final HashMap<String, Temp18B20> deviceMap = new HashMap<>();

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

  /**
   * list locations
   * @return
   */
  @RequestMapping(value = "/temperature", method = RequestMethod.GET)
  public String[] getLocations() {
    return deviceMap.keySet().toArray(new String[0]);
  }

  /**
   *
   * @param location
   * @return
   */
  @RequestMapping(value = "/temperature/{location}", method = RequestMethod.GET)
  public HashMap<String, String> getLocation(@PathVariable String location) {
    HashMap<String, String> map = new HashMap<>();
    Temp18B20 temp18B20;

    String errorS = null;
    String timeS = null;
    String tempFS = null;

    if ((temp18B20 = deviceMap.get(location)) != null) {
      Reading reading = temp18B20.getTemperature(null);
      if (reading instanceof ReadingError) {
        errorS = reading.getError();
        timeS = toDateString(System.currentTimeMillis());
      } else {
        tempFS = String.format("%.2f", (float) Convert.toFahrenheit(reading.getTempC()));
        timeS = toDateString(reading.getTimeMSec());
      }
    } else {
      errorS = "location not found";
    }

    map.put("location", location);
    if (errorS != null) {
      map.put("error", errorS);
    }
    if (tempFS != null) {
      map.put("tempF", tempFS);
    }
    if (timeS != null) {
      map.put("time", timeS);
    }

    return map;
  }

  @PostConstruct
  public void start() {
    for (Temperature tempConfig : config.getList()) {
      String location = tempConfig.getLocation();
      DSAddress dsAddress = tempConfig.getDsAddress();
      byte resolution = tempConfig.getResolution();

      Temp18B20 temp18B20 = new Temp18B20(dsAddress, resolution,
          DS18B20Scratchpad.DEFAULT_HALARM,
          DS18B20Scratchpad.DEFAULT_LALARM)
          .setBusMasterRegistry(bmRegistry);

      deviceMap.put(location, temp18B20);
    }
  }

}
