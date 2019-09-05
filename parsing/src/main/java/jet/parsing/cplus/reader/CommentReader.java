package jet.parsing.cplus.reader;

import jet.opengl.postprocessing.util.StringUtils;

public final class CommentReader implements CReader{

    private static final String SINGLE_COMMENT_START = "//";

    private static final String DOUBLE_COMMNET_STAT = "/*";
    private static final String DOUBLE_COMMNET_END = "*/";

    private final StringBuilder mComment = new StringBuilder();
    private static CommentReader instance;

    private int mState;  // 0: unkown; 1, single comment; 2, double comment

    public static CommentReader getInstance(){
        if(instance == null)
            instance = new CommentReader();

        return instance;
    }

    private CommentReader(){}

    @Override
    public CReadResult read(CReadParams params) {
        if(params.state == CReadState.INITALIZE){
            // Start to parsing the comments. First we need check whether the given token or line is comment.
            mComment.setLength(0);
            boolean result;
            if(params.isToken){
                result = checkStartToken(params.data);
            }else{
                result = checkStartLine(params.data);
            }

            if(!result){
                // If it is not comment, we simply refuse the work.
                return CReadResult.REJECT;
            }else{
                if(!params.isToken){
                    if(isSingle()){
                        // if the given data is a line and a single comment, we receive it
                        int firstIndex = StringUtils.firstNonSpecifedCharacter(params.data, '/');
                        if(firstIndex < 0)
                            throw new Error("Inner Error!  firstIndex < " + firstIndex);

                        mComment.append(params.data.substring(firstIndex));
                        return CReadResult.FINISH;
                    }else if(isDouble()){
                        int endIndex = params.data.indexOf(DOUBLE_COMMNET_END, 2);
                        if(endIndex > 0){
                            mComment.append(params.data.substring(DOUBLE_COMMNET_STAT.length(), endIndex));
                            return CReadResult.FINISH;
                        }else{
                            mComment.append(params.data.substring(DOUBLE_COMMNET_STAT.length())).append('\n');
                            return CReadResult.NEXT_LINE;
                        }
                    }else{
                        throw new Error("Inner error!");
                    }
                }else{
                    return CReadResult.NEXT_LINE;
                }
            }
        }else if(params.state == CReadState.PARSING){
            // we continue parsing the comments.
            if(isSingle()){
                throw new Error("Inner error!");
            }else if(isDouble()){
                int endIndex = params.data.indexOf(DOUBLE_COMMNET_END, 2);
                if(endIndex > 0){
                    mComment.append(params.data.substring(DOUBLE_COMMNET_STAT.length(), endIndex));
                    return CReadResult.FINISH;
                }else{
                    mComment.append(params.data.substring(DOUBLE_COMMNET_STAT.length())).append('\n');
                    return CReadResult.NEXT_LINE;
                }
            }else{
                throw new Error("Inner error!");
            }
        }

        throw new Error("Inner error!");
    }

    private boolean isSingle(){
        return mState == 1;
    }

    private boolean isDouble(){
        return mState == -1;
    }

    private boolean checkStartToken(String token){
        if(token.equals(SINGLE_COMMENT_START)){
            mState = 1;
            return true;
        }else if(token.equals(DOUBLE_COMMNET_STAT)){
            mState = -1;
            return true;
        }else if(token.equals(DOUBLE_COMMNET_END)){
            mState = 0;
            return true;
        }else{
            return false;
        }

    }

    private boolean checkStartLine(String line){
        if(line.startsWith(SINGLE_COMMENT_START)){
            mState = 1;
            return true;
        }else if(line.startsWith(DOUBLE_COMMNET_STAT)){
            mState = -1;
            return true;
        }else{
            return false;
        }
    }

    @Override
    public CReaderType getType() {
        return CReaderType.COMMENT;
    }

    @Override
    public Object parseValue() {
        return mComment.toString();
    }

}
