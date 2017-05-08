package jet.opengl.postprocessing.shader;

import java.io.IOException;

/**
 * Created by mazhen'gui on 2017/4/18.
 */

public class VisualDepthTextureProgram extends GLSLProgram{

    private int uniformIndex;

    public VisualDepthTextureProgram(boolean array) throws IOException {
        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/PostProcessingDebugVisualDepthTex.frag",
                new Macro("SHADOW_MAP_ARRAY", array ? 1 : 0));
        enable();
        int iChannel0Loc = getUniformLocation("g_shadowMap");
        gl.glUniform1i(iChannel0Loc, 0);  // set the texture0 location.

        uniformIndex = getUniformLocation("g_Uniforms");
    }

//    #define g_lightZNear    g_Uniforms.x
//    #define g_lightZFar     g_Uniforms.y
//    #define g_slice         g_Uniforms.z
//    #define g_scalerFactor  g_Uniforms.w
    public void setUniforms(float lightZNear, float lightZFar, float textureArraySlice, float scalerFactor){
        gl.glUniform4f(uniformIndex, lightZNear, lightZFar, textureArraySlice, scalerFactor);
    }
}
