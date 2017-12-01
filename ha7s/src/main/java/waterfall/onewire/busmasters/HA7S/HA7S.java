package waterfall.onewire.busmasters.HA7S;

import java.util.ArrayList;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.BusMaster;
import waterfall.onewire.busmaster.ConvertTCmd;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.NotifySearchBusCmdHelper;
import waterfall.onewire.busmaster.NotifySearchBusCmdResult;
import waterfall.onewire.busmaster.ReadPowerSupplyCmd;
import waterfall.onewire.busmaster.ReadScratchpadCmd;
import waterfall.onewire.busmaster.SearchBusCmd;
import waterfall.onewire.busmaster.SearchPusherByBusCmd;
import waterfall.onewire.busmaster.StartBusCmd;
import waterfall.onewire.busmaster.StopBusCmd;
import waterfall.onewire.busmasters.HA7S.HA7SSerial.HexByteArrayListResult;
import waterfall.onewire.busmasters.HA7S.HA7SSerial.NoDataResult;

public class HA7S implements BusMaster {

  private Boolean started = null;
  private HA7SSerial serialPort = null;
  private NotifySearchBusCmdHelper searchHelper = null;
  private NotifySearchBusCmdHelper searchByAlarmHelper = null;

  /*
  * Begin HA7S specific methods
  */
  public HA7S(HA7SSerial serial) {
    if (serial == null) {
      throw new IllegalArgumentException("serial must non-null");
    }
    searchHelper = new NotifySearchBusCmdHelper(new SearchPusherByBusCmd(this, false), this);
    searchByAlarmHelper = new NotifySearchBusCmdHelper(new SearchPusherByBusCmd(this, true), this);
    serialPort = serial;
  }

  @Override
  public String getName() {
    return "HA7S on " + serialPort.getPortName();
  }

  @Override
  public long getCurrentTimeMillis() {
    return System.currentTimeMillis();
  }

  @Override
  public boolean getIsStarted() {
    return ((started != null) && started);
  }

  @Override
  public StartBusCmd queryStartBusCmd() {
    return new HA7SStartBusCmd(this);
  }

  @Override
  public StopBusCmd queryStopBusCmd() {
    return new HA7SStopBusCmd(this);
  }

  @Override
  public SearchBusCmd querySearchBusCmd() {
    return new HA7SSearchBusCmd(this, false);
  }

  @Override
  public SearchBusCmd querySearchBusByFamilyCmd(short familyCode) {
    return new HA7SSearchBusCmd(this, familyCode);
  }

  @Override
  public SearchBusCmd querySearchBusByAlarmCmd() {
    return new HA7SSearchBusCmd(this, true);
  }

  @Override
  public ScheduleNotifySearchBusCmdResult scheduleNotifySearchBusCmd(NotifySearchBusCmdResult obj,
      boolean typeByAlarm, long minPeriodMSec) {
    if (!getIsStarted()) {
      return ScheduleNotifySearchBusCmdResult.SNSBCR_BusMasterNotStarted;
    }
    if (!typeByAlarm) {
      return searchHelper.scheduleSearchNotifyFor(obj, minPeriodMSec);
    } else {
      return searchByAlarmHelper.scheduleSearchNotifyFor(obj, minPeriodMSec);
    }
  }

  @Override
  public UpdateScheduledNotifySearchBusCmdResult updateScheduledNotifySearchBusCmd(
      NotifySearchBusCmdResult obj, boolean typeByAlarm, long minPeriodMSec) {
    if (!getIsStarted()) {
      return UpdateScheduledNotifySearchBusCmdResult.USNSBC_BusMasterNotStarted;
    }
    if (!typeByAlarm) {
      return searchHelper.updateScheduledSearchNotifyFor(obj, minPeriodMSec);
    } else {
      return searchByAlarmHelper.updateScheduledSearchNotifyFor(obj, minPeriodMSec);
    }
  }

  @Override
  public CancelScheduledNotifySearchBusCmdResult cancelScheduledNotifySearchBusCmd(
      NotifySearchBusCmdResult obj, boolean typeByAlarm) {
    if (!getIsStarted()) {
      return CancelScheduledNotifySearchBusCmdResult.CSNSBC_BusMasterNotStarted;
    }
    if (!typeByAlarm) {
      return searchHelper.cancelScheduledSearchNotifyFor(obj);
    } else {
      return searchByAlarmHelper.cancelScheduledSearchNotifyFor(obj);
    }
  }

  public void searchBusCmdExecuteCallback(SearchBusCmd cmd) {
    if ((cmd.getBusMaster() == this) && (cmd.getResult() == SearchBusCmd.Result.success)) {
      if (cmd.isByAlarm()) {
        searchByAlarmHelper.notifySearchResult(cmd.getResultData());
      } else if (!cmd.isByFamilyCode()) {
        searchHelper.notifySearchResult(cmd.getResultData());
      }
    }
  }

