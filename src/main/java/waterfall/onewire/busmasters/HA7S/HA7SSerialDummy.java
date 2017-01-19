package waterfall.onewire.busmasters.HA7S;

import waterfall.onewire.busmaster.Logger;

import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.concurrent.SynchronousQueue;

/**
 * Created by dwaterfa on 1/18/17.
 */
public class HA7SSerialDummy implements HA7SSerial  {
    private static final String[] dsAddrs = new String[] {
            "EE0000065BC0AE28",
            "090000065BD53528",
            "5F0000065CCD1A28",
            "260000065BE22D28",
            "7C0000063BB13028",
            "5A0000063B7AF528",
            "AA0000063BF51928",
            "390000063B759F28",
            "7F0000063BA12F28"
    };

    private boolean started = false;
    private int searchIndex = 1000;

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

        try {
            for (; ; ) {
                wEnd = parse(wBuf, wStart);

                if (wEnd == wStart) {
                    if (readResult.error == null) {
                        readResult.postWriteCTM = System.currentTimeMillis();
                    }
                    return readResult;
                }

                switch (wBuf[wStart]) {
                    case 'S':
                        search(wBuf, wStart, wEnd, rBuf, optLogger, readResult, searchIndex = 0);
                        break;

                    case 's':
                        search(wBuf, wStart, wEnd, rBuf, optLogger, readResult, ++searchIndex);
                        break;

                    case 'F':
                    case 'f':
                    case 'C':
                    case 'c':
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

    private void search(byte[] wBuf, int wStart, int wEnd, byte[] rBuf, Logger optLogger, ReadResult readResult, int searchIndex) throws IllegalArgumentException {
        if ((wEnd - wStart) != 1) {
            throw new IllegalArgumentException("search not single cmd char");
        }

        if (searchIndex < dsAddrs.length) {
            int i = 0;
            while (i < dsAddrs[searchIndex].length()) {
                rBuf[readResult.readCount++] = (byte)dsAddrs[searchIndex].charAt(i);
            }
        }
        rBuf[readResult.readCount++] = '\r';
    }

    // Even values are the char code, odd values are the expected total chars for the command data, -1 means terminated
    // with CR.
    private static final byte cmdLen[] = new byte[] {
            '\r', 1,
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

        return wEnd;
    }

}
