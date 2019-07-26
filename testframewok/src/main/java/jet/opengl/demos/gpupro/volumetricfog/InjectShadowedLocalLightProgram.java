package jet.opengl.demos.gpupro.volumetricfog;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;

final class InjectShadowedLocalLightProgram extends GLSLProgram {
    InjectShadowedLocalLightProgram(String prefx){
        try {
            setSourceFromFiles(prefx+"WriteToBoundingSphereVS.vert", prefx+"InjectShadowedLocalLightPS.frag", null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
