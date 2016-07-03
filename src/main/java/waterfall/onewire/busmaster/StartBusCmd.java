package waterfall.onewire.busmaster;


/**
 * Created by dwaterfa on 6/9/16.
 */
public abstract class StartBusCmd {

    protected BusMaster busMaster;

    protected Result result = null;
    protected Logger optLogger;

    /**
     * The BusMaster the command is attached to
     */
    public BusMaster getBusMaster() {
        return busMaster;
    }

    /**
     *
     * @return
     */
    public Logger getOptLogger() {
        return optLogger;
    }

    /**
     *
     */
    public enum Result {
        already_started,
        busy,
        bus_not_found,
        communication_error,
        started
    }

    /**
     * The method to call to start the bus. It is acceptable to re-execute the same command and the
     * implementation must be responsible for re-initializing the result.
     */
    public Result execute() {
        synchronized (this) {
            if (result == Result.busy) {
                throw new NoResultException("busy");
            }

            result = Result.busy;
        }

        try {
            if (optLogger != null) {
                optLogger.pushLevel(this.getClass().getSimpleName() + ".execute() ");
            }

            result = execute_internal();

            optLogger.debug("result:" + result.name());

        } catch (Exception e) {
            if (optLogger != null) {
                optLogger.error(e);
            }
            result = Result.communication_error;
        }
        finally {
            if (optLogger != null) {
                optLogger.popLevel();
            }
        }

        return result;
    }

    /**
     * The result of the StartCmd.
     *
     * @return Result
     */
    public Result getResult() {
        return result;
    }

    /**
     *
     * @param busMaster
     * @param optLogger
     */
    protected StartBusCmd(BusMaster busMaster, Logger optLogger) {
        this.busMaster = busMaster;
        this.optLogger = optLogger;
    }

    /**
     *
     * @return
     */
    protected abstract Result execute_internal();

}
