package waterfall.onewire.busmasters.HA7S;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import waterfall.onewire.DSAddress;
import waterfall.onewire.HexByteArray;
import waterfall.onewire.busmaster.Logger;

import java.util.ArrayList;

/**
 * Created by dwaterfa on 8/12/17.
 */
public class HA7SSerialTest {

    /**
     * Returns the answer from a call to HA7SSerial.writeReadTilCR()
     *
     * @param serial
     * @return
     */
    public static Answer<HA7SSerial.ReadResult> makeWriteReadTilCRResult(HA7SSerial serial) {
        return new Answer<HA7SSerial.ReadResult>() {
            @Override
            public HA7SSerial.ReadResult answer(final InvocationOnMock invocation) {
                Assert.assertEquals(invocation.getArguments().length, 4);
                byte[] wbuf = (byte[]) (invocation.getArguments())[0];
                byte[] rbuf = (byte[]) (invocation.getArguments())[1];
                Long rTimeoutMSec = (Long) (invocation.getArguments())[2];
                Logger logger = (Logger) (invocation.getArguments())[3];

                return serial.writeReadTilCR(wbuf, rbuf, rTimeoutMSec, logger);
            }
        };
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

        public String getPortName() {
            Assert.assertNotNull("no not call");
            return null;
        }

        public StartResult start(Logger logger) {
            Assert.assertNotNull(startResult);
            return startResult;
        }

        public ReadResult writeReadTilCR(byte[] wbuf, byte[] rbuf, long rTimeoutMSec, Logger logger) {
            Assert.assertNotNull(readResult);
            Assert.assertNotNull(wbuf);
            Assert.assertTrue(wbuf.length > 0);
            Assert.assertNotNull(rbuf);

            if ((readResult.error == ReadResult.ErrorCode.RR_Success) && (readRBufData != null)) {
                for (int i = 0; i < readRBufData.length; i++) {
                    rbuf[i] = readRBufData[i];
                }
            }
            return readResult;
        }

        public StopResult stop(Logger logger) {
            Assert.assertNotNull(stopResult);
            return stopResult;
        }

    }

