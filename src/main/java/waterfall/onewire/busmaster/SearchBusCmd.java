package waterfall.onewire.busmaster;

import java.util.List;

/**
 * Created by dwaterfa on 6/9/16.
 */
public abstract class SearchBusCmd {

    protected BusMaster busMaster;
    protected final Short familyCode;
    protected final Boolean byAlarm;

    protected Result result = null;
    protected List<String> resultList;
    protected long resultWriteCTM;
    protected Logger optLogger;

    /**
     * The BusMaster the command is attached to
     */
    public BusMaster getBusMaster() {
        return busMaster;
    }

    public boolean isByAlarm() {
        return ((byAlarm != null) && (byAlarm));
    }

    public boolean isByFamilyCode() {
        return (familyCode != null);
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
            resultList = null;
            resultWriteCTM = 0;
        }

        try {
            if (optLogger != null) {
                optLogger.pushLevel(this.getClass().getSimpleName() + "execute(" + (isByAlarm() ? "byAlarm" : (isByFamilyCode() ? Integer.toHexString(familyCode) : "")) + ") ");
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
     * @return List of devices Addresses.
     * @throws NoResultException if the current result is not success.
     */
    public List<String> getResultList() throws NoResultException {
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
    protected SearchBusCmd(BusMaster busMaster, boolean byAlarm, Logger optLogger) {
        this.busMaster = busMaster;
        this.familyCode = null;
        if (byAlarm) {
            this.byAlarm = new Boolean(byAlarm);
        } else {
            this.byAlarm = null;
        }
        this.optLogger = optLogger;
    }

    /**
     *
     * @param busMaster
     * @param familyCode
     * @param optLogger
     */
    protected SearchBusCmd(BusMaster busMaster, short familyCode, Logger optLogger) {
        this.busMaster = busMaster;
        this.familyCode = new Short(familyCode);
        this.byAlarm = null;
        this.optLogger = optLogger;
    }

    /**
     *
     * @return
     */
    protected abstract Result execute_internal();

}
