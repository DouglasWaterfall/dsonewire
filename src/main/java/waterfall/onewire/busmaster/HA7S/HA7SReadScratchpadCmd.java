package waterfall.onewire.busmaster.HA7S;

import com.dalsemi.onewire.utils.CRC8;
import com.dalsemi.onewire.utils.Convert;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.ReadScratchpadCmd;

/**
 * Created by dwaterfa on 6/11/16.
 */
public class HA7SReadScratchpadCmd extends ReadScratchpadCmd {

    private byte[] readScratchpadCmdData;
    private final static String logContext = "HA7SReadScratchPad";

    public HA7SReadScratchpadCmd(HA7S ha7s, DSAddress dsAddr, short requestByteCount, boolean log) {
        super(ha7s, dsAddr, requestByteCount, log);
    }

    public void setResultData(byte[] resultData) {
        assert (result == ReadScratchpadCmd.Result.busy);
        this.resultData = resultData;
    }

    public void setResultWriteCTM(long resultWriteCTM) {
        assert (result == ReadScratchpadCmd.Result.busy);
        this.resultWriteCTM = resultWriteCTM;
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
            readScratchpadCmdData[i++] = ((HA7S)busMaster).fourBitsToHex(((int) (requestByteCount + 1) & 0xff) >> 4);
            readScratchpadCmdData[i++] = ((HA7S)busMaster).fourBitsToHex(((int) (requestByteCount + 1) & 0xff) & 0xf);
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
            if (getLogger() != null) {
                getLogger().logError(logContext, "Expected readCount of " + expectedReadCount + ", got:" + ret.readCount);
            }
            return ReadScratchpadCmd.Result.communication_error;
        }

        // check the CRC
        final short crcIndex = dsAddrToCRCIndex(dsAddr);
        if ((crcIndex >= 0) && ((crcIndex + 1) == getRequestByteCount())) {
            try {
                String s = new String(rbuf, 2, getRequestByteCount());
                byte[] ba = Convert.toByteArray(s);
                if (CRC8.compute(ba) != 0) {
                    if (getLogger() != null) {
                        getLogger().logError(logContext, "CRC8 failed, crcIndex:" + crcIndex + " s:" + s);
                    }
                    return ReadScratchpadCmd.Result.communication_error;
                }
            }
            catch (Convert.ConvertException e) {
                if (getLogger() != null) {
                    getLogger().logError(logContext, e);
                }
                return ReadScratchpadCmd.Result.communication_error;
            }
        }

        // return count of characters in the char buffer.
        byte[] resultData = new byte[getRequestByteCount()];
        ((HA7S) busMaster).hexToChar(rbuf, 2, (getRequestByteCount() * 2), resultData, 0);
        setResultData(resultData);
        setResultWriteCTM(ret.writeCTM);

        return ReadScratchpadCmd.Result.success;
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
