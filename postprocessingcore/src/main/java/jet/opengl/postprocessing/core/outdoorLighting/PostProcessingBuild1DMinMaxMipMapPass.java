package jet.opengl.postprocessing.core.outdoorLighting;

import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.core.RenderTexturePool;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;

/**
 * Created by mazhen'gui on 2017/6/5.
 */

final class PostProcessingBuild1DMinMaxMipMapPass extends PostProcessingRenderPass{

    private RenderTechnique m_InitializeMinMaxShadowMapProgram;
    private RenderTechnique m_ComputeMinMaxSMLevelProgram;

    private final Texture2D[] m_ptex2DMinMaxShadowMap = new Texture2D[2];
    private SharedData m_sharedData;
    private final int m_iCascadeIndex;

    public PostProcessingBuild1DMinMaxMipMapPass(SharedData sharedData, int iCascadeIndex) {
        super("Build1DMinMaxMipMap");

        m_sharedData = sharedData;
        m_iCascadeIndex = iCascadeIndex;

        // input0: tex2DSliceUVDirAndOrigin
        // input1: tex2DLightSpaceDepthMap
        // output: tex2DMinMaxLightSpaceDepth
        set(2, 1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(m_InitializeMinMaxShadowMapProgram == null){
            m_InitializeMinMaxShadowMapProgram = m_sharedData.getInitializeMinMaxShadowMapProgram();
        }

        if(m_ComputeMinMaxSMLevelProgram == null){
            m_ComputeMinMaxSMLevelProgram = m_sharedData.getComputeMinMaxSMLevelProgram();
        }

        final Texture2D ptex2DSliceUVDirAndOriginSRV = getInput(0);
        final Texture2D ptex2DShadowMapSRV = getInput(1);

        final Texture2D _output = getOutputTexture(0);
        final Texture2D tempTex = RenderTexturePool.getInstance().findFreeElement(_output.getWidth(), _output.getHeight(), _output.getFormat());
        m_ptex2DMinMaxShadowMap[0] = _output;
        m_ptex2DMinMaxShadowMap[1] = tempTex;

        // Computing min/max mip map using compute shader is much slower because a lot of threads are idle
        int uiXOffset = 0;
        int uiPrevXOffset = 0;
        int uiParity = 0;
        // Note that we start rendering min/max shadow map from step == 2
        for(int iStep = 2; iStep <= m_sharedData.m_ScatteringFrameAttribs.m_fMaxShadowMapStep; iStep *=2, uiParity = (uiParity+1)%2 )
        {
            // Use two buffers which are in turn used as the source and destination
//            FrameAttribs.pd3dDeviceContext->OMSetRenderTargets( 1, &m_ptex2DMinMaxShadowMapRTV[uiParity].p, NULL);
//        	rtManager.setTexture2DRenderTargets(m_ptex2DMinMaxShadowMap[uiParity].getTexture(), 0);
//            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, m_ptex2DMinMaxShadowMap[uiParity].getTarget(), m_ptex2DMinMaxShadowMap[uiParity].getTexture(), 0);
            final Texture2D output = m_ptex2DMinMaxShadowMap[uiParity];
            // Set source and destination min/max data offsets:
            SMiscDynamicParams MiscDynamicParams = m_sharedData.m_MiscDynamicParams;
            MiscDynamicParams.ui4SrcMinMaxLevelXOffset = uiPrevXOffset;
            MiscDynamicParams.ui4DstMinMaxLevelXOffset = uiXOffset;
            MiscDynamicParams.fCascadeInd = m_iCascadeIndex;
//            UpdateConstantBuffer(FrameAttribs.pd3dDeviceContext, m_pcbMiscParams, &MiscDynamicParams, sizeof(MiscDynamicParams));
            //cbuffer cbMiscDynamicParams : register( b4 )
//            FrameAttribs.pd3dDeviceContext->PSSetConstantBuffers(4, 1, &m_pcbMiscParams.p);
//            bindBuffer(LSConstant.MISC_BINDING, m_pcbMiscParams, MiscDynamicParams);
//            GLError.checkError();

            if( iStep == 2 )
            {
                // At the initial pass, the shader gathers 8 depths which will be used for
                // PCF filtering at the sample location and its next neighbor along the slice
                // and outputs min/max depths

//                ID3D11ShaderResourceView *pSRVs[] =
//                {
//                    FrameAttribs.ptex2DShadowMapSRV, // Texture2D<float2> g_tex2DLightSpaceDepthMap    : register( t3 );
//                    nullptr,                         // t4
//                    nullptr,                         // t5
//                    m_ptex2DSliceUVDirAndOriginSRV   // Texture2D<float4> g_tex2DSliceUVDirAndOrigin   : register( t6 );
//                };
//                FrameAttribs.pd3dDeviceContext->PSSetShaderResources( 3, _countof(pSRVs), pSRVs );

//            	if(iStep>2){
//            		FrameAttribs.ptex2DShadowMapSRV.bind(m_ComputeMinMaxSMLevelTech.getShadowMapUnit(), m_psamPointClamp);
//            		m_ptex2DSliceUVDirAndOrigin.bind(m_ComputeMinMaxSMLevelTech.getSliceUVDirAndOriginUnit(), m_psamLinearClamp);
//            		GLError.checkError();
//            	}else{
//            		FrameAttribs.ptex2DShadowMapSRV.bind(m_InitializeMinMaxShadowMapTech.getShadowMapUnit(), m_psamPointClamp);
//            		m_ptex2DSliceUVDirAndOrigin.bind(m_InitializeMinMaxShadowMapTech.getSliceUVDirAndOriginUnit(), m_psamLinearClamp);
//            		GLError.checkError();
//            	}

//                FrameAttribs.ptex2DShadowMapSRV.bind(ScatteringRenderTechnique.TEX2D_LIGHT_SPACE_DEPTH, m_psamLinearBorder0);
//                m_ptex2DSliceUVDirAndOrigin.bind(ScatteringRenderTechnique.TEX2D_SLICE_UV_ORIGIN, m_psamLinearClamp);
                context.bindTexture(ptex2DShadowMapSRV, RenderTechnique.TEX2D_LIGHT_SPACE_DEPTH, m_sharedData.m_psamLinearBorder0);
                context.bindTexture(ptex2DSliceUVDirAndOriginSRV, RenderTechnique.TEX2D_SLICE_UV_ORIGIN, m_sharedData.m_psamLinearClamp);
            }
            else
            {
                // At the subsequent passes, the shader loads two min/max values from the next finer level
                // to compute next level of the binary tree

                // Texture2D<float2> g_tex2DMinMaxLightSpaceDepth  : register( t4 );
//                FrameAttribs.pd3dDeviceContext->PSSetShaderResources( 4, 1, &m_ptex2DMinMaxShadowMapSRV[ (uiParity+1)%2 ].p );
//            	if(iStep>2){
//            		m_ptex2DMinMaxShadowMap[ (uiParity+1)%2 ].bind(m_ComputeMinMaxSMLevelTech.getMinMaxShadowMapUnit(), m_psamLinearClamp);
//            	}else{
//            		m_ptex2DMinMaxShadowMap[ (uiParity+1)%2 ].bind(m_InitializeMinMaxShadowMapTech.getMinMaxShadowMapUnit(), m_psamLinearClamp);
//            	}

//                m_ptex2DMinMaxShadowMap[ (uiParity+1)%2 ].bind(ScatteringRenderTechnique.TEX2D_MIN_MAX_LIGHT_DEPTH, m_psamLinearClamp);
                context.bindTexture(m_ptex2DMinMaxShadowMap[ (uiParity+1)%2 ], RenderTechnique.TEX2D_MIN_MAX_LIGHT_DEPTH, m_sharedData.m_psamLinearClamp);
            }

//            setupUniforms((iStep>2) ? m_ComputeMinMaxSMLevelTech : m_InitializeMinMaxShadowMapTech, FrameAttribs);
//            renderQuad( /*FrameAttribs.pd3dDeviceContext,*/
//                    (iStep>2) ? m_ComputeMinMaxSMLevelTech : m_InitializeMinMaxShadowMapTech,
//                    m_PostProcessingAttribs.m_uiMinMaxShadowMapResolution / iStep, iMinMaxTexHeight,
//                    uiXOffset, 0 );
//            GLError.checkError();
            final int m_uiMinMaxShadowMapResolution = m_sharedData.m_CommonFrameAttribs.shadowMapTexture.getWidth();
            final int iMinMaxTexHeight = _output.getHeight();
            RenderTechnique renderProgram = (iStep>2) ? m_ComputeMinMaxSMLevelProgram : m_InitializeMinMaxShadowMapProgram;
            context.setVAO(null);
            context.setProgram(renderProgram);
            m_sharedData.setUniforms(renderProgram, MiscDynamicParams);
            context.setBlendState(null);
            context.setDepthStencilState(null);
            context.setRasterizerState(null);
            context.setViewport(uiXOffset,0, m_uiMinMaxShadowMapResolution / iStep, iMinMaxTexHeight);
            context.setRenderTarget(output);
            context.drawFullscreenQuad();

            // All the data must reside in 0-th texture, so copy current level, if necessary, from 1-st texture
            if( uiParity == 1 )
            {
//                D3D11_BOX SrcBox;
//                SrcBox.left = uiXOffset;
//                SrcBox.right = uiXOffset + m_PostProcessingAttribs.m_uiMinMaxShadowMapResolution / iStep;
//                SrcBox.top = 0;
//                SrcBox.bottom = iMinMaxTexHeight;
//                SrcBox.front = 0;
//                SrcBox.back = 1;
//                FrameAttribs.pd3dDeviceContext->CopySubresourceRegion(presMinMaxShadowMap0, 0, uiXOffset, 0, 0,
//                                                                        presMinMaxShadowMap1, 0, &SrcBox);
//            	textureCopy.setSrcBox(uiXOffset, uiXOffset + m_PostProcessingAttribs.m_uiMinMaxShadowMapResolution / iStep, 0, iMinMaxTexHeight);
//            	textureCopy.setDstLoc(uiXOffset, 0, 0);
//            	GLError.checkError();
//            	textureCopy.copySubresourceRegion(m_ptex2DMinMaxShadowMap[0].getTexture(), m_ptex2DMinMaxShadowMap[0].getTarget(),
//            									  m_ptex2DMinMaxShadowMap[1].getTexture(), m_ptex2DMinMaxShadowMap[1].getTarget(),
//            									  GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
                GLFuncProviderFactory.getGLFuncProvider().glCopyImageSubData(m_ptex2DMinMaxShadowMap[1].getTexture(), m_ptex2DMinMaxShadowMap[1].getTarget(), 0, uiXOffset, 0, 0,
                                                                             m_ptex2DMinMaxShadowMap[0].getTexture(), m_ptex2DMinMaxShadowMap[0].getTarget(), 0, uiXOffset, 0, 0,
                                                                             m_uiMinMaxShadowMapResolution / iStep, iMinMaxTexHeight, 1);

//                GLError.checkError();
            }

            uiPrevXOffset = uiXOffset;
            uiXOffset += m_uiMinMaxShadowMapResolution / iStep;
        }

        RenderTexturePool.getInstance().freeUnusedResource(tempTex);
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        out.sampleCount = 1;
        out.arraySize = 1;

        int iMinMaxTexHeight = m_sharedData.m_ScatteringInitAttribs.m_uiNumEpipolarSlices;
        if( m_sharedData.m_ScatteringInitAttribs.m_bUseCombinedMinMaxTexture )
            iMinMaxTexHeight *= (m_sharedData.m_CommonFrameAttribs.cascadeShadowMapAttribs.numCascades - m_sharedData.m_ScatteringInitAttribs.m_iFirstCascade);

        out.width = m_sharedData.m_CommonFrameAttribs.shadowMapTexture.getWidth();  //m_PostProcessingAttribs.m_uiMinMaxShadowMapResolution;
        out.height = iMinMaxTexHeight;
        if(m_sharedData.m_ScatteringInitAttribs.m_bIs32BitMinMaxMipMap){
            out.format = GLenum.GL_RG32F;
        }else{
            out.format = GLenum.GL_RG16F;
        }
        super.computeOutDesc(index, out);
    }
}
