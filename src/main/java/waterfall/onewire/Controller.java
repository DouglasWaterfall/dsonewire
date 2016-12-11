package waterfall.onewire;

import com.dalsemi.onewire.utils.Convert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import waterfall.onewire.HttpClient.BaseCmdResult;
import waterfall.onewire.HttpClient.StatusCmdResult;
import waterfall.onewire.HttpClient.TimeDiffResult;
import waterfall.onewire.busmaster.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * Created by dwaterfa on 6/12/16.
 */
@RestController
@RequestMapping(value = "/httpbusmaster")
public class Controller implements Observer {

    private final BusMasterRegistry bmRegistry;
    private final HashMap<String, BusMaster> bmMap;
    private final Base64.Encoder encoder;
    private String currentAuthenticationValue;

    @Autowired
    public Controller(BusMasterRegistry bmRegistry) {
        this.bmRegistry = bmRegistry;
        this.bmMap = new HashMap<String, BusMaster>();
        this.encoder = Base64.getUrlEncoder();
        this.currentAuthenticationValue = null;
        // This is how we find out about BusMasters which we will export
        bmRegistry.addObserver(this);
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
            @RequestParam(value = "logLevel", required = false) String parmLogLevel) {

        boolean doLog;
        Logger.LogLevel logLevel = null;
        if (parmLogLevel != null) {
            logLevel = logLevelFromLogParam(parmLogLevel);
            if (logLevel == null) {
                return new waterfall.onewire.HttpClient.ListBusMastersCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_logLevel);
            }
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
                                                                      @RequestParam(value = "logLevel", required = false) String parmLogLevel) {
        checkAuthenticationHeader(authorization);

        BusMaster bm;
        if ((bmIdent == null) || ((bm = getBusMasterByIdent(bmIdent)) == null)) {
            return new waterfall.onewire.HttpClient.StartBusCmdResult(BaseCmdResult.ControllerErrors.Unknown_bmIdent);
        }

        Logger.LogLevel logLevel = null;
        if (parmLogLevel != null) {
            logLevel = logLevelFromLogParam(parmLogLevel);
            if (logLevel == null) {
                return new waterfall.onewire.HttpClient.StartBusCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_logLevel);
            }
        }

        StartBusCmd cmd = bm.queryStartBusCmd(logLevel);
        cmd.execute();

