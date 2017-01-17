package waterfall.onewire.httpserver;

import com.dalsemi.onewire.utils.Convert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import waterfall.onewire.DSAddress;
import waterfall.onewire.HttpClient.*;
import waterfall.onewire.busmaster.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by dwaterfa on 6/12/16.
 */
@RestController
@RequestMapping(value = "/httpbusmaster")
public class Controller {

    private Model model;

    @Autowired
    public Controller(Model model) {
        this.model = model;
    }

    /*
    @RequestMapping(value = "",
                    method = RequestMethod.POST,
                    produces = {MediaType.APPLICATION_JSON_VALUE},
                    consumes = {MediaType.APPLICATION_JSON_VALUE})
    public Token create(Principal principal, @RequestBody(required = true) Token token) throws ServiceException {
        ...
    */

    // temporary hack until we can sort out how the client can authentication itself and establish control
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String login() {
        return model.getCurrentAuthenticationValue();
    }

    @RequestMapping(value = "/waitForEvent", method = RequestMethod.POST)
    public WaitForEventResult waitforEvent(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                           @RequestBody WaitForEventCmd cmd) {
        model.checkAuthenticationHeader(authorization);

        return model.waitForEvent(cmd);
    }

    @RequestMapping(value = "/busStatusCmd/{bmIdent}", method = RequestMethod.POST)
    public waterfall.onewire.HttpClient.StatusCmdResult busStatusCmd(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                                     @PathVariable String bmIdent) {
        model.checkAuthenticationHeader(authorization);

        BusMaster bm;
        if ((bmIdent == null) || ((bm = model.getBusMasterByIdent(bmIdent)) == null)) {
            return new waterfall.onewire.HttpClient.StatusCmdResult(BaseCmdResult.ControllerErrors.Unknown_bmIdent);
        }

        return new StatusCmdResult(bmIdent, bm);
    }

