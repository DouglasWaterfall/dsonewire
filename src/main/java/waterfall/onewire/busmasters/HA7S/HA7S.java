package waterfall.onewire.busmasters.HA7S;

import com.dalsemi.onewire.utils.Address;
import waterfall.onewire.Convert;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.*;

import java.util.*;

public class HA7S implements BusMaster {

    private String portDevName;
    private Boolean started = null;
    private HA7SSerial serialPort = null;

    private class SearchBusNotifyData extends TimerTask {
        private boolean isAlarmSearch;
        private HashMap<Object, Long> notifyMap = null;
        private SearchBusCmd searchBusCmd = null;
        private Thread searchBusCmdThread = null;
        private Timer timer = null;
        private long currentMinPeriodMs = -1;

        private class Notify implements Runnable {
            private Object[] objs;
            private List<String> result;
            private boolean isAlarmSearch;

            public Notify(Object[] objs, List<String> result, boolean isAlarmSearch) {
                this.objs = objs;
                this.result = result;
                this.isAlarmSearch = isAlarmSearch;
            }

            public void run() {
                for (Object obj : objs) {
                    try {
                        if (isAlarmSearch) {
                            SearchBusByAlarmNotify.class.cast(obj).notify(HA7S.this, result);
                        } else {
                            SearchBusNotify.class.cast(obj).notify(HA7S.this, result);
                        }
                    }
                    catch (Exception e) {
                    }
                }
            }
        }

        // Called from our outerclass
        public SearchBusNotifyData(boolean isAlarmSearch) {
            this.isAlarmSearch = isAlarmSearch;
        }

        // Called from our outerclass
        public synchronized boolean scheduleSearchNotifyFor(Object obj, long minPeriodMs) {
            if ((obj == null) ||
                (!isAlarmSearch && (!SearchBusNotify.class.isInstance(obj))) ||
                (isAlarmSearch && (!SearchBusByAlarmNotify.class.isInstance(obj))) ||
                ((notifyMap != null) && (notifyMap.get(obj) != null)) ||
                (minPeriodMs <= 0)) {
                return false;
            }

            if (notifyMap == null) {
                notifyMap = new HashMap<Object, Long>();
            }
            notifyMap.put(obj, new Long(minPeriodMs));

            if ((notifyMap.size() == 1) || (minPeriodMs < currentMinPeriodMs)) {
                // looks like we might be the ones driving the current search rate. We will need
                // to recalculate to see what should happen next.
                recalculateTimer();
            }

            return true;
        }

        // Called from our outerclass
        public synchronized boolean cancelSearchNotifyFor(Object obj) {
            Long minPeriodMs;

            if ((obj == null) || ((minPeriodMs = notifyMap.get(obj)) == null)) {
              return false;
            }

            notifyMap.remove(obj);

            if (minPeriodMs == currentMinPeriodMs) {
                // looks like we might be the ones driving the current search rate. We will need
                // to recalculate to see what should happen next.
                recalculateTimer();
            }

            return true;
        }

        private void recalculateTimer() {
            if (notifyMap.size() == 0) {
                // We may be removed the last entry. If we have a timer then we need to disable
                // it and let it and the current searchBusCmd go so it can be garbage collected.
                // We do not touch the searchBusCmdThread since there may be a thread active
                // there - it will reset it when it returns.
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                    currentMinPeriodMs = -1;
                    searchBusCmd = null;
                }
            }
            else {
                // Somebody is waiting. Lets find out how long we need to wait
                long minPeriodMs = Long.MAX_VALUE;
                for (Long l : notifyMap.values()) {
                    if (l < minPeriodMs) {
                        minPeriodMs = l.longValue();
                    }
                }

                if (timer == null) {
                    // We must have just added one entry. Start things up.
                    searchBusCmd = (isAlarmSearch ? querySearchBusByAlarmCmd(Logger.LogLevel.CmdOnlyLevel()) : querySearchBusCmd(Logger.LogLevel.CmdOnlyLevel()));
                    currentMinPeriodMs = minPeriodMs;
                    timer = new Timer(true);
                    timer.schedule(this, 0, currentMinPeriodMs);
                }
                else if (minPeriodMs != currentMinPeriodMs) {
                    // We have an active timer so we need to start over. Note that this means
                    // we will automatically start a search immediately unless we goto the
                    // effort to figure out how far we have to go to our meet the next period.
                    // If we just returned a result 5ms earlier there is no point in asking again.
                    timer.cancel();
                    currentMinPeriodMs = minPeriodMs;
                    timer = new Timer(true);
                    timer.schedule(this, 0, currentMinPeriodMs);
                }
            }
        }

