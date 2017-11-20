package waterfall.onewire.busmaster;

/**
 * Created by dwaterfa on 6/9/16.
 *
 * This class signals that the command has either has not started or has not completed.
 */
public class NoResultException extends RuntimeException {

  public NoResultException() {
    super();
  }

  public NoResultException(String s) {
    super(s);
  }
}
