package waterfall.onewire.busmasters.Http.Manager;

import waterfall.onewire.HttpClient.PostErrors;
import waterfall.onewire.HttpClient.WaitForEventCmdData;
import waterfall.onewire.HttpClient.WaitForEventCmdResult;
import waterfall.onewire.busmaster.Logger;
import waterfall.onewire.busmasters.Util;

/**
 * Created by dwaterfa on 2/18/17.
 */
public class WaitForEventThread extends Thread {


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

    private final String endpoint;
    private final Manager manager;

    private State state;
    private String authorization;

    // This keeps us synchronized with the server on a global level - if this changes then all our
    // prior state about it needs to be changed too. Zero means "I have no prior state" and will not
    // trigger a controller error on the wait call. This also means that there must be a zero for
    // bmUpdateTimestampMSec and an empty Client Map.
    private long currentServerTimestampMSec;

    public WaitForEventThread(Manager manager, String endpoint) {

        this.manager = manager;
        this.endpoint = endpoint;
        this.currentServerTimestampMSec = 0;

        setStateNotLoggedIn();
    }

    @Override
    public void run() {

        for (;;) {
            switch (state) {
                case NotLoggedIn:
                    // Attempt to login
                    if ((authorization = manager.login()) != null) {
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

                case WaitingForEvents:
                    WaitForEventCmdResult = waitForEvent();

                default:
                    System.err.println("Unknown state:" + state.name());
                    break;
            }
        }
    }

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

}