        // Called from our outerclass only if the resultList is non-empty.
        public synchronized void notifySearchResult(List<String> resultList) {
            if (notifyMap.size() > 0) {
                Object[] objs = new Object[notifyMap.size()];
                int i = 0;
                for (Object obj : notifyMap.keySet()) {
                    objs[i++] = obj;
                }

                if (Thread.currentThread() == searchBusCmdThread) {
                    // we launched the search so we can notify on our own thread.
                    (new Notify(objs, resultList, isAlarmSearch)).run();
                }
                else {
                    // Let us be nice and do the notify on a different thread than who launched
                    // the command.
                    Thread notifyThread = new Thread(new Notify(objs, resultList, isAlarmSearch));
                    notifyThread.setDaemon(true);
                    notifyThread.start();

                    // we might be called by our own cmd or someone else's instance. We are only
                    // interested here if it was someone else since that can potentially change
                    // our timing of when we need to ask again.
                    //
                    // But that is an optimization (delaying our notification) we'll leave to
                    // another time.
                }
            }
        }

        // Called as TimerTask from Timer.
        public void run() {
            synchronized (this) {
                if (searchBusCmdThread != null) {
                    return;
                }
                searchBusCmdThread = Thread.currentThread();
            }

            try {
                // The command will internally call back to this class when it is successful, so
                // all we need to do here is just push.
                searchBusCmd.execute();
            }
            finally {
                synchronized (this) {
                    searchBusCmdThread = null;
                }
            }
        }


    };

    private SearchBusNotifyData searchBusNotifyData = null;
    private SearchBusNotifyData searchBusByAlarmNotifyData = null;

    private static final long defaultTimeoutMSec = 5000;

    public String getName() {
        return "HA7S on " + ((portDevName != null) ? portDevName : "no device");
    }

    public long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    public boolean getIsStarted() {
        return ((started != null) && started.booleanValue());
    }

    public StartBusCmd queryStartBusCmd(Logger.LogLevel logLevel) {
        return new HA7SStartBusCmd(this, logLevel);
    }

    public StopBusCmd queryStopBusCmd(Logger.LogLevel logLevel) {
        return new HA7SStopBusCmd(this, logLevel);
    }

    public SearchBusCmd querySearchBusCmd(Logger.LogLevel logLevel) {
        return new HA7SSearchBusCmd(this, false, logLevel);
    }

    public boolean scheduleSearchNotifyFor(SearchBusNotify obj, long minPeriodMs) {
       return searchBusNotifyData.scheduleSearchNotifyFor(obj, minPeriodMs);
    }

    public boolean cancelSearchNotifyFor(SearchBusNotify obj) {
        return searchBusNotifyData.cancelSearchNotifyFor(obj);
    }

    public SearchBusCmd querySearchBusByFamilyCmd(short familyCode, Logger.LogLevel logLevel) {
        return new HA7SSearchBusCmd(this, familyCode, logLevel);
    }

    public SearchBusCmd querySearchBusByAlarmCmd(Logger.LogLevel logLevel) {
        return new HA7SSearchBusCmd(this, true, logLevel);
    }

    public boolean scheduleAlarmSearchNotifyFor(SearchBusByAlarmNotify obj, long minPeriodMs) {
        return searchBusByAlarmNotifyData.scheduleSearchNotifyFor(obj, minPeriodMs);
    }

    public boolean cancelAlarmSearchNotifyFor(SearchBusByAlarmNotify obj) {
        return searchBusByAlarmNotifyData.cancelSearchNotifyFor(obj);
    }

    public ReadPowerSupplyCmd queryReadPowerSupplyCmd(DSAddress dsAddr, Logger.LogLevel logLevel) {
        return new HA7SReadPowerSupplyCmd(this, dsAddr, logLevel);
    }

    public ConvertTCmd queryConvertTCmd(DSAddress dsAddr, Logger.LogLevel logLevel) {
        return new HA7SConvertTCmd(this, dsAddr, logLevel);
    }

    public ReadScratchpadCmd queryReadScratchpadCmd(DSAddress dsAddr, short requestByteCount, Logger.LogLevel logLevel) {
        return new HA7SReadScratchpadCmd(this, dsAddr, requestByteCount, logLevel);
    }

    /*
    * Begin HA7S specific methods
    */
    public HA7S(String portDevName) {
        this.portDevName = portDevName;
        searchBusNotifyData = new SearchBusNotifyData(false);
        searchBusByAlarmNotifyData = new SearchBusNotifyData(true);
    }

    public synchronized StartBusCmd.Result executeStartBusCmd(HA7SStartBusCmd cmd) {
        final byte[] resetBusCmd = {'R'};

        StartBusCmd.Result result = null;

        if (started != null) {
            result = StartBusCmd.Result.already_started;
            return result;
        }

        if (serialPort == null) {
            serialPort = new JSSC(portDevName);
        }

        HA7SSerial.StartResult startResult = serialPort.start(cmd.getDeviceLevelLogger());

        if (startResult != HA7SSerial.StartResult.SR_Success) {
            return StartBusCmd.Result.communication_error;
        }

        byte[] rbuf = new byte[8];

        HA7SSerial.ReadResult readResult = serialPort.writeReadTilCR(resetBusCmd, rbuf, defaultTimeoutMSec, cmd.getDeviceLevelLogger());

        if ((readResult.error == HA7SSerial.ReadResult.ErrorCode.RR_Success) &&
                (readResult.readCount == 1) &&
                (rbuf[0] == 0x07)) {
            // This can occur during development when when the first thing read after open is
            // 0x07 0x0D. So we try this again once.
            readResult = serialPort.writeReadTilCR(resetBusCmd, rbuf, defaultTimeoutMSec, cmd.getDeviceLevelLogger());
        }

        if ((readResult.error != HA7SSerial.ReadResult.ErrorCode.RR_Success) ||
                (readResult.readCount != 0)) {
            cmd.logErrorInternal(readResult.error.name() + " readCount:" + readResult.readCount);

            cmd.logErrorInternal(readResult.error.name() + " stopping port");
            HA7SSerial.StopResult stopResult = serialPort.stop((Logger)cmd);
            cmd.logErrorInternal(readResult.error.name() + " stop result:" + stopResult.name());

            return StartBusCmd.Result.communication_error;
        }

        started = new Boolean(true);

        return StartBusCmd.Result.started;
    }

    public synchronized StopBusCmd.Result executeStopBusCmd(HA7SStopBusCmd cmd) {

        StopBusCmd.Result result = null;

        if ((started == null) || (serialPort == null)) {
            return StopBusCmd.Result.not_started;
        }

        HA7SSerial.StopResult stopResult = serialPort.stop(cmd.getDeviceLevelLogger());

        if (stopResult != HA7SSerial.StopResult.SR_Success) {
            return StopBusCmd.Result.communication_error;
        }

        started = null;
        return StopBusCmd.Result.stopped;
    }

    // We will be called by our own HA7SSearchBusCmd only if anything is found in the search.
    public void notifySearchSuccess(HA7SSearchBusCmd cmd) {
        if (cmd.isByAlarm()) {
            searchBusByAlarmNotifyData.notifySearchResult(cmd.getResultList());
        } else if (!cmd.isByFamilyCode()) {
            searchBusNotifyData.notifySearchResult(cmd.getResultList());
        }
    }

    public enum cmdResult {
        Success,
        NotStarted,
        DeviceNotFound,
        ReadTimeout,
        ReadOverrun,
        ReadError
    };

    public class cmdReturn {
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

        public cmdResult   result;
        public int readCount;
        public long writeCTM;
    }

    public cmdReturn cmdSearchROM(byte[] rbuf, Logger optLogger) {
        final byte[] wbuf = { 'S' };
        return cmdSearchInternal(wbuf, rbuf, optLogger, "cmdSearchROM");
    }

    public cmdReturn cmdNextSearchROM(byte[] rbuf, Logger optLogger) {
        final byte[] wbuf = { 's' };
        return cmdSearchInternal(wbuf, rbuf, optLogger, "cmdNextSearchROM");
    }

    public cmdReturn cmdFamilySearch(byte familyCode, byte[] rbuf, Logger optLogger) {
        final byte[] wbuf = {'F', '0', '0'};
        wbuf[1] = Convert.fourBitsToHex(((int)familyCode & 0xff) >> 4);
        wbuf[2] = Convert.fourBitsToHex(((int)familyCode & 0xff) & 0xf);
        return cmdSearchInternal(wbuf, rbuf, optLogger, "cmdFamilySearch");
    }

    public cmdReturn cmdNextFamilySearch(byte[] rbuf, Logger optLogger) {
        final byte[] wbuf = { 'f' };
        return cmdSearchInternal(wbuf, rbuf, optLogger, "cmdNextFamilySearch");
    }

    public cmdReturn cmdConditionalSearch(byte[] rbuf, Logger optLogger) {
        final byte[] wbuf = { 'C' };
        return cmdSearchInternal(wbuf, rbuf, optLogger, "cmdConditionalSearch");
    }

    public cmdReturn cmdNextConditionalSearch(byte[] rbuf, Logger optLogger) {
        final byte[] wbuf = { 'c' };
        return cmdSearchInternal(wbuf, rbuf, optLogger, "cmdNextConditionalSearch");
    }

    private cmdReturn logAndReturn(cmdReturn ret, Logger optLogger, String logContext) {
        if (optLogger != null) {
            optLogger.logError(logContext, ret.result.name());
        }
        return ret;
    }

    private cmdReturn cmdSearchInternal(byte[] wbuf, byte[] rbuf, Logger optLogger, String logContext) {
        if ((started == null) || (serialPort == null)) {
            return logAndReturn(new cmdReturn(cmdResult.NotStarted), optLogger, logContext);
        }

        HA7SSerial.ReadResult readResult = serialPort.writeReadTilCR(wbuf, rbuf, defaultTimeoutMSec, optLogger);

        switch (readResult.error) {
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
                    optLogger.logError(logContext, "unknown HA7SSerial.ReadResult:" + readResult.error.name());
                }
                return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
        }

        if (readResult.readCount == 0) {
            return logAndReturn(new cmdReturn(0, readResult.postWriteCTM), optLogger, logContext);
        }

        if (readResult.readCount != 16) {
            if (optLogger != null) {
                optLogger.logError(logContext, "expected 16, readCount:" + readResult.readCount);
            }
            return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
        }

        final String addr = new String(rbuf, 0, 16);
        if (!Address.isValid(addr)) {
            if (optLogger != null) {
                optLogger.logError(logContext, "not valid address:" + addr);
            }
            return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
        }

        return logAndReturn(new cmdReturn(readResult.readCount, readResult.postWriteCTM), optLogger, logContext);
    }

    private static byte[] buildSelectCmdData(DSAddress dsAddr) {
        // Select CMD (16 hex address bytes)
        byte[] data = new byte[] {
                'A',
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                '\r'
        };

        final String str = dsAddr.toString();
        for (int i = 0; i < 16; i++) {
            data[1 + i] = (byte) str.charAt(i);
        }

        return data;
    }

    public cmdReturn cmdAddressSelect(DSAddress dsAddr, Logger optLogger) {
        final byte[] rbuf = new byte[16];
        final String logContext = "cmdAddressSelect";

        if ((started == null) || (serialPort == null)) {
            return logAndReturn(new cmdReturn(cmdResult.NotStarted), optLogger, logContext);
        }

        // Do we already have a wbuf for this dsAddress?
        final HashMap<DSAddress, byte[]> dsAddrToCmdData = new HashMap<DSAddress, byte[]>();

        byte[] selectCmdData = dsAddrToCmdData.get(dsAddr);
        if (selectCmdData == null) {
            selectCmdData = buildSelectCmdData(dsAddr);
            dsAddrToCmdData.put(dsAddr, selectCmdData);
        }

        HA7SSerial.ReadResult readResult = serialPort.writeReadTilCR(selectCmdData, rbuf, defaultTimeoutMSec, optLogger);

        switch (readResult.error) {
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
                    optLogger.logError(logContext, "unknown HA7SSerial.ReadResult:" + readResult.error.name());
                }
                return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
        }

        if ((readResult.readCount == 1) && (rbuf[0] == 0x7)) {
            // This is what the HA7S returns on an error, in this case the Address is unknown.
            return logAndReturn(new cmdReturn(cmdResult.DeviceNotFound), optLogger, logContext);
        }

        if (readResult.readCount != 16) {
            if (optLogger != null) {
                optLogger.logError(logContext, "expected 16, readCount:" + readResult.readCount);
            }
            return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
        }

        return logAndReturn(new cmdReturn(0, readResult.postWriteCTM), optLogger, logContext);
    }

    public cmdReturn cmdReadBit(byte[] rbuf, Logger optLogger) {
        final byte[] wbuf = new byte[] { 'O' };
        final String logContext = "cmdReadBit";

        if ((started == null) || (serialPort == null)) {
            return logAndReturn(new cmdReturn(cmdResult.NotStarted), optLogger, logContext);
        }

        HA7SSerial.ReadResult readResult = serialPort.writeReadTilCR(wbuf, rbuf, defaultTimeoutMSec, optLogger);

        switch (readResult.error) {
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
                    optLogger.logError(logContext, "unknown HA7SSerial.ReadResult:" + readResult.error.name());
                }
                return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
        }

        if (readResult.readCount != 1) {
            if (optLogger != null) {
                optLogger.logError(logContext, "expected 1, readCount:" + readResult.readCount);
            }
            return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
        }

        if ((rbuf[0] != '0') && (rbuf[0] != '1')) {
            if (optLogger != null) {
                optLogger.logError(logContext, "expected 0 or 1, got:" + Byte.toString(rbuf[0]));
            }
            return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
        }


        return logAndReturn(new cmdReturn(0, readResult.postWriteCTM), optLogger, logContext);
    }

    public cmdReturn cmdWriteBlock(byte[] wbuf, byte[] rbuf, Logger optLogger) {
        final String logContext = "cmdWriteBlock";

        if ((started == null) || (serialPort == null)) {
            return logAndReturn(new cmdReturn(cmdResult.NotStarted), optLogger, logContext);
        }

        HA7SSerial.ReadResult readResult = serialPort.writeReadTilCR(wbuf, rbuf, defaultTimeoutMSec, optLogger);

        switch (readResult.error) {
            case RR_Success:
                return logAndReturn(new cmdReturn(readResult.readCount, readResult.postWriteCTM), optLogger, logContext);

            case RR_ReadTimeout:
                return logAndReturn(new cmdReturn(cmdResult.ReadTimeout), optLogger, logContext);

            case RR_ReadOverrun:
                return logAndReturn(new cmdReturn(cmdResult.ReadOverrun), optLogger, logContext);

            case RR_Error:
                return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);

            default:
                if (optLogger != null) {
                    optLogger.logError(logContext, "unknown HA7SSerial.ReadResult:" + readResult.error.name());
                }
                return logAndReturn(new cmdReturn(cmdResult.ReadError), optLogger, logContext);
        }
    }

}
