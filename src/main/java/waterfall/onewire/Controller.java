package waterfall.onewire;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import waterfall.onewire.busmaster.*;

import java.util.*;

/**
 * Created by dwaterfa on 6/12/16.
 */
@RestController
@RequestMapping(value = "/httpbusmaster")
public class Controller {
    BusMasterManager busMasterManager;

    @Autowired
    public Controller(BusMasterManager busMasterManager) {
        this.busMasterManager = busMasterManager;
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
    public Collection<Map<String, String>> status() {
        // return list of busmasters with their status started/stopped
        String[] bmIdents = busMasterManager.getBusMasterIdents();

        Base64.Encoder encoder = Base64.getEncoder();

        ArrayList<Map<String, String>> result = new ArrayList<Map<String, String>>();
        for (String bmIdent : bmIdents) {
            HashMap<String, String> data = new HashMap<String, String>();
            BusMaster bm = busMasterManager.getBusMasterByIdent(bmIdent);
            data.put("ident", bmIdent);
            data.put("name", bm.getName());
            data.put("started", Boolean.toString(bm.getIsStarted()));
            result.add(data);
        }

        return result;
    }

    /*
    public class myStartCmdResult extends HashMap<String, String> {
        String error;
        String result;
        String log;

        public myStartCmdResult(final String error) {
            this.error = error;
        }

        public myStartCmdResult(final Map<String, String> map) {
            String e = map.get("error");
            if (e != null) {
                error = e;
            }
            String r = map.get("result");
            if (r != null) {
                result = r;
            }
            String l = map.get("log");
            if (l != null) {
                log = l;
            }
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public String getLog() {
            return log;
        }

        public void setLog(String log) {
            this.log = log;
        }

        public static myStartCmdResult paramErrorUnknownBMIdent(String unknown_bmIdent) {
            return new myStartCmdResult("Unknown bmIdent:" + unknown_bmIdent);
        }

        public static myStartCmdResult paramErrorBadLog(String bad_log) {
            return new myStartCmdResult("Invalid log, must be true/false. Was:" + bad_log);
        }

    }

    @RequestMapping(value = "/startCmd_new/{bmIdent}", method = RequestMethod.POST)
    public myStartCmdResult startCmd_new(@PathVariable(value = "bmIdent") String bmIdent,
                                        @RequestParam(value = "log", required = false, defaultValue = "false") String log) {
        Map<String, String> result = startCmd(bmIdent, log);

        return new myStartCmdResult(result);
    }
    */

    @RequestMapping(value = "/startCmd/{bmIdent}", method = RequestMethod.POST)
    public Map<String, String> startCmd(@PathVariable(value = "bmIdent") String bmIdent,
                                        @RequestParam(value = "log", required = false, defaultValue = "false") String log) {

        Map<String, String> result = new HashMap<String, String>();

        BusMaster bm = busMasterManager.getBusMasterByIdent(bmIdent);

        if (bm == null) {
            result.put("error", "Unknown bmIdent:" + bmIdent);
            return result;
        }

        boolean _log = false;
        if (log != null) {
            boolean hasTrue = "true".equals(log);
            boolean hasFalse = ((!hasTrue) && ("false".equals(log)));

            if (!hasTrue && !hasFalse) {
                result.put("error", "log is optional and must be true/false");
                return result;
            }

            _log = hasTrue;
        }

        StartBusCmd cmd = bm.queryStartBusCmd(_log ? new Logger() : null);
        cmd.execute();

        if (cmd.getOptLogger() != null) {
            result.put("log", cmd.getOptLogger().toString());
        }

        result.put("result", cmd.getResult().getClass().getName() + "." + cmd.getResult().name());
        return result;
    }

    @RequestMapping(value = "/stopCmd/{bmIdent}", method = RequestMethod.POST)
    public Map<String, String> stopCmd(@PathVariable(value = "bmIdent") String bmIdent,
                                       @RequestParam(value = "log", required = false, defaultValue = "false") String log) {

        Map<String, String> result = new HashMap<String, String>();

        BusMaster bm = busMasterManager.getBusMasterByIdent(bmIdent);

        if (bm == null) {
            result.put("error", "Unknown bmIdent:" + bmIdent);
            return result;
        }

        boolean _log = false;
        if (log != null) {
            boolean hasTrue = "true".equals(log);
            boolean hasFalse = ((!hasTrue) && ("false".equals(log)));

            if (!hasTrue && !hasFalse) {
                result.put("error", "log is optional and must be true/false");
                return result;
            }

            _log = hasTrue;
        }

        StopBusCmd cmd = bm.queryStopBusCmd("true".equals(log) ? new Logger() : null);
        cmd.execute();

        if (cmd.getOptLogger() != null) {
            result.put("log", cmd.getOptLogger().toString());
        }

        result.put("result", cmd.getResult().name());

        return result;
    }

    @RequestMapping(value = "/searchCmd/{bmIdent}", method = RequestMethod.POST)
    public Map<String, String> searchCmd(@PathVariable(value = "bmIdent") String bmIdent,
                                         @RequestParam(value = "byAlarm", required = false, defaultValue = "false") String byAlarm,
                                         @RequestParam(value = "byFamilyCode", required = false, defaultValue = "") String byFamilyCode,
                                         @RequestParam(value = "log", required = false, defaultValue = "false") String log) {

        Map<String, String> result = new HashMap<String, String>();

        BusMaster bm = busMasterManager.getBusMasterByIdent(bmIdent);

        if (bm == null) {
            result.put("error", "Unknown bmIdent:" + bmIdent);
            return result;
        }

        boolean _log = false;
        if (log != null) {
            boolean hasTrue = "true".equals(log);
            boolean hasFalse = ((!hasTrue) && ("false".equals(log)));

            if (!hasTrue && !hasFalse) {
                result.put("error", "log is optional and must be true/false");
                return result;
            }

            _log = hasTrue;
        }

        boolean _byAlarm = false;
        if (byAlarm != null) {
            boolean hasTrue = "true".equals(byAlarm);
            boolean hasFalse = ((!hasTrue) && ("false".equals(byAlarm)));

            if ((!hasTrue) && (!hasFalse)) {
                result.put("error", "byAlarm is optional must be true/false");
                return result;
            }
            _byAlarm = hasTrue;
        }

        Short familyCode = null;
        if (!byFamilyCode.isEmpty()) {
            try {
                familyCode = Short.valueOf(byFamilyCode);
            } catch (NumberFormatException e) {
                result.put("error", "invalid byFamilyCode:" + byFamilyCode);
                return result;
            }

            if ((familyCode < 0) || (familyCode > 255)) {
                result.put("error", "0 <= byFamilyCode <= 255 invalid:" + familyCode);
                return result;
            }
        }

        if ((_byAlarm) && (familyCode != null)) {
            result.put("error", "cannot have both byAlarm and byFamilyCode");
            return result;
        }

        SearchBusCmd cmd = null;

        if (_byAlarm) {
            cmd = bm.querySearchBusByAlarmCmd(_log ? new Logger() : null);
        } else if (familyCode != null) {
            cmd = bm.querySearchBusByFamilyCmd(familyCode, _log ? new Logger() : null);
        } else {
            cmd = bm.querySearchBusCmd(_log ? new Logger() : null);
        }

        cmd.execute();

        if (cmd.getOptLogger() != null) {
            result.put("log", cmd.getOptLogger().toString());
        }

        result.put("result", cmd.getResult().name());

        if (cmd.getResult() == SearchBusCmd.Result.success) {
            StringBuffer sb = new StringBuffer();
            boolean updateDM = ((!cmd.isByAlarm()) && (!cmd.isByFamilyCode()));

            for (String addrAsHex : cmd.getResultList()) {
                if (updateDM) {
                    busMasterManager.addDevice(addrAsHex, bm);
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
        return busMasterManager.getDevices();
    }

    @RequestMapping(value = "/readPowerSupplyCmd/{dsAddr}", method = RequestMethod.POST)
    public Map<String, String> readPowerSupplyCmd(@PathVariable(value = "dsAddr") String dsAddr,
                                                  @RequestParam(value = "log", required = false, defaultValue = "false") String log) {

        Map<String, String> result = new HashMap<String, String>();

        if ((dsAddr == null) || (!DSAddress.isValid(dsAddr))) {
            result.put("error", "invalid dsAddr:" + dsAddr);
            return result;
        }

        DSAddress _dsAddr = new DSAddress(dsAddr);

        boolean _log = false;
        if (log != null) {
            boolean hasTrue = "true".equals(log);
            boolean hasFalse = ((!hasTrue) && ("false".equals(log)));

            if (!hasTrue && !hasFalse) {
                result.put("error", "log is optional and must be true/false");
                return result;
            }

            _log = hasTrue;
        }

        BusMaster bm = busMasterManager.getBusMasterForDevice(dsAddr);
        if (bm == null) {
            result.put("error", "no BusMaster for dsAddr:" + dsAddr);
        }

        ReadPowerSupplyCmd cmd = bm.queryReadPowerSupplyCmd(_dsAddr, _log ? new Logger() : null);
        cmd.execute();

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
