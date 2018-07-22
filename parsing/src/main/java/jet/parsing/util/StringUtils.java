package jet.parsing.util;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Level;

/**
 * Created by Administrator on 2018/7/12 0012.
 */

public final class StringUtils {

    private StringUtils(){}

    public static int firstNonSpaceCharacter(String str, int from){
        if(from >= str.length())
            throw new IllegalArgumentException();

        for(int i = from; i < str.length(); i++){
            final char c = str.charAt(i);
            if(!Character.isWhitespace(c)){
                return i;
            }
        }

        return -1;
    }

    public static int firstNonSpaceCharacter(String str){
        return firstNonSpaceCharacter(str, 0);
    }

    public static int firstNonSpecifedCharacter(String str, char c, int from){
        if(from >= str.length())
            throw new IllegalArgumentException();

        for(int i = from; i < str.length(); i++){
            if(str.charAt(i) != c)
                return i;
        }

        return -1;
    }

    public static int firstNonSpecifedCharacter(String str, char c){
        return firstNonSpecifedCharacter(str, c, 0);
    }

    public static int lastNonSpecifedCharacter(String str, char c, int from){
        if(from >= str.length())
            throw new IllegalArgumentException();

        for(int i = from; i>=0; i--){
            if(str.charAt(i) != c)
                return i;
        }

        return -1;
    }

    public static int lastNonSpecifedCharacter(String str, char c){
        return lastNonSpecifedCharacter(str, c, str.length() - 1);
    }

    public static String[] splits(String str, String tokens){
        StringTokenizer tokenizer = new StringTokenizer(str, tokens);
        if(!tokenizer.hasMoreTokens()){
            return null;
        }

        ArrayList<String> list = new ArrayList<>();

        while (tokenizer.hasMoreTokens()){
            list.add(tokenizer.nextToken());
        }

        return list.toArray(new String[list.size()]);
    }
}
