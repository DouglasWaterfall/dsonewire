package waterfall.onewire.busmasters.HA7S;

import com.dalsemi.onewire.utils.Address;
import waterfall.onewire.Convert;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.*;

import java.util.*;

public class HA7S implements BusMaster {

    private Boolean started = null;
    private HA7SSerial serialPort = null;

    private NotifySearchBusCmdHelper searchHelper = null;
    private NotifySearchBusCmdHelper searchByAlarmHelper = null;

    private static final long defaultTimeoutMSec = 5000;

    @Override
    public String getName() {
        return "HA7S on " + serialPort.getPortName();
    }

    @Override
    public long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public boolean getIsStarted() {
        return ((started != null) && started);
    }

    @Override
    public StartBusCmd queryStartBusCmd(Logger.LogLevel logLevel) {
        return new HA7SStartBusCmd(this, logLevel);
    }

    @Override
    public StopBusCmd queryStopBusCmd(Logger.LogLevel logLevel) {
        return new HA7SStopBusCmd(this, logLevel);
    }

    @Override
    public SearchBusCmd querySearchBusCmd(Logger.LogLevel logLevel) {
        return new HA7SSearchBusCmd(this, false, logLevel);
    }

    @Override
    public SearchBusCmd querySearchBusByFamilyCmd(short familyCode, Logger.LogLevel logLevel) {
        return new HA7SSearchBusCmd(this, familyCode, logLevel);
    }

    @Override
    public SearchBusCmd querySearchBusByAlarmCmd(Logger.LogLevel logLevel) {
        return new HA7SSearchBusCmd(this, true, logLevel);
    }

    @Override
    public ScheduleNotifySearchBusCmdResult scheduleNotifySearchBusCmd(NotifySearchBusCmdResult obj, boolean typeByAlarm, long minPeriodMSec) {
        if (!getIsStarted()) {
            return ScheduleNotifySearchBusCmdResult.SNSBCR_BusMasterNotStarted;
        }
        if (!typeByAlarm) {
            return searchHelper.scheduleSearchNotifyFor(obj, minPeriodMSec);
        }
        else {
            return searchByAlarmHelper.scheduleSearchNotifyFor(obj, minPeriodMSec);
        }
    }

    @Override
    public UpdateScheduledNotifySearchBusCmdResult updateScheduledNotifySearchBusCmd(NotifySearchBusCmdResult obj, boolean typeByAlarm, long minPeriodMSec) {
        if (!getIsStarted()) {
            return UpdateScheduledNotifySearchBusCmdResult.USNSBC_BusMasterNotStarted;
        }
        if (!typeByAlarm) {
            return searchHelper.updateScheduledSearchNotifyFor(obj, minPeriodMSec);
        }
        else {
            return searchByAlarmHelper.updateScheduledSearchNotifyFor(obj, minPeriodMSec);
        }
    }

    @Override
    public CancelScheduledNotifySearchBusCmdResult cancelScheduledNotifySearchBusCmd(NotifySearchBusCmdResult obj, boolean typeByAlarm) {
        if (!getIsStarted()) {
            return CancelScheduledNotifySearchBusCmdResult.CSNSBC_BusMasterNotStarted;
        }
        if (!typeByAlarm) {
            return searchHelper.cancelScheduledSearchNotifyFor(obj);
        }
        else {
            return searchByAlarmHelper.cancelScheduledSearchNotifyFor(obj);
        }
    }

    public void searchBusCmdExecuteCallback(SearchBusCmd cmd) {
        if ((cmd.getBusMaster() == this) && (cmd.getResult() == SearchBusCmd.Result.success)) {
            if (cmd.isByAlarm()) {
                searchByAlarmHelper.notifySearchResult(cmd.getResultData());
            }
            else if (!cmd.isByFamilyCode()) {
                searchHelper.notifySearchResult(cmd.getResultData());
            }
        }
    }

    @Override
    public ReadPowerSupplyCmd queryReadPowerSupplyCmd(DSAddress dsAddr, Logger.LogLevel logLevel) {
        return new HA7SReadPowerSupplyCmd(this, dsAddr, logLevel);
    }

