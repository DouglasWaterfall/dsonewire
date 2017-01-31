package waterfall.onewire.busmasters.HA7S;

import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.Logger;

import java.util.*;
import java.util.concurrent.SynchronousQueue;

/**
 * Created by dwaterfa on 1/18/17.
 */
public class HA7SSerialDummy implements HA7SSerial  {

    public class DeviceData {
        public String dsAddr;
        public boolean alarm;

        public DeviceData(String dsAddr, boolean alarm) {
            this.dsAddr = dsAddr;
            this.alarm = alarm;
        }
    }

    private final ArrayList<DeviceData> deviceDataList = new ArrayList<>();

    private boolean started = false;

    private int searchIndex = 1000;

    private enum ActiveSearch {
        General,
        Alarm,
        Family
    }

    private ActiveSearch searchType = null;
    private byte[] lastFamilySearchCode = null;

    @Override
    public String getPortName() {
        return "Dummy";
    }

    @Override
    public StartResult start(Logger optLogger) {
        if (!started) {
            if (optLogger != null) {
                optLogger.logInfo("start", "Starting");
            }
            started = true;
        }
        return StartResult.SR_Success;
    }

    @Override
    public ReadResult writeReadTilCR(byte[] wBuf, byte[] rBuf, long rTimeoutMSec, Logger optLogger) {
        final String logContext = "writeReadTilCR()";

        if (wBuf == null) {
            if (optLogger != null) {
                optLogger.logError(logContext, "null wBuf");
            }
            return new ReadResult(ReadResult.ErrorCode.RR_Error);
        }

        int wStart = 0;
        int wEnd = 0;

        ReadResult readResult = new ReadResult();

        readResult.postWriteCTM = System.currentTimeMillis();

        try {
            for (; ; ) {
                wEnd = parse(wBuf, wStart);

                if (wEnd == wStart) {
                    if (readResult.error == null) {
                        readResult.error = ReadResult.ErrorCode.RR_Success;
                        if (readResult.postWriteCTM == System.currentTimeMillis()) {
                            try {
                                Thread.sleep(1);
                            }
                            catch (InterruptedException e) {
                                optLogger.logError(logContext, "sleep delay interrupted!");
                            }
                        }
                    }
                    return readResult;
                }

                switch (wBuf[wStart]) {
                    case 'S':
                        search(wBuf, wStart, wEnd, rBuf, optLogger, readResult);
                        break;

                    case 's':
                        searchNext(wBuf, wStart, wEnd, rBuf, optLogger, readResult);
                        break;

                    case 'R':
                        searchIndex = 1000;
                        searchType = null;
                        lastFamilySearchCode = null;
                        reset(wBuf, wStart, wEnd, rBuf, optLogger, readResult);
                        break;

                    case 'F':
                        familySearch(wBuf, wStart, wEnd, rBuf, optLogger, readResult);
                        break;

                    case 'f':
                        familySearchNext(wBuf, wStart, wEnd, rBuf, optLogger, readResult);
                        break;

                    case 'C':
                        alarmSearch(wBuf, wStart, wEnd, rBuf, optLogger, readResult);
                        break;

                    case 'c':
                        alarmSearchNext(wBuf, wStart, wEnd, rBuf, optLogger, readResult);
                        break;

                    case 'W':
                        if (optLogger != null) {
                            optLogger.logError(logContext, "unsupported cmd code:" + (char) wBuf[wStart]);
                        }
                        return new ReadResult(ReadResult.ErrorCode.RR_Error);

                    default:
                        if (optLogger != null) {
                            optLogger.logError(logContext, "unknown cmd code:" + (char) wBuf[wStart]);
                        }
                        return new ReadResult(ReadResult.ErrorCode.RR_Error);
                }

                wStart = wEnd;
            }
        }
        catch (IllegalArgumentException e) {
            readResult.error = ReadResult.ErrorCode.RR_Error;
            if (optLogger != null) {
                optLogger.logError(logContext, e.getMessage());
            }
            return readResult;
        }
    }

