package waterfall.onewire.busmasters.HA7S;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.TimeUnit;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortException;
import jssc.SerialPortList;
import waterfall.onewire.busmasters.HA7S.HA7SSerial.ReadResult.ErrorCode;


public class JSSC implements HA7SSerial {

  SharedData sharedData = new SharedData();
  private final String portName;
  private final long readTimeoutMSec;
  private final org.slf4j.Logger optLogger = null; // LoggerFactory.getLogger(this.getClass());

  private SerialPort serialPort = null;

  public JSSC(String portName, long readTimeoutMSec) {
    this.portName = portName;
    this.readTimeoutMSec = readTimeoutMSec;
  }

  public JSSC(String portName) {
    this.portName = portName;
    this.readTimeoutMSec = TimeUnit.SECONDS.toMillis(15);
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

  @Override
  public String getPortName() {
    return this.portName;
  }

  @Override
  public boolean isStarted() {
    return sharedData.started;
  }

  @Override
  public synchronized StartResult start() {

    String logContext = null;

    if (optLogger != null) {
      logContext =
          this.getClass().getSimpleName() + ".start() " + ((portName != null) ? portName : "")
              + " ";
    }

    if (sharedData.started) {
      if (optLogger != null) {
        optLogger.info(logContext, "Already started");
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
          optLogger.error(logContext, "No portName. Valid portNames are " + s);
        }
      }
      return StartResult.SR_NoPortName;
    }

    serialPort = new SerialPort(portName);
    try {
      serialPort.openPort();
    } catch (SerialPortException ex) {
      if (optLogger != null) {
        optLogger.error(logContext, ex);
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
        optLogger.error(logContext, ex);
      }
      serialPort = null;
      return StartResult.SR_Error;
    }

    // flush anything we find.
    try {
      if (serialPort.getInputBufferBytesCount() > 0) {
        byte[] flushed = serialPort.readBytes();
        if (optLogger != null) {
          optLogger.info(logContext, "flushing[" + flushed.length + "]:" + JSSC
              .byteToSafeString(flushed, 0, flushed.length));
        }
      }
    } catch (SerialPortException ex) {
      if (optLogger != null) {
        optLogger.error(logContext, ex);
      }
      ;
      serialPort = null;
      return StartResult.SR_Error;
    }

    try {
      serialPort.addEventListener((SerialPortEvent serialPortEvent) -> {
        if (serialPortEvent.isRXCHAR()) {
          synchronized (serialPort) {
            try {
              byte[] rbuf = serialPort.readBytes();
              if ((rbuf != null) && (rbuf.length > 0)) {
                sharedData
                    .info("read[" + rbuf.length + "]:" + byteToSafeString(rbuf, 0, rbuf.length));
                if ((sharedData.waitingThread == null) || (sharedData.readComplete)) {
                  // nobody is waiting so log and clear.
                  sharedData.error("No thread or readComplete, flushing");
                } else {
                  for (int i = 0; i < rbuf.length; i++) {
                    if (rbuf[i] == '\r') {
                      // found the terminator
                      sharedData.readComplete = true;
                      sharedData.readCRCTM = System.currentTimeMillis();
                      if ((i + 1) != rbuf.length) {
                        sharedData.error((rbuf.length - i - 1) + " extra bytes ignored");
                      }

                      if (sharedData.waitingThread != null) {
                        serialPort.notify();
                      }

                      break; // necessary if the read has extra chars

                    } else if (sharedData.readBuffer != null) {
                      if ((sharedData.readOffset + sharedData.readCount)
                          < sharedData.readBuffer.length) {
                        sharedData.readBuffer[sharedData.readOffset
                            + sharedData.readCount++] = rbuf[i];
                      } else if (!sharedData.readOverrun) {
                        sharedData.error(String.format("Read overrun at index %d", i));
                        sharedData.readOverrun = true;
                      }
                    }
                  }
                }
              } else {
                sharedData.error("Read zero chars?");
              }
            } catch (SerialPortException ex) {
              sharedData.error(ex.toString());
            }
          }
        }
      }, SerialPort.MASK_RXCHAR);
    } catch (SerialPortException ex) {
      if (optLogger != null) {
        optLogger.error(logContext, ex);
      }
      serialPort = null;
      return StartResult.SR_Error;
    }

    sharedData.started = true;
    if (optLogger != null) {
      optLogger.info(logContext, "started");
    }
    return StartResult.SR_Success;
  }

