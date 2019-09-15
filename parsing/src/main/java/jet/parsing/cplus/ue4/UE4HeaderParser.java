package jet.parsing.cplus.ue4;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jet.opengl.postprocessing.util.DebugTools;
import jet.opengl.postprocessing.util.StringUtils;

public class UE4HeaderParser {

    public static void main(String[] args){
        String source = UPROPERTYParser.getTextFromClipBoard();

        List<Object> results = parseSourceCode(source);
        printMembers(results);
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
            while (offset >= 0 && isModifer(source, offset)){
                globalModifier = getModifer(source, offset);
                offset +=globalModifier.length() + 1;
                offset = StringUtils.firstNotEmpty(source, offset);
            }

            // Parsing the comments.
            int commentEnd;
            while (offset >= 0 && (commentEnd = UPROPERTYParser.isDocument(source, offset)) > 0){
                String comment = UPROPERTYParser.parseDocument(source, offset, commentEnd);
                comments.add(comment);

                offset  = commentEnd + 2;
                offset = StringUtils.firstNotEmpty(source, offset);
            }

            // parsing the processors.
            while (offset >= 0 && isProcessor(source, offset)){
                offset  = source.indexOf('\n', offset); // go the line end
                if(offset < 0)
                    offset = source.length();

                offset = StringUtils.firstNotEmpty(source, offset);
            }

            // parsing the methods.
            int methodEnd;
            while (offset >= 0 && (methodEnd = isMethod(source, offset)) > 0){
                UMethod method = parsingMethod(source, offset, methodEnd);

                method.modifier = globalModifier;
                method.documents = combineComments(comments);
                results.add(method);
                offset = StringUtils.firstNotEmpty(source, methodEnd+1);
            }

            int fieldEnd;
            while (offset >= 0 && (fieldEnd = isField(source, offset)) > 0 ){
                UField field = parseingField(source, offset, fieldEnd);

                field.modifier = globalModifier;
                field.documents = combineComments(comments);
                results.add(field);
                offset = StringUtils.firstNotEmpty(source, fieldEnd+1);
            }
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
        if(isModifer(source, offset) || isComments(source, offset) || isProcessor(source, offset))
            return -1;

        final int leftBrackt = StringUtils.indexOfWithComments(source, offset, '(');
        if(leftBrackt < 0)
            return -1;

        final int end = StringUtils.indexOfWithComments(source, offset, ';');

        if(end < 0 || leftBrackt < end){
            // We got a method.
            final int rightBrackt = StringUtils.findEndBrackek('(', ')',source, leftBrackt);
            if(rightBrackt < 0)
                throw new IllegalArgumentException("Invalid source");

            final int startBrackt = StringUtils.indexOfWithComments(source, rightBrackt+1, '{');
            if(startBrackt > 0 && startBrackt < end){
                // may be the body of the method. But need to check.
                String token = StringUtils.removeComments(source.substring(rightBrackt + 1, startBrackt)).trim();
                if(token.isEmpty() || token.equals("const")){

                    // Yes we found a method.
                    return StringUtils.findEndBrackek('{','}', source, startBrackt) + 1;
                }
            }else{
                // There is no '{', may be it's a pure virtual method.
                if(end < 0){
                    // no ';' found
                    return -1;
                }else{
                    if(rightBrackt + 1 == end){
                        // This is no space between the ');'
                        return end;
                    }

                    String token = StringUtils.removeComments(source.substring(rightBrackt + 1, end)).trim();
                    token = StringUtils.removeBlank(token);  // remove all of the blanks

                    if(token.isEmpty() || token.equals("=0") || token.equals("const=0")){
                        return end;
                    }
                }
            }

        }

        return -1;
    }