        return new waterfall.onewire.HttpClient.StartBusCmdResult(cmd);
    }

    @RequestMapping(value = "/stopBusCmd/{bmIdent}", method = RequestMethod.POST)
    public waterfall.onewire.HttpClient.StopBusCmdResult stopBusCmd(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                                    @PathVariable(value = "bmIdent") String bmIdent,
                                                                    @RequestParam(value = "logLevel", required = false) String parmLogLevel) {
        checkAuthenticationHeader(authorization);

        BusMaster bm;
        if ((bmIdent == null) || ((bm = getBusMasterByIdent(bmIdent)) == null)) {
            return new waterfall.onewire.HttpClient.StopBusCmdResult(BaseCmdResult.ControllerErrors.Unknown_bmIdent);
        }

        Logger.LogLevel logLevel = null;
        if (parmLogLevel != null) {
            logLevel = logLevelFromLogParam(parmLogLevel);
            if (logLevel == null) {
                return new waterfall.onewire.HttpClient.StopBusCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_logLevel);
            }
        }

        StopBusCmd cmd = bm.queryStopBusCmd(logLevel);
        cmd.execute();

        return new waterfall.onewire.HttpClient.StopBusCmdResult(cmd);
    }

    @RequestMapping(value = "/searchBusCmd/{bmIdent}", method = RequestMethod.POST)
    public waterfall.onewire.HttpClient.SearchBusCmdResult searchBusCmd(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                                        @PathVariable(value = "bmIdent") String bmIdent,
                                                                        @RequestParam(value = "byAlarm", required = false) String parmByAlarm,
                                                                        @RequestParam(value = "byFamilyCode", required = false) String parmByFamilyCode,
                                                                        @RequestParam(value = "logLevel", required = false) String parmLogLevel) {
        checkAuthenticationHeader(authorization);

        BusMaster bm;
        if ((bmIdent == null) || ((bm = getBusMasterByIdent(bmIdent)) == null)) {
            return new waterfall.onewire.HttpClient.SearchBusCmdResult(BaseCmdResult.ControllerErrors.Unknown_bmIdent);
        }

        Logger.LogLevel logLevel = null;
        if (parmLogLevel != null) {
            logLevel = logLevelFromLogParam(parmLogLevel);
            if (logLevel == null) {
                return new waterfall.onewire.HttpClient.SearchBusCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_logLevel);
            }
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

            cmd = bm.querySearchBusByFamilyCmd(familyCode, logLevel);
        } else if ((parmByAlarm == null) || ("false".equals(parmByAlarm))) {
            cmd = bm.querySearchBusCmd(logLevel);
        } else if ("true".equals(parmByAlarm)) {
            cmd = bm.querySearchBusByAlarmCmd(logLevel);
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
                                                                                    @RequestParam(value = "logLevel", required = false) String parmLogLevel) {
        checkAuthenticationHeader(authorization);

        BusMaster bm;
        if ((bmIdent == null) || ((bm = getBusMasterByIdent(bmIdent)) == null)) {
            return new waterfall.onewire.HttpClient.ReadPowerSupplyCmdResult(BaseCmdResult.ControllerErrors.Unknown_bmIdent);
        }

        if ((dsAddr == null) || (!DSAddress.isValid(dsAddr))) {
            return new waterfall.onewire.HttpClient.ReadPowerSupplyCmdResult(BaseCmdResult.ControllerErrors.Invalid_dsAddr);
        }

        Logger.LogLevel logLevel = null;
        if (parmLogLevel != null) {
            logLevel = logLevelFromLogParam(parmLogLevel);
            if (logLevel == null) {
                return new waterfall.onewire.HttpClient.ReadPowerSupplyCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_logLevel);
            }
        }

        ReadPowerSupplyCmd cmd = bm.queryReadPowerSupplyCmd(new DSAddress(dsAddr), logLevel);
        cmd.execute();

        return new waterfall.onewire.HttpClient.ReadPowerSupplyCmdResult(cmd);
    }

    @RequestMapping(value = "/readScratchpadCmd/{bmIdent}/{dsAddr}/{rCount}", method = RequestMethod.POST)
    public waterfall.onewire.HttpClient.ReadScratchpadCmdResult readScratchPadCmd(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                                                  @PathVariable(value = "bmIdent") String bmIdent,
                                                                                  @PathVariable(value = "dsAddr") String dsAddr,
                                                                                  @PathVariable(value = "rCount") Long rCount,
                                                                                  @RequestParam(value = "logLevel", required = false) String parmLogLevel) {
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

        Logger.LogLevel logLevel = null;
        if (parmLogLevel != null) {
            logLevel = logLevelFromLogParam(parmLogLevel);
            if (logLevel == null) {
                return new waterfall.onewire.HttpClient.ReadScratchpadCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_logLevel);
            }
        }

        ReadScratchpadCmd cmd = bm.queryReadScratchpadCmd(new DSAddress(dsAddr), rCount.shortValue(), logLevel);
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
                                                                      @RequestParam(value = "logLevel", required = false) String parmLogLevel) {
        checkAuthenticationHeader(authorization);

        BusMaster bm;
        if ((bmIdent == null) || ((bm = getBusMasterByIdent(bmIdent)) == null)) {
            return new waterfall.onewire.HttpClient.ConvertTCmdResult(BaseCmdResult.ControllerErrors.Unknown_bmIdent);
        }

        if ((dsAddr == null) || (!DSAddress.isValid(dsAddr))) {
            return new waterfall.onewire.HttpClient.ConvertTCmdResult(BaseCmdResult.ControllerErrors.Invalid_dsAddr);
        }

        Logger.LogLevel logLevel = null;
        if (parmLogLevel != null) {
            logLevel = logLevelFromLogParam(parmLogLevel);
            if (logLevel == null) {
                return new waterfall.onewire.HttpClient.ConvertTCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_logLevel);
            }
        }

        ConvertTCmd cmd = bm.queryConvertTCmd(new DSAddress(dsAddr), logLevel);
        cmd.execute();

        return new waterfall.onewire.HttpClient.ConvertTCmdResult(cmd);
    }

    @RequestMapping(value = "/timeDiff/{clientSentTimeMSec}", method = RequestMethod.POST)
    public TimeDiffResult timeDiff(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                   @PathVariable(value = "clientSentTimeMSec") String clientSentTimeMSec) {
        long serverReceivedTimeMSec = System.currentTimeMillis();

        checkAuthenticationHeader(authorization);

        Long t;

        try {
            t = Long.valueOf(clientSentTimeMSec);
            if (t.longValue() <= 0) {
                throw new NumberFormatException();
            }
        }
        catch (NumberFormatException e) {
            return new TimeDiffResult(BaseCmdResult.ControllerErrors.Bad_parm_clientSentTimeMSec);
        }

        return new TimeDiffResult(t, serverReceivedTimeMSec);
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

    // Called from the BusMasterRegistry when a new BusMaster is found. Observable will be the BusMasterRegistry
    // and the arg will be the new BusMaster
    @Override
    public void update(Observable o, Object arg) {
        if ((o instanceof BusMasterRegistry) &&
            (arg instanceof BusMaster)) {
            BusMaster bm = (BusMaster)arg;
            String bmIdent = getIdentFor(bm);

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
    }

    private String getIdentFor(final BusMaster bm) {
        return encoder.encodeToString(bm.getName().getBytes());
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

    private Logger.LogLevel logLevelFromLogParam(String logLevelParam) {
        Logger.LogLevel l = new Logger.LogLevel();

        String[] levels = logLevelParam.split(",");

        for (String level : levels) {
            if ("device".equals(level)) {
                if (l.isLevelDevice()) {
                    return null;
                }
                l.setLevelDevice();
            }
            if ("cmd".equals(level)) {
                if (l.isLevelCmd()) {
                    return null;
                }
                l.setLevelCmd();
            }
            if ("comm".equals(level)) {
                if (l.isLevelComm()) {
                    return null;
                }
                l.setLevelComm();
            }
        }

        return l;
    }


}

