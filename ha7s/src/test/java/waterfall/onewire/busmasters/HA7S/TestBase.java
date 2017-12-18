package waterfall.onewire.busmasters.HA7S;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.BusMaster;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmaster.NotifySearchBusCmdResult;
import waterfall.onewire.busmaster.SearchBusCmd;
import waterfall.onewire.busmasters.HA7S.HA7S.cmdReturn;
import waterfall.onewire.busmasters.HA7S.HA7SSerial.ReadResult;
import waterfall.onewire.busmasters.HA7S.part.DS18B20;

/**
 * Created by dwaterfa on 12/17/17.
 *
 * Here is helpful site with which to fabricate addresses: http://www.datastat.com/sysadminjournal/maximcrc.cgi
 *
 * The trick is when calculating the crc8 you need to do it in reverse order, that is from right to
 * left of the address as we maintain it. If you do the 14 bytes what will be left will be a two
 * byte CRC value. This is what can then be the FIRST two nibbles of the full address.
 *
 * "EE0000065BC0AE28", "090000065BD53528", "5F0000065CCD1A28", "260000065BE22D28",
 * "7C0000063BB13028", "5A0000063B7AF528", "AA0000063BF51928", "390000063B759F28",
 * "7F0000063BA12F28"
 */
public class TestBase {
  
  public static final long periodNegativeOneMSec = -1;
  public static final long periodZeroMSec = 0;
  public static final long periodOneMSec = 1;
  public static final long period250MSec = 250;
  public static final long period500MSec = 500;
  public static final long period750MSec = 750;


  protected HA7SSerial getReadyToStartMockSerial() {
    HA7SSerial mockSerial = mock(HA7SSerial.class);

    when(mockSerial.start(any(Logger.class))).thenReturn(HA7SSerial.StartResult.SR_Success);

    // We are actually writing a Reset Bus 'R' cmd here
    when(mockSerial.writeReadTilCR(any(byte[].class), any(byte[].class), any(Logger.class)))
        .thenAnswer(makeAnswerForReadZero(1L, 2L));

    return mockSerial;
  }

  public static Answer<cmdReturn> makeWriteBlockCmdReturnAnswer(HA7S.cmdResult result,
      byte[] rbuf_data, long writeCTM) {
    return new Answer<HA7S.cmdReturn>() {
      @Override
      public HA7S.cmdReturn answer(final InvocationOnMock invocation) {
        byte[] wbuf = (byte[]) (invocation.getArguments())[0];
        byte[] rbuf = (byte[]) (invocation.getArguments())[1];
        Logger logger = (Logger) (invocation.getArguments())[2];

        if (result == HA7S.cmdResult.Success) {
          int read_count = 0;
          if (rbuf_data != null) {
            read_count = rbuf_data.length;
            for (int i = 0; i < read_count; i++) {
              rbuf[i] = rbuf_data[i];
            }
          }
          return new HA7S.cmdReturn(read_count, writeCTM);
        }
        return new HA7S.cmdReturn(result);
      }
    };
  }

  public static Answer<HA7S.cmdReturn> makeSearchCmdReturnAnswer(HA7S.cmdResult result,
      ArrayList<byte[]> hexByteArrayList_data, long writeCTM) {
    return new Answer<HA7S.cmdReturn>() {
      @Override
      public HA7S.cmdReturn answer(final InvocationOnMock invocation) {
        Byte familyCode = null;
        ArrayList<byte[]> hexByteArrayList = null;
        Logger logger = null;
        if (invocation.getArguments().length == 2) {
          hexByteArrayList = (ArrayList<byte[]>) (invocation.getArguments())[0];
          logger = (Logger) (invocation.getArguments())[1];
        } else {
          familyCode = (Byte) (invocation.getArguments())[0];
          hexByteArrayList = (ArrayList<byte[]>) (invocation.getArguments())[1];
          logger = (Logger) (invocation.getArguments())[2];
        }

        if (result == HA7S.cmdResult.Success) {
          int read_count = 0;
          if (hexByteArrayList_data != null) {
            read_count = hexByteArrayList_data.size();
            for (int i = 0; i < read_count; i++) {
              hexByteArrayList.add(hexByteArrayList_data.get(i));
            }
          }
          return new HA7S.cmdReturn(read_count, writeCTM);
        }
        return new HA7S.cmdReturn(result);
      }
    };
  }

  protected Answer<HA7SSerial.ReadResult> makeAnswerForReadResult(
      HA7SSerial.ReadResult serialReadResult, byte[] rbuf_data) {
    return new Answer<HA7SSerial.ReadResult>() {
      @Override
      public HA7SSerial.ReadResult answer(final InvocationOnMock invocation) {
        byte[] wbuf = (byte[]) (invocation.getArguments())[0];
        byte[] rbuf = (byte[]) (invocation.getArguments())[1];
        Logger logger = (Logger) (invocation.getArguments())[2];

        if (rbuf_data != null) {
          for (int i = 0; i < rbuf_data.length; i++) {
            rbuf[i] = rbuf_data[i];
          }
        }

        return serialReadResult;
      }
    };
  }

  protected Answer<HA7SSerial.ReadResult> makeAnswerForReadZero(long writeCTM, long readCRCTM) {
    return makeAnswerForReadResult(new HA7SSerial.ReadResult(0, writeCTM, readCRCTM), null);
  }

