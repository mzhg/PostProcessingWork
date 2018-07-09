package jet.parsing.cplus.reader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jet.parsing.cplus.type.CType;

public class CHFile {
    private boolean mPragmaOnce;
    private ReaderContext mContext = new ReaderContext();

//    private

    public void load(String filename) throws IOException{

    }

    void setPragmaOnce(boolean flag){mPragmaOnce = flag;}
}
