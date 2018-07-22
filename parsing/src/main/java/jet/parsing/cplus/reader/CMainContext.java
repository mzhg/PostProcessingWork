package jet.parsing.cplus.reader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/7/13 0013.
 */

public final class CMainContext {
    private final List<String> mFiles = new ArrayList<>();
    private ReaderContext mContext = new ReaderContext();

    public void addFile(String filename){
        if(!mFiles.contains(filename))
            mFiles.add(filename);
    }

    public void parse() throws IOException{
        for(int i = 0; i < mFiles.size(); i++){

        }
    }
}
