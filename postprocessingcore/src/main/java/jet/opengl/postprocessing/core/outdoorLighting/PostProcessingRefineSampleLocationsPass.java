package jet.opengl.postprocessing.core.outdoorLighting;

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

    private RenderTechnique g_RefineSampleLocationsProgram = null;
    private SharedData m_sharedData;

    public PostProcessingRefineSampleLocationsPass(SharedData sharedData, boolean autoExposure) {
        super("RefineSampleLocations");

        m_sharedData = sharedData;

        // input0: coordinate texture
        // input1: epipolar cam space Z  or tex2DInscattering
        // input2: tex2DAverageLuminance

        int inputCount = autoExposure ? 3 : 2;
        set(inputCount, 1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_RefineSampleLocationsProgram == null /*|| g_GenerateSliceEndpointsProgram.getProgram() == 0*/){
            g_RefineSampleLocationsProgram = m_sharedData.getRefineSampleLocationsProgram();
        }

        Texture2D output = getOutputTexture(0);
        Texture2D input0 = getInput(0);  // coordinate texture
        Texture2D input1 = getInput(1);  // epipolar cam space z or tex2DInscattering
        Texture2D input2 = getInput(2);  // tex2DAverageLuminance

        output.setName("InterpolationSources");

        context.setProgram(g_RefineSampleLocationsProgram);
        m_sharedData.setUniforms(g_RefineSampleLocationsProgram);

//        context.bindTexture(input0, VolumetricLightingProgram.TEX2D_COORDINATES, m_sharedData.m_psamLinearClamp);
//        context.bindTexture(input1, VolumetricLightingProgram.TEX2D_EPIPOLAR_CAM, m_sharedData.m_psamLinearClamp);

//        m_ptex2DEpipolarInscattering.bind(ScatteringRenderTechnique.TEX2D_SCATTERED_COLOR, m_psamLinearClamp);
//        m_ptex2DCoordinateTexture.bind(ScatteringRenderTechnique.TEX2D_COORDINATES, m_psamLinearClamp);
//        m_ptex2DEpipolarCamSpaceZ.bind(ScatteringRenderTechnique.TEX2D_EPIPOLAR_CAM_SPACE, m_psamLinearClamp);
//        if(m_ptex2DAverageLuminance != null)
//            m_ptex2DAverageLuminance.bind(ScatteringRenderTechnique.TEX2D_AVERAGE_LUMINACE, m_psamLinearClamp);
        context.bindTexture(input0, RenderTechnique.TEX2D_COORDINATES, m_sharedData.m_psamLinearClamp);
        if(m_sharedData.m_ScatteringInitAttribs.m_bRefinementCriterionInsctrDiff){
            context.bindTexture(input1, RenderTechnique.TEX2D_SCATTERED_COLOR, m_sharedData.m_psamLinearClamp);
        }else{
            context.bindTexture(input1, RenderTechnique.TEX2D_EPIPOLAR_CAM_SPACE, m_sharedData.m_psamLinearClamp);
        }

        if(input2 != null)
            context.bindTexture(input2, RenderTechnique.TEX2D_AVERAGE_LUMINACE, 0);

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glBindImageTexture(0, output.getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, output.getFormat());

        gl.glDispatchCompute(m_sharedData.m_ScatteringInitAttribs.m_uiMaxSamplesInSlice/m_sharedData.m_uiSampleRefinementCSThreadGroupSize,
                             m_sharedData.m_ScatteringInitAttribs.m_uiNumEpipolarSlices,
                             1);
        gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        gl.glBindImageTexture(0, 0, 0, false, 0, GLenum.GL_WRITE_ONLY, output.getFormat()); // unbind the image.

        if(m_sharedData.m_CommonFrameAttribs.outputCurrentFrameLog){
            gl.glFlush();
            SharedData.saveTextureAsText(output, "RefineSampleLocationDX.txt");
        }
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
       if(index == 0){
           Texture2D input0 = getInput(0);
           input0.getDesc(out);
           out.format = GLenum.GL_RG16UI;
       }
    }
}
