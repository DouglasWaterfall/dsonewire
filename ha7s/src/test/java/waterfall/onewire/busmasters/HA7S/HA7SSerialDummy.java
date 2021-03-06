package waterfall.onewire.busmasters.HA7S;

import java.util.ArrayList;
import java.util.HashMap;
import waterfall.onewire.Convert;
import waterfall.onewire.DSAddress;

/**
 * Created by dwaterfa on 1/18/17.
 */
public class HA7SSerialDummy implements HA7SSerial {

  // Even values are the char code, odd values are the expected total chars for the command data, -1 means terminated
  // with CR.
  private static final byte cmdLen[] = new byte[]{
      '\r', 1,
      'A', -1,
      'R', 1,
      'S', 1,
      's', 1,
      'F', 3,
      'f', 1,
      'C', 1,
      'c', 1,
      'O', 1,
      'W', -1
  };
  private final HashMap<String, HA7SDummyDevice> deviceDataList = new HashMap<>();
  private String portName;
  private boolean started = false;
  private ActiveSearch activeSearch = null;
  private String activeDeviceHexAddr = null;

  public HA7SSerialDummy(String portName) {
    this.portName = portName;
  }

  @Override
  public String getPortName() {
    return portName;
  }

  @Override
  public boolean isStarted() {
    return started;
  }

  @Override
  public StartResult start() {
    if (!started) {
      started = true;
    }
    return StartResult.SR_Success;
  }

  @Override
  public ReadResult writeReadTilCR(byte[] wBuf, byte[] rBuf) {
    final String logContext = "writeReadTilCR()";

    if (wBuf == null) {
      throw new IllegalArgumentException("null wBuf");
    }

    int wStart = 0;
    int wEnd = 0;

    long postWriteCTM = System.currentTimeMillis();
    int readCount = 0;

      for (; ; ) {
        wEnd = parse(wBuf, wStart);

        if (wEnd == wStart) {
          long readCRCTM;
          while ((readCRCTM = System.currentTimeMillis()) == postWriteCTM) {
            try {
              Thread.sleep(1);
            }
            catch (InterruptedException e) {
              ;
            }
          }
          return new ReadResult(readCount, postWriteCTM, readCRCTM);
        }

        switch (wBuf[wStart]) {
          case 'A':
            readCount += addressSelect(wBuf, wStart, wEnd, rBuf);
            break;

          case 'S':
            readCount += search(wBuf, wStart, wEnd, rBuf);
            break;

          case 's':
            readCount += searchNext(wBuf, wStart, wEnd, rBuf);
            break;

          case 'R':
            readCount += reset(wBuf, wStart, wEnd, rBuf);
            break;

          case 'F':
            readCount += familySearch(wBuf, wStart, wEnd, rBuf);
            break;

          case 'f':
            readCount += familySearchNext(wBuf, wStart, wEnd, rBuf);
            break;

          case 'C':
            readCount += alarmSearch(wBuf, wStart, wEnd, rBuf);
            break;

          case 'c':
            readCount += alarmSearchNext(wBuf, wStart, wEnd, rBuf);
            break;

          case 'O':
            readCount += readBit(wBuf, wStart, wEnd, rBuf);
            break;

          case 'W':
            readCount += writeBlock(wBuf, wStart, wEnd, rBuf);
            break;

          default:
            throw new IllegalArgumentException("unknown cmd code:" + (char) wBuf[wStart]);
        }

        wStart = wEnd;
      }
  }

  @Override
  public StopResult stop() {
    if (started) {
      started = false;
    }
    return StopResult.SR_Success;
  }

  /* For our testing */
  public HA7SSerialDummy addDevice(HA7SDummyDevice device) {
    if (device == null) {
      throw new IllegalArgumentException("device is null");
    }
    if (deviceDataList.containsKey(device.getDSAddress().toString())) {
      throw new IllegalArgumentException("duplicate device " + device.getDSAddress().toString());
    }
    deviceDataList.put(device.getDSAddress().toString(), device);
    return this;
  }

  public HA7SSerialDummy removeDevice(DSAddress dsAddress) {
    if (!deviceDataList.containsKey(dsAddress.toString())) {
      throw new IllegalArgumentException("dsAddress not found " + dsAddress.toString());
    }
    deviceDataList.remove(dsAddress.toString());
    return this;
  }

