package jet.opengl.postprocessing.core.dof;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;
import java.nio.FloatBuffer;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/5/8.
 */

final class PostProcessingDOFSetupProgram extends GLSLProgram {

    private int centerIndex = -1;
    private int matIndex = -1;

    PostProcessingDOFSetupProgram(boolean bInFarBlur, boolean bInNearBlur ) throws IOException {
        final int FAR_BLUR = bInFarBlur?1:0;
        final int NEAR_BLUR = bInNearBlur?1:0;

        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/PostProcessingDOFSetupPS.frag",
                new Macro("FAR_BLUR", FAR_BLUR), new Macro("NEAR_BLUR", NEAR_BLUR), new Macro("MRT_COUNT", FAR_BLUR + NEAR_BLUR));
        enable();

        setTextureUniform("g_Input0", 0);
        setTextureUniform("g_Input1", 1);

        centerIndex = getUniformLocation("g_Uniforms");
        matIndex = getUniformLocation("g_ScreenToWorld");
    }

    // g_Uniforms[0].x : g_DepthOfFieldFocalDistance
// g_Uniforms[0].y : g_DepthOfFieldFocalRegion
// g_Uniforms[0].z : g_DepthOfFieldNearTransitionRegion
// g_Uniforms[0].w : g_DepthOfFieldFarTransitionRegion

// g_Uniforms[1].x : g_DepthOfFieldScale
// g_Uniforms[1].y : g_NearPlane
// g_Uniforms[1].z : g_FarPlane

    // g_Uniforms[2].xy : The dimension of the g_Input0
// g_Uniforms[2].zw : The dimension of the g_Input1 (DepthTex)
    public void setUniform(float textureWidth, float textureHeight, float znear, float zfar,
                           float focalDistance, float focalRegion, float nearTransitionRegion,
                           float farTransitionRegion, float fieldScale){
        FloatBuffer buffer = CacheBuffer.getCachedFloatBuffer(12);
        buffer.put(focalDistance);
        buffer.put(focalRegion);
        buffer.put(nearTransitionRegion);
        buffer.put(farTransitionRegion);

        buffer.put(fieldScale);
        buffer.put(znear);
        buffer.put(zfar);
        buffer.put(0.0f);

        buffer.put(textureWidth);
        buffer.put(textureHeight);
        buffer.put(textureWidth);  // Assume the dimension of scene color texture as same as scene depth texture's.
        buffer.put(textureHeight);

        buffer.flip();
        gl.glUniform4fv(centerIndex, buffer);
    }

    public void setScreenToWorld(Matrix4f mat){
        gl.glUniform4fv(matIndex, CacheBuffer.wrap(Matrix4f.IDENTITY));
    }
}
