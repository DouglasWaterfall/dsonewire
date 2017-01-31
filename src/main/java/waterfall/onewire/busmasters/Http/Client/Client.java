package waterfall.onewire.busmasters.Http.Client;

import waterfall.onewire.DSAddress;
import waterfall.onewire.HttpClient.*;
import waterfall.onewire.busmaster.*;
import waterfall.onewire.busmasters.Util;

import java.util.ArrayList;

/**
 * Created by dwaterfa on 2/15/16.
 */
public class Client implements BusMaster {
    private final String USER_AGENT = "waterfall.onewire.busmaster.HTTP;1.0";

    private final int connectionTimeoutMSec = 10000;

    private final String endpoint;
    private final String authorization;
    private final String bmIdent;
    private final String bmName;
    private final NotifySearchBusCmdHelper searchHelper;
    private final NotifySearchBusCmdHelper searchByAlarmHelper;

    private long remoteTimeDiffMSec;

    public Client(String endpoint, String authorization, String bmIdent, String bmName) {
        this.endpoint = endpoint;
        this.bmIdent = bmIdent;
        {
            StringBuffer sb = new StringBuffer();
            sb.append(this.getClass().getSimpleName());
            sb.append(" [");
            if (endpoint != null) {
                sb.append(endpoint);
            }
            if (bmName != null) {
                sb.append("/" + bmName);
            }
            sb.append("]");
            this.bmName = sb.toString();
        }
        this.authorization = authorization;

        this.searchHelper = new NotifySearchBusCmdHelper(this, false);
        this.searchByAlarmHelper = new NotifySearchBusCmdHelper(this, true);

        this.remoteTimeDiffMSec = calculateTimeDiff(null);
    }

    public String getName() { return bmName; }

    public long getCurrentTimeMillis() {
        return System.currentTimeMillis() + remoteTimeDiffMSec;
    }

    public boolean getIsStarted() {
        return true;
    }

    public waterfall.onewire.busmaster.StartBusCmd queryStartBusCmd(Logger.LogLevel logLevel) {
        return new StartBusCmd(this, logLevel);
    }

    public waterfall.onewire.busmaster.StopBusCmd queryStopBusCmd(Logger.LogLevel logLevel) {
        return new StopBusCmd(this, logLevel);
    }

    public waterfall.onewire.busmaster.SearchBusCmd querySearchBusCmd(Logger.LogLevel logLevel) {
        return new SearchBusCmd(this, false, logLevel);
    }

    public waterfall.onewire.busmaster.SearchBusCmd querySearchBusByFamilyCmd(short familyCode, Logger.LogLevel logLevel) {
        return new SearchBusCmd(this, familyCode, logLevel);
    }

    public waterfall.onewire.busmaster.SearchBusCmd querySearchBusByAlarmCmd(Logger.LogLevel logLevel) {
        return new SearchBusCmd(this, true, logLevel);
    }

    public ScheduleNotifySearchBusCmdResult scheduleNotifySearchBusCmd(NotifySearchBusCmdResult obj, boolean byAlarm, long minPeriodMSec) {
        if (!byAlarm) {
            return searchHelper.scheduleSearchNotifyFor(obj, minPeriodMSec);
        } else {
            return searchByAlarmHelper.scheduleSearchNotifyFor(obj, minPeriodMSec);
        }
    }

    public UpdateScheduledNotifySearchBusCmdResult updateScheduledNotifySearchBusCmd(NotifySearchBusCmdResult obj, boolean byAlarm, long minPeriodMSec) {
        if (!byAlarm) {
            return searchHelper.updateScheduledSearchNotifyFor(obj, minPeriodMSec);
        } else {
            return searchByAlarmHelper.updateScheduledSearchNotifyFor(obj, minPeriodMSec);
        }
    }

    public CancelScheduledNotifySearchBusCmdResult cancelScheduledNotifySearchBusCmd(NotifySearchBusCmdResult obj, boolean byAlarm) {
        if (!byAlarm) {
            return searchHelper.cancelScheduledSearchNotifyFor(obj);
        }
        else {
            return searchByAlarmHelper.cancelScheduledSearchNotifyFor(obj);
        }
    }

