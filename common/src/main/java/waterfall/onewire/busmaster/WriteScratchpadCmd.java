package waterfall.onewire.busmaster;

import waterfall.onewire.DSAddress;

/**
 * Created by dwaterfa on 12/30/17.
 */
public abstract class WriteScratchpadCmd extends DeviceBaseCmd {

  protected Result result = null;
  protected byte[] writeData = null;

  protected WriteScratchpadCmd(BusMaster busMaster, DSAddress dsAddr, byte[] writeData) {
    super(busMaster, dsAddr);
    if ((writeData == null) || (writeData.length < 1)) {
      throw new IllegalArgumentException("writeData");
    }
    this.writeData = writeData;
  }

  /**
   * Update the data which will be written to the device.
   * @param writeData The data to write.
   */
  public void updateWriteData(byte[] writeData) {
    if ((writeData == null) || (writeData.length < 1)) {
      throw new IllegalArgumentException("writeData");
    }
    this.writeData = writeData;
  }

  /**
   * The method to call to start the command on the bus. It is acceptable to re-execute the same
   * command and the implementation must be responsible for re-initializing the result and
   * writeCTM.
   */
  public Result execute() {
    synchronized (this) {
      if (result == Result.cmdBusy) {
        throw new NoResultException("busy");
      }

      result = Result.cmdBusy;
      resultWriteCTM = 0;
    }

    try {
      result = execute_internal();

    } catch (Exception e) {
      e.printStackTrace();
      result = Result.deviceFault;
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
   * This the closest value for System.getCurrentTimeMillis() on the physical bus controlling the
   * device when the write for the ConvertT command was executed.
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

  /**
   * @return result of the execution - the same value as returned with getResult()
   */
  protected abstract Result execute_internal();

  /**
   * Set the result from the operation
   *
   * @param resultWriteCTM the time mark taken after the CR is written for the cmd byte
   */
  protected abstract void setResultData(long resultWriteCTM);

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
     * The bus has failed to perform the cmd. This may be because the bus is stopped, or it is
     * in a fault state. Regardless the cmd did not execute.
     */
    busFault,

    /**
     * The device did not respond correctly and is believed to be in error.
     */
    deviceFault
  }

}

