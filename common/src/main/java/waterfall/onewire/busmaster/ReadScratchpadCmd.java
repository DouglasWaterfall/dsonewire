package waterfall.onewire.busmaster;

import waterfall.onewire.DSAddress;

/**
 * Created by dwaterfa on 6/9/16. <p> ReadScratchPad BEh
 */
public abstract class ReadScratchpadCmd extends DeviceBaseCmd {

  /**
   *
   */
  public enum Result {
    /**
     * The cmd is busy performing the operation.
     */
    cmdBusy,

    /**
     * The cmd executed successfully.
     */
    success,

    /**
     * The bus has failed to perform the cmd. This may be because the bus is not started, or
     * it is in a fault state. Regardless the cmd did not execute.
     */
    busFault,

    /**
     * The device did not respond correctly and is believed to be in error.
     */
    deviceFault

  }

  protected short requestByteCount;

  protected Result result = null;
  protected byte[] resultData;
  protected byte[] resultHexData;

  /**
   * Protected Methods and Constructors
   */
  protected ReadScratchpadCmd(BusMaster busMaster, DSAddress dsAddr, short requestByteCount) {
    super(busMaster, dsAddr);
    assert (requestByteCount >= 1);
    this.requestByteCount = requestByteCount;
  }

  /**
   * @return The number of bytes requested from the scratchpad.
   */
  public short getRequestByteCount() {
    return requestByteCount;
  }

  /**
   * The method to call to start the command on the bus. It is acceptable to re-execute the same
   * command and the implementation must be responsible for re-initializing the result and
   * writeCTM.
   */
  public Result execute() {
    clearLog();

    synchronized (this) {
      if (result == Result.cmdBusy) {
        throw new NoResultException("busy");
      }

      result = Result.cmdBusy;
      resultData = null;
      resultHexData = null;
      resultWriteCTM = 0;
    }

    if (!getBusMaster().getIsStarted()) {
      result = Result.busFault;
    } else {
      try {
        logInfo("execute(dsAddr:" + getAddress().toString() + ")");
        result = execute_internal();
        logInfo("result:" + result.name());

      } catch (Exception e) {
        logError(e);
        result = Result.deviceFault;
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
    if ((result == null) || (result == Result.cmdBusy)) {
      throw new NoResultException();
    }
    if (result != Result.success) {
      throw new NoResultDataException();
    }

    return resultData;
  }

  /**
   * @return the bytes requested from the scratchpad encoded as two hex byte chars 0-9A-F.
   * @throws NoResultException if the current result is not done.
   */
  public byte[] getResultHexData() throws NoResultException, NoResultDataException {
    if ((result == null) || (result == Result.cmdBusy)) {
      throw new NoResultException();
    }
    if (result != Result.success) {
      throw new NoResultDataException();
    }

    return resultHexData;
  }

  /**
   * This the closest value for System.getCurrentTimeMillis() on the physical bus controlling the
   * device when the write for the bus command was executed.
   *
   * @return system time in milliseconds
   * @throws NoResultException if the current result is not done.
   */
  public long getResultWriteCTM() throws NoResultException {
    if ((result == null) || (result == Result.cmdBusy)) {
      throw new NoResultException();
    }
    if (result != Result.success) {
      throw new NoResultDataException();
    }

    return resultWriteCTM;
  }

  protected abstract Result execute_internal();

  protected abstract void setResultData(long resultWriteCTM, byte[] resultData,
      byte[] resultHexData);

}
