package waterfall.onewire;

/**
 * Created by dwaterfa on 9/10/17.
 */
public class BMList {

    public final long lastBMListUpdateTimestamp;
    public final String[] bmIdents;

    public BMList(long lastBMListUpdateTimestamp, String[] bmIdents) {
        this.lastBMListUpdateTimestamp = lastBMListUpdateTimestamp;
        this.bmIdents = bmIdents;
    }
}
