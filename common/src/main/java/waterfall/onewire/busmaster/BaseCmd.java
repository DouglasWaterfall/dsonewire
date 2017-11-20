package waterfall.onewire.busmaster;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by dwaterfa on 7/7/16.
 */
public class BaseCmd implements Logger {

  protected final BusMaster busMaster;
  protected ArrayList<String> logger;
  protected LogLevel logLevel;
  protected String logContext;

  public BaseCmd(BusMaster busMaster) {
    if (busMaster == null) {
      throw new IllegalArgumentException("busMaster");
    }
    this.busMaster = busMaster;
    setLogLevel(null);
  }

  /**
   * The BusMaster the command is attached to
   */
  public BusMaster getBusMaster() {
    return busMaster;
  }

  /**
   *
   * @return
   */
  public Logger getLogger() {
    return (logger != null) ? this : null;
  }

  /**
   *
   * @return
   */
  public LogLevel getLogLevel() {
    return logLevel;
  }

  /**
   *
   * @param logLevel
   */
  public void setLogLevel(LogLevel logLevel) {
    if ((logLevel != null) && (logLevel.isAnyLevelSet())) {
      this.logger = new ArrayList<String>();
      this.logLevel = logLevel;
      this.logContext = this.getClass().getSimpleName() + " ";
    } else {
      this.logger = null;
      this.logLevel = null;
      this.logContext = null;
    }
  }

  /**
   *
   * @param msg
   */
  public void logDebug(String msg) {
    logDebug(logContext, msg);
  }

  /**
   *
   * @param msg
   */
  public void logInfo(String msg) {
    logInfo(logContext, msg);
  }

  /**
   *
   * @param msg
   */
  public void logError(String msg) {
    logError(logContext, msg);
  }

  /**
   *
   * @param e
   */
  public void logError(Throwable e) {
    logError(logContext, e);
  }


  /**
   * interface Logger.logError()
   */
  public void logError(String context, String msg) {
    if (logger != null) {
      logger.add("[ERROR] " + context + msg);
    }
  }

  /**
   * interface Logger.logError()
   */
  public void logError(String context, Throwable t) {
    if (logger != null) {
      ByteArrayOutputStream baStream = new ByteArrayOutputStream();
      PrintStream pStream = new PrintStream(baStream);
      t.printStackTrace(pStream);
      logger.add("[ERROR] " + context + baStream.toString());
    }
  }

  /**
   * interface Logger.logInfo()
   */
  public void logInfo(String context, String msg) {
    if (logger != null) {
      logger.add("[INFO] " + context + msg);
    }
  }

  /**
   * interface Logger.logDebug()
   */
  public void logDebug(String context, String msg) {
    if (logger != null) {
      logger.add("[DEBUG] " + context + msg);
    }
  }

  /**
   * Merge another log stream into this one.
   */
  public void logMerge(ArrayList<String> other) {
    if (logger != null) {
      for (String s : other) {
        logger.add(s);
      }
    }
  }

  /**
   * interface Logger.getLogSize()
   */
  public int getLogSize() {
    if (logger != null) {
      return logger.size();
    }
    return 0;
  }

  /**
   * interface Logger.getLogIter()
   */
  public Iterator<String> getLogIter() {
    if (logger != null) {
      return logger.iterator();
    }
    return null;
  }

  /**
   * interface Logger.clearLog()
   */
  public void clearLog() {
    if (logger != null) {
      logger.clear();
    }
  }

}

