package jet.opengl.postprocessing.core.volumetricLighting;

import java.nio.FloatBuffer;

import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.core.RenderTexturePool;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017-05-18 18:38:48.
 */

final class PostProcessingBuild1DMinMaxMipMapPass extends PostProcessingRenderPass{

    private VolumetricLightingProgram m_RenderSliceUVDirInSMProgram = null;
    private VolumetricLightingProgram m_InitializeMinMaxShadowMapProgram = null;
    private VolumetricLightingProgram m_ComputeMinMaxSMLevelProgram = null;

    private SharedData m_sharedData;
    private final Texture2D[] m_ptex2DMinMaxShadowMapSRV = new Texture2D[2];
    private SMiscDynamicParams m_MiscDynamicParams = new SMiscDynamicParams();

    public PostProcessingBuild1DMinMaxMipMapPass(SharedData sharedData) {
        super("Build1DMinMaxMipMap");

        m_sharedData = sharedData;

        // input0: CamSpaceZ
        // input1: SliceEndPoints
        // input2: LightSpaceDepthBuffer
        // output0: SliceUVDirAndOrigin
        // output1: MinMaxShadowMap
        set(3, 2);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        renderSliceUVDirInShadowMap(context);
        build1DMinMaxMipMap(context);
    }

    private void renderSliceUVDirInShadowMap(PostProcessingRenderContext context){
        if(m_RenderSliceUVDirInSMProgram == null /*|| g_GenerateSliceEndpointsProgram.getProgram() == 0*/){
            m_RenderSliceUVDirInSMProgram = m_sharedData.getSliceUVDirInSMProgram();
        }

        Texture2D ptex2DCameraSpaceZSRV = getInput(0);
        Texture2D ptex2DSliceEndpointsSRV = getInput(1);

        Texture2D output = getOutputTexture(0);
        output.setName("SliceUVDirAndOriginTexture");
        context.setViewport(0,0, output.getWidth(), output.getHeight());
//        context.setViewport(0,0, 1280, 720);
        context.setVAO(null);
        context.setProgram(m_RenderSliceUVDirInSMProgram);
        m_sharedData.setUniforms(m_RenderSliceUVDirInSMProgram);

//        m_ptex2DCameraSpaceZSRV.bind(RenderTechnique.TEX2D_CAM_SPACEZ, m_psamLinearClamp);
//        m_ptex2DSliceEndpointsSRV.bind(RenderTechnique.TEX2D_SLICE_END_POINTS, m_psamLinearClamp);
        context.bindTexture(ptex2DCameraSpaceZSRV, VolumetricLightingProgram.TEX2D_CAM_SPACEZ, m_sharedData.m_psamLinearClamp);
        context.bindTexture(ptex2DSliceEndpointsSRV, VolumetricLightingProgram.TEX2D_SLICE_END_POINTS, m_sharedData.m_psamLinearClamp);

        context.setBlendState(null);
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTarget(output);

        FloatBuffer clearColors = CacheBuffer.wrap(-10000f, -10000, 0,0);
        GLFuncProviderFactory.getGLFuncProvider().glClearBufferfv(GLenum.GL_COLOR, 0, clearColors);

        context.drawFullscreenQuad();
    }

