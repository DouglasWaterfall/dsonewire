package waterfall.onewire;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import waterfall.onewire.busmaster.BusMaster;

@Service
public class BusMasterManager {

    private Logger logger = LogManager.getLogger(BusMasterManager.class.getName());

    private ArrayList<BusMaster> busMasterList = new ArrayList<BusMaster>();
    private Hashtable<String, BusMaster> devices = new Hashtable<String, BusMaster>();

    public BusMasterManager() {
    }

    public void add(BusMaster bm) {
        if (!busMasterList.contains(bm)) {
            logger.info("Adding busMaster" + bm.getName());
            busMasterList.add(bm);
        }
        else {
            logger.error("Duplicate add of busMaster" + bm.getName());
        }
    }

    public void addDevice(String addrAsHex, BusMaster bm) {
        BusMaster currBM = devices.get(addrAsHex);
        if (currBM != bm) {
            devices.put(addrAsHex, bm);
        }
    }

    public BusMaster getBusMasterByName(String bmName) {
        for (BusMaster bm : busMasterList) {
            if (bm.getName().equals(bmName)) {
                return bm;
            }
        }

        return null;
    }

    public BusMaster[] getBusMasters() {
        return busMasterList.toArray(new BusMaster[busMasterList.size()]);
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