  public HA7SSerialDummy removeAllDevices() {
    deviceDataList.clear();
    return this;
  }

  private int addressSelect(byte[] wBuf, int wStart, int wEnd, byte[] rBuf) throws IllegalArgumentException {
    if ((wEnd - wStart) != 18) { // A {16} CR
      throw new IllegalArgumentException("address select must include address");
    }
    if (wBuf[wEnd - 1] != 0x0d) {
      throw new IllegalArgumentException("address select must be terminated by CR");
    }
    byte[] dsHexBytes = new byte[16];
    int index = 0;
    for (int i = (wStart + 1); i < (wEnd - 1); i++) {
      if (!(((wBuf[i] >= '0') && (wBuf[i] <= '9')) || ((wBuf[i] >= 'A') && (wBuf[i] <= 'F')))) {
        throw new IllegalArgumentException("address has bad chars");
      }
      rBuf[index] = wBuf[i];
      dsHexBytes[index++] = wBuf[i];
    }
    String dsHexAddr = new String(dsHexBytes);

    if (!deviceDataList.containsKey(dsHexAddr)) {
      // this is technically not an error for the DS - it just does something. We care because of a logic error.
      // however, once we start thinking about device disappearing then we will have to do something useful when
      // the device is not found.
      throw new IllegalArgumentException("address has no device");
    }

    activeDeviceHexAddr = dsHexAddr;
    return 16; // readCount
  }

  private int search(byte[] wBuf, int wStart, int wEnd, byte[] rBuf) throws IllegalArgumentException {
    if ((wEnd - wStart) != 1) {
      throw new IllegalArgumentException("search is single char cmd");
    }

    activeSearch = new ActiveSearch(ActiveSearchType.General,
        deviceDataList.keySet().toArray(new String[0]));
    return activeSearch.nextDSHexAddr(rBuf);

    // never pass back the CR
  }

  private int searchNext(byte[] wBuf, int wStart, int wEnd, byte[] rBuf) throws IllegalArgumentException {
    if ((wEnd - wStart) != 1) {
      throw new IllegalArgumentException("search next is single char cmd");
    }
    if ((activeSearch == null) || (activeSearch.getType() != ActiveSearchType.General)) {
      throw new IllegalArgumentException("general search is not active");
    }

    return activeSearch.nextDSHexAddr(rBuf);

    // never pass back the CR
  }

  private int alarmSearch(byte[] wBuf, int wStart, int wEnd, byte[] rBuf) throws IllegalArgumentException {
    if ((wEnd - wStart) != 1) {
      throw new IllegalArgumentException("alarm search is single char cmd");
    }

    ArrayList<String> alarms = new ArrayList<>();
    for (HA7SDummyDevice dd : deviceDataList.values()) {
      if (dd.hasAlarm()) {
        alarms.add(dd.getDSAddress().toString());
      }
    }
    activeSearch = new ActiveSearch(ActiveSearchType.Alarm, alarms.toArray(new String[0]));

    return activeSearch.nextDSHexAddr(rBuf);

    // never pass back the CR
  }

  private int alarmSearchNext(byte[] wBuf, int wStart, int wEnd, byte[] rBuf) throws IllegalArgumentException {
    if ((wEnd - wStart) != 1) {
      throw new IllegalArgumentException("alarm search next is single char cmd");
    }

    if ((activeSearch == null) || (activeSearch.getType() != ActiveSearchType.Alarm)) {
      throw new IllegalArgumentException("alarm search is not active");
    }

    return activeSearch.nextDSHexAddr(rBuf);

    // never pass back the CR
  }

  private int familySearch(byte[] wBuf, int wStart, int wEnd, byte[] rBuf) throws IllegalArgumentException {
    if ((wEnd - wStart) != 3) {
      throw new IllegalArgumentException("family search cmd length must be three");
    }

    short familySearchCode = (short) ((Convert.hexToFourBits(wBuf[wStart + 1]) << 4) | Convert
        .hexToFourBits(wBuf[wStart + 2]));

    ArrayList<String> families = new ArrayList<>();
    for (HA7SDummyDevice dd : deviceDataList.values()) {
      if ((dd.getDSAddress().getFamilyCode()) == familySearchCode) {
        families.add(dd.getDSAddress().toString());
      }
    }
    activeSearch = new ActiveSearch(ActiveSearchType.Family, families.toArray(new String[0]));

    return activeSearch.nextDSHexAddr(rBuf);

    // never pass back the CR
  }

