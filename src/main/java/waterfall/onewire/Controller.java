package waterfall.onewire;

import com.dalsemi.onewire.utils.Convert;
import org.springframework.beans.factory.annotation.Autowired;
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
    BusMasterManager busMasterManager;
    String currentAuthenticationValue;

    public Controller() {
        this.busMasterManager = new BusMasterManager();
        this.currentAuthenticationValue = null;
    }

    public void addBusMaster(BusMaster bm) {
        busMasterManager.add(bm);
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
    public List<String> bms() {
        return busMasterManager.getBusMasterIdents();
    }

    @RequestMapping(value = "/busStatusCmd/{bmIdent}", method = RequestMethod.POST)
    public waterfall.onewire.HttpClient.StatusCmdResult busStatusCmd(@PathVariable String bmIdent) {

        BusMaster bm = busMasterManager.getBusMasterByIdent(bmIdent);

        if (bm == null) {
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
                                                                   @PathVariable String bmIdent,
                                                                   @RequestParam(value = "log", required = false, defaultValue = "false") String parmLog) {
        checkAuthenticationHeader(authorization);

        BusMaster bm = busMasterManager.getBusMasterByIdent(bmIdent);

        if (bm == null) {
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
                                                                 @PathVariable String bmIdent,
                                                                 @RequestParam(value = "log", required = false, defaultValue = "false") String parmLog) {
        checkAuthenticationHeader(authorization);

        Map<String, String> result = new HashMap<String, String>();

        BusMaster bm = busMasterManager.getBusMasterByIdent(bmIdent);

        if (bm == null) {
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

        BusMaster bm = busMasterManager.getBusMasterByIdent(bmIdent);

        if (bm == null) {
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

        if ((cmd.getResult() == SearchBusCmd.Result.success) && (!cmd.isByAlarm()) && (!cmd.isByFamilyCode())) {
            for (String addrAsHex : cmd.getResultList()) {
                busMasterManager.addDevice(addrAsHex, bm);
            }
        }

        return new waterfall.onewire.HttpClient.SearchBusCmdResult(cmd);
    }

    @RequestMapping(value = "/readPowerSupplyCmd/{dsAddr}", method = RequestMethod.POST)
    public waterfall.onewire.HttpClient.ReadPowerSupplyCmdResult readPowerSupplyCmd(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                                                    @PathVariable String dsAddr,
                                                                                    @RequestParam(value = "log", required = false, defaultValue = "false") String parmLog) {
        checkAuthenticationHeader(authorization);

        if ((dsAddr == null) || (!DSAddress.isValid(dsAddr))) {
            return new waterfall.onewire.HttpClient.ReadPowerSupplyCmdResult(BaseCmdResult.ControllerErrors.Invalid_dsAddr);
        }

        DSAddress _dsAddr = new DSAddress(dsAddr);

        boolean log;
        if ("true".equals(parmLog)) {
            log = true;
        } else if ("false".equals(parmLog)) {
            log = false;
        } else {
            return new waterfall.onewire.HttpClient.ReadPowerSupplyCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_log_not_true_or_false);
        }

        BusMaster bm = busMasterManager.getBusMasterForDevice(dsAddr);
        if (bm == null) {
            return new waterfall.onewire.HttpClient.ReadPowerSupplyCmdResult(BaseCmdResult.ControllerErrors.No_BM_for_dsAddr);
        }

        ReadPowerSupplyCmd cmd = bm.queryReadPowerSupplyCmd(_dsAddr, log);
        cmd.execute();

        return new waterfall.onewire.HttpClient.ReadPowerSupplyCmdResult(cmd);
    }

    @RequestMapping(value = "/readScratchpadCmd/{dsAddr}/{rCount}", method = RequestMethod.POST)
    public waterfall.onewire.HttpClient.ReadScratchpadCmdResult readScratchPadCmd(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                                                  @PathVariable(value = "dsAddr") String dsAddr,
                                                                                  @PathVariable(value = "rCount") Long rCount,
                                                                                  @RequestParam(value = "log", required = false, defaultValue = "false") String parmLog) {
        checkAuthenticationHeader(authorization);

        if ((dsAddr == null) || (!DSAddress.isValid(dsAddr))) {
            return new waterfall.onewire.HttpClient.ReadScratchpadCmdResult(BaseCmdResult.ControllerErrors.Invalid_dsAddr);
        }

        DSAddress _dsAddr = new DSAddress(dsAddr);

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

        BusMaster bm = busMasterManager.getBusMasterForDevice(dsAddr);
        if (bm == null) {
            return new waterfall.onewire.HttpClient.ReadScratchpadCmdResult(BaseCmdResult.ControllerErrors.No_BM_for_dsAddr);
        }

        ReadScratchpadCmd cmd = bm.queryReadScratchpadCmd(_dsAddr, rCount.shortValue(), doLog);
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

    @RequestMapping(value = "/convertTCmd/{dsAddr}", method = RequestMethod.POST)
    public waterfall.onewire.HttpClient.ConvertTCmdResult convertTCmd(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                                      @PathVariable(value = "dsAddr") String dsAddr,
                                                                      @RequestParam(value = "log", required = false, defaultValue = "false") String parmLog) {
        checkAuthenticationHeader(authorization);

        if ((dsAddr == null) || (!DSAddress.isValid(dsAddr))) {
            return new waterfall.onewire.HttpClient.ConvertTCmdResult(BaseCmdResult.ControllerErrors.Invalid_dsAddr);
        }

        DSAddress _dsAddr = new DSAddress(dsAddr);

        boolean log;
        if ("true".equals(parmLog)) {
            log = true;
        } else if ("false".equals(parmLog)) {
            log = false;
        } else {
            return new waterfall.onewire.HttpClient.ConvertTCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_log_not_true_or_false);
        }

        BusMaster bm = busMasterManager.getBusMasterForDevice(dsAddr);
        if (bm == null) {
            return new waterfall.onewire.HttpClient.ConvertTCmdResult(BaseCmdResult.ControllerErrors.No_BM_for_dsAddr);
        }

        ConvertTCmd cmd = bm.queryConvertTCmd(_dsAddr, log);
        cmd.execute();

        return new waterfall.onewire.HttpClient.ConvertTCmdResult(cmd);
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

