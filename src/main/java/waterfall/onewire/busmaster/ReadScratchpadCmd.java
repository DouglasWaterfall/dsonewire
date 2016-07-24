package waterfall.onewire.busmaster;

import waterfall.onewire.DSAddress;

/**
 * Created by dwaterfa on 6/9/16.
 * <p>
 * ReadScratchPad BEh
 */
public abstract class ReadScratchpadCmd extends BaseCmd {

    protected DSAddress dsAddr;
    protected short requestByteCount;

    protected Result result = null;
    protected long resultWriteCTM;
    protected byte[] resultData;

    /**
     * @return The device address as a 8 character hex string.
     */
    public DSAddress getAddress() {
        return dsAddr;
    }

    /**
     * @return The number of bytes requested from the scratchpad.
     */
    public short getRequestCount() {
        return requestByteCount;
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
            resultData = null;
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
     * @return the bytes requested from the scratchpad.
     * @throws NoResultException if the current result is not done.
     */
    public byte[] getResultData() throws NoResultException {
        if ((result == null) || (result == Result.busy)) {
            throw new NoResultException();
        }

        return resultData;
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
    protected ReadScratchpadCmd(BusMaster busMaster, DSAddress dsAddr, short requestByteCount, boolean log) {
        super(busMaster, log);
        assert (dsAddr != null);
        assert (requestByteCount >= 1);
        this.dsAddr = dsAddr;
        this.requestByteCount = requestByteCount;
    }

    protected abstract Result execute_internal();

}
