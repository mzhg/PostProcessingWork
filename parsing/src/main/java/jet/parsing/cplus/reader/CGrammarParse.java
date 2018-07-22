package jet.parsing.cplus.reader;

/**
 * Created by Administrator on 2018/7/12 0012.
 */

public class CGrammarParse {
    private String source;
    private int cursor;

    private static final String[] KEYWORDS_CPLUS = {
        "_asm", "auto", "bool", "break", "case", "catch", "char", "class", "const", "const_cast",
        "continue", "default", "delete", "do", "double", "dynamic_cast", "else", "enum", "explicit",
            ""
    };

    private String[] keywords;


    public CGrammarParse(){}

    public CGrammarParse(String source){
        setSource(source);
    }

    public void setSource(String source){
        this.source = source;
        cursor = 0;
    }

    public boolean hasNext(){
        return false;
    }

    public String nextToken(){
        return null;
    }
}
