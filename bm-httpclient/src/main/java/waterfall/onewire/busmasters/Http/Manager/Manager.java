package waterfall.onewire.busmasters.Http.Manager;

import waterfall.onewire.BusMasterRegistry;
import waterfall.onewire.HttpClient.*;
import waterfall.onewire.busmaster.BusMaster;
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
public class Manager {
    private final BusMasterRegistry bmRegistry;
    private final String endpoint;

    // We call login to get this. Though it does not currently happen, it is possible that we fail an
    // authorization but it does mean that our state changes. It depends on how the server treats this
    // when it changes the currentServerTimestampMSec.
    private String authorization;

    // This represents our version of the list of BusMasters we are managing as clients. Zero means
    // we know nothing, and also have no Clients under management.
    private long bmUpdateTimestampMSec;

    // The clients we are managing, by bmIdent.
    private Map<String, Client> bmIdentClientMap;

    // The thread which polls the server.
    private WaitForEventThread waitForEventThread;

    private WaitForEventCmdResult.BMListChangedData bmListChangedData = null;

    public Manager(BusMasterRegistry bmRegistry, String endpoint) {
        this.bmRegistry = bmRegistry;
        this.endpoint = endpoint;

        authorization = null;
//        currentServerTimestampMSec = 0;
        bmUpdateTimestampMSec = 0;
        bmIdentClientMap = new HashMap<String, Client>();

        waitForEventThread = new WaitForEventThread(this, endpoint);
        waitForEventThread.setDaemon(true);
        waitForEventThread.start();
    }

    /**
     * The handshaking sequence with the server is that we need to login to get the authorization code which will
     * be necessary for calling any other apis on the server.
     * @return true if the login was successful and authorization was set with the current value.
     */
    public String login(Logger.LogLevel logLevel) {
        String logLevelParam = Util.computeLogLevelParam(logLevel);
        String suffix = "waitForEvent/" + ((logLevelParam != null) ? logLevelParam : "");

        LoginCmdResult loginCmdResult = (LoginCmdResult)Util.postURLData(endpoint, suffix, null, 5000, null, LoginCmdResult.class);

        if (loginCmdResult.hasPostError()) {
            System.err.println("Manager.login() postError:" + loginCmdResult.getPostError().name());
            return null;
        }

        authorization = loginCmdResult.authorization;
        return authorization;
    }

    /*
     */
    public synchronized void bmListChanged(long updateTimestampMSec, String[] bmIdents) {
        bmUpdateTimestampMSec = updateTimestampMSec;

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

                    bmRegistry.addBusMaster(client);
                }
            }
        }

        for (Client client : bmIdentClientMap.values()) {
            bmRegistry.removeBusMaster(client);
        }

        bmIdentClientMap = newBMIdentClientMap;
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

    private void syncClients(WaitForEventCmdResult.BMListChangedData changeData) {

    }
       // public long currentServerTimestampMSec;
        //public long bmUpdateTimestampMSec;
        //public String[] bmIdents;

    private void forgetClient(String bmIdent, Client bmClient) {

    }

}

