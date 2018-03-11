package waterfall;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import waterfall.onewire.BusMasterRegistry;
import waterfall.onewire.DSAddress;
import waterfall.onewire.HA7SBusMasterManager;
import waterfall.onewire.busmasters.HA7S.HA7S;


/**
 * Created by dwaterfa on 12/3/17.
 */
@SpringBootApplication
public class Application {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private BusMasterRegistry bmRegistry;

  @Value("${ttypath}")
  private String ttyPath;

  @Value("${ha7sserialclass}")
  private String ha7sSerialClass;


  public static void main(String[] args) {
    ApplicationContext ctx = SpringApplication.run(Application.class, args);
    Application app = (Application) ctx.getBean(Application.class);
    app.start(args);
  }

  private void start(String[] args) {
    HA7SBusMasterManager ha7SBusMasterManager = new HA7SBusMasterManager(bmRegistry);
    HA7S[] bmList = null;
    try {
      bmList = ha7SBusMasterManager.start(ttyPath, ha7sSerialClass);
    } catch (NoSuchMethodException e) {
      logger.error(e.toString());
      System.exit(1);
    }

    if ((bmList == null) || (bmList.length == 0)) {
      logger.error("No busmaster initialized");
      System.exit(2);
    }
  }

  @Bean
  public static BusMasterRegistry makeBMRegistryBean() {
    return new BusMasterRegistry();
  }

}
