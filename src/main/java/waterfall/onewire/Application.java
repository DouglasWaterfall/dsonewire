package waterfall.onewire;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import waterfall.onewire.busmaster.BusMaster;
import waterfall.onewire.busmaster.HA7S.HA7S;

/*
import org.springframework.beans.BeansException;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
*/

@SpringBootApplication
@EnableScheduling
public class Application {

    @Autowired
    private BusMasterManager busMasterManager;

    public void start() {
        System.out.println("starting dsonewireserver...");

        //
        // This a hard wired device configuration.
        //
        BusMaster busmaster = new HA7S("/dev/ttyAMA0"); // or "/dev/ttyS0"

        busMasterManager.add(busmaster);

        try {
            Thread.sleep(1000 * 60 * 60 * 24 * 7);
        } catch (InterruptedException e) {
            System.out.println("Sleep interrupted");
        }
    }

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(Application.class, args);
        Application app = (Application) ctx.getBean(Application.class);
        app.start();
        System.exit(0);
    }

}