    private static UMethod parsingMethod(String source, int offset, int end){
        final String methodContent = StringUtils.removeComments(source.substring(offset, end)).trim();

        final int leftBrackt = StringUtils.indexOfWithComments(methodContent, 0, '(');
        if(leftBrackt < 0)
            throw new IllegalStateException("Inner error!");

        // Get the method name
        int blankIndex = StringUtils.lastSpecifedCharacter(methodContent, " >", leftBrackt-1);
        if(blankIndex < 0 )
            throw new IllegalStateException("Inner error!");

        String methodName = StringUtils.removeComments(methodContent.substring(blankIndex+1, leftBrackt));

        // Get the type name
        String typeStr = methodContent.substring(0, blankIndex).trim();

        boolean isInline = typeStr.contains("inline");
        boolean isVoid = typeStr.contains("void");
        boolean isSingleWorld = typeStr.indexOf(' ') > 0 || typeStr.indexOf('<') > 0|| typeStr.indexOf('>') > 0;
        boolean isVirtual = typeStr.contains("virtual");

        // Get the parameters
        final int rightBrackt = StringUtils.findEndBrackek('(', ')',methodContent, leftBrackt);
        final String paramContent = methodContent.substring(leftBrackt+1, rightBrackt).trim();
        List<UField> uFields = parsingParameters(paramContent);

        // Get the body of method.
        int startBracket = methodContent.indexOf('{');
        String methodBody = null;
        if(startBracket < 0){
            // no body
        }else{
            int endBracket = StringUtils.findEndBrackek('{', '}', methodContent, startBracket);
            methodBody = methodContent.substring(startBracket+1, endBracket);
        }

        UMethod method = new UMethod();
        method.body = methodBody;
        method.returnType = typeStr;
        method.parameters = uFields;
        method.name = methodName;
        return method;
    }

    private static List<UField> parsingParameters(String paramContent){
        if(StringUtils.isBlank(paramContent) || paramContent.equals("void")){
            return Collections.emptyList();
        }

        List<UField> uFields = new ArrayList<>();

        int lastCursor = 0;
        int cursor = 0;
        while (cursor < paramContent.length() && cursor >= 0){
            cursor = StringUtils.firstNotEmpty(paramContent, cursor);

            // first we need to find the single paramter token
            String token = null;
            boolean foundEqual = false;
            for(int i = cursor; i < paramContent.length(); i++){
                final char c = paramContent.charAt(i);

                if(c == '='){
                    if(foundEqual)
                        throw new IllegalArgumentException("Invalid paramContent: " + paramContent);
                    foundEqual = true;
                }else if(c == '(' || c =='<'){
                    final char endChar = c == '(' ? ')' : '>';
                    // go to the end
                    int endBrackek = StringUtils.findEndBrackek(c, endChar, paramContent, i);
                    if(endBrackek < 0)
                        throw new IllegalArgumentException("Invalid paramContent: " + paramContent.substring(i));

                    i = endBrackek +1;
                    continue;
                }else if(c == ','){
                    // we got a token, stopping the loop
                    cursor = i+1;

                    token = paramContent.substring(lastCursor, i).trim();
                    lastCursor = cursor;
                    break;
                }
            }

            if(token == null){
                int blankIndex = paramContent.lastIndexOf(' ');
                if(blankIndex > lastCursor && blankIndex > cursor){
                    token = paramContent.substring(lastCursor);
                    cursor = paramContent.length(); // goto the end
                }
            }

            // parsing the parameter
            if(token != null){
                String defualtValue = null;

                if(foundEqual){
                    // Parsing the value
                    String[] tokens = StringUtils.split(token, "=");
                    defualtValue = tokens[1].trim();

                    token = tokens[0].trim();

                    if(StringUtils.isBlank(token))
                        throw new IllegalStateException("Inner error!");
                }

                // parsing the type and varname
                String type;
                String name;

                char splitChar = ' ';
                int blankIndex = token.lastIndexOf(splitChar);
                if(blankIndex < 0){
                    // It's a generic type
                    blankIndex = token.lastIndexOf('>');

                    if(blankIndex < 0)
                        throw new IllegalStateException("Invalid parameter: " + token);
                }

                type = token.substring(0, blankIndex).trim();
                name = token.substring(blankIndex+1).trim();

                UField uField = new UField();
                uField.documents = null;
                uField.isParameter = true;
                uField.modifier= null;
                uField.name = name;
                uField.type = type;
                uField.defualtValue = defualtValue;

                uFields.add(uField);
            }
        }

        return uFields;
    }

