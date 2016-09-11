
package waterfall.onewire.busmaster;

/**
 * Created by dwaterfa on 6/9/16.
 */
public abstract class StopBusCmd extends BaseCmd {

    protected Result result = null;

    /**
     *
     */
    public enum Result {
        not_started,
        busy,
        communication_error,
        stopped
    };

    /**
     * The method to call to stop the bus. It is acceptable to re-execute the same command and the
     * implementation must be responsible for re-initializing the result.
     */
    public Result execute() {
        clearLog();

        synchronized (this) {
            if (result == Result.busy) {
                throw new NoResultException("busy");
            }

            result = Result.busy;
        }

        if (!getBusMaster().getIsStarted()) {
            result = Result.stopped;
        }
        else {
            try {
                logInfo("execute()");
                result = execute_internal();
                logInfo("result:" + result.name());

            } catch (Exception e) {
                logError(e);
                result = Result.communication_error;
            }
        }

        return result;
    }

    /**
     * The result of the StopBusCmd.
     *
     * @return Result
     */
    public Result getResult() {
        return result;
    }

    /**
     *
     * @param busMaster
     * @param logLevel
     */
    protected StopBusCmd(BusMaster busMaster, LogLevel logLevel) {
        super(busMaster, logLevel);
    }

    /**
     *
     * @return
     */
    protected abstract Result execute_internal();

}
