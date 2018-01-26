package waterfall.model;

import com.dalsemi.onewire.utils.Convert;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
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
   * These are our pre-wired devices by location.
   * Location
   * DSAddress
   * Resolution
   */
  private static final String[][] devices = {
      {"Attic", "7C0000063BB13028", "3"},
      {"EastBedroom", "740000063B353428", "3"},
      {"WestBedroom", "7F0000063BA12F28", "3"},
      {"Main", "5A0000063B7AF528", "1"},
      {"Storage", "ED0000063BC00428", "1"},
      {"TVRoom", "EE0000065BC0AE28", "1"},
      {"Outside", "410000063C088028", "1"}
  };

  private HashMap<String, Temp18B20> deviceMap = new HashMap<>();

  @PostConstruct
  public void start() {
    for (String[] dev: devices) {
      String location = dev[0];
      DSAddress dsAddress = DSAddress.fromUncheckedHex(dev[1]);
      byte resolution = Byte.valueOf(dev[2]);

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

    if ((temp18B20 = deviceMap.get(location)) != null) {
      Reading reading = temp18B20.getTemperature(null);
      if (reading instanceof ReadingError) {
        map.put("error", reading.getError());
        map.put("time" , new Date(System.currentTimeMillis()).toString());
      }
      else {
        map.put("tempF", String.valueOf(Convert.toFahrenheit(reading.getTempC())));
        map.put("time" , new Date(reading.getTimeMSec()).toString());
      }
    }
    else {
      map.put("error", "location:" + location + " not found");
    }

    return map;
  }

}
