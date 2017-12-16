package waterfall;

import com.dalsemi.onewire.utils.Convert;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import waterfall.onewire.BusMasterRegistry;
import waterfall.onewire.DSAddress;
import waterfall.onewire.HA7SBusMasterManager;
import waterfall.onewire.Temp18B20;
import waterfall.onewire.Temp18B20.Reading;
import waterfall.onewire.Temp18B20.ReadingError;
import waterfall.onewire.busmaster.BusMaster;
import waterfall.onewire.busmaster.SearchBusCmd;
import waterfall.onewire.busmaster.SearchBusCmd.Result;
import waterfall.onewire.busmasters.HA7S.HA7S;
import waterfall.onewire.device.DS18B20Scratchpad;


/**
 * Created by dwaterfa on 12/3/17.
 */
public class Application {

  public static void main(String[] args) {

    BusMasterRegistry bmRegistry = new BusMasterRegistry();
    HA7SBusMasterManager ha7SBusMasterManager = new HA7SBusMasterManager(bmRegistry);
    HA7S[] bmList = null;
    try {
      bmList = ha7SBusMasterManager
          .start("/dev/ttyAMA0", waterfall.onewire.busmasters.HA7S.JSSC.class, null);
    } catch (NoSuchMethodException e) {
      System.err.println(e);
      System.exit(1);
    }

    if ((bmList == null) || (bmList.length == 0)) {
      System.err.println("No busmaster initialized");
      System.exit(2);
    }

    SearchBusCmd cmd = bmList[0].querySearchBusByFamilyCmd((short) 0x28);
    cmd.execute();
    if (cmd.getResult() != Result.success) {
      System.err.println("Search by familyCode 0x28 found nothing");
      System.exit(2);
    }

    long limitTimeMSec = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
    long delayMSec = TimeUnit.SECONDS.toMillis(15);
    long startDelayIncrementMSec = TimeUnit.SECONDS.toMillis(2);
    long startDelayMSec = 0;

    int count = cmd.getResultList().size();
    for (int i = 0; i < count; i++) {
      DSAddress dsAddress = cmd.getResultList().get(i);

      Temp18B20Agent agent = new Temp18B20Agent(dsAddress, bmList[0], startDelayMSec, delayMSec, limitTimeMSec);
      startDelayMSec += startDelayIncrementMSec;

      if ((i + 1) < count) {
        new Thread(agent).start();
      }
      else {
        agent.run();
      }
    }
  }

  private static class Temp18B20Agent implements Runnable {

    private final DSAddress dsAddress;
    private final BusMaster bm;
    private final long startDelayMSec;
    private final long delayMSec;
    private final long limitTimeMSec;

    public Temp18B20Agent(DSAddress dsAddress, BusMaster bm, long startDelayMSec, long delayMSec,
        long limitTimeMSec) {
      this.dsAddress = dsAddress;
      this.bm = bm;
      this.startDelayMSec = startDelayMSec;
      this.delayMSec = delayMSec;
      this.limitTimeMSec = limitTimeMSec;
    }

    public void run() {
      System.err.println(
          dsAddress.toString() + " delayMSec:" + delayMSec + " limit:"
              + (new Date(limitTimeMSec).toString()));

      Temp18B20 temp18B20 = new Temp18B20(dsAddress, DS18B20Scratchpad.DEFAULT_RESOLUTION,
          DS18B20Scratchpad.DEFAULT_HALARM, DS18B20Scratchpad.DEFAULT_LALARM)
          .setBusMaster(bm);

      delay(startDelayMSec);

      for (; ; ) {
        StringBuffer sb = new StringBuffer();
        sb.append(dsAddress.toString());
        sb.append(",");

        Reading reading = temp18B20.getTemperature(0L);
        if (reading instanceof ReadingError) {
          sb.append(new Date(System.currentTimeMillis()));
          sb.append(",");
          sb.append(reading.getError());
        } else {
          sb.append(new Date(reading.getTimeMSec()).toString());
          sb.append(",");
          sb.append(Convert.toFahrenheit(reading.getTempC()));
        }

        System.out.println(sb.toString());
        System.out.flush();

        if (System.currentTimeMillis() >= limitTimeMSec) {
          break;
        }

        delay(delayMSec);
      }
    }

    private void delay(long msec) {
      if (msec > 0) {
        try {
          Thread.sleep(msec);
        } catch (InterruptedException e) {
        }
      }
    }

  }

}
