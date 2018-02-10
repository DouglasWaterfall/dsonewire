package waterfall.onewire;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import waterfall.onewire.busmasters.HA7S.HA7S;
import waterfall.onewire.busmasters.HA7S.HA7SSerial;

/**
 * Created by dwaterfa on 12/10/16.
 */
public class HA7SBusMasterManager {

  private final BusMasterRegistry bmRegistry;
  private ArrayList<HA7S> bmList = new ArrayList<>();

  public HA7SBusMasterManager(BusMasterRegistry bmRegistry) {
    if (bmRegistry == null) {
      throw new IllegalArgumentException("bmRegistry");
    }
    this.bmRegistry = bmRegistry;
  }

  public synchronized HA7S[] start(String ha7sTTYArg, String ttyClassName)
      throws IllegalArgumentException, NoSuchMethodException {

    String[] bmPaths = checkTTYArg(ha7sTTYArg);

    if ((ttyClassName == null) || (ttyClassName.isEmpty())) {
      throw new IllegalArgumentException("ttyClass null or empty");
    }

    Class ttyClass = null;

    try {
      ttyClass = this.getClass().getClassLoader().loadClass(ttyClassName);
      if (ttyClass.isAssignableFrom(HA7SSerial.class)) {
        throw new IllegalArgumentException(ttyClass.getSimpleName() + ": assignable from HA7SSerial.class");
      }
    }
    catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(ttyClassName + ": " + e);
    }

    Constructor c = null;
    try {
      c = ttyClass.getConstructor(java.lang.String.class);
    } catch (NoSuchMethodException e) {
      System.err.println("Class " + ttyClass.toString() + " does not have String constructor");
      throw e;
    }

    ArrayList<HA7S> addedList = new ArrayList<>();

    for (String s : bmPaths) {

      HA7SSerial serial = null;

      try {
        serial = (HA7SSerial)c.newInstance(s);
      } catch (IllegalAccessException e) {
        System.err.println(e);
      } catch (InstantiationException e) {
        System.err.println(e);
      } catch (InvocationTargetException e) {
        System.err.println(e);
      }

      if (serial == null) {
        throw new IllegalArgumentException("cannot instantiate " + c.getName() + "(" + s + ")");
      }

      try {
          startBus(serial);
      }
      catch (RuntimeException e) {
        throw new IllegalArgumentException("cannot start " + serial.getPortName() + " msg:" + e.getMessage());
      }

      // dumpLog(startCmd.getLogger());

      HA7S ha7s = new HA7S(serial);
      bmRegistry.addBusMaster(ha7s);
      addedList.add(ha7s);
    }

    return addedList.toArray(new HA7S[addedList.size()]);
  }

  public synchronized void stop(HA7S ha7s) {
    if (ha7s == null) {
      throw new IllegalArgumentException("ha7s");
    }

    if (ha7s == null) {
      throw new IllegalArgumentException("ha7s is not started");
    }

    bmRegistry.removeBusMaster(ha7s);

    ha7s.stopBus();
  }

  private String[] checkTTYArg(String ttyArg) {
    final String exceptionStr = "ha7sTTYArg";
    String[] bmPaths = null;

    if ((ttyArg == null) || ((bmPaths = ttyArg.split(",")).length == 0)) {
      throw new IllegalArgumentException(exceptionStr);
    }

    for (String s : bmPaths) {
      if (s.isEmpty()) {
        throw new IllegalArgumentException(exceptionStr);
      }
    }

    return bmPaths;
  }

  private void startBus(HA7SSerial serialPort) throws RuntimeException {
    final byte[] resetBusCmd = {'R'};

    HA7SSerial.StartResult startResult = serialPort.start();

    if (startResult != HA7SSerial.StartResult.SR_Success) {
      String message = serialPort.getPortName() + ":" + startResult.name();
      throw new RuntimeException(message);
    }

    byte[] rbuf = new byte[8];

    HA7SSerial.ReadResult readResult = serialPort.writeReadTilCR(resetBusCmd, rbuf);

    if ((readResult.getError() == HA7SSerial.ReadResult.ErrorCode.RR_Success) &&
        (readResult.getReadCount() == 1) &&
        (rbuf[0] == 0x07)) { // bell
      // This can occur during development when when the first thing read after open is
      // 0x07 0x0D. So we try this again once.
      readResult = serialPort.writeReadTilCR(resetBusCmd, rbuf);
    }

    if ((readResult.getError() != HA7SSerial.ReadResult.ErrorCode.RR_Success) ||
        (readResult.getReadCount() != 0)) {
      String message = readResult.getError().name() + " readCount:" + readResult.getReadCount();
      HA7SSerial.StopResult stopResult = serialPort.stop();
      throw new RuntimeException(message);
    }
  }

}

