/**
 * Created by dwaterfa on 10/30/17.
 */

package waterfall.onewire;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyShort;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import waterfall.onewire.Temp18B20.Reading;
import waterfall.onewire.Temp18B20.ReadingData;
import waterfall.onewire.Temp18B20.ReadingError;
import waterfall.onewire.busmaster.BusMaster;
import waterfall.onewire.busmaster.ConvertTCmd;
import waterfall.onewire.busmaster.ReadScratchpadCmd;
import waterfall.onewire.busmasters.HA7S.HA7S;
import waterfall.onewire.busmasters.HA7S.HA7SSerial;
import waterfall.onewire.busmasters.HA7S.HA7SSerialDummy;
import waterfall.onewire.busmasters.HA7S.part.DS18B20;
import waterfall.onewire.device.DS18B20Scratchpad;


public class Temp18B20Test {

  private static final byte[] deviceNotFoundRaw;
  private static final DS18B20Scratchpad badCRCData;

  static {
    deviceNotFoundRaw = new byte[9];
    Arrays.fill(deviceNotFoundRaw, (byte) 0xff);

    badCRCData = new DS18B20Scratchpad();
    badCRCData.getRawBytes()[0] = (byte) ~badCRCData.getRawBytes()[0];
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "dsAddress")
  public void testConstructorNullAddress() {
    new Temp18B20(null, DS18B20Scratchpad.DEFAULT_RESOLUTION, Byte.MIN_VALUE, Byte.MAX_VALUE);
  }

  @DataProvider
  public Object[][] constructorResolutionNegativeCases() {
    return new Object[][]{
        {(byte) -1},
        {(byte) 4}
    };
  }

