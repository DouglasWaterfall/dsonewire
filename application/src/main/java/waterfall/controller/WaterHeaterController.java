package waterfall.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import waterfall.model.WaterHeater;
import waterfall.model.WaterHeater.Current;

/**
 * Created by dwaterfa on 1/12/18.
 */
@RestController
@EnableAutoConfiguration
public class WaterHeaterController {

  @Autowired
  private WaterHeater waterHeater;

  @RequestMapping("/waterheater")
  public Current get() {
    return waterHeater.getCurrent();
  }

}


