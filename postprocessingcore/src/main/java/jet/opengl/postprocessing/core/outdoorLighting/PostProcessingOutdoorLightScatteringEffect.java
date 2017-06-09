package jet.opengl.postprocessing.core.outdoorLighting;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingCommonData;
import jet.opengl.postprocessing.core.PostProcessingDownsamplePass;
import jet.opengl.postprocessing.core.PostProcessingEffect;
import jet.opengl.postprocessing.core.PostProcessingFrameAttribs;
import jet.opengl.postprocessing.core.PostProcessingReconstructCameraZPass;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.core.PostProcessingRenderPassInput;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/6/5.
 */

public class PostProcessingOutdoorLightScatteringEffect extends PostProcessingEffect {
    @Override
    protected void fillRenderPass(PostProcessing context, PostProcessingCommonData commonData) {
        SharedData m_sharedData = new SharedData(commonData.frameAttribs);
        final OutdoorLightScatteringInitAttribs scatteringInitAttribs = (OutdoorLightScatteringInitAttribs)getInitValue();
        final OutdoorLightScatteringFrameAttribs scatteringFrameAttribs = (OutdoorLightScatteringFrameAttribs)getUniformValue();
        final PostProcessingFrameAttribs  commonFrameAttribs = commonData.frameAttribs;
        m_sharedData.prepare(commonFrameAttribs, scatteringInitAttribs, scatteringFrameAttribs);
                {
            // Add the prepare pass.
            PostProcessingPreparePass preparePass = new PostProcessingPreparePass(m_sharedData, scatteringInitAttribs, scatteringFrameAttribs);
            context.appendRenderPass("Outdoor_VolumeLightingPrepare", preparePass);
        }

        PostProcessingPrecomputeScatteringPass precomputeScatteringPass = new PostProcessingPrecomputeScatteringPass(m_sharedData, commonData.frameAttribs.ambientSkyLight);
        context.appendRenderPass("Outdoor_PrecomputeScattering", precomputeScatteringPass);

        PostProcessingReconstructCameraZPass reconstructCameraSpaceZ = new PostProcessingReconstructCameraZPass(false, 0, true);  // TODO could reuse the exsit cameraPss.
        reconstructCameraSpaceZ.setOutputTexture(true);
        reconstructCameraSpaceZ.setDependency(0, commonData.sceneDepthTexture, 0);
        context.appendRenderPass("ReconstructCameraZ", reconstructCameraSpaceZ);
        int m_uiShadowMapResolution = commonData.frameAttribs.shadowMapTexture != null ? commonData.frameAttribs.shadowMapTexture.getWidth() : 1024;

        if(scatteringInitAttribs.m_bEnableEpipolarSampling){
            PostProcessingGenerateSliceEndpointsPass generateSliceEndpointsPass = new PostProcessingGenerateSliceEndpointsPass(m_sharedData);
            context.appendRenderPass("Outdoor_GenerateSliceEndpoints", generateSliceEndpointsPass);

            PostProcessingGenerateCoordinateTexturePass generateCoordinateTexturePass = new PostProcessingGenerateCoordinateTexturePass(m_sharedData);
            generateCoordinateTexturePass.setDependency(0, reconstructCameraSpaceZ, 0);
            generateCoordinateTexturePass.setDependency(1, generateSliceEndpointsPass, 0);
            context.appendRenderPass("Outdoor_GenerateCoordinateTexture", generateCoordinateTexturePass);

            PostProcessingGenerateCoarseUnshadowedInctrPass generateCoarseUnshadowedInctrPass = null;
            if( scatteringInitAttribs.m_bRefinementCriterionInsctrDiff ||
                    scatteringInitAttribs.m_bExtinctionEvalMode )
            {
                generateCoarseUnshadowedInctrPass = new PostProcessingGenerateCoarseUnshadowedInctrPass(m_sharedData, scatteringInitAttribs.m_bExtinctionEvalMode,
                                                                                                scatteringInitAttribs.m_bRefinementCriterionInsctrDiff);
                generateCoarseUnshadowedInctrPass.setDependency(0, generateCoordinateTexturePass, 0);
                generateCoarseUnshadowedInctrPass.setDependency(1, generateCoordinateTexturePass, 1);
                generateCoarseUnshadowedInctrPass.setDependency(2, precomputeScatteringPass, 3);
                context.appendRenderPass("Outdoor_GenerateCoarseUnshadowedInctr", generateCoarseUnshadowedInctrPass);
            }

            Texture2D m_ptex2DAverageLuminance = null;
            if(scatteringInitAttribs.m_bAutoExposure){
                // Create the scene average luminance texture if the auto exposure enabled.
                Texture2DDesc desc = new Texture2DDesc(1,1, GLenum.GL_R16F);
                m_ptex2DAverageLuminance = TextureUtils.createTexture2D(desc, null);
            }

            PostProcessingRenderPassInput luminanceTex = new PostProcessingRenderPassInput("AverageLuminance", m_ptex2DAverageLuminance);

            PostProcessingRefineSampleLocationsPass refineSampleLocationsPass = new PostProcessingRefineSampleLocationsPass(m_sharedData, scatteringInitAttribs.m_bAutoExposure);
            refineSampleLocationsPass.setDependency(0, generateCoordinateTexturePass, 0);
            if(scatteringInitAttribs.m_bRefinementCriterionInsctrDiff){
                refineSampleLocationsPass.setDependency(1, generateCoarseUnshadowedInctrPass, 0);
            }else{
                refineSampleLocationsPass.setDependency(1, generateCoordinateTexturePass, 1);
            }
            if(scatteringInitAttribs.m_bAutoExposure) {
                refineSampleLocationsPass.setDependency(2, luminanceTex, 0);
            }
            context.appendRenderPass("Outdoor_RefineSampleLocations", refineSampleLocationsPass);

            PostProcessingMarkRayMarchingSamplesPass markRayMarchingSamplesPass = new PostProcessingMarkRayMarchingSamplesPass(m_sharedData);
            markRayMarchingSamplesPass.setDependency(0, refineSampleLocationsPass, 0);
            context.appendRenderPass("Outdoor_MarkRayMarchingSamples", markRayMarchingSamplesPass);

            PostProcessingGenerateSliceUVDirAndOrigPass generateSliceUVDirAndOrigPass = null;
            if( scatteringInitAttribs.m_bEnableLightShafts && scatteringInitAttribs.m_bUse1DMinMaxTree )
            {
                generateSliceUVDirAndOrigPass = new PostProcessingGenerateSliceUVDirAndOrigPass(m_sharedData);
                generateSliceUVDirAndOrigPass.setDependency(0, generateSliceEndpointsPass, 0);
                context.appendRenderPass("Outdoor_GenerateSliceUVDirAndOrig", generateSliceUVDirAndOrigPass);
            }

            int iLastCascade = (scatteringInitAttribs.m_bEnableLightShafts && scatteringInitAttribs.m_uiCascadeProcessingMode == 1/*CASCADE_PROCESSING_MODE_MULTI_PASS*/) ?
                    commonFrameAttribs.cascadeShadowMapAttribs.numCascades - 1 : scatteringInitAttribs.m_iFirstCascade;

            PostProcessingRayMarchingPass lastRayMarchingPass = null;
            for(int iCascadeInd = scatteringInitAttribs.m_iFirstCascade; iCascadeInd <= iLastCascade; ++iCascadeInd)
            {
                // Build min/max mip map
                PostProcessingBuild1DMinMaxMipMapPass build1DMinMaxMipMapPass = null;
                if( scatteringInitAttribs.m_bEnableLightShafts && scatteringInitAttribs.m_bUse1DMinMaxTree )
                {
//                    Build1DMinMaxMipMap(frameAttribs, iCascadeInd);
                    build1DMinMaxMipMapPass = new PostProcessingBuild1DMinMaxMipMapPass(m_sharedData, iCascadeInd);
                    build1DMinMaxMipMapPass.setDependency(0, generateSliceUVDirAndOrigPass, 0);
                    build1DMinMaxMipMapPass.setDependency(1, commonData.shadowMapTexture, 0);
                    context.appendRenderPass("Outdoor_Build1DMinMaxMipMap" + iCascadeInd, build1DMinMaxMipMapPass);
                }


                // Perform ray marching for selected samples
//                doRayMarching(frameAttribs, m_PostProcessingAttribs.m_uiShadowMapResolution, SMAttribs, iCascadeInd);
                PostProcessingRayMarchingPass rayMarchingPass = new PostProcessingRayMarchingPass(m_sharedData, m_uiShadowMapResolution, iCascadeInd);
                rayMarchingPass.setDependency(0, generateCoordinateTexturePass, 1);
                rayMarchingPass.setDependency(1, generateCoordinateTexturePass, 0);
                rayMarchingPass.setDependency(2, commonData.shadowMapTexture, 0);
                rayMarchingPass.setDependency(3, build1DMinMaxMipMapPass, 0);
                rayMarchingPass.setDependency(4, generateSliceUVDirAndOrigPass, 0);

                rayMarchingPass.setDependency(5, precomputeScatteringPass, 1);
                rayMarchingPass.setDependency(6, precomputeScatteringPass, 2);
                rayMarchingPass.setDependency(7, precomputeScatteringPass, 3);

                context.appendRenderPass("Outdoor_RayMarch" + iCascadeInd, rayMarchingPass);
                lastRayMarchingPass = rayMarchingPass;
            }

            // Interpolate ray marching samples onto the rest of samples
            PostProcessingInterpolateInsctrIrradiancePass interpolateInsctrIrradiancePass = new PostProcessingInterpolateInsctrIrradiancePass(m_sharedData);
            interpolateInsctrIrradiancePass.setDependency(0, lastRayMarchingPass, 0);
            interpolateInsctrIrradiancePass.setDependency(1, refineSampleLocationsPass, 0);
            context.appendRenderPass("Outdoor_InterpolateInsctrIrradiance", interpolateInsctrIrradiancePass);

            final int uiMaxStepsAlongRayAtDepthBreak0 = Math.min(m_uiShadowMapResolution/4, 256);
            final int uiMaxStepsAlongRayAtDepthBreak1 = Math.min(m_uiShadowMapResolution/8, 128);

            if( scatteringInitAttribs.m_bAutoExposure )
            {
                // Render scene luminance to low-resolution texture
//	            FrameAttribs.pd3dDeviceContext->OMSetRenderTargets(1, &m_ptex2DLowResLuminanceRTV.p, nullptr);
//                GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, m_ptex2DLowResLuminance.getTarget(), m_ptex2DLowResLuminance.getTexture(), 0);
//                UnwarpEpipolarScattering(frameAttribs, true, m_ptex2DLowResLuminance.getWidth(), m_ptex2DLowResLuminance.getHeight());
                PostProcessingUnwarpEpipolarScatteringPass unwarpEpipolarScatteringPass = new PostProcessingUnwarpEpipolarScatteringPass(m_sharedData, true);
                unwarpEpipolarScatteringPass.setDependency(0, reconstructCameraSpaceZ, 0);
                unwarpEpipolarScatteringPass.setDependency(1, commonData.sceneColorTexture, 0);
                unwarpEpipolarScatteringPass.setDependency(2, generateCoordinateTexturePass, 1);
                unwarpEpipolarScatteringPass.setDependency(3, generateCoarseUnshadowedInctrPass, 1);
                unwarpEpipolarScatteringPass.setDependency(4, interpolateInsctrIrradiancePass, 0);
                unwarpEpipolarScatteringPass.setDependency(5, generateSliceEndpointsPass, 0);
                context.appendRenderPass("Outdoor_UnwrapSceneLumiance", unwarpEpipolarScatteringPass);

                PostProcessingUpdateAverageLuminancePass updateAverageLuminancePass = new PostProcessingUpdateAverageLuminancePass(m_sharedData, m_ptex2DAverageLuminance);
                updateAverageLuminancePass.setDependency(0, unwarpEpipolarScatteringPass, 0);
                context.appendRenderPass("Outdoor_UpdateAverageLuminance", updateAverageLuminancePass);
            }

            Texture2D depthStencilTexture = null;
            Texture2D colorTexture = null;
            if(scatteringInitAttribs.m_bCorrectScatteringAtDepthBreaks){
                depthStencilTexture = m_sharedData.getScreenSizedDSV();
                colorTexture = m_sharedData.getScreenSizedRTV();
            }

            PostProcessingUnwarpEpipolarScatteringPass unwarpEpipolarScatteringPass = new PostProcessingUnwarpEpipolarScatteringPass(m_sharedData, colorTexture, depthStencilTexture,
                                scatteringInitAttribs.m_bAutoExposure);
            unwarpEpipolarScatteringPass.setDependency(0, reconstructCameraSpaceZ, 0);
            unwarpEpipolarScatteringPass.setDependency(1, commonData.sceneColorTexture, 0);
            unwarpEpipolarScatteringPass.setDependency(2, generateCoordinateTexturePass, 1);
            unwarpEpipolarScatteringPass.setDependency(3, generateCoarseUnshadowedInctrPass, 1);
            unwarpEpipolarScatteringPass.setDependency(4, interpolateInsctrIrradiancePass, 0);
            unwarpEpipolarScatteringPass.setDependency(5, generateSliceEndpointsPass, 0);
            if(scatteringInitAttribs.m_bAutoExposure )
                unwarpEpipolarScatteringPass.setDependency(6, luminanceTex, 0);

            context.appendRenderPass("Outdoor_UnwarpEpipolarScattering", unwarpEpipolarScatteringPass);

            if( scatteringInitAttribs.m_bCorrectScatteringAtDepthBreaks ){
                PostProcessingFixInscatteringAtDepthBreaksPass fixInscatteringAtDepthBreaksPass = new PostProcessingFixInscatteringAtDepthBreaksPass(m_sharedData, uiMaxStepsAlongRayAtDepthBreak0,
                        colorTexture, depthStencilTexture, scatteringInitAttribs.m_bEnableLightShafts);

                fixInscatteringAtDepthBreaksPass.setDependency(0, reconstructCameraSpaceZ, 0);
                fixInscatteringAtDepthBreaksPass.setDependency(1, commonData.sceneColorTexture, 0);
                fixInscatteringAtDepthBreaksPass.setDependency(2, commonData.shadowMapTexture, 0);
                fixInscatteringAtDepthBreaksPass.setDependency(3, precomputeScatteringPass, 2);
                fixInscatteringAtDepthBreaksPass.setDependency(4, precomputeScatteringPass, 1);
                fixInscatteringAtDepthBreaksPass.setDependency(5, luminanceTex, 0);
                context.appendRenderPass("FixInscatteringAtDepthBreaks", fixInscatteringAtDepthBreaksPass);

                PostProcessingDownsamplePass blitPass = new PostProcessingDownsamplePass(1, PostProcessingDownsamplePass.DOWMSAMPLE_FASTEST);
                blitPass.setDependency(0, fixInscatteringAtDepthBreaksPass, 0);
                blitPass.setOutputFixSize(0, colorTexture.getWidth(), colorTexture.getHeight());
                context.appendRenderPass("BlitInternalTexture" + Numeric.random(0, 100), blitPass);
            }
        }else{
            // only for debug
            PostProcessingFixInscatteringAtDepthBreaksPass fixInscatteringAtDepthBreaksPass = new PostProcessingFixInscatteringAtDepthBreaksPass(m_sharedData,
                    m_uiShadowMapResolution, null, null, scatteringInitAttribs.m_bEnableLightShafts);
            fixInscatteringAtDepthBreaksPass.setDependency(0, reconstructCameraSpaceZ, 0);
            fixInscatteringAtDepthBreaksPass.setDependency(1, commonData.sceneColorTexture, 0);
            if(scatteringInitAttribs.m_bEnableLightShafts) {
                fixInscatteringAtDepthBreaksPass.setDependency(2, commonData.shadowMapTexture, 0);
                fixInscatteringAtDepthBreaksPass.setDependency(3, precomputeScatteringPass, 2);
                fixInscatteringAtDepthBreaksPass.setDependency(4, precomputeScatteringPass, 1);
            }else{
                fixInscatteringAtDepthBreaksPass.setDependency(2, precomputeScatteringPass, 3);
            }

            context.appendRenderPass("FixInscatteringAtDepthBreaks", fixInscatteringAtDepthBreaksPass);
        }
    }

    @Override
    protected void fillRenderPass(PostProcessing context, PostProcessingRenderPass sceneColorTexture, PostProcessingRenderPass sceneDepthTexture) {}

    @Override
    public String getEffectName() {
        return PostProcessing.OUTDOOR_LIGHTING;
    }

    @Override
    public int getPriority() {
        return PostProcessing.OUTDOOR_LIGHTING_PRIPORTY;
    }
}
