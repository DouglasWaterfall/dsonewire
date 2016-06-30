package waterfall.onewire.busmaster.HA7S;

import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.SearchBusCmd;

import java.util.List;

/**
 * Created by dwaterfa on 6/9/16.
 */
public class HA7SSearchBusCmd extends SearchBusCmd {

    public HA7SSearchBusCmd(HA7S ha7s, short familyCode, Logger optLogger) {
        super(ha7s, familyCode, optLogger);
    }

    public HA7SSearchBusCmd(HA7S ha7s, boolean byAlarm, Logger optLogger) {
        super(ha7s, byAlarm, optLogger);
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

        return ((HA7S) busMaster).executeSearchBusCmd(this);
    }

}

