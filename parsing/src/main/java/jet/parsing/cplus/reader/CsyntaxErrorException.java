package jet.parsing.cplus.reader;

/**
 * Created by Administrator on 2018/7/16 0016.
 */

public class CsyntaxErrorException extends RuntimeException {
    public CsyntaxErrorException() {
    }

    public CsyntaxErrorException(String message) {
        super(message);
    }
}
