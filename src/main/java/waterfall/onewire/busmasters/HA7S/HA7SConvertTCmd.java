package waterfall.onewire.busmasters.HA7S;

import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.ConvertTCmd;

/**
 * Created by dwaterfa on 6/11/16.
 */
public class HA7SConvertTCmd extends ConvertTCmd {

    public HA7SConvertTCmd(HA7S ha7s, DSAddress dsAddr, boolean log) {
        super(ha7s, dsAddr, log);
    }

    protected ConvertTCmd.Result execute_internal() {
        assert (result == Result.busy);
        assert (resultWriteCTM == 0);

        HA7S.cmdReturn ret = ((HA7S)busMaster).cmdAddressSelect(getAddress(), getLogger());
        switch (ret.result) {
            case Success:
                break;
            case NotStarted:
                return ConvertTCmd.Result.bus_not_started;
            case DeviceNotFound:
                return ConvertTCmd.Result.device_not_found;
            case ReadTimeout:
            case ReadOverrun:
            case ReadError:
            default:
                return ConvertTCmd.Result.communication_error;
        }

        final byte[] convertTCmdData = {
                'W', '0', '1', '4', '4', '\r'
        };

        final byte[] rbuf = new byte[2];

        ret = ((HA7S) busMaster).cmdWriteBlock(convertTCmdData, rbuf, getLogger());

        if (ret.result != HA7S.cmdResult.Success) {
            // All other returns are basically logic errors or real errors.
            return ConvertTCmd.Result.communication_error;
        }

        if (ret.readCount != 2) {
            if (getLogger() != null) {
                getLogger().logError(this.getClass().getSimpleName(), "Expected readCount of 2, got:" + ret.readCount);
            }
            return ConvertTCmd.Result.communication_error;
        }

        setResultData(ret.writeCTM);

        // Bonus...
        try {
            for (int i = 0; i < 10; i++) {
                ret = ((HA7S) busMaster).cmdReadBit(rbuf, getLogger());
                if (getLogger() != null) {
                    getLogger().logError(this.getClass().getSimpleName() + " O Cmd ", ret.result.name());
                }
                if ((ret.result == HA7S.cmdResult.Success) && (rbuf[0] == '1')) {
                    break;
                }
                Thread.sleep(100);
            }
        }
        catch (InterruptedException e) {
            if (getLogger() != null) {
                getLogger().logError(this.getClass().getSimpleName() + " O Cmd ", e);
            }
        }

        return ConvertTCmd.Result.success;
    }

    public void setResultData(long resultWriteCTM) {
        assert (result == Result.busy);
        this.resultWriteCTM = resultWriteCTM;
    }

}