    @DataProvider
    public Object[][] cmdAddressNegativeCasesProvider() {
        DSAddress dsAddress = new DSAddress("EE0000065BC0AE28");
        long rTimeoutMSec = 5;
        Logger optLogger = null;

        byte[] nullRBuf = null;
        long writeCTM = 5;

        byte[] mismatchDSAddress = dsAddress.toString().getBytes();
        mismatchDSAddress[1] = '0';

        return new Object[][]{
                // Error - ReadOverrun
                {dsAddress, rTimeoutMSec, optLogger,
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadOverrun), nullRBuf, writeCTM,
                        new HA7SSerial.NoDataResult().setFailure("RR_ReadOverrun")
                },
                // Error - ReadTimeout
                {dsAddress, rTimeoutMSec, optLogger,
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadTimeout), nullRBuf, writeCTM,
                        new HA7SSerial.NoDataResult().setFailure("RR_ReadTimeout")
                },
                // Error - Error
                {dsAddress, rTimeoutMSec, optLogger,
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Error), nullRBuf, writeCTM,
                        new HA7SSerial.NoDataResult().setFailure("RR_Error")
                },
                // Success - Read underrun
                {dsAddress, rTimeoutMSec, optLogger,
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Success, 6, writeCTM), nullRBuf, writeCTM,
                        new HA7SSerial.NoDataResult().setFailure("Underrun - expected 16 got:6")
                },
                // Success - Bad hex chars
                {dsAddress, rTimeoutMSec, optLogger,
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Success, 16, writeCTM),
                        mismatchDSAddress, writeCTM,
                        new HA7SSerial.NoDataResult().setFailure("Invalid char index:1")
                }
        };
    }

    @Test(dataProvider = "cmdAddressNegativeCasesProvider")
    public void cmdAddressNegativeTests(DSAddress dsAddress, long rTimeoutMSec, Logger optLogger,
                                        HA7SSerial.ReadResult readResult, byte[] readRBufData, long readWriteCTM,
                                        HA7SSerial.NoDataResult expectedResult) {

        TestHA7SSerial serial = new TestHA7SSerial(readResult, readRBufData, readWriteCTM);
        HA7SSerial.NoDataResult result = serial.cmdAddress(dsAddress, rTimeoutMSec, optLogger);
        Assert.assertEquals(result, expectedResult);
    }

    @DataProvider
    public Object[][] cmdResetNegativeCasesProvider() {
        long rTimeoutMSec = 5;
        Logger optLogger = null;

        byte[] nullRBuf = null;
        long writeCTM = 5;

        return new Object[][]{
                // Error - ReadOverrun
                {rTimeoutMSec, optLogger,
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadOverrun), nullRBuf, writeCTM,
                        new HA7SSerial.NoDataResult().setFailure("RR_ReadOverrun")
                },
                // Error - ReadTimeout
                {rTimeoutMSec, optLogger,
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadTimeout), nullRBuf, writeCTM,
                        new HA7SSerial.NoDataResult().setFailure("RR_ReadTimeout")
                },
                // Error - Error
                {rTimeoutMSec, optLogger,
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Error), nullRBuf, writeCTM,
                        new HA7SSerial.NoDataResult().setFailure("RR_Error")
                }
        };
    }

    @Test(dataProvider = "cmdResetNegativeCasesProvider")
    public void cmdResetNegativeTests(long rTimeoutMSec, Logger optLogger,
                                      HA7SSerial.ReadResult readResult, byte[] readRBufData, long readWriteCTM,
                                      HA7SSerial.NoDataResult expectedResult) {

        TestHA7SSerial serial = new TestHA7SSerial(readResult, readRBufData, readWriteCTM);
        HA7SSerial.NoDataResult result = serial.cmdReset(rTimeoutMSec, optLogger);
        Assert.assertEquals(result, expectedResult);
    }

    @DataProvider
    public Object[][] cmdSearchNegativeCasesProvider() {
        long rTimeoutMSec = 5;
        Logger optLogger = null;

        byte[] nullRBuf = null;
        long writeCTM = 5;

        byte[] mismatchDSAddress = ("EG0000065BC0AE28").getBytes();

        return new Object[][]{
                // Error - ReadOverrun
                {rTimeoutMSec, optLogger,
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadOverrun), nullRBuf, writeCTM,
                        new HA7SSerial.HexByteArrayResult().setFailure("RR_ReadOverrun")
                },
                // Error - ReadTimeout
                {rTimeoutMSec, optLogger,
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadTimeout), nullRBuf, writeCTM,
                        new HA7SSerial.HexByteArrayResult().setFailure("RR_ReadTimeout")
                },
                // Error - Error
                {rTimeoutMSec, optLogger,
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Error), nullRBuf, writeCTM,
                        new HA7SSerial.HexByteArrayResult().setFailure("RR_Error")
                },
                // Success - Read underrun
                {rTimeoutMSec, optLogger,
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Success, 6, writeCTM), nullRBuf, writeCTM,
                        new HA7SSerial.HexByteArrayResult().setFailure("Underrun - expected 0 or 16 got:6")
                },
                // Success - Bad hex chars
                {rTimeoutMSec, optLogger,
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Success, 16, writeCTM),
                        mismatchDSAddress, writeCTM,
                        new HA7SSerial.HexByteArrayResult().setFailure("Not hex bytes")
                }
        };
    }

    @Test(dataProvider = "cmdSearchNegativeCasesProvider")
    public void cmdSearchNegativeTests(long rTimeoutMSec, Logger optLogger,
                                       HA7SSerial.ReadResult readResult, byte[] readRBufData, long readWriteCTM,
                                       HA7SSerial.HexByteArrayResult expectedResult) {

        TestHA7SSerial serial = new TestHA7SSerial(readResult, readRBufData, readWriteCTM);
        HA7SSerial.HexByteArrayResult result = serial.cmdSearch(rTimeoutMSec, optLogger);
        Assert.assertEquals(result, expectedResult);
    }

    @Test(dataProvider = "cmdSearchNegativeCasesProvider")
    public void cmdSearchNextNegativeTests(long rTimeoutMSec, Logger optLogger,
                                           HA7SSerial.ReadResult readResult, byte[] readRBufData, long readWriteCTM,
                                           HA7SSerial.HexByteArrayResult expectedResult) {

        TestHA7SSerial serial = new TestHA7SSerial(readResult, readRBufData, readWriteCTM);
        HA7SSerial.HexByteArrayResult result = serial.cmdSearchNext(rTimeoutMSec, optLogger);
        Assert.assertEquals(result, expectedResult);
    }

    @Test(dataProvider = "cmdSearchNegativeCasesProvider")
    public void cmdConditionalSearchNegativeTests(long rTimeoutMSec, Logger optLogger,
                                                  HA7SSerial.ReadResult readResult, byte[] readRBufData, long readWriteCTM,
                                                  HA7SSerial.HexByteArrayResult expectedResult) {

        TestHA7SSerial serial = new TestHA7SSerial(readResult, readRBufData, readWriteCTM);
        HA7SSerial.HexByteArrayResult result = serial.cmdConditionalSearch(rTimeoutMSec, optLogger);
        Assert.assertEquals(result, expectedResult);
    }

    @Test(dataProvider = "cmdSearchNegativeCasesProvider")
    public void cmdConditionalSearchNextNegativeTests(long rTimeoutMSec, Logger optLogger,
                                                      HA7SSerial.ReadResult readResult, byte[] readRBufData, long readWriteCTM,
                                                      HA7SSerial.HexByteArrayResult expectedResult) {

        TestHA7SSerial serial = new TestHA7SSerial(readResult, readRBufData, readWriteCTM);
        HA7SSerial.HexByteArrayResult result = serial.cmdConditionalSearchNext(rTimeoutMSec, optLogger);
        Assert.assertEquals(result, expectedResult);
    }

    @DataProvider
    public Object[][] cmdFamilySearchNegativeCasesProvider() {

        long rTimeoutMSec = 5;
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
        long rTimeoutMSec = 5;
        Logger optLogger = null;

        Short badFamilyCode_minus1 = -1;
        Short badFamilyCode_256 = 256;

        return new Object[][] {
                { badFamilyCode_minus1, rTimeoutMSec, optLogger },
                { badFamilyCode_256, rTimeoutMSec, optLogger }
        };
    }

    @Test(dataProvider = "cmdFamilySearchNegativeCasesProvider")
    public void cmdFamilySearchNegativeTests(short familyCode, long rTimeoutMSec, Logger optLogger,
                                                 HA7SSerial.ReadResult readResult, byte[] readRBufData, long readWriteCTM,
                                                 HA7SSerial.HexByteArrayResult expectedResult) {

        TestHA7SSerial serial = new TestHA7SSerial(readResult, readRBufData, readWriteCTM);
        HA7SSerial.HexByteArrayResult result = serial.cmdFamilySearch(familyCode, rTimeoutMSec, optLogger);
        Assert.assertEquals(result, expectedResult);
    }

    @Test(dataProvider = "cmdFamilySearchNegativeArgCasesProvider", expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Bad familyCode")
    public void cmdFamilySearchNegativeArgTests(short familyCode, long rTimeoutMSec, Logger optLogger) {

        TestHA7SSerial serial = new TestHA7SSerial(null, null, 0);
        serial.cmdFamilySearch(familyCode, rTimeoutMSec, optLogger);
    }

    @Test(dataProvider = "cmdSearchNegativeCasesProvider")
    public void cmdFamilySearchNextNegativeTests(long rTimeoutMSec, Logger optLogger,
                                                 HA7SSerial.ReadResult readResult, byte[] readRBufData, long readWriteCTM,
                                                 HA7SSerial.HexByteArrayResult expectedResult) {

        TestHA7SSerial serial = new TestHA7SSerial(readResult, readRBufData, readWriteCTM);
        HA7SSerial.HexByteArrayResult result = serial.cmdFamilySearchNext(rTimeoutMSec, optLogger);
        Assert.assertEquals(result, expectedResult);
    }

    @DataProvider
    public Object[][] cmdReadBitNegativeCasesProvider() {
        long rTimeoutMSec = 5;
        Logger optLogger = null;

        byte[] nullRBuf = null;
        long writeCTM = 5;

        return new Object[][]{
                // Error - ReadOverrun
                {rTimeoutMSec, optLogger,
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadOverrun),
                        nullRBuf, writeCTM,
                        new HA7SSerial.BooleanResult().setFailure("RR_ReadOverrun")
                },
                // Error - ReadTimeout
                {rTimeoutMSec, optLogger,
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadTimeout),
                        nullRBuf, writeCTM,
                        new HA7SSerial.BooleanResult().setFailure("RR_ReadTimeout")
                },
                // Error - Error
                {rTimeoutMSec, optLogger,
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Error),
                        nullRBuf, writeCTM,
                        new HA7SSerial.BooleanResult().setFailure("RR_Error")
                },
                // Success - Bad count
                {rTimeoutMSec, optLogger,
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Success, 0, writeCTM),
                        new byte[] {0}, writeCTM,
                        new HA7SSerial.BooleanResult().setFailure("Underrun - expected 1 byte")
                },
                // Success - Bad count
                {rTimeoutMSec, optLogger,
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Success, 1, writeCTM),
                        new byte[] {'A'}, writeCTM,
                        new HA7SSerial.BooleanResult().setFailure("Data error - not 0 or 1")
                }
        };
    }

    @Test(dataProvider = "cmdReadBitNegativeCasesProvider")
    public void cmdReadBitNegativeTests(long rTimeoutMSec, Logger optLogger,
                                      HA7SSerial.ReadResult readResult, byte[] readRBufData, long readWriteCTM,
                                      HA7SSerial.BooleanResult expectedResult) {

        TestHA7SSerial serial = new TestHA7SSerial(readResult, readRBufData, readWriteCTM);
        HA7SSerial.BooleanResult result = serial.cmdReadBit(rTimeoutMSec, optLogger);
        Assert.assertEquals(result, expectedResult);
    }

    @DataProvider
    public Object[][] cmdWriteBlockNegativeCasesProvider() {
        HexByteArray hexByteArray = new HexByteArray(new byte[] { 'F', 'F', 'F', 'F' });
        long rTimeoutMSec = 5;
        Logger optLogger = null;

        byte[] nullRBuf = null;
        long writeCTM = 5;

        return new Object[][]{
                // Error - ReadOverrun
                {hexByteArray, rTimeoutMSec, optLogger,
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadOverrun), nullRBuf, writeCTM,
                        new HA7SSerial.HexByteArrayResult().setFailure("RR_ReadOverrun")
                },
                // Error - ReadTimeout
                {hexByteArray, rTimeoutMSec, optLogger,
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadTimeout), nullRBuf, writeCTM,
                        new HA7SSerial.HexByteArrayResult().setFailure("RR_ReadTimeout")
                },
                // Error - Error
                {hexByteArray, rTimeoutMSec, optLogger,
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Error), nullRBuf, writeCTM,
                        new HA7SSerial.HexByteArrayResult().setFailure("RR_Error")
                },
                // Success - Read underrun
                {hexByteArray, rTimeoutMSec, optLogger,
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Success, 3, writeCTM), nullRBuf, writeCTM,
                        new HA7SSerial.HexByteArrayResult().setFailure("Underrun - expected:4 got:3")
                },
                // Success - Bad hex chars
                {hexByteArray, rTimeoutMSec, optLogger,
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Success, 4, writeCTM),
                        new byte[] { 'f', 'f', 'f', 'f' }, writeCTM,
                        new HA7SSerial.HexByteArrayResult().setFailure("Not hex bytes")
                }
        };
    }

    @Test(dataProvider = "cmdWriteBlockNegativeCasesProvider")
    public void cmdWriteBlockNegativeTests(HexByteArray hData, long rTimeoutMSec, Logger optLogger,
                                           HA7SSerial.ReadResult readResult, byte[] readRBufData, long readWriteCTM,
                                           HA7SSerial.HexByteArrayResult expectedResult) {

        TestHA7SSerial serial = new TestHA7SSerial(readResult, readRBufData, readWriteCTM);
        HA7SSerial.HexByteArrayResult result = serial.cmdWriteBlock(hData, rTimeoutMSec, optLogger);
        Assert.assertEquals(result, expectedResult);
    }
}
