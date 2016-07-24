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
     * @param doLog
     * @return
     */
    public StartBusCmd queryStartBusCmd(boolean doLog);

    /**
     * @param doLog
     * @return
     */
    public StopBusCmd queryStopBusCmd(boolean doLog);

    /**
     * @param doLog
     * @return
     */
    public SearchBusCmd querySearchBusCmd(boolean doLog);

    /**
     * @param familyCode
     * @param doLog
     * @return
     */
    public SearchBusCmd querySearchBusByFamilyCmd(short familyCode, boolean doLog);

    /**
     * @param doLog
     * @return
     */
    public SearchBusCmd querySearchBusByAlarmCmd(boolean doLog);

    /**
     * @param dsAddr
     * @param doLog
     * @return
     */
    public ConvertTCmd queryConvertTCmd(final DSAddress dsAddr, boolean doLog);

    /**
     * @param dsAddr
     * @param doLog
     * @return
     */
    public ReadPowerSupplyCmd queryReadPowerSupplyCmd(final DSAddress dsAddr, boolean doLog);

    /**
     * @param dsAddr
     * @param requestByteCount
     * @param doLog
     * @return
     */
    public ReadScratchpadCmd queryReadScratchpadCmd(final DSAddress dsAddr, short requestByteCount, boolean doLog);

    // ReadStatusCmd
    // AA {0000 index} FFFFFFFFFF00007F {EDC1 crc}, so write 1 + 2 + 8 + 2 = 13 = 0D
}
