package jet.parsing.cplus.reader;

import java.util.ArrayList;
import java.util.List;

import jet.parsing.cplus.type.CType;

public class ReaderContext {
    private final List<CType> mTypes = new ArrayList<>();
    private final List<CReader>  mReaders = new ArrayList<>();
    CHFile mCurrentFile;
}
