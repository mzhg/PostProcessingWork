package jet.opengl.renderer.Unreal4.lgi;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;

final class FLightGridCompactCS extends GLSLProgram {
    FLightGridCompactCS(String prefix, int threadGroupSize, int GMaxNumReflectionCaptures){
        CharSequence source = null;

        try {
            source = ShaderLoader.loadShaderFile(prefix + "LightGridCompactCS.comp", false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ShaderSourceItem item = new ShaderSourceItem(source, ShaderType.COMPUTE);
        item.macros = new Macro[]{
                new Macro("THREADGROUP_SIZE", threadGroupSize),
                new Macro("MAX_CAPTURES", GMaxNumReflectionCaptures),
        };
        setSourceFromStrings(item);
    }
}
