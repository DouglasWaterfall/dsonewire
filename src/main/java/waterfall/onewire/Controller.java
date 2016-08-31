package waterfall.onewire;

import com.dalsemi.onewire.utils.Convert;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import waterfall.onewire.HttpClient.BaseCmdResult;
import waterfall.onewire.HttpClient.StatusCmdResult;
import waterfall.onewire.busmaster.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * Created by dwaterfa on 6/12/16.
 */
@RestController
@RequestMapping(value = "/httpbusmaster")
public class Controller {
    HashMap<String, BusMaster> bmMap;
    String currentAuthenticationValue;

    public Controller() {
        this.bmMap = new HashMap<String, BusMaster>();
        this.currentAuthenticationValue = null;
    }

    public void addBusMaster(BusMaster bm) {
        Base64.Encoder encoder = Base64.getUrlEncoder();

        String bmIdent = encoder.encodeToString(bm.getName().getBytes());

        if (!bmMap.containsKey(bmIdent)) {
            System.out.println("Adding busMaster" + bm.getName());
            bmMap.put(bmIdent, bm);
        }
        else if (bmMap.get(bmIdent) == bm) {
            System.err.println("Duplicate add of busMaster" + bm.getName());
        }
        else {
            System.err.println("name encoding collision of busMaster:" + bm.getName() + " and " + bmMap.get(bmIdent).getName());
        }
    }
    
    /*
    @RequestMapping(value = "",
                    method = RequestMethod.POST,
                    produces = {MediaType.APPLICATION_JSON_VALUE},
                    consumes = {MediaType.APPLICATION_JSON_VALUE})
    public Token create(Principal principal, @RequestBody(required = true) Token token) throws ServiceException {
        ...
    */

    @RequestMapping(value = "/bmList", method = RequestMethod.POST)
    public waterfall.onewire.HttpClient.ListBusMastersCmdResult listBusMasters(
            @RequestParam(value = "log", required = false, defaultValue = "false") String parmLog) {

        boolean doLog;
        if ("true".equals(parmLog)) {
            doLog = true;
        } else if ("false".equals(parmLog)) {
            doLog = false;
        } else {
            return new waterfall.onewire.HttpClient.ListBusMastersCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_log_not_true_or_false);
        }