    static boolean isComments(String source, int offset){
        if(source.startsWith("/*", offset)){
           return true;
        }

        if(source.startsWith("//", offset)){
            return true;
        }

        return false;
    }

    private static int isField(String source, int offset){
        if(isModifer(source, offset) || isComments(source, offset) || isProcessor(source, offset))
            return -1;

        if(isMethod(source, offset) >= 0)
            return -1;


        int end = StringUtils.indexOfWithComments(source,offset,';');
        if(end < 0)
            return -1;

        return end;
    }

    private static UField parseingField(String source, int offset, int end){
        final String fieldContent = StringUtils.removeComments(source.substring(offset, end)).trim();

        String defualtValue = null;

        String token = fieldContent;
        int foundEqual = fieldContent.indexOf('=');
        if(foundEqual > 0){
            // Parsing the value
            String[] tokens = StringUtils.split(fieldContent, "=");
            defualtValue = tokens[1].trim();

            token = tokens[0].trim();

            if(StringUtils.isBlank(token))
                throw new IllegalStateException("Inner error!");
        }

        // parsing the type and varname
        String type;
        String name;

        char splitChar = ' ';
        int blankIndex = token.lastIndexOf(splitChar);
        if(blankIndex < 0){
            // It's a generic type
            blankIndex = token.lastIndexOf('>');

            if(blankIndex < 0)
                throw new IllegalStateException("Invalid parameter: " + token);
        }

        type = token.substring(0, blankIndex).trim();
        name = token.substring(blankIndex+1).trim();

        UField uField = new UField();
        uField.name = name;
        uField.type = type;
        uField.defualtValue = defualtValue;

        return uField;
    }

    private static String combineComments(List<String> comments){
        if(comments.isEmpty())
            return null;

        StringBuilder sb = new StringBuilder();
        for(String source : comments){
            String[] tokens = StringUtils.split(source, "\n");
            for(String s : tokens){
                int startIndex = StringUtils.firstNotEmpty(s);
                if(startIndex < 0)
                    continue;

                if(s.startsWith("* ", startIndex)){
                    sb.append(s.substring(startIndex+2));
                }else{
                    sb.append(s.substring(startIndex));
                }

                sb.append('\n');
            }
        }

        if(sb.length() > 0){
            sb.setLength(sb.length());
        }

        comments.clear();
        return sb.toString();
    }

    private static void printMembers(List<Object> members){
        for (Object m : members){
            if(m instanceof  UField){
                System.out.println(makeFieldStr((UField)m));
            }else{
                System.out.println(makeMethodStr((UMethod)m));
            }
        }
    }

    private static String makeFieldStr(UField field){
        StringBuilder sb = new StringBuilder();
        UPROPERTYParser.makeCommentStr(sb, field.documents);
        UPROPERTYParser.makeStr(sb, field.type, field.name, field.defualtValue);

        return sb.toString();
    }

    private static String makeMethodStr(UMethod method){
        StringBuilder sb = new StringBuilder();
        UPROPERTYParser.makeCommentStr(sb, method.documents);
        sb.append('\t');
        if(method.modifier != null)
            sb.append(method.modifier).append(' ');


        if(!StringUtils.isBlank(method.returnType)) {
//            sb.append(method.returnType);
            UPROPERTYParser.makeReadWriteType(sb, method.returnType);
            sb.append(' ');
        }

        sb.append(method.name).append('(');

        FieldTypeDesc desc = new FieldTypeDesc();

        if(method.parameters != null){
            for(UField field : method.parameters){
                parsingFileType(field.type, desc);
                if(UPROPERTYParser.isIngoreType(desc.javaType)){
                    // TODO
                }else{
                    sb.append(makeParameterTypeStr(desc)).append(' ').append(field.name);
                    if(field.defualtValue != null){
                        sb.append("/* = ").append(field.defualtValue).append("*/");
                    }

                    sb.append(',');
                    sb.append(' ');
                }


            }

            if(!method.parameters.isEmpty())
                sb.setLength(sb.length() - 2);

            sb.append(')');
        }

        sb.append('{').append('\n');
        if(!StringUtils.isEmpty(method.body) && !StringUtils.isBlank(method.body)){
            sb.append(method.body);
        }else{
            sb.append("\t\tthrow new UnsupportedOperationException();\n\t");
        }

        sb.append('}').append('\n');

        return sb.toString();
    }

