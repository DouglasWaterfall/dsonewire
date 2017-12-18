package waterfall.onewire.busmasters.HA7S;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by dwaterfa on 12/17/17.
 */
public class ConstructorTests {

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

  @Test
  public void testConstructorDummySerial() {
    HA7S ha7s = null;

    try {
      ha7s = new HA7S(new HA7SSerialDummy("port"));
    } catch (Exception e) {
      Assert.fail("Exception not expected");
    }

    Assert.assertNotNull(ha7s);
    Assert.assertNotNull(ha7s.getName());
    Assert.assertTrue(ha7s.getName().startsWith("HA7S on "));

    long ctm = ha7s.getCurrentTimeMillis();
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Assert.fail("Unexpected exception:" + e);
    }
    Assert.assertTrue(ctm < ha7s.getCurrentTimeMillis());

    Assert.assertFalse(ha7s.getIsStarted());
  }

}
