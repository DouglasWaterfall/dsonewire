package waterfall.onewire.busmaster;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by dwaterfa on 7/7/16.
 */
public class BaseCmd implements Logger {
    protected BusMaster busMaster;
    protected ArrayList<String> logger;
    protected String logContext;

    public BaseCmd(BusMaster busMaster, boolean log) {
        this.busMaster = busMaster;
        if (log) {
            this.logger = new ArrayList<String>();
            this.logContext = this.getClass().getSimpleName() + " ";
        }
        else {
            this.logger = null;
        }
    }

    /**
     * The BusMaster the command is attached to
     */
    public BusMaster getBusMaster() {
        return busMaster;
    }

    /**
     *
     * @param msg
     */
    public void logDebug(String msg) {
        logDebug(logContext, msg);
    }

    /**
     *
     * @param msg
     */
    public void logInfo(String msg) {
        logInfo(logContext, msg);
    }

    /**
     *
     * @param msg
     */
    public void logError(String msg) {
        logError(logContext, msg);
    }

    /**
     *
     * @param e
     */
    public void logError(Throwable e) {
        logError(logContext, e);
    }


    /**
     * interface Logger.logError()
     *
     * @param context
     * @param msg
     */
    public void logError(String context, String msg) {
        if (logger != null) {
            logger.add("[ERROR] " + context + msg);
        }
    }

    /**
     * interface Logger.logError()
     *
     * @param context
     * @param t
     */
    public void logError(String context, Throwable t) {
        if (logger != null) {
            ByteArrayOutputStream baStream = new ByteArrayOutputStream();
            PrintStream pStream = new PrintStream(baStream);
            t.printStackTrace(pStream);
            logger.add("[ERROR] " + context + baStream.toString());
        }
    }

    /**
     * interface Logger.logInfo()
     *
     * @param context
     * @param msg
     */
    public void logInfo(String context, String msg) {
        if (logger != null) {
            logger.add("[INFO] " + context + msg);
        }
    }

    /**
     * interface Logger.logDebug()
     *
     * @param context
     * @param msg
     */
    public void logDebug(String context, String msg) {
        if (logger != null) {
            logger.add("[DEBUG] " + context + msg);
        }
    }

    /**
     * interface Logger.getLogSize()
     *
     * @return
     */
    public int getLogSize() {
        if (logger != null) {
            return logger.size();
        }
        return 0;
    }

    /**
     * interface Logger.getLogIter()
     *
     * @return
     */
    public Iterator<String> getLogIter() {
        if (logger != null) {
            return logger.iterator();
        }
        return null;
    }

}