    public void searchBusCmdExecuteCallback(waterfall.onewire.busmaster.SearchBusCmd cmd) {
        if ((cmd.getBusMaster() == this) && (cmd.getResult() == waterfall.onewire.busmaster.SearchBusCmd.Result.success)) {
            if (cmd.isByAlarm()) {
                searchByAlarmHelper.notifySearchResult(cmd.getResultData());
            } else if (!cmd.isByFamilyCode()) {
                searchHelper.notifySearchResult(cmd.getResultData());
            }
        }
    }

    public waterfall.onewire.busmaster.ConvertTCmd queryConvertTCmd(DSAddress dsAddr, Logger.LogLevel logLevel) {
        return new ConvertTCmd(this, dsAddr, logLevel);
    }

    public waterfall.onewire.busmaster.ReadPowerSupplyCmd queryReadPowerSupplyCmd(DSAddress dsAddr, Logger.LogLevel logLevel) {
        return new ReadPowerSupplyCmd(this, dsAddr, logLevel);
    }

    public waterfall.onewire.busmaster.ReadScratchpadCmd queryReadScratchpadCmd(DSAddress dsAddr, short requestByteCount, Logger.LogLevel logLevel) {
        return new ReadScratchpadCmd(this, dsAddr, requestByteCount, logLevel);
    }

    //
    // Client specific methods.
    //
    public String getBmIdent() {
        return bmIdent;
    }

    public synchronized waterfall.onewire.busmaster.StartBusCmd.Result StartBusCmd(Logger optLogger) {
        return waterfall.onewire.busmaster.StartBusCmd.Result.already_started;
    }

    public synchronized waterfall.onewire.busmaster.StopBusCmd.Result StopBusCmd(Logger optLogger) {
        return waterfall.onewire.busmaster.StopBusCmd.Result.communication_error;
    }

    public long getRemoteTimeDiffMSec() {
        return remoteTimeDiffMSec;
    }

    private long calculateTimeDiff(Logger optLogger) {
        final String logContext = (optLogger != null) ? Client.class.getSimpleName() + ".calculateTimeDiff: " : "";
        ArrayList<TimeDiffResult> results = new ArrayList<TimeDiffResult>();

        for (int i = 0; i < 5; i++) {
            final String suffix = "timeDiff/" + System.currentTimeMillis();

            TimeDiffResult result = (TimeDiffResult) postURLDataWithAuthorization(suffix, TimeDiffResult.class);

            long clientReceivedTimeMSec = System.currentTimeMillis();

            if (result.hasPostError()) {
                Util.logErrorCommLevel(optLogger, logContext, " postError:" + result.getPostError().name());
            } else if (result.controllerError != null) {
                Util.logErrorCommLevel(optLogger, logContext, " controllerError:" + result.controllerError);
            } else {
                result.setClientReceivedTimeMSec(clientReceivedTimeMSec);
                results.add(result);
            }
        }

        if (results.size() == 0) {
            return 0;
        }

        // what to ADD to the server writeCTM to put it in our local terms.
        long minDiff = (results.get(0).serverReceivedTimeMSec - results.get(0).clientSentTimeMSec);

        for (int i = 1; i < results.size(); i++) {
            long t_minDiff = (results.get(i).serverReceivedTimeMSec - results.get(i).clientSentTimeMSec);
            if (t_minDiff < minDiff) {
                minDiff = t_minDiff;
            }
        }

        Util.logErrorCommLevel(optLogger, logContext, " minDiff:" + minDiff);
        return minDiff;
    }

    public BaseCmdPostResult postURLDataNoAuthorization(String suffix, Class respClass) {
        return Util.postURLData(endpoint, suffix, null, connectionTimeoutMSec, null, respClass);
    }

    public BaseCmdPostResult postURLDataWithAuthorization(String suffix, Class respClass) {
        return Util.postURLData(endpoint, suffix, authorization, connectionTimeoutMSec, null, respClass);
    }

}
