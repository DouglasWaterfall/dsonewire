package waterfall.onewire.busmasters.Http.Manager;

import waterfall.onewire.BusMasterRegistry;
import waterfall.onewire.HttpClient.*;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmasters.Http.Client.Client;
import waterfall.onewire.busmasters.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dwaterfa on 01/20/17.
 *
 * We maintain the stable of Http.Client instances which are known to be on the endpoint. By definition they must
 * have been started to have been added to the BusMasterRegistry on the server which is where we find them.
 *
 * Our first and foremost responsibility is to establish the WaitForEvent Thread which will ping the server and
 * wait on events such as new BusMasters and BusMaster specific search and alarm search notifications.
 */
public class Manager implements Runnable {
    private final BusMasterRegistry bmRegistry;
    private final String endpoint;

    // We call login to get this. Though it does not currently happen, it is possible that we fail an
    // authorization but it does mean that our state changes. It depends on how the server treats this
    // when it changes the currentServerTimestampMSec.
    private String authorization;

    // This keeps us synchronized with the server on a global level - if this changes then all our
    // prior state about it needs to be changed too. Zero means "I have no prior state" and will not
    // trigger a controller error on the wait call. This also means that there must be a zero for
    // bmUpdateTimestampMSec and an empty Client Map.
    private long currentServerTimestampMSec;

    // This represents our version of the list of BusMasters we are managing as clients. Zero means
    // we know nothing, and also have not Clients under management.
    private long bmUpdateTimestampMSec;

    // The clients we are managing, by bmIdent.
    private Map<String, Client> bmIdentClientMap;

    // The thread which polls the server.
    private Thread waitForEventThread;

    private enum State {
        /**
         * Our authorization is equal to zero or we had a login and were told that it was out of date
         * at which point we will have reset it to zero to start over.
         */
        NotLoggedIn,

        /**
         * We attempted to login but got some sort of post error which means we failed to connect to
         * the server, we will goto sleep and try again.
         */
        NotLoggedInDelay,

        /**
         * Our server timestamp is equal to zero, or we did have it and were told that it was out of
         * date at which point we will have reset it to zero to start over.
         */
        NoServerTimestamp,

        /**
         * We attempted to get the server timestamp but got some sort of post error which means we
         * failed to connect to the server, we will goto sleep and try again.
         */
        NoServerTimestampDelay,

        /**
         * Here we are in regular sequence of waiting for events from the server.
         */
        WaitingForEvents,

        /**
         * We attempted to get the server timestamp but got some sort of post error which means we
         * failed to connect to the server, we will goto sleep and try again.
         */
        WaitingForEventsDelay
;
    }

    private State state;

    private WaitForEventCmdResult.BMListChangedData bmListChangedData = null;

    public Manager(BusMasterRegistry bmRegistry, String endpoint) {
        this.bmRegistry = bmRegistry;
        this.endpoint = endpoint;

        authorization = null;
        currentServerTimestampMSec = 0;
        bmUpdateTimestampMSec = 0;
        bmIdentClientMap = new HashMap<String, Client>();

        state = null;

        setStateNotLoggedIn();

        waitForEventThread = new Thread(this);
        waitForEventThread.setDaemon(true);
        waitForEventThread.start();
    }


    /**
     * Called when waitForEvent returns a PostError
     */
    private void eventPostError(PostErrors pe) {
    }

    private void eventController() {

    }


    /**
     * Rather than return a separate Controller Error for a mismatch between the server and the wait request the
     * Controller solely relies on the manager to detect that there has been a difference and to treat it as a start
     * over as appropriate.
     * What is returned is the new server timestamp and the current BMs, if any.
     * All outstanding searches, if any have been cancelled.
     * @param listChangedData
     */
    private void eventServerTimestampChanged(WaitForEventCmdResult.BMListChangedData listChangedData) {
        if (currentServerTimestampMSec == 0) {
            // We had no prior state and were trying to start over.

        }
        else {
            // The server is in a new state than we thought so we will attempt to resync the BMs but of course it will
            // fail because the Controller will be generated unique bmIdents for them. But we can do it the same way
            // regardless as if they were going missing.
            currentServerTimestampMSec = listChangedData.currentServerTimestampMSec;
            eventBMListChanged(listChangedData.bmUpdateTimestampMSec, listChangedData.bmIdents);
        }
    }

    private void eventBMListChanged(long updateTimestampMSec, String[] bmIdents) {
        bmUpdateTimestampMSec = updateTimestampMSec;

        ArrayList<String>   removedBMIdentList = null;
        ArrayList<String>   newBMIdentList = null;

        HashMap<String, Client> newBMIdentClientMap = new HashMap<String, Client>();
        if (bmIdents != null) {
            for (String bmIdent : bmIdents) {
                Client client = bmIdentClientMap.get(bmIdent);
                if (client != null) {
                    // BusMasters we already knew about
                    newBMIdentClientMap.put(bmIdent, client);
                    bmIdentClientMap.remove(bmIdent);
                }
                else {
                    // new BusMaster
                    client = new Client(endpoint, authorization, bmIdent, "name");
                    if (newBMIdentList == null) {
                        newBMIdentList = new ArrayList<>();
                    }
                    newBMIdentList.add(bmIdent);
                    newBMIdentClientMap.put(bmIdent, client);
                }
            }
        }

        for (String bmIdent : bmIdentClientMap.keySet()) {
            if (removedBMIdentList == null) {
                removedBMIdentList = new ArrayList<>();
            }
            removedBMIdentList.add(bmIdent);
        }

        bmIdentClientMap = newBMIdentClientMap;

        forgetClient(bmIdent, client);
    }