  @Override
  public ReadPowerSupplyCmd queryReadPowerSupplyCmd(DSAddress dsAddr) {
    return new HA7SReadPowerSupplyCmd(this, dsAddr);
  }

  @Override
  public ConvertTCmd queryConvertTCmd(DSAddress dsAddr) {
    return new HA7SConvertTCmd(this, dsAddr);
  }

  @Override
  public ReadScratchpadCmd queryReadScratchpadCmd(DSAddress dsAddr, short requestByteCount) {
    return new HA7SReadScratchpadCmd(this, dsAddr, requestByteCount);
  }

  public synchronized StartBusCmd.Result executeStartBusCmd(HA7SStartBusCmd cmd) {
    final byte[] resetBusCmd = {'R'};

    StartBusCmd.Result result = null;

    if (started != null) {
      result = StartBusCmd.Result.already_started;
      return result;
    }

    HA7SSerial.StartResult startResult = serialPort.start(cmd.getDeviceLevelLogger());

    if (startResult != HA7SSerial.StartResult.SR_Success) {
      return StartBusCmd.Result.communication_error;
    }

    byte[] rbuf = new byte[8];

    HA7SSerial.ReadResult readResult = serialPort
        .writeReadTilCR(resetBusCmd, rbuf, cmd.getDeviceLevelLogger());

    if ((readResult.getError() == HA7SSerial.ReadResult.ErrorCode.RR_Success) &&
        (readResult.getReadCount() == 1) &&
        (rbuf[0] == 0x07)) {
      // This can occur during development when when the first thing read after open is
      // 0x07 0x0D. So we try this again once.
      readResult = serialPort
          .writeReadTilCR(resetBusCmd, rbuf, cmd.getDeviceLevelLogger());
    }

    if ((readResult.getError() != HA7SSerial.ReadResult.ErrorCode.RR_Success) ||
        (readResult.getReadCount() != 0)) {
      cmd.logErrorInternal(
          readResult.getError().name() + " readCount:" + readResult.getReadCount());

      cmd.logErrorInternal(readResult.getError().name() + " stopping port");
      HA7SSerial.StopResult stopResult = serialPort.stop((Logger) cmd);
      cmd.logErrorInternal(readResult.getError().name() + " stop result:" + stopResult.name());

      return StartBusCmd.Result.communication_error;
    }

    started = new Boolean(true);

    return StartBusCmd.Result.started;
  }

  public synchronized StopBusCmd.Result executeStopBusCmd(HA7SStopBusCmd cmd) {

    StopBusCmd.Result result = null;

    if (started == null) {
      return StopBusCmd.Result.not_started;
    }

    HA7SSerial.StopResult stopResult = serialPort.stop(cmd.getDeviceLevelLogger());

    if (stopResult != HA7SSerial.StopResult.SR_Success) {
      return StopBusCmd.Result.communication_error;
    }

    started = null;
    searchHelper.cancelAllScheduledSearchNotifyFor();
    searchByAlarmHelper.cancelAllScheduledSearchNotifyFor();
    return StopBusCmd.Result.stopped;
  }

  public cmdReturn cmdSearchROM(ArrayList<byte[]> hexByteArrayList, Logger optLogger) {
    final String logContext = "cmdSearchROM";

    HexByteArrayListResult result = serialPort.cmdSearch(optLogger);
    if (!result.isSuccess()) {
      if (optLogger != null) {
        optLogger.logError(logContext, logContext + " error:" + result.getErrorMsg());
      }
      return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
    }

    hexByteArrayList.addAll(result.getValue());

    return logAndReturn(new cmdReturn(hexByteArrayList.size(), result.getWriteCTM()), optLogger,
        logContext);
  }

  public cmdReturn cmdFamilySearch(byte familyCode, ArrayList<byte[]> hexByteArrayList,
      Logger optLogger) {
    final String logContext = "cmdFamilySearch";

    HexByteArrayListResult result = serialPort.cmdFamilySearch(familyCode, optLogger);
    if (!result.isSuccess()) {
      if (optLogger != null) {
        optLogger.logError(logContext, logContext + " error:" + result.getErrorMsg());
      }
      return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
    }

    hexByteArrayList.addAll(result.getValue());

    return logAndReturn(new cmdReturn(hexByteArrayList.size(), result.getWriteCTM()), optLogger,
        logContext);
  }

