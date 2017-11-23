/**
 * Created by dwaterfa on 10/30/17.
 */

package waterfall.onewire;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyShort;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
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
import waterfall.onewire.busmasters.HA7S.HA7SStartBusCmd;
import waterfall.onewire.busmasters.HA7S.part.DS18B20;
import waterfall.onewire.device.DS18B20Scratchpad;


public class Temp18B20Test {

  private String validDSAddress = "EE0000065BC0AE28";

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
    new Temp18B20(new DSAddress(validDSAddress), resolution, Byte.MIN_VALUE, Byte.MAX_VALUE);
  }

  @DataProvider
  public Object[][] constructorPositiveCases() {
    DSAddress valid = new DSAddress(validDSAddress);

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
    DSAddress dsAddress = new DSAddress(validDSAddress);

    Temp18B20 t = new Temp18B20(dsAddress, (byte)3, Byte.MIN_VALUE, Byte.MAX_VALUE);

    Temp18B20.Reading r = t.getTemperature(100L);
    Assert.assertTrue(r instanceof Temp18B20.ReadingError);
    Assert.assertEquals(((Temp18B20.ReadingError) r).getError(), Temp18B20.ERR_NO_BUSMASTER);
  }

  @Test
  public void testSetBusMaster() {
    DSAddress dsAddress = new DSAddress(validDSAddress);

    Temp18B20 t = new Temp18B20(dsAddress, (byte)2, Byte.MIN_VALUE, Byte.MAX_VALUE);
    Assert.assertNull(t.getBusMaster());

    BusMaster mockBM = getMockBMFor(dsAddress, null, 0, null, 0, null);

    t.setBusMaster(mockBM);
    Assert.assertEquals(t.getBusMaster(), mockBM);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "dup bm")
  public void testSetDuplicateBusMaster() {
    DSAddress dsAddress = new DSAddress(validDSAddress);

    Temp18B20 t = new Temp18B20(dsAddress, (byte)2, Byte.MIN_VALUE, Byte.MAX_VALUE);
    Assert.assertNull(t.getBusMaster());

    BusMaster mockBM = getMockBMFor(dsAddress, null, 0, null, 0, null);

    t.setBusMaster(mockBM);
    t.setBusMaster(mockBM);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "dup bMR")
  public void testSetBusMasterRegistry() {
    DSAddress dsAddress = new DSAddress(validDSAddress);

    Temp18B20 t = new Temp18B20(dsAddress, (byte)2, Byte.MIN_VALUE, Byte.MAX_VALUE);
    Assert.assertNull(t.getBusMaster());

    BusMasterRegistry bmR = new BusMasterRegistry();
    t.setBusMasterRegistry(bmR);
    t.setBusMasterRegistry(bmR);
  }

  @DataProvider
  public Object[][] getTemperatureCases() {
    DSAddress dsAddress = new DSAddress(validDSAddress);
    BusMaster nullBM = null;

    byte[] deviceNotFoundRaw = new byte[9];
    Arrays.fill(deviceNotFoundRaw, (byte) 0xff);

    DS18B20Scratchpad badCRCData = new DS18B20Scratchpad();
    badCRCData.getRawBytes()[0] = (byte) ~badCRCData.getRawBytes()[0];

    return new Object[][]{
        /*
        // busmaster not assigned
        {dsAddress, nullBM, PrecisionBits.Twelve, Byte.MIN_VALUE, Byte.MAX_VALUE,
            new ReadingError(Temp18B20.ERR_NO_BUSMASTER)},
        // device not found
        {dsAddress, getStartedHA7S(new HA7SSerialDummy("port")
            .addDevice(new DS18B20(dsAddress).setScratchPadData(new byte[][]{deviceNotFoundRaw}))),
            PrecisionBits.Twelve, Byte.MIN_VALUE, Byte.MAX_VALUE,
            new ReadingError(Temp18B20.ERR_DEVICE_NOT_FOUND)},
        // bad data when reading the device scratchpad
        {dsAddress, getStartedHA7S(new HA7SSerialDummy("port")
            .addDevice(
                new DS18B20(dsAddress).setScratchPadData(new byte[][]{badCRCData.getRawBytes()}))),
            PrecisionBits.Twelve, Byte.MIN_VALUE, Byte.MAX_VALUE,
            new ReadingError(Temp18B20.ERR_SCRATCHPAD_DATA_NOT_VALID)},
        // device goes missing after convertT
        {dsAddress, getStartedHA7S(new HA7SSerialDummy("port")
            .addDevice(new DS18B20(dsAddress).setScratchPadData(new byte[][]{
                new DS18B20Scratchpad().getRawBytes(),
                deviceNotFoundRaw}))),
            PrecisionBits.Twelve, Byte.MIN_VALUE, Byte.MAX_VALUE,
            new ReadingError(Temp18B20.ERR_DEVICE_NOT_FOUND)},
        // device gets permanent CRC after convertT
        {dsAddress, getStartedHA7S(new HA7SSerialDummy("port")
            .addDevice(new DS18B20(dsAddress).setScratchPadData(new byte[][]{
                new DS18B20Scratchpad().getRawBytes(),
                badCRCData.getRawBytes()}))),
            PrecisionBits.Twelve, Byte.MIN_VALUE, Byte.MAX_VALUE,
            new ReadingError(Temp18B20.ERR_SCRATCHPAD_DATA_CRC)},
        */

        // success read 15.5c
        {dsAddress, getStartedHA7S(new HA7SSerialDummy("port")
            .addDevice(new DS18B20(dsAddress).setScratchPadData(new byte[][]{
                new DS18B20Scratchpad().getRawBytes(),
                new DS18B20Scratchpad().setTempC(15.5F).getRawBytes()}))),
            DS18B20Scratchpad.DEFAULT_RESOLUTION,
            DS18B20Scratchpad.DEFAULT_HALARM,
            DS18B20Scratchpad.DEFAULT_LALARM,
            new ReadingData(15.5F, 1L)},
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

  /*
  * Tests
  * Get temperature, device not found
  * Get temperature, device needs to be initialized
  * Get temperature, no previous temperature, get new temperature
  * Get temperature, use previous temp
  * Get temperature, get new temperature
  * Get temperature, device was powercycled re-initialized
  * Get temperature, device goes missing
  * Get temperature, device CRC error, not fixed
  * Get temperature, device CRC error, fixed
  * Get temperature, already in progress
  *
  * Bonus:
  * Get temperature, BusMaster not assigned yet
  * Get temperature, Device not found, look for new BusMaster after certain number of retries?
  */

  @Test
  public void testSimpleGetTemperature() {
    DSAddress dsAddress = new DSAddress(validDSAddress);

    Temp18B20 t = new Temp18B20(dsAddress, DS18B20Scratchpad.DEFAULT_RESOLUTION,
        DS18B20Scratchpad.DEFAULT_HALARM, DS18B20Scratchpad.DEFAULT_LALARM);

    DS18B20Scratchpad data_1 = new DS18B20Scratchpad();
    data_1.setTempC((float) 1.0);

    DS18B20Scratchpad data_2 = new DS18B20Scratchpad();
    data_2.setTempC((float) 77.0);

    DS18B20 dummyDS18B20 = new DS18B20(dsAddress)
        .setScratchPadData(new byte[][]{
            data_1.getRawBytes(),
            data_1.getRawBytes(),
            data_2.getRawBytes(),
        });

    t.setBusMaster(getStartedHA7S(new HA7SSerialDummy("port").addDevice(dummyDS18B20)));

    Temp18B20.Reading r1 = t.getTemperature(0L);
    Assert.assertTrue(r1 != null);
    Assert.assertEquals(r1.getTempC(), (float) 1.0);

    Temp18B20.Reading r2 = t.getTemperature(0L);
    Assert.assertTrue(r2 != null);
    Assert.assertNotEquals(r1.getTempC(), r2.getTempC());
    Assert.assertEquals(r2.getTempC(), (float) 77.0);
  }

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
    BusMaster ha7s = new HA7S(ha7SSerial);

    Assert.assertFalse(ha7s.getIsStarted());
    Assert.assertEquals(HA7SStartBusCmd.Result.started, ha7s.queryStartBusCmd().execute());

    return ha7s;
  }

}