    /**
     *
     * This will be called when the server resets the server timestamps and we need to throw away
     * all the state we know about the BusMasters it had. Due to the nature of the Controller the
     * new BusMaster Idents will be different even though the physical device may be the same.
     */
    private void clearClientMap() {
        for (String bmIdent : bmIdentClientMap.keySet()) {
            forgetClient(bmIdent, bmIdentClientMap.get(bmIdent));
        }
    }

    private void forgetClient(String bmIdent, Client client) {

        // set the client as not being started.
        // any queued cmds waiting for us (if any?) then they need to be cancelled.
        // we need a special method on the Client which we can call to trigger a cancel so all cmds
        // return with errors and all future cmds will return not started/bm not found.

        bmIdentClientMap.remove(bmIdent);
    }

    private void syncClients(WaitForEventCmdResult.BMListChangedData changeData) {

    }
       // public long currentServerTimestampMSec;
        //public long bmUpdateTimestampMSec;
        //public String[] bmIdents;


    // Where the waitForEvent thread lives
    public void run() {

        for (;;) {
            switch (state) {
                case NotLoggedIn:
                    // Attempt to login
                    if (login()) {
                        setStateNoServerTimestamp();
                    }
                    else {
                        setStateNotLoggedInDelay();
                    }
                    break;

                case NotLoggedInDelay:
                    delayInSeconds(15);
                    setStateNotLoggedIn();
                    break;

                case NoServerTimestamp:

                case NoServerTimestampDelay:

                case Running: // waiting for events
                 default:
                     System.err.println("Unknown state:" + state.name());
                     break;
            }
        }
    }

    private void setStateNotLoggedIn() {
        if ((state != State.NotLoggedIn)  && (state != State.NotLoggedInDelay)) {
            authorization = null;
            bmListChangedData = null;
        }
        state = State.NotLoggedIn;
    }

    private void setStateNotLoggedInDelay() {
        if ((state != State.NotLoggedIn)  && (state != State.NotLoggedInDelay)) {
            authorization = null;
            bmListChangedData = null;
        }
        state = State.NotLoggedInDelay;
    }

    private void setStateNoServerTimestamp() {
        if ((state != State.NoServerTimestamp)  && (state != State.NoServerTimestampDelay)) {
            bmListChangedData = null;
        }
        state = State.NoServerTimestamp;
    }

    private void setStateNoServerTimestampDelay() {
        if ((state != State.NoServerTimestamp)  && (state != State.NoServerTimestampDelay)) {
            bmListChangedData = null;
        }
        state = State.NoServerTimestampDelay;
    }

    private void setStateWaitingForEvents() {
        if ((state != State.WaitingForEvents)  && (state != State.WaitingForEvents)) {
            ;
        }
        state = State.WaitingForEvents;
    }

    /**
     * The handshaking sequence with the server is that we need to login to get the authorization code which will
     * be necessary for calling any other apis on the server.
     * @return true if the login was successful and authorization was set with the current value.
     */
    private boolean login(Logger.LogLevel logLevel) {
        String logLevelParam = Util.computeLogLevelParam(logLevel);
        String suffix = "waitForEvent/" + ((logLevelParam != null) ? logLevelParam : "");

        LoginCmdResult loginCmdResult = (LoginCmdResult)Util.postURLData(endpoint, suffix, null, 5000, null, LoginCmdResult.class);

        if (loginCmdResult.hasPostError()) {
            System.err.println("Manager.login() postError:" + loginCmdResult.getPostError().name());
            return false;
        }

        authorization = loginCmdResult.authorization;
        return true;
    }

    private void delayInSeconds(int seconds) {
        if (seconds > 0) {
            long delayMSec = (seconds * 1000);
            long endTimeMSec = (System.currentTimeMillis() + delayMSec);

            do {
                try {
                    Thread.sleep(delayMSec);
                } catch (InterruptedException e) {
                    ;
                }
            }
            while (System.currentTimeMillis() < endTimeMSec);
        }
    }

    // we need to be the one to coordinate with the remote

    /*
    private void startAndRegister(HA7S ha7s, Logger.LogLevel logLevel) {
        System.out.println("Starting dsonewire-busmasterserver on " + ha7s.getName());

        startCmd = ha7s.queryStartBusCmd(logLevel);
        .Result startResult = startCmd.execute();

        // dumpLog(startCmd.getLogger());

        if (startResult == Result.started) {
            bmRegistry.addBusMaster(ha7s);
        }
        else {
            System.out.println("Failed on " + ha7s.getName() + ": " + startResult.name());
        }
    }
    */

    public static WaitForEventCmdResult go(String endpoint,
                                           String authHeader,
                                           WaitForEventCmdData cmd,
                                           Logger optLogger) {
        final String logContext = (optLogger != null) ? WaitForEventCmdData.class.getSimpleName() + ".go: " : "";

        // Try to list the remote busmasters.
        String logLevelParam = Util.computeLogLevelParam(optLogger);
        final String suffix = "waitForEvent/" + ((logLevelParam != null) ? logLevelParam : "");

        WaitForEventCmdResult waitForEventPostResult = (WaitForEventCmdResult) Util.postURLData(endpoint, suffix, authHeader, cmd.getWaitTimeoutMSec(), cmd, WaitForEventCmdResult.class);

        if (waitForEventPostResult.getPostError() != null) {
            Util.logErrorCommLevel(optLogger, logContext, " postError:" + waitForEventPostResult.getPostError().name());
        } else if (waitForEventPostResult.getControllerError() != null) {
            Util.logErrorCommLevel(optLogger, logContext, " controllerError:" + waitForEventPostResult.getControllerError());
        }

        return waitForEventPostResult;
    }
}

