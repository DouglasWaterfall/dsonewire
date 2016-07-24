package waterfall.onewire.busmaster.HA7S;

import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.ConvertTCmd;

/**
 * Created by dwaterfa on 6/11/16.
 */
public class HA7SConvertTCmd extends ConvertTCmd {

    private final byte[] selectCmd;

    public HA7SConvertTCmd(HA7S ha7s, DSAddress dsAddr, boolean log) {
        super(ha7s, dsAddr, log);
        selectCmd = ha7s.buildSelectCmdData(dsAddr);
    }

    public byte[] getSelectCmdData() {
        return selectCmd;
    }

    public void setResultWriteCTM(long resultWriteCTM) {
        assert (result == Result.busy);
        this.resultWriteCTM = resultWriteCTM;
    }

    protected ConvertTCmd.Result execute_internal() {
        assert (result == Result.busy);
        assert (resultWriteCTM == 0);

        return ((HA7S) busMaster).executeConvertTCmd(this);
    }

}


