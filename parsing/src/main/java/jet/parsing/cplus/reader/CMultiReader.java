package jet.parsing.cplus.reader;

import java.util.ArrayList;

public class CMultiReader implements CReader {
    protected final ArrayList<CReader> mRegisterReaders = new ArrayList<>();
    protected CReaderType mType;

    private final ArrayList<CReadResult> mParsedResults = new ArrayList<>();
    private final ArrayList<CReader> mResults = new ArrayList<>();

    public CMultiReader(CReaderType type) {
        this.mType = type;
    }

    public void addReader(CReader reader){
        mRegisterReaders.add(reader);
    }

    @Override
    public CReadResult read(CReadParams params) {
        if(params.state == CReadState.INITALIZE){
            mParsedResults.clear();
            mResults.clear();

            for(int i = 0; i < mRegisterReaders.size(); i++){
                mParsedResults.add(CReadResult.UNKOWN);
            }

            return innerParsing(params);
        }else if(params.state == CReadState.PARSING){
            return innerParsing(params);
        }

        return null;
    }

    @Override
    public CReaderType getType() {
        return mType;
    }

    private CReadResult innerParsing(CReadParams params){
        int rejectCount = 0;
        int finishCount = 0;
        for(int i = 0; i < mRegisterReaders.size(); i++){
            CReadResult result = mParsedResults.get(i);
            if(result != CReadResult.REJECT  && result != CReadResult.FINISH) {
                CReader reader = mRegisterReaders.get(i);
                result = reader.read(params);
                mParsedResults.set(i, result);
            }

            if(result == CReadResult.REJECT){
                rejectCount ++;
            }else if(result == CReadResult.FINISH){
                finishCount ++;
            }
        }

        if(rejectCount == mRegisterReaders.size())
            return CReadResult.REJECT;
        else if(finishCount == mRegisterReaders.size()){
            mResults.addAll(mRegisterReaders);
            return CReadResult.FINISH;
        }else {
            return CReadResult.NEXT_TOKEN;
        }
    }


    @Override
    public Object parseValue() {
        throw new UnsupportedOperationException();
    }

    public int getResultCount() { return mResults.size();}
    public Object parseValue(int index) { return mResults.get(index).parseValue();}
    public final boolean isMultiReader() {return true;}
}
