package waterfall.onewire.busmaster;

import java.util.Iterator;

/**
 * Created by dwaterfa on 6/12/16.
 */
public interface Logger {

    public void logDebug(String context, String str);

    public void logInfo(String context, String str);

    public void logError(String context, String str);

    public void logError(String context, Throwable t);

    public int getLogSize();

    public Iterator<String> getLogIter();

}
