package jet.opengl.demos.Unreal4.volumetricfog;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.util.CommonUtil;

final class VolumetricFogFinalIntegrationCS extends GLSLProgram {

    VolumetricFogFinalIntegrationCS(String prefix, int threadSize){
        CharSequence source = null;

        try {
            source = ShaderLoader.loadShaderFile(prefix + "FinalIntegrationCS.comp", false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ShaderSourceItem item = new ShaderSourceItem(source, ShaderType.COMPUTE);
        item.macros = CommonUtil.toArray(new Macro("THREADGROUP_SIZE", threadSize));
        setSourceFromStrings(item);
    }
}
