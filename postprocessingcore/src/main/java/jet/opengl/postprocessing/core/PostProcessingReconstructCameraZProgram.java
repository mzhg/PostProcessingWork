package jet.opengl.postprocessing.core;

import java.io.IOException;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;

/**
 * Created by mazhen'gui on 2017/4/18.
 */

public class PostProcessingReconstructCameraZProgram extends GLSLProgram{

    private int centerIndex = -1;
    private int sampleIndex = -1;

    public PostProcessingReconstructCameraZProgram(boolean enableMSAA) throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/PostProcessingReconstructCamSpaceZPS.frag",
                new Macro("DEPTHLINEARIZE_USEMSAA", enableMSAA));

        enable();
        int iChannel0Loc = getUniformLocation("g_DepthTexture");
        gl.glUniform1i(iChannel0Loc, 0);  // set the texture0 location.
        centerIndex = getUniformLocation("g_Uniforms");
        sampleIndex = getUniformLocation("g_SampleIndex");
    }

    public void setCameraRange(float near, float far){
        gl.glUniform2f(centerIndex, near, far);
    }

    public void setSampleIndex(int idx){
        if(sampleIndex >=0){
            gl.glUniform1i(sampleIndex, idx);
        }
    }
}
