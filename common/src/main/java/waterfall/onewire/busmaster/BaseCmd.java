package waterfall.onewire.busmaster;

/**
 * Created by dwaterfa on 7/7/16.
 */
public class BaseCmd {

  protected final BusMaster busMaster;

  public BaseCmd(BusMaster busMaster) {
    if (busMaster == null) {
      throw new IllegalArgumentException("busMaster");
    }
    this.busMaster = busMaster;
  }

  /**
   * The BusMaster the command is attached to
   */
  public BusMaster getBusMaster() {
    return busMaster;
  }

}

