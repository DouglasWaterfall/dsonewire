package waterfall.onewire.busmaster;

/**
 * Created by dwaterfa on 6/9/16.
 *
 * This class indicates that the cmd has finished but it did not complete successfully and there is no data to return.
 */
public class NoResultDataException extends RuntimeException {
    public NoResultDataException() {
        super();
    }

    public NoResultDataException(String s) {
        super(s);
    }
}
