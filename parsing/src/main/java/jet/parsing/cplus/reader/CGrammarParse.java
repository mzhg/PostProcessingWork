package jet.parsing.cplus.reader;

import jet.parsing.util.StringUtils;

/**
 * Created by Administrator on 2018/7/12 0012.
 */

public class CGrammarParse {
    private String source;
    private int cursor;
    private boolean hasToken;
    private String token;

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

    private String[] keywords = KEYWORDS_CPLUS;


    public CGrammarParse(){}

    public CGrammarParse(String source){
        setSource(source);
    }

    public void setSource(String source){
        this.source = source;
        this.cursor = 0;
        this.hasToken = false;
    }

    public String nextLine(){
        if(cursor == -1 || cursor == source.length()){
            return null;
        }

        int start = cursor;
        int end = StringUtils.firstSpecifedCharacter(source, "\n", cursor);
        if(end > 0){
            cursor = end;
            hasToken = false;
            return source.substring(start, end);
        }else{
            hasToken = false;
            cursor = source.length();
            return source.substring(start);
        }
    }

    public boolean hasNext(){
        if(!hasToken) {
            return _nextToken();
        }

        return hasToken;
    }

    public String nextToken(){
        if(hasToken){
            hasToken = false;
            return token;
        }else{
            boolean result = _nextToken();
            return result ? token : null;
        }
    }

    private boolean _nextToken(){
        cursor = StringUtils.firstNonSpaceCharacter(source, cursor);
        if(cursor == -1 || cursor == source.length()){
            return false;
        }

        char c = source.charAt(cursor);
        if(Character.isLetter(c) || c == '_'){
            int end = StringUtils.firstSpecifedCharacter(source," \n\t+-*/", c + 1);
            if(end >0){
                token = source.substring(cursor, end);
                cursor = end;
                hasToken = true;
            }else{
                token = source.substring(cursor);
                cursor = source.length();
                hasToken = true;
            }

            return true;
        }else if(isOperator(c)){
            char next = source.charAt(cursor + 1);
            if(isOperator(next)){
                token = source.substring(cursor, cursor + 2);
                cursor += 2;
                hasToken = true;
            }else{
                token = Character.toString(c);
                cursor ++;
                hasToken = true;
            }

            return true;
        }else if(c == '#'){
            token = "#";
            cursor ++;
            hasToken = true;

            return true;
        }

        return false;
    }

    private static boolean isOperator(char c){
        if(c == '+' || c == '-' || c == '*' || c == '/' || c == '='){
            return true;
        }

        return false;
    }
}
