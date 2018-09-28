package jet.parsing.cplus.reader;

import java.util.Arrays;

import jet.parsing.util.StringUtils;

/**
 * Created by Administrator on 2018/7/12 0012.
 */

public class PragmaProcessorReader extends ProcessorReader {

    public enum Type{
        Unkown,
        Once,
    }

    private static final int SHARAP_SYMBLE = 0;  // "#“
    private static final int PRAGMA_SYMBLE = 1;  // "pragma"
    private static final int VALUE_SYMBLE = 2;   // "once", "pack"
    private static final int COLON_SYMBLE = 3;   // ;

    private Type mType = Type.Unkown;
    private final boolean[] conditions = new boolean[4];

    private static final String PRAGMA_TOKEN = "pragma";

    @Override
    public CReadResult read(CReadParams params) {
        if(params.state == CReadState.INITALIZE){
            mType = Type.Unkown;
            Arrays.fill(conditions, false);

            if(params.isToken){
                // handle the token case, we first check it to see whether it is a processor symble.
                if(params.data.equals(PROCESSOR_START)){
                    conditions[SHARAP_SYMBLE] = true;
                    return CReadResult.NEXT_TOKEN;
                }
            }else{
                // handle the line case
                String[] tokens = StringUtils.splits(params.data, " \t");
                if(tokens != null && tokens.length == 4 &&
                        tokens[0].equals(PROCESSOR_START) && tokens[1].equals(PRAGMA_TOKEN) && tokens[3].equals(";")){
                    conditions[0] = true;
                    conditions[1] = true;
                    conditions[3] = true;

                    if(parseValue(tokens[2])){
                        return CReadResult.FINISH;
                    }
                }
            }

            return CReadResult.REJECT;
        }else if(params.state == CReadState.PARSING){
            if(!params.isToken){
                throw new IllegalStateException();  // inner error!.
            }

           if(conditions[PRAGMA_SYMBLE]){
                // "#"->"pragma"->value
                // parsing the value.
                if(parseValue(params.data)){
                    return CReadResult.FINISH;
                }else{
                    return CReadResult.REJECT;
                }
            }else if(conditions[SHARAP_SYMBLE]){
                // "#"->"pragma"
                if(params.data.equals(PRAGMA_TOKEN)){
                    conditions[PRAGMA_SYMBLE] = true;
                    return CReadResult.NEXT_TOKEN;
                }else{
                    return CReadResult.REJECT;
                }
            }else{
                // sharpe symble is fase, it can't hanppen here.
                throw new Error("Innerr error!");
            }
        }else if(params.state == CReadState.OPTION){
            if(conditions[VALUE_SYMBLE]){
                // "#"->"pragma"->value
                // if value had set, it must be the colon symble.
                if((conditions[COLON_SYMBLE] = (params.data.equals(";")))){
                    return CReadResult.FINISH;
                }else{
                    return CReadResult.REJECT;
                }
            }
        }

        return null;
    }

    private boolean parseValue(String data){
        switch (data){
            case "once":
                conditions[VALUE_SYMBLE] = true;
                mType = Type.Once;
                return true;
            default:
                throw new IllegalArgumentException("Unkown pragma value: " + data);

        }
    }

    @Override
    public Object parseValue() {
        return mType;
    }
}
