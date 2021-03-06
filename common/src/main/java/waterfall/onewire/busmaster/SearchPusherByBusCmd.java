package waterfall.onewire.busmaster;

/**
 * This class implements the SearchPusher through a Timer and a call to a conventional SearchBusCmd
 * on the BusMaster.
 */
public class SearchPusherByBusCmd extends SearchPusher {

  private final BusMaster bm;
  private final boolean isAlarmSearch;

  private Thread searchBusCmdThread;
  private SearchBusCmd searchBusCmd;

  public SearchPusherByBusCmd(BusMaster bm, boolean isAlarmSearch) {
    super(isAlarmSearch);
    this.bm = bm;
    this.isAlarmSearch = isAlarmSearch;
    this.searchBusCmdThread = null;
    this.searchBusCmd = null;
  }

  @Override
  protected void push() {
    synchronized (this) {
      if (searchBusCmdThread != null) {
        return;
      }
      searchBusCmdThread = Thread.currentThread();
    }

    // now that ourselves are marked as the search thread, we can get on with it
    try {
      if (searchBusCmd == null) {
        searchBusCmd = (isAlarmSearch ? bm.querySearchBusByAlarmCmd() : bm.querySearchBusCmd());
      }

      // The command will internally call back to this class when it is successful, so
      // all we need to do here is just push.
      searchBusCmd.execute();

    } finally {
      synchronized (this) {
        searchBusCmdThread = null;
        if (!isTimerActive()) {
          searchBusCmd = null;
        }
      }
    }
  }

}
