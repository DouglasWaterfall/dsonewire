package waterfall.onewire.busmaster;


/**
 * Created by dwaterfa on 6/9/16.
 */
public abstract class StartBusCmd extends BaseCmd {

  protected Result result = null;

  /**
   * @param busMaster
   */
  protected StartBusCmd(BusMaster busMaster) {
    super(busMaster);
  }

  /**
   * The method to call to start the bus. It is acceptable to re-execute the same command and the
   * implementation must be responsible for re-initializing the result.
   */
  public Result execute() {
    clearLog();

    synchronized (this) {
      if (result == Result.busy) {
        throw new NoResultException("busy");
      }

      result = Result.busy;
    }

    try {
      logInfo("execute()");
      result = execute_internal();
      logInfo("result:" + result.name());

    } catch (Exception e) {
      logError(e);
      result = Result.communication_error;
    }

    return result;
  }

  /**
   * The result of the StartCmd.
   *
   * @return Result
   */
  public Result getResult() {
    return result;
  }

  /**
   * @return
   */
  protected abstract Result execute_internal();

  /**
   *
   */
  public enum Result {
    already_started,
    busy,
    bus_not_found,
    communication_error,
    started
  }

}