  protected Answer<HA7SSerial.ReadResult> makeAnswerForAddress(long writeCTM, long readCRCTM) {
    return new Answer<HA7SSerial.ReadResult>() {
      @Override
      public HA7SSerial.ReadResult answer(final InvocationOnMock invocation) {
        byte[] wbuf = (byte[]) (invocation.getArguments())[0];
        byte[] rbuf = (byte[]) (invocation.getArguments())[1];
        Logger logger = (Logger) (invocation.getArguments())[2];

        Assert.assertNotNull(wbuf);
        Assert.assertEquals(wbuf.length, 18);
        Assert.assertEquals(wbuf[0], 'A');
        Assert.assertEquals(wbuf.length, (1 + 16 + 1));
        Assert.assertNotNull(rbuf);
        Assert.assertTrue(rbuf.length >= 16);

        for (int i = 0; i < 16; i++) {
          rbuf[i] = wbuf[1 + i];
        }

        return new ReadResult(16, writeCTM, readCRCTM);
      }
    };
  }

  protected Answer<HA7SSerial.ReadResult> makeAnswerForSearch(byte[] cmdHex, byte[] rbuf_data,
      long writeCTM, long readCRCTM) {
    return new Answer<HA7SSerial.ReadResult>() {
      @Override
      public HA7SSerial.ReadResult answer(final InvocationOnMock invocation) {
        byte[] wbuf = (byte[]) (invocation.getArguments())[0];
        byte[] rbuf = (byte[]) (invocation.getArguments())[1];
        Logger logger = (Logger) (invocation.getArguments())[2];

        Assert.assertNotNull(wbuf);
        Assert.assertTrue(wbuf.length >= 1);
        for (int i = 0; i < cmdHex.length; i++) {
          Assert.assertEquals(wbuf[i], cmdHex[i]);
        }
        Assert.assertNotNull(rbuf);
        Assert.assertTrue(rbuf.length >= 16);
        Assert
            .assertTrue((rbuf_data == null) || (rbuf_data.length == 0) || (rbuf_data.length == 16));

        int readCount = 0;
        if (rbuf_data != null) {
          for (int i = 0; i < rbuf_data.length; i++) {
            rbuf[i] = rbuf_data[i];
          }
          readCount = rbuf_data.length;
        }

        return new ReadResult(readCount, writeCTM, readCRCTM);
      }
    };
  }

  public class myNotifySearchBusCmdResult implements NotifySearchBusCmdResult {

    protected int notifyCount;
    protected Data data;
    protected HA7SSerialDummy serialDummy;
    protected DSAddress updateOnNotifyDev;
    protected boolean updateOnNotifyAlarm;
    protected boolean updateOnNotifyDoAdd;

    public myNotifySearchBusCmdResult() {
      notifyCount = -1;
      data = null;
      serialDummy = null;
      updateOnNotifyDev = null;
      updateOnNotifyAlarm = false;
      updateOnNotifyDoAdd = false;
    }

    public synchronized void notify(BusMaster bm, boolean byAlarm,
        SearchBusCmd.ResultData searchResultData) {
      data = new Data(++notifyCount, bm, byAlarm, searchResultData);

      if (serialDummy != null) {
        if (updateOnNotifyDoAdd) {
          serialDummy.addDevice(new DS18B20(updateOnNotifyDev).setHasAlarm(updateOnNotifyAlarm));
        } else {
          serialDummy.removeDevice(updateOnNotifyDev);
        }
        updateOnNotifyDoAdd = !updateOnNotifyDoAdd;
      }
    }

    public synchronized Data getData() {
      return data;
    }

    public boolean wait500MSecForNotifyChange(int afterNotifyCount) {
      return internalWaitForNotifyChange(5, 100, afterNotifyCount);
    }

    public boolean wait1000MSecForNotifyChange(int afterNotifyCount) {
      return internalWaitForNotifyChange(5, 200, afterNotifyCount);
    }

    protected boolean internalWaitForNotifyChange(int count, long timeMSec, int afterNotifyCount) {
      for (int i = 0; i < count; i++) {
        Data t_data = getData();
        if ((t_data != null) && (t_data.notifyCount != afterNotifyCount)) {
          return true;
        }
        try {
          Thread.sleep(timeMSec);
        } catch (InterruptedException e) {

        }
      }
      return false;
    }

    public synchronized void setAddRemoveEveryNotify(HA7SSerialDummy serialDummy, DSAddress dev,
        boolean activeAlarm) {
      if (this.serialDummy != null) {
        throw new IllegalArgumentException("already registered");
      }

      this.serialDummy = serialDummy;
      updateOnNotifyDev = dev;
      updateOnNotifyAlarm = activeAlarm;
      updateOnNotifyDoAdd = true;
    }

    public synchronized void clearAddRemoveEveryNotify() {
      if (serialDummy != null) {
        if (!updateOnNotifyDoAdd) {
          serialDummy.removeDevice(updateOnNotifyDev);
        }
        serialDummy = null;
        updateOnNotifyDev = null;
      }
    }

    public class Data {

      public int notifyCount;
      public BusMaster bm;
      public Boolean byAlarm;
      public SearchBusCmd.ResultData searchResultData;

      public Data(int notifyCount, BusMaster bm, boolean byAlarm,
          SearchBusCmd.ResultData searchResultData) {
        this.notifyCount = notifyCount;
        this.bm = bm;
        this.byAlarm = byAlarm;
        this.searchResultData = searchResultData;
      }
    }
  }
  
}
