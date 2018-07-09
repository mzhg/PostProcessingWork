package jet.parsing.cplus.reader;

public final class CommentReader implements CReader{

    private static final String SINGLE_COMMENT_START = "//";

    private static final String DOUBLE_COMMNET_STAT = "/*";
    private static final String DOUBLE_COMMNET_END = "/";

    private final StringBuilder mComment = new StringBuilder();

    private int mState = 0;  // 0, READY; 1, PARSING; 2, FINISHED

    @Override
    public boolean accept(String token) {
        mComment.setLength(0);
        mState = 0;
        return token.startsWith(SINGLE_COMMENT_START) || token.startsWith(DOUBLE_COMMNET_STAT);
    }

    @Override
    public CReadResult read(ReaderContext context, String line, String[] reminder) {
        if(mState == 0){
            if(line.startsWith(SINGLE_COMMENT_START)){

            }
        }

        return null;
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
