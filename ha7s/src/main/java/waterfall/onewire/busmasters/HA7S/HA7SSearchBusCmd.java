package waterfall.onewire.busmasters.HA7S;

import java.util.ArrayList;
import java.util.List;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.SearchBusCmd;

/**
 * Created by dwaterfa on 6/9/16.
 */
public class HA7SSearchBusCmd extends SearchBusCmd {

  // All (so !Alarm)
  public HA7SSearchBusCmd(HA7S ha7s) {
    super(ha7s, false);
  }

  // By familyCode
  public HA7SSearchBusCmd(HA7S ha7s, short familyCode) {
    super(ha7s, familyCode);
  }

  // By Alarm
  public HA7SSearchBusCmd(HA7S ha7s, boolean byAlarm) {
    super(ha7s, byAlarm);
  }

  public short getFamilyCode() {
    assert isByFamilyCode();
    return familyCode;
  }

  protected Result execute_internal() {
    assert (result == Result.busy);
    assert (resultData == null);

    ArrayList<byte[]> hexByteArrayList = new ArrayList<byte[]>();

    HA7S.cmdReturn ret;

    if (isByFamilyCode()) {
      ret = ((HA7S) busMaster)
          .cmdFamilySearch((byte) getFamilyCode(), hexByteArrayList, getDeviceLevelLogger());
    } else if (isByAlarm()) {
      ret = ((HA7S) busMaster).cmdConditionalSearch(hexByteArrayList, getDeviceLevelLogger());
    } else {
      ret = ((HA7S) busMaster).cmdSearchROM(hexByteArrayList, getDeviceLevelLogger());
    }

    switch (ret.result) {
      case NotStarted:
        return Result.bus_not_started;
      case Success:
        break;
      case DeviceNotFound:
      case ReadTimeout:
      case ReadOverrun:
      case ReadError:
      default:
        return Result.communication_error;
    }

    ArrayList<DSAddress> resultList = new ArrayList<>();

    try {
      for (byte[] hexByteArray : hexByteArrayList) {
        resultList.add(DSAddress.takeUncheckedHex(hexByteArray));
      }
    }
    catch (IllegalArgumentException e) {
      return Result.communication_error;
    }

    setResultData(ret.writeCTM, resultList);
    return Result.success;
  }

  public void setResultData(long resultWriteCTM, List<DSAddress> resultList) {
    assert (result == SearchBusCmd.Result.busy);
    this.resultData = new ResultData(resultList, resultWriteCTM);
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

