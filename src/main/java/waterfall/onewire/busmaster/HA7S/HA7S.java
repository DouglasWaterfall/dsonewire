package waterfall.onewire.busmaster.HA7S;

import java.util.HashMap;

import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.*;
import waterfall.onewire.Convert;
import com.dalsemi.onewire.utils.Address;

public class HA7S implements BusMaster {

    private String portDevName;
    private Boolean started = null;
    private HA7SSerial serialPort = null;

    private static final long defaultTimeoutMSec = 5000;

    public String getName() {
        return "HA7S on " + ((portDevName != null) ? portDevName : "no device");
    }

    public long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    public boolean getIsStarted() {
        return ((started != null) && started.booleanValue());
    }

    public StartBusCmd queryStartBusCmd(boolean log) {
        return new HA7SStartBusCmd(this, log);
    }

    public StopBusCmd queryStopBusCmd(boolean doLog) {
        return new HA7SStopBusCmd(this, doLog);
    }

    public SearchBusCmd querySearchBusCmd(boolean doLog) {
        return new HA7SSearchBusCmd(this, false, doLog);
    }

    public SearchBusCmd querySearchBusByFamilyCmd(short familyCode, boolean doLog) {
        return new HA7SSearchBusCmd(this, familyCode, doLog);
    }

    public SearchBusCmd querySearchBusByAlarmCmd(boolean doLog) {
        return new HA7SSearchBusCmd(this, true, doLog);
    }

    public ReadPowerSupplyCmd queryReadPowerSupplyCmd(DSAddress dsAddr, boolean doLog) {
        return new HA7SReadPowerSupplyCmd(this, dsAddr, doLog);
    }

    public ConvertTCmd queryConvertTCmd(DSAddress dsAddr, boolean doLog) {
        return new HA7SConvertTCmd(this, dsAddr, doLog);
    }

    public ReadScratchpadCmd queryReadScratchpadCmd(DSAddress dsAddr, short requestByteCount, boolean doLog) {
        return new HA7SReadScratchpadCmd(this, dsAddr, requestByteCount, doLog);
    }

    /*
    * Begin HA7S specific methods
    */
    public HA7S(String portDevName) {
        this.portDevName = portDevName;
    }

    public synchronized StartBusCmd.Result executeStartBusCmd(HA7SStartBusCmd cmd) {
        final byte[] resetBusCmd = {'R'};

        StartBusCmd.Result result = null;

        if (started != null) {
            result = StartBusCmd.Result.already_started;
            return result;
        }

        if (serialPort == null) {
            serialPort = new JSSC(portDevName);
        }

        HA7SSerial.StartResult startResult = serialPort.start((Logger)cmd);

        if (startResult != HA7SSerial.StartResult.SR_Success) {
            return StartBusCmd.Result.communication_error;
        }

        byte[] rbuf = new byte[8];

        HA7SSerial.ReadResult readResult = serialPort.writeReadTilCR(resetBusCmd, rbuf, defaultTimeoutMSec, cmd.getLogger());

        if ((readResult.error == HA7SSerial.ReadResult.ErrorCode.RR_Success) &&
                (readResult.readCount == 1) &&
                (rbuf[0] == 0x07)) {
            // This can occur during development when when the first thing read after open is
            // 0x07 0x0D. So we try this again once.
            readResult = serialPort.writeReadTilCR(resetBusCmd, rbuf, defaultTimeoutMSec, cmd.getLogger());
        }

        if ((readResult.error != HA7SSerial.ReadResult.ErrorCode.RR_Success) ||
                (readResult.readCount != 0)) {
            cmd.logError(readResult.error.name() + " readCount:" + readResult.readCount);

            cmd.logError(readResult.error.name() + " stopping port");
            HA7SSerial.StopResult stopResult = serialPort.stop((Logger)cmd);
            cmd.logError(readResult.error.name() + " stop result:" + stopResult.name());

            return StartBusCmd.Result.communication_error;
        }

        started = new Boolean(true);

        return StartBusCmd.Result.started;
    }

    public synchronized StopBusCmd.Result executeStopBusCmd(HA7SStopBusCmd cmd) {

        StopBusCmd.Result result = null;

        if ((started == null) || (serialPort == null)) {
            return StopBusCmd.Result.not_started;
        }

        HA7SSerial.StopResult stopResult = serialPort.stop((Logger)cmd);

        if (stopResult != HA7SSerial.StopResult.SR_Success) {
            return StopBusCmd.Result.communication_error;
        }

        started = null;
        return StopBusCmd.Result.stopped;
    }

    public enum cmdResult {
        Success,
        NotStarted,
        DeviceNotFound,
        ReadTimeout,
        ReadOverrun,
        ReadError
    };

    public class cmdReturn {
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

        public cmdResult   result;
        public int readCount;
        public long writeCTM;
    }

    public cmdReturn cmdSearchROM(byte[] rbuf, Logger optLogger) {
        final byte[] wbuf = { 'S' };
        return cmdSearchInternal(wbuf, rbuf, optLogger, "cmdSearchROM");
    }

    public cmdReturn cmdNextSearchROM(byte[] rbuf, Logger optLogger) {
        final byte[] wbuf = { 's' };
        return cmdSearchInternal(wbuf, rbuf, optLogger, "cmdNextSearchROM");
    }

    public cmdReturn cmdFamilySearch(byte familyCode, byte[] rbuf, Logger optLogger) {
        final byte[] wbuf = {'F', '0', '0'};
        wbuf[1] = Convert.fourBitsToHex(((int)familyCode & 0xff) >> 4);
        wbuf[2] = Convert.fourBitsToHex(((int)familyCode & 0xff) & 0xf);
        return cmdSearchInternal(wbuf, rbuf, optLogger, "cmdFamilySearch");
    }

    public cmdReturn cmdNextFamilySearch(byte[] rbuf, Logger optLogger) {
        final byte[] wbuf = { 'f' };
        return cmdSearchInternal(wbuf, rbuf, optLogger, "cmdNextFamilySearch");
    }

    public cmdReturn cmdConditionalSearch(byte[] rbuf, Logger optLogger) {
        final byte[] wbuf = { 'C' };
        return cmdSearchInternal(wbuf, rbuf, optLogger, "cmdConditionalSearch");
    }

    public cmdReturn cmdNextConditionalSearch(byte[] rbuf, Logger optLogger) {
        final byte[] wbuf = { 'c' };
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
        byte[] data = new byte[] {
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
        final byte[] wbuf = new byte[] { 'O' };
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
