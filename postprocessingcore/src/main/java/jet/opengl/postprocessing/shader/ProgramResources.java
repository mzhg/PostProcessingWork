package jet.opengl.postprocessing.shader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mazhen'gui on 2017/7/14.
 */

public class ProgramResources {
    /** The properties of active uniform variables for program. */
    public UniformProperty[] active_uniform_properties;
    public final List<UniformBlockProperties> uniformBlockProperties = new ArrayList<>();

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder(512);

        if(active_uniform_properties!= null && active_uniform_properties.length > 0){
            out.append("ACTIVE UNIFORMS: \n");
            for(UniformProperty property : active_uniform_properties){
                out.append(property.toString()).append('\n');
            }
        }

        if(uniformBlockProperties.size() > 0){
            for(UniformBlockProperties property : uniformBlockProperties){
                out.append(property.toString()).append('\n');
            }
        }
        return out.toString();
    }
}
