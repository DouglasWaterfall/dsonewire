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