        return new waterfall.onewire.HttpClient.ListBusMastersCmdResult(getBusMasterIdents(), null);
    }

    @RequestMapping(value = "/busStatusCmd/{bmIdent}", method = RequestMethod.POST)
    public waterfall.onewire.HttpClient.StatusCmdResult busStatusCmd(@PathVariable String bmIdent) {

        BusMaster bm;
        if ((bmIdent == null) || ((bm = getBusMasterByIdent(bmIdent)) == null)) {
            return new waterfall.onewire.HttpClient.StatusCmdResult(BaseCmdResult.ControllerErrors.Unknown_bmIdent);
        }

        if (currentAuthenticationValue == null) {
            currentAuthenticationValue = Base64.getUrlEncoder().encodeToString(new Date(System.currentTimeMillis()).toString().getBytes());
            System.out.println("status " + bmIdent + " currentAuthenticationValue:" + currentAuthenticationValue);
        }

        return new StatusCmdResult(bmIdent, bm, currentAuthenticationValue);
    }

    @RequestMapping(value = "/startBusCmd/{bmIdent}", method = RequestMethod.POST)
    public waterfall.onewire.HttpClient.StartBusCmdResult startBusCmd(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                                      @PathVariable(value = "bmIdent") String bmIdent,
                                                                      @RequestParam(value = "log", required = false, defaultValue = "false") String parmLog) {
        checkAuthenticationHeader(authorization);

        BusMaster bm;
        if ((bmIdent == null) || ((bm = getBusMasterByIdent(bmIdent)) == null)) {
            return new waterfall.onewire.HttpClient.StartBusCmdResult(BaseCmdResult.ControllerErrors.Unknown_bmIdent);
        }

        boolean doLog;
        if ("true".equals(parmLog)) {
            doLog = true;
        } else if ("false".equals(parmLog)) {
            doLog = false;
        } else {
            return new waterfall.onewire.HttpClient.StartBusCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_log_not_true_or_false);
        }

        StartBusCmd cmd = bm.queryStartBusCmd(doLog);
        cmd.execute();

        return new waterfall.onewire.HttpClient.StartBusCmdResult(cmd);
    }

    @RequestMapping(value = "/stopBusCmd/{bmIdent}", method = RequestMethod.POST)
    public waterfall.onewire.HttpClient.StopBusCmdResult stopBusCmd(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                                    @PathVariable(value = "bmIdent") String bmIdent,
                                                                    @RequestParam(value = "log", required = false, defaultValue = "false") String parmLog) {
        checkAuthenticationHeader(authorization);

        BusMaster bm;
        if ((bmIdent == null) || ((bm = getBusMasterByIdent(bmIdent)) == null)) {
            return new waterfall.onewire.HttpClient.StopBusCmdResult(BaseCmdResult.ControllerErrors.Unknown_bmIdent);
        }

        boolean log;
        if ("true".equals(parmLog)) {
            log = true;
        } else if ("false".equals(parmLog)) {
            log = false;
        } else {
            return new waterfall.onewire.HttpClient.StopBusCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_log_not_true_or_false);
        }

        StopBusCmd cmd = bm.queryStopBusCmd(log);
        cmd.execute();

        return new waterfall.onewire.HttpClient.StopBusCmdResult(cmd);
    }

    @RequestMapping(value = "/searchBusCmd/{bmIdent}", method = RequestMethod.POST)
    public waterfall.onewire.HttpClient.SearchBusCmdResult searchBusCmd(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                                        @PathVariable(value = "bmIdent") String bmIdent,
                                                                        @RequestParam(value = "byAlarm", required = false) String parmByAlarm,
                                                                        @RequestParam(value = "byFamilyCode", required = false) String parmByFamilyCode,
                                                                        @RequestParam(value = "log", required = false, defaultValue = "false") String parmLog) {
        checkAuthenticationHeader(authorization);

        BusMaster bm;
        if ((bmIdent == null) || ((bm = getBusMasterByIdent(bmIdent)) == null)) {
            return new waterfall.onewire.HttpClient.SearchBusCmdResult(BaseCmdResult.ControllerErrors.Unknown_bmIdent);
        }

        boolean doLog;
        if ("true".equals(parmLog)) {
            doLog = true;
        } else if ("false".equals(parmLog)) {
            doLog = false;
        } else {
            return new waterfall.onewire.HttpClient.SearchBusCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_log_not_true_or_false);
        }

        SearchBusCmd cmd = null;

        if (parmByFamilyCode != null) {
            if (parmByAlarm != null) {
                return new waterfall.onewire.HttpClient.SearchBusCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_only_one_byAlarm_or_byFamilyCode_allowed);
            }
            if (parmByFamilyCode.isEmpty()) {
                return new waterfall.onewire.HttpClient.SearchBusCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_byFamilyCode_not_a_number);
            }

            Short familyCode = null;

            try {
                familyCode = Short.valueOf(parmByFamilyCode);
            } catch (NumberFormatException e) {
                return new waterfall.onewire.HttpClient.SearchBusCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_byFamilyCode_not_a_number);
            }

            if ((familyCode < 0) || (familyCode > 255)) {
                return new waterfall.onewire.HttpClient.SearchBusCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_byFamilyCode_must_be_unsigned_byte);
            }

            cmd = bm.querySearchBusByFamilyCmd(familyCode, doLog);
        } else if ((parmByAlarm == null) || ("false".equals(parmByAlarm))) {
            cmd = bm.querySearchBusCmd(doLog);
        } else if ("true".equals(parmByAlarm)) {
            cmd = bm.querySearchBusByAlarmCmd(doLog);
        } else {
            return new waterfall.onewire.HttpClient.SearchBusCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_byAlarm_not_true_false);
        }

        cmd.execute();

        return new waterfall.onewire.HttpClient.SearchBusCmdResult(cmd);
    }

    @RequestMapping(value = "/readPowerSupplyCmd/{bmIdent}/{dsAddr}", method = RequestMethod.POST)
    public waterfall.onewire.HttpClient.ReadPowerSupplyCmdResult readPowerSupplyCmd(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                                                    @PathVariable(value = "bmIdent") String bmIdent,
                                                                                    @PathVariable(value = "dsAddr") String dsAddr,
                                                                                    @RequestParam(value = "log", required = false, defaultValue = "false") String parmLog) {
        checkAuthenticationHeader(authorization);

        BusMaster bm;
        if ((bmIdent == null) || ((bm = getBusMasterByIdent(bmIdent)) == null)) {
            return new waterfall.onewire.HttpClient.ReadPowerSupplyCmdResult(BaseCmdResult.ControllerErrors.Unknown_bmIdent);
        }

        if ((dsAddr == null) || (!DSAddress.isValid(dsAddr))) {
            return new waterfall.onewire.HttpClient.ReadPowerSupplyCmdResult(BaseCmdResult.ControllerErrors.Invalid_dsAddr);
        }

        boolean log;
        if ("true".equals(parmLog)) {
            log = true;
        } else if ("false".equals(parmLog)) {
            log = false;
        } else {
            return new waterfall.onewire.HttpClient.ReadPowerSupplyCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_log_not_true_or_false);
        }

        ReadPowerSupplyCmd cmd = bm.queryReadPowerSupplyCmd(new DSAddress(dsAddr), log);
        cmd.execute();

        return new waterfall.onewire.HttpClient.ReadPowerSupplyCmdResult(cmd);
    }

    @RequestMapping(value = "/readScratchpadCmd/{bmIdent}/{dsAddr}/{rCount}", method = RequestMethod.POST)
    public waterfall.onewire.HttpClient.ReadScratchpadCmdResult readScratchPadCmd(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                                                  @PathVariable(value = "bmIdent") String bmIdent,
                                                                                  @PathVariable(value = "dsAddr") String dsAddr,
                                                                                  @PathVariable(value = "rCount") Long rCount,
                                                                                  @RequestParam(value = "log", required = false, defaultValue = "false") String parmLog) {
        checkAuthenticationHeader(authorization);

        BusMaster bm;
        if ((bmIdent == null) || ((bm = getBusMasterByIdent(bmIdent)) == null)) {
            return new waterfall.onewire.HttpClient.ReadScratchpadCmdResult(BaseCmdResult.ControllerErrors.Unknown_bmIdent);
        }

        if ((dsAddr == null) || (!DSAddress.isValid(dsAddr))) {
            return new waterfall.onewire.HttpClient.ReadScratchpadCmdResult(BaseCmdResult.ControllerErrors.Invalid_dsAddr);
        }

        if ((rCount == null) || (rCount < 0) || (rCount > 254)) {
            return new waterfall.onewire.HttpClient.ReadScratchpadCmdResult(BaseCmdResult.ControllerErrors.Invalid_rCount);
        }

        boolean doLog;
        if ("true".equals(parmLog)) {
            doLog = true;
        } else if ("false".equals(parmLog)) {
            doLog = false;
        } else {
            return new waterfall.onewire.HttpClient.ReadScratchpadCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_log_not_true_or_false);
        }

        ReadScratchpadCmd cmd = bm.queryReadScratchpadCmd(new DSAddress(dsAddr), rCount.shortValue(), doLog);
        cmd.execute();

        if (cmd.getResult() == ReadScratchpadCmd.Result.success) {
            Float tempF = null;
            Float tempC = null;

            if ((cmd.getAddress().getFamilyCode() == 0x28) && (cmd.getRequestByteCount() >= 2)) {
                int msb = (int) cmd.getResultData()[1] & 0xff;
                int lsb = (int) cmd.getResultData()[0] & 0xff;

                tempC = ((float) ((msb << 8) + lsb) / (float) 16.0);
                tempF = (float) Convert.toFahrenheit(tempC);
            }
            return new waterfall.onewire.HttpClient.ReadScratchpadCmdResult(cmd, tempF, tempC);
        }

        return new waterfall.onewire.HttpClient.ReadScratchpadCmdResult(cmd, null, null);
    }

    @RequestMapping(value = "/convertTCmd/{bmIdent}/{dsAddr}", method = RequestMethod.POST)
    public waterfall.onewire.HttpClient.ConvertTCmdResult convertTCmd(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                                      @PathVariable(value = "bmIdent") String bmIdent,
                                                                      @PathVariable(value = "dsAddr") String dsAddr,
                                                                      @RequestParam(value = "log", required = false, defaultValue = "false") String parmLog) {
        checkAuthenticationHeader(authorization);

        BusMaster bm;
        if ((bmIdent == null) || ((bm = getBusMasterByIdent(bmIdent)) == null)) {
            return new waterfall.onewire.HttpClient.ConvertTCmdResult(BaseCmdResult.ControllerErrors.Unknown_bmIdent);
        }

        if ((dsAddr == null) || (!DSAddress.isValid(dsAddr))) {
            return new waterfall.onewire.HttpClient.ConvertTCmdResult(BaseCmdResult.ControllerErrors.Invalid_dsAddr);
        }

        boolean log;
        if ("true".equals(parmLog)) {
            log = true;
        } else if ("false".equals(parmLog)) {
            log = false;
        } else {
            return new waterfall.onewire.HttpClient.ConvertTCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_log_not_true_or_false);
        }

        ConvertTCmd cmd = bm.queryConvertTCmd(new DSAddress(dsAddr), log);
        cmd.execute();

        return new waterfall.onewire.HttpClient.ConvertTCmdResult(cmd);
    }

    private BusMaster getBusMasterByIdent(String bmIdent) {
        return bmMap.get(bmIdent);
    }

    private ArrayList<String> getBusMasterIdents() {
        ArrayList<String> list = new ArrayList<String>();

        for (Iterator<String> iter = bmMap.keySet().iterator(); iter.hasNext(); ) {
            list.add(iter.next());
        }

        return list;
    }

    public class AuthException extends RuntimeException {
        public AuthException() {
            super();
        }
    }

    @ExceptionHandler(AuthException.class)
    void handleBadRequests(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.GONE.value(), "Authorization header mismatch");
    }

    private void checkAuthenticationHeader(String authentication) throws AuthException {
        if ((authentication == null) ||
                (currentAuthenticationValue == null) ||
                (!authentication.equals(currentAuthenticationValue))) {
            throw new AuthException();
        }
    }

}

