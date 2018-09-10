package jet.parsing.cplus.reader;

import sun.util.resources.cldr.es.CalendarData_es_PY;

public class CToken {
    public enum Type{
        STRNG,
        DIGIT,
        CHARACTOR
    }

    public final String stoken;
    public final double vtoken;
    public final Type type;

    public CToken(String stoken, double vtoken, Type type) {
        this.stoken = stoken;
        this.vtoken = vtoken;
        this.type = type;
    }

    @Override
    public String toString() {
//        return isDigit ? String.valueOf(vtoken) : stoken;
        switch (type){
            case DIGIT: return String.valueOf(vtoken);
            default:    return stoken;
        }
    }
}
