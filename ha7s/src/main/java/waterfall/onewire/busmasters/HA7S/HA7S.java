package waterfall.onewire.busmasters.HA7S;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.BiConsumer;
import waterfall.onewire.Convert;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.BusMaster;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.NotifySearchBusCmdHelper;
import waterfall.onewire.busmaster.NotifySearchBusCmdResult;
import waterfall.onewire.busmaster.SearchPusherByBusCmd;
import waterfall.onewire.busmasters.HA7S.HA7SSerial.HexByteArrayListResult;
import waterfall.onewire.busmasters.HA7S.HA7SSerial.ReadResult;

public class HA7S implements BusMaster {

  private HA7SSerial serialPort = null;
  private NotifySearchBusCmdHelper searchHelper = null;
  private NotifySearchBusCmdHelper searchByAlarmHelper = null;

  /*
  * Begin HA7S specific methods
  */
  public HA7S(HA7SSerial serial) {
    if ((serial == null) || (!serial.isStarted())) {
      throw new IllegalArgumentException("serial must non-null and started");
    }
    searchHelper = new NotifySearchBusCmdHelper(new SearchPusherByBusCmd(this, false), this);
    searchByAlarmHelper = new NotifySearchBusCmdHelper(new SearchPusherByBusCmd(this, true), this);
    serialPort = serial;
  }

  @Override
  public String getName() {
    if (serialPort != null) {
      return "HA7S on " + serialPort.getPortName();
    } else {
      // must have been stopped.
      return "HA7S on STOPPED";
    }
  }

  @Override
  public long getCurrentTimeMillis() {
    return System.currentTimeMillis();
  }

  @Override
  public SearchBusCmd querySearchBusCmd() {
    return new HA7S.SearchBusCmd(false);
  }

  @Override
  public SearchBusCmd querySearchBusByFamilyCmd(short familyCode) {
    return new HA7S.SearchBusCmd(familyCode);
  }

  @Override
  public SearchBusCmd querySearchBusByAlarmCmd() {
    return new HA7S.SearchBusCmd(true);
  }

  @Override
  public void scheduleNotifySearchBusCmd(NotifySearchBusCmdResult obj,
      boolean typeByAlarm, long minPeriodMSec) {
    if (serialPort == null) {
      throw new IllegalArgumentException("SNSBCR_BusMasterNotStarted");
    }
    if (!typeByAlarm) {
      searchHelper.scheduleSearchNotifyFor(obj, minPeriodMSec);
    } else {
      searchByAlarmHelper.scheduleSearchNotifyFor(obj, minPeriodMSec);
    }
  }

  @Override
  public void updateScheduledNotifySearchBusCmd(NotifySearchBusCmdResult obj, boolean typeByAlarm,
      long minPeriodMSec) {
    if (serialPort == null) {
      throw new IllegalArgumentException("USNSBC_BusMasterNotStarted");
    }
    if (!typeByAlarm) {
      searchHelper.updateScheduledSearchNotifyFor(obj, minPeriodMSec);
    } else {
      searchByAlarmHelper.updateScheduledSearchNotifyFor(obj, minPeriodMSec);
    }
  }

  @Override
  public void cancelScheduledNotifySearchBusCmd(NotifySearchBusCmdResult obj, boolean typeByAlarm) {
    if (serialPort == null) {
      throw new IllegalArgumentException("CSNSBC_BusMasterNotStarted");
    }
    if (!typeByAlarm) {
      searchHelper.cancelScheduledSearchNotifyFor(obj);
    } else {
      searchByAlarmHelper.cancelScheduledSearchNotifyFor(obj);
    }
  }

  @Override
  public ReadPowerSupplyCmd queryReadPowerSupplyCmd(DSAddress dsAddr) {
    return new HA7S.ReadPowerSupplyCmd(dsAddr);
  }

  @Override
  public ConvertTCmd queryConvertTCmd(DSAddress dsAddr) {
    return new HA7S.ConvertTCmd(dsAddr);
  }