    @Override
    public ConvertTCmd queryConvertTCmd(DSAddress dsAddr, Logger.LogLevel logLevel) {
        return new HA7SConvertTCmd(this, dsAddr, logLevel);
    }

    @Override
    public ReadScratchpadCmd queryReadScratchpadCmd(DSAddress dsAddr, short requestByteCount, Logger.LogLevel logLevel) {
        return new HA7SReadScratchpadCmd(this, dsAddr, requestByteCount, logLevel);
    }

    /*
    * Begin HA7S specific methods
    */
    public HA7S(HA7SSerial serial) {
        if (serial == null) {
            throw new IllegalArgumentException("serial must non-null");
        }
        searchHelper = new NotifySearchBusCmdHelper(new SearchPusherByBusCmd(this, false), this);
        searchByAlarmHelper = new NotifySearchBusCmdHelper(new SearchPusherByBusCmd(this, true), this);
        serialPort = serial;
    }

    public synchronized StartBusCmd.Result executeStartBusCmd(HA7SStartBusCmd cmd) {
        final byte[] resetBusCmd = {'R'};

        StartBusCmd.Result result = null;

        if (started != null) {
            result = StartBusCmd.Result.already_started;
            return result;
        }

        HA7SSerial.StartResult startResult = serialPort.start(cmd.getDeviceLevelLogger());

        if (startResult != HA7SSerial.StartResult.SR_Success) {
            return StartBusCmd.Result.communication_error;
        }

        byte[] rbuf = new byte[8];

        HA7SSerial.ReadResult readResult = serialPort.writeReadTilCR(resetBusCmd, rbuf, defaultTimeoutMSec, cmd.getDeviceLevelLogger());

        if ((readResult.error == HA7SSerial.ReadResult.ErrorCode.RR_Success) &&
                (readResult.readCount == 1) &&
                (rbuf[0] == 0x07)) {
            // This can occur during development when when the first thing read after open is
            // 0x07 0x0D. So we try this again once.
            readResult = serialPort.writeReadTilCR(resetBusCmd, rbuf, defaultTimeoutMSec, cmd.getDeviceLevelLogger());
        }

        if ((readResult.error != HA7SSerial.ReadResult.ErrorCode.RR_Success) ||
                (readResult.readCount != 0)) {
            cmd.logErrorInternal(readResult.error.name() + " readCount:" + readResult.readCount);

            cmd.logErrorInternal(readResult.error.name() + " stopping port");
            HA7SSerial.StopResult stopResult = serialPort.stop((Logger) cmd);
            cmd.logErrorInternal(readResult.error.name() + " stop result:" + stopResult.name());

            return StartBusCmd.Result.communication_error;
        }

        started = new Boolean(true);

        return StartBusCmd.Result.started;
    }

    public synchronized StopBusCmd.Result executeStopBusCmd(HA7SStopBusCmd cmd) {

        StopBusCmd.Result result = null;

        if (started == null) {
            return StopBusCmd.Result.not_started;
        }

        HA7SSerial.StopResult stopResult = serialPort.stop(cmd.getDeviceLevelLogger());

        if (stopResult != HA7SSerial.StopResult.SR_Success) {
            return StopBusCmd.Result.communication_error;
        }

        started = null;
        searchHelper.cancelAllScheduledSearchNotifyFor();
        searchByAlarmHelper.cancelAllScheduledSearchNotifyFor();
        return StopBusCmd.Result.stopped;
    }

    public enum cmdResult {
        Success,
        NotStarted,
        DeviceNotFound,
        ReadTimeout,
        ReadOverrun,
        ReadError
    }

    public static class cmdReturn {
        public cmdReturn(cmdResult result) {
            this.result = result;
            this.readCount = 0;
            this.writeCTM = 0;
        }

        public cmdReturn(int readCount, long writeCTM) {
            this.result = cmdResult.Success;
            this.readCount = readCount;
            this.writeCTM = writeCTM;
        }

        public cmdResult result;
        public int readCount;
        public long writeCTM;
    }

    public cmdReturn cmdSearchROM(byte[] rbuf, Logger optLogger) {
        final byte[] wbuf = {'S'};
        return cmdSearchInternal(wbuf, rbuf, optLogger, "cmdSearchROM");
    }