  @Override
  public ReadResult writeReadTilCR(byte wBuf[], byte rBuf[]) {
    String logContext = null;

    if (optLogger != null) {
      logContext =
          this.getClass().getSimpleName() + ".writeReadTilCR() " + ((portName != null) ? portName
              : "") + " ";
    }

    ReadResult.ErrorCode error = null;
    int readCount = 0;
    long postWriteCTM = 0;
    long readCRCTM = 0;

    synchronized (this) {

      if (sharedData.started) {
        synchronized (serialPort) {
          try {
            sharedData.addWaitingThread(Thread.currentThread(), rBuf, optLogger, logContext);

            try {
              final int wcount = wBuf.length;

              if (optLogger != null) {
                optLogger.info(logContext,
                    "write[" + wcount + "]:" + byteToSafeString(wBuf, 0, wcount));
              }

              for (int i = 0; i < wcount; i++) {
                serialPort.writeByte(wBuf[i]);
              }

              postWriteCTM = System.currentTimeMillis();

              serialPort.wait(readTimeoutMSec);

              if (sharedData.readComplete) {
                if (sharedData.readOverrun) {
                  error = ReadResult.ErrorCode.RR_ReadOverrun;
                } else {
                  error = ReadResult.ErrorCode.RR_Success;
                }
                readCount = sharedData.readCount;
                readCRCTM = sharedData.readCRCTM;
              } else {
                if (optLogger != null) {
                  optLogger.error(logContext, "read not complete?");
                }
                error = ReadResult.ErrorCode.RR_Error;
              }
            } catch (SerialPortException ex) {
              if (optLogger != null) {
                optLogger.error(logContext, ex);
              }
              error = ReadResult.ErrorCode.RR_Error;
            } catch (InterruptedException ex) {
              if (optLogger != null) {
                optLogger.error(logContext, ex);
              }
              error = ReadResult.ErrorCode.RR_ReadTimeout;
              readCount = sharedData.readCount;
            }
          } finally {
            sharedData.clearWaitingThread();
          }
        }
      } else {
        if (optLogger != null) {
          optLogger.info(logContext, "not started.");
        }
        error = ReadResult.ErrorCode.RR_Error;
      }
    }
    if (optLogger != null) {
      optLogger.info(logContext, error.name());
    }

    if (error == ErrorCode.RR_Success) {
      return new ReadResult(readCount, postWriteCTM, readCRCTM);
    }
    else {
      return new ReadResult(error);
    }
  }

  @Override
  public synchronized StopResult stop() {

    String logContext = null;

    if (optLogger != null) {
      logContext =
          this.getClass().getSimpleName() + ".stop() " + ((portName != null) ? portName : "") + " ";
    }

    if (optLogger != null) {
      optLogger.info(logContext, "stop()\n");
    }

    if (!sharedData.started) {
      if (optLogger != null) {
        optLogger.info(logContext, "Already stopped");
      }
      return StopResult.SR_Success;
    }

    sharedData.started = false;

    try {
      boolean closeResult = serialPort.closePort();
      if (!closeResult) {
        if (optLogger != null) {
          optLogger.error(logContext, "serial port failed to close");
        }
      }
    } catch (SerialPortException ex) {
      if (optLogger != null) {
        optLogger.error(logContext, ex);
      }
      return StopResult.SR_Error;
    }

    if (optLogger != null) {
      optLogger.info(logContext, "stopped");
    }

    return StopResult.SR_Success;
  }

  class SharedData {

    public boolean started = false;

    public Thread waitingThread = null;
    public Logger waitingThreadLogger = null;
    public String waitingThreadLoggerContext = null;
    public byte[] readBuffer = null;

    public int readOffset = 0;
    public int readCount = 0;
    public boolean readOverrun = false;
    public boolean readComplete = true;
    public long readCRCTM = 0;

    public void addWaitingThread(Thread waitingThread, byte[] rBuf, Logger waitingThreadLogger,
        String waitingThreadLoggerContext) {
      this.waitingThread = waitingThread;
      this.waitingThreadLogger = waitingThreadLogger;
      this.waitingThreadLoggerContext = waitingThreadLoggerContext;
      this.readBuffer = rBuf;
      readOffset = 0;
      readCount = 0;
      readOverrun = false;
      readComplete = false;
      readCRCTM = 0;
    }

    public void clearWaitingThread() {
      this.waitingThread = null;
      this.waitingThreadLogger = null;
      this.waitingThreadLoggerContext = null;
      this.readBuffer = null;
    }

    public void info(String msg) {
      if (waitingThread != null) {
        if (waitingThreadLogger != null) {
          waitingThreadLogger.info(waitingThreadLoggerContext, msg);
        }
      } else {
        System.err.println("[INFO] " + "noWaitingThread " + msg);
      }
    }

    public void error(String msg) {
      if (waitingThread != null) {
        if (waitingThreadLogger != null) {
          waitingThreadLogger.error(waitingThreadLoggerContext, msg);
        }
      } else {
        System.err.println("[ERROR] " + "noWaitingThread " + msg);
      }
    }

  }

}