  @Override
  public ReadScratchpadCmd queryReadScratchpadCmd(DSAddress dsAddr, short requestByteCount) {
    return new HA7S.ReadScratchpadCmd(dsAddr, requestByteCount);
  }

  @Override
  public WriteScratchpadCmd queryWriteScratchpadCmd(DSAddress dsAddr, byte[] writeData) {
    return new HA7S.WriteScratchpadCmd(dsAddr, writeData);
  }

  //
  // Private
  //

  /**
   * We stop our usage of the serial port, and cancel all pushes. It is the responsibility of the
   * HA7SBusMasterManager to stop the HA7Serial.
   */
  public synchronized void stopBus(Logger optLogger) {
    // may already be stopped
    if (serialPort != null) {
      searchHelper.cancelAllScheduledSearchNotifyFor();
      searchByAlarmHelper.cancelAllScheduledSearchNotifyFor();

      serialPort = null;
      searchHelper = null;
      searchByAlarmHelper = null;
    }
  }

  //
  // Exceptions
  //
  private static class BusFaultException extends RuntimeException {

    public BusFaultException(String message) {
      super(message);
    }
  }

  private static class BusDataException extends RuntimeException {

    public BusDataException(String message) {
      super(message);
    }
  }

  //
  // Class Command implementations
  //
  private class ConvertTCmd extends waterfall.onewire.busmaster.ConvertTCmd {

    private final byte[] selectCmd = new byte[]{'A',
        'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', '\r'};
    private final byte[] convertTCmd = {'W', '0', '1', '4', '4', '\r'};
    private final byte[] resetCmdData = {'R'};
    private final byte[] readBuf = new byte[16];
    private final byte[] readBitBuf = new byte[1];

    BiConsumer<byte[], Integer> checkAddressResultData = (rbuf, readCount) -> {
      if (readCount != 16) {
        throw new BusDataException("checkAddress wrong count:" + readCount);
      }
      for (int i = 0; i < 16; i++) {
        if (rbuf[i] != selectCmd[i + 1]) {
          throw new BusDataException("checkAddress wrong data i:" + i + " expected:"
              + selectCmd[i + 1] + " got:" + rbuf[i]);
        }
      }
    };

    BiConsumer<byte[], Integer> checkConvertTReturn = (rbuf, readCount) -> {
      if (readCount != 2) {
        throw new BusDataException("ConvertT failed cmd check read:" + readCount);
      }
      if ((rbuf[0] != convertTCmd[3]) || (rbuf[1] != convertTCmd[4])) {
        throw new BusDataException(
            "ConvertT failed cmd check expected:44 got:" + rbuf[0] + rbuf[1]);
      }
    };

    BiConsumer<byte[], Integer> checkReadBitReturn = (rbuf, readCount) -> {
      if (readCount != 1) {
        throw new BusFaultException("ReadBit failed check read:" + readCount);
      }
      if ((rbuf[0] != '0') && (rbuf[0] != '1')) {
        throw new BusFaultException("ReadBit failed check expected:0/1 got:" + rbuf[0]);
      }
    };

    BiConsumer<byte[], Integer> checkResetReturn = (rbuf, readCount) -> {
      if (readCount != 0) {
        throw new BusFaultException("Reset failed check read:" + readCount);
      }
    };

    private Object[][] cmdDataSequence = {
        // cmd data to send, read into buf, checkFunction, returned readResult
        {selectCmd, readBuf, checkAddressResultData, null}, // check against the dsAddress
        {convertTCmd, readBuf, checkConvertTReturn, null},
        {resetCmdData, readBuf, checkResetReturn, null}
    };

    private HA7SSerial.ReadResult results[] = new HA7SSerial.ReadResult[4];

    private ConvertTCmd(DSAddress dsAddr) {
      super(HA7S.this, dsAddr);
      dsAddr.copyHexBytesTo(selectCmd, 1);
    }

