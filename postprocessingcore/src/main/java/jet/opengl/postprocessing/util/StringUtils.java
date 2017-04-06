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
        for(int i = 0; i < str.length(); i++){
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
}
