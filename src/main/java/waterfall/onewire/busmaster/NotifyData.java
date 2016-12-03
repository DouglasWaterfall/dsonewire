package waterfall.onewire.busmaster;

import java.util.List;
import java.util.zip.CRC32;

/**
 * Created by dwaterfa on 11/26/16.
 */
public class NotifyData {
    protected BusMaster bm;
    protected List<String> searchResultList;
    protected long searchResultListCRC32;
    protected long searchWriteCTM;

    public NotifyData(BusMaster bm, List<String> searchResultList, long searchWriteCTM) {
        this.bm = bm;
        this.searchResultList = searchResultList;
        CRC32 crc = new CRC32();
        for(String string : searchResultList) {
            crc.update(string.getBytes());
        }
        this.searchResultListCRC32 = crc.getValue();
        this.searchWriteCTM = searchWriteCTM;
    }

    public BusMaster getBusMaster() {
        return this.bm;
    }

    public List<String> getSearchResultList() {
        return this.searchResultList;
    }

    public long getSearchResultListCRC32() {
        return this.searchResultListCRC32;
    }

    public long getSearchWriteCTM() {
        return this.searchWriteCTM;
    }

}
