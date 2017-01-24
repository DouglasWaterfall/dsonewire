package waterfall.onewire.busmasters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.springframework.http.HttpHeaders;
import waterfall.onewire.HttpClient.BaseCmdPostResult;
import waterfall.onewire.HttpClient.BaseCmdResult;
import waterfall.onewire.HttpClient.PostErrors;
import waterfall.onewire.busmaster.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

/**
 * Created by dwaterfa on 1/20/17.
 */
public class Util {

    public static BaseCmdPostResult postURLDataNoAuthorization(String endpoint, String suffix, Class respClass) {
        int connectionTimeoutMSec = 10000;
        return Util.postURLData(endpoint, suffix, null, connectionTimeoutMSec, null, respClass);
    }

    public static BaseCmdPostResult postURLData(final String endpoint, String suffix, String authHeader, int connectionTimeoutMSec, Object content, Class respClass) {

        HttpClient httpClient = new HttpClient();
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(connectionTimeoutMSec);
        httpClient.getHttpConnectionManager().getParams().setSoTimeout(connectionTimeoutMSec);

        PostMethod httppost = new PostMethod("http://" + endpoint + "/httpbusmaster/" + suffix);

        if (authHeader != null) {
            httppost.addRequestHeader(HttpHeaders.AUTHORIZATION, authHeader);
        }

        ObjectMapper objectMapper = new ObjectMapper();

        if (content != null) {
            String stringContent = null;
            try {
                stringContent = objectMapper.writeValueAsString(content);
            }
            catch (JsonProcessingException e) {
                return (BaseCmdPostResult) respClass.cast(createPostErrorCmdResult(respClass, PostErrors.ParseException));
            }
            httppost.setRequestBody(stringContent);
        }

        try {
            // Execute request
            int result = httpClient.executeMethod(httppost);

            if (result == HttpStatus.SC_METHOD_NOT_ALLOWED) {
                return (BaseCmdPostResult) respClass.cast(createPostErrorCmdResult(respClass, PostErrors.MethodNotAllowed));
            }

            if (result == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                return (BaseCmdPostResult) respClass.cast(createPostErrorCmdResult(respClass, PostErrors.ServerError));
            }

            if (result != HttpStatus.SC_OK) {
                System.err.println("Status:" + result);
                return (BaseCmdPostResult) respClass.cast(createPostErrorCmdResult(respClass, PostErrors.UnknownError));
            }

            InputStream stream = httppost.getResponseBodyAsStream();

            return (BaseCmdPostResult) objectMapper.readValue(httppost.getResponseBodyAsStream(), respClass);

        } catch (ConnectException e) {
            return (BaseCmdPostResult) respClass.cast(createPostErrorCmdResult(respClass, PostErrors.ConnectException));
        } catch (SocketTimeoutException e) {
            return (BaseCmdPostResult) respClass.cast(createPostErrorCmdResult(respClass, PostErrors.ReadTimeout));
        } catch (IOException e) {
            System.out.println(e);
            return (BaseCmdPostResult) respClass.cast(createPostErrorCmdResult(respClass, PostErrors.UnknownError));
        } finally {
            if (httppost != null) {
                httppost.releaseConnection();
            }
        }
    }

    private static Object createPostErrorCmdResult(Class clz, PostErrors pe) {
        try {
            return clz.getConstructor(PostErrors.class).newInstance(pe);
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

        return computeLogLevelParam(logLevel);
    }

    public static String computeLogLevelParam(Logger.LogLevel logLevel) {
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

    public static void logErrorCommLevel(Logger optLogger, String context, String msg) {
        if ((optLogger != null) && (optLogger.getLogLevel().isLevelComm())) {
            optLogger.logError(context, msg);
        }
    }

}
