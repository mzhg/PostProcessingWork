package jet.opengl.postprocessing.core.ssao;

import java.io.IOException;
import java.nio.FloatBuffer;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.util.CachaRes;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017-05-11 14:50:22.
 */

final class PostProcessingHBAOProgram extends GLSLProgram{

    private int centerIndex = -1;

    public PostProcessingHBAOProgram(int deinterleaved, int blur) throws IOException {
        CharSequence vertSrc = ShaderLoader.loadShaderFile("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", false);
        CharSequence geomSrc = null;
        if(gl.isSupportExt("GL_NV_geometry_shader_passthrough")){
            geomSrc = ShaderLoader.loadShaderFile("shader_libs/PostProcessingHBAOGS_NV.geom", false);
        }else{
            geomSrc = ShaderLoader.loadShaderFile("shader_libs/PostProcessingHBAOGS.geom", false);
        }

        CharSequence fragSrc = ShaderLoader.loadShaderFile("shader_libs/PostProcessingHBAOPS.frag", false);

        ShaderSourceItem vs_item = new ShaderSourceItem(vertSrc, ShaderType.VERTEX);
        ShaderSourceItem gs_item = new ShaderSourceItem(geomSrc, ShaderType.GEOMETRY);
        ShaderSourceItem ps_item = new ShaderSourceItem(fragSrc, ShaderType.FRAGMENT);

        Macro[] macros = {
            new Macro("AO_LAYERED", 2),
            new Macro("AO_DEINTERLEAVED", deinterleaved),
            new Macro("AO_BLUR", blur)
        };
        ps_item.macros = macros;
        setSourceFromStrings(vs_item, gs_item, ps_item);
        enable();
        setTextureUniform("g_LinearDepthTex", 0);
        setTextureUniform("g_ViewNormalTex", 1);
        setTextureUniform("g_TexRandom", 2);
        centerIndex = getUniformLocation("g_Uniforms");
        if(GLCheck.CHECK)
            GLCheck.checkError("PostProcessingHBAOProgram::init()");
    }

    @CachaRes
    public void setUniform(float offsetX, float offsetY, float jitterX, float jitterY, float jitterZ, float jitterW){
        FloatBuffer buffer = CacheBuffer.getCachedFloatBuffer(8);
        buffer.put(offsetX).put(offsetY).put(0).put(0);
        buffer.put(jitterX).put(jitterY).put(jitterZ).put(jitterW);
        buffer.flip();

        gl.glUniform4fv(centerIndex, buffer);
    }
}