    protected ConvertTCmd.Result execute_internal() {
      assert (result == Result.cmdBusy);
      assert (resultWriteCTM == 0);

      try {
        if (serialPort == null) {
          throw new BusFaultException("bus was stopped");
        }

        for (Object[] sequence : cmdDataSequence) {
          byte[] cmdData = (byte[]) sequence[0];
          byte[] rBuf = (byte[]) sequence[1];
          BiConsumer<byte[], Integer> checkDataF = (BiConsumer<byte[], Integer>) sequence[2];

          HA7SSerial.ReadResult readResult = serialPort.writeReadTilCR(cmdData, rBuf,
              getDeviceLevelLogger());

          if (readResult.getError() != ReadResult.ErrorCode.RR_Success) {
            throw new BusDataException("writeReadTilCR:" + readResult.getError().name());
          }

          if (checkDataF != null) {
            checkDataF.accept(rBuf, readResult.getReadCount());
          }

          sequence[3] = readResult;
        }

        // We are interested in tracking the time the device started the temperature calculation
        long writeCTM = ((HA7SSerial.ReadResult) cmdDataSequence[1][3]).getPostWriteCTM();

        setResultData(writeCTM);

        return ConvertTCmd.Result.success;
      } catch (BusDataException e) {
        // TODO: Here is a potential place to imagine recovering from the bus fault and then
        // TODO: trying again.
        // TODO: logging with the HA7S (need more data in the throw errors)
        // TODO: resync with the HA7S (a bunch of resets and read until the queue is empty) and
        // TODO: then retry...
        logErrorInternal("dsAddr:" + dsAddr.toString() + "exception:" + e);
        return Result.busFault;
      } catch (BusFaultException e) {
        logErrorInternal("dsAddr:" + dsAddr.toString() + "exception:" + e);
        return Result.busFault;
      }
    }

