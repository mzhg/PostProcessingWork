package jet.parsing.cplus.reader;

public interface CReader {

    boolean accept(String token);

    CReadResult read(ReaderContext context, String line, String[] reminder);

    CReaderType getType();

    Object parseValue();
}
