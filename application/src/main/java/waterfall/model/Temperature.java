package waterfall.model;

import com.dalsemi.onewire.utils.Convert;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import waterfall.onewire.BusMasterRegistry;
import waterfall.onewire.DSAddress;
import waterfall.onewire.Temp18B20;
import waterfall.onewire.Temp18B20.Reading;
import waterfall.onewire.Temp18B20.ReadingError;
import waterfall.onewire.device.DS18B20Scratchpad;

/**
 * Created by dwaterfa on 1/15/18.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class Temperature {

  @Autowired
  BusMasterRegistry bmRegistry;

  /**
   * These are our pre-wired devices by location. Location DSAddress Resolution
   */
  private static final Object[][] devices = {
      {"Attic", "7C0000063BB13028", DS18B20Scratchpad.RESOLUTION_10},
      // North wall lower bookcase
      {"EastBedroom", "740000063B353428", DS18B20Scratchpad.RESOLUTION_10},
      // ToDo: East Bedroom Ceiling Light

      // Behind TV
      {"WestBedroomTV", "7F0000063BA12F28", DS18B20Scratchpad.RESOLUTION_10},
      // Ceiling fan housing
      {"WestBedroomCeiling", "5F0000065CCD1A28", DS18B20Scratchpad.RESOLUTION_10},
      // South wall white couch
      {"Main", "5A0000063B7AF528", DS18B20Scratchpad.RESOLUTION_10},
      // Patch panel
      {"Storage", "ED0000063BC00428", DS18B20Scratchpad.RESOLUTION_10},
      // North wall behind TV
      {"TVRoom", "EE0000065BC0AE28", DS18B20Scratchpad.RESOLUTION_10},
      // South Wall near NIC
      {"Outside", "410000063C088028", DS18B20Scratchpad.RESOLUTION_10}
  };

  private HashMap<String, Temp18B20> deviceMap = new HashMap<>();

  @PostConstruct
  public void start() {
    for (Object[] dev : devices) {
      String location = (String) dev[0];
      DSAddress dsAddress = DSAddress.fromUncheckedHex((String) dev[1]);
      byte resolution = (Byte) dev[2];

      Temp18B20 temp18B20 = new Temp18B20(dsAddress, resolution,
          DS18B20Scratchpad.DEFAULT_HALARM,
          DS18B20Scratchpad.DEFAULT_LALARM)
          .setBusMasterRegistry(bmRegistry);

      deviceMap.put(location, temp18B20);
    }
  }

  public String[] getLocations() {
    return deviceMap.keySet().toArray(new String[0]);
  }

  public HashMap<String, String> getTemperature(String location) {
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
