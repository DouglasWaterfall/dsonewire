package waterfall.onewire.busmaster.HA7S;

import java.util.ArrayList;

import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.*;

public class HA7S implements BusMaster {

    private String portDevName;
    private Boolean started = null;
    private HA7SSerial serialPort = null;

    private static final long defaultTimeoutMSec = 5000;

    /*
    * Begin HA7S specific methods
    */
    public String getName() {
        return "HA7S on " + ((portDevName != null) ? portDevName : "no device");
    }

    public long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    public boolean getIsStarted() {
        return ((started != null) && started.booleanValue());
    }

    public StartBusCmd queryStartBusCmd(Logger optLogger) {
        return new HA7SStartBusCmd(this, optLogger);
    }

    public StopBusCmd queryStopBusCmd(Logger optLogger) {
        return new HA7SStopBusCmd(this, optLogger);
    }

    public SearchBusCmd querySearchBusCmd(Logger optLogger) {
        return new HA7SSearchBusCmd(this, false, optLogger);
    }

    public SearchBusCmd querySearchBusByFamilyCmd(short familyCode, Logger optLogger) {
        return new HA7SSearchBusCmd(this, familyCode, optLogger);
    }

    public SearchBusCmd querySearchBusByAlarmCmd(Logger optLogger) {
        return new HA7SSearchBusCmd(this, true, optLogger);
    }

    public ReadPowerSupplyCmd queryReadPowerSupplyCmd(DSAddress dsAddr, Logger optLogger) {
        return new HA7SReadPowerSupplyCmd(this, dsAddr, optLogger);
    }

    public ConvertTCmd queryConvertTCmd(DSAddress dsAddr, Logger optLogger) {
        return new HA7SConvertTCmd(this, dsAddr, optLogger);
    }

