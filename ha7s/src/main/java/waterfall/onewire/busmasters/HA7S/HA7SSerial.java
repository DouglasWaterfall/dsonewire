package waterfall.onewire.busmasters.HA7S;

import java.util.ArrayList;
import java.util.List;
import waterfall.onewire.Convert;
import waterfall.onewire.DSAddress;
import waterfall.onewire.HexByteArray;
import waterfall.onewire.busmaster.Logger;

public abstract class HA7SSerial {

  /**
   * Some human readable description of this physical device we are talking through.
   */
  public abstract String getPortName();

  /**
   * Start the serial connector.
   *
   * @return 0 if successful, negative values for errors
   */
  public abstract StartResult start(Logger optLogger);

  /**
   * The HA7S uses a write/read sequence to communicate with the host and this method will write
   * bytes (terminated by the caller with a CR as appropriate) and then wait for a response which
   * will always be terminated by the a CR.
   *
   * @param wBuf array of byte buffers to write from
   * @param rBuf byte buffer to place read bytes into, expected length, including space for CR is
   * what is expected to be read.
   * @return ReadResult
   */
  public abstract ReadResult writeReadTilCR(byte wBuf[], byte rBuf[], Logger optLogger);

  /**
   * Stop the serial connector.
   *
   * @return StopResult
   */
  public abstract StopResult stop(Logger optLogger);

  /**
   * @param dsAddress
   * @param optLogger
   * @return
   */
  public NoDataResult cmdAddress(DSAddress dsAddress, Logger optLogger) {
    if (dsAddress == null) {
      throw new NullPointerException("dsAddress");
    }

    final byte[] selectCmdData = new byte[]{
        'A',
        'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F',
        0x0D
    };

    String s = dsAddress.toString();
    for (int i = 0; i < 16; i++) {
      selectCmdData[1 + i] = (byte) s.charAt(i);
    }

    byte[] rbuf = new byte[16];

    HA7SSerial.ReadResult readResult = writeReadTilCR(selectCmdData, rbuf, optLogger);

    if (readResult.error != ReadResult.ErrorCode.RR_Success) {
      return new NoDataResult().setFailure(readResult.error.name());
    }

    if (readResult.readCount != 16) {
      return new NoDataResult().setFailure("Underrun - expected 16 got:" + readResult.readCount);
    }

    // Expect to match what we passed in
    for (int i = 0; i < 16; i++) {
      if (rbuf[i] != s.charAt(i)) {
        return new NoDataResult().setFailure("Invalid char index:" + i);
      }
    }

    return new NoDataResult().setSuccess(readResult.postWriteCTM, readResult.readCRCTM);
  }

  /**
   * @param optLogger
   * @return
   */
  public HexByteArrayListResult cmdSearch(Logger optLogger) {
    return cmdSearchInternal(new byte[]{'S'}, new byte[] {'s'}, optLogger);
  }

  /**
   *
   * @param optLogger
   * @return
   */
  public HexByteArrayListResult cmdConditionalSearch(Logger optLogger) {
    return cmdSearchInternal(new byte[]{'C'}, new byte[] {'c'}, optLogger);
  }

  /**
   *
   * @param familyCode
   * @param optLogger
   * @return
   */
  public HexByteArrayListResult cmdFamilySearch(short familyCode, Logger optLogger) {
    if ((familyCode < 0) || (familyCode > 255)) {
      throw new IllegalArgumentException("Bad familyCode");
    }
    byte[] cmdData = new byte[] {'F',
        Convert.fourBitsToHex(familyCode >> 4),
        Convert.fourBitsToHex(familyCode & 0xf)};
    byte[] cmdNextData = new byte[] {'f'};
    return cmdSearchInternal(cmdData, cmdNextData, optLogger);
  }

