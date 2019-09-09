package jet.parsing.cplus.ue4;

import org.lwjgl.util.vector.ReadableVector4f;
import org.omg.CORBA.PUBLIC_MEMBER;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import jet.opengl.postprocessing.util.Pair;
import jet.opengl.postprocessing.util.StringUtils;

public class UPROPERTYParser {

    static void putValue(StringBuilder sb, Object value){
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

    // Parsing the UPROPERTY macro or the paramters list in the method decla.
    static List<Object> parseLine(String line){
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

    static int isDocument(String source, int offset){
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

    static String parseDocument(String source, int start, int end){
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
        void makeStr(StringBuilder out, String varname, String defualtValue);

        void makeArrayStr(StringBuilder out, String varname, String size);

        default String toReadOnlyType(){
            return getType();
        }

        default String toReadWriteType(){
            return getType();
        }

        default String getType(){ return null;}
    }

    private static final class VecBuildString implements BuildString{
        private final String type;
        private final String readType;

        public VecBuildString(String type, String readType){
            this.type = type;
            this.readType = readType;
        }

        @Override
        public void makeStr(StringBuilder out, String varname, String defualtValue) {
            out.append("\tpublic final ").append(type).append(' ').append(varname).append(" = new ").append(type).append("(");
            if(defualtValue != null)
                out.append(defualtValue);
            out.append(");\n");
        }

        @Override
        public void makeArrayStr(StringBuilder out, String varname, String size) {
            out.append("\tpublic final ").append(type).append("[] ").append(varname).append(" = new ").append(type).append("[").append(size).append("];\n");
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public String toReadOnlyType() {
            return readType;
        }
    }

    private static final class ValueBuildString implements  BuildString{
        private final String type;

        public ValueBuildString(String type){
            this.type = type;
        }

        @Override
        public void makeStr(StringBuilder out, String varname, String defualtValue) {
            out.append("\tpublic ").append(type).append(' ').append(varname);

            if(defualtValue != null)
                out.append(" = ").append(defualtValue);

            out.append(";\n");
        }

        @Override
        public void makeArrayStr(StringBuilder out, String varname, String size) {
            out.append("\tpublic final ").append(type).append("[] ").append(varname).append(" = new ").append(type).append("[").append(size).append("];\n");
        }

        @Override
        public String getType() {
            return type;
        }
    }

    public static void makeStr(StringBuilder sb, String type, String varname){
        BuildString buildString = OBJ_BUILDER.get(type);
        if(buildString != null){
            buildString.makeStr(sb, varname, null);
        }else{
            sb.append('\t').append("public ").append(type).append(' ').append(varname).append(';').append('\n');
        }
    }

    public static void makeStr(StringBuilder sb, String type, String varname, String defualtValue){
        BuildString buildString = OBJ_BUILDER.get(type);
        if(buildString != null){
            buildString.makeStr(sb, varname, defualtValue);
        }else{
            sb.append('\t').append("public ").append(type).append(' ').append(varname).append(';').append('\n');
        }
    }

    public static void makeArrayStr(StringBuilder sb, String type, String varname, String size){
        BuildString buildString = OBJ_BUILDER.get(type);
        if(buildString != null){
            buildString.makeArrayStr(sb, varname, size);
        }else{
            sb.append("\tpublic final ").append(type).append("[] ").append(varname).append(" = new ").append(type).append("[").append(size).append("];\n");
        }
    }

    public static void makeReadType(StringBuilder sb, String type){
        BuildString buildString = OBJ_BUILDER.get(type);
        if(buildString != null){
            sb.append(buildString.toReadOnlyType());
        }else{
            sb.append(type);
        }
    }

    public static void makeReadWriteType(StringBuilder sb, String type){
        BuildString buildString = OBJ_BUILDER.get(type);
        if(buildString != null){
            sb.append(buildString.toReadWriteType());
        }else{
            sb.append(type);
        }
    }

    static void makeCommentStr(StringBuilder sb, String commnets){
        if(commnets != null){
            sb.append("\t/**");
            String[] tokesn = commnets.split("\n");
            for(String s : tokesn){
                s = s.trim();
                if(s.length() == 0)
                    continue;

                if(s.charAt(0) != '*'){
                    sb.append("\t* ");
                }

                sb.append(s).append("\n");
            }
            sb.setLength(sb.length() - 1); // remove the last '\n'
            sb.append("*/\n");
        }
    }

    public static String getJavaType(String type){
        BuildString bs = OBJ_BUILDER.get(type);
        if(bs != null){
            return bs.getType();
        }else{
            return type;
        }
    }

    private static final HashMap<String, BuildString> OBJ_BUILDER = new HashMap<>();

    public static boolean isBaseType(String type){
        return Arrays.binarySearch(BASE_TYPES, type) >= 0;
    }

    public static boolean isIngoreType(String type){
        return Arrays.binarySearch(INGORE_TYPES, type) >= 0;
    }

    private static final String [] BASE_TYPES = {
            "int",
            "boolean",
            "byte",
            "char",
            "short",
            "float",
            "long",
            "double"
    };

    private static final String[] INGORE_TYPES = {
         "FRHICommandList"
    };

    static {
        Arrays.sort(BASE_TYPES);
        Arrays.sort(INGORE_TYPES);
    }

    static {

        final String[] vectors = {
          "FLinearColor,Vector4f, ReadableVector4f",
          "FVector4,Vector4f, ReadableVector4f",
          "FVector,Vector3f, ReadableVector3f",
          "FVector2D,Vector2f, ReadableVector2f",
          "FMatrix,Matrix4f, Matrix4f",
          "FIntPoint,Vector2i,Vector2i",
          "TArray,ArrayList,List",
          "FIntRect,Recti,Recti",
        };

        final String[] values = {
            "int32,int",
            "uint32,int",
            "bool,boolean",
            "ERHIFeatureLevel::Type,int",
        };

        for(String vec : vectors){
            String[] token = StringUtils.split(vec,", ");
            OBJ_BUILDER.put(token[0], new VecBuildString(token[1], token[2]));
        }

        for(String vec : values){
            String[] token = StringUtils.split(vec,", ");
            OBJ_BUILDER.put(token[0], new ValueBuildString(token[1]));
        }
    }
}
