package waterfall.onewire.busmaster;

import waterfall.onewire.DSAddress;

/**
 * Created by dwaterfa on 6/9/16.
 * <p>
 * ReadScratchPad BEh
 */
public abstract class ReadScratchpadCmd extends DeviceBaseCmd {

    protected short requestByteCount;

    protected Result result = null;
    protected byte[] resultData;
    protected byte[] resultHexData;

    /**
     * @return The number of bytes requested from the scratchpad.
     */
    public short getRequestByteCount() {
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
        clearLog();

        synchronized (this) {
            if (result == Result.busy) {
                throw new NoResultException("busy");
            }

            result = Result.busy;
            resultData = null;
            resultHexData = null;
            resultWriteCTM = 0;
        }

        if (!getBusMaster().getIsStarted()) {
            result = Result.bus_not_started;
        }
        else {
            try {
                logInfo("execute(dsAddr:" + getAddress().toString() + ")");
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
     * @return the bytes requested from the scratchpad encoded as two hex byte chars 0-9A-F.
     * @throws NoResultException if the current result is not done.
     */
    public byte[] getResultHexData() throws NoResultException, NoResultDataException {
        if ((result == null) || (result == Result.busy)) {
            throw new NoResultException();
        }
        if (result != Result.success) {
            throw new NoResultDataException();
        }

        return resultHexData;
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
    protected ReadScratchpadCmd(BusMaster busMaster, DSAddress dsAddr, short requestByteCount, LogLevel logLevel) {
        super(busMaster, dsAddr, logLevel);
        assert (requestByteCount >= 1);
        this.requestByteCount = requestByteCount;
    }

    protected abstract Result execute_internal();

    protected abstract void setResultData(long resultWriteCTM, byte[] resultData, byte[] resultHexData);

}
