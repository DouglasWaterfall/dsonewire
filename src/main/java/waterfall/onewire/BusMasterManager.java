package waterfall.onewire;

import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import waterfall.onewire.busmaster.BusMaster;

@Service
public class BusMasterManager {

    private Logger logger = LogManager.getLogger(BusMasterManager.class.getName());

    private Hashtable<String, BusMaster> bms = new Hashtable<String, BusMaster>();
    private Hashtable<String, BusMaster> devices = new Hashtable<String, BusMaster>();

    public BusMasterManager() {
    }

    public void add(BusMaster bm) {
        assert (bm != null);
        
        Base64.Encoder encoder = Base64.getUrlEncoder();
        
        String bmIdent = encoder.encodeToString(bm.getName().getBytes());
        
        if (!bms.containsKey(bmIdent)) {
            logger.info("Adding busMaster" + bm.getName());
            bms.put(bmIdent, bm);
        }
        else if (bms.get(bmIdent) == bm) {
            logger.error("Duplicate add of busMaster" + bm.getName());
        }
        else {
            logger.error("name encoding collision of busMaster:" + bm.getName() + " and " + bms.get(bmIdent).getName());
        }
    }

    public BusMaster getBusMasterByIdent(String bmIdent) {
        assert (bmIdent != null);

        return bms.get(bmIdent);
    }

    public String[] getBusMasterIdents() {
        String[] list = new String[bms.size()];

        int i = 0;
        for (Enumeration<String> bmEnum = bms.keys(); bmEnum.hasMoreElements(); ) {
            list[i++] = bmEnum.nextElement();
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