  /**
   *
   * @param cmdData
   * @param optLogger
   * @return
   */
  private HexByteArrayListResult cmdSearchInternal(byte[] cmdData, byte[] nextCmdData,
      Logger optLogger) {
    ArrayList<byte[]> resultList = new ArrayList<>();

    long firstPostWriteCTM = 0;

    int index = 0;
    while (true) {
      byte[] rHexBuf = new byte[16];

      boolean first = (index == 0);

      HA7SSerial.ReadResult readResult = writeReadTilCR((first ? cmdData : nextCmdData), rHexBuf,
          optLogger);

      if (readResult.error != ReadResult.ErrorCode.RR_Success) {
        return new HexByteArrayListResult().setFailure(readResult.error.name());
      }

      if (first) {
        firstPostWriteCTM = readResult.postWriteCTM;
      }

      if (readResult.readCount == 0) {
        return new HexByteArrayListResult().setSuccess(resultList, firstPostWriteCTM,
            readResult.readCRCTM);
      }

      if (readResult.readCount != 16) {
        return new HexByteArrayListResult()
            .setFailure("Underrun - expected 0 or 16 got:" + readResult.readCount);
      }

      if (!isValidUpperCaseHex(rHexBuf, readResult.readCount)) {
        return new HexByteArrayListResult().setFailure("Not hex bytes");
      }

      resultList.add(rHexBuf);
      index++;
    }
  }

  /**
   *
   * @param optLogger
   * @return
   */
  public NoDataResult cmdReset(Logger optLogger) {
    byte[] cmdData = new byte[]{'R'};
    byte[] rbuf = new byte[0];

    HA7SSerial.ReadResult readResult = writeReadTilCR(cmdData, rbuf, optLogger);

    if (readResult.error != ReadResult.ErrorCode.RR_Success) {
      return new NoDataResult().setFailure(readResult.error.name());
    }

    return new NoDataResult().setSuccess(readResult.postWriteCTM, readResult.readCRCTM);
  }

  /**
   *
   * @param optLogger
   * @return
   */
  public BooleanResult cmdReadBit(Logger optLogger) {
    byte[] cmdData = new byte[]{'O'};
    byte[] rbuf = new byte[1];

    HA7SSerial.ReadResult readResult = writeReadTilCR(cmdData, rbuf, optLogger);

    if (readResult.error != ReadResult.ErrorCode.RR_Success) {
      return new BooleanResult().setFailure(readResult.error.name());
    }

    if (readResult.readCount != 1) {
      return new BooleanResult().setFailure("Underrun - expected 1 byte");
    }

    boolean v;

    if (rbuf[0] == '0') {
      v = false;
    } else if (rbuf[0] == '1') {
      v = false;
    } else {
      return new BooleanResult().setFailure("Data error - not 0 or 1");
    }

    return new BooleanResult().setSuccess(v, readResult.postWriteCTM, readResult.readCRCTM);
  }

  /**
   *
   * @param wData
   * @param optLogger
   * @return
   */
  public HexByteArrayResult cmdWriteBlock(HexByteArray wData, Logger optLogger) {
    if (wData == null) {
      throw new NullPointerException("wData");
    }
    if (wData.size() < 1) {
      throw new IllegalArgumentException("wData.size < 1");
    }
    if (wData.size() > 32) {
      throw new IllegalArgumentException("wData.size > 32");
    }

    int bCount = wData.getRaw().length;
    byte[] cmdData = new byte[1 + 2 + bCount + 1];
    cmdData[0] = 'W';
    cmdData[1] = Convert.fourBitsToHex(bCount >> 4);
    cmdData[2] = Convert.fourBitsToHex(bCount & 0x0f);
    for (int i = 0; i < bCount; i++) {
      cmdData[3 + i] = wData.getRaw()[i];
    }
    cmdData[cmdData.length - 1] = 0x0D;

    byte[] rbuf = new byte[bCount];

    HA7SSerial.ReadResult readResult = writeReadTilCR(cmdData, rbuf, optLogger);

    if (readResult.error != ReadResult.ErrorCode.RR_Success) {
      return new HexByteArrayResult().setFailure(readResult.error.name());
    }

    if (readResult.readCount != bCount) {
      return new HexByteArrayResult()
          .setFailure("Underrun - expected:" + bCount + " got:" + readResult.readCount);
    }

    if (!isValidUpperCaseHex(rbuf, readResult.readCount)) {
      return new HexByteArrayResult().setFailure("Not hex bytes");
    }

    return new HexByteArrayResult()
        .setSuccess(rbuf, readResult.postWriteCTM, readResult.readCRCTM);
  }

  /**
   *
   * @param buf
   * @param count
   * @return
   */
  public boolean isValidUpperCaseHex(byte[] buf, int count) {
    for (int i = 0; i < count; i++) {
      if (!(((buf[i] >= 'A') && (buf[i] <= 'F')) || ((buf[i] >= '0') && (buf[i] <= '9')))) {
        return false;
      }
    }
    return true;
  }

