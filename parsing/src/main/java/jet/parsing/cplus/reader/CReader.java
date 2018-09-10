package jet.parsing.cplus.reader;

public interface CReader {

    CReadResult read(CReadParams params);

    CReaderType getType();

    Object parseValue();

    default int getResultCount() { return 0;}
    default Object parseValue(int index) { return null;}
    default boolean isMultiReader() {return false;}
}
