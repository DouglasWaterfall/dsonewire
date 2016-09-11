package waterfall.onewire.busmasters.HA7S;

import com.dalsemi.onewire.utils.CRC8;
import waterfall.onewire.Convert;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.ReadScratchpadCmd;

import java.util.Arrays;

/**
 * Created by dwaterfa on 6/11/16.
 */
public class HA7SReadScratchpadCmd extends ReadScratchpadCmd {

    private byte[] readScratchpadCmdData;

    public HA7SReadScratchpadCmd(HA7S ha7s, DSAddress dsAddr, short requestByteCount, LogLevel logLevel) {
        super(ha7s, dsAddr, requestByteCount, logLevel);
    }

    protected ReadScratchpadCmd.Result execute_internal() {
        assert (result == ReadScratchpadCmd.Result.busy);
        assert (resultData == null);
        assert (resultWriteCTM == 0);

        if (readScratchpadCmdData == null) {
            final int totalLength = (5 + (requestByteCount * 2) + 1);
            readScratchpadCmdData = new byte[totalLength];
            int i = 0;
            readScratchpadCmdData[i++] = 'W';
            readScratchpadCmdData[i++] = Convert.fourBitsToHex(((int) (requestByteCount + 1) & 0xff) >> 4);
            readScratchpadCmdData[i++] = Convert.fourBitsToHex(((int) (requestByteCount + 1) & 0xff) & 0xf);
            readScratchpadCmdData[i++] = 'B';
            readScratchpadCmdData[i++] = 'E';
            while (i < (totalLength - 1)) {
                readScratchpadCmdData[i++] = 'F';
            }
            assert (i == (totalLength - 1));
            readScratchpadCmdData[i] = '\r';
        }

        HA7S.cmdReturn ret = ((HA7S) busMaster).cmdAddressSelect(getAddress(), getLogger());
        switch (ret.result) {
            case Success:
                break;
            case NotStarted:
                return ReadScratchpadCmd.Result.bus_not_started;
            case DeviceNotFound:
                return ReadScratchpadCmd.Result.device_not_found;
            case ReadTimeout:
            case ReadOverrun:
            case ReadError:
            default:
                return ReadScratchpadCmd.Result.communication_error;
        }

        final byte[] rbuf = new byte[readScratchpadCmdData.length];

        ret = ((HA7S) busMaster).cmdWriteBlock(readScratchpadCmdData, rbuf, getLogger());

        if (ret.result != HA7S.cmdResult.Success) {
            // All other returns are basically logic errors or real errors.
            return ReadScratchpadCmd.Result.communication_error;
        }

        final int expectedReadCount = (2 + (getRequestByteCount() * 2));

        if (ret.readCount != expectedReadCount) {
            logErrorInternal("Expected readCount of " + expectedReadCount + ", got:" + ret.readCount);
            return ReadScratchpadCmd.Result.communication_error;
        }

        final int hexByteCount = (getRequestByteCount() * 2);
        byte[] resultData = new byte[getRequestByteCount()];
        Convert.hexToByte(rbuf, 2, hexByteCount, resultData, 0);

        byte[] resultHexData = Arrays.copyOfRange(rbuf, 2, (2 + hexByteCount));

        // check the CRC
        final short crcIndex = dsAddrToCRCIndex(dsAddr);
        if ((crcIndex >= 0) && ((crcIndex + 1) == getRequestByteCount()) && (CRC8.compute(resultData) != 0)) {
            logErrorInternal("CRC8 failed, crcIndex:" + crcIndex + " hex:" + resultHexData);
            return ReadScratchpadCmd.Result.communication_error;
        }

        // return count of characters in the char buffer.
        setResultData(ret.writeCTM, resultData, resultHexData);

        return ReadScratchpadCmd.Result.success;
    }

    public void setResultData(long resultWriteCTM, byte[] resultData, byte[] resultHexData) {
        assert (result == ReadScratchpadCmd.Result.busy);
        this.resultWriteCTM = resultWriteCTM;
        this.resultData = resultData;
        this.resultHexData = resultHexData;
    }

    private Logger getDeviceLevelLogger() {
        if ((getLogger() != null) && (getLogLevel().isLevelDevice())) {
            return getLogger();
        }
        return null;
    }

    private void logErrorInternal(String str) {
        if ((getLogger() != null) && (getLogLevel().isLevelDevice())) {
            getLogger().logError(this.getClass().getSimpleName(), str);
        }
    }

    private static short dsAddrToCRCIndex(final DSAddress dsAddr) {
        switch (dsAddr.getFamilyCode()) {
            case 0x28:  // DS18B20
                return 8;
            default:
                return -1;
        }
    }

}