  private int familySearchNext(byte[] wBuf, int wStart, int wEnd, byte[] rBuf) throws IllegalArgumentException {
    if ((wEnd - wStart) != 1) {
      throw new IllegalArgumentException("family search next is single char cmd");
    }

    if ((activeSearch == null) || (activeSearch.getType() != ActiveSearchType.Family)) {
      throw new IllegalArgumentException("family search is not active");
    }

    return activeSearch.nextDSHexAddr(rBuf);

    // never pass back the CR
  }

  private int reset(byte[] wBuf, int wStart, int wEnd, byte[] rBuf) throws IllegalArgumentException {
    if ((wEnd - wStart) != 1) {
      throw new IllegalArgumentException("Reset is single cmd char");
    }

    activeSearch = null;
    activeDeviceHexAddr = null;

    // leave readCount alone
    return 0;
  }

  private int readBit(byte[] wBuf, int wStart, int wEnd, byte[] rBuf) throws IllegalArgumentException {
    if ((wEnd - wStart) != 1) {
      throw new IllegalArgumentException("ReadBit is single cmd char");
    }

    rBuf[0] = '0';
    return 1;
  }

  private int writeBlock(byte[] wBuf, int wStart, int wEnd, byte[] rBuf) throws IllegalArgumentException {
    if ((wEnd - wStart) < 3) {
      throw new IllegalArgumentException("Write block must be at least 3 chars");
    }
    if (wBuf[wEnd - 1] != 0x0d) {
      throw new IllegalArgumentException("WriteBlock must be terminated by CR");
    }
    if (activeDeviceHexAddr == null) {
      throw new IllegalArgumentException("Write block has no active device addr");
    }
    HA7SDummyDevice dd = deviceDataList.get(activeDeviceHexAddr);
    if (dd == null) {
      throw new IllegalArgumentException("Write block cannot find active device addr");
    }
    short end = 0;
    for (int i = wStart + 3; i < (wEnd - 1); i++) {
      rBuf[end++] = wBuf[i];
    }
    try {
      dd.writeBlock(rBuf, (short) 0, end);
      return (end - 0);
    } catch (Exception e) {
      throw new RuntimeException("Write block device threw exception", e);
    }
  }

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
      throw new IllegalArgumentException("cmd byte note found:" + (char) wBuf[wStart]);
    }

    if (found != -1) {
      // fixed length
      if ((wStart + found) > wBuf.length) {
        throw new IllegalArgumentException(
            "cmd byte overrun:" + (char) wBuf[wStart] + " needs:" + found);
      }

      return (wStart + found);
    }

    // variable length terminated by cr
    int wEnd = wStart;
    while ((wEnd < wBuf.length) && (wBuf[wEnd] != '\r')) {
      wEnd++;
    }

    if (wEnd == wBuf.length) {
      throw new IllegalArgumentException(
          "cmd byte underrun:" + (char) wBuf[wStart] + " CR not found");
    } else {
      // we want to be PAST the CR.
      return (wEnd + 1);
    }
  }

  private enum ActiveSearchType {
    General,
    Alarm,
    Family
  }

  private class ActiveSearch {

    private final ActiveSearchType type;
    private final String[] dsHexAddrs;
    private int index;

    public ActiveSearch(ActiveSearchType type, String[] dsHexAddrs) {
      this.type = type;
      this.dsHexAddrs = dsHexAddrs;
      index = 0;
    }

    public ActiveSearchType getType() {
      return type;
    }

    public short nextDSHexAddr(byte[] rBuf) {
      short readCount = 0;

      if (index < dsHexAddrs.length) {
        for (int i = 0; i < 16; i++) {
          rBuf[i] = (byte) dsHexAddrs[index].charAt(i);
        }

        index++;
        readCount += 16;
      }

      return readCount;
    }
  }

}
