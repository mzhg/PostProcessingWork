package jet.opengl.renderer.Unreal4.volumetricfog;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;

final class VolumetricFogShadingProgram extends GLSLProgram {

    VolumetricFogShadingProgram(String prefix){
        try {
            Macro[] macros = {
//                    new Macro("DYNAMICALLY_SHADOWED", bDynamicallyShadowed?1:0),
//                    new Macro("INVERSE_SQUARED_FALLOFF", bInverseSquared?1:0),
//                    new Macro("USE_TEMPORAL_REPROJECTION", bTemporalReprojection?1:0),
            };

            setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", prefix+"ShadingFrag.frag", macros);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
