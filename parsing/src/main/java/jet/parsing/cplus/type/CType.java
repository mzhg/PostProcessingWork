package jet.parsing.cplus.type;

public interface CType {

    String getName();

    boolean isPrimitive() ;

    int getSize();
}
