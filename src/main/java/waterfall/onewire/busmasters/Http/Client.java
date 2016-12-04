package waterfall.onewire.busmasters.Http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.springframework.http.HttpHeaders;
import waterfall.onewire.DSAddress;
import waterfall.onewire.HttpClient.*;
import waterfall.onewire.busmaster.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

/**
 * Created by dwaterfa on 2/15/16.
 */
public class Client implements BusMaster {
    private final String USER_AGENT = "waterfall.onewire.busmaster.HTTP;1.0";

    private final String endpoint;
    private final String bmIdent;

    private SearchBusNotifyHelper searchHelper = null;
    private SearchBusNotifyHelper searchByAlarmHelper = null;

    private boolean started;
    private long remoteTimeDiffMSec;

    private String bmName;
    private String authorization;

    public Client(String endpoint, String bmIdent) {
        this.endpoint = endpoint;
        this.bmIdent = bmIdent;

        this.searchHelper = new SearchBusNotifyHelper(this, false);
        this.searchByAlarmHelper = new SearchBusNotifyHelper(this, true);

        this.started = false;
        this.bmName = null;
        this.remoteTimeDiffMSec = 0;
    }

    public String getName() {
        StringBuffer sb = new StringBuffer();

        sb.append(this.getClass().getSimpleName());
        sb.append(" [");
        if (endpoint != null) {
            sb.append(endpoint);
        }
        if (bmIdent != null) {
            sb.append("/" + bmIdent);
        }
        if (bmName != null) {
            sb.append("/" + bmName);
        }
        sb.append("]");

        return sb.toString();
    }

    public long getCurrentTimeMillis() {
        return System.currentTimeMillis() + remoteTimeDiffMSec;
    }

    public boolean getIsStarted() {
        return started;
    }

    public StartBusCmd queryStartBusCmd(Logger.LogLevel logLevel) {
        return new HttpStartBusCmd(this, logLevel);
    }

    public StopBusCmd queryStopBusCmd(Logger.LogLevel logLevel) {
        return new HttpStopBusCmd(this, logLevel);
    }

    public SearchBusCmd querySearchBusCmd(Logger.LogLevel logLevel) {
        return new HttpSearchBusCmd(this, false, logLevel);
    }

    public ScheduleSearchResult scheduleSearchNotifyFor(SearchBusCmdNotifyResult obj, long minPeriodMSec) {
        return searchHelper.scheduleSearchNotifyFor(obj, minPeriodMSec);
    }

    public boolean cancelSearchNotifyFor(SearchBusCmdNotifyResult obj) {
        return searchHelper.cancelSearchNotifyFor(obj);
    }

    public void searchBusCmdExecuteCallback(SearchBusCmd cmd) {
        if ((cmd.getBusMaster() == this) && (cmd.getResult() == SearchBusCmd.Result.success)) {
            if (cmd.isByAlarm()) {
                searchByAlarmHelper.notifySearchResult(cmd.getResultData());
            }
            else if (!cmd.isByFamilyCode()) {
                searchHelper.notifySearchResult(cmd.getResultData());
            }
        }
    }

    public SearchBusCmd querySearchBusByFamilyCmd(short familyCode, Logger.LogLevel logLevel) {
        return new HttpSearchBusCmd(this, familyCode, logLevel);
    }

    public SearchBusCmd querySearchBusByAlarmCmd(Logger.LogLevel logLevel) {
        return new HttpSearchBusCmd(this, true, logLevel);
    }

    public ScheduleSearchResult scheduleAlarmSearchNotifyFor(SearchBusByAlarmCmdNotifyResult obj, long minPeriodMSec) {
        return searchByAlarmHelper.scheduleSearchNotifyFor(obj, minPeriodMSec);
    }

    public boolean cancelAlarmSearchNotifyFor(SearchBusByAlarmCmdNotifyResult obj) {
        return searchByAlarmHelper.cancelSearchNotifyFor(obj);
    }

    public ConvertTCmd queryConvertTCmd(DSAddress dsAddr, Logger.LogLevel logLevel) {
        return new HttpConvertTCmd(this, dsAddr, logLevel);
    }

    public ReadPowerSupplyCmd queryReadPowerSupplyCmd(DSAddress dsAddr, Logger.LogLevel logLevel) {
        return new HttpReadPowerSupplyCmd(this, dsAddr, logLevel);
    }

    public ReadScratchpadCmd queryReadScratchpadCmd(DSAddress dsAddr, short requestByteCount, Logger.LogLevel logLevel) {
        return new HttpReadScratchpadCmd(this, dsAddr, requestByteCount, logLevel);
    }

    //
    // Client specific methods.
    //
    public String getBmIdent() {
        return bmIdent;
    }

