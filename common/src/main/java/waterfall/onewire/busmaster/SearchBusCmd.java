package waterfall.onewire.busmaster;

import java.util.List;
import java.util.zip.CRC32;

/**
 * Created by dwaterfa on 6/9/16.
 */
public abstract class SearchBusCmd extends BaseCmd {

    protected final Short familyCode;
    protected final Boolean byAlarm;

    protected Result result = null;
    protected ResultData resultData;

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
            resultData = null;
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

        if (result == Result.success) {
            getBusMaster().searchBusCmdExecuteCallback(this);
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
     * Class to effectively contain the results of the operation when the result is success.
     */
    public class ResultData {
        private final List<String> list;
        private long listCRC32;
        private final long writeCTM;

        public ResultData(final List<String> list, final long writeCTM) {
            this.list = list;
            if (list == null) {
                throw new IllegalArgumentException("list");
            }
            if (writeCTM <= 0) {
                throw new IllegalArgumentException("writeCTM");
            }
            this.writeCTM = writeCTM;
            CRC32 crc = new CRC32();
            for(String string : list) {
                crc.update(string.getBytes());
            }
            this.listCRC32 = crc.getValue();
        }

        /**
         * Return list of devices Addresses.
         */
        public List<String> getList() { return list; }

        /**
         * Return CRC32 calcualation on the list of devices Addresses.
         */
        public long getListCRC32() { return listCRC32; }

        /**
         * This the closest value for System.getCurrentTimeMillis() on the physical bus controlling the device when
         * the write for the bus command was executed.
         */
        public long getWriteCTM() { return writeCTM; }

    }

    /**
     *
     * @return result of the successful operation
     * @throws NoResultDataException
     */
    public ResultData getResultData() throws NoResultDataException {
        if ((result == null) || (result == Result.busy)) {
            throw new NoResultException();
        }
        if (result != Result.success) {
            throw new NoResultDataException();
        }
        return resultData;
    }

    /**
     * @return List of devices Addresses.
     * @throws NoResultException if the current result is not success.
     */
    public List<String> getResultList() throws NoResultDataException {
        return getResultData().getList();
    }

    /**
     *
     * @return CRC32 of the list of device addresses
     * @throws NoResultDataException
     */
    public long getResultListCRC32() throws NoResultDataException {
        return getResultData().getListCRC32();
    }

    /**
     * @return system time in milliseconds when the first write to the physical bus for the operation started.
     * @throws NoResultException if the current result is not done.
     */
    public long getResultWriteCTM() throws NoResultException {
        return getResultData().getWriteCTM();
    }

    /**
     * Protected Constructors and Methods
     */
    protected SearchBusCmd(BusMaster busMaster, boolean byAlarm, LogLevel logLevel) {
        super(busMaster, logLevel);
        this.familyCode = null;
        if (byAlarm) {
            this.byAlarm = new Boolean(byAlarm);
        } else {
            this.byAlarm = null;
        }
    }

    /**
     * Protected Methods and Constructors
     */

    /**
     * @param busMaster
     * @param familyCode
     */
    protected SearchBusCmd(BusMaster busMaster, short familyCode, LogLevel logLevel) {
        super(busMaster, logLevel);
        if ((familyCode < 0) || (familyCode > 255)) {
            throw new IllegalArgumentException("familyCode");
        }
        this.familyCode = new Short(familyCode);
        this.byAlarm = null;
    }

    protected abstract Result execute_internal();

    protected abstract void setResultData(long resultWriteCTM, List<String> resultList);

}
