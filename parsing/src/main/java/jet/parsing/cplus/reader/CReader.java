package jet.parsing.cplus.reader;

public interface CReader {

    CReadResult read(CReadParams params);

    CReaderType getType();

    Object parseValue();

}
