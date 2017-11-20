package waterfall.onewire.busmaster;

import waterfall.onewire.DSAddress;

/**
 * Created by dwaterfa on 7/24/16.
 */
public abstract class DeviceBaseCmd extends BaseCmd {

  protected DSAddress dsAddr;
  /**
   * This the closest value for System.getCurrentTimeMillis() on the physical bus controlling the
   * device when the write for the bus command was executed.
   */
  protected long resultWriteCTM;

  /**
   * Protected Methods and Constructors
   */
  protected DeviceBaseCmd(BusMaster busMaster, DSAddress dsAddr) {
    super(busMaster);
    if (dsAddr == null) {
      throw new IllegalArgumentException("dsAddr");
    }
    this.dsAddr = dsAddr;
  }

  /**
   * @return The device address as a 8 character hex string.
   */
  public DSAddress getAddress() {
    return dsAddr;
  }
}