    public synchronized StartBusCmd.Result StartBusCmd(Logger optLogger) {
        if (started) {
            return StartBusCmd.Result.already_started;
        }

        final String logContext = (optLogger != null) ? this.getClass().getSimpleName() + ".StartBusCmd bmIdent:" + bmIdent + " " : "";
        final String statusSuffix = "busStatusCmd/" + bmIdent;

        StatusCmdResult statusPostResult = (StatusCmdResult) postURLDataNoAuthorization(statusSuffix, StatusCmdResult.class);

        if (statusPostResult.postError != null) {
            logErrorCommLevel(optLogger, logContext, " status postError:" + statusPostResult.postError.name());
            return StartBusCmd.Result.communication_error;
        }

        if (statusPostResult.controllerError != null) {
            logErrorCommLevel(optLogger, logContext, " status controllerError:" + statusPostResult.controllerError);
            return StartBusCmd.Result.communication_error;
        }

        bmName = statusPostResult.name;

        authorization = statusPostResult.authorization;

        logErrorCommLevel(optLogger, logContext, " status name:" + bmName + " authorization:" + authorization);

        if (statusPostResult.started) {
            logErrorCommLevel(optLogger, logContext, " status already started");
            started = true;

            remoteTimeDiffMSec = calculateTimeDiff(optLogger);

            return StartBusCmd.Result.started;
        }

        // Try to start the remote busmaster.
        String logLevelParam = computeLogLevelParam(optLogger);
        final String startSuffix = "startBusCmd/" + bmIdent + ((logLevelParam != null) ? logLevelParam : "");

        StartBusCmdResult startPostResult = (StartBusCmdResult) postURLDataWithAuthorization(startSuffix, StartBusCmdResult.class);

        if (startPostResult.postError != null) {
            logErrorCommLevel(optLogger, logContext, " start postError:" + startPostResult.postError.name());
            return StartBusCmd.Result.bus_not_found;
        }

        if (startPostResult.controllerError != null) {
            logErrorCommLevel(optLogger, logContext, " start controllerError:" + startPostResult.controllerError);
            return StartBusCmd.Result.communication_error;
        }

        StartBusCmd.Result tStartResult;
        try {
            tStartResult = Enum.valueOf(StartBusCmd.Result.class, startPostResult.result);
        } catch (IllegalArgumentException e) {
            logErrorCommLevel(optLogger, logContext, " start bad result enum:" + startPostResult.result);
            return StartBusCmd.Result.communication_error;
        }

        if ((tStartResult != StartBusCmd.Result.already_started) &&
                (tStartResult != StartBusCmd.Result.started)) {
            logErrorCommLevel(optLogger, logContext, " start result:" + tStartResult.name());
            return StartBusCmd.Result.communication_error;
        }

        started = true;

        remoteTimeDiffMSec = calculateTimeDiff(optLogger);

        return StartBusCmd.Result.started;
    }

    public synchronized StopBusCmd.Result StopBusCmd(Logger optLogger) {
        if (!started) {
            return StopBusCmd.Result.not_started;
        }

        final String logContext = (optLogger != null) ? this.getClass().getSimpleName() + ".StopBusCmd bmIdent:" + bmIdent + " " : "";

        // Try to stop the remote busmaster.
        String logLevelParam = computeLogLevelParam(optLogger);
        final String suffix = "stopBusCmd/" + bmIdent + ((logLevelParam != null) ? logLevelParam : "");

        StopBusCmdResult stopPostResult = (StopBusCmdResult) postURLDataWithAuthorization(suffix, StopBusCmdResult.class);

        if (stopPostResult.postError != null) {
            logErrorCommLevel(optLogger, logContext, " stop postError:" + stopPostResult.postError.name());
            return StopBusCmd.Result.communication_error;
        }

        if (stopPostResult.controllerError != null) {
            logErrorCommLevel(optLogger, logContext, " stop controllerError:" + stopPostResult.controllerError);
            return StopBusCmd.Result.communication_error;
        }

        StopBusCmd.Result tStopResult;
        try {
            tStopResult = Enum.valueOf(StopBusCmd.Result.class, stopPostResult.result);
        } catch (IllegalArgumentException e) {
            logErrorCommLevel(optLogger, logContext, " stop bad result enum:" + stopPostResult.result);
            return StopBusCmd.Result.communication_error;
        }

        if ((tStopResult != StopBusCmd.Result.not_started) &&
                (tStopResult != StopBusCmd.Result.stopped)) {
            logErrorCommLevel(optLogger, logContext, " stop result:" + tStopResult.name());
            return StopBusCmd.Result.communication_error;
        }

        started = false;

        return StopBusCmd.Result.stopped;
    }

    public long getRemoteTimeDiffMSec() {
        return remoteTimeDiffMSec;
    }

    public static ListBusMastersCmdResult ListBusMastersCmd(String endpoint, Logger optLogger) {
        final String logContext = (optLogger != null) ? Client.class.getSimpleName() + ".ListBusMastersCmd: " : "";

        // Try to list the remote busmasters.
        String logLevelParam = computeLogLevelParam(optLogger);
        final String suffix = "bmList/" + ((logLevelParam != null) ? logLevelParam : "");

        ListBusMastersCmdResult listBMSPostResult = (ListBusMastersCmdResult) postURLDataNoAuthorization(endpoint, suffix, ListBusMastersCmdResult.class);

        if (listBMSPostResult.postError != null) {
            logErrorCommLevel(optLogger, logContext, " postError:" + listBMSPostResult.postError.name());
        } else if (listBMSPostResult.controllerError != null) {
            logErrorCommLevel(optLogger, logContext, " controllerError:" + listBMSPostResult.controllerError);
        }

        return listBMSPostResult;
    }

