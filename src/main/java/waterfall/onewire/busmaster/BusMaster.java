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
     * @param optLogger
     * @return
     */
    public StartBusCmd queryStartBusCmd(Logger optLogger);

    /**
     * @param optLogger
     * @return
     */
    public StopBusCmd queryStopBusCmd(Logger optLogger);

    /**
     * @param optLogger
     * @return
     */
    public SearchBusCmd querySearchBusCmd(Logger optLogger);

    /**
     * @param familyCode
     * @param optLogger
     * @return
     */
    public SearchBusCmd querySearchBusByFamilyCmd(short familyCode, Logger optLogger);

    /**
     * @param optLogger
     * @return
     */
    public SearchBusCmd querySearchBusByAlarmCmd(Logger optLogger);

    /**
     * @param dsAddr
     * @param optLogger
     * @return
     */
    public ConvertTCmd queryConvertTCmd(final DSAddress dsAddr, Logger optLogger);

    /**
     * @param dsAddr
     * @param optLogger
     * @return
     */
    public ReadPowerSupplyCmd queryReadPowerSupplyCmd(final DSAddress dsAddr, Logger optLogger);

    /**
     * @param dsAddr
     * @param requestByteCount
     * @param optLogger
     * @return
     */
    public ReadScratchpadCmd queryReadScratchpadCmd(final DSAddress dsAddr, short requestByteCount, Logger optLogger);

}
