package waterfall.onewire.busmaster.HA7S;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortException;
import jssc.SerialPortList;

import waterfall.onewire.busmaster.Logger;


public class JSSC implements HA7SSerial {
    private String portName;
    private SerialPort serialPort = null;

    class SharedData {
        public boolean started = false;

        public Thread waitingThread = null;
        public Logger waitingThreadLogger = null;
        public byte[] readBuffer = null;

        public int readOffset = 0;
        public int readCount = 0;
        public boolean readOverrun = false;
        public boolean readComplete = true;

        public Logger noWaitingThreadLogger = new Logger();

        public void addWaitingThread(Thread waitingThread, byte[] rBuf, Logger waitingThreadLogger) {
            this.waitingThread = waitingThread;
            this.waitingThreadLogger = waitingThreadLogger;
            this.readBuffer = rBuf;
            readOffset = 0;
            readCount = 0;
            readOverrun = false;
            readComplete = false;

            if ((waitingThreadLogger != null) && (!noWaitingThreadLogger.isEmpty())) {
                waitingThreadLogger.append(noWaitingThreadLogger.popLevel());
                noWaitingThreadLogger.clear();
                noWaitingThreadLogger.pushLevel("NoWaitingThread");
            }
        }

        public void clearWaitingThread() {
            this.waitingThread = null;
            this.waitingThreadLogger = null;
            this.readBuffer = null;
        }

    }

    SharedData sharedData = new SharedData();

    public JSSC(String portName) {
        this.portName = portName;
    }