    public ReadScratchpadCmd queryReadScratchpadCmd(DSAddress dsAddr, short requestByteCount, Logger optLogger) {
        return new HA7SReadScratchpadCmd(this, dsAddr, requestByteCount, optLogger);
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

        HA7SSerial.StartResult startResult = serialPort.start(cmd.getOptLogger());

        if (startResult != HA7SSerial.StartResult.SR_Success) {
            return StartBusCmd.Result.communication_error;
        }

        byte[] rbuf = new byte[8];

        HA7SSerial.ReadResult readResult = serialPort.writeReadTilCR(resetBusCmd, rbuf, defaultTimeoutMSec, cmd.getOptLogger());

        if ((readResult.error == HA7SSerial.ReadResult.ErrorCode.RR_Success) &&
                (readResult.readCount == 1) &&
                (rbuf[0] == 0x07)) {
            // This can occur during development when when the first thing read after open is
            // 0x07 0x0D. So we try this again once.
            readResult = serialPort.writeReadTilCR(resetBusCmd, rbuf, defaultTimeoutMSec, cmd.getOptLogger());
        }

        if ((readResult.error != HA7SSerial.ReadResult.ErrorCode.RR_Success) ||
                (readResult.readCount != 0)) {
            if (cmd.getOptLogger() != null) {
                cmd.getOptLogger().error(readResult.error.name() + " readCount:" + readResult.readCount);
            }
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

        HA7SSerial.StopResult stopResult = serialPort.stop(cmd.getOptLogger());

        if (stopResult != HA7SSerial.StopResult.SR_Success) {
            return StopBusCmd.Result.communication_error;
        }

        started = null;
        return StopBusCmd.Result.stopped;
    }

    public synchronized SearchBusCmd.Result executeSearchBusCmd(HA7SSearchBusCmd cmd) {
        final byte[] firstNotByAlarm = {'S'};
        final byte[] secondNotByAlarm = {'s'};

        final byte[] firstByAlarm = {'C'};
        final byte[] secondByAlarm = {'c'};

        final byte[] firstByFamilyCode = {'F', '0', '0'};
        final byte[] secondByFamilyCode = {'f'};

        byte[] rbuf = new byte[(8 * 2) + 1]; // 16 hex + CR

        byte[] first, second;

        ArrayList<String> resultList = new ArrayList<String>();
        long resultWriteCTM = 0;

        if (cmd.isByFamilyCode()) {
            firstByFamilyCode[1] = fourBitsToHex(((int) cmd.getFamilyCode() & 0xff) >> 4);
            firstByFamilyCode[2] = fourBitsToHex(((int) cmd.getFamilyCode() & 0xff) & 0xf);

            first = firstByFamilyCode;
            second = secondByFamilyCode;
        } else if (cmd.isByAlarm()) {
            first = firstByAlarm;
            second = secondByAlarm;
        } else {
            first = firstNotByAlarm;
            second = secondNotByAlarm;
        }

        SearchBusCmd.Result result = null;

        for (; ; ) {
            final byte[] wbuf = (resultList.size() == 0) ? first : second;

            HA7SSerial.ReadResult readResult = serialPort.writeReadTilCR(wbuf, rbuf, defaultTimeoutMSec, cmd.getOptLogger());

            if ((readResult.error != HA7SSerial.ReadResult.ErrorCode.RR_Success) ||
                    ((readResult.readCount != 0) && (readResult.readCount != 16))) {
                return SearchBusCmd.Result.communication_error;
            }

            if (resultList.size() == 0) {
                resultWriteCTM = readResult.postWriteCTM;
            }

            if (readResult.readCount == 0) {
                cmd.setResultWriteCTM(resultWriteCTM);
                cmd.setResultList(resultList);
                return SearchBusCmd.Result.success;
            }

            resultList.add(new String(rbuf, 0, 16));
        }
    }

    public ReadPowerSupplyCmd.Result executeReadPowerSupplyCmd(HA7SReadPowerSupplyCmd cmd) {

        final byte[] readPowerSupplyCmd = {
                'W', '0', '2', 'B', '4', 'F', 'F', '\r'
        };

        final byte[] rbuf = new byte[Math.max(cmd.getSelectCmdData().length, readPowerSupplyCmd.length)];

        ReadPowerSupplyCmd.Result result = null;

        HA7SSerial.ReadResult readResult = serialPort.writeReadTilCR(cmd.getSelectCmdData(), rbuf, defaultTimeoutMSec, cmd.getOptLogger());

        if (readResult.error != HA7SSerial.ReadResult.ErrorCode.RR_Success) {
            return ReadPowerSupplyCmd.Result.communication_error;
        }

        if (readResult.readCount != 16) {
            return ReadPowerSupplyCmd.Result.communication_error;
        }

        readResult = serialPort.writeReadTilCR(readPowerSupplyCmd, rbuf, defaultTimeoutMSec, cmd.getOptLogger());

        if (readResult.error != HA7SSerial.ReadResult.ErrorCode.RR_Success) {
            return ReadPowerSupplyCmd.Result.communication_error;
        }

        if (readResult.readCount != 4) {
            return ReadPowerSupplyCmd.Result.communication_error;
        }

        // externally powered will pull the bus high
        cmd.setResultIsParasitic((hexToFourBits(rbuf[3]) & 0x01) == 0);
        cmd.setResultWriteCTM(readResult.postWriteCTM);
        return ReadPowerSupplyCmd.Result.success;
    }

    public synchronized ConvertTCmd.Result executeConvertTCmd(HA7SConvertTCmd cmd) {

        final byte[] convertTCmd = {
                'W', '0', '1', '4', '4', '\r'
        };

        final byte[] rbuf = new byte[Math.max(cmd.getSelectCmdData().length, convertTCmd.length)];

        ConvertTCmd.Result result = null;

        HA7SSerial.ReadResult readResult = serialPort.writeReadTilCR(cmd.getSelectCmdData(), rbuf, defaultTimeoutMSec, cmd.getOptLogger());

        if (readResult.error != HA7SSerial.ReadResult.ErrorCode.RR_Success) {
            return ConvertTCmd.Result.communication_error;
        }

        if (readResult.readCount != 16) {
            return ConvertTCmd.Result.communication_error;
        }

        readResult = serialPort.writeReadTilCR(convertTCmd, rbuf, defaultTimeoutMSec, cmd.getOptLogger());

        if (readResult.error != HA7SSerial.ReadResult.ErrorCode.RR_Success) {
            return ConvertTCmd.Result.communication_error;
        }

        if (readResult.readCount != 2) {
            return ConvertTCmd.Result.communication_error;
        }

        cmd.setResultWriteCTM(readResult.postWriteCTM);
        return ConvertTCmd.Result.success;
    }

    public synchronized ReadScratchpadCmd.Result executeReadScratchpadCmd(HA7SReadScratchpadCmd cmd) {

        final byte[] rbuf = new byte[Math.max(cmd.getSelectCmdData().length, cmd.getReadScratchpadCmdData().length)];

        ReadScratchpadCmd.Result result = null;

        HA7SSerial.ReadResult readResult = serialPort.writeReadTilCR(cmd.getSelectCmdData(), rbuf, defaultTimeoutMSec, cmd.getOptLogger());

        if (readResult.error != HA7SSerial.ReadResult.ErrorCode.RR_Success) {
            return ReadScratchpadCmd.Result.communication_error;
        }

        if (readResult.readCount != 16) {
            return ReadScratchpadCmd.Result.communication_error;
        }

        readResult = serialPort.writeReadTilCR(cmd.getReadScratchpadCmdData(), rbuf, defaultTimeoutMSec, cmd.getOptLogger());

        if (readResult.error != HA7SSerial.ReadResult.ErrorCode.RR_Success) {
            return ReadScratchpadCmd.Result.communication_error;
        }

        if (readResult.readCount != (2 + (cmd.getRequestCount() * 2))) {
            return ReadScratchpadCmd.Result.communication_error;
        }

        // return count of characters in the char buffer.
        byte[] resultData = new byte[cmd.getRequestCount()];
        hexToChar(rbuf, 2, (cmd.getRequestCount() * 2), resultData, 0);
        cmd.setResultData(resultData);
        cmd.setResultWriteCTM(readResult.postWriteCTM);
        return ReadScratchpadCmd.Result.success;
    }

    //
    // char-hex conversion static methods
    //
    // returns new value for toffset.
    //
    private static int byteToHex(final byte[] from, int from_offset, int from_count, final byte[] tbuf, int toffset) {
        int f_index = from_offset;
        int t_idx = toffset;

        for (int i = 0; i < from_count; i++) {
            tbuf[t_idx++] = fourBitsToHex(((int) from[f_index] & 0xff) >> 4);
            tbuf[t_idx++] = fourBitsToHex(((int) from[f_index++] & 0xff) & 0xf);
        }

        return t_idx;
    }

    private static int byteToHex(final byte[] from, final byte[] tbuf, int toffset) {
        return byteToHex(from, 0, from.length, tbuf, toffset);
    }

    private static int byteToHex(byte fill_char, int fill_count, final byte[] tbuf, int toffset) {
        int t_idx = toffset;
        byte b1 = fourBitsToHex(((int) fill_char & 0xff) >> 4);
        byte b2 = fourBitsToHex(((int) fill_char & 0xff) & 0xf);

        for (int i = 0; i < fill_count; i++) {
            tbuf[t_idx++] = b1;
            tbuf[t_idx++] = b2;
        }

        return t_idx;
    }

    // return count of characters in the char buffer.
    public static int hexToChar(final byte[] from, int findex, int fcount, byte[] tbuf, int toffset) {
        int idx = toffset;

        for (int i = 0; i < fcount; i += 2) {
            tbuf[idx++] = (byte) ((hexToFourBits(from[findex + i]) << 4) + hexToFourBits(from[findex + i + 1]));
        }

        return (fcount / 2);
    }

    private static final byte[] toHexTable = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
            'E', 'F'};

    public static byte fourBitsToHex(int fourBits) {
        return toHexTable[fourBits];
    }

    public static byte hexToFourBits(byte hex) {
        if ((hex >= '0') && (hex <= '9')) {
            return (byte) (hex - '0');
        }

        if ((hex >= 'A') && (hex <= 'F')) {
            return (byte) (10 + (hex - 'A'));
        }

        Thread.dumpStack();
        System.exit(1);
        return -1;
    }

    public static byte[] buildSelectCmdData(DSAddress dsAddr) {
        // Select CMD (16 hex address bytes)
        byte[] data = new byte[]{
                'A', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '\r'};

        final String str = dsAddr.toString();
        for (int i = 0; i < 16; i++) {
            data[1 + i] = (byte) str.charAt(i);
        }

        return data;
    }

    ;

}
