package waterfall.onewire.busmaster;

import waterfall.onewire.DSAddress;

/**
 * Created by dwaterfa on 6/9/16. <p> ConvertT 44h
 */
public abstract class ConvertTCmd extends DeviceBaseCmd {

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
     * The device did not respond and is believed to not be present.
     */
    deviceNotFound,

    /**
     * The device did not respond correctly and is believed to be in error.
     */
    deviceFault

  }

  protected Result result = null;

  /**
   * Protected Methods and Constructors
   */
  protected ConvertTCmd(BusMaster busMaster, DSAddress dsAddr) {
    super(busMaster, dsAddr);
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

    return resultWriteCTM;
  }

  /**
   *
   * @return result of the execution - the same value as returned with getResult()
   */
  protected abstract Result execute_internal();

  /**
   * Set the result from the operation
   * @param resultWriteCTM the time mark taken after the CR is written for the cmd byte
   */
  protected abstract void setResultData(long resultWriteCTM);

}

