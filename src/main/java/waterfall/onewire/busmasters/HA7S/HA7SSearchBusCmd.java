package waterfall.onewire.busmasters.HA7S;

import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.SearchBusCmd;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dwaterfa on 6/9/16.
 */
public class HA7SSearchBusCmd extends SearchBusCmd {

    // All (so !Alarm)
    public HA7SSearchBusCmd(HA7S ha7s, LogLevel logLevel) {
        super(ha7s, false, logLevel);
    }

    // By familyCode
    public HA7SSearchBusCmd(HA7S ha7s, short familyCode, LogLevel logLevel) {
        super(ha7s, familyCode, logLevel);
    }

    // By Alarm
    public HA7SSearchBusCmd(HA7S ha7s, boolean byAlarm, LogLevel logLevel) {
        super(ha7s, byAlarm, logLevel);
    }

    public short getFamilyCode() {
        assert isByFamilyCode();
        return familyCode;
    }

    protected SearchBusCmd.Result execute_internal() {
        assert (result == SearchBusCmd.Result.busy);
        assert (resultData == null);

        ArrayList<String> resultList = new ArrayList<String>();

        byte[] rbuf = new byte[16];

        for (int i = 0; true; i++) {
            HA7S.cmdReturn ret;

            if (isByFamilyCode()) {
                if (i == 0) {
                    ret = ((HA7S)busMaster).cmdFamilySearch((byte)getFamilyCode(), rbuf, getDeviceLevelLogger());
                }
                else {
                    ret = ((HA7S) busMaster).cmdNextFamilySearch(rbuf, getDeviceLevelLogger());
                }
            }
            else if (isByAlarm()) {
                if (i == 0) {
                    ret = ((HA7S)busMaster).cmdConditionalSearch(rbuf, getDeviceLevelLogger());
                }
                else {
                    ret = ((HA7S) busMaster).cmdNextConditionalSearch(rbuf, getDeviceLevelLogger());
                }
            }
            else {
                if (i == 0) {
                    ret = ((HA7S)busMaster).cmdSearchROM(rbuf, getDeviceLevelLogger());
                }
                else {
                    ret = ((HA7S) busMaster).cmdNextSearchROM(rbuf, getDeviceLevelLogger());
                }
            }

            switch (ret.result) {
                case NotStarted:
                    return SearchBusCmd.Result.bus_not_started;
                case Success:
                    if ((ret.readCount == 0) || (ret.readCount == 16)) {
                        break;
                    }
                    logErrorInternal("Expected readCount of 0 or 16, got:" + ret.readCount);
                    // FALLTHROUGH
                case DeviceNotFound:
                case ReadTimeout:
                case ReadOverrun:
                case ReadError:
                default:
                    return SearchBusCmd.Result.communication_error;
            }

            if (i == 0)  {
                setResultData(ret.writeCTM, resultList);
            }

            if (ret.readCount == 0) {
                return SearchBusCmd.Result.success;
            }

            resultList.add(new String(rbuf, 0, 16));
        }
    }

    public void setResultData(long resultWriteCTM, List<String> resultList) {
        assert (result == SearchBusCmd.Result.busy);
        this.resultData = new SearchBusCmd.ResultData(resultList, resultWriteCTM);
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

}

