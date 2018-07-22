package jet.parsing.cplus.reader;

/**
 * Created by Administrator on 2018/7/12 0012.
 */

public abstract class ProcessorReader implements CReader {

    protected static final String PROCESSOR_START = "#";

    @Override
    public CReaderType getType() {
        return CReaderType.PROCESSOR;
    }
}
