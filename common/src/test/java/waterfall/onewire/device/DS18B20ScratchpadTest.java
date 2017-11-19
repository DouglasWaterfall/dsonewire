package waterfall.onewire.device;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Created by dwaterfa on 11/18/17.
 */
public class DS18B20ScratchpadTest {

    @Test
    public void testPowerOnDefaults() {

        DS18B20Scratchpad data = new DS18B20Scratchpad();
        Assert.assertTrue(data.checkValid());

        // default
        Assert.assertEquals(data.getResolution(), 3);

        // default
        Assert.assertEquals(data.getTempC(), (float) 1.0);
    }

    @DataProvider
    public Object[][] resolutionNegativeCases() {
        return new Object[][] {
                { -1 },
                { 4 }
        };
    }

    @Test(dataProvider = "resolutionNegativeCases", expectedExceptions = IllegalArgumentException.class)
    public void testResolutionNegativeCases(int resolution) {
        DS18B20Scratchpad data = new DS18B20Scratchpad();
        data.setResolution((byte) resolution);
    }

    @DataProvider
    public Object[][] resolutionPositiveCases() {
        return new Object[][] {
                { 0 },
                { 1 },
                { 2 },
                { 3 }
        };
    }

    @Test(dataProvider = "resolutionPositiveCases")
    public void testResolutionPositiveCases(int resolution) {
        DS18B20Scratchpad data = new DS18B20Scratchpad();
        data.setResolution((byte) resolution);
        Assert.assertTrue(data.checkValid());
        Assert.assertEquals(data.getResolution(), resolution);
    }

    @DataProvider
    public Object[][] tempAlarmPositiveCases() {
        return new Object[][] {
                { (byte)-127 },
                { (byte)-63 },
                { (byte)0 },
                { (byte)31 },
                { (byte)127 },
        };
    }

    @Test(dataProvider = "tempAlarmPositiveCases")
    public void testTempLAlarmPositiveCases(byte tempC) {
        DS18B20Scratchpad data = new DS18B20Scratchpad();
        data.setTempLAlarm(tempC);
        Assert.assertTrue(data.checkValid());
        Assert.assertEquals(data.getTempLAlarm(), tempC);
    }

    @Test(dataProvider = "tempAlarmPositiveCases")
    public void testTempHAlarmPositiveCases(byte tempC) {
        DS18B20Scratchpad data = new DS18B20Scratchpad();
        data.setTempHAlarm(tempC);
        Assert.assertTrue(data.checkValid());
        Assert.assertEquals(data.getTempHAlarm(), tempC);
    }

    @DataProvider
    public Object[][] tempCNegativeCases() {
        return new Object[][] {
                { (float)-128.0 },
                { (float)128.0 }
        };
    }

    @Test(dataProvider = "tempCNegativeCases", expectedExceptions = IllegalArgumentException.class)
    public void testTempCNegativeCases(float tempC) {
        DS18B20Scratchpad data = new DS18B20Scratchpad();
        data.setTempC(tempC);
    }

    @DataProvider
    public Object[][] tempCPositiveCases() {
        return new Object[][] {
                { (float)127.0 + 15/16 },
                { (float)64.0 + 14/16 },
                { (float)63.0 + 13/16 },
                { (float)32.0 + 12/16 },
                { (float)31.0 + 11/16 },
                { (float)16.0 + 10/16 },
                { (float)15.0 + 9/16 },
                { (float)8.0 + 8/16 },
                { (float)7.0 + 7/16 },
                { (float)6.0 + 6/16 },
                { (float)5.0 + 5/16 },
                { (float)4.0 + 4/16 },
                { (float)3.0 + 3/16 },
                { (float)2.0 + 2/16 },
                { (float)1.0 + 1/16 },
                { (float)0.0 },
                { (float)-127.0 - 15/16 },
                { (float)-64.0 - 14/16 },
                { (float)-63.0 - 13/16 },
                { (float)-32.0 - 12/16 },
                { (float)-31.0 - 11/16 },
                { (float)-16.0 - 10/16 },
                { (float)-15.0 - 9/16 },
                { (float)-8.0 - 8/16 },
                { (float)-7.0 - 7/16 },
                { (float)-6.0 - 6/16 },
                { (float)-5.0 - 5/16 },
                { (float)-4.0 - 4/16 },
                { (float)-3.0 - 3/16 },
                { (float)-2.0 - 2/16 },
                { (float)-1.0 - 1/16 }
        };
    }

    @Test(dataProvider = "tempCPositiveCases")
    public void testTempCPositiveCases(float tempC) {
        DS18B20Scratchpad data = new DS18B20Scratchpad();
        data.setTempC(tempC);
        Assert.assertTrue(data.checkValid());
        Assert.assertEquals(data.getTempC(), tempC);
    }

    @DataProvider
    public Object[][] tempCResolutionCases() {

        float positiveF = (float)6.0 + 15/16;
        float negativeF = (float)-6.0 - 7/16;

        return new Object[][] {
                { positiveF, (byte)3, positiveF },
                { positiveF, (byte)2, (float)6.0 + 7/8 },
                { positiveF, (byte)1, (float)6.0 + 3/4 },
                { positiveF, (byte)0, (float)6.0 + 1/2 },
                { negativeF, (byte)3, negativeF },
                { negativeF, (byte)2, (float)-6.0 - 3/8 },
                { negativeF, (byte)1, (float)-6.0 - 1/4 },
                { negativeF, (byte)0, (float)-6.0 - 1/2 },
        };
    }

    @Test(dataProvider = "tempCResolutionCases")
    public void testTempCResolutionCases(float tempC, byte resolution, float expectedTempC) {
        DS18B20Scratchpad data = new DS18B20Scratchpad();
        data.setResolution(resolution);
        data.setTempC(tempC);
        Assert.assertTrue(data.checkValid());
        Assert.assertEquals(data.getTempC(), expectedTempC);
    }

}
