package jet.opengl.postprocessing.core.dof;

import java.io.IOException;
import java.nio.FloatBuffer;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/5/8.
 */

final class PostProcessingDOFRecombineProgram extends GLSLProgram {

    private int centerIndex = -1;

    PostProcessingDOFRecombineProgram(boolean bInFarBlur, boolean bInNearBlur ) throws IOException {
        final int FAR_BLUR = bInFarBlur?1:0;
        final int NEAR_BLUR = bInNearBlur?1:0;
        setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/PostProcessingDOFRecombinePS.frag",
                new Macro("FAR_BLUR", FAR_BLUR), new Macro("NEAR_BLUR", NEAR_BLUR));
        enable();

        setTextureUniform("g_Input0", 0);
        setTextureUniform("g_DepthTex", 1);
        if(bInFarBlur)
            setTextureUniform("g_Input1", 2);  // far blurred tex
        if(bInNearBlur)
            setTextureUniform("g_Input2", 3);  // near blurred tex.

        centerIndex = getUniformLocation("g_Uniforms");
    }

    public void setUniform(float textureWidth, float textureHeight, float znear, float zfar,
                           float focalDistance, float focalRegion, float nearTransitionRegion,
                           float farTransitionRegion){
        if(centerIndex < 0) return;

        FloatBuffer buffer = CacheBuffer.getCachedFloatBuffer(8);
        buffer.put(focalDistance);
        buffer.put(focalRegion);
        buffer.put(nearTransitionRegion);
        buffer.put(farTransitionRegion);

        buffer.put(znear).put(zfar);
        buffer.put(textureWidth).put(textureHeight).flip();

        gl.glUniform4fv(centerIndex, buffer);
    }
}
