package jet.opengl.desktop.lwjgl;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jet.opengl.postprocessing.util.Pair;
import jet.opengl.postprocessing.util.StringUtils;

public class UPROPERTYParser {

    private static final class UField{
        String documents;
        String type;
        String name;

        List<Object> uProperties;

        @Override
        public String toString(){
            StringBuilder sb = new StringBuilder();
            if(documents != null){
                sb.append("\t/*");
                boolean skipFirstLine = true;
                String[] tokesn = documents.split("\n");
                for(String s : tokesn){
                    if(!skipFirstLine)
                        sb.append('\t');

                    sb.append(s).append("<br>\n");
                    skipFirstLine = false;
                }
                sb.setLength(sb.length() - 1); // remove the last '\n'
                sb.append("*/\n");
            }

            if(uProperties!= null){
                for(Object property : uProperties){
                    if(property instanceof  CharSequence){
                        // This a string.
                        sb.append("\t@").append(StringUtils.toFirstLetterUpperCase((String)property)).append('\n');
                    }else{
                        // A pair value
                        Pair<Object, Object> pairValue = (Pair<Object, Object>)property;
                        sb.append('\t').append('@').append(StringUtils.toFirstLetterUpperCase((String)pairValue.first)).append('(');

                        if(pairValue.second instanceof CharSequence){
                            // simple string value
                            sb.append("value = "); //append('"').append((String)pairValue.second).append('"');
                            putValue(sb, pairValue.second);
                        }else{  // The second must be a lists.
                            List<Object> subValues = (List<Object>)pairValue.second;
                            StringBuilder hintValues = new StringBuilder();

                            for(Object subValue : subValues){
                                if(subValue instanceof String){
                                    hintValues.append(subValue).append('|');
                                }else{
                                    Pair<Object, Object> pairSubValue = (Pair<Object, Object>)subValue;
                                    sb.append(StringUtils.toFirstLetterUpperCase((String)pairSubValue.first)).append(" = ");
                                    putValue(sb, pairSubValue.second);
                                    sb.append(',');
                                }
                            }

                            if(hintValues.length() > 0){
                                hintValues.setLength(hintValues.length() - 1);
                                sb.append("Hints = ").append(hintValues);
                            }else {
                                sb.setLength(sb.length() - 1); // remove the last ','
                            }
                        }

                        sb.append(')').append('\n');
                    }
                }
            }

            makeStr(sb, type, name);
            return sb.toString();
        }
    }

    private static void putValue(StringBuilder sb, Object value){
        if(value instanceof String){
            sb.append('"').append((String)value).append('"');
        }else if(value instanceof Integer){
            sb.append(value);
        }else if(value instanceof Float){
            sb.append(value).append('f');
        }
    }

    public static void main(String[] args){
//        String s = "EditAnywhere, BlueprintReadWrite, Category=\"Rendering Features\", meta=( Keywords=\"PostProcess\", DisplayName = \"Post Process Materials\" )";
//
//        List<Object> results = parseLine(s);
//        System.out.println(results);

        final String source = getTextFromClipBoard();
        List<UField> files = new ArrayList<>();
        int offset = 0;

        while (offset >= 0 && offset < source.length()){
            offset = StringUtils.firstNotEmpty(source, offset);

            String comments = null;
            List<Object >uProperties = null;
            Pair<String, String> field = null;

            int commentEnd;
            while ((commentEnd = isDocument(source, offset)) > 0){
                comments = parseDocument(source, offset, commentEnd);
                offset  = commentEnd + 2;
                offset = StringUtils.firstNotEmpty(source, offset);
            }

            if(isUPROPERTY(source, offset)){
                uProperties = parseUPROPERTY(source, offset);

                offset=source.indexOf('\n', offset);
                offset = StringUtils.firstNotEmpty(source, offset+1);
            }

            field = parseField(source, offset);
            offset=source.indexOf('\n', offset);

            UField uField = new UField();
            uField.documents = comments;
            uField.name = field.second;
            uField.type = field.first;
            uField.uProperties = uProperties;

            files.add(uField);
        }

        for(UField f:files)
            System.out.println(f);
    }