  /**
   *
   */
  public enum StartResult {
    /**
     * The serial device has been started or was already started.
     */
    SR_Success,

    /**
     * No port has been specified.
     */
    SR_NoPortName,

    /**
     * The port is already in use.
     */
    SR_Busy,

    /**
     * an internal exception was caught and logged.
     */
    SR_Error

  }

  /**
   *
   */
  public enum StopResult {
    /**
     * The serial device has been started or was already started.
     */
    SR_Success,

    /**
     * Failed. See log for more information.
     */
    SR_Error

  }

  public static class ReadResult {

    /**
     * Result of the call. Initialized to null.
     */
    private ErrorCode error;

    /**
     * Number of bytes actually read, NOT including the terminating CR. So if the return returns
     * RR_Success and the readCount was zero then ONLY a CR return was read (and not returned). You
     * cannot read the CR through this API.
     */
    private int readCount;

    /**
     * To be filled in with System.currentTimeMillis() after the final write has completed.
     */
    private Long postWriteCTM; // non null if valid

    /**
     * To be filled in with System.currentTimeMillis() after the CR has been read.
     */
    private Long readCRCTM; // non null if valid

    public ReadResult() {
      error = null;
      readCount = 0;
      postWriteCTM = null;
      readCRCTM = null;
    }

    public ReadResult(ErrorCode error) {
      this.error = error;
      readCount = 0;
      postWriteCTM = null;
      readCRCTM = null;
    }

    public ReadResult(int readCount, long postWriteCTM, long readCRCTM) {
      this.error = ErrorCode.RR_Success;
      this.readCount = readCount;
      this.postWriteCTM = postWriteCTM;
      this.readCRCTM = readCRCTM;
    }

    public ErrorCode getError() {
      return error;
    }

    public int getReadCount() {
      return readCount;
    }

    public long getPostWriteCTM() {
      return postWriteCTM;
    }

    public long getReadCRCTM() {
      return readCRCTM;
    }

    public enum ErrorCode {
      /**
       * The expected number of bytes were read.
       */
      RR_Success,

      /**
       * Too many bytes to fill the size of rbuf. readCount is the total number of bytes up to and
       * including the CR, but only rbuf.length bytes will be returned.
       */
      RR_ReadOverrun,

      /**
       * The read did not return enough characters in the time allowed by the call. One or more
       * bytes may be stored in which case readCount will be advanced.
       */
      RR_ReadTimeout,

      /**
       * An exception was thrown internally. The log will have more information.
       */
      RR_Error

    }

  }

  /**
   *
   */
  public static class BaseCmdResult {

    protected long writeCTM;
    protected long readCRCTM;
    protected boolean success;
    protected String errorMsg;

    protected BaseCmdResult() {
      writeCTM = -1;
      readCRCTM = -1;
      success = false;
      errorMsg = null;
    }

    protected BaseCmdResult setSuccess(long writeCTM, long readCRCTM) {
      if ((writeCTM < 0) || (readCRCTM < 0)) {
        throw new IllegalArgumentException("setSuccess() bad values for writeCTM or readCRCTM");
      }
      if ((success != false) || (errorMsg != null)) {
        throw new IllegalArgumentException("setSuccess() called twice");
      }
      this.writeCTM = writeCTM;
      this.readCRCTM = readCRCTM;
      success = true;
      return this;
    }

    public BaseCmdResult setFailure(String errorMsg) {
      if ((errorMsg == null) || (errorMsg.isEmpty())) {
        throw new IllegalArgumentException("setFailure() message must not be blank");
      }
      if ((success != false) || (this.errorMsg != null)) {
        throw new IllegalArgumentException("setFailure() called twice");
      }
      this.errorMsg = errorMsg;
      return this;
    }

    public boolean isSuccess() {
      return success;
    }

    public String getErrorMsg() {
      if (errorMsg == null) {
        throw new IllegalArgumentException("errorMsg uninitialized");
      }
      return errorMsg;
    }

    public long getWriteCTM() {
      if (writeCTM == -1) {
        throw new IllegalArgumentException("writeCTM uninitialized");
      }
      return writeCTM;
    }

