package jet.opengl.postprocessing.core.volumetricLighting;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;

/**
 * Created by mazhen'gui on 2017/5/17.
 */

final class PostProcessingRefineSampleLocationsPass extends PostProcessingRenderPass{

    private VolumetricLightingProgram g_RefineSampleLocationsProgram = null;
    private SharedData m_sharedData;

    public PostProcessingRefineSampleLocationsPass(SharedData sharedData) {
        super("GenerateSliceEndpoints");

        m_sharedData = sharedData;

        // input0: coordinate texture
        // input1: epipolar cam space Z
        set(2, 1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_RefineSampleLocationsProgram == null /*|| g_GenerateSliceEndpointsProgram.getProgram() == 0*/){
            g_RefineSampleLocationsProgram = m_sharedData.getRefineSampleLocationsProgram();
        }

        Texture2D output = getOutputTexture(0);
        Texture2D input0 = getInput(0);
        Texture2D input1 = getInput(1);

        output.setName("InterpolationSources");

        g_RefineSampleLocationsProgram.enable();
        m_sharedData.setUniforms(g_RefineSampleLocationsProgram);

        context.bindTexture(input0, VolumetricLightingProgram.TEX2D_COORDINATES, m_sharedData.m_psamLinearClamp);
        context.bindTexture(input1, VolumetricLightingProgram.TEX2D_EPIPOLAR_CAM, m_sharedData.m_psamLinearClamp);

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glBindImageTexture(0, output.getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, output.getFormat());

        gl.glDispatchCompute(m_sharedData.m_ScatteringInitAttribs.m_uiMaxSamplesInSlice/m_sharedData.m_uiSampleRefinementCSThreadGroupSize,
                             m_sharedData.m_ScatteringInitAttribs.m_uiNumEpipolarSlices,
                             1);
        gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        gl.glBindImageTexture(0, 0, 0, false, 0, GLenum.GL_WRITE_ONLY, output.getFormat()); // unbind the image.
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
       if(index == 0){
           Texture2D input0 = getInput(0);
           input0.getDesc(out);
           out.format = GLenum.GL_RG16UI;
       }
    }

    @Override
    public void dispose() {
        m_sharedData.dispose();
    }
}