    @RequestMapping(value = "/startBusCmd/{bmIdent}", method = RequestMethod.POST)
    public waterfall.onewire.HttpClient.StartBusCmdResult startBusCmd(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                                      @PathVariable(value = "bmIdent") String bmIdent,
                                                                      @RequestParam(value = "logLevel", required = false) String parmLogLevel) {
        model.checkAuthenticationHeader(authorization);

        BusMaster bm;
        if ((bmIdent == null) || ((bm = model.getBusMasterByIdent(bmIdent)) == null)) {
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
        model.checkAuthenticationHeader(authorization);

        BusMaster bm;
        if ((bmIdent == null) || ((bm = model.getBusMasterByIdent(bmIdent)) == null)) {
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
        model.checkAuthenticationHeader(authorization);

        BusMaster bm;
        if ((bmIdent == null) || ((bm = model.getBusMasterByIdent(bmIdent)) == null)) {
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

    @RequestMapping(value = "/scheduleNotifySearchBusCmd/{bmIdent}/{byAlarm}/{minPeriodMSec}", method = RequestMethod.POST)
    public ScheduleSearchBusCmdResult scheduleSearchBusCmd(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                            @PathVariable(value = "bmIdent") String bmIdent,
                                                           @PathVariable(value = "byAlarm") String byAlarm,
                                                           @PathVariable(value = "minPeriodMSec") String minPeriodMSec) {
        model.checkAuthenticationHeader(authorization);

        if (!model.isBMIdentValid(bmIdent)) {
            return new waterfall.onewire.HttpClient.ScheduleSearchBusCmdResult(BaseCmdResult.ControllerErrors.Unknown_bmIdent);
        }

        boolean t_byAlarm;
        if ("false".equals(byAlarm)) {
            t_byAlarm = false;
        }
        else if ("true".equals(byAlarm)) {
            t_byAlarm = true;
        }
        else {
            return new waterfall.onewire.HttpClient.ScheduleSearchBusCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_byAlarm_not_true_false);
        }

        Long l = null;
        try {
            l = Long.valueOf(minPeriodMSec);
        }
        catch (NumberFormatException e) {
        }
        if (l == null) {
            return new waterfall.onewire.HttpClient.ScheduleSearchBusCmdResult(BaseCmdResult.ControllerErrors.Invalid_minPeriodMSec);
        }

        return new waterfall.onewire.HttpClient.ScheduleSearchBusCmdResult(model.scheduleSearch(bmIdent, t_byAlarm, l));
    }

    @RequestMapping(value = "/updateScheduledNotifySearchBusCmd/{bmIdent}/{byAlarm}/{minPeriodMSec}", method = RequestMethod.POST)
    public UpdateScheduledSearchBusCmdResult updateScheduledNotifySearchBusCmd(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                           @PathVariable(value = "bmIdent") String bmIdent,
                                                           @PathVariable(value = "byAlarm") String byAlarm,
                                                           @PathVariable(value = "minPeriodMSec") String minPeriodMSec) {
        model.checkAuthenticationHeader(authorization);

        if (!model.isBMIdentValid(bmIdent)) {
            return new waterfall.onewire.HttpClient.UpdateScheduledSearchBusCmdResult(BaseCmdResult.ControllerErrors.Unknown_bmIdent);
        }

        boolean t_byAlarm;
        if ("false".equals(byAlarm)) {
            t_byAlarm = false;
        }
        else if ("true".equals(byAlarm)) {
            t_byAlarm = true;
        }
        else {
            return new waterfall.onewire.HttpClient.UpdateScheduledSearchBusCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_byAlarm_not_true_false);
        }

        Long l = null;
        try {
            l = Long.valueOf(minPeriodMSec);
        }
        catch (NumberFormatException e) {
        }
        if (l == null) {
            return new waterfall.onewire.HttpClient.UpdateScheduledSearchBusCmdResult(BaseCmdResult.ControllerErrors.Invalid_minPeriodMSec);
        }

        return new waterfall.onewire.HttpClient.UpdateScheduledSearchBusCmdResult(model.updateScheduledSearch(bmIdent, t_byAlarm, l));
    }

    @RequestMapping(value = "/cancelScheduledNotifySearchBusCmd/{bmIdent}/{byAlarm}", method = RequestMethod.POST)
    public CancelScheduledSearchBusCmdResult cancelScheduledNotifySearchBusCmd(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                           @PathVariable(value = "bmIdent") String bmIdent,
                                                            @PathVariable(value = "byAlarm") String byAlarm) {
        model.checkAuthenticationHeader(authorization);

        if (!model.isBMIdentValid(bmIdent)) {
            return new waterfall.onewire.HttpClient.CancelScheduledSearchBusCmdResult(BaseCmdResult.ControllerErrors.Unknown_bmIdent);
        }

        boolean t_byAlarm;
        if ("false".equals(byAlarm)) {
            t_byAlarm = false;
        }
        else if ("true".equals(byAlarm)) {
            t_byAlarm = true;
        }
        else {
            return new waterfall.onewire.HttpClient.CancelScheduledSearchBusCmdResult(BaseCmdResult.ControllerErrors.Bad_parm_byAlarm_not_true_false);
        }

        return new waterfall.onewire.HttpClient.CancelScheduledSearchBusCmdResult(model.cancelScheduledSearch(bmIdent, t_byAlarm));
    }

    @RequestMapping(value = "/readPowerSupplyCmd/{bmIdent}/{dsAddr}", method = RequestMethod.POST)
    public waterfall.onewire.HttpClient.ReadPowerSupplyCmdResult readPowerSupplyCmd(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                                                                    @PathVariable(value = "bmIdent") String bmIdent,
                                                                                    @PathVariable(value = "dsAddr") String dsAddr,
                                                                                    @RequestParam(value = "logLevel", required = false) String parmLogLevel) {
        model.checkAuthenticationHeader(authorization);

        BusMaster bm;
        if ((bmIdent == null) || ((bm = model.getBusMasterByIdent(bmIdent)) == null)) {
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
        model.checkAuthenticationHeader(authorization);

        BusMaster bm;
        if ((bmIdent == null) || ((bm = model.getBusMasterByIdent(bmIdent)) == null)) {
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
        model.checkAuthenticationHeader(authorization);

        BusMaster bm;
        if ((bmIdent == null) || ((bm = model.getBusMasterByIdent(bmIdent)) == null)) {
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

        model.checkAuthenticationHeader(authorization);

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

    @ExceptionHandler(Model.AuthException.class)
    void handleBadRequests(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.GONE.value(), "Authorization header mismatch");
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

