package waterfall.onewire;

/**
 * Created by dwaterfa on 6/15/16.
 */
public class DSAddress {
    private final static String INVALID_DSADDR = "Invalid DSAddress:";
    private final String addrAsHex;

    public DSAddress(String addr) {
        if ((addr == null) || (addr.length() != 16) || (!addr.matches("[0-9A-F]*"))) {
            addrAsHex = (INVALID_DSADDR + "null");
        } else if ((addr.length() != 16) || (!addr.matches("[0-9A-F]*"))) {
            addrAsHex = (INVALID_DSADDR + addr);
        } else {
            addrAsHex = new String(addr);
        }
    }

    public short getFamilyCode() {
        return (short)((Character.digit(addrAsHex.charAt(14), 16) << 4) + Character.digit(addrAsHex.charAt(15), 16));
    }

    public String toString() {
        return addrAsHex;
    }

    public static boolean isValid(final String addr) {
        return ((addr != null) && (addr.length() == 16) && (addr.matches("[0-9A-F]*")));
    }

    public boolean equals(Object other) {
        return ((other instanceof DSAddress) &&
                (addrAsHex.equals(((DSAddress) other).addrAsHex)));
    }

}
