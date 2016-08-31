package waterfall.onewire.busmaster;

import java.util.List;

/**
 * Created by dwaterfa on 6/9/16.
 */
public abstract class SearchBusCmd extends BaseCmd {

    protected final Short familyCode;
    protected final Boolean byAlarm;

    protected Result result = null;
    protected List<String> resultList;
    protected long resultWriteCTM;

    /**
     * @return
     */
    public boolean isByAlarm() {
        return ((byAlarm != null) && (byAlarm));
    }

    /**
     * @return
     */
    public boolean isByFamilyCode() {
        return (familyCode != null);
    }

    /**
     *
     */
    public enum Result {
        busy,
        bus_not_started,
        communication_error,
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
            resultList = null;
            resultWriteCTM = 0;
        }

        if (!getBusMaster().getIsStarted()) {
            result = Result.bus_not_started;
        }
        else {
            try {
                logInfo("execute(" + (isByAlarm() ? "byAlarm" : (isByFamilyCode() ? Integer.toHexString(familyCode) : "")) + ")");
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
     * @return List of devices Addresses.
     * @throws NoResultException if the current result is not success.
     */
    public List<String> getResultList() throws NoResultException, NoResultDataException {
        if (result != Result.success) {
            throw new NoResultDataException();
        }
        if (result != Result.success) {
            throw new NoResultDataException();
        }

        return resultList;
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
     * Protected Constructors and Methods
     */
    protected SearchBusCmd(BusMaster busMaster, boolean byAlarm, boolean log) {
        super(busMaster, log);
        this.familyCode = null;
        if (byAlarm) {
            this.byAlarm = new Boolean(byAlarm);
        } else {
            this.byAlarm = null;
        }
    }

    /**
     * @param busMaster
     * @param familyCode
     */
    protected SearchBusCmd(BusMaster busMaster, short familyCode, boolean log) {
        super(busMaster, log);
        this.familyCode = new Short(familyCode);
        this.byAlarm = null;
    }

    /**
     * @return
     */
    protected abstract Result execute_internal();

}
