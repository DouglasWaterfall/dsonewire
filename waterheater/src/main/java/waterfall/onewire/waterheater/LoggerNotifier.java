package waterfall.onewire.waterheater;

import org.slf4j.Logger;

public class LoggerNotifier extends StateChangeNotifier {

    private final Logger logger;

    public LoggerNotifier(Logger logger) {
        this.logger = logger;
    }

    public void stateChanged(Current prevCurrent, Current newCurrent) {
        StringBuffer sb = new StringBuffer();
        sb.append(WaterHeater.toDateString(newCurrent.stateStartMSec));
        sb.append('\t');
        sb.append(newCurrent.state.name());
        sb.append('\t');
        if (newCurrent.tempF != null) {
            sb.append(newCurrent.tempF);
        }
        else if (newCurrent.error != null) {
            sb.append(newCurrent.error);
        }
        sb.append('\n');
        logger.info(sb.toString());
    }

}