    public cmdReturn cmdNextSearchROM(byte[] rbuf, Logger optLogger) {
        final byte[] wbuf = {'s'};
        return cmdSearchInternal(wbuf, rbuf, optLogger, "cmdNextSearchROM");
    }

    public cmdReturn cmdFamilySearch(byte familyCode, byte[] rbuf, Logger optLogger) {
        final byte[] wbuf = {'F', '0', '0'};
        wbuf[1] = Convert.fourBitsToHex(((int) familyCode & 0xff) >> 4);
        wbuf[2] = Convert.fourBitsToHex(((int) familyCode & 0xff) & 0xf);
        return cmdSearchInternal(wbuf, rbuf, optLogger, "cmdFamilySearch");
    }

    public cmdReturn cmdNextFamilySearch(byte[] rbuf, Logger optLogger) {
        final byte[] wbuf = {'f'};
        return cmdSearchInternal(wbuf, rbuf, optLogger, "cmdNextFamilySearch");
    }

    public cmdReturn cmdConditionalSearch(byte[] rbuf, Logger optLogger) {
        final byte[] wbuf = {'C'};
        return cmdSearchInternal(wbuf, rbuf, optLogger, "cmdConditionalSearch");
    }

    public cmdReturn cmdNextConditionalSearch(byte[] rbuf, Logger optLogger) {
        final byte[] wbuf = {'c'};
        return cmdSearchInternal(wbuf, rbuf, optLogger, "cmdNextConditionalSearch");
    }

    private cmdReturn logAndReturn(cmdReturn ret, Logger optLogger, String logContext) {
        if (optLogger != null) {
            optLogger.logError(logContext, ret.result.name());
        }
        return ret;
    }

    private cmdReturn cmdSearchInternal(byte[] wbuf, byte[] rbuf, Logger optLogger, String logContext) {
        if ((started == null) || (serialPort == null)) {
            return logAndReturn(new cmdReturn(cmdResult.NotStarted), optLogger, logContext);
        }

        HA7SSerial.ReadResult readResult = serialPort.writeReadTilCR(wbuf, rbuf, defaultTimeoutMSec, optLogger);

        switch (readResult.error) {
            case RR_Success:
                break;

            case RR_ReadTimeout:
                return logAndReturn(new cmdReturn(cmdResult.ReadTimeout), optLogger, logContext);

            case RR_ReadOverrun:
                return logAndReturn(new cmdReturn(cmdResult.ReadOverrun), optLogger, logContext);

            case RR_Error:
                return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);

            default:
                if (optLogger != null) {
                    optLogger.logError(logContext, "unknown HA7SSerial.ReadResult:" + readResult.error.name());
                }
                return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
        }

        if (readResult.readCount == 0) {
            return logAndReturn(new cmdReturn(0, readResult.postWriteCTM), optLogger, logContext);
        }

        if (readResult.readCount != 16) {
            if (optLogger != null) {
                optLogger.logError(logContext, "expected 16, readCount:" + readResult.readCount);
            }
            return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
        }

        final String addr = new String(rbuf, 0, 16);
        if (!Address.isValid(addr)) {
            if (optLogger != null) {
                optLogger.logError(logContext, "not valid address:" + addr);
            }
            return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
        }

