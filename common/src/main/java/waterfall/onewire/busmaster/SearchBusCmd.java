package waterfall.onewire.busmaster;

import java.util.List;
import java.util.zip.CRC32;
import waterfall.onewire.DSAddress;

/**
 * Created by dwaterfa on 6/9/16.
 */
public abstract class SearchBusCmd extends BaseCmd {

  protected final Short familyCode;
  protected final Boolean byAlarm;

  protected Result result = null;
  protected ResultData resultData;

  /**
   * Protected Constructors and Methods
   */
  protected SearchBusCmd(BusMaster busMaster, boolean byAlarm) {
    super(busMaster);
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
  protected SearchBusCmd(BusMaster busMaster, short familyCode) {
    super(busMaster);
    if ((familyCode < 0) || (familyCode > 255)) {
      throw new IllegalArgumentException("familyCode");
    }
    this.familyCode = new Short(familyCode);
    this.byAlarm = null;
  }

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
   * The method to call to start the command on the bus. It is acceptable to re-execute the same
   * command and the implementation must be responsible for re-initializing the result and
   * writeCTM.
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

    try {
      logInfo("execute(" + (isByAlarm() ? "byAlarm"
          : (isByFamilyCode() ? Integer.toHexString(familyCode) : "")) + ")");
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
   * @return result of the successful operation
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
   * Protected Methods and Constructors
   */
  protected abstract void setResultData(ResultData resultData);

  /**
   * @return List of devices Addresses.
   * @throws NoResultException if the current result is not success.
   */
  public List<DSAddress> getResultList() throws NoResultDataException {
    return getResultData().getList();
  }

  /**
   * @return CRC32 of the list of device addresses
   */
  public long getResultListCRC32() throws NoResultDataException {
    return getResultData().getListCRC32();
  }

  /**
   * @return system time in milliseconds when the first write to the physical bus for the operation
   * started.
   * @throws NoResultException if the current result is not done.
   */
  public long getResultWriteCTM() throws NoResultException {
    return getResultData().getWriteCTM();
  }

  protected abstract Result execute_internal();

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
   * Class to effectively contain the results of the operation when the result is success.
   */
  public static class ResultData {

    private final List<DSAddress> list;
    private final long listCRC32;
    private final long writeCTM;

    public ResultData(final List<DSAddress> list, final long writeCTM) {
      this.list = list;
      if (list == null) {
        throw new IllegalArgumentException("list");
      }
      if (writeCTM <= 0) {
        throw new IllegalArgumentException("writeCTM");
      }
      this.writeCTM = writeCTM;
      CRC32 crc = new CRC32();
      byte[] b = new byte[8];
      for (DSAddress dsAddress : list) {
        crc.update(dsAddress.copyRawBytesTo(b, 0));
      }
      this.listCRC32 = crc.getValue();
    }

    /**
     * Return list of devices Addresses.
     */
    public List<DSAddress> getList() {
      return list;
    }

    /**
     * Return CRC32 calculation on the list of devices Addresses.
     */
    public long getListCRC32() {
      return listCRC32;
    }

    /**
     * This the closest value for System.getCurrentTimeMillis() on the physical bus controlling the
     * device when the write for the bus command was executed.
     */
    public long getWriteCTM() {
      return writeCTM;
    }

  }

}
