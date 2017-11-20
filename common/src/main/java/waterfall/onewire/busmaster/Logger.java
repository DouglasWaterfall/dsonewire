package waterfall.onewire.busmaster;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by dwaterfa on 6/12/16.
 */
public interface Logger {

  public LogLevel getLogLevel();

  public void logInfo(String context, String str);

  public void logError(String context, String str);

  public void logError(String context, Throwable t);

  public void logMerge(ArrayList<String> data);

  public int getLogSize();

  public Iterator<String> getLogIter();

  public void clearLog();

  public class LogLevel {

    public boolean device;
    public boolean cmd;
    public boolean comm;
    // public boolean app;

    public LogLevel() {
      this.device = false;
      this.cmd = false;
      this.comm = false;
    }

    public LogLevel(boolean device, boolean cmd, boolean comm) {
      this.device = device;
      this.cmd = cmd;
      this.comm = comm;
    }

    public static LogLevel DeviceOnlyLevel() {
      return new LogLevel(true, false, false);
    }

    public static LogLevel CmdOnlyLevel() {
      return new LogLevel(false, true, false);
    }

    public static LogLevel CommOnlyLevel() {
      return new LogLevel(false, false, true);
    }

    public boolean isLevelDevice() {
      return device;
    }

    public LogLevel setLevelDevice() {
      this.device = true;
      return this;
    }

    public boolean isLevelCmd() {
      return cmd;
    }

    public LogLevel setLevelCmd() {
      this.cmd = true;
      return this;
    }

    public boolean isLevelComm() {
      return comm;
    }

    public LogLevel setLevelComm() {
      this.comm = true;
      return this;
    }

    public boolean isAnyLevelSet() {
      return ((device != false) || (cmd != false) || (comm != false));
    }

  }

}
