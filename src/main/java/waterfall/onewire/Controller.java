package waterfall.onewire;

import com.dalsemi.onewire.utils.Convert;
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

    enum Errors {
        Unknown_bmIdent,
        BM_not_started,
        Bad_parm_log_not_true_or_false,
        Bad_parm_byAlarm_not_true_false,
        Bad_parm_only_one_byAlarm_or_byFamilyCode_allowed,
        Bad_parm_byFamilyCode_not_a_number,
        Bad_parm_byFamilyCode_must_be_unsigned_byte,
        No_BM_for_dsAddr,
        Invalid_dsAddr,
        Invalid_rCount
    }

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

    @RequestMapping(value = "/startCmd/{bmIdent}", method = RequestMethod.POST)
    public Map<String, Object> startCmd(@PathVariable String bmIdent,
                                        @RequestParam(value = "log", required = false, defaultValue = "false") String parmLog) {

        Map<String, String> result = new HashMap<String, String>();

        BusMaster bm = busMasterManager.getBusMasterByIdent(bmIdent);

        if (bm == null) {
            return buildErrorResult(Errors.Unknown_bmIdent);
        }

        boolean doLog;
        if ("true".equals(parmLog)) {
            doLog = true;
        } else if ("false".equals(parmLog)) {
            doLog = false;
        } else {
            return buildErrorResult(Errors.Bad_parm_log_not_true_or_false);
        }

        StartBusCmd cmd = bm.queryStartBusCmd(doLog);
        cmd.execute();

        return buildCmdExecuteResult((Logger)cmd, cmd.getResult());
    }

    @RequestMapping(value = "/stopCmd/{bmIdent}", method = RequestMethod.POST)
    public Map<String, Object> stopCmd(@PathVariable String bmIdent,
                                       @RequestParam(value = "log", required = false, defaultValue = "false") String parmLog) {

        Map<String, String> result = new HashMap<String, String>();

        BusMaster bm = busMasterManager.getBusMasterByIdent(bmIdent);

        if (bm == null) {
            return buildErrorResult(Errors.Unknown_bmIdent);
        }

        boolean log;
        if ("true".equals(parmLog)) {
            log = true;
        } else if ("false".equals(parmLog)) {
            log = false;
        } else {
            return buildErrorResult(Errors.Bad_parm_log_not_true_or_false);
        }

        StopBusCmd cmd = bm.queryStopBusCmd(log);
        cmd.execute();

        return buildCmdExecuteResult((Logger)cmd, cmd.getResult());
    }

    @RequestMapping(value = "/searchCmd/{bmIdent}", method = RequestMethod.POST)
    public Map<String, Object> searchCmd(@PathVariable(value = "bmIdent") String bmIdent,
                                         @RequestParam(value = "byAlarm", required = false) String parmByAlarm,
                                         @RequestParam(value = "byFamilyCode", required = false) String parmByFamilyCode,
                                         @RequestParam(value = "log", required = false, defaultValue = "false") String parmLog) {

        BusMaster bm = busMasterManager.getBusMasterByIdent(bmIdent);

        if (bm == null) {
            return buildErrorResult(Errors.Unknown_bmIdent);
        }

        boolean doLog;
        if ("true".equals(parmLog)) {
            doLog = true;
        } else if ("false".equals(parmLog)) {
            doLog = false;
        } else {
            return buildErrorResult(Errors.Bad_parm_log_not_true_or_false);
        }

        SearchBusCmd cmd = null;

        if (parmByFamilyCode != null) {
            if (parmByAlarm != null) {
                return buildErrorResult(Errors.Bad_parm_only_one_byAlarm_or_byFamilyCode_allowed);
            }
            if (parmByFamilyCode.isEmpty()) {
                return buildErrorResult(Errors.Bad_parm_byFamilyCode_not_a_number);
            }

            Short familyCode = null;

            try {
                familyCode = Short.valueOf(parmByFamilyCode);
            } catch (NumberFormatException e) {
                return buildErrorResult(Errors.Bad_parm_byFamilyCode_not_a_number);
            }

            if ((familyCode < 0) || (familyCode > 255)) {
                return buildErrorResult(Errors.Bad_parm_byFamilyCode_must_be_unsigned_byte);
            }

            cmd = bm.querySearchBusByFamilyCmd(familyCode, doLog);
        } else if ((parmByAlarm == null) || ("false".equals(parmByAlarm))) {
            cmd = bm.querySearchBusCmd(doLog);
        } else if ("true".equals(parmByAlarm)) {
            cmd = bm.querySearchBusByAlarmCmd(doLog);
        } else {
            return buildErrorResult(Errors.Bad_parm_byAlarm_not_true_false);
        }

        cmd.execute();

        Map<String, Object> result = buildCmdExecuteResult((Logger)cmd, cmd.getResult());

        if (cmd.getResult() == SearchBusCmd.Result.success) {
            result.put("resultList", cmd.getResultList());

            if ((!cmd.isByAlarm()) && (!cmd.isByFamilyCode())) {
                for (String addrAsHex : cmd.getResultList()) {
                    busMasterManager.addDevice(addrAsHex, bm);
                }
            }
        }

        return result;
    }

    @RequestMapping(value = "/devices", method = RequestMethod.POST)
    public String[] devices() {
        // return list of devices gathered from the last full search
        return busMasterManager.getDevices();
    }

    @RequestMapping(value = "/readPowerSupplyCmd/{dsAddr}", method = RequestMethod.POST)
    public Map<String, Object> readPowerSupplyCmd(@PathVariable String dsAddr,
                                                  @RequestParam(value = "log", required = false, defaultValue = "false") String parmLog) {

        if ((dsAddr == null) || (!DSAddress.isValid(dsAddr))) {
            return buildErrorResult(Errors.Invalid_dsAddr);
        }

        DSAddress _dsAddr = new DSAddress(dsAddr);

        boolean log;
        if ("true".equals(parmLog)) {
            log = true;
        } else if ("false".equals(parmLog)) {
            log = false;
        } else {
            return buildErrorResult(Errors.Bad_parm_log_not_true_or_false);
        }

        BusMaster bm = busMasterManager.getBusMasterForDevice(dsAddr);
        if (bm == null) {
            return buildErrorResult(Errors.No_BM_for_dsAddr);
        }

        ReadPowerSupplyCmd cmd = bm.queryReadPowerSupplyCmd(_dsAddr, log);
        cmd.execute();

        Map<String, Object> result = buildCmdExecuteResult((Logger)cmd, cmd.getResult());

        if (cmd.getResult() == ReadPowerSupplyCmd.Result.success) {
            result.put("isParasitic", String.valueOf(cmd.getResultIsParasitic()));
        }

        return result;
    }

    @RequestMapping(value = "/readScratchPadCmd/{dsAddr}/{rCount}", method = RequestMethod.POST)
    public Map<String, Object> readScratchPadCmd(@PathVariable(value = "dsAddr") String dsAddr,
                                                 @PathVariable(value = "rCount") Long rCount,
                                                 @RequestParam(value = "log", required = false, defaultValue = "false") String parmLog) {
        if ((dsAddr == null) || (!DSAddress.isValid(dsAddr))) {
            return buildErrorResult(Errors.Invalid_dsAddr);
        }

        DSAddress _dsAddr = new DSAddress(dsAddr);

        if ((rCount == null) || (rCount < 0) || (rCount > 254)) {
            return buildErrorResult(Errors.Invalid_rCount);
        }

        boolean doLog;
        if ("true".equals(parmLog)) {
            doLog = true;
        } else if ("false".equals(parmLog)) {
            doLog = false;
        } else {
            return buildErrorResult(Errors.Bad_parm_log_not_true_or_false);
        }

        BusMaster bm = busMasterManager.getBusMasterForDevice(dsAddr);
        if (bm == null) {
            return buildErrorResult(Errors.No_BM_for_dsAddr);
        }

        ReadScratchpadCmd cmd = bm.queryReadScratchpadCmd(_dsAddr, rCount.shortValue(), doLog);
        cmd.execute();

        Map<String, Object> result = buildCmdExecuteResult((Logger)cmd, cmd.getResult());

        if (cmd.getResult() == ReadScratchpadCmd.Result.success) {
            result.put("dataAsHex", Convert.toHexString(cmd.getResultData()));
        }

        return result;
    }

    @RequestMapping(value = "/convertTCmd/{dsAddr}", method = RequestMethod.POST)
    public Map<String, Object> convertTCmd(@PathVariable(value = "dsAddr") String dsAddr,
                                           @RequestParam(value = "log", required = false, defaultValue = "false") String parmLog) {
        if ((dsAddr == null) || (!DSAddress.isValid(dsAddr))) {
            return buildErrorResult(Errors.Invalid_dsAddr);
        }

        DSAddress _dsAddr = new DSAddress(dsAddr);

        boolean log;
        if ("true".equals(parmLog)) {
            log = true;
        } else if ("false".equals(parmLog)) {
            log = false;
        } else {
            return buildErrorResult(Errors.Bad_parm_log_not_true_or_false);
        }

        BusMaster bm = busMasterManager.getBusMasterForDevice(dsAddr);
        if (bm == null) {
            return buildErrorResult(Errors.No_BM_for_dsAddr);
        }

        ConvertTCmd cmd = bm.queryConvertTCmd(_dsAddr, log);
        cmd.execute();

        Map<String, Object> result = buildCmdExecuteResult((Logger)cmd, cmd.getResult());

        return result;
    }

    // Result builders
    private static final String RESULT_KEY_ERROR = "error";
    private static final String RESULT_KEY_LOG = "log";
    private static final String RESULT_KEY_RESULT = "result";

    public static Map<String, Object> buildErrorResult(Errors error) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(RESULT_KEY_ERROR, error.getClass().getCanonicalName() + "." + error.name());
        return result;
    }

    public static Map<String, Object> buildCmdExecuteResult(final Logger optLogger, final Enum cmdResult) {
        Map<String, Object> result = new HashMap<String, Object>();

        if ((optLogger != null) && (optLogger.getLogSize() > 0)) {
            String[] logs = new String[optLogger.getLogSize()];
            int i = 0;
            for (Iterator<String> iter = optLogger.getLogIter(); iter.hasNext(); ) {
                logs[i++] = iter.next();
            }
            result.put(RESULT_KEY_LOG, logs);
        }

        if (cmdResult != null) {
            result.put(RESULT_KEY_RESULT, cmdResult.getClass().getCanonicalName() + "." + cmdResult.name());
        } else {
            result.put(RESULT_KEY_RESULT, null);
        }

        return result;
    }

}

