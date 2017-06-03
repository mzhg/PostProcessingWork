package jet.opengl.postprocessing.core.outdoorLighting;

import java.nio.FloatBuffer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.Texture3D;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/6/3.
 */

final class PostProcessingGenerateCoarseUnshadowedInctrPass extends PostProcessingRenderPass{

    private RenderTechnique m_RenderCoarseUnshadowedInsctrProgram = null;
    private SharedData m_sharedData;
    private final Texture2D[] m_RenderTargets = new Texture2D[3];

    public PostProcessingGenerateCoarseUnshadowedInctrPass(SharedData sharedData) {
        super("GenerateCoordinateTexture");

        m_sharedData = sharedData;

//        Uniform [sampler2D name=g_tex2DCoordinates, location=12, value = 3]
//        Uniform [sampler2D name=g_tex2DEpipolarCamSpaceZ, location=13, value = 4]
//        Uniform [sampler3D name=g_tex3DMultipleSctrLUT, location=14, value = 16]
        // input0:  coordinates texture
        // input1:  epipolar cam space z texture.
        // input2:  multiple sctr LUT
        // output0: Inscattering
        // output1: Extinction (Option)
        // output2: EpipolarCamSpaceZ
        int output = (sharedData.m_ScatteringInitAttribs.m_bExtinctionEvalMode ? 1 : 0) +
                     (sharedData.m_ScatteringInitAttribs.m_bRefinementCriterionInsctrDiff ? 1 : 0);
        set(3, output);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(m_RenderCoarseUnshadowedInsctrProgram == null /*|| g_GenerateSliceEndpointsProgram.getProgram() == 0*/){
            m_RenderCoarseUnshadowedInsctrProgram = m_sharedData.getRenderCoarseUnshadowedInsctrProgram();
        }

        Texture2D input0 = getInput(0);
        Texture2D input1 = getInput(1);
        Texture3D input2 = getInput(2);

        Texture2D output0 = getOutputTexture(0);  // Inscattering for refinement pass
        Texture2D output1 = getOutputTexture(1);  // Extinction for unwrap pass

        m_RenderTargets[2] = m_sharedData.getEpipolarImageDSV();
        if(m_sharedData.m_ScatteringInitAttribs.m_bRefinementCriterionInsctrDiff) {
            m_RenderTargets[0] = output0;
        }else {
            m_RenderTargets[0] = null;
        }

        if(m_sharedData.m_ScatteringInitAttribs.m_bExtinctionEvalMode){
            m_RenderTargets[1] = output1 != null ? output1 : output0;
        }else{
            m_RenderTargets[1] = null;
        }

        // binding input textures.
        context.bindTexture(input0, RenderTechnique.TEX2D_COORDINATES, m_sharedData.m_psamLinearClamp);
        context.bindTexture(input1, RenderTechnique.TEX2D_EPIPOLAR_CAM_SPACE, m_sharedData.m_psamLinearClamp);
        context.bindTexture(input2, RenderTechnique.TEX3D_MULTIPLE_LUT, m_sharedData.m_psamLinearClamp);

        output0.setName("tex2DEpipolarInscattering");
        if(output1 != null)
            output1.setName("tex2DEpipolarExtinction");
        context.setViewport(0,0, output0.getWidth(), output0.getHeight());
        context.setVAO(null);
        context.setProgram(m_RenderCoarseUnshadowedInsctrProgram);
        m_sharedData.setUniforms(m_RenderCoarseUnshadowedInsctrProgram);

        context.setBlendState(null);
        m_sharedData.m_pNoDepth_StEqual_KeepStencilDS.backFace.stencilRef = 1;
        m_sharedData.m_pNoDepth_StEqual_KeepStencilDS.frontFace.stencilRef = 1;
        context.setDepthStencilState(m_sharedData.m_pNoDepth_StEqual_KeepStencilDS);
        context.setRasterizerState(null);
        context.setRenderTargets(m_RenderTargets);

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        if(m_sharedData.m_ScatteringInitAttribs.m_bRefinementCriterionInsctrDiff){
            final float fInvalidCoordinate = -1e+30f;
            FloatBuffer invalidCoords = CacheBuffer.wrap(fInvalidCoordinate, fInvalidCoordinate,fInvalidCoordinate,fInvalidCoordinate);
            // Clear both render targets with values that can't be correct projection space coordinates and camera space Z:
            gl.glClearBufferfv(GLenum.GL_COLOR, 0, invalidCoords);
        }

        if(m_sharedData.m_ScatteringInitAttribs.m_bExtinctionEvalMode) {
            FloatBuffer one = CacheBuffer.wrap(1f, 1f, 1f, 1f);
            gl.glClearBufferfv(GLenum.GL_COLOR, 1, one);
        }

        context.drawFullscreenQuad();
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        out.arraySize = 1;
        out.sampleCount = 1;
        out.width = m_sharedData.m_ScatteringInitAttribs.m_uiMaxSamplesInSlice;
        out.height = m_sharedData.m_ScatteringInitAttribs.m_uiNumEpipolarSlices;
        out.format = GLenum.GL_RGB16F;  // 16FP or 32FP
    }
}
