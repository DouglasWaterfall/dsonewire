package waterfall.onewire.HttpClient;

import java.util.Map;

/**
 * Created by dwaterfa on 1/14/17.
 */
public class WaitForEventCmd {
    private long waitTimeoutMSec;

    private long serverTimestampMSec;

    private long bmListChangedNotifyTimestampMSec;

    private Map<String, Long> bmSearchNotifyTimestampMSec;

    private Map<String, Long> bmAlarmSearchNotifyTimestampMSec;

    public WaitForEventCmd(long waitTimeoutMSec,
                           long serverTimestampMSec, long bmListChangedNotifyTimestampMSec,
                           Map<String, Long> bmSearchNotifyTimestampMSec,
                           Map<String, Long> bmAlarmSearchNotifyTimestampMSec) {
        this.waitTimeoutMSec = waitTimeoutMSec;
        this.serverTimestampMSec = serverTimestampMSec;
        this.bmListChangedNotifyTimestampMSec = bmListChangedNotifyTimestampMSec;
        this.bmSearchNotifyTimestampMSec = bmSearchNotifyTimestampMSec;
        this.bmAlarmSearchNotifyTimestampMSec = bmAlarmSearchNotifyTimestampMSec;
    }

    /**
     * @return
     */
    public long getWaitTimeoutMSec() {
        return waitTimeoutMSec;
    }

    public void setWaitTimeoutMSec(long waitTimeoutMSec) {
        this.waitTimeoutMSec = waitTimeoutMSec;
    }

    /**
     * May be zero when the client has no prior state.
     * @return
     */
    public long getServerTimestampMSec() {
        return serverTimestampMSec;
    }

    public void setServerTimestampMSec(long serverTimestampMSec) {
        this.serverTimestampMSec = serverTimestampMSec;
    }

    /**
     * May be zero when the client has no prior state.
     * @return
     */
    public long getBMListChangedNotifyTimestampMSec() {
        return bmListChangedNotifyTimestampMSec;
    }

    public void setBmListChangedNotifyTimestampMSec(long bmListChangedNotifyTimestampMSec) {
        this.bmListChangedNotifyTimestampMSec = bmListChangedNotifyTimestampMSec;
    }

    /**
     * May be null
     * @return
     */
    public Map<String, Long> getBMSearchNotifyTimestampMSec() {
        return bmSearchNotifyTimestampMSec;
    }

    public void setBMSearchNotifyTimestampMSec(Map<String, Long> bmSearchNotifyTimestampMSec) {
        this.bmSearchNotifyTimestampMSec = bmSearchNotifyTimestampMSec;
    }

    /**
     * May be null
     * @return
     */
    public Map<String, Long> getBMAlarmSearchNotifyTimestampMSec() {
        return bmAlarmSearchNotifyTimestampMSec;
    }

    public void setBMAlarmSearchNotifyTimestampMSec(Map<String, Long> bmAlarmSearchNotifyTimestampMSec) {
        this.bmAlarmSearchNotifyTimestampMSec = bmAlarmSearchNotifyTimestampMSec;
    }

}
