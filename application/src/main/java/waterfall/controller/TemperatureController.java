package waterfall.controller;

import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import waterfall.model.Temperature;

/**
 * Created by dwaterfa on 1/14/18.
 */
@RestController
@EnableAutoConfiguration
public class TemperatureController {

  @Autowired
  Temperature temperature;

  /**
   * list locations
   * @return
   */
  @RequestMapping(value = "/temperature", method = RequestMethod.GET)
  public String[] getLocations() {
    return temperature.getLocations();
  }

  /**
   *
   * @param location
   * @return
   */
  @RequestMapping(value = "/temperature/{location}", method = RequestMethod.GET)
  public HashMap<String, String> getLocation(@PathVariable String location) {
    return temperature.getTemperature(location);
  }

}
