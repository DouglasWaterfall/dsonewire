package waterfall.onewire.busmaster.Http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import waterfall.onewire.BusMasterManager;
import waterfall.onewire.DSAddress;
import waterfall.onewire.busmaster.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dwaterfa on 6/12/16.
 */
@RestController
@RequestMapping(value = "/httpbusmaster")
public class Controller {
    BusMasterManager deviceManager;

    @Autowired
    public Controller(BusMasterManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    /*
    @RequestMapping(value = "",
                    method = RequestMethod.POST,
                    produces = {MediaType.APPLICATION_JSON_VALUE},
                    consumes = {MediaType.APPLICATION_JSON_VALUE})
    public Token create(Principal principal, @RequestBody(required = true) Token token) throws ServiceException {
        ...
    */


    @RequestMapping(value = "/status", method = RequestMethod.POST)
    public Map<String, String> status() {
        // return list of busmasters with their status started/stopped
        BusMaster[] bms = deviceManager.getBusMasters();

        Map<String, String> result = new HashMap<String, String>();
        for (BusMaster bm : bms) {
            result.put(bm.getName(), Boolean.toString(bm.getIsStarted()));
        }

        return result;
    }

    @RequestMapping(value = "/startCmd/{bm}", method = RequestMethod.POST)
    public Map<String, String> startCmd(@PathVariable(value = "bm") String bm,
                                        @RequestParam(value = "log", required = false, defaultValue = "false") String log) {

        BusMaster _bm  = deviceManager.getBusMasterByName(bm);
        if (_bm == null) {
            throw new RuntimeException("Unknown bm:" + bm);
        }

        StartBusCmd cmd = _bm.queryStartBusCmd("true".equals(log) ? new Logger() : null);
        cmd.execute();

        Map<String, String> result = new HashMap<String, String>();
        if (cmd.getOptLogger() != null) {
            result.put("log", cmd.getOptLogger().toString());
        }

        result.put("result", cmd.getResult().name());

        return result;
    }

    @RequestMapping(value = "/stopCmd/{bm}", method = RequestMethod.POST)
    public Map<String, String> stopCmd(@PathVariable(value = "bm") String bm,
                                       @RequestParam(value = "log", required = false, defaultValue = "false") String log) {

        BusMaster _bm  = deviceManager.getBusMasterByName(bm);
        if (_bm == null) {
            throw new RuntimeException("Unknown bm:" + bm);
        }

        StopBusCmd cmd = _bm.queryStopBusCmd("true".equals(log) ? new Logger() : null);
        cmd.execute();

        Map<String, String> result = new HashMap<String, String>();
        if (cmd.getOptLogger() != null) {
            result.put("log", cmd.getOptLogger().toString());
        }

        result.put("result", cmd.getResult().name());

        return result;
    }

    @RequestMapping(value = "/searchCmd/{bm}", method = RequestMethod.POST)
    public Map<String, String> searchCmd(@PathVariable(value = "bm") String bm,
                                         @RequestParam(value = "byAlarm", required = false, defaultValue = "false") String byAlarm,
                                         @RequestParam(value = "byFamilyCode", required = false, defaultValue = "") String byFamilyCode,
                                         @RequestParam(value = "log", required = false, defaultValue = "false") String log) {
        BusMaster _bm  = deviceManager.getBusMasterByName(bm);
        if (_bm == null) {
            throw new RuntimeException("Unknown bm:" + bm);
        }

        boolean _byAlarm = ((!byAlarm.isEmpty()) && ("true".equals(byAlarm)));

        Short familyCode = null;
        if (!byFamilyCode.isEmpty()) {
            try {
                familyCode = Short.valueOf(byFamilyCode);
            } catch (NumberFormatException e) {
                throw new RuntimeException("invalid byFamilyCode:" + byFamilyCode);
            }

            if ((familyCode < 0) || (familyCode > 255)) {
                throw new RuntimeException("invalid byFamilyCode:" + familyCode);
            }
        }

        if ((_byAlarm) && (familyCode != null)) {
            throw new RuntimeException("cannot have both byAlarm and byFamilyCode");
        }

        SearchBusCmd cmd = null;

        if (_byAlarm) {
            cmd = _bm.querySearchBusByAlarmCmd("true".equals(log) ? new Logger() : null);
        }
        else if (familyCode != null) {
            cmd = _bm.querySearchBusByFamilyCmd(familyCode, "true".equals(log) ? new Logger() : null);
        }
        else {
            cmd = _bm.querySearchBusCmd("true".equals(log) ? new Logger() : null);
        }

        cmd.execute();

        Map<String, String> result = new HashMap<String, String>();
        if (cmd.getOptLogger() != null) {
            result.put("log", cmd.getOptLogger().toString());
        }

        result.put("result", cmd.getResult().name());

        if (cmd.getResult() == SearchBusCmd.Result.success) {
            StringBuffer sb = new StringBuffer();
            boolean updateDM = ((!cmd.isByAlarm()) && (!cmd.isByFamilyCode()));

            for (String addrAsHex : cmd.getResultList()) {
                if (updateDM) {
                    deviceManager.addDevice(addrAsHex, _bm);
                }

                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(addrAsHex);
            }

            result.put("resultList", sb.toString());
        }

        return result;
    }

    @RequestMapping(value = "/devices", method = RequestMethod.POST)
    public String[] devices() {
        // return list of devices gathered from the last full search
        return deviceManager.getDevices();
    }

    @RequestMapping(value = "/readPowerSupplyCmd/{dsAddr}", method = RequestMethod.POST)
    public Map<String, String> readPowerSupplyCmd(@PathVariable(value = "dsAddr") String dsAddr,
                                                  @RequestParam(value = "log", required = false, defaultValue = "false") String log) {
        if ((dsAddr == null) || (!DSAddress.isValid(dsAddr))) {
            throw new RuntimeException("invalid dsAddr:" + dsAddr);
        }

        BusMaster bm = deviceManager.getBusMasterForDevice(dsAddr);
        if (bm == null) {
            throw new RuntimeException("no BusMaster for dsAddr:" + dsAddr);
        }

        ReadPowerSupplyCmd cmd = bm.queryReadPowerSupplyCmd(new DSAddress(dsAddr), ("true".equals(log) ? new Logger() : null));
        cmd.execute();

        Map<String, String> result = new HashMap<String, String>();
        if (cmd.getOptLogger() != null) {
            result.put("log", cmd.getOptLogger().toString());
        }

        result.put("result", cmd.getResult().name());

        if (cmd.getResult() == ReadPowerSupplyCmd.Result.success) {
            result.put("isParasitic", String.valueOf(cmd.getResultIsParasitic()));
        }

        return result;
    }

    @RequestMapping(value = "/readScratchPadCmd/{dsAddr}/{rCount}", method = RequestMethod.POST)
    public Map<String, String> readScratchPadCmd(@PathVariable(value = "dsAddr") String dsAddr,
                                                 @PathVariable(value = "rCount") Long rCount,
                                                 @RequestParam(value = "log", required = false, defaultValue = "false") String log) {
        // must specify a DSAddress
        // must specify a read count
        // optional logging request
        return null;
    }

    @RequestMapping(value = "/convertTCmd/{dsAddr}", method = RequestMethod.POST)
    public Map<String, String> convertTCmd(@PathVariable(value = "dsAddr") String dsAddr,
                                           @RequestParam(value = "log", required = false, defaultValue = "false") String log) {
        // must specify a DSAddress
        // optional logging request
        return null;
    }

}
