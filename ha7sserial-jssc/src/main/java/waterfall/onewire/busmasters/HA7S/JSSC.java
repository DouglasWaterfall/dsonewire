package waterfall.onewire.busmasters.HA7S;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortException;
import jssc.SerialPortList;
import waterfall.onewire.busmaster.Logger;


public class JSSC extends HA7SSerial {

  SharedData sharedData = new SharedData();
  private String portName;
  private SerialPort serialPort = null;

  public JSSC(String portName) {
    this.portName = portName;
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
  public synchronized StartResult start(Logger optLogger) {

    String logContext = null;

    if (optLogger != null) {
      logContext =
          this.getClass().getSimpleName() + ".start() " + ((portName != null) ? portName : "")
              + " ";
    }

    if (sharedData.started) {
      if (optLogger != null) {
        optLogger.logInfo(logContext, "Already started");
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
          optLogger.logError(logContext, "No portName. Valid portNames are " + s);
        }
      }
      return StartResult.SR_NoPortName;
    }

    serialPort = new SerialPort(portName);
    try {
      serialPort.openPort();
    } catch (SerialPortException ex) {
      if (optLogger != null) {
        optLogger.logError(logContext, ex);
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
        optLogger.logError(logContext, ex);
      }
      serialPort = null;
      return StartResult.SR_Error;
    }

    // flush anything we find.
    try {
      if (serialPort.getInputBufferBytesCount() > 0) {
        byte[] flushed = serialPort.readBytes();
        if (optLogger != null) {
          optLogger.logInfo(logContext, "flushing[" + flushed.length + "]:" + JSSC
              .byteToSafeString(flushed, 0, flushed.length));
        }
      }
    } catch (SerialPortException ex) {
      if (optLogger != null) {
        optLogger.logError(logContext, ex);
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
                    .logInfo("read[" + rbuf.length + "]:" + byteToSafeString(rbuf, 0, rbuf.length));
                if ((sharedData.waitingThread == null) || (sharedData.readComplete)) {
                  // nobody is waiting so log and clear.
                  sharedData.logError("No thread or readComplete, flushing");
                } else {
                  for (int i = 0; i < rbuf.length; i++) {
                    if (rbuf[i] == '\r') {
                      // found the terminator
                      sharedData.readComplete = true;
                      if ((i + 1) != rbuf.length) {
                        sharedData.logError((rbuf.length - i - 1) + " extra bytes ignored");
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
                        sharedData.logError(String.format("Read overrun at index %d", i));
                        sharedData.readOverrun = true;
                      }
                    }
                  }
                }
              } else {
                sharedData.logError("Read zero chars?");
              }
            } catch (SerialPortException ex) {
              sharedData.logError(ex.toString());
            }
          }
        }
      }, SerialPort.MASK_RXCHAR);
    } catch (SerialPortException ex) {
      if (optLogger != null) {
        optLogger.logError(logContext, ex);
      }
      serialPort = null;
      return StartResult.SR_Error;
    }

    sharedData.started = true;
    if (optLogger != null) {
      optLogger.logInfo(logContext, "started");
    }
    return StartResult.SR_Success;
  }

  @Override
  public ReadResult writeReadTilCR(byte wBuf[], byte rBuf[], long rTimeoutMSec, Logger optLogger) {
    ReadResult result = new ReadResult();

    String logContext = null;

    if (optLogger != null) {
      logContext =
          this.getClass().getSimpleName() + ".writeReadTilCR() " + ((portName != null) ? portName
              : "") + " ";
    }

    synchronized (this) {

      if (sharedData.started) {
        synchronized (serialPort) {
          try {
            sharedData.addWaitingThread(Thread.currentThread(), rBuf, optLogger, logContext);

            try {
              final int wcount = wBuf.length;

              if (optLogger != null) {
                optLogger.logInfo(logContext,
                    "write[" + wcount + "]:" + byteToSafeString(wBuf, 0, wcount));
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
                  optLogger.logError(logContext, "read not complete?");
                }
                result.error = ReadResult.ErrorCode.RR_Error;
              }
            } catch (SerialPortException ex) {
              if (optLogger != null) {
                optLogger.logError(logContext, ex);
              }
              result.error = ReadResult.ErrorCode.RR_Error;
            } catch (InterruptedException ex) {
              if (optLogger != null) {
                optLogger.logError(logContext, ex);
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
          optLogger.logInfo(logContext, "not started.");
        }
        result.error = ReadResult.ErrorCode.RR_Error;
      }
    }
    if (optLogger != null) {
      optLogger.logInfo(logContext, result.error.name());
    }

    return result;
  }

  @Override
  public synchronized StopResult stop(Logger optLogger) {

    String logContext = null;

    if (optLogger != null) {
      logContext =
          this.getClass().getSimpleName() + ".stop() " + ((portName != null) ? portName : "") + " ";
    }

    if (optLogger != null) {
      optLogger.logInfo(logContext, "stop()\n");
    }

    if (!sharedData.started) {
      if (optLogger != null) {
        optLogger.logInfo(logContext, "Already stopped");
      }
      return StopResult.SR_Success;
    }

    sharedData.started = false;

    try {
      boolean closeResult = serialPort.closePort();
      if (!closeResult) {
        if (optLogger != null) {
          optLogger.logError(logContext, "serial port failed to close");
        }
      }
    } catch (SerialPortException ex) {
      if (optLogger != null) {
        optLogger.logError(logContext, ex);
      }
      return StopResult.SR_Error;
    }

    if (optLogger != null) {
      optLogger.logInfo(logContext, "stopped");
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
    }

    public void clearWaitingThread() {
      this.waitingThread = null;
      this.waitingThreadLogger = null;
      this.waitingThreadLoggerContext = null;
      this.readBuffer = null;
    }

    public void logInfo(String msg) {
      if (waitingThread != null) {
        if (waitingThreadLogger != null) {
          waitingThreadLogger.logInfo(waitingThreadLoggerContext, msg);
        }
      } else {
        System.err.println("[INFO] " + "noWaitingThread " + msg);
      }
    }

    public void logError(String msg) {
      if (waitingThread != null) {
        if (waitingThreadLogger != null) {
          waitingThreadLogger.logError(waitingThreadLoggerContext, msg);
        }
      } else {
        System.err.println("[ERROR] " + "noWaitingThread " + msg);
      }
    }

  }

}
