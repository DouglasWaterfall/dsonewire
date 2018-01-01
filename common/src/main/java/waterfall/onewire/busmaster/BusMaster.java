package waterfall.onewire.busmaster;

import waterfall.onewire.DSAddress;

/**
 * A BusMaster instance is assumed to always be started and ready for business. There is no
 * provision for stopping the bus via this interface, rather it needs to be managed through
 * whatever created the BusMaster instance.
 */
public interface BusMaster {

  /**
   * returns the human readable name of the bus
   *
   * @return human readable description of the bus
   */
  public String getName();

  /**
   * Get the current time in millis for the system the BusMaster is controlled by. This is useful
   * for comparing time which has elapsed on the bus. All methods which a return a Long in this
   * class return this same time source.
   *
   * @return the currentTimeMillis for the VM talking to the physical Bus.
   */
  public long getCurrentTimeMillis();

  /**
   * Get a searchBusCmd for this busmaster.
   *
   * @return searchBusCmd
   */
  public SearchBusCmd querySearchBusCmd();

  /**
   * Get a searchBusCmd by family code for this busmaster.
   *
   * @param familyCode
   * @return searchBusCmd
   */
  public SearchBusCmd querySearchBusByFamilyCmd(short familyCode);

  /**
   * Get a searchBusCmd by alarm for this busmaster.
   *
   * @return searchBusCmd
   */
  public SearchBusCmd querySearchBusByAlarmCmd();

  /**
   * This method will ask the BusMaster to perform a bus search cmd at the specified minimum time
   * period. If another Object is registered with a shorter time period then an earlier search may
   * be performed. Likewise if an independent SearchCmd is performed then there will be a callback
   * opportunity too, the only guarantee is that the the BusMaster will generate a SearchCmd at the
   * period specified. Callback objects will only be notified if the data from a search is different
   * since the last time they were notified.
   *
   * @param obj obj with SearchBusNotify interface.
   * @param typeByAlarm true if the search should be by active alarms, otherwise it will be a
   * general bus search
   * @throws IllegalArgumentException if the arguments are illegal
   */
  public void scheduleNotifySearchBusCmd(NotifySearchBusCmdResult obj, boolean typeByAlarm,
      long minPeriodMSec) throws IllegalArgumentException;

  /**
   * This method will ask the BusMaster to update previously scheduled search bus with a new
   * specified minimum time period. If another Object is registered with a shorter time period then
   * the an earlier search may be performed. Likewise if an independent SearchCmd is performed then
   * there will be a callback opportunity too. The only guarantee is that the the BusMaster will
   * generate a SearchCmd at the period specified.
   *
   * @param obj obj with SearchBusNotify interface. The obj will only be called back if the data is
   * different since the last time it was called back. The search itself will be performed but no
   * callback will occur.
   * @param typeByAlarm true if the search was active alarms, otherwise general bus search
   * @throws IllegalArgumentException if the arguments are illegal
   */
  public void updateScheduledNotifySearchBusCmd(NotifySearchBusCmdResult obj, boolean typeByAlarm,
      long minPeriodMSec) throws IllegalArgumentException;

  /**
   * Cancel a previously scheduled search notification.
   *
   * @param obj Instance which had successfully called scheduleSearchNotify()
   * @param typeByAlarm true if the search was active alarms, otherwise general bus search
   * @throws IllegalArgumentException if the arguments are illegal
   */
  public void cancelScheduledNotifySearchBusCmd( NotifySearchBusCmdResult obj, boolean typeByAlarm)
      throws IllegalArgumentException;

  /**
   * Get a ConvertTCmd for the specified DSAddress from this busmaster.
   *
   * @param dsAddr
   * @return ConvertTCmd
   */
  public ConvertTCmd queryConvertTCmd(final DSAddress dsAddr);

  /**
   * Get a ReadPowerSupplyCmd for the specified DSAddress from this busmaster.
   *
   * @param dsAddr
   * @return ReadPowerSupplyCmd
   */
  public ReadPowerSupplyCmd queryReadPowerSupplyCmd(final DSAddress dsAddr);

  /**
   * Get a ReadScratchpadCmd for the specified DSAddress from this busmaster.
   *
   * @param dsAddr
   * @param requestByteCount
   * @return ReadScratchpadCmd
   */
  public ReadScratchpadCmd queryReadScratchpadCmd(final DSAddress dsAddr, short requestByteCount);

  /**
   * Get a WriteScratchpadCmd for the specified DSAddress from this busmaster.
   *
   * @param dsAddr
   * @param writeData
   * @return WriteScratchpadCmd
   */
  public WriteScratchpadCmd queryWriteScratchpadCmd(final DSAddress dsAddr, byte[] writeData);

  // ReadStatusCmd
  // AA {0000 index} FFFFFFFFFF00007F {EDC1 crc}, so write 1 + 2 + 8 + 2 = 13 = 0D

}