    @Override
    public synchronized StartResult start(Logger optLogger) {

        if (optLogger != null) {
            optLogger.pushLevel(this.getClass().getSimpleName() + ".start()" + portName);
        }

        try {
            if (sharedData.started) {
                if (optLogger != null) {
                    optLogger.debug("Already started");
                }
                return StartResult.SR_Success;
            }

            if (portName == null) {
                if (true) {
                    String[] portList = SerialPortList.getPortNames();
                    String s = null;
                    if (portList.length == 1) {
                        s = portList[0];
                    } else {
                        StringBuilder sb = new StringBuilder();
                        sb.append(portList[0]);
                        for (int i = 1; i < portList.length; i++) {
                            sb.append(", " + portList[i]);
                        }
                        s = sb.toString();
                    }
                    if (optLogger != null) {
                        optLogger.error("No portName. Valid portNames are " + s);
                    }
                }
                return StartResult.SR_NoPortName;
            }

            serialPort = new SerialPort(portName);
            try {
                serialPort.openPort();
            } catch (SerialPortException ex) {
                if (optLogger != null) {
                    optLogger.error(ex);
                }
                serialPort = null;
                return StartResult.SR_Error;
            }

            try {
                serialPort.setParams(
                        SerialPort.BAUDRATE_9600,
                        SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);
            } catch (SerialPortException ex) {
                if (optLogger != null) {
                    optLogger.error(ex);
                }
                serialPort = null;
                return StartResult.SR_Error;
            }

            // flush anything we find.
            try {
                if (serialPort.getInputBufferBytesCount() > 0) {
                    byte[] flushed = serialPort.readBytes();
                    if (optLogger != null) {
                        optLogger.debug("flushing[" + flushed.length + "]:" + JSSC.byteToSafeString(flushed, 0, flushed.length));
                    }
                }
            } catch (SerialPortException ex) {
                if (optLogger != null) {
                    optLogger.error(ex);
                }
                ;
                serialPort = null;
                return StartResult.SR_Error;
            }

            try {
                sharedData.noWaitingThreadLogger.pushLevel("NoWaitingThread");

                serialPort.addEventListener((SerialPortEvent serialPortEvent) -> {
                    if (serialPortEvent.isRXCHAR()) {
                        synchronized (serialPort) {
                            Logger activeLogger;
                            if ((sharedData.waitingThread == null) ||
                                    (sharedData.waitingThreadLogger == null)) {
                                activeLogger = sharedData.noWaitingThreadLogger;
                            } else {
                                activeLogger = sharedData.waitingThreadLogger;
                            }

                            try {
                                byte[] rbuf = serialPort.readBytes();
                                if ((rbuf != null) && (rbuf.length > 0)) {
                                    activeLogger.debug("read[" + rbuf.length + "]:" + byteToSafeString(rbuf, 0, rbuf.length));
                                    if ((sharedData.waitingThread == null) || (sharedData.readComplete)) {
                                        // nobody is waiting so log and clear.
                                        activeLogger.error("No thread or readComplete, flushing");
                                    } else {
                                        for (int i = 0; i < rbuf.length; i++) {
                                            if (rbuf[i] == '\r') {
                                                // found the terminator
                                                sharedData.readComplete = true;
                                                if ((i + 1) != rbuf.length) {
                                                    activeLogger.error((rbuf.length - i - 1) + " extra bytes ignored");
                                                }

                                                if (sharedData.waitingThread != null) {
                                                    serialPort.notify();
                                                }
                                            } else if (sharedData.readBuffer != null) {
                                                if ((sharedData.readOffset + sharedData.readCount) < sharedData.readBuffer.length) {
                                                    sharedData.readBuffer[sharedData.readOffset + sharedData.readCount++] = rbuf[i];
                                                } else if (!sharedData.readOverrun) {
                                                    activeLogger.error(String.format("Read overrun at index %d", i));
                                                    sharedData.readOverrun = true;
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    activeLogger.error("Read zero chars?");
                                }
                            } catch (SerialPortException ex) {
                                activeLogger.error(ex);
                            }
                        }
                    }
                }, SerialPort.MASK_RXCHAR);
            } catch (SerialPortException ex) {
                if (optLogger != null) {
                    optLogger.error(ex);
                }
                serialPort = null;
                return StartResult.SR_Error;
            }

            sharedData.started = true;
            if (optLogger != null) {
                optLogger.info("started");
            }
            return StartResult.SR_Success;
        } finally {
            if (optLogger != null) {
                optLogger.popLevel();
            }
        }

    }

    public ReadResult writeReadTilCR(byte wBuf[], byte rBuf[], long rTimeoutMSec, Logger optLogger) {
        ReadResult result = new ReadResult();

        if (optLogger != null) {
            optLogger.pushLevel(this.getClass().getSimpleName() + ".writeReadTilCR");
        }

        try {
            synchronized (this) {

                if (sharedData.started) {
                    synchronized (serialPort) {
                        try {
                            sharedData.addWaitingThread(Thread.currentThread(), rBuf, optLogger);

                            try {
                                final int wcount = wBuf.length;

                                if (optLogger != null) {
                                    optLogger.debug("write[" + wcount + "]:" + byteToSafeString(wBuf, 0, wcount));
                                }

                                for (int i = 0; i < wcount; i++) {
                                    serialPort.writeByte(wBuf[i]);
                                }

                                result.postWriteCTM = new Long(System.currentTimeMillis());

                                serialPort.wait(rTimeoutMSec);

                                if (sharedData.readComplete) {
                                    if (sharedData.readOverrun) {
                                        result.error = ReadResult.ErrorCode.RR_ReadOverrun;
                                    } else {
                                        result.error = ReadResult.ErrorCode.RR_Success;
                                    }
                                    result.readCount = sharedData.readCount;
                                } else {
                                    if (optLogger != null) {
                                        optLogger.error("read not complete?");
                                    }
                                    result.error = ReadResult.ErrorCode.RR_Error;
                                }
                            } catch (SerialPortException ex) {
                                if (optLogger != null) {
                                    optLogger.error(ex);
                                }
                                result.error = ReadResult.ErrorCode.RR_Error;
                            } catch (InterruptedException ex) {
                                if (optLogger != null) {
                                    optLogger.error(ex);
                                }
                                result.error = ReadResult.ErrorCode.RR_ReadTimeout;
                                result.readCount = sharedData.readCount;
                            }
                        } finally {
                            sharedData.clearWaitingThread();
                        }
                    }
                } else {
                    if (optLogger != null) {
                        optLogger.debug("not started.");
                    }
                    result.error = ReadResult.ErrorCode.RR_Error;
                }
            }
            if (optLogger != null) {
                optLogger.debug(result.error.name());
            }

        } finally {
            if (optLogger != null) {
                optLogger.popLevel();
            }
        }

        return result;
    }

    public synchronized StopResult stop(Logger optLogger) {

        if (optLogger != null) {
            optLogger.pushLevel(this.getClass().getSimpleName() + ".stop() " + portName);
        }

        try {
            if (optLogger != null) {
                optLogger.info("stop()\n");
            }

            if (!sharedData.started) {
                if (optLogger != null) {
                    optLogger.debug("Already stopped");
                }
                return StopResult.SR_Success;
            }

            sharedData.started = false;

            try {
                boolean closeResult = serialPort.closePort();
                if (!closeResult) {
                    if (optLogger != null) {
                        optLogger.error("serial port failed to close");
                    }
                }
            } catch (SerialPortException ex) {
                if (optLogger != null) {
                    optLogger.error(ex);
                }
                return StopResult.SR_Error;
            }

            if (optLogger != null) {
                optLogger.debug("stopped");
            }

            return StopResult.SR_Success;
        } finally {
            if (optLogger != null)
                optLogger.popLevel();
        }
    }

    public static String byteToSafeString(byte[] buf, int bOffset, int bCount) {
        if (bCount == 0) {
            return "{empty}";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < bCount; i++) {
            if (buf[i] < 32) {
                sb.append(String.format("{%02X}", buf[i]));
            } else {
                sb.append((char) buf[i]);
            }
        }

        return sb.toString();
    }

}