  public cmdReturn cmdConditionalSearch(ArrayList<byte[]> hexByteArrayList, Logger optLogger) {
    final String logContext = "cmdConditionalSearch";

    HexByteArrayListResult result = serialPort.cmdConditionalSearch(optLogger);
    if (!result.isSuccess()) {
      if (optLogger != null) {
        optLogger.logError(logContext, logContext + " error:" + result.getErrorMsg());
      }
      return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
    }

    hexByteArrayList.addAll(result.getValue());

    return logAndReturn(new cmdReturn(hexByteArrayList.size(), result.getWriteCTM()), optLogger,
        logContext);
  }

  private cmdReturn logAndReturn(cmdReturn ret, Logger optLogger, String logContext) {
    if (optLogger != null) {
      optLogger.logError(logContext, ret.result.name());
    }
    return ret;
  }

  public cmdReturn cmdAddressSelect(DSAddress dsAddr, Logger optLogger) {
    final String logContext = "cmdAddressSelect";

    if ((started == null) || (serialPort == null)) {
      return logAndReturn(new cmdReturn(cmdResult.NotStarted), optLogger, logContext);
    }

    NoDataResult result = serialPort.cmdAddress(dsAddr, optLogger);
    if (!result.isSuccess()) {
      if (optLogger != null) {
        optLogger.logError(logContext, "cmdAddress error:" + result.getErrorMsg());
      }
      return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
    }

    return logAndReturn(new cmdReturn(0, result.getWriteCTM()), optLogger, logContext);
  }

  public cmdReturn cmdReadBit(byte[] rbuf, Logger optLogger) {
    final byte[] wbuf = new byte[]{'O'};
    final String logContext = "cmdReadBit";

    if ((started == null) || (serialPort == null)) {
      return logAndReturn(new cmdReturn(cmdResult.NotStarted), optLogger, logContext);
    }

    HA7SSerial.ReadResult readResult = serialPort.writeReadTilCR(wbuf, rbuf, optLogger);

    switch (readResult.getError()) {
      case RR_Success:
        break;

      case RR_ReadTimeout:
        return logAndReturn(new cmdReturn(cmdResult.ReadTimeout), optLogger, logContext);

      case RR_ReadOverrun:
        return logAndReturn(new cmdReturn(cmdResult.ReadOverrun), optLogger, logContext);

      case RR_Error:
        return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);

      default:
        if (optLogger != null) {
          optLogger
              .logError(logContext,
                  "unknown HA7SSerial.ReadResult:" + readResult.getError().name());
        }
        return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
    }

    if (readResult.getReadCount() != 1) {
      if (optLogger != null) {
        optLogger.logError(logContext, "expected 1, readCount:" + readResult.getReadCount());
      }
      return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
    }

    if ((rbuf[0] != '0') && (rbuf[0] != '1')) {
      if (optLogger != null) {
        optLogger.logError(logContext, "expected 0 or 1, got:" + Byte.toString(rbuf[0]));
      }
      return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
    }

    return logAndReturn(new cmdReturn(0, readResult.getPostWriteCTM()), optLogger, logContext);
  }

  public cmdReturn cmdWriteBlock(byte[] wbuf, byte[] rbuf, Logger optLogger) {
    final String logContext = "cmdWriteBlock";

    if ((started == null) || (serialPort == null)) {
      return logAndReturn(new cmdReturn(cmdResult.NotStarted), optLogger, logContext);
    }

    HA7SSerial.ReadResult readResult = serialPort
        .writeReadTilCR(wbuf, rbuf, optLogger);

    switch (readResult.getError()) {
      case RR_Success:
        return logAndReturn(new cmdReturn(readResult.getReadCount(), readResult.getPostWriteCTM()),
            optLogger,
            logContext);

      case RR_ReadTimeout:
        return logAndReturn(new cmdReturn(cmdResult.ReadTimeout), optLogger, logContext);

      case RR_ReadOverrun:
        return logAndReturn(new cmdReturn(cmdResult.ReadOverrun), optLogger, logContext);

      case RR_Error:
        return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);

      default:
        if (optLogger != null) {
          optLogger
              .logError(logContext,
                  "unknown HA7SSerial.ReadResult:" + readResult.getError().name());
        }
        return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
    }
  }

  public enum cmdResult {
    Success,
    NotStarted,
    DeviceNotFound,
    ReadTimeout,
    ReadOverrun,
    ReadError
  }

  public static class cmdReturn {

    public cmdResult result;
    public int readCount;
    public long writeCTM;

    public cmdReturn(cmdResult result) {
      this.result = result;
      this.readCount = 0;
      this.writeCTM = 0;
    }

    public cmdReturn(int readCount, long writeCTM) {
      this.result = cmdResult.Success;
      this.readCount = readCount;
      this.writeCTM = writeCTM;
    }
  }

}
