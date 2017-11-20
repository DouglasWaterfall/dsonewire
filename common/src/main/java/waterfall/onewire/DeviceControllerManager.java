package waterfall.onewire;

/**
 * Created by dwaterfa on 10/9/17.
 *
 * This class manages the lifetime of a particular type of Device Controller.
 */
public interface DeviceControllerManager {

  DeviceController getDevice();

}
