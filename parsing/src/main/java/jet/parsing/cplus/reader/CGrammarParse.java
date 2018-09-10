package jet.parsing.cplus.reader;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.Arrays;

/**
 * Created by Administrator on 2018/7/12 0012.
 */

public class CGrammarParse {
    private String stoken;
    private double vtoken;
    private CToken.Type type;
    private StreamTokenizer tokenizer;

    private int mState; // 0 means unkown; 1, has token; 2,

    private static final String[] KEYWORDS_CPLUS = {
        "_asm", "auto", "bool", "break", "case", "catch", "char", "class", "const", "const_cast",
        "continue", "default", "delete", "do", "double", "dynamic_cast", "else", "enum", "explicit",
            "export", "extern", "false", "float", "for", "friend", "goto", "if", "inline", "int",
            "long", "mutable", "namespace", "new", "operator", "private", "protected", "public",
            "register", "reinterpret_cast", "return", "short", "signed", "sizeof", "static",
            "static_cast", "struct", "switch", "template", "this", "throw", "true", "try", "typedef",
            "typeid", "typename", "union", "unsigned", "using", "virtual", "void", "volatile", "wchar_t",
            "while"
    };

    static {
        Arrays.sort(KEYWORDS_CPLUS);
    }

    public static boolean isKeyWord(String word){
        return Arrays.binarySearch(KEYWORDS_CPLUS, word) >= 0;
    }

    public CGrammarParse(String source){
        tokenizer = new StreamTokenizer(new StringReader(source));
        tokenizer.slashStarComments(false);
        tokenizer.slashSlashComments(false);
        tokenizer.ordinaryChar('/');
    }

    public boolean hasNext(){
        if(mState == 0){
            // we need check to see whether it has next token
            checkNextToken();
            return mState == 1;
        }else if(mState == 2){
            // has reach the end
            return false;
        }else{  // mState == 1
            return true ;
        }
    }

    private void checkNextToken(){
        if(mState != 0){
            throw new IllegalStateException();
        }
        try {
            if(tokenizer.nextToken() != StreamTokenizer.TT_EOF){
                switch (tokenizer.ttype){
                    case StreamTokenizer.TT_EOL:

                        break;
                    case StreamTokenizer.TT_NUMBER:
                        vtoken = tokenizer.nval;
                        type = CToken.Type.DIGIT;
                        break;
                    case StreamTokenizer.TT_WORD:
                        stoken = tokenizer.sval;
                        type = CToken.Type.STRNG;
                        break;
                    default:
                        stoken = Character.toString((char)tokenizer.ttype);
                        type = CToken.Type.CHARACTOR;
                        break;
                }

                mState = 1;
            }else{
                mState = 2;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CToken nextToken(){
        if(mState == 0){
            checkNextToken();
        }else if(mState == 2){
            return null;
        }

        if(mState == 1){
            mState = 0;  // we unkown the next token type.
            return new CToken(stoken, vtoken, type);
        }else{
            // mState must be 2.
            return null;
        }
    }

    private static boolean isOperator(char c){
        if(c == '+' || c == '-' || c == '*' || c == '/' || c == '='){
            return true;
        }

        return false;
    }

    public static void main(String[] args){
        CGrammarParse parse = new CGrammarParse("public static int firstNonSpecifedCharacter(String str, char c/**/);");
        CToken token;
        while (parse.hasNext()){
            System.out.println(parse.nextToken());
        }
    }
}
