package waterfall.onewire.busmaster;

public interface AlarmSearchBusCmdNotifyResult {

  /**
   * Notify of a successful SearchBusCmd.
   */
  public void notify(final BusMaster bm, final SearchBusCmd.ResultData searchResultData);


}

