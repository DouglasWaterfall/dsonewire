package waterfall.onewire.busmaster;

import waterfall.onewire.DSAddress;

/**
 * Created by dwaterfa on 6/9/16.
 * <p>
 * ConvertT 44h
 */
public abstract class ConvertTCmd {

    protected BusMaster busMaster;
    protected DSAddress dsAddr;
    protected Logger optLogger;

    protected Result result = null;
    protected long resultWriteCTM;

    /**
     * The BusMaster the command is attached to
     */
    public BusMaster getBusMaster() {
        return busMaster;
    }

    /**
     * @return The device address as a 8 character hex string.
     */
    public DSAddress getAddress() {
        return dsAddr;
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
        busy,
        bus_not_started,
        communication_error,
        device_not_found,
        device_error,
        success
    }

    /**
     * The method to call to start the command on the bus. It is acceptable to re-execute the same command and the
     * implementation must be responsible for re-initializing the result and writeCTM.
     */
    public Result execute() {
        synchronized (this) {
            if (result == Result.busy) {
                throw new NoResultException("busy");
            }

            result = Result.busy;
            resultWriteCTM = 0;
        }

        try {
            if (optLogger != null) {
                optLogger.pushLevel(this.getClass().getSimpleName() + ".execute() ");
                optLogger.debug("dsAddr:" + getAddress().toString());
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
     * The result of the Cmd. May be null if the command has not been started or completed.
     *
     * @return Result
     */
    public Result getResult() {
        return result;
    }

    /**
     * This the closest value for System.getCurrentTimeMillis() on the physical bus controlling the device when
     * the write for the bus command was executed.
     *
     * @return system time in milliseconds
     * @throws NoResultException if the current result is not done.
     */
    public long getResultWriteCTM() throws NoResultException {
        if ((result == null) || (result == Result.busy)) {
            throw new NoResultException();
        }

        return resultWriteCTM;
    }

    /**
     * Protected Methods and Constructors
     */
    protected ConvertTCmd(BusMaster busMaster, DSAddress dsAddr, Logger optLogger) {
        assert (busMaster != null);
        assert (dsAddr != null);
        this.busMaster = busMaster;
        this.dsAddr = dsAddr;
        this.optLogger = optLogger;
    }

    protected abstract Result execute_internal();

}

