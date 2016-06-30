package waterfall.onewire.busmaster.Http;

import waterfall.onewire.busmaster.HA7S.HA7SSerial;

/**
 * Created by dwaterfa on 2/15/16.
 */
public class ServerResponse {

    public HA7SSerial.ReadResult.ErrorCode error;

    public byte[] rbuf; // non null if non-zero read

    public Long postWriteCTM; // non null if valid
}
