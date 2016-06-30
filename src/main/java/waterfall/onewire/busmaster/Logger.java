package waterfall.onewire.busmaster;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * Created by dwaterfa on 6/12/16.
 */
public class Logger {
    String lastLevel = null;
    ArrayList<String> nameList = new ArrayList<String>();
    ArrayList<StringBuffer> levelList = new ArrayList<StringBuffer>();

    public Logger() {
    }

    public Logger(String name) {
        pushLevel(name);
    }

    public void debug(String str) {
        add("debug", str);
    }

    public void info(String str) {
        add("info", str);
    }

    public void error(String str) {
        add("error", str);
    }

    public void error(Throwable t) {
        add("error", t.toString());
        final int lIndex = (nameList.size() - 1);
        levelList.get(lIndex).append(toString(t));
    }

    public void append(String str) {
        add(null, str);
    }

    public void pushLevel(String name) {
        nameList.add(name);
        levelList.add(new StringBuffer());
    }

    public String popLevel() {
        final int lIndex = (nameList.size() - 1);
        assert (lIndex > 1);
        nameList.remove(lIndex);
        StringBuffer sb = levelList.remove(lIndex);
        String str = null;
        if (sb.length() > 0) {
            str = sb.toString();
            append(str);
        }
        if (lIndex == 0) {
            lastLevel = str;
        }
        return str;
    }

    public String toString() {
        assert (nameList.size() == 0);
        return lastLevel;
    }

    public void clear() {
        nameList.clear();
        levelList.clear();
        lastLevel = null;
    }

    private void add(String type, String message) {
        if ((message != null) && (message.length() != 0)) {
            final int lIndex = (nameList.size() - 1);
            String str;
            if (message != null) {
                str = "[" + nameList.get(lIndex) + "] [" + type + "] " + message + "\n";
            } else {
                str = message;
            }
            levelList.get(lIndex).append(str);
        }
    }

    private static String toString(Throwable t) {
        ByteArrayOutputStream baStream = new ByteArrayOutputStream();
        PrintStream pStream = new PrintStream(baStream);
        t.printStackTrace(pStream);
        return baStream.toString();
    }

}
