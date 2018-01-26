package waterfall.onewire;

import java.util.Observable;
import java.util.Observer;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import waterfall.onewire.busmaster.BusMaster;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmasters.HA7S.HA7S;
import waterfall.onewire.busmasters.HA7S.HA7SSerial;
import waterfall.onewire.busmasters.HA7S.HA7SSerialDummy;

/**
 * Created by dwaterfa on 1/31/17.
 */
public class HA7SBusMasterManagerTest {

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullRegistry() {
    HA7SBusMasterManager ha7sBMM = new HA7SBusMasterManager(null);
  }

  @Test(dataProvider = "getNegativeTestData")
  public void testNegativeTests(String ttyArg, Class serialClass, Logger.LogLevel logLevel,
      Class exceptionClass) {
    BusMasterRegistry bmRegistry = new BusMasterRegistry();

    HA7SBusMasterManager ha7sBMM = new HA7SBusMasterManager(bmRegistry);

    try {
      ha7sBMM.start(ttyArg, serialClass.getName(), logLevel);
      Assert.fail("expected exception");
    } catch (Exception e) {
      Assert.assertNotNull(exceptionClass);
      Assert.assertTrue(e.getClass().equals(exceptionClass));
    }
  }

  @DataProvider
  public Object[][] getNegativeTestData() {
    return new Object[][]{
        // ttyArg
        {null, HA7SSerialDummy.class, Logger.LogLevel.CmdOnlyLevel(),
            IllegalArgumentException.class},
        {"", HA7SSerialDummy.class, Logger.LogLevel.CmdOnlyLevel(), IllegalArgumentException.class},
        {",", HA7SSerialDummy.class, Logger.LogLevel.CmdOnlyLevel(),
            IllegalArgumentException.class},

        // class
        {"Foo", null, Logger.LogLevel.CmdOnlyLevel(), IllegalArgumentException.class},
        {"Foo", Object.class, Logger.LogLevel.CmdOnlyLevel(), IllegalArgumentException.class},

        // Logger
        {"Foo,", HA7SSerialDummy.class, null, IllegalArgumentException.class}
    };
  }

  @Test
  public void testAddRemoveNotification() {
    BusMasterRegistry bmRegistry = new BusMasterRegistry();

    myBusMasterRegistryObserver o = new myBusMasterRegistryObserver(bmRegistry);
    Assert.assertNull(o.added);
    Assert.assertNull(o.removed);
    Assert.assertEquals(o.bmRegistry, bmRegistry);

    // We are observing BEFORE the add
    bmRegistry.addObserver(o);

    HA7SBusMasterManager ha7sBMM = new HA7SBusMasterManager(bmRegistry);

    HA7S[] addedList = null;

    try {
      addedList = ha7sBMM.start("Foo", HA7SSerialDummy.class.getName(), Logger.LogLevel.CmdOnlyLevel());
    } catch (Exception e) {
      Assert.fail("exception not expected");
    }

    Assert.assertNotNull(addedList);
    Assert.assertNotNull(o.added);
    Assert.assertEquals(o.added.getBusMaster(), addedList[0]);
    Assert.assertNull(o.removed);

    // We are observing AFTER the add
    myBusMasterRegistryObserver o2 = new myBusMasterRegistryObserver(bmRegistry);
    Assert.assertNull(o2.added);
    Assert.assertNull(o2.removed);

    bmRegistry.addObserver(o2);
    Assert.assertNotNull(o2.added);
    Assert.assertEquals(o2.added.getBusMaster(), addedList[0]);
    Assert.assertNull(o2.removed);

    o2.reset();

    // We are removing ourself BEFORE the remove
    bmRegistry.deleteObserver(o2);

    o.reset();

    HA7S ha7s = (HA7S) bmRegistry.getBusMasters()[0];

    ha7sBMM.stop(ha7s);

    Assert.assertNull(o.added);
    Assert.assertNotNull(o.removed);
    Assert.assertEquals(o.removed.getBusMaster(), ha7s);
    Assert.assertEquals(bmRegistry.getBusMasters().length, 0);

    // not notified as we were removed earlier
    Assert.assertNull(o2.added);
    Assert.assertNull(o2.removed);
  }

  @Test
  public void test() {
    BusMasterRegistry bmRegistry = new BusMasterRegistry();

    HA7SBusMasterManager ha7sBMM = new HA7SBusMasterManager(bmRegistry);

    String foo = "Foo";
    String bar = "Bar";
    String ha7sTTYArg = foo + "," + bar;

    try {
      ha7sBMM.start(ha7sTTYArg, HA7SSerialDummy.class.getName(), Logger.LogLevel.CmdOnlyLevel());
    } catch (Exception e) {
      Assert.fail("exception not expected");
    }

    Assert.assertNotNull(bmRegistry.getBusMasters());
    Assert.assertEquals(bmRegistry.getBusMasters().length, 2);
    int fooCount = 0;
    int barCount = 0;
    for (BusMaster bm : bmRegistry.getBusMasters()) {
      if (bm.getName().equals("HA7S on " + foo)) {
        fooCount++;
      } else if (bm.getName().equals("HA7S on " + bar)) {
        barCount++;
      }
    }
    Assert.assertEquals(fooCount, 1);
    Assert.assertEquals(barCount, 1);

    for (BusMaster bm : bmRegistry.getBusMasters()) {
      ha7sBMM.stop((HA7S) bm);
    }

    Assert.assertNotNull(bmRegistry.getBusMasters());
    Assert.assertEquals(bmRegistry.getBusMasters().length, 0);

  }

  public class myBusMasterRegistryObserver implements Observer {

    public BusMasterRegistry bmRegistry;
    public BusMasterRegistry.BusMasterAdded added;
    public BusMasterRegistry.BusMasterRemoved removed;

    public myBusMasterRegistryObserver(BusMasterRegistry bmRegistry) {
      this.bmRegistry = bmRegistry;
      this.added = null;
      this.removed = null;
    }

    @Override
    public void update(Observable o, Object arg) {
      Assert.assertEquals(o, bmRegistry);
      Assert.assertNotNull(arg);
      Assert.assertTrue((arg instanceof BusMasterRegistry.BusMasterAdded) ||
          (arg instanceof BusMasterRegistry.BusMasterRemoved));
      if (arg instanceof BusMasterRegistry.BusMasterAdded) {
        added = (BusMasterRegistry.BusMasterAdded) arg;
        removed = null;
      } else if (arg instanceof BusMasterRegistry.BusMasterRemoved) {
        added = null;
        removed = (BusMasterRegistry.BusMasterRemoved) arg;
      } else {
        Assert.fail("Unknown arg type");
      }
    }

    public void reset() {
      added = null;
      removed = null;
    }
  }

    /*
    public void addObserver(Observer o) {
    */

}

