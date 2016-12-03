package waterfall.onewire.busmaster;

import waterfall.onewire.DSAddress;

public interface BusMaster {

    /**
     * returns the human readable name of the bus
     *
     * @return human readable description of the bus
     */
    public String getName();

    /**
     * Get the current time in millis for the system the BusMaster is controlled by.
     * This is useful for comparing time which has elapsed on the bus.
     * All methods which a return a Long in this class return this same time source.
     *
     * @return the currentTimeMillis for the VM talking to the physical Bus.
     */
    public long getCurrentTimeMillis();

    /**
     * @return true if started, false otherwise.
     */
    public boolean getIsStarted();

    /**
     * @param logLevel may be null for no logging
     * @return
     */
    public StartBusCmd queryStartBusCmd(Logger.LogLevel logLevel);

    /**
     * @param logLevel may be null for no logging
     * @return
     */
    public StopBusCmd queryStopBusCmd(Logger.LogLevel logLevel);

    /**
     * @param logLevel may be null for no logging
     * @return
     */
    public SearchBusCmd querySearchBusCmd(Logger.LogLevel logLevel);

    /**
     *
     */
    public enum ScheduleSearchResult {
        /**
         * The obj specified to notify back on is null.
         */
        SSR_NotifyObjNull,

        /**
         * The value for minPeriodMSec is equal to or less than zero.
         */
        SSR_MinPeriodInvalid,

        /**
         * The obj specified to notify back on is already scheduled.
         */
        SSR_NotifyObjAlreadyScheduled,

        /**
         * The specified BusMaster is not started.
         */
        SSR_BusMasterNotStarted,

        /**
         * The obj has been scheduled for search callback.
         */
        SSR_Success
    }

    /**
     * This method will ask the BusMaster to perform a bus search cmd at the specified minimum time period. If another
     * Object is registered with a shorter time period then the an earlier search may be performed. Likewise if an
     * independent SearchCmd is performed then there will be a callback opportunity too. The only guarantee is that the
     * the BusMaster will generate a SearchCmd at the period specified.
     * @param obj obj with SearchBusNotify interface. The obj will only be called back if the data is different since
     *            the last time it was called back. The search itself will be performed but no callback will occur.
     * @param minPeriodMSec
     * @return ScheduleSearchResult
     */
    public ScheduleSearchResult scheduleSearchNotifyFor(SearchBusCmdNotifyResult obj, long minPeriodMSec);

    /**
     * Cancel a previously scheduled search notification.
     * @param obj Instance which had successfully called scheduleSearchNotify()
     * @return true if cancelled, false if unknown instance to the search scheduler.
     */
    public boolean cancelSearchNotifyFor(SearchBusCmdNotifyResult obj);

    /**
     * This method will be called by any instances of the SearchBusCmd and SearchBusByAlarmCmd generated from this
     * BusMaster after they have executed with success and right before they return to the caller. The callback will
     * be on the thread of whoever is executing the SearchCmd so minimize what you do there.
     * @param cmd Command instance from this BusMaster with a successful result.
     */
    public void searchBusCmdExecuteCallback(SearchBusCmd cmd);

    /**
     * @param familyCode
     * @param logLevel may be null for no logging
     * @return
     */
    public SearchBusCmd querySearchBusByFamilyCmd(short familyCode, Logger.LogLevel logLevel);

    /**
     * @param logLevel
     * @return
     */
    public SearchBusCmd querySearchBusByAlarmCmd(Logger.LogLevel logLevel);

    /**
     * This method will ask the BusMaster to perform an bus alarm search cmd at the specified minimum time period. If
     * another Object is registered with a shorter time period then the an earlier search may be performed. Likewise if
     * an independent SearchByAlarmCmd is performed then there will be a callback opportunity too. The only guarantee
     * is that the BusMaster will generate a SearchByAlarmCmd at the period specified.
     * @param obj obj with SearchBusByAlarmNotify interface. The obj will only be called back if the data is different
     *            since the last time it was called back. The search itself will be performed but no callback will occur.
     * @param minPeriodMSec
     * @return ScheduleSearchResult
     */
    public ScheduleSearchResult scheduleAlarmSearchNotifyFor(SearchBusByAlarmCmdNotifyResult obj, long minPeriodMSec);

    /**
     * Cancel a previously scheduled alarm search notification.
     * @param obj Instance which had successfully called scheduleAlarmSearchNotify()
     * @return true if cancelled, false if unknown instance to the alarm search scheduler.
     */
    public boolean cancelAlarmSearchNotifyFor(SearchBusByAlarmCmdNotifyResult obj);

    /**
     * @param dsAddr
     * @param logLevel may be null for no logging
     * @return
     */
    public ConvertTCmd queryConvertTCmd(final DSAddress dsAddr, Logger.LogLevel logLevel);

    /**
     * @param dsAddr
     * @param logLevel may be null for no logging
     * @return
     */
    public ReadPowerSupplyCmd queryReadPowerSupplyCmd(final DSAddress dsAddr, Logger.LogLevel logLevel);

    /**
     * @param dsAddr
     * @param requestByteCount
     * @param logLevel may be null for no logging
     * @return
     */
    public ReadScratchpadCmd queryReadScratchpadCmd(final DSAddress dsAddr, short requestByteCount, Logger.LogLevel logLevel);

    // ReadStatusCmd
    // AA {0000 index} FFFFFFFFFF00007F {EDC1 crc}, so write 1 + 2 + 8 + 2 = 13 = 0D

}