    private void build1DMinMaxMipMap(PostProcessingRenderContext context){
        if(m_InitializeMinMaxShadowMapProgram == null /*|| g_GenerateSliceEndpointsProgram.getProgram() == 0*/){
            m_InitializeMinMaxShadowMapProgram = m_sharedData.getInitializeMinMaxShadowMapProgram();
        }

        if(m_ComputeMinMaxSMLevelProgram == null /*|| g_GenerateSliceEndpointsProgram.getProgram() == 0*/){
            m_ComputeMinMaxSMLevelProgram = m_sharedData.getComputeMinMaxSMLevelProgram();
        }

        final Texture2D ptex2DSliceUVDirAndOriginSRV = getOutputTexture(0);
        final Texture2D ptex2DShadowMapSRV = getInput(2);
        Texture2D _output = getOutputTexture(1);
        final Texture2D tempTex = RenderTexturePool.getInstance().findFreeElement(_output.getWidth(), _output.getHeight(), _output.getFormat());

        m_ptex2DMinMaxShadowMapSRV[0] = _output;
        m_ptex2DMinMaxShadowMapSRV[1] = tempTex;

        // Computing min/max mip map using compute shader is much slower because a lot of threads are idle
        int uiXOffset = 0;
        int uiPrevXOffset = 0;
        int uiParity = 0;

//            CComPtr<ID3D11Resource> presMinMaxShadowMap0, presMinMaxShadowMap1;
//            m_ptex2DMinMaxShadowMapRTV[0]->GetResource(&presMinMaxShadowMap0);
//            m_ptex2DMinMaxShadowMapRTV[1]->GetResource(&presMinMaxShadowMap1);

        int presMinMaxShadowMap0, presMinMaxShadowMap1;
        presMinMaxShadowMap0 = m_ptex2DMinMaxShadowMapSRV[0].getTexture();
        presMinMaxShadowMap1 = m_ptex2DMinMaxShadowMapSRV[1].getTexture();
        // Note that we start rendering min/max shadow map from step == 2
        for(int iStep = 2; iStep <= m_sharedData.m_ScatteringFrameAttribs.m_uiMaxShadowMapStep; iStep *=2, uiParity = (uiParity+1)%2 )
        {
            // Use two buffers which are in turn used as the source and destination
//                FrameAttribs.pd3dDeviceContext->OMSetRenderTargets( 1, &m_ptex2DMinMaxShadowMapRTV[uiParity].p, NULL);
//            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, m_ptex2DMinMaxShadowMapSRV[uiParity].getTarget(), m_ptex2DMinMaxShadowMapSRV[uiParity].getTexture(), 0);
            final Texture2D output = m_ptex2DMinMaxShadowMapSRV[uiParity];
            if( iStep == 2 )
            {
                // At the initial pass, the shader gathers 8 depths which will be used for
                // PCF filtering at the sample location and its next neighbor along the slice
                // and outputs min/max depths

                // Texture2D<float4> g_tex2DSliceUVDirAndOrigin   : register( t2 );
                // Texture2D<float2> g_tex2DLightSpaceDepthMap    : register( t3 );
//                    ID3D11ShaderResourceView *pSRVs[] = {m_ptex2DSliceUVDirAndOriginSRV, FrameAttribs.ptex2DShadowMapSRV,};
//                    FrameAttribs.pd3dDeviceContext->PSSetShaderResources( 2, _countof(pSRVs), pSRVs );
//                m_ptex2DSliceUVDirAndOriginSRV.bind(RenderTechnique.TEX2D_SLICE_UV_ORIGIN, m_psamLinearClamp);
//                frameAttribs.ptex2DShadowMapSRV.bind(RenderTechnique.TEX2D_SHADOWM_BUFFER, m_psamLinearBorder0);
                context.bindTexture(ptex2DSliceUVDirAndOriginSRV, VolumetricLightingProgram.TEX2D_SLICE_UV_ORIGIN, m_sharedData.m_psamLinearClamp);
                context.bindTexture(ptex2DShadowMapSRV, VolumetricLightingProgram.TEX2D_SHADOWM_BUFFER, m_sharedData.m_psamLinearBorder0);
            }
            else
            {
                // At the subsequent passes, the shader loads two min/max values from the next finer level
                // to compute next level of the binary tree

                // Set source and destination min/max data offsets:
                SMiscDynamicParams MiscDynamicParams = m_MiscDynamicParams;
                MiscDynamicParams.ui4SrcMinMaxLevelXOffset = uiPrevXOffset;
                MiscDynamicParams.ui4DstMinMaxLevelXOffset = uiXOffset;
//                    UpdateConstantBuffer(FrameAttribs.pd3dDeviceContext, m_pcbMiscParams, &MiscDynamicParams, sizeof(MiscDynamicParams));
                //cbuffer cbMiscDynamicParams : register( b4 )
//                    FrameAttribs.pd3dDeviceContext->PSSetConstantBuffers(4, 1, &m_pcbMiscParams.p);

                // Texture2D<float2> g_tex2DMinMaxLightSpaceDepth  : register( t4 );
//                    FrameAttribs.pd3dDeviceContext->PSSetShaderResources( 4, 1, &m_ptex2DMinMaxShadowMapSRV[ (uiParity+1)%2 ].p );
//                m_ptex2DMinMaxShadowMapSRV[ (uiParity+1)%2 ].bind(RenderTechnique.TEX2D_MIN_MAX_DEPTH, m_psamLinearClamp);
                context.bindTexture(m_ptex2DMinMaxShadowMapSRV[ (uiParity+1)%2 ], VolumetricLightingProgram.TEX2D_MIN_MAX_DEPTH, m_sharedData.m_psamLinearClamp);
            }

//            renderQuad( frameAttribs,
//                    (iStep>2) ? m_ComputeMinMaxSMLevelTech : m_InitializeMinMaxShadowMapTech,
//                    m_PostProcessingAttribs.m_uiMinMaxShadowMapResolution / iStep, m_PostProcessingAttribs.m_uiNumEpipolarSlices,
//                    uiXOffset, 0 );
            VolumetricLightingProgram renderProgram = (iStep>2) ? m_ComputeMinMaxSMLevelProgram : m_InitializeMinMaxShadowMapProgram;
            context.setVAO(null);
            context.setProgram(renderProgram);
            m_sharedData.setUniforms(renderProgram, m_MiscDynamicParams);
            context.setBlendState(null);
            context.setDepthStencilState(null);
            context.setRasterizerState(null);
            context.setViewport(uiXOffset, 0, m_sharedData.m_LightAttribs.m_uiMinMaxShadowMapResolution / iStep, m_sharedData.m_ScatteringInitAttribs.m_uiNumEpipolarSlices);
            context.setRenderTarget(output);
            context.drawFullscreenQuad();

            // All the data must reside in 0-th texture, so copy current level, if necessary, from 1-st texture
            if( uiParity == 1 )
            {
//                    D3D11_BOX SrcBox;
//                    SrcBox.left = uiXOffset;
//                    SrcBox.right = uiXOffset + m_PostProcessingAttribs.m_uiMinMaxShadowMapResolution / iStep;
//                    SrcBox.top = 0;
//                    SrcBox.bottom = m_PostProcessingAttribs.m_uiNumEpipolarSlices;
//                    SrcBox.front = 0;
//                    SrcBox.back = 1;
//                    FrameAttribs.pd3dDeviceContext->CopySubresourceRegion(presMinMaxShadowMap0, 0, uiXOffset, 0, 0,
//                                                                            presMinMaxShadowMap1, 0, &SrcBox);
                int srcX = uiXOffset;
                int srcY = 0;
                int srcZ = 0;

                int dstX = uiXOffset;
                int dstY = 0;
                int dstZ = 0;

                int srcWidth = m_sharedData.m_LightAttribs.m_uiMinMaxShadowMapResolution / iStep;
                int srcHeight = m_sharedData.m_ScatteringInitAttribs.m_uiNumEpipolarSlices;
                int srcDepth = 1;
                GLFuncProviderFactory.getGLFuncProvider().glCopyImageSubData(presMinMaxShadowMap1, GLenum.GL_TEXTURE_2D, 0, srcX, srcY, srcZ,
                        presMinMaxShadowMap0, GLenum.GL_TEXTURE_2D, 0, dstX, dstY, dstZ,
                        srcWidth, srcHeight, srcDepth);
            }

            uiPrevXOffset = uiXOffset;
            uiXOffset += m_sharedData.m_LightAttribs.m_uiMinMaxShadowMapResolution / iStep;
        }

        RenderTexturePool.getInstance().freeUnusedResource(tempTex);
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        out.arraySize = 1;
        out.sampleCount = 1;
        if(index == 0) {
            out.width = m_sharedData.m_ScatteringInitAttribs.m_uiNumEpipolarSlices;
            out.height = 1;
            out.format = GLenum.GL_RGBA32F;  // 16FP or 32FP
        }else { // min max out put.
            out.width = m_sharedData.m_LightAttribs.m_uiMinMaxShadowMapResolution;
            out.height = m_sharedData.m_ScatteringInitAttribs.m_uiNumEpipolarSlices;
            out.format = m_sharedData.m_ScatteringInitAttribs.m_uiAccelStruct == AccelStruct.MIN_MAX_TREE ? GLenum.GL_RG16F : GLenum.GL_RGBA16F;
        }
    }
}
