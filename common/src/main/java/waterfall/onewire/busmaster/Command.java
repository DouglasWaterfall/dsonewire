package waterfall.onewire.busmaster;

public class Command {

  /**
   *
   */
  public static final byte SEARCH_ROM = (byte) 0xF0;

  /**
   *
   */
  public static final byte READ_ROM = (byte) 0x33;

  /**
   *
   */
  public static final byte MATCH_ROM = (byte) 0x55;

  /**
   *
   */
  public static final byte SKIP_ROM = (byte) 0xCC;

  /**
   *
   */
  public static final byte ALARM_SEARCH = (byte) 0xEC;

  /**
   *
   */
  public static final byte CONVERT_T = (byte) 0x44;

  /**
   *
   */
  public static final byte WRITE_SCRATCHPAD = (byte) 0x4E;

  /**
   *
   */
  public static final byte READ_SCRATCHPAD = (byte) 0xBE;

  /**
   *
   */
  public static final byte COPY_SCRATCHPAD = (byte) 0x48;

  /**
   *
   */
  public static final byte RECALL_E2 = (byte) 0xB8;

  /**
   *
   */
  public static final byte READ_POWER_SUPPLY = (byte) 0xB4;

  /**
   *
   */
  public static final byte READ_STATUS = (byte) 0xAA;

  /**
   *
   */
  public static final byte CHANNEL_ACCESS = (byte) 0xF5;

}