  @Test(dataProvider = "constructorResolutionNegativeCases",
      expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = "resolution")
  public void testConstructorResolutionNegativeCases(byte resolution) {
    new Temp18B20(DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28), resolution, Byte.MIN_VALUE, Byte.MAX_VALUE);
  }

  @DataProvider
  public Object[][] constructorPositiveCases() {
    DSAddress valid = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);

    return new Object[][]{
        {valid, (byte) 0, Byte.MAX_VALUE, Byte.MIN_VALUE},
        {valid, (byte) 1, Byte.MAX_VALUE, Byte.MIN_VALUE},
        {valid, (byte) 2, Byte.MAX_VALUE, Byte.MIN_VALUE},
        {valid, (byte) 3, Byte.MAX_VALUE, Byte.MIN_VALUE},
        {valid, (byte) 0, (byte) 0, Byte.MIN_VALUE},
        {valid, (byte) 0, Byte.MIN_VALUE, Byte.MIN_VALUE},
        {valid, (byte) 0, Byte.MAX_VALUE, (byte) 0},
        {valid, (byte) 0, Byte.MIN_VALUE, Byte.MAX_VALUE}
    };
  }

  @Test(dataProvider = "constructorPositiveCases")
  public void testConstructorValues(DSAddress dsAddress, byte resolution, byte hAlarm,
      byte lAlarm) {
    Temp18B20 t = new Temp18B20(dsAddress, resolution, hAlarm, lAlarm);

    Assert.assertEquals(t.getDSAddress(), dsAddress);
    Assert.assertEquals(t.getResolution(), resolution);
    Assert.assertEquals(t.getTempHAlarm(), hAlarm);
    Assert.assertEquals(t.getTempLAlarm(), lAlarm);
    Assert.assertNull(t.getBusMaster());
  }

  @Test
  public void testGetTemperatureNoBM() {
    DSAddress dsAddress = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);

    Temp18B20 t = new Temp18B20(dsAddress, (byte)3, Byte.MIN_VALUE, Byte.MAX_VALUE);

    Temp18B20.Reading r = t.getTemperature(100L);
    Assert.assertTrue(r instanceof Temp18B20.ReadingError);
    Assert.assertEquals(((Temp18B20.ReadingError) r).getError(), Temp18B20.ERR_NO_BUSMASTER);
  }

  @Test
  public void testSetBusMaster() {
    DSAddress dsAddress = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);

    Temp18B20 t = new Temp18B20(dsAddress, (byte)2, Byte.MIN_VALUE, Byte.MAX_VALUE);
    Assert.assertNull(t.getBusMaster());

    BusMaster mockBM = getMockBMFor(dsAddress, null, 0, null, 0, null);

    t.setBusMaster(mockBM);
    Assert.assertEquals(t.getBusMaster(), mockBM);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "dup bm")
  public void testSetDuplicateBusMaster() {
    DSAddress dsAddress = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);

    Temp18B20 t = new Temp18B20(dsAddress, (byte)2, Byte.MIN_VALUE, Byte.MAX_VALUE);
    Assert.assertNull(t.getBusMaster());

    BusMaster mockBM = getMockBMFor(dsAddress, null, 0, null, 0, null);

    t.setBusMaster(mockBM);
    t.setBusMaster(mockBM);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "dup bMR")
  public void testSetBusMasterRegistry() {
    DSAddress dsAddress = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);

    Temp18B20 t = new Temp18B20(dsAddress, (byte)2, Byte.MIN_VALUE, Byte.MAX_VALUE);
    Assert.assertNull(t.getBusMaster());

    BusMasterRegistry bmR = new BusMasterRegistry();
    t.setBusMasterRegistry(bmR);
    t.setBusMasterRegistry(bmR);
  }

  /*
  * Tests
  * Get temperature, device needs to be initialized
  * Get temperature, device was powercycled re-initialized
  *
  * Bonus:
  * Get temperature, Device not found, look for new BusMaster after certain number of retries?
  */

  @DataProvider
  public Object[][] getTemperatureCases() {
    DSAddress dsAddress = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);
    BusMaster nullBM = null;

    return new Object[][]{
        // busmaster not assigned
        {dsAddress, nullBM,
            DS18B20Scratchpad.DEFAULT_RESOLUTION,
            DS18B20Scratchpad.DEFAULT_HALARM,
            DS18B20Scratchpad.DEFAULT_LALARM,
            new ReadingError(Temp18B20.ERR_NO_BUSMASTER)},

        // device not found
        {dsAddress, getStartedHA7S(new HA7SSerialDummy("port")
            .addDevice(new DS18B20(dsAddress).setScratchPadData(new byte[][]{
            deviceNotFoundRaw
            }))),
            DS18B20Scratchpad.DEFAULT_RESOLUTION,
            DS18B20Scratchpad.DEFAULT_HALARM,
            DS18B20Scratchpad.DEFAULT_LALARM,
            new ReadingError(Temp18B20.ERR_DEVICE_NOT_FOUND)},

        // bad data when reading the device scratchpad
        {dsAddress, getStartedHA7S(new HA7SSerialDummy("port")
            .addDevice(new DS18B20(dsAddress).setScratchPadData(new byte[][]{
                badCRCData.getRawBytes()
                }))),
            DS18B20Scratchpad.DEFAULT_RESOLUTION,
            DS18B20Scratchpad.DEFAULT_HALARM,
            DS18B20Scratchpad.DEFAULT_LALARM,

            new ReadingError(Temp18B20.ERR_SCRATCHPAD_DATA_NOT_VALID)},

        // device goes missing after convertT
        {dsAddress, getStartedHA7S(new HA7SSerialDummy("port")
            .addDevice(new DS18B20(dsAddress).setScratchPadData(new byte[][]{
                new DS18B20Scratchpad().getRawBytes(),
                deviceNotFoundRaw}))),
            DS18B20Scratchpad.DEFAULT_RESOLUTION,
            DS18B20Scratchpad.DEFAULT_HALARM,
            DS18B20Scratchpad.DEFAULT_LALARM,
            new ReadingError(Temp18B20.ERR_DEVICE_NOT_FOUND)},

        // device gets permanent CRC after convertT
        {dsAddress, getStartedHA7S(new HA7SSerialDummy("port")
            .addDevice(new DS18B20(dsAddress).setScratchPadData(new byte[][]{
                new DS18B20Scratchpad().getRawBytes(),
                badCRCData.getRawBytes()}))),
            DS18B20Scratchpad.DEFAULT_RESOLUTION,
            DS18B20Scratchpad.DEFAULT_HALARM,
            DS18B20Scratchpad.DEFAULT_LALARM,
            new ReadingError(Temp18B20.ERR_SCRATCHPAD_DATA_CRC)},

        // success read 15.5c
        {dsAddress, getStartedHA7S(new HA7SSerialDummy("port")
            .addDevice(new DS18B20(dsAddress).setScratchPadData(new byte[][]{
                new DS18B20Scratchpad().getRawBytes(),
                new DS18B20Scratchpad().setTempC(15.5F).getRawBytes()}))),
            DS18B20Scratchpad.DEFAULT_RESOLUTION,
            DS18B20Scratchpad.DEFAULT_HALARM,
            DS18B20Scratchpad.DEFAULT_LALARM,
            new ReadingData(15.5F, 1L)},

        // success read 77.125c
        {dsAddress, getStartedHA7S(new HA7SSerialDummy("port")
            .addDevice(new DS18B20(dsAddress).setScratchPadData(new byte[][]{
                new DS18B20Scratchpad().setTempC((float) 1.0).getRawBytes(),
                new DS18B20Scratchpad().setTempC((float) 77.125).getRawBytes()
            }))),
            DS18B20Scratchpad.DEFAULT_RESOLUTION,
            DS18B20Scratchpad.DEFAULT_HALARM,
            DS18B20Scratchpad.DEFAULT_LALARM,
            new ReadingData((float)77.125, 1L)},

        // success read with CRC, recovered -79.375
        {dsAddress, getStartedHA7S(new HA7SSerialDummy("port")
            .addDevice(new DS18B20(dsAddress).setScratchPadData(new byte[][]{
                new DS18B20Scratchpad().setTempC((float) 1.0).getRawBytes(),
                badCRCData.getRawBytes(), // CRC error
                badCRCData.getRawBytes(), // CRC error
                badCRCData.getRawBytes(), // CRC error
                new DS18B20Scratchpad().setTempC((float)-79.375).getRawBytes()
            }))),
            DS18B20Scratchpad.DEFAULT_RESOLUTION,
            DS18B20Scratchpad.DEFAULT_HALARM,
            DS18B20Scratchpad.DEFAULT_LALARM,
            new ReadingData((float)-79.375, 1L)},

        // read with CRC, failed
        {dsAddress, getStartedHA7S(new HA7SSerialDummy("port")
            .addDevice(new DS18B20(dsAddress).setScratchPadData(new byte[][]{
                new DS18B20Scratchpad().setTempC((float) 1.0).getRawBytes(),
                badCRCData.getRawBytes(), // CRC error
                badCRCData.getRawBytes(), // CRC error
                badCRCData.getRawBytes(), // CRC error
                badCRCData.getRawBytes(), // CRC error
                badCRCData.getRawBytes(), // CRC error
            }))),
            DS18B20Scratchpad.DEFAULT_RESOLUTION,
            DS18B20Scratchpad.DEFAULT_HALARM,
            DS18B20Scratchpad.DEFAULT_LALARM,
            new ReadingError(Temp18B20.ERR_SCRATCHPAD_DATA_CRC)},

        // read with device going missing, failed
        {dsAddress, getStartedHA7S(new HA7SSerialDummy("port")
            .addDevice(new DS18B20(dsAddress).setScratchPadData(new byte[][]{
                new DS18B20Scratchpad().getRawBytes(),
                deviceNotFoundRaw
            }))),
            DS18B20Scratchpad.DEFAULT_RESOLUTION,
            DS18B20Scratchpad.DEFAULT_HALARM,
            DS18B20Scratchpad.DEFAULT_LALARM,
            new ReadingError(Temp18B20.ERR_DEVICE_NOT_FOUND)},
    };
  }

  @Test(dataProvider = "getTemperatureCases")
  public void testGetTemperatureCases(DSAddress dsAddress, BusMaster bm, byte resolution,
      byte tempHAlarm, byte tempLAlarm, Reading expectedReading) {

    Temp18B20 t = new Temp18B20(dsAddress, resolution, tempHAlarm, tempLAlarm);

    t.setBusMaster(bm);

    Temp18B20.Reading r1 = t.getTemperature(0L);
    Assert.assertTrue(r1 != null);
    Assert.assertEquals(r1.getClass(), expectedReading.getClass());
    if (r1 instanceof ReadingError) {
      Assert.assertEquals(r1.getError(), expectedReading.getError());
    } else {
      Assert.assertTrue(r1 instanceof ReadingData);
      Assert.assertEquals(r1.getTempC(), expectedReading.getTempC());
    }
  }

  @Test
  public void testGetTemperatureWithinTimeMSec(){
    DSAddress dsAddress = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);

    Temp18B20 t = new Temp18B20(dsAddress, DS18B20Scratchpad.DEFAULT_RESOLUTION,
        DS18B20Scratchpad.DEFAULT_HALARM, DS18B20Scratchpad.DEFAULT_LALARM)
        .setBusMaster(getStartedHA7S(new HA7SSerialDummy("port")
            .addDevice(new DS18B20(dsAddress).setScratchPadData(new byte[][]{
            new DS18B20Scratchpad().setTempC((float) 1.0).getRawBytes(),
                new DS18B20Scratchpad().setTempC((float) 5.0).getRawBytes(),
                new DS18B20Scratchpad().setTempC((float) 99.0).getRawBytes(),
        }))));

    // first temperature will return a 5.0
    Temp18B20.Reading r = t.getTemperature(0L);
    Assert.assertTrue(r != null);
    Assert.assertTrue(r instanceof ReadingData);
    Assert.assertEquals(r.getTempC(), (float)5.0);

    // the second get temperature is asking for a temperature between 5 seconds ago and NOW so we
    // will get the same temperature again.
    r = t.getTemperature(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(5));
    Assert.assertTrue(r != null);
    Assert.assertTrue(r instanceof ReadingData);
    Assert.assertEquals(r.getTempC(), (float)5.0);

    // the third temperature is asking for a temperature between NOW and NOW so we will have to
    // generate a new temperature
    r = t.getTemperature(System.currentTimeMillis());
    Assert.assertTrue(r != null);
    Assert.assertTrue(r instanceof ReadingData);
    Assert.assertEquals(r.getTempC(), (float)99.0);
  }

  /**
   * Tests that multiple threads can read the same temperature and only one will push and the others
   * will return the same value.
   */
  @Test
  public void testGetTemperatureThreaded() {
    DSAddress dsAddress = DSAddress.fromUncheckedHex(DSAddress._EE0000065BC0AE28);

    Temp18B20 t = new Temp18B20(dsAddress, DS18B20Scratchpad.DEFAULT_RESOLUTION,
        DS18B20Scratchpad.DEFAULT_HALARM, DS18B20Scratchpad.DEFAULT_LALARM)
        .setBusMaster(getStartedHA7S(new HA7SSerialDummy("port")
            .addDevice(new DS18B20(dsAddress).setScratchPadData(new byte[][]{
                new DS18B20Scratchpad().setTempC((float) 1.0).getRawBytes(),
                new DS18B20Scratchpad().setTempC((float) 5.0).getRawBytes(),
                badCRCData.getRawBytes()
            }))));

    Temp18B20.Reading[] readings = new Temp18B20.Reading[5];
    Thread[] threads = new Thread[5];
    for (int i = 0; i < threads.length; i++) {
      threads[i] = new Thread(new GetTemperatureThread(t, 2000, readings, i));
    }
    for (int i = 0; i < threads.length; i++) {
      threads[i].start();
    }
    for (int i = 0; i < threads.length; i++) {
      try {
        threads[i].join();
      }
      catch (InterruptedException e) {
        Assert.fail("join:" + i + " interrupted:" + e);
      }
      Assert.assertTrue(readings[i] != null);
      Assert.assertTrue(readings[i] instanceof ReadingData);
      Assert.assertEquals(readings[i].getTempC(), (float)5.0);
      Assert.assertEquals(readings[i].getTimeMSec(), readings[0].getTimeMSec());
    }
  }

  private static class GetTemperatureThread implements Runnable {
    private final Temp18B20 t;
    private final long withinTimeMSec;
    private final Temp18B20.Reading[] array;
    private final int index;

    public GetTemperatureThread(Temp18B20 t, long withinTimeMSec, Temp18B20.Reading[] array, int index) {
      this.t = t;
      this.withinTimeMSec = withinTimeMSec;
      this.array = array;
      this.index = index;
    }

    public void run() {
      try {
        Thread.sleep(20 * index);
      }
      catch (InterruptedException e) {
        ;
      }

      array[index] = t.getTemperature(withinTimeMSec);
    }
  }

  /*
   * Get temperature, already in progress
   */

  private BusMaster getMockBMFor(DSAddress dsAddress,
      ConvertTCmd.Result convertTCmdResult,
      long convertTCmdWriteCTM,
      ReadScratchpadCmd.Result readScratchpadCmdResult,
      long readScratchpadCmdWriteCTM,
      byte[] readScratchpadCmdResultData) {
    BusMaster mockBM = mock(BusMaster.class);

    ConvertTCmd mockConvertTCmd = mock(ConvertTCmd.class);
    when(mockConvertTCmd.getAddress()).thenReturn(dsAddress);
    when(mockConvertTCmd.getBusMaster()).thenReturn(mockBM);
    when(mockConvertTCmd.execute()).thenReturn(convertTCmdResult);
    when(mockConvertTCmd.getResultWriteCTM()).thenReturn(convertTCmdWriteCTM);

    ReadScratchpadCmd mockReadScratchpadCmd = mock(ReadScratchpadCmd.class);
    when(mockReadScratchpadCmd.getAddress()).thenReturn(dsAddress);
    when(mockReadScratchpadCmd.getBusMaster()).thenReturn(mockBM);
    when(mockReadScratchpadCmd.execute()).thenReturn(readScratchpadCmdResult);
    when(mockReadScratchpadCmd.getResultWriteCTM()).thenReturn(readScratchpadCmdWriteCTM);
    when(mockReadScratchpadCmd.getResultData()).thenReturn(readScratchpadCmdResultData);

    when(mockBM.queryConvertTCmd(any(DSAddress.class))).thenReturn(mockConvertTCmd);
    when(mockBM.queryReadScratchpadCmd(any(DSAddress.class), anyShort()))
        .thenReturn(mockReadScratchpadCmd);

    return mockBM;
  }

  private BusMaster getStartedHA7S(HA7SSerial ha7SSerial) {
    ha7SSerial.start();
    return new HA7S(ha7SSerial);
  }

}
