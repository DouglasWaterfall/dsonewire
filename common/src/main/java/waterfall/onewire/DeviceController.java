package waterfall.onewire;

/**
 * Created by dwaterfa on 10/9/17.
 */
public interface DeviceController {

  /**
   *
   */
  int STATE_UNINITIALIZED = 0;

  /**
   *
   */
  int STATE_RUNNING = 1;

  /**
   *
   */
  int STATE_ERROR = 2;

  /**
   * / int STATE_RECOVERY_FAILED = 3;
   *
   * /**
   */
  int STATE_DEVICE_MISSING = 4;

  /**
   * The
   */
  int getState();

  /**
   *
   */
  String getDescription();

  /**
   *
   */
  void initialize();

}
