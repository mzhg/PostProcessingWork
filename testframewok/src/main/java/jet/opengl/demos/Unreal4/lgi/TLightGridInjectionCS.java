package jet.opengl.demos.Unreal4.lgi;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLException;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;

final class TLightGridInjectionCS extends GLSLProgram {

    TLightGridInjectionCS(String prefix, int threadGroupSize, boolean bLightLinkedListCulling){
        CharSequence source = null;

        try {
            source = ShaderLoader.loadShaderFile(prefix + "LightGridInjectionCS.comp", false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ShaderSourceItem item = new ShaderSourceItem(source, ShaderType.COMPUTE);
        item.macros = new Macro[]{
                new Macro("THREADGROUP_SIZE", threadGroupSize),
                new Macro("USE_LINKED_CULL_LIST", bLightLinkedListCulling?1:0),
        };
        setSourceFromStrings(item);
    }
}