    private long calculateTimeDiff(Logger optLogger) {
        final String logContext = (optLogger != null) ? Client.class.getSimpleName() + ".calculateTimeDiff: " : "";
        ArrayList<TimeDiffResult> results = new ArrayList<TimeDiffResult>();

        for (int i = 0; i < 5; i++) {
            final String suffix = "timeDiff/" + System.currentTimeMillis();

            TimeDiffResult result = (TimeDiffResult) postURLDataWithAuthorization(suffix, TimeDiffResult.class);

            long clientReceivedTimeMSec = System.currentTimeMillis();

            if (result.postError != null) {
                logErrorCommLevel(optLogger, logContext, " postError:" + result.postError.name());
            } else if (result.controllerError != null) {
                logErrorCommLevel(optLogger, logContext, " controllerError:" + result.controllerError);
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

        logErrorCommLevel(optLogger, logContext, " minDiff:" + minDiff);
        return minDiff;
    }

    public static BaseCmdResult postURLDataNoAuthorization(String endpoint, String suffix, Class respClass) {
        return postURLData(endpoint, suffix, null, respClass);
    }

    public BaseCmdResult postURLDataNoAuthorization(String suffix, Class respClass) {
        return postURLData(endpoint, suffix, null, respClass);
    }

    public BaseCmdResult postURLDataWithAuthorization(String suffix, Class respClass) {
        return postURLData(endpoint, suffix, authorization, respClass);
    }

    private static BaseCmdResult postURLData(final String endpoint, String suffix, String authHeader, Class respClass) {
        int connectionTimeoutMSec = 10000;

        HttpClient httpClient = new HttpClient();
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(connectionTimeoutMSec);
        httpClient.getHttpConnectionManager().getParams().setSoTimeout(connectionTimeoutMSec);

        PostMethod httppost = new PostMethod("http://" + endpoint + "/httpbusmaster/" + suffix);

        if (authHeader != null) {
            httppost.addRequestHeader(HttpHeaders.AUTHORIZATION, authHeader);
        }

        try {
            // Execute request
            int result = httpClient.executeMethod(httppost);

            if (result == HttpStatus.SC_METHOD_NOT_ALLOWED) {
                return (BaseCmdResult) respClass.cast(createPostErrorCmdResult(respClass, BaseCmdResult.PostErrors.MethodNotAllowed));
            }

            if (result == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                return (BaseCmdResult) respClass.cast(createPostErrorCmdResult(respClass, BaseCmdResult.PostErrors.ServerError));
            }

            if (result != HttpStatus.SC_OK) {
                System.err.println("Status:" + result);
                return (BaseCmdResult) respClass.cast(createPostErrorCmdResult(respClass, BaseCmdResult.PostErrors.UnknownError));
            }

            InputStream stream = httppost.getResponseBodyAsStream();

            return (BaseCmdResult) new ObjectMapper().readValue(httppost.getResponseBodyAsStream(), respClass);

        } catch (ConnectException e) {
            return (BaseCmdResult) respClass.cast(createPostErrorCmdResult(respClass, BaseCmdResult.PostErrors.ConnectException));
        } catch (SocketTimeoutException e) {
            return (BaseCmdResult) respClass.cast(createPostErrorCmdResult(respClass, BaseCmdResult.PostErrors.ReadTimeout));
        } catch (IOException e) {
            System.out.println(e);
            return (BaseCmdResult) respClass.cast(createPostErrorCmdResult(respClass, BaseCmdResult.PostErrors.UnknownError));
        } finally {
            if (httppost != null) {
                httppost.releaseConnection();
            }
        }
    }

    private static Object createPostErrorCmdResult(Class clz, BaseCmdResult.PostErrors pe) {
        try {
            return clz.getConstructor(BaseCmdResult.PostErrors.class).newInstance(pe);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        } catch (InstantiationException e) {
            return null;
        } catch (InvocationTargetException e) {
            return null;
        }
    }

    public static String computeLogLevelParam(Logger optLogger) {
        Logger.LogLevel logLevel = null;

        if ((optLogger == null) || ((logLevel = optLogger.getLogLevel()) == null) || (!logLevel.isAnyLevelSet())) {
            return null;
        }

        StringBuffer sb = new StringBuffer();

        int count = 0;
        sb.append("?logLevel=");
        if (logLevel.isLevelDevice()) {
            if (count > 0) {
                sb.append(",");
            }
            sb.append("device");
            count++;
        }
        if (logLevel.isLevelCmd()) {
            if (count > 0) {
                sb.append(",");
            }
            sb.append("cmd");
            count++;
        }
        if (logLevel.isLevelComm()) {
            if (count > 0) {
                sb.append(",");
            }
            sb.append("comm");
            count++;
        }

        return sb.toString();
    }

    private static void logErrorCommLevel(Logger optLogger, String context, String msg) {
        if ((optLogger != null) && (optLogger.getLogLevel().isLevelComm())) {
            optLogger.logError(context, msg);
        }
    }

}
