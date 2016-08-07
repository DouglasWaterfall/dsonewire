package waterfall.onewire.busmaster;

import waterfall.onewire.DSAddress;

/**
 * Created by dwaterfa on 6/9/16.
 * <p>
 * ReadPowerSupply B4h
 */
public abstract class ReadPowerSupplyCmd extends DeviceBaseCmd {

    protected Result result = null;
    protected boolean resultIsParasitic;
    protected long resultWriteCTM;

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
            resultIsParasitic = false;
            resultWriteCTM = 0;
        }

        try {
            logInfo("execute(dsAddr:" + getAddress().toString() + ")");
            result = execute_internal();
            logInfo("result:" + result.name());

        } catch (Exception e) {
            logError(e);
            result = Result.communication_error;
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
     * Returns true if the device is powered, false if parasitic.
     *
     * @throws NoResultException if the current result is not success.
     */
    public boolean getResultIsParasitic() throws NoResultException, NoResultDataException {
        if ((result == null) || (result == Result.busy)) {
            throw new NoResultException();
        }
        if (result != Result.success) {
            throw new NoResultDataException();
        }

        return resultIsParasitic;
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
    protected ReadPowerSupplyCmd(BusMaster busMaster, DSAddress dsAddr, boolean log) {
        super(busMaster, dsAddr, log);
    }

    protected abstract void setResultIsParasitic(boolean isParasitic);

    public abstract void setResultWriteCTM(long resultWriteCTM);

    protected abstract Result execute_internal();

}
