package jet.opengl.postprocessing.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by mazhen'gui on 2017/4/6.
 */

public class StringUtils {
    /**
     * Make the first letter of the string by <i>str</i> to upper case.<p>
     * For example: "abc" -> "Abc".
     * @param str the source string
     * @return the result.
     * @throws NullPointerException if <i>str</i> is null.
     * @throws IndexOutOfBoundsException if the string is empty.
     */
    public static String toFirstLetterUpperCase(String str){
        char l = str.charAt(0);
        if(Character.isUpperCase(l))
            return str;

        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    public static String safeStr(String str){
        return str == null ? "" : str;
    }

    public static int findFirstUpperCaseLetter(CharSequence str){
        if(str == null || str.length() == 0)
            return -1;

        int length = str.length();
        for(int i = 0; i < length; i++){
            char c = str.charAt(i);
            if(c >= 'A' && c <='Z'){
                return i;
            }
        }

        return -1;
    }

    public static boolean isFirstLetterUpperCase(String str){
        return Character.isUpperCase(str.charAt(0));
    }

    public static String[] split(String source, String charSet){
        StringTokenizer token = new StringTokenizer(source, charSet);
        List<String> list = new ArrayList<String>();
        while(token.hasMoreTokens()){
            list.add(token.nextToken());
        }

        return list.toArray(new String[list.size()]);
    }

    public static final boolean isEmpty(CharSequence str){
        if(str == null || str.length() ==0)
            return true;
        return false;
    }

    public static final boolean isBlank(CharSequence str){
        return isBlank(str, 0);
    }

    public static final boolean isBlank(CharSequence str, int offset){
        if(str == null || str.length() ==0)
            return true;
        if(offset < 0) offset = 0;
        if(offset >= str.length())
            throw new IllegalArgumentException("The offset is large than the string length. The length is " + str.length() + ", the offset is " + offset);

        for(int i = offset; i < str.length(); i++){
            char c = str.charAt(i);
            if(c != ' ' && c != '\t' && c != '\n')
                return false;
        }

        return true;
    }

    public static final int firstNotEmpty(CharSequence str){
        return firstNotEmpty(str, 0);
    }

    public static final int firstNotEmpty(CharSequence str, int offset){
        for(int i = offset; i < str.length(); i++){
            char c = str.charAt(i);
            if(c != ' ' && c != '\t' && c != '\n')
                return i;
        }

        return -1;
    }

    public static final int lastNotEmpty(CharSequence str){
        return lastNotEmpty(str, str.length()-1);
    }

    public static final int lastNotEmpty(CharSequence str, int offset){
        for(int i = Math.min(str.length() - 1, offset); i >= 0 ; i--){
            char c = str.charAt(i);
            if(c != ' ' && c != '\t' && c != '\n')
                return i;
        }

        return -1;
    }

    public static final boolean startWith(CharSequence src, int src_offset, CharSequence tag, int tag_offset){
        if(src_offset < 0) src_offset = 0;
        if(tag_offset < 0) tag_offset = 0;

        int len = Math.min(src.length() - src_offset, tag.length() - tag_offset);
        if(len <= 0) return false;

        for(int i = 0; i < len; i++){
            char s = src.charAt(src_offset + i);
            char t = tag.charAt(tag_offset + i);
            if(s != t){
                return false;
            }
        }

        return true;
    }

    public static List<String> splitToLines(String str){
        int pre = 0;
        int length = str.length();
        ArrayList<String> lines = new ArrayList<String>(32);

        for(int i=0; i < length; i ++){
            char c = str.charAt(i);
            if(c == '\n'){
                lines.add(str.substring(pre, i));
//				System.out.println(pre + ", " + i);
                pre = i + 1;
            }
        }

        if(pre < length){
            lines.add(str.substring(pre, length));
        }

        lines.trimToSize();
        return lines;
    }

    public static String filterCharater(String source, String chars){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < source.length(); i++){
            char c = source.charAt(i);
            if(chars.indexOf(c) <0)
                sb.append(c);
        }

        return sb.toString();
    }

    /**
     * Return the index of the end bracket
     * @param src
     * @param offset
     * @return
     */
    public static int findEndBrackek(char startBracket, char endBrackt, CharSequence src, int offset){
        int index = offset;
        int level = 0;
        boolean found = false;

        if(startBracket == endBrackt)
            throw new IllegalArgumentException();

        for(; index < src.length(); index++){
            char c = src.charAt(index);
            if(c == startBracket){
                level ++;
                found = true;
            }else if(c == endBrackt){
                level --;

                if(level < 0)
                    throw new IllegalArgumentException("Invalid source: " + src);

                if(level == 0){
                    if(!found){  // Can't happend
                        throw new IllegalStateException("Inner error!");
                    }

                    return index;
                }
            }
        }

        // No found the end because the bad source.
        if(found)
            throw new IllegalArgumentException("Invalid source: " + src);

        return -1;
    }

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

    public static int firstSpaceCharacter(String str, int from){
        if(from >= str.length())
            throw new IllegalArgumentException();

        for(int i = from; i < str.length(); i++){
            final char c = str.charAt(i);
            if(Character.isWhitespace(c)){
                return i;
            }
        }

        return -1;
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

    public static int firstSpecifedCharacter(String str, String charSet, int from){
        if(from >= str.length())
            throw new IllegalArgumentException();

        for(int i = from; i < str.length(); i++){
            final char c = str.charAt(i);
            for(int j = 0; j < charSet.length(); j++){
                if(c == charSet.charAt(j)){
                    return i + 1;
                }
            }
        }

        return -1;
    }
}
