package waterfall.onewire.busmaster.HA7S;

import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.ReadScratchpadCmd;

/**
 * Created by dwaterfa on 6/11/16.
 */
public class HA7SReadScratchpadCmd extends ReadScratchpadCmd {

    private final byte[] selectCmd;
    private final byte[] readScratchpadCmd;

    public HA7SReadScratchpadCmd(HA7S ha7s, DSAddress dsAddr, short requestByteCount, Logger optLogger) {
        super(ha7s, dsAddr, requestByteCount, optLogger);
        selectCmd = ha7s.buildSelectCmdData(dsAddr);

        final int totalLength = (5 + (requestByteCount * 2) + 1);
        readScratchpadCmd = new byte[totalLength];
        int i = 0;
        readScratchpadCmd[i++] = 'W';
        readScratchpadCmd[i++] = ha7s.fourBitsToHex(((int) (requestByteCount + 1) & 0xff) >> 4);
        readScratchpadCmd[i++] = ha7s.fourBitsToHex(((int) (requestByteCount + 1) & 0xff) & 0xf);
        readScratchpadCmd[i++] = 'B';
        readScratchpadCmd[i++] = 'E';
        while (i < (totalLength - 1)) {
            readScratchpadCmd[i++] = 'F';
        }
        assert (i == (totalLength - 1));
        readScratchpadCmd[i] = '\r';
    }

    public byte[] getSelectCmdData() {
        return selectCmd;
    }

    public byte[] getReadScratchpadCmdData() {
        return readScratchpadCmd;
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

        return ((HA7S) busMaster).executeReadScratchpadCmd(this);
    }

}