    private static final class FieldTypeDesc{
        String typeSource;

        String cType;
        String javaType;

        int pointerLevel;
        boolean isReference;
        boolean isInnerConstant;
        boolean isOutConstant;

        String[] templateTypes;

        void reset(){
            typeSource = null;
            cType = null;
            javaType = null;
            pointerLevel = 0;
            isOutConstant = false;
            isInnerConstant = false;
            isReference = false;
        }
    }

    private static boolean parsingFileType(String type, FieldTypeDesc outDesc){
        type = type.trim();

        outDesc.reset();
        outDesc.typeSource = type;

        final String CONST = "const";
        if(type.startsWith(CONST)){
            outDesc.isOutConstant = true;

            type = type.substring(CONST.length()).trim();
        }

        final String CLASS = "class";
        if(type.startsWith(CLASS)){
            // remove the class modifer.
            type = type.substring(CLASS.length()).trim();
        }

        // next it must be a type
        char currentChar = type.charAt(0);
        if(StringUtils.isProgrammingLanguageFieldValidStartChar(currentChar)){
            int index = 1;
            for(; index < type.length(); index ++){
                currentChar = type.charAt(index);

                if(currentChar == '<'){
                    // template in the type.
                    int start = index;
                    int end = StringUtils.findEndBrackek('<','>', type, start);

                    List<Object> templatesType = UPROPERTYParser.parseLine(type.substring(start+1, end));
                    outDesc.templateTypes = new String[templatesType.size()];
                    for(int l = 0; l < templatesType.size(); l++){
                        outDesc.templateTypes[l] = (String)templatesType.get(l);
                    }

                    index = end + 1;
                    continue;
                }else if(!(Character.isLetterOrDigit(currentChar) || currentChar == '_' || currentChar == ':')){
                    break;
                }
            }

            String cType = type.substring(0, index);
            outDesc.javaType = UPROPERTYParser.getJavaType(cType);

            while (index < type.length()){
                currentChar = type.charAt(index);

                if(currentChar == '*'){
                    outDesc.pointerLevel ++;
                    index ++;
                }else if(currentChar == '&'){
                    outDesc.isReference = true;
                    index++;
                }else if(currentChar == 'c'){
                    if(type.startsWith(CONST, index)){
                        if(outDesc.isInnerConstant)
                            throw new IllegalStateException("invalid field type: " + outDesc.typeSource);
                        outDesc.isInnerConstant = true;

                        index += CONST.length();
                    }else if(type.startsWith(CLASS, index)){
                        index += CLASS.length();
                    }
                }else if(currentChar == ' '){
                    index = StringUtils.firstNotEmpty(type, index);
                }else if(currentChar == ':'){
                    index ++;
                }
            }
        }else{
            throw new IllegalStateException("Unable to parse the type: " + type);
        }

        return true;
    }

    private static CharSequence makeParameterTypeStr(FieldTypeDesc desc){
        StringBuilder sb = new StringBuilder();
        if(desc.isInnerConstant || desc.isOutConstant) {
            UPROPERTYParser.makeReadType(sb, desc.javaType);
        }else{
            sb.append(desc.javaType);
        }

        if(desc.templateTypes != null && desc.templateTypes.length > 0){
            sb.append('<');
            sb.append(desc.templateTypes[0]);
            sb.append('>');
        }

        return sb;
    }

}
