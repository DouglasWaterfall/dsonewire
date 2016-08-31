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

/**
 * Created by dwaterfa on 2/15/16.
 */
public class Client implements BusMaster {
    private final String USER_AGENT = "waterfall waterfall.onewire.busmaster.HA7S.HTTP;1.0";

    private final String endpoint;
    private final String bmIdent;

    private boolean started;

    private String bmName;
    private String authorization;

    public Client(String endpoint, String bmIdent) {
        this.endpoint = endpoint;
        this.bmIdent = bmIdent;
        this.started = false;
        this.bmName = null;
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
        return 0;
    }

    public boolean getIsStarted() {
        return started;
    }

    public StartBusCmd queryStartBusCmd(boolean doLog) {
        return new HttpStartBusCmd(this, doLog);
    }

    public StopBusCmd queryStopBusCmd(boolean doLog) {
        return new HttpStopBusCmd(this, doLog);
    }

    public SearchBusCmd querySearchBusCmd(boolean doLog) {
        return new HttpSearchBusCmd(this, false, doLog);
    }

    public SearchBusCmd querySearchBusByFamilyCmd(short familyCode, boolean doLog) {
        return new HttpSearchBusCmd(this, familyCode, doLog);
    }

    public SearchBusCmd querySearchBusByAlarmCmd(boolean doLog) {
        return new HttpSearchBusCmd(this, true, doLog);
    }

    public ConvertTCmd queryConvertTCmd(DSAddress dsAddr, boolean doLog) {
        return new HttpConvertTCmd(this, dsAddr, doLog);
    }

    public ReadPowerSupplyCmd queryReadPowerSupplyCmd(DSAddress dsAddr, boolean doLog) {
        return new HttpReadPowerSupplyCmd(this, dsAddr, doLog);
    }

    public ReadScratchpadCmd queryReadScratchpadCmd(DSAddress dsAddr, short requestByteCount, boolean doLog) {
        return new HttpReadScratchpadCmd(this, dsAddr, requestByteCount, doLog);
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

            if (optLogger != null) {
                optLogger.logError(logContext, " status postError:" + statusPostResult.postError.name());
            }

            return StartBusCmd.Result.communication_error;
        }

        if (statusPostResult.controllerError != null) {

            if (optLogger != null) {
                optLogger.logError(logContext, " status controllerError:" + statusPostResult.controllerError);
            }

            return StartBusCmd.Result.communication_error;
        }

        bmName = statusPostResult.name;

        authorization = statusPostResult.authorization;

        if (optLogger != null) {
            optLogger.logError(logContext, " status name:" + bmName + " authorization:" + authorization);
        }

        if (statusPostResult.started) {
            if (optLogger != null) {
                optLogger.logError(logContext, " status already started");
            }
            started = true;
            return StartBusCmd.Result.started;
        }

        // Try to start the remote busmaster.
        final String startSuffix = "startBusCmd/" + bmIdent + ((optLogger != null) ? "?log=true" : "");

        StartBusCmdResult startPostResult = (StartBusCmdResult) postURLDataWithAuthorization(startSuffix, StartBusCmdResult.class);

        if (startPostResult.postError != null) {
            if (optLogger != null) {
                optLogger.logError(logContext, " start postError:" + startPostResult.postError.name());
            }
            return StartBusCmd.Result.bus_not_found;
        }

        if (startPostResult.controllerError != null) {
            if (optLogger != null) {
                optLogger.logError(logContext, " start controllerError:" + startPostResult.controllerError);
            }
            return StartBusCmd.Result.communication_error;
        }

        StartBusCmd.Result tStartResult;
        try {
            tStartResult = Enum.valueOf(StartBusCmd.Result.class, startPostResult.result);
        } catch (IllegalArgumentException e) {
            if (optLogger != null) {
                optLogger.logError(logContext, " start bad result enum:" + startPostResult.result);
            }
            return StartBusCmd.Result.communication_error;
        }

        if ((tStartResult != StartBusCmd.Result.already_started) &&
                (tStartResult != StartBusCmd.Result.started)) {
            if (optLogger != null) {
                optLogger.logError(logContext, " start result:" + tStartResult.name());
            }
            return StartBusCmd.Result.communication_error;
        }

        started = true;

        return StartBusCmd.Result.started;
    }

    public synchronized StopBusCmd.Result StopBusCmd(Logger optLogger) {
        if (!started) {
            return StopBusCmd.Result.not_started;
        }
        
        final String logContext = (optLogger != null) ? this.getClass().getSimpleName() + ".StopBusCmd bmIdent:" + bmIdent + " " : "";

        // Try to stop the remote busmaster.
        final String suffix = "stopBusCmd/" + bmIdent + ((optLogger != null) ? "?log=true" : "");

        StopBusCmdResult stopPostResult = (StopBusCmdResult) postURLDataWithAuthorization(suffix, StopBusCmdResult.class);

        if (stopPostResult.postError != null) {
            if (optLogger != null) {
                optLogger.logError(logContext, " stop postError:" + stopPostResult.postError.name());
            }
            return StopBusCmd.Result.communication_error;
        }

        if (stopPostResult.controllerError != null) {
            if (optLogger != null) {
                optLogger.logError(logContext, " stop controllerError:" + stopPostResult.controllerError);
            }
            return StopBusCmd.Result.communication_error;
        }

        StopBusCmd.Result tStopResult;
        try {
            tStopResult = Enum.valueOf(StopBusCmd.Result.class, stopPostResult.result);
        } catch (IllegalArgumentException e) {
            if (optLogger != null) {
                optLogger.logError(logContext, " stop bad result enum:" + stopPostResult.result);
            }
            return StopBusCmd.Result.communication_error;
        }

        if ((tStopResult != StopBusCmd.Result.not_started) &&
                (tStopResult != StopBusCmd.Result.stopped)) {
            if (optLogger != null) {
                optLogger.logError(logContext, " stop result:" + tStopResult.name());
            }
            return StopBusCmd.Result.communication_error;
        }

        started = false;

        return StopBusCmd.Result.stopped;
    }

    public static ListBusMastersCmdResult ListBusMastersCmd(String endpoint, Logger optLogger) {
        final String logContext = (optLogger != null) ? Client.class.getSimpleName() + ".ListBusMastersCmd: " : "";

        // Try to list the remote busmasters.
        final String suffix = "bmList/" + ((optLogger != null) ? "?log=true" : "");

        ListBusMastersCmdResult listBMSPostResult = (ListBusMastersCmdResult) postURLDataNoAuthorization(endpoint, suffix, ListBusMastersCmdResult.class);

        if (listBMSPostResult.postError != null) {
            if (optLogger != null) {
                optLogger.logError(logContext, " listBMS postError:" + listBMSPostResult.postError.name());
            }
        }
        else if (listBMSPostResult.controllerError != null) {
            if (optLogger != null) {
                optLogger.logError(logContext, " listBMS controllerError:" + listBMSPostResult.controllerError);
            }
        }

        return listBMSPostResult;
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

}
