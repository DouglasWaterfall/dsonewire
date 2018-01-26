package waterfall;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import waterfall.onewire.BusMasterRegistry;
import waterfall.onewire.DSAddress;
import waterfall.onewire.HA7SBusMasterManager;
import waterfall.onewire.busmaster.Logger.LogLevel;
import waterfall.onewire.busmasters.HA7S.HA7S;


/**
 * Created by dwaterfa on 12/3/17.
 */
@SpringBootApplication
public class Application {

  @Autowired
  private BusMasterRegistry bmRegistry;

  public static void main(String[] args) {
    ApplicationContext ctx = SpringApplication.run(Application.class, args);
    Application app = (Application) ctx.getBean(Application.class);
    app.start(args);
  }

  private void start(String[] args) {
    HA7SBusMasterManager ha7SBusMasterManager = new HA7SBusMasterManager(bmRegistry);
    HA7S[] bmList = null;
    try {
      bmList = ha7SBusMasterManager
          .start("/dev/ttyAMA0", "waterfall.onewire.busmasters.HA7S.JSSC", LogLevel.CmdOnlyLevel());
      //    .start("/dev/ttyAMA0", "waterfall.onewire.busmasters.HA7S.HA7SSerialDummy", LogLevel.CmdOnlyLevel());
    } catch (NoSuchMethodException e) {
      System.err.println(e);
      System.exit(1);
    }

    if ((bmList == null) || (bmList.length == 0)) {
      System.err.println("No busmaster initialized");
      System.exit(2);
    }
  }

  @Bean
  public static BusMasterRegistry makeBMRegistryBean() {
    return new BusMasterRegistry();
  }

}
