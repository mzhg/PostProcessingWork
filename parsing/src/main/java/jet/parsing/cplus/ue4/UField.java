package jet.parsing.cplus.ue4;

import java.util.List;

import jet.opengl.postprocessing.util.Pair;
import jet.opengl.postprocessing.util.StringUtils;

public class UField {
    String documents;
    String type;
    String name;
    String modifier = "public";
    String defualtValue = null;

    boolean isParameter;
    List<Object> uProperties;

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        UPROPERTYParser.makeCommentStr(sb, documents);

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
                        UPROPERTYParser.putValue(sb, pairValue.second);
                    }else{  // The second must be a lists.
                        List<Object> subValues = (List<Object>)pairValue.second;
                        StringBuilder hintValues = new StringBuilder();

                        for(Object subValue : subValues){
                            if(subValue instanceof String){
                                hintValues.append(subValue).append('|');
                            }else{
                                Pair<Object, Object> pairSubValue = (Pair<Object, Object>)subValue;
                                sb.append(StringUtils.toFirstLetterUpperCase((String)pairSubValue.first)).append(" = ");
                                UPROPERTYParser.putValue(sb, pairSubValue.second);
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

        UPROPERTYParser.makeStr(sb, type, name);
        return sb.toString();
    }
}
