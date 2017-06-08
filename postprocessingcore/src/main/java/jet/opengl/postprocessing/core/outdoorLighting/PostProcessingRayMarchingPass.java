package jet.opengl.postprocessing.core.outdoorLighting;

import java.nio.FloatBuffer;

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
 * Created by mazhen'gui on 2017/6/5.
 */

final class PostProcessingRayMarchingPass extends PostProcessingRenderPass {

    private SharedData m_sharedData;
    private final int m_uiMaxStepsAlongRay;
    private final int m_iCascadeIndex;

    private RenderTechnique g_DoRayMarchProgram;
    private final Texture2D[] m_RenderTargets = new Texture2D[2];

    private static final int CASCADE_PROCESSING_MODE_SINGLE_PASS = 0 ;//CascadePassMode.SINGLE.ordinal();
    private static final int CASCADE_PROCESSING_MODE_MULTI_PASS = 1; //CascadePassMode.MULTI.ordinal();
    private static final int CASCADE_PROCESSING_MODE_MULTI_PASS_INST = 2; //CascadePassMode.MULTI_INST.ordinal();

    public PostProcessingRayMarchingPass(SharedData sharedData, int uiMaxStepsAlongRay, int iCascadeIndex) {
        super("DoRayMarching");

        m_sharedData = sharedData;
        m_uiMaxStepsAlongRay = uiMaxStepsAlongRay;
        m_iCascadeIndex = iCascadeIndex;

//        m_ptex2DEpipolarCamSpaceZ.bind(ScatteringRenderTechnique.TEX2D_EPIPOLAR_CAM_SPACE, m_psamLinearClamp);    // 0
//        m_ptex2DCoordinateTexture.bind(ScatteringRenderTechnique.TEX2D_COORDINATES, m_psamLinearClamp);  // 1
//        FrameAttribs.ptex2DShadowMapSRV.bind(ScatteringRenderTechnique.TEX2D_SHADOW_MAP, m_psamComparison);  // 2
//        m_ptex2DMinMaxShadowMap[0].bind(ScatteringRenderTechnique.TEX2D_MIN_MAX_LIGHT_DEPTH, m_psamLinearClamp); // 3
//        m_ptex2DSliceUVDirAndOrigin.bind(ScatteringRenderTechnique.TEX2D_SLICE_UV_ORIGIN, m_psamLinearClamp);  // 4
//        m_ptex3DSingleScatteringSRV.bind(ScatteringRenderTechnique.TEX3D_SINGLE_LUT, m_psamLinearClamp);   // 5
//        m_ptex3DHighOrderScatteringSRV.bind(ScatteringRenderTechnique.TEX3D_HIGH_ORDER_LUT, m_psamLinearClamp);  //6
//        m_ptex3DMultipleScatteringSRV.bind(ScatteringRenderTechnique.TEX3D_MULTIPLE_LUT, m_psamLinearClamp);  // 7
        set(8, 1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_DoRayMarchProgram == null /*|| g_GenerateSliceEndpointsProgram.getProgram() == 0*/){
            g_DoRayMarchProgram = m_sharedData.getRayMarchProgram();
        }

        Texture2D output = getOutputTexture(0);
//        m_ptex2DCameraSpaceZSRV.bind(RenderTechnique.TEX2D_CAM_SPACEZ, m_psamLinearClamp);
//        m_ptex2DCoordianteTextureSRV.bind(RenderTechnique.TEX2D_COORDINATES, m_psamLinearClamp);
//        m_ptex2DSliceUVDirAndOriginSRV.bind(RenderTechnique.TEX2D_SLICE_UV_ORIGIN, m_psamLinearClamp);
//        frameAttribs.ptex2DShadowMapSRV.bind(RenderTechnique.TEX2D_SHADOWM_MAP, m_psamComparison);
//        m_ptex2DMinMaxShadowMapSRV[0].bind(RenderTechnique.TEX2D_MIN_MAX_DEPTH, m_psamLinearClamp);
//        if(frameAttribs.ptex2DStainedGlassSRV != null)
//            frameAttribs.ptex2DStainedGlassSRV.bind(RenderTechnique.TEX2D_STAINED_DEPTH, m_psamLinearClamp);
//        if(m_ptex2DPrecomputedPointLightInsctrSRV != null)
//            m_ptex2DPrecomputedPointLightInsctrSRV.bind(RenderTechnique.TEX2D_PRECOMPUTED_INSCTR, m_psamLinearClamp);
        Texture2D ptex2DCameraSpaceZSRV = getInput(0);
        Texture2D ptex2DCoordianteTextureSRV = getInput(1);
        Texture2D ptex2DSliceUVDirAndOriginSRV = getInput(4);
        Texture2D ptex2DShadowMapSRV = getInput(2);
        Texture2D ptex2DMinMaxShadowMapSRV = getInput(3);

        Texture3D ptex3DSingleScatteringSRV = getInput(5);
        Texture3D ptex3DHighOrderScatteringSRV = getInput(6);
        Texture3D ptex3DMultipleScatteringSRV = getInput(7);

        // binding textures.
        int m_psamLinearClamp = m_sharedData.m_psamLinearClamp;
        context.bindTexture(ptex2DCameraSpaceZSRV, RenderTechnique.TEX2D_EPIPOLAR_CAM_SPACE, m_psamLinearClamp);
        context.bindTexture(ptex2DCoordianteTextureSRV, RenderTechnique.TEX2D_COORDINATES, m_psamLinearClamp);
        context.bindTexture(ptex2DSliceUVDirAndOriginSRV, RenderTechnique.TEX2D_SLICE_UV_ORIGIN, m_psamLinearClamp);
        context.bindTexture(ptex2DShadowMapSRV, RenderTechnique.TEX2D_SHADOW_MAP, m_sharedData.m_psamComparison);
        context.bindTexture(ptex2DMinMaxShadowMapSRV, RenderTechnique.TEX2D_MIN_MAX_LIGHT_DEPTH, m_psamLinearClamp);
        context.bindTexture(ptex3DSingleScatteringSRV, RenderTechnique.TEX3D_SINGLE_LUT, m_psamLinearClamp);
        context.bindTexture(ptex3DHighOrderScatteringSRV, RenderTechnique.TEX3D_HIGH_ORDER_LUT, m_psamLinearClamp);
        context.bindTexture(ptex3DMultipleScatteringSRV, RenderTechnique.TEX3D_MULTIPLE_LUT, m_psamLinearClamp);

        SMiscDynamicParams MiscDynamicParams = m_sharedData.m_MiscDynamicParams;
        MiscDynamicParams.fMaxStepsAlongRay = m_uiMaxStepsAlongRay;
        MiscDynamicParams.fCascadeInd = m_iCascadeIndex;

        output.setName("RayMarchingTexture");
        context.setViewport(0,0, output.getWidth(), output.getHeight());
//        context.setViewport(0,0, 1280, 720);
        context.setVAO(null);
        context.setProgram(g_DoRayMarchProgram);
        m_sharedData.setUniforms(g_DoRayMarchProgram, MiscDynamicParams);

        context.setBlendState(null);
        m_RenderTargets[0] = output;
        m_RenderTargets[1] = m_sharedData.getEpipolarImageDSV();
        context.setRenderTargets(m_RenderTargets);

        FloatBuffer zeros = CacheBuffer.wrap(0f,0,0,0);
        GLFuncProviderFactory.getGLFuncProvider().glClearBufferfv(GLenum.GL_COLOR, 0, zeros);


        // Depth stencil view now contains 2 for these pixels, for which ray marchings is to be performed
        // Depth stencil state is configured to pass only these pixels and discard the rest
        m_sharedData.m_pNoDepth_StEqual_KeepStencilDS.backFace.stencilRef = 2;
        m_sharedData.m_pNoDepth_StEqual_KeepStencilDS.frontFace.stencilRef = 2;
        context.setDepthStencilState(m_sharedData.m_pNoDepth_StEqual_KeepStencilDS);
        context.setRasterizerState(null);

        int iNumInst = 0;
        if( m_sharedData.m_ScatteringInitAttribs.m_bEnableLightShafts )
        {
            switch(m_sharedData.m_ScatteringInitAttribs.m_uiCascadeProcessingMode)
            {
                case CASCADE_PROCESSING_MODE_SINGLE_PASS:
                case CASCADE_PROCESSING_MODE_MULTI_PASS:
                    iNumInst = 1;
                    break;
                case CASCADE_PROCESSING_MODE_MULTI_PASS_INST:
                    iNumInst = m_sharedData.m_CommonFrameAttribs.cascadeShadowMapAttribs.numCascades - m_sharedData.m_ScatteringInitAttribs.m_iFirstCascade;
                    break;
            }
        }
        else
        {
            iNumInst = 1;
        }

        context.drawArrays(GLenum.GL_TRIANGLES, 0, 3, iNumInst);
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        out.arraySize = 1;
        out.sampleCount = 1;
        out.width = m_sharedData.getEpipolarImageDSV().getWidth();
        out.height = m_sharedData.getEpipolarImageDSV().getHeight();
        out.format = GLenum.GL_RGB16F;
    }
}
