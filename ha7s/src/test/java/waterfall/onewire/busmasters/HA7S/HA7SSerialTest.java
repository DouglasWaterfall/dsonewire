package waterfall.onewire.busmasters.HA7S;

import java.util.ArrayList;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import waterfall.onewire.DSAddress;
import waterfall.onewire.HexByteArray;
import waterfall.onewire.busmaster.Logger;

/**
 * Created by dwaterfa on 8/12/17.
 */
public class HA7SSerialTest {

  /**
   * Returns the answer from a call to HA7SSerial.writeReadTilCR()
   */
  public static Answer<HA7SSerial.ReadResult> makeWriteReadTilCRResult(HA7SSerial serial) {
    return new Answer<HA7SSerial.ReadResult>() {
      @Override
      public HA7SSerial.ReadResult answer(final InvocationOnMock invocation) {
        Assert.assertEquals(invocation.getArguments().length, 3);
        byte[] wbuf = (byte[]) (invocation.getArguments())[0];
        byte[] rbuf = (byte[]) (invocation.getArguments())[1];
        Logger logger = (Logger) (invocation.getArguments())[2];

        return serial.writeReadTilCR(wbuf, rbuf, logger);
      }
    };
  }

  @DataProvider
  public Object[][] cmdAddressNegativeCasesProvider() {
    DSAddress dsAddress = DSAddress.fromUncheckedHex("EE0000065BC0AE28");
    Logger optLogger = null;

    byte[] nullRBuf = null;
    long writeCTM = 5;
    long readCRCTM = 7;

    byte[] mismatchDSAddress = dsAddress.toString().getBytes();
    mismatchDSAddress[1] = '0';

    return new Object[][]{
        // Error - ReadOverrun
        {dsAddress, optLogger,
            new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadOverrun), nullRBuf,
            writeCTM,
            new HA7SSerial.NoDataResult().setFailure("RR_ReadOverrun")
        },
        // Error - ReadTimeout
        {dsAddress, optLogger,
            new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadTimeout), nullRBuf,
            writeCTM,
            new HA7SSerial.NoDataResult().setFailure("RR_ReadTimeout")
        },
        // Error - Error
        {dsAddress, optLogger,
            new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Error), nullRBuf, writeCTM,
            new HA7SSerial.NoDataResult().setFailure("RR_Error")
        },
        // Success - Read underrun
        {dsAddress, optLogger,
            new HA7SSerial.ReadResult(6, writeCTM, readCRCTM),
            nullRBuf, writeCTM,
            new HA7SSerial.NoDataResult().setFailure("Underrun - expected 16 got:6")
        },
        // Success - Bad hex chars
        {dsAddress, optLogger,
            new HA7SSerial.ReadResult(16, writeCTM, readCRCTM),
            mismatchDSAddress, writeCTM,
            new HA7SSerial.NoDataResult().setFailure("Invalid char index:1")
        }
    };
  }

  @Test(dataProvider = "cmdAddressNegativeCasesProvider")
  public void cmdAddressNegativeTests(DSAddress dsAddress, Logger optLogger,
      HA7SSerial.ReadResult readResult, byte[] readRBufData, long readWriteCTM,
      HA7SSerial.NoDataResult expectedResult) {

    TestHA7SSerial serial = new TestHA7SSerial(readResult, readRBufData, readWriteCTM);
    HA7SSerial.NoDataResult result = serial.cmdAddress(dsAddress, optLogger);
    Assert.assertEquals(result, expectedResult);
  }

  @DataProvider
  public Object[][] cmdResetNegativeCasesProvider() {
    Logger optLogger = null;

    byte[] nullRBuf = null;
    long writeCTM = 5;

    return new Object[][]{
        // Error - ReadOverrun
        {optLogger,
            new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadOverrun), nullRBuf,
            writeCTM,
            new HA7SSerial.NoDataResult().setFailure("RR_ReadOverrun")
        },
        // Error - ReadTimeout
        {optLogger,
            new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadTimeout), nullRBuf,
            writeCTM,
            new HA7SSerial.NoDataResult().setFailure("RR_ReadTimeout")
        },
        // Error - Error
        {optLogger,
            new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Error), nullRBuf, writeCTM,
            new HA7SSerial.NoDataResult().setFailure("RR_Error")
        }
    };
  }

  @Test(dataProvider = "cmdResetNegativeCasesProvider")
  public void cmdResetNegativeTests(Logger optLogger,
      HA7SSerial.ReadResult readResult, byte[] readRBufData, long readWriteCTM,
      HA7SSerial.NoDataResult expectedResult) {

    TestHA7SSerial serial = new TestHA7SSerial(readResult, readRBufData, readWriteCTM);
    HA7SSerial.NoDataResult result = serial.cmdReset(optLogger);
    Assert.assertEquals(result, expectedResult);
  }

  @DataProvider
  public Object[][] cmdSearchNegativeCasesProvider() {
    Logger optLogger = null;

    byte[] nullRBuf = null;
    long writeCTM = 5;
    long readCRCTM = 8;

    byte[] mismatchDSAddress = ("EG0000065BC0AE28").getBytes();

    return new Object[][]{
        // Error - ReadOverrun
        {optLogger,
            new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadOverrun), nullRBuf,
            writeCTM,
            new HA7SSerial.HexByteArrayResult().setFailure("RR_ReadOverrun")
        },
        // Error - ReadTimeout
        {optLogger,
            new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadTimeout), nullRBuf,
            writeCTM,
            new HA7SSerial.HexByteArrayResult().setFailure("RR_ReadTimeout")
        },
        // Error - Error
        {optLogger,
            new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Error), nullRBuf, writeCTM,
            new HA7SSerial.HexByteArrayResult().setFailure("RR_Error")
        },
        // Success - Read underrun
        {optLogger,
            new HA7SSerial.ReadResult(6, writeCTM, readCRCTM),
            nullRBuf, writeCTM,
            new HA7SSerial.HexByteArrayResult().setFailure("Underrun - expected 0 or 16 got:6")
        },
        // Success - Bad hex chars
        {optLogger,
            new HA7SSerial.ReadResult(16, writeCTM, readCRCTM),
            mismatchDSAddress, writeCTM,
            new HA7SSerial.HexByteArrayResult().setFailure("Not hex bytes")
        }
    };
  }

  @Test(dataProvider = "cmdSearchNegativeCasesProvider")
  public void cmdSearchNegativeTests(Logger optLogger,
      HA7SSerial.ReadResult readResult, byte[] readRBufData, long readWriteCTM,
      HA7SSerial.HexByteArrayResult expectedResult) {

    TestHA7SSerial serial = new TestHA7SSerial(readResult, readRBufData, readWriteCTM);
    HA7SSerial.HexByteArrayListResult result = serial.cmdSearch(optLogger);
    Assert.assertEquals(result, expectedResult);
  }

  @Test(dataProvider = "cmdSearchNegativeCasesProvider")
  public void cmdConditionalSearchNegativeTests(Logger optLogger,
      HA7SSerial.ReadResult readResult, byte[] readRBufData, long readWriteCTM,
      HA7SSerial.HexByteArrayResult expectedResult) {

    TestHA7SSerial serial = new TestHA7SSerial(readResult, readRBufData, readWriteCTM);
    HA7SSerial.HexByteArrayListResult result = serial.cmdConditionalSearch(optLogger);
    Assert.assertEquals(result, expectedResult);
  }

  @DataProvider
  public Object[][] cmdFamilySearchNegativeCasesProvider() {
    Logger optLogger = null;

    byte[] nullRBuf = null;
    long writeCTM = 5;

    Short goodFamilyCode = 22;

    ArrayList<Object[]> cases = new ArrayList<>();
    Object[][] baseCases = cmdSearchNegativeCasesProvider();

    for (Object[] _case : baseCases) {
      Object[] mergedCase = new Object[_case.length + 1];
      mergedCase[0] = goodFamilyCode;
      for (int i = 0; i < _case.length; i++) {
        mergedCase[1 + i] = _case[i];
      }
      cases.add(mergedCase);
    }

    return cases.toArray(new Object[cases.size()][]);
  }

  @DataProvider
  public Object[][] cmdFamilySearchNegativeArgCasesProvider() {
    Logger optLogger = null;

    Short badFamilyCode_minus1 = -1;
    Short badFamilyCode_256 = 256;

    return new Object[][]{
        {badFamilyCode_minus1, optLogger},
        {badFamilyCode_256, optLogger}
    };
  }

  @Test(dataProvider = "cmdFamilySearchNegativeCasesProvider")
  public void cmdFamilySearchNegativeTests(short familyCode, Logger optLogger,
      HA7SSerial.ReadResult readResult, byte[] readRBufData, long readWriteCTM,
      HA7SSerial.HexByteArrayResult expectedResult) {

    TestHA7SSerial serial = new TestHA7SSerial(readResult, readRBufData, readWriteCTM);
    HA7SSerial.HexByteArrayListResult result = serial.cmdFamilySearch(familyCode, optLogger);
    Assert.assertEquals(result, expectedResult);
  }

  @Test(dataProvider = "cmdFamilySearchNegativeArgCasesProvider",
      expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = "Bad familyCode")
  public void cmdFamilySearchNegativeArgTests(short familyCode, Logger optLogger) {
    TestHA7SSerial serial = new TestHA7SSerial(null, null, 0);
    serial.cmdFamilySearch(familyCode, optLogger);
  }

  @DataProvider
  public Object[][] cmdReadBitNegativeCasesProvider() {
    Logger optLogger = null;

    byte[] nullRBuf = null;
    long writeCTM = 5;
    long readCRCTM = 9;

    return new Object[][]{
        // Error - ReadOverrun
        {optLogger,
            new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadOverrun),
            nullRBuf, writeCTM,
            new HA7SSerial.BooleanResult().setFailure("RR_ReadOverrun")
        },
        // Error - ReadTimeout
        {optLogger,
            new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadTimeout),
            nullRBuf, writeCTM,
            new HA7SSerial.BooleanResult().setFailure("RR_ReadTimeout")
        },
        // Error - Error
        {optLogger,
            new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Error),
            nullRBuf, writeCTM,
            new HA7SSerial.BooleanResult().setFailure("RR_Error")
        },
        // Success - Bad count
        {optLogger,
            new HA7SSerial.ReadResult(0, writeCTM, readCRCTM),
            new byte[]{0}, writeCTM,
            new HA7SSerial.BooleanResult().setFailure("Underrun - expected 1 byte")
        },
        // Success - Bad count
        {optLogger,
            new HA7SSerial.ReadResult(1, writeCTM, readCRCTM),
            new byte[]{'A'}, writeCTM,
            new HA7SSerial.BooleanResult().setFailure("Data error - not 0 or 1")
        }
    };
  }

  @Test(dataProvider = "cmdReadBitNegativeCasesProvider")
  public void cmdReadBitNegativeTests(Logger optLogger,
      HA7SSerial.ReadResult readResult, byte[] readRBufData, long readWriteCTM,
      HA7SSerial.BooleanResult expectedResult) {

    TestHA7SSerial serial = new TestHA7SSerial(readResult, readRBufData, readWriteCTM);
    HA7SSerial.BooleanResult result = serial.cmdReadBit(optLogger);
    Assert.assertEquals(result, expectedResult);
  }

  @DataProvider
  public Object[][] cmdWriteBlockNegativeCasesProvider() {
    HexByteArray hexByteArray = new HexByteArray(new byte[]{'F', 'F', 'F', 'F'});
    Logger optLogger = null;

    byte[] nullRBuf = null;
    long writeCTM = 5;
    long readCRCTM = 10;

    return new Object[][]{
        // Error - ReadOverrun
        {hexByteArray, optLogger,
            new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadOverrun), nullRBuf,
            writeCTM,
            new HA7SSerial.HexByteArrayResult().setFailure("RR_ReadOverrun")
        },
        // Error - ReadTimeout
        {hexByteArray, optLogger,
            new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadTimeout), nullRBuf,
            writeCTM,
            new HA7SSerial.HexByteArrayResult().setFailure("RR_ReadTimeout")
        },
        // Error - Error
        {hexByteArray, optLogger,
            new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Error), nullRBuf, writeCTM,
            new HA7SSerial.HexByteArrayResult().setFailure("RR_Error")
        },
        // Success - Read underrun
        {hexByteArray, optLogger,
            new HA7SSerial.ReadResult(3, writeCTM, readCRCTM),
            nullRBuf, writeCTM,
            new HA7SSerial.HexByteArrayResult().setFailure("Underrun - expected:4 got:3")
        },
        // Success - Bad hex chars
        {hexByteArray, optLogger,
            new HA7SSerial.ReadResult(4, writeCTM, readCRCTM),
            new byte[]{'f', 'f', 'f', 'f'}, writeCTM,
            new HA7SSerial.HexByteArrayResult().setFailure("Not hex bytes")
        }
    };
  }

  @Test(dataProvider = "cmdWriteBlockNegativeCasesProvider")
  public void cmdWriteBlockNegativeTests(HexByteArray hData, Logger optLogger,
      HA7SSerial.ReadResult readResult, byte[] readRBufData, long readWriteCTM,
      HA7SSerial.HexByteArrayResult expectedResult) {

    TestHA7SSerial serial = new TestHA7SSerial(readResult, readRBufData, readWriteCTM);
    HA7SSerial.HexByteArrayResult result = serial.cmdWriteBlock(hData, optLogger);
    Assert.assertEquals(result, expectedResult);
  }

  public static class TestHA7SSerial extends HA7SSerial {

    private StartResult startResult = null;

    private ReadResult readResult = null;
    private byte[] readRBufData = null;
    private Long readWriteCTM = null;

    private StopResult stopResult = null;

    public TestHA7SSerial(StartResult startResult) {
      this.startResult = startResult;
    }

    public TestHA7SSerial(ReadResult readResult, byte[] readRBufData, long readWriteCTM) {
      this.readResult = readResult;
      this.readRBufData = readRBufData;
      this.readWriteCTM = readWriteCTM;
    }

    public TestHA7SSerial(StopResult stopResult) {
      this.stopResult = stopResult;
    }

    @Override
    public String getPortName() {
      Assert.assertNotNull("no not call");
      return null;
    }

    @Override
    public StartResult start(Logger logger) {
      Assert.assertNotNull(startResult);
      return startResult;
    }

    @Override
    public ReadResult writeReadTilCR(byte[] wbuf, byte[] rbuf, Logger logger) {
      Assert.assertNotNull(readResult);
      Assert.assertNotNull(wbuf);
      Assert.assertTrue(wbuf.length > 0);
      Assert.assertNotNull(rbuf);

      if ((readResult.getError() == ReadResult.ErrorCode.RR_Success) && (readRBufData != null)) {
        for (int i = 0; i < readRBufData.length; i++) {
          rbuf[i] = readRBufData[i];
        }
      }
      return readResult;
    }

    @Override
    public StopResult stop(Logger logger) {
      Assert.assertNotNull(stopResult);
      return stopResult;
    }

  }
}