    @Override
    public StopResult stop(Logger optLogger) {
        if (started) {
            if (optLogger != null) {
                optLogger.logInfo("stop", "Stopping");
            }
            started = false;
        }
        return StopResult.SR_Success;
    }

    /* For our testing */
    public void addDevice(String dsAddr, boolean alarmActive) {
        if ((dsAddr == null) || (!DSAddress.isValid(dsAddr))) {
            throw new IllegalArgumentException("dsAddr is not valid");
        }

        for (int i = 0; i < deviceDataList.size(); i++) {
            if (deviceDataList.get(i).dsAddr.equals(dsAddr)) {
                throw new IllegalArgumentException("dsAddr is duplicate");
            }
        }

        deviceDataList.add(new DeviceData(dsAddr, alarmActive));
    }

    public void removeDevice(String dsAddr) {
        for (int i = 0; i < deviceDataList.size(); i++) {
            if (deviceDataList.get(i).dsAddr.equals(dsAddr)) {
                deviceDataList.remove(i);
                return;
            }
        }

        throw new IllegalArgumentException("dsAddr not found");
    }

    public void removeAllDevices() {
        deviceDataList.clear();
    }

    private void search(byte[] wBuf, int wStart, int wEnd, byte[] rBuf, Logger optLogger, ReadResult readResult) throws IllegalArgumentException {
        if ((wEnd - wStart) != 1) {
            throw new IllegalArgumentException("search is single char cmd");
        }

        searchIndex = 0;
        searchType = ActiveSearch.General;
        lastFamilySearchCode = null;

        if (searchIndex < deviceDataList.size()) {
            String dsAddr = deviceDataList.get(searchIndex++).dsAddr;
            for (int i = 0; i < dsAddr.length(); i++) {
                rBuf[readResult.readCount++] = (byte)dsAddr.charAt(i);
            }
        }
        // never pass back the CR
    }

    private void searchNext(byte[] wBuf, int wStart, int wEnd, byte[] rBuf, Logger optLogger, ReadResult readResult) throws IllegalArgumentException {
        if ((wEnd - wStart) != 1) {
            throw new IllegalArgumentException("search next is single char cmd");
        }
        if (searchType != ActiveSearch.General) {
            throw new IllegalArgumentException("general search is not active");
        }

        if (searchIndex < deviceDataList.size()) {
            String dsAddr = deviceDataList.get(searchIndex++).dsAddr;
            for (int i = 0; i < dsAddr.length(); i++) {
                rBuf[readResult.readCount++] = (byte)dsAddr.charAt(i);
            }
        }
        // never pass back the CR
    }

    private void alarmSearch(byte[] wBuf, int wStart, int wEnd, byte[] rBuf, Logger optLogger, ReadResult readResult) throws IllegalArgumentException {
        if ((wEnd - wStart) != 1) {
            throw new IllegalArgumentException("alarm search is single char cmd");
        }

        searchIndex = 0;
        searchType = ActiveSearch.Alarm;
        lastFamilySearchCode = null;

        while (searchIndex < deviceDataList.size()) {
            DeviceData data = deviceDataList.get(searchIndex++);
            if (data.alarm) {
                for (int i = 0; i < data.dsAddr.length(); i++) {
                    rBuf[readResult.readCount++] = (byte) data.dsAddr.charAt(i);
                }
                break;
            }
        }
        // never pass back the CR
    }

    private void alarmSearchNext(byte[] wBuf, int wStart, int wEnd, byte[] rBuf, Logger optLogger, ReadResult readResult) throws IllegalArgumentException {
        if ((wEnd - wStart) != 1) {
            throw new IllegalArgumentException("alarm search next is single char cmd");
        }

        if (searchType != ActiveSearch.Alarm) {
            throw new IllegalArgumentException("alarm search is not active");
        }

        while (searchIndex < deviceDataList.size()) {
            DeviceData data = deviceDataList.get(searchIndex++);
            if (data.alarm) {
                for (int i = 0; i < data.dsAddr.length(); i++) {
                    rBuf[readResult.readCount++] = (byte) data.dsAddr.charAt(i);
                }
                break;
            }
        }
        // never pass back the CR
    }

