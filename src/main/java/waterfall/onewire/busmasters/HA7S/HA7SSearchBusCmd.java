package waterfall.onewire.busmasters.HA7S;

import waterfall.onewire.busmaster.SearchBusCmd;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dwaterfa on 6/9/16.
 */
public class HA7SSearchBusCmd extends SearchBusCmd {

    // All (so !Alarm)
    public HA7SSearchBusCmd(HA7S ha7s, boolean log) {
        super(ha7s, false, log);
    }

    // By familyCode
    public HA7SSearchBusCmd(HA7S ha7s, short familyCode, boolean log) {
        super(ha7s, familyCode, log);
    }

    // By Alarm
    public HA7SSearchBusCmd(HA7S ha7s, boolean byAlarm, boolean log) {
        super(ha7s, byAlarm, log);
    }

    public short getFamilyCode() {
        assert isByFamilyCode();
        return familyCode;
    }

    public void setResultList(List<String> resultList) {
        assert (result == SearchBusCmd.Result.busy);
        this.resultList = resultList;
    }

    public void setResultWriteCTM(long resultWriteCTM) {
        assert (result == SearchBusCmd.Result.busy);
        this.resultWriteCTM = resultWriteCTM;
    }

    protected SearchBusCmd.Result execute_internal() {
        assert (result == SearchBusCmd.Result.busy);
        assert (resultList == null);
        assert (resultWriteCTM == 0);

        ArrayList<String> resultList = new ArrayList<String>();

        byte[] rbuf = new byte[16];

        for (int i = 0; true; i++) {
            HA7S.cmdReturn ret;

            if (isByFamilyCode()) {
                if (i == 0) {
                    ret = ((HA7S)busMaster).cmdFamilySearch((byte)getFamilyCode(), rbuf, getLogger());
                }
                else {
                    ret = ((HA7S) busMaster).cmdNextFamilySearch(rbuf, getLogger());
                }
            }
            else if (isByAlarm()) {
                if (i == 0) {
                    ret = ((HA7S)busMaster).cmdConditionalSearch(rbuf, getLogger());
                }
                else {
                    ret = ((HA7S) busMaster).cmdNextConditionalSearch(rbuf, getLogger());
                }
            }
            else {
                if (i == 0) {
                    ret = ((HA7S)busMaster).cmdSearchROM(rbuf, getLogger());
                }
                else {
                    ret = ((HA7S) busMaster).cmdNextSearchROM(rbuf, getLogger());
                }
            }

            switch (ret.result) {
                case NotStarted:
                    return SearchBusCmd.Result.bus_not_started;
                case Success:
                    if ((ret.readCount == 0) || (ret.readCount == 16)) {
                        break;
                    }
                    if (getLogger() != null) {
                        getLogger().logError(this.getClass().getSimpleName(), "Expected readCount of 0 or 16, got:" + ret.readCount);
                    }
                    // FALLTHROUGH
                case DeviceNotFound:
                case ReadTimeout:
                case ReadOverrun:
                case ReadError:
                default:
                    return SearchBusCmd.Result.communication_error;
            }

            if (i == 0)  {
                setResultWriteCTM(ret.writeCTM);
                setResultList(resultList);
            }

            if (ret.readCount == 0) {
                return SearchBusCmd.Result.success;
            }

            resultList.add(new String(rbuf, 0, 16));
        }
    }
}

