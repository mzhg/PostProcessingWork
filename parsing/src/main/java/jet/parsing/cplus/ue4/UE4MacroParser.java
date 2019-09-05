package jet.parsing.cplus.ue4;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.util.StringUtils;

public class UE4MacroParser {

    private static final class UMember{
        String macro;
        String[] tokens;

        UMember(String macro, String[] tokens){
            this.macro = macro;
            this.tokens = tokens;
        }
    }

    public static void main(String[] args){
        String source = UPROPERTYParser.getTextFromClipBoard();
        printMembers(readMembers(source));
    }

    public static List<UMember> readMembers(String source){
        final List<UMember> results = new ArrayList<>();
        final List<String> lines = StringUtils.splitToLines(source);

        for(String line : lines){
            int start = line.indexOf('(');
            int end = line.lastIndexOf(')');

            if(start < 0 || end < 0)
                continue;

            String macro = line.substring(0, start).trim();
            String token = line.substring(start+1, end);
            String[] tokens = StringUtils.split(token, ",");

            results.add(new UMember(macro, tokens));
        }

        return results;
    }

    private static final String VIEW_MEMBER = "VIEW_UNIFORM_BUFFER_MEMBER";
    private static final String VIEW_MEMBER_EX = "VIEW_UNIFORM_BUFFER_MEMBER_EX";
    private static final String VIEW_MEMBER_ARRAY = "VIEW_UNIFORM_BUFFER_MEMBER_ARRAY";
    private static final String SHADER_SAMPLER = "SHADER_PARAMETER_SAMPLER";
    private static final String SHADER_TEXTURE = "SHADER_PARAMETER_TEXTURE";
    private static final String SHADER_SRV = "SHADER_PARAMETER_SRV";
    private static final String SHADER_PARAM = "SHADER_PARAMETER";
    private static final String SHADER_PARAM_EX = "SHADER_PARAMETER_EX";
    private static final String SHADER_PARAM_ARRAY = "SHADER_PARAMETER_ARRAY";

    public static void printMembers(List<UMember> members){
        StringBuilder sb = new StringBuilder(1024);

        for(UMember member : members){
            switch (member.macro){
                case VIEW_MEMBER:
                case VIEW_MEMBER_EX:
                case VIEW_MEMBER_ARRAY:
                case SHADER_PARAM:
                case SHADER_PARAM_EX:
                case SHADER_PARAM_ARRAY:
                {
                    String type = member.tokens[0].trim();
                    String name = member.tokens[1].trim();

                    if(member.macro.equals(VIEW_MEMBER_ARRAY) || member.macro.equals(SHADER_PARAM_ARRAY)){
                        int start = member.tokens[2].indexOf('[');
                        int end = member.tokens[2].lastIndexOf(']');
                        String size = member.tokens[2].substring(start+1, end).trim();

                        UPROPERTYParser.makeArrayStr(sb, type, name, size);
                    }else{
                        UPROPERTYParser.makeStr(sb, type, name);
                    }

                    break;
                }

                case SHADER_TEXTURE:
                {
                    String type = member.tokens[0].trim();
                    String name = member.tokens[1].trim();

                    int dot = type.indexOf('<');
                    if(dot > 0)
                        type = type.substring(0,dot);

                    UPROPERTYParser.makeStr(sb, type, name);
                    break;
                }

                case SHADER_SRV:
                {
                    UPROPERTYParser.makeStr(sb, "BufferGL", member.tokens[1].trim());
                    break;
                }

                case SHADER_SAMPLER:
                {
                    break;
                }

                default:
                    throw new IllegalStateException("Invalid token: " + member.macro);
            }
        }

        System.out.println(sb);
    }
}