        return logAndReturn(new cmdReturn(readResult.readCount, readResult.postWriteCTM), optLogger, logContext);
    }

    private static byte[] buildSelectCmdData(DSAddress dsAddr) {
        // Select CMD (16 hex address bytes)
        byte[] data = new byte[]{
                'A',
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                '\r'
        };

        final String str = dsAddr.toString();
        for (int i = 0; i < 16; i++) {
            data[1 + i] = (byte) str.charAt(i);
        }

        return data;
    }

    public cmdReturn cmdAddressSelect(DSAddress dsAddr, Logger optLogger) {
        final byte[] rbuf = new byte[16];
        final String logContext = "cmdAddressSelect";

        if ((started == null) || (serialPort == null)) {
            return logAndReturn(new cmdReturn(cmdResult.NotStarted), optLogger, logContext);
        }

        // Do we already have a wbuf for this dsAddress?
        final HashMap<DSAddress, byte[]> dsAddrToCmdData = new HashMap<DSAddress, byte[]>();

        byte[] selectCmdData = dsAddrToCmdData.get(dsAddr);
        if (selectCmdData == null) {
            selectCmdData = buildSelectCmdData(dsAddr);
            dsAddrToCmdData.put(dsAddr, selectCmdData);
        }

        HA7SSerial.ReadResult readResult = serialPort.writeReadTilCR(selectCmdData, rbuf, defaultTimeoutMSec, optLogger);

        switch (readResult.error) {
            case RR_Success:
                break;

            case RR_ReadTimeout:
                return logAndReturn(new cmdReturn(cmdResult.ReadTimeout), optLogger, logContext);

            case RR_ReadOverrun:
                return logAndReturn(new cmdReturn(cmdResult.ReadOverrun), optLogger, logContext);

            case RR_Error:
                return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);

            default:
                if (optLogger != null) {
                    optLogger.logError(logContext, "unknown HA7SSerial.ReadResult:" + readResult.error.name());
                }
                return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
        }

        if ((readResult.readCount == 1) && (rbuf[0] == 0x7)) {
            // This is what the HA7S returns on an error, in this case the Address is unknown.
            return logAndReturn(new cmdReturn(cmdResult.DeviceNotFound), optLogger, logContext);
        }

        if (readResult.readCount != 16) {
            if (optLogger != null) {
                optLogger.logError(logContext, "expected 16, readCount:" + readResult.readCount);
            }
            return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
        }

        return logAndReturn(new cmdReturn(0, readResult.postWriteCTM), optLogger, logContext);
    }

    public cmdReturn cmdReadBit(byte[] rbuf, Logger optLogger) {
        final byte[] wbuf = new byte[]{'O'};
        final String logContext = "cmdReadBit";

        if ((started == null) || (serialPort == null)) {
            return logAndReturn(new cmdReturn(cmdResult.NotStarted), optLogger, logContext);
        }

        HA7SSerial.ReadResult readResult = serialPort.writeReadTilCR(wbuf, rbuf, defaultTimeoutMSec, optLogger);

        switch (readResult.error) {
            case RR_Success:
                break;

            case RR_ReadTimeout:
                return logAndReturn(new cmdReturn(cmdResult.ReadTimeout), optLogger, logContext);

            case RR_ReadOverrun:
                return logAndReturn(new cmdReturn(cmdResult.ReadOverrun), optLogger, logContext);

            case RR_Error:
                return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);

            default:
                if (optLogger != null) {
                    optLogger.logError(logContext, "unknown HA7SSerial.ReadResult:" + readResult.error.name());
                }
                return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
        }

        if (readResult.readCount != 1) {
            if (optLogger != null) {
                optLogger.logError(logContext, "expected 1, readCount:" + readResult.readCount);
            }
            return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
        }

        if ((rbuf[0] != '0') && (rbuf[0] != '1')) {
            if (optLogger != null) {
                optLogger.logError(logContext, "expected 0 or 1, got:" + Byte.toString(rbuf[0]));
            }
            return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
        }


        return logAndReturn(new cmdReturn(0, readResult.postWriteCTM), optLogger, logContext);
    }

    public cmdReturn cmdWriteBlock(byte[] wbuf, byte[] rbuf, Logger optLogger) {
        final String logContext = "cmdWriteBlock";

        if ((started == null) || (serialPort == null)) {
            return logAndReturn(new cmdReturn(cmdResult.NotStarted), optLogger, logContext);
        }

        HA7SSerial.ReadResult readResult = serialPort.writeReadTilCR(wbuf, rbuf, defaultTimeoutMSec, optLogger);

        switch (readResult.error) {
            case RR_Success:
                return logAndReturn(new cmdReturn(readResult.readCount, readResult.postWriteCTM), optLogger, logContext);

            case RR_ReadTimeout:
                return logAndReturn(new cmdReturn(cmdResult.ReadTimeout), optLogger, logContext);

            case RR_ReadOverrun:
                return logAndReturn(new cmdReturn(cmdResult.ReadOverrun), optLogger, logContext);

            case RR_Error:
                return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);

            default:
                if (optLogger != null) {
                    optLogger.logError(logContext, "unknown HA7SSerial.ReadResult:" + readResult.error.name());
                }
                return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
        }
    }

}
