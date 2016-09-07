package waterfall.onewire.busmasters.HA7S;

import waterfall.onewire.Convert;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.ReadPowerSupplyCmd;

/**
 * Created by dwaterfa on 6/9/16.
 */
public class HA7SReadPowerSupplyCmd extends ReadPowerSupplyCmd {

    public HA7SReadPowerSupplyCmd(HA7S ha7s, DSAddress dsAddr, boolean log) {
        super(ha7s, dsAddr, log);
    }

    @Override
    protected ReadPowerSupplyCmd.Result execute_internal() {
        assert (result == Result.busy);
        assert (resultWriteCTM == 0);


        HA7S.cmdReturn ret = ((HA7S)busMaster).cmdAddressSelect(getAddress(), getLogger());
        switch (ret.result) {
            case Success:
                break;
            case NotStarted:
                return ReadPowerSupplyCmd.Result.bus_not_started;
            case DeviceNotFound:
                return ReadPowerSupplyCmd.Result.device_not_found;
            case ReadTimeout:
            case ReadOverrun:
            case ReadError:
            default:
                return ReadPowerSupplyCmd.Result.communication_error;
        }

        final byte[] readPowerSupplyCmdData = {
                'W', '0', '2', 'B', '4', 'F', 'F', '\r'
        };

        final byte[] rbuf = new byte[readPowerSupplyCmdData.length];

        ret = ((HA7S)busMaster).cmdWriteBlock(readPowerSupplyCmdData, rbuf, getLogger());

        if (ret.result != HA7S.cmdResult.Success) {
            // All other returns are basically logic errors or real errors.
            return ReadPowerSupplyCmd.Result.communication_error;
        }

        if (ret.readCount != 4) {
            if (getLogger() != null) {
                getLogger().logError(this.getClass().getSimpleName(), "Expected readCount of 4, got:" + ret.readCount);
            }
            return ReadPowerSupplyCmd.Result.communication_error;
        }

        // externally powered will pull the bus high
        final int v = Convert.hexToFourBits(rbuf[3]);
        setResultData(ret.writeCTM, ((v & 0x01) == 0));

        return ReadPowerSupplyCmd.Result.success;
    }

    @Override
    public void setResultData(long resultWriteCTM, boolean isParasitic) {
        assert (result == Result.busy);
        this.resultWriteCTM = resultWriteCTM;
        this.resultIsParasitic = isParasitic;
    }

}