    // Parsing the UPROPERTY macro
    private static List<Object> parseLine(String line){
        boolean bInToken = false;

        final ArrayList<Object> results = new ArrayList<>();
        StringBuilder token = new StringBuilder();

        for(int i = 0; i < line.length(); i++){
            char c = line.charAt(i);
            if(!bInToken){
                if(Character.isWhitespace(c)){
                    continue;
                }

                token.append(c);
                bInToken = true;
            }else{
                if(c == '='){
                    // This is a pair
                    String keyValue = pop(token);

                    // try to find the '('
                    int index = StringUtils.firstNotEmpty(line, i + 1);
                    if(index < 0)
                        throw new IllegalStateException("Inner error or invalid line: " + line.substring(i+1));

                    if(line.charAt(index) == '('){
                        //ok, we found, then search the end ')'
                        int endBraket = StringUtils.findEndBrackek('(', ')', line, index);
                        List<Object> subValues = parseLine(line.substring(index+1, endBraket));

                        results.add(new Pair<>(keyValue, subValues));

                        // put the index to the next ','
                        int dotIndex = line.indexOf(',', endBraket);
                        if(dotIndex > 0){
                            i = dotIndex+1;
                            bInToken = false;
                            continue;
                        }else{
                            // we reach the end of the line.
                            i = line.length();
                        }

                    }else{ // No brakets, we continue find the ','
                        int dotIndex = line.indexOf(',', index + 1);
                        if(dotIndex < 0){
                            dotIndex = line.length();
                        }

                        String value = line.substring(index, dotIndex).trim();
                        value = value.replace("\"", "");

                        results.add(new Pair<>(keyValue, flatValue(value)));
                        bInToken = false;
                        i = dotIndex;
                    }
                }else if(c == ','){
                    String value = pop(token);
                    results.add(value);
                    bInToken = false;
                }else{
                    token.append(c);
                }
            }
        }

        if(token.length() > 0)
            results.add(pop(token));

        return results;
    }

    private static Object flatValue(String value){
        int iValue = 0;
        boolean isIValue = true;
        try {
            iValue = Integer.parseInt(value);
        }catch (NumberFormatException e){
            isIValue = false;
        }

        if(isIValue){
            return iValue;
        }


        float fValue = 0;
        boolean isFValue = true;
        try {
            fValue = Float.parseFloat(value);
        }catch (NumberFormatException e){
            isFValue = false;
        }

        if(isFValue){
            return fValue;
        }

        // This not a number.
        return value;
    }

    private static boolean isUPROPERTY(String line, int offset){
        return line.startsWith("UPROPERTY", offset);
    }

    private static List<Object> parseUPROPERTY(String souce, int offset){
        int end = souce.indexOf('\n', offset);
        if(end < 0)
            throw new IllegalArgumentException("Invalid UPROPERTY: " + souce.substring(offset));

        int start = souce.indexOf('(', offset);
        end = StringUtils.findEndBrackek('(', ')', souce, start);

        // Drop the UPROPERTY brackeks.
        return parseLine(souce.substring(start+1, end));
    }

    private static Pair<String, String> parseField(String line, int offset){
        int end = line.indexOf(';',offset);
        if(end < 0)
            throw new IllegalArgumentException("Invalid field decalre line: " + line.substring(offset));

        line = line.substring(offset, end).trim();
        if(line.endsWith("1")){
            // may be a boolean value
            int semIndex = line.lastIndexOf(':');
            if(semIndex > 0){
                semIndex = StringUtils.lastNotEmpty(line, semIndex-1);
                int blank = line.lastIndexOf(' ', semIndex);
                return new Pair<>("boolean", line.substring(blank+1, semIndex).trim());
            }
        }

        int blank = line.lastIndexOf(' ');
        return new Pair<>(line.substring(0, blank).trim(), line.substring(blank+1).trim());
    }

    private static int isDocument(String source, int offset){
        if(source.startsWith("/*", offset)){
            int end = source.indexOf("*/", offset + 2);
            return end;
        }

        if(source.startsWith("//", offset)){
            int end  = source.indexOf("\n", offset + 2);
            return end;
        }

        return -1;
    }

    private static String parseDocument(String source, int start, int end){
        String comments = source.substring(start + 2, end);
        return comments;
    }

    private static String pop(StringBuilder source){
        String s = source.toString();
        source.setLength(0);

        return s.trim();
    }

    public static String getTextFromClipBoard() {
        Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        try {
            return (String) t.getTransferData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    private static interface BuildString{
        void makeStr(StringBuilder out, String varname);
    }

    private static final class VecBuildString implements BuildString{
        private final String type;

        public VecBuildString(String type){
            this.type = type;
        }

        @Override
        public void makeStr(StringBuilder out, String varname) {
            out.append("\tpublic final ").append(type).append(' ').append(varname).append(" = new ").append(type).append("();\n");
        }
    }

    static void makeStr(StringBuilder sb, String type, String varname){
        BuildString buildString = OBJ_BUILDER.get(type);
        if(buildString != null){
            buildString.makeStr(sb, varname);
        }else{
            sb.append('\t').append("public ").append(type).append(' ').append(varname).append(';').append('\n');
        }
    }

    private static final HashMap<String, BuildString> OBJ_BUILDER = new HashMap<>();

    static {
        final String[] vectors = {
          "FLinearColor,Vector4f",
          "FVector4,Vector4f",
          "FVector,Vector3f",
          "FVector2D,Vector2f",
        };

        for(String vec : vectors){
            String[] token = StringUtils.split(vec,", ");
            OBJ_BUILDER.put(token[0], new VecBuildString(token[1]));
        }


    }
}
