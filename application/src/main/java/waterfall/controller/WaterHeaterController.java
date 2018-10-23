package waterfall.controller;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import waterfall.db.entities.WaterHeaterBurnEntity;
import waterfall.onewire.BusMasterRegistry;
import waterfall.onewire.waterheater.State;
import waterfall.onewire.waterheater.StateChangeNotifier;
import waterfall.onewire.waterheater.WaterHeater;
import waterfall.onewire.waterheater.Current;

/**
 * Created by dwaterfa on 1/12/18.
 */
@RestController
@EnableAutoConfiguration
public class WaterHeaterController extends StateChangeNotifier {

  @Autowired
  BusMasterRegistry busMasterRegistry;

  @Autowired
  waterfall.config.WaterHeaterConfig config;

  @Autowired
  waterfall.db.repositories.WaterHeaterRepository repository;

  private WaterHeater waterHeater;

  @RequestMapping("/waterheater")
  public Current get() {
    return waterHeater.getCurrent();
  }

  @PostConstruct
  void start() {
    waterHeater = new WaterHeater(busMasterRegistry, config.getTriggerTemp(),
        config.getSampleTimeSec(), config.getWindowSize(), config.getDsAddress(), this);
  }

  // This will be called back by the WaterHeater when the state changes.
  public void stateChanged(Current prevCurrent, Current newCurrent) {

    // We only track burns so we have to look for state transitions from burning to anything else.
    if ((prevCurrent == null) || (prevCurrent.state != State.burning)) {
      return;
    }

    // this is theoretically not possible according to the spec, but let's be careful.
    if (newCurrent.state == State.burning) {
      return;
    }

    long burnTime = newCurrent.stateStartMSec - prevCurrent.stateStartMSec;

    WaterHeaterBurnEntity whe = new WaterHeaterBurnEntity();
    whe.setStartDTS(prevCurrent.stateStartMSec);
    whe.setEndDTS(newCurrent.stateStartMSec);
    repository.save(whe);
  }

}


