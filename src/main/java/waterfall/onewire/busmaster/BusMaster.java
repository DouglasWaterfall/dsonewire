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
     *
     */
    public enum ScheduleNotifySearchBusCmdResult {
        /**
         * The obj specified to notify back on is null.
         */
        SNSBCR_NotifyObjNull,

        /**
         * The value for minPeriodMSec is equal to or less than zero.
         */
        SNSBCR_MinPeriodInvalid,

        /**
         * The obj specified to notify back on is already scheduled.
         */
        SNSBCR_NotifyObjAlreadyScheduled,

        /**
         * The specified BusMaster is not started.
         */
        SNSBCR_BusMasterNotStarted,

        /**
         * The obj has been scheduled for search callback.
         */
        SNSBCR_Success
    }

    /**
     * This method will ask the BusMaster to perform a bus search cmd at the specified minimum time period. If another
     * Object is registered with a shorter time period then an earlier search may be performed.
     * Likewise if an independent SearchCmd is performed then there will be a callback opportunity too, the only
     * guarantee is that the the BusMaster will generate a SearchCmd at the period specified.
     * Callback objects will only be notified if the data from a search is different since the last time they were notified.
     * @param obj obj with SearchBusNotify interface.
     * @param typeByAlarm true if the search should be by active alarms, otherwise it will be a general bus search
     * @param minPeriodMSec
     * @return ScheduleNotifySearchBusCmdResult
     */
    public ScheduleNotifySearchBusCmdResult scheduleNotifySearchBusCmd(NotifySearchBusCmdResult obj, boolean typeByAlarm, long minPeriodMSec);

    /**
     *
     */
    public enum UpdateScheduledNotifySearchBusCmdResult {
        /**
         * The obj specified to notify back on has not been previously scheduled for the specified search type.
         */
        USNSBC_NotifyObjNotAlreadyScheduled,

        /**
         * The value for minPeriodMSec is equal to or less than zero.
         */
        USNSBC_MinPeriodInvalid,

        /**
         * The value for minPeriodMSec is equal to the currently period for this object.
         */
        USNSBC_MinPeriodUnchanged,

        /**
         * The specified BusMaster is not started.
         */
        USNSBC_BusMasterNotStarted,

        /**
         * The scheduled search callback period has been updated with the new period.
         */
        USNSBC_Success;
    }

    /**
     * This method will ask the BusMaster to update previously scheduled search bus with a new specified minimum time
     * period. If another Object is registered with a shorter time period then the an earlier search may be performed.
     * Likewise if an independent SearchCmd is performed then there will be a callback opportunity too. The only
     * guarantee is that the the BusMaster will generate a SearchCmd at the period specified.
     * @param obj obj with SearchBusNotify interface. The obj will only be called back if the data is different since
     *            the last time it was called back. The search itself will be performed but no callback will occur.
     * @param typeByAlarm true if the search was active alarms, otherwise general bus search
     * @param minPeriodMSec
     * @return UpdateScheduledSearchBusCmdResult
     */
    public UpdateScheduledNotifySearchBusCmdResult updateScheduledNotifySearchBusCmd(NotifySearchBusCmdResult obj, boolean typeByAlarm, long minPeriodMSec);

    /**
     *
     */
    public enum CancelScheduledNotifySearchBusCmdResult {
        /**
         * The obj specified to notify back on has not been previously scheduled for the specified search type.
         */
        CSNSBC_NotifyObjNotAlreadyScheduled,

        /**
         * The specified BusMaster is not started.
         */
        CSNSBC_BusMasterNotStarted,

        /**
         * The scheduled search callback period has been cancelled.
         */
        CSNSBC_Success
    }

    /**
     * Cancel a previously scheduled search notification.
     * @param obj Instance which had successfully called scheduleSearchNotify()
     * @param typeByAlarm true if the search was active alarms, otherwise general bus search
     * @return CancelScheduleSearchBusCmdResult
     */
    public CancelScheduledNotifySearchBusCmdResult cancelScheduledNotifySearchBusCmd(NotifySearchBusCmdResult obj, boolean typeByAlarm);

    /**
     * This method will be called by any instances of the SearchBusCmd and SearchBusByAlarmCmd generated from this
     * BusMaster after they have executed with success and right before they return to the caller. The callback will
     * be on the thread of whoever is executing the SearchCmd so minimize what you do there.
     * @param cmd Command instance from this BusMaster with a successful result.
     */
    public void searchBusCmdExecuteCallback(SearchBusCmd cmd);

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
