package waterfall.onewire.HttpClient;

/**
 * Created by dwaterfa on 1/14/17.
 */
public enum PostErrors {
    ConnectException,
    MethodNotAllowed,
    ReadTimeout,
    ParseException,
    ServerError, // 500
    UnknownError;

}
