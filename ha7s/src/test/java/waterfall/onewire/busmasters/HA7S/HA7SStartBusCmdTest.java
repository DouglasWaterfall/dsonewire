package waterfall.onewire.busmasters.HA7S;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.StartBusCmd;

import static org.mockito.Mockito.*;


/**
 * Created by dwaterfa on 8/6/17.
 */
public class HA7SStartBusCmdTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testConstructorNullSerial() {
        new HA7S(null);
    }

    @Test
    public void testConstructorDefaults() {
        String portName = "foo";

        HA7SSerial mockSerial = mock(HA7SSerial.class);
        when(mockSerial.getPortName()).thenReturn(portName);

        HA7S ha7s = new HA7S(mockSerial);
        Assert.assertFalse(ha7s.getIsStarted());
        Assert.assertTrue(ha7s.getCurrentTimeMillis() > 0);
        Assert.assertEquals(ha7s.getName(), "HA7S on " + portName);
    }

    @Test(dataProvider = "createSerialPortStartNegativeCases")
    public void testSerialPortStartNegative(HA7SSerial.StartResult serialStartResult,
                                            StartBusCmd.Result expectedResult) {

        HA7SSerial mockSerial = mock(HA7SSerial.class);
        when(mockSerial.start(any(Logger.class))).thenReturn(serialStartResult);

        HA7S ha7s = new HA7S(mockSerial);
        Assert.assertFalse(ha7s.getIsStarted());

        StartBusCmd startCmd = ha7s.queryStartBusCmd();
        Assert.assertEquals(startCmd.execute(), expectedResult);
    }

    @DataProvider
    public Object[][] createSerialPortStartNegativeCases() {
        return new Object[][]{
                {HA7SSerial.StartResult.SR_NoPortName, StartBusCmd.Result.communication_error},
                {HA7SSerial.StartResult.SR_Busy, StartBusCmd.Result.communication_error},
                {HA7SSerial.StartResult.SR_Error, StartBusCmd.Result.communication_error}
        };
    }

    @Test(dataProvider = "createSerialPortReadTilCRNegativeCases")
    public void testSerialPortWriteReadTilCRNegative(HA7SSerial.ReadResult first_serialReadResult, byte[] first_rbuf_data,
                                                     HA7SSerial.ReadResult second_serialReadResult, byte[] second_rbuf_data,
                                                     StartBusCmd.Result expectedResult) {

        HA7SSerial mockSerial = mock(HA7SSerial.class);
        when(mockSerial.start(any(Logger.class))).thenReturn(HA7SSerial.StartResult.SR_Success);

        Answer<HA7SSerial.ReadResult> firstAnswer = makeAnswerForReadResult(first_serialReadResult, first_rbuf_data);

        if (second_serialReadResult != null) {
            Answer<HA7SSerial.ReadResult> secondAnswer = makeAnswerForReadResult(second_serialReadResult, second_rbuf_data);
            when(mockSerial.writeReadTilCR(any(byte[].class), any(byte[].class), any(Long.TYPE), any(Logger.class)))
                    .thenAnswer(firstAnswer)
                    .thenAnswer(secondAnswer);
        } else {
            when(mockSerial.writeReadTilCR(any(byte[].class), any(byte[].class), any(Long.TYPE), any(Logger.class)))
                    .thenAnswer(firstAnswer);
        }

        when(mockSerial.stop(any(Logger.class))).thenReturn(HA7SSerial.StopResult.SR_Success);

        HA7S ha7s = new HA7S(mockSerial);
        Assert.assertFalse(ha7s.getIsStarted());

        StartBusCmd startCmd = ha7s.queryStartBusCmd();
        Assert.assertEquals(startCmd.execute(), expectedResult);
    }

    private Answer<HA7SSerial.ReadResult> makeAnswerForReadResult(HA7SSerial.ReadResult serialReadResult, byte[] rbuf_data) {
        return new Answer<HA7SSerial.ReadResult>() {
            @Override
            public HA7SSerial.ReadResult answer(final InvocationOnMock invocation) {
                byte[] wbuf = (byte[]) (invocation.getArguments())[0];
                byte[] rbuf = (byte[]) (invocation.getArguments())[1];
                long timeout = (long) (invocation.getArguments())[2];
                Logger logger = (Logger) (invocation.getArguments())[3];

                if (rbuf_data != null) {
                    for (int i = 0; i < rbuf_data.length; i++) {
                        rbuf[i] = rbuf_data[i];
                    }
                }

                return serialReadResult;
            }
        };
    }

    @DataProvider
    public Object[][] createSerialPortReadTilCRNegativeCases() {

        return new Object[][]{
                // generic read overrun
                {new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadOverrun), null,
                        null, null,
                        StartBusCmd.Result.communication_error},
                // generic read timeout
                {new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_ReadTimeout), null,
                        null, null,
                        StartBusCmd.Result.communication_error},
                // generic error
                {new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Error), null,
                        null, null,
                        StartBusCmd.Result.communication_error},
                // reset failure followed by error
                {new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Success, 1, 5), new byte[]{0x7},
                        new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Error), null,
                        StartBusCmd.Result.communication_error},
        };
    }

    @Test
    public void testSerialPortStartPositive() {

        HA7SSerial mockSerial = mock(HA7SSerial.class);
        when(mockSerial.start(any(Logger.class))).thenReturn(HA7SSerial.StartResult.SR_Success);

        HA7SSerial.ReadResult readResult = new HA7SSerial.ReadResult(HA7SSerial.ReadResult.ErrorCode.RR_Success, 0, 1);

        when(mockSerial.writeReadTilCR(any(byte[].class), any(byte[].class), any(Long.TYPE), any(Logger.class)))
                .thenReturn(readResult);

        HA7S ha7s = new HA7S(mockSerial);
        Assert.assertFalse(ha7s.getIsStarted());

        StartBusCmd startCmd = ha7s.queryStartBusCmd();
        Assert.assertEquals(startCmd.execute(), StartBusCmd.Result.started);
        Assert.assertEquals(startCmd.execute(), StartBusCmd.Result.already_started);
    }

}
