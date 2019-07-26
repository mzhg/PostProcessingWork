package jet.opengl.demos.gpupro.volumetricfog;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.util.CommonUtil;

final class MaterialSetupCS extends GLSLProgram {

    MaterialSetupCS(String prefix, int threadSize){
        CharSequence source = null;

        try {
            source = ShaderLoader.loadShaderFile(prefix + "MaterialSetupCS.comp", false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ShaderSourceItem item = new ShaderSourceItem(source, ShaderType.COMPUTE);
        item.macros = CommonUtil.toArray(new Macro("THREADGROUP_SIZE", threadSize));

        setSourceFromStrings(item);
    }

    void applyParameters(MaterialSetupParams params, FVolumetricFogIntegrationParameterData integrationParameterData){
        GLSLUtil.setFloat4(this, "FogStruct_ExponentialFogParameters", params.ExponentialFogParameters);
        GLSLUtil.setFloat4(this, "FogStruct_ExponentialFogParameters2", params.ExponentialFogParameters2);
        GLSLUtil.setFloat4(this, "FogStruct_ExponentialFogParameters3", params.ExponentialFogParameters3);
        GLSLUtil.setFloat3(this, "GlobalAlbedo", params.GlobalAlbedo);
        GLSLUtil.setFloat3(this, "GlobalEmissive", params.GlobalEmissive);
        GLSLUtil.setFloat3(this, "View_PreViewTranslation", params.View_PreViewTranslation);
        GLSLUtil.setFloat3(this, "VolumetricFog_GridSize", params.VolumetricFog_GridSize);
        GLSLUtil.setFloat3(this, "VolumetricFog_GridZParams", params.VolumetricFog_GridZParams);
        GLSLUtil.setMat4(this, "g_ViewProj", params.g_ViewProj);
        GLSLUtil.setMat4(this, "UnjitteredClipToTranslatedWorld", params.UnjitteredClipToTranslatedWorld);

        gl.glBindImageTexture(0, integrationParameterData.VBufferARenderTarget.getTexture(),0, false, 0, GLenum.GL_WRITE_ONLY, integrationParameterData.VBufferARenderTarget.getFormat());
        gl.glBindImageTexture(1, integrationParameterData.VBufferBRenderTarget.getTexture(),0, false, 0, GLenum.GL_WRITE_ONLY, integrationParameterData.VBufferBRenderTarget.getFormat());
    }

    void unbind(){
        gl.glBindImageTexture(0, 0,0, false, 0, GLenum.GL_WRITE_ONLY, GLenum.GL_RGBA16F);
        gl.glBindImageTexture(1, 0,0, false, 0, GLenum.GL_WRITE_ONLY, GLenum.GL_RGBA16F);
    }
}