    private void familySearch(byte[] wBuf, int wStart, int wEnd, byte[] rBuf, Logger optLogger, ReadResult readResult) throws IllegalArgumentException {
        if ((wEnd - wStart) != 3) {
            throw new IllegalArgumentException("family search cmd length must be three");
        }

        searchIndex = 0;
        searchType = ActiveSearch.Family;
        lastFamilySearchCode = new byte[2];
        lastFamilySearchCode[0] = wBuf[wStart + 1];
        lastFamilySearchCode[1] = wBuf[wStart + 2];

        while (searchIndex < deviceDataList.size()) {
            String dsAddr = deviceDataList.get(searchIndex++).dsAddr;
            if ((dsAddr.charAt(14) == lastFamilySearchCode[0]) &&
                (dsAddr.charAt(15) == lastFamilySearchCode[1])) {
                for (int i = 0; i < dsAddr.length(); i++) {
                    rBuf[readResult.readCount++] = (byte) dsAddr.charAt(i);
                }
                break;
            }
        }
        // never pass back the CR
    }

    private void familySearchNext(byte[] wBuf, int wStart, int wEnd, byte[] rBuf, Logger optLogger, ReadResult readResult) throws IllegalArgumentException {
        if ((wEnd - wStart) != 1) {
            throw new IllegalArgumentException("family search next is single char cmd");
        }

        if (searchType != ActiveSearch.Family) {
            throw new IllegalArgumentException("family search is not active");
        }

        while (searchIndex < deviceDataList.size()) {
            String dsAddr = deviceDataList.get(searchIndex++).dsAddr;
            if ((dsAddr.charAt(14) == lastFamilySearchCode[0]) &&
                    (dsAddr.charAt(15) == lastFamilySearchCode[1])) {
                for (int i = 0; i < dsAddr.length(); i++) {
                    rBuf[readResult.readCount++] = (byte) dsAddr.charAt(i);
                }
                break;
            }
        }
        // never pass back the CR
    }

    private void reset(byte[] wBuf, int wStart, int wEnd, byte[] rBuf, Logger optLogger, ReadResult readResult) throws IllegalArgumentException {
        if ((wEnd - wStart) != 1) {
            throw new IllegalArgumentException("Reset not single cmd char");
        }

        // leave readCount alone
    }

    // Even values are the char code, odd values are the expected total chars for the command data, -1 means terminated
    // with CR.
    private static final byte cmdLen[] = new byte[] {
            '\r', 1,
            'R', 1,
            'S', 1,
            's', 1,
            'F', 3,
            'f', 1,
            'C', 1,
            'c', 1,
            'W', -1
    };

    // This method knows about which cmd codes are supported and what their format is, in particular
    // which ones do not need CRs at the end. Returns the end index of the parse relative to the start.

    private int parse(byte[] wBuf, int wStart) throws IllegalArgumentException {
        if (wStart == wBuf.length) {
            return wStart;
        }

        int found = -99;

        for (int i = 0; i < cmdLen.length; i += 2) {
            if (cmdLen[i] == wBuf[wStart]) {
                found = cmdLen[i + 1];
                break;
            }
        }

        if (found == -99) {
            throw new IllegalArgumentException("cmd byte note found:" + (char)wBuf[wStart]);
        }

        if (found != -1) {
            // fixed length
            if ((wStart + found) > wBuf.length) {
                throw new IllegalArgumentException("cmd byte overrun:" + (char)wBuf[wStart] + " needs:" + found);
            }

            return (wStart + found);
        }

        // variable length terminated by cr
        int wEnd = wStart;
        while ((wEnd < wBuf.length) && (wBuf[wEnd] != '\r')) {
            wEnd++;
        }

        if (wEnd == wBuf.length) {
            throw new IllegalArgumentException("cmd byte underrun:" + (char) wBuf[wStart] + " CR not found");
        }
        else {
            // we want to be PAST the CR.
            return (wEnd + 1);
        }
    }

}