    public void setResultData(long resultWriteCTM) {
      assert (result == Result.cmdBusy);
      this.resultWriteCTM = resultWriteCTM;
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

  private class ReadPowerSupplyCmd extends waterfall.onewire.busmaster.ReadPowerSupplyCmd {

    private final byte[] selectCmd = {'A',
        'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', '\r'};
    private final byte[] readPowerSupplyCmd = {'W', '0', '2', 'B', '4', 'F', 'F', '\r'};
    private final byte[] resetCmd = {'R'};
    private final byte[] readBuf = new byte[16];
    private final byte[] readPowerSupplyData = new byte[4];

    private BiConsumer<byte[], Integer> checkAddressResultData = (rbuf, readCount) -> {
      if (readCount != 16) {
        throw new BusDataException("checkAddress wrong count:" + readCount);
      }
      for (int i = 0; i < 16; i++) {
        if (rbuf[i] != selectCmd[i + 1]) {
          throw new BusDataException("checkAddress wrong data i:" + i + " expected:"
              + selectCmd[i + 1] + " got:" + rbuf[i]);
        }
      }
    };

    private BiConsumer<byte[], Integer> checkReadPowerSupplyReturn = (rbuf, readCount) -> {
      if (readCount != 4) {
        throw new BusDataException("ReadPowerSupply failed cmd check read:" + readCount);
      }
      if ((rbuf[0] != readPowerSupplyCmd[3]) || (rbuf[1] != readPowerSupplyCmd[4])) {
        throw new BusDataException(
            "ReadPowerSupply failed cmd check expected:B4 got:" + rbuf[0] + rbuf[1]);
      }
      if (((rbuf[2] != 'F') && (rbuf[2] != '0')) || ((rbuf[3] != 'F') && (rbuf[3] != '0'))) {
        throw new BusDataException(
            "ReadPowerSupply failed cmd check expected:00/FF got:" + rbuf[2] + rbuf[3]);
      }
    };

    private BiConsumer<byte[], Integer> checkResetReturn = (rbuf, readCount) -> {
      if (readCount != 0) {
        throw new BusFaultException("Reset failed check read:" + readCount);
      }
    };

    private Object[][] cmdDataSequence = {
        // cmd data to send, read into buf, checkFunction, returned readResult
        {selectCmd, readBuf, checkAddressResultData, null}, // check against the dsAddress
        {readPowerSupplyCmd, readPowerSupplyData, checkReadPowerSupplyReturn, null},
        {resetCmd, readBuf, checkResetReturn, null}
    };

    public ReadPowerSupplyCmd(DSAddress dsAddr) {
      super(HA7S.this, dsAddr);
      dsAddr.copyHexBytesTo(selectCmd, 1);
    }

    @Override
    protected ReadPowerSupplyCmd.Result execute_internal() {
      assert (result == Result.cmdBusy);
      assert (resultWriteCTM == 0);

      try {
        if (serialPort == null) {
          throw new BusFaultException("bus was stopped");
        }

        for (Object[] sequence : cmdDataSequence) {
          byte[] cmdData = (byte[]) sequence[0];
          byte[] rBuf = (byte[]) sequence[1];
          BiConsumer<byte[], Integer> checkDataF = (BiConsumer<byte[], Integer>) sequence[2];

          HA7SSerial.ReadResult readResult = serialPort.writeReadTilCR(cmdData, rBuf,
              getDeviceLevelLogger());

          if (readResult.getError() != ReadResult.ErrorCode.RR_Success) {
            throw new BusDataException("writeReadTilCR:" + readResult.getError().name());
          }

          if (checkDataF != null) {
            checkDataF.accept(rBuf, readResult.getReadCount());
          }

          sequence[3] = readResult;
        }

        // parasitic powered devices will pull the bus low
        boolean isParasitic = (readPowerSupplyData[3] == '0');

        long writeCTM = ((HA7SSerial.ReadResult) cmdDataSequence[1][3]).getPostWriteCTM();

        setResultData(writeCTM, isParasitic);

        return Result.success;
      } catch (BusDataException e) {
        // TODO: Here is a potential place to imagine recovering from the bus fault and then
        // TODO: trying again.
        // TODO: logging with the HA7S (need more data in the throw errors)
        // TODO: resync with the HA7S (a bunch of resets and read until the queue is empty) and
        // TODO: then retry...
        logErrorInternal("dsAddr:" + dsAddr.toString() + "exception:" + e);
        return Result.busFault;
      } catch (BusFaultException e) {
        logErrorInternal("dsAddr:" + dsAddr.toString() + "exception:" + e);
        return Result.busFault;
      }
    }

    @Override
    public void setResultData(long resultWriteCTM, boolean isParasitic) {
      assert (result == Result.cmdBusy);
      this.resultWriteCTM = resultWriteCTM;
      this.resultIsParasitic = isParasitic;
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

  private class ReadScratchpadCmd extends waterfall.onewire.busmaster.ReadScratchpadCmd {

    private final byte[] selectCmd = {'A',
        'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', '\r'};
    private byte[] readScratchpadCmd = null;
    private final byte[] readBuf = new byte[16];
    private byte[] readScratchpadResultData = null;

    private BiConsumer<byte[], Integer> checkAddressResultData = (rbuf, readCount) -> {
      if (readCount != 16) {
        throw new BusDataException("checkAddress wrong count:" + readCount);
      }
      for (int i = 0; i < 16; i++) {
        if (rbuf[i] != selectCmd[i + 1]) {
          throw new BusDataException("checkAddress wrong data i:" + i + " expected:"
              + selectCmd[i + 1] + " got:" + rbuf[i]);
        }
      }
    };

    private BiConsumer<byte[], Integer> checkReadScratchpadReturn = (rbuf, readCount) -> {
      final int expectedReadCount = ((requestByteCount + 1) * 2);
      if (readCount != expectedReadCount) {
        throw new BusDataException(
            "ReadScratchpad failed cmd check expected:" + expectedReadCount + " read:" + readCount);
      }
      if ((rbuf[0] != readScratchpadCmd[3]) || (rbuf[1] != readScratchpadCmd[4])) {
        throw new BusDataException(
            "ReadScratchpad failed cmd check expected:BE got:" + (char) rbuf[0] + (char) rbuf[1]);
      }
      for (int i = 2; i < expectedReadCount; i++) {
        if (!(((readScratchpadResultData[i] >= '0') && (readScratchpadResultData[i] <= '9')) ||
            ((readScratchpadResultData[i] >= 'A') && (readScratchpadResultData[i] <= 'F')))) {
          throw new BusDataException(
              "checkReadScratchpad not hex data i:" + i + " got:" + readScratchpadResultData[i]);
        }
      }
    };

    private Object[][] cmdDataSequence = null;

    public ReadScratchpadCmd(DSAddress dsAddr, short requestByteCount) {
      super(HA7S.this, dsAddr, requestByteCount);
      dsAddr.copyHexBytesTo(selectCmd, 1);

      int totalLength = (5 + (requestByteCount * 2) + 1);
      readScratchpadCmd = new byte[totalLength];
      int i = 0;
      readScratchpadCmd[i++] = 'W';
      readScratchpadCmd[i++] = Convert
          .fourBitsToHex(((int) (requestByteCount + 1) & 0xff) >> 4);
      readScratchpadCmd[i++] = Convert
          .fourBitsToHex(((int) (requestByteCount + 1) & 0xff) & 0xf);
      readScratchpadCmd[i++] = 'B';
      readScratchpadCmd[i++] = 'E';
      while (i < (totalLength - 1)) {
        readScratchpadCmd[i++] = 'F';
      }
      readScratchpadCmd[i] = '\r';

      readScratchpadResultData = new byte[(requestByteCount + 1) * 2];

      cmdDataSequence = new Object[][]{
          // cmd data to send, read into buf, checkFunction, returned readResult
          {selectCmd, readBuf, checkAddressResultData, null}, // check against the dsAddress
          {readScratchpadCmd, readScratchpadResultData, checkReadScratchpadReturn, null},
      };
    }

    protected ReadScratchpadCmd.Result execute_internal() {
      assert (result == Result.cmdBusy);
      assert (resultData == null);
      assert (resultWriteCTM == 0);

      try {
        if (serialPort == null) {
          throw new BusFaultException("bus was stopped");
        }

        final int hexByteCount = (requestByteCount * 2);

        Arrays.fill(readScratchpadCmd, 5, (5 + hexByteCount), (byte) 'F');

        for (Object[] sequence : cmdDataSequence) {
          byte[] cmdData = (byte[]) sequence[0];
          byte[] rBuf = (byte[]) sequence[1];
          BiConsumer<byte[], Integer> checkDataF = (BiConsumer<byte[], Integer>) sequence[2];

          HA7SSerial.ReadResult readResult = serialPort.writeReadTilCR(cmdData, rBuf,
              getDeviceLevelLogger());

          if (readResult.getError() != ReadResult.ErrorCode.RR_Success) {
            throw new BusDataException("writeReadTilCR:" + readResult.getError().name());
          }

          if (checkDataF != null) {
            checkDataF.accept(rBuf, readResult.getReadCount());
          }

          sequence[3] = readResult;
        }

        byte[] resultData = new byte[requestByteCount];
        Convert.hexToByte(readScratchpadResultData, 2, hexByteCount, resultData, 0);
        byte[] resultHexData = Arrays.copyOfRange(readScratchpadResultData, 2, (2 + hexByteCount));
        long writeCTM = ((HA7SSerial.ReadResult) cmdDataSequence[1][3]).getPostWriteCTM();

        setResultData(writeCTM, resultData, resultHexData);
        return Result.success;

      } catch (BusDataException e) {
        // TODO: Here is a potential place to imagine recovering from the bus fault and then
        // TODO: trying again.
        // TODO: logging with the HA7S (need more data in the throw errors)
        // TODO: resync with the HA7S (a bunch of resets and read until the queue is empty) and
        // TODO: then retry...
        logErrorInternal("dsAddr:" + dsAddr.toString() + "exception:" + e);
        return Result.busFault;
      } catch (BusFaultException e) {
        logErrorInternal("dsAddr:" + dsAddr.toString() + "exception:" + e);
        return Result.busFault;
      }
    }

    public void setResultData(long resultWriteCTM, byte[] resultData, byte[] resultHexData) {
      assert (result == Result.cmdBusy);
      this.resultWriteCTM = resultWriteCTM;
      this.resultData = resultData;
      this.resultHexData = resultHexData;
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

  private class WriteScratchpadCmd extends waterfall.onewire.busmaster.WriteScratchpadCmd {

    private final byte[] selectCmd = {'A',
        'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', '\r'};
    private byte[] writeScratchpadCmd = null;
    private final byte[] readBuf = new byte[16];

    private BiConsumer<byte[], Integer> checkAddressResultData = (rbuf, readCount) -> {
      if (readCount != 16) {
        throw new BusDataException("checkAddress wrong count:" + readCount);
      }
      for (int i = 0; i < 16; i++) {
        if (rbuf[i] != selectCmd[i + 1]) {
          throw new BusDataException("checkAddress wrong data i:" + i + " expected:"
              + selectCmd[i + 1] + " got:" + rbuf[i]);
        }
      }
    };

    private Object[][] cmdDataSequence = null;

    public WriteScratchpadCmd(DSAddress dsAddr, byte[] writeData) {
      super(HA7S.this, dsAddr, writeData);
      dsAddr.copyHexBytesTo(selectCmd, 1);

      cmdDataSequence = new Object[][]{
          // cmd data to send, read into buf, checkFunction, returned readResult
          {selectCmd, readBuf, checkAddressResultData, null}, // check against the dsAddress
          {writeScratchpadCmd, readBuf, null, null},
      };
    }

    public void updateWriteData(byte[] writeData) {
      super.updateWriteData(writeData);

      int totalLength = (5 + (writeData.length * 2) + 1);
      writeScratchpadCmd = new byte[totalLength];
      int i = 0;
      writeScratchpadCmd[i++] = 'W';
      writeScratchpadCmd[i++] = Convert
          .fourBitsToHex(((int) (writeData.length + 1) & 0xff) >> 4);
      writeScratchpadCmd[i++] = Convert
          .fourBitsToHex(((int) (writeData.length + 1) & 0xff) & 0xf);
      writeScratchpadCmd[i++] = '4';
      writeScratchpadCmd[i++] = 'E';
      for (int j = 0; j < writeData.length; j++) {
        writeScratchpadCmd[i++] = Convert
            .fourBitsToHex(((int) (writeData[j]) & 0xff) >> 4);
        writeScratchpadCmd[i++] = Convert
            .fourBitsToHex(((int) (writeData[j]) & 0xff) & 0xf);
      }
      writeScratchpadCmd[i] = '\r';
    }

    protected WriteScratchpadCmd.Result execute_internal() {
      assert (result == Result.cmdBusy);
      assert (resultWriteCTM == 0);

      try {
        if (serialPort == null) {
          throw new BusFaultException("bus was stopped");
        }

        for (Object[] sequence : cmdDataSequence) {
          byte[] cmdData = (byte[]) sequence[0];
          byte[] rBuf = (byte[]) sequence[1];
          BiConsumer<byte[], Integer> checkDataF = (BiConsumer<byte[], Integer>) sequence[2];

          HA7SSerial.ReadResult readResult = serialPort.writeReadTilCR(cmdData, rBuf,
              getDeviceLevelLogger());

          if (readResult.getError() != ReadResult.ErrorCode.RR_Success) {
            throw new BusDataException("writeReadTilCR:" + readResult.getError().name());
          }

          if (checkDataF != null) {
            checkDataF.accept(rBuf, readResult.getReadCount());
          }

          sequence[3] = readResult;
        }

        long writeCTM = ((HA7SSerial.ReadResult) cmdDataSequence[1][3]).getPostWriteCTM();

        setResultData(writeCTM);
        return Result.success;

      } catch (BusDataException e) {
        // TODO: Here is a potential place to imagine recovering from the bus fault and then
        // TODO: trying again.
        // TODO: logging with the HA7S (need more data in the throw errors)
        // TODO: resync with the HA7S (a bunch of resets and read until the queue is empty) and
        // TODO: then retry...
        logErrorInternal("dsAddr:" + dsAddr.toString() + "exception:" + e);
        return Result.busFault;
      } catch (BusFaultException e) {
        logErrorInternal("dsAddr:" + dsAddr.toString() + "exception:" + e);
        return Result.busFault;
      }
    }

    public void setResultData(long resultWriteCTM) {
      assert (result == Result.cmdBusy);
      this.resultWriteCTM = resultWriteCTM;
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

  private class SearchBusCmd extends waterfall.onewire.busmaster.SearchBusCmd {

    // All (so !Alarm)
    public SearchBusCmd() {
      super(HA7S.this, false);
    }

    // By familyCode
    public SearchBusCmd(short familyCode) {
      super(HA7S.this, familyCode);
    }

    // By Alarm
    public SearchBusCmd(boolean byAlarm) {
      super(HA7S.this, byAlarm);
    }

    protected Result execute_internal() {
      assert (result == Result.busy);
      assert (resultData == null);

      if (serialPort == null) {
        throw new BusFaultException("bus was stopped");
      }

      final byte[] cmdSearchROM = {'S'};
      final byte[] cmdSearchROMNext = {'s'};
      final byte[] cmdAlarmSearch = {'C'};
      final byte[] cmdAlarmSearchNext = {'c'};
      final byte[] cmdFamilySearch = {'F', '0', '0'};
      final byte[] cmdFamilySearchNext = {'f'};

      byte[] firstCmd = null;
      byte[] nextCmd = null;

      if (isByFamilyCode()) {
        cmdFamilySearch[1] = Convert.fourBitsToHex(familyCode >> 4);
        cmdFamilySearch[2] = Convert.fourBitsToHex(familyCode & 0xf);
        firstCmd = cmdFamilySearch;
        nextCmd = cmdFamilySearchNext;
      } else if (isByAlarm()) {
        firstCmd = cmdAlarmSearch;
        nextCmd = cmdAlarmSearchNext;
      } else {
        firstCmd = cmdSearchROM;
        nextCmd = cmdSearchROMNext;
      }

      ArrayList<DSAddress> resultList = new ArrayList<>();
      long firstPostWriteCTM = 0;

      int index = 0;
      while (true) {
        byte[] rHexBuf = new byte[16];

        HA7SSerial.ReadResult readResult = serialPort
            .writeReadTilCR(((index == 0) ? firstCmd : nextCmd), rHexBuf, getLogger());

        if (readResult.getError() != ReadResult.ErrorCode.RR_Success) {
          throw new BusDataException("writeReadTilCR:" + readResult.getError().name());
        }

        if (index == 0) {
          firstPostWriteCTM = readResult.getPostWriteCTM();
        }

        if (readResult.getReadCount() == 0) {
          break;
        }

        if (readResult.getReadCount() != 16) {
          throw new BusDataException(
              "Search failed read count expected:16 got:" + readResult.getReadCount());
        }

        if (!HA7SSerial.isValidUpperCaseHex(rHexBuf, readResult.getReadCount())) {
          throw new BusDataException("Not hex bytes");
        }

        resultList.add(DSAddress.takeUnCRCCheckedHex(rHexBuf));
        index++;
      }

      ResultData _resultData = new ResultData(resultList, firstPostWriteCTM);

      if (isByAlarm()) {
        searchByAlarmHelper.notifySearchResult(_resultData);
      } else if (!isByFamilyCode()) {
        searchHelper.notifySearchResult(_resultData);
      }

      setResultData(_resultData);

      return Result.success;
    }

    public void setResultData(ResultData resultData) {
      assert (result == SearchBusCmd.Result.busy);
      this.resultData = resultData;
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

}
