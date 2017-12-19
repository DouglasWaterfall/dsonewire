package waterfall.onewire.busmasters.HA7S;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Created by dwaterfa on 12/17/17.
 */
public class ConstructorTests {

  @Test(dataProvider = "getConstructorNegativeCase",
      expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = "serial must non-null and started")
  public void testConstructorNegativeCases(HA7SSerial serial) {
    new HA7S(serial);
  }

  @DataProvider
  public Object[][] getConstructorNegativeCases() {
    return new Object[][] {
        { null },
        { mock(HA7SSerial.class)}
    };
  }

  @Test
  public void testConstructorDefaults() {

    HA7SSerial mockSerial = mock(HA7SSerial.class);
    String portName = "foo";
    when(mockSerial.getPortName()).thenReturn(portName);
    when(mockSerial.isStarted()).thenReturn(true);

    HA7S ha7s = new HA7S(mockSerial);
    Assert.assertEquals(ha7s.getName(), "HA7S on " + portName);


    try {
      long ctm = ha7s.getCurrentTimeMillis();
      Assert.assertTrue(ctm > 0);
      Thread.sleep(100);
      long next_ctm = ha7s.getCurrentTimeMillis();
      Assert.assertTrue(ctm < next_ctm);
    } catch (InterruptedException e) {
      Assert.fail("Unexpected exception:" + e);
    }
  }

}
