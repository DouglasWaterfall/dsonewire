package waterfall.onewire;

import java.util.*;

import waterfall.onewire.busmaster.BusMaster;

public class BusMasterManager {

    private Hashtable<String, BusMaster> bms = new Hashtable<String, BusMaster>();
    private Hashtable<String, BusMaster> devices = new Hashtable<String, BusMaster>();

    public BusMasterManager() {
    }

    public void add(BusMaster bm) {
        assert (bm != null);
        
        Base64.Encoder encoder = Base64.getUrlEncoder();
        
        String bmIdent = encoder.encodeToString(bm.getName().getBytes());
        
        if (!bms.containsKey(bmIdent)) {
            System.out.println("Adding busMaster" + bm.getName());
            bms.put(bmIdent, bm);
        }
        else if (bms.get(bmIdent) == bm) {
            System.err.println("Duplicate add of busMaster" + bm.getName());
        }
        else {
            System.err.println("name encoding collision of busMaster:" + bm.getName() + " and " + bms.get(bmIdent).getName());
        }
    }

    public BusMaster getBusMasterByIdent(String bmIdent) {
        assert (bmIdent != null);

        return bms.get(bmIdent);
    }

    public ArrayList<String> getBusMasterIdents() {
        ArrayList<String> list = new ArrayList<String>();

        for (Enumeration<String> bmEnum = bms.keys(); bmEnum.hasMoreElements(); ) {
            list.add(bmEnum.nextElement());
        }

        return list;
    }

    public void addDevice(String addrAsHex, BusMaster bm) {
        BusMaster currBM = devices.get(addrAsHex);
        if (currBM != bm) {
            devices.put(addrAsHex, bm);
        }
    }

    public String[] getDevices() {
        String[] list = new String[devices.size()];

        int i = 0;
        for (Enumeration<String> devEnum = devices.keys(); devEnum.hasMoreElements(); ) {
            list[i++] = devEnum.nextElement();
        }

        return list;
    }

    public BusMaster getBusMasterForDevice(String addrAsHex) {
        return devices.get(addrAsHex);
    }

}
