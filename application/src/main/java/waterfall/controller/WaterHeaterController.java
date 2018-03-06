package waterfall.controller;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import waterfall.onewire.BusMasterRegistry;
import waterfall.onewire.WaterHeater;
import waterfall.onewire.WaterHeater.Current;

/**
 * Created by dwaterfa on 1/12/18.
 */
@RestController
@EnableAutoConfiguration
public class WaterHeaterController {

  @Autowired
  BusMasterRegistry busMasterRegistry;

  @Autowired
  waterfall.config.WaterHeaterConfig config;

  private WaterHeater waterHeater;

  @RequestMapping("/waterheater")
  public Current get() {
    return waterHeater.getCurrent();
  }

  @PostConstruct
  void start() {
    waterHeater = new WaterHeater(busMasterRegistry, config.getTriggerTemp(),
        config.getSampleTimeSec(), config.getWindowSize(), config.getDsAddress());
  }

}


