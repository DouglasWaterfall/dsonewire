package waterfall.onewire.busmasters.HA7S;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import waterfall.onewire.busmaster.Logger;

/**
 * Created by dwaterfa on 8/12/17.
 */
public class HA7SSerialTest {

    /**
     * Returns the answer from a call to HA7SSerial.writeReadTilCR()
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

    public static class TestHA7SSerial implements HA7SSerial {
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
            Assert.assertTrue(rbuf.length > 0);

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

}