    public long getReadCRCTM() {
      if (readCRCTM == -1) {
        throw new IllegalArgumentException("readCRCTM uninitialized");
      }
      return readCRCTM;
    }

    @Override
    public boolean equals(Object other) {
      return ((other != null) &&
          (other instanceof BaseCmdResult) &&
          (((BaseCmdResult) other).writeCTM == writeCTM) &&
          (((BaseCmdResult) other).readCRCTM == readCRCTM) &&
          (((BaseCmdResult) other).success == success) &&
          (((((BaseCmdResult) other).errorMsg == null) && (errorMsg == null)) ||
              ((((BaseCmdResult) other).errorMsg != null) && (errorMsg != null)
                  && (((BaseCmdResult) other).errorMsg.equals(errorMsg)))));
    }

  }

  public static class NoDataResult extends BaseCmdResult {

    public NoDataResult() {
      super();
    }

    public NoDataResult setSuccess(long writeCTM, long readCRCTM) {
      super.setSuccess(writeCTM, readCRCTM);
      return this;
    }

    public NoDataResult setFailure(String errorMsg) {
      super.setFailure(errorMsg);
      return this;
    }

    @Override
    public boolean equals(Object other) {
      return (super.equals(other) && (other instanceof NoDataResult));
    }
  }

  public static class HexByteArrayListResult extends BaseCmdResult {

    private List<byte[]> value;

    public HexByteArrayListResult() {
      super();
      value = null;
    }

    public HexByteArrayListResult setSuccess(List<byte[]> hexByteArrayList, long writeCTM,
        long readCRCTM) {
      super.setSuccess(writeCTM, readCRCTM);
      if ((hexByteArrayList != null) && (hexByteArrayList.size() > 0)) {
        this.value = hexByteArrayList;
      }
      return this;
    }

    public HexByteArrayListResult setFailure(String errorMsg) {
      super.setFailure(errorMsg);
      return this;
    }

    // may return null if none
    public List<byte[]> getValue() {
      if (!success) {
        throw new IllegalArgumentException("getValue() not success");
      }
      return value;
    }

    @Override
    public boolean equals(Object other) {
      return (super.equals(other) &&
          (other instanceof HexByteArrayListResult) &&
          (((((HexByteArrayListResult) other).value == null) && (value == null)) ||
              ((((HexByteArrayListResult) other).value != null) && (value != null)
                  && (((HexByteArrayListResult) other).value.equals(value)))));
    }
  }

  public static class HexByteArrayResult extends BaseCmdResult {

    private HexByteArray value;

    public HexByteArrayResult() {
      super();
      value = null;
    }

    public HexByteArrayResult setSuccess(byte[] hexData, long writeCTM, long readCRCTM) {
      super.setSuccess(writeCTM, readCRCTM);
      if ((hexData != null) && (hexData.length > 0)) {
        this.value = new HexByteArray(hexData);
      }
      return this;
    }

    public HexByteArrayResult setFailure(String errorMsg) {
      super.setFailure(errorMsg);
      return this;
    }

    // may return null if none
    public HexByteArray getValue() {
      if (!success) {
        throw new IllegalArgumentException("getValue() not success");
      }
      return value;
    }

    @Override
    public boolean equals(Object other) {
      return (super.equals(other) &&
          (other instanceof HexByteArrayResult) &&
          (((((HexByteArrayResult) other).value == null) && (value == null)) ||
              ((((HexByteArrayResult) other).value != null) && (value != null)
                  && (((HexByteArrayResult) other).value.equals(value)))));
    }
  }

  public static class BooleanResult extends BaseCmdResult {

    private boolean value;

    public BooleanResult() {
      super();
      value = false;
    }

    public BooleanResult setSuccess(boolean value, long writeCTM, long readCRCTM) {
      super.setSuccess(writeCTM, readCRCTM);
      this.value = value;
      return this;
    }

    public BooleanResult setFailure(String errorMsg) {
      super.setFailure(errorMsg);
      return this;
    }

    public boolean getValue() {
      if (!success) {
        throw new IllegalArgumentException("getValue() not success");
      }
      return value;
    }

    @Override
    public boolean equals(Object other) {
      return (super.equals(other) &&
          (other instanceof BooleanResult) &&
          (((BooleanResult) other).value == value));
    }
  }
}
