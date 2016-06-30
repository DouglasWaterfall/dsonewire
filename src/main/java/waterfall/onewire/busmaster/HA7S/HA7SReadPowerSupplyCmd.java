package waterfall.onewire.busmaster.HA7S;

import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.ReadPowerSupplyCmd;

/**
 * Created by dwaterfa on 6/9/16.
 */
public class HA7SReadPowerSupplyCmd extends ReadPowerSupplyCmd {

    private final byte[] selectCmd;

    public HA7SReadPowerSupplyCmd(HA7S ha7s, DSAddress dsAddr, Logger optLogger) {
        super(ha7s, dsAddr, optLogger);
        selectCmd = ha7s.buildSelectCmdData(dsAddr);
    }

    public byte[] getSelectCmdData() {
        return selectCmd;
    }

    public void setResultIsParasitic(boolean isParasitic) {
        assert (result == Result.busy);
        this.resultIsParasitic = isParasitic;
    }

    public void setResultWriteCTM(long resultWriteCTM) {
        assert (result == Result.busy);
        this.resultWriteCTM = resultWriteCTM;
    }

    protected ReadPowerSupplyCmd.Result execute_internal() {
        assert (result == Result.busy);
        assert (resultWriteCTM == 0);

        return ((HA7S) busMaster).executeReadPowerSupplyCmd(this);
    }

}

