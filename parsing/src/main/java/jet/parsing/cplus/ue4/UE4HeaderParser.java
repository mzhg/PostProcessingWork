package jet.parsing.cplus.ue4;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.util.StringUtils;

public class UE4HeaderParser {

    public static void main(String[] args){
        String source = UPROPERTYParser.getTextFromClipBoard();


    }

    private static List<Object> parseSourceCode(String source){
        List<Object> results = new ArrayList<>();

        int offset = 0;
        final int length = source.length();

        String globalModifier = "public";
        List<String> comments = new ArrayList<>();
        List<String> processers = new ArrayList<>();  // #if

        while (offset < length && offset >=0){
            offset = StringUtils.firstNotEmpty(source, offset);

            // Parseing the global modifer.
            while (isModifer(source, offset)){
                globalModifier = getModifer(source, offset);
                offset +=globalModifier.length() + 1;
                offset = StringUtils.firstNotEmpty(source, offset);
            }

            // Parsing the comments.
            int commentEnd;
            while ((commentEnd = UPROPERTYParser.isDocument(source, offset)) > 0){
                String comment = UPROPERTYParser.parseDocument(source, offset, commentEnd);
                comments.add(comment);

                offset  = commentEnd + 2;
                offset = StringUtils.firstNotEmpty(source, offset);
            }

            // parsing the processors.
            while (isProcessor(source, offset)){
                offset  = source.indexOf('\n', offset); // go the line end
                if(offset < 0)
                    offset = source.length();

                offset = StringUtils.firstNotEmpty(source, offset);
            }

            // parsing the method.

        }

        return results;
    }

    private static boolean isModifer(String source, int offset){
        if(source.startsWith("public", offset)){
            return true;
        }

        if(source.startsWith("protected", offset)){
            return true;
        }

        if(source.startsWith("private", offset)){
            return true;
        }

        return false;
    }

    private static String getModifer(String source, int offset){
        int end = source.indexOf(':', offset);
        if(end < 0)
            throw new IllegalStateException("Inner error");

        return source.substring(offset, end);
    }

    private static boolean isProcessor(String source, int offset){
        if(source.startsWith("#", offset)){
            offset = StringUtils.firstNotEmpty(source, offset);

            if(source.startsWith("if",offset) || source.startsWith("else",offset) || source.startsWith("endif",offset)){
                return true;
            }

            if(source.startsWith("#include")){
                // unable to parsint the include processor.
                int end  = source.indexOf('\n', offset);
                if(end < 0)
                    end = source.length();

                throw new IllegalStateException("Unable to handle the include procssor : "+ source.substring(offset, end));
            }
        }

        return false;
    }

    private static int isMethod(String source, int offset){
        int leftBrackt = source.
    }
}
