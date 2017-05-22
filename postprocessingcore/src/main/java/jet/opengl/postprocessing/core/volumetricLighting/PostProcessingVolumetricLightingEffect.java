package jet.opengl.postprocessing.core.volumetricLighting;

import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingCommonData;
import jet.opengl.postprocessing.core.PostProcessingDownsamplePass;
import jet.opengl.postprocessing.core.PostProcessingEffect;
import jet.opengl.postprocessing.core.PostProcessingReconstructCameraZPass;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/5/17.
 */

public class PostProcessingVolumetricLightingEffect extends PostProcessingEffect {
    private SharedData m_sharedData;

    @Override
    protected void fillRenderPass(PostProcessing context, PostProcessingCommonData commonData) {
        if(m_sharedData == null)
            m_sharedData = new SharedData(commonData.frameAttribs);
        m_sharedData.prepare(commonData.frameAttribs, (LightScatteringInitAttribs)getInitValue(), (LightScatteringFrameAttribs)getUniformValue());

        // no inputs and no outputs.
        PostProcessingPreparePass preparePass = new PostProcessingPreparePass(m_sharedData, (LightScatteringInitAttribs)getInitValue(), (LightScatteringFrameAttribs)getUniformValue());
        context.appendRenderPass("VolumeLightingPrepare", preparePass);

        InscaterringIntegralEvalution eInsctrIntglEvalMethod = m_sharedData.m_ScatteringInitAttribs.m_uiInsctrIntglEvalMethod;
        PostProcessingPrecomputedPointLightInscatteringPass precomputedPointLightInscatteringPass = null;
        if(m_sharedData.m_ScatteringInitAttribs.m_uiLightType != LightType.DIRECTIONAL && (eInsctrIntglEvalMethod == InscaterringIntegralEvalution.MY_LUT||
                eInsctrIntglEvalMethod == InscaterringIntegralEvalution.SRNN05)){
            precomputedPointLightInscatteringPass = new PostProcessingPrecomputedPointLightInscatteringPass(m_sharedData);
            context.appendRenderPass("PrecomputedPointLightInscattering", precomputedPointLightInscatteringPass);
        }

        PostProcessingReconstructCameraZPass reconstructCameraSpaceZ = new PostProcessingReconstructCameraZPass(false, 0, true);
        reconstructCameraSpaceZ.setDependency(0, commonData.sceneDepthTexture, 0);
        context.appendRenderPass("ReconstructCameraZ", reconstructCameraSpaceZ);

        if(m_sharedData.m_ScatteringInitAttribs.m_bEnableEpipolarSampling){
            PostProcessingGenerateSliceEndpointsPass generateSliceEndpointsPass = new PostProcessingGenerateSliceEndpointsPass(m_sharedData);
            generateSliceEndpointsPass.setOutputFixSize(0, m_sharedData.m_ScatteringInitAttribs.m_uiNumEpipolarSlices, 1);
            context.appendRenderPass("GenerateSliceEndpoints", generateSliceEndpointsPass);

            PostProcessingGenerateCoordinateTexturePass generateCoordinateTexturePass = new PostProcessingGenerateCoordinateTexturePass(m_sharedData);
            generateCoordinateTexturePass.setDependency(0, reconstructCameraSpaceZ, 0);
            generateCoordinateTexturePass.setDependency(1, generateSliceEndpointsPass, 0);
            context.appendRenderPass("GenerateCoordinateTexture", generateCoordinateTexturePass);

            PostProcessingRefineSampleLocationsPass refineSampleLocationsPass = new PostProcessingRefineSampleLocationsPass(m_sharedData);
            refineSampleLocationsPass.setDependency(0, generateCoordinateTexturePass, 0);
            refineSampleLocationsPass.setDependency(1, generateCoordinateTexturePass, 1);
            context.appendRenderPass("RefineSampleLocations", refineSampleLocationsPass);

            PostProcessingMarkRayMarchingSamplesPass markRayMarchingSamplesPass = new PostProcessingMarkRayMarchingSamplesPass(m_sharedData);
            markRayMarchingSamplesPass.setDependency(0, refineSampleLocationsPass, 0);
            context.appendRenderPass("MarkRayMarchingSamples", markRayMarchingSamplesPass);

            // Build min/max mip map
            PostProcessingBuild1DMinMaxMipMapPass build1DMinMaxMipMapPass = null;
            if( m_sharedData.m_ScatteringInitAttribs.m_uiAccelStruct != AccelStruct.NONE ) {
                build1DMinMaxMipMapPass = new PostProcessingBuild1DMinMaxMipMapPass(m_sharedData);
                build1DMinMaxMipMapPass.setDependency(0, reconstructCameraSpaceZ, 0);
                build1DMinMaxMipMapPass.setDependency(1, generateSliceEndpointsPass, 0);
                build1DMinMaxMipMapPass.setDependency(2, commonData.shadowMapTexture, 0);

                context.appendRenderPass("Build1DMinMaxMipMap", build1DMinMaxMipMapPass);
            }

            PostProcessingRayMarchingPass doRayMarchingPass = new PostProcessingRayMarchingPass(m_sharedData);
            doRayMarchingPass.setDependency(0, reconstructCameraSpaceZ, 0);
            doRayMarchingPass.setDependency(1, generateCoordinateTexturePass, 0);
            doRayMarchingPass.setDependency(2, commonData.shadowMapTexture, 0);
            doRayMarchingPass.setDependency(3, build1DMinMaxMipMapPass, 1);
            doRayMarchingPass.setDependency(4, build1DMinMaxMipMapPass, 0);
            if(precomputedPointLightInscatteringPass != null)
                doRayMarchingPass.setDependency(5, precomputedPointLightInscatteringPass, 0);
            context.appendRenderPass("RayMarching", doRayMarchingPass);

            PostProcessingInterpolateInsctrIrradiancePass interpolateInsctrIrradiancePass = new PostProcessingInterpolateInsctrIrradiancePass(m_sharedData);
            interpolateInsctrIrradiancePass.setDependency(0, doRayMarchingPass, 0);
            interpolateInsctrIrradiancePass.setDependency(1, refineSampleLocationsPass, 0);
            context.appendRenderPass("InterpolateInsctrIrradiance", interpolateInsctrIrradiancePass);

            final int uiMaxStepsAlongRayAtDepthBreak0 = Math.min(m_sharedData.m_LightAttribs.m_uiShadowMapResolution/4, 256);
            final int uiMaxStepsAlongRayAtDepthBreak1 = Math.min(m_sharedData.m_LightAttribs.m_uiShadowMapResolution/8, 128);
            int outputCount = 1;
            int outWidth = commonData.sceneColorTexture.getOutputTexture(0).getWidth();
            int outHeight = commonData.sceneColorTexture.getOutputTexture(0).getHeight();

            if(m_sharedData.m_ScatteringInitAttribs.m_iDownscaleFactor != 1){
                outWidth = outWidth / m_sharedData.m_ScatteringInitAttribs.m_iDownscaleFactor;
                outHeight = outHeight / m_sharedData.m_ScatteringInitAttribs.m_iDownscaleFactor;
            }

            Texture2D depthStencilTexture = null;
            Texture2D colorTexture = null;
            if(m_sharedData.m_ScatteringInitAttribs.m_bCorrectScatteringAtDepthBreaks){
                if(m_sharedData.m_ScatteringInitAttribs.m_iDownscaleFactor == 1) {
                    depthStencilTexture = m_sharedData.getScreenSizedDSV();
                    colorTexture = m_sharedData.getScreenSizedRTV();
                }else {
                    colorTexture = m_sharedData.getDownsampleRTV();
                    depthStencilTexture = m_sharedData.getDownsampleDSV();
                }
            }

            PostProcessingUnwarpEpipolarScatteringPass unwarpEpipolarScatteringPass = new PostProcessingUnwarpEpipolarScatteringPass(m_sharedData, colorTexture, depthStencilTexture);
            for(int i = 0; i < outputCount; i++){
                unwarpEpipolarScatteringPass.setOutputFixSize(i, outWidth, outHeight);
            }
            unwarpEpipolarScatteringPass.setDependency(0, reconstructCameraSpaceZ, 0);
            unwarpEpipolarScatteringPass.setDependency(1, commonData.sceneColorTexture, 0);
            unwarpEpipolarScatteringPass.setDependency(2, generateCoordinateTexturePass, 1);
            unwarpEpipolarScatteringPass.setDependency(3, interpolateInsctrIrradiancePass, 0);
            unwarpEpipolarScatteringPass.setDependency(4, generateSliceEndpointsPass, 0);
            context.appendRenderPass("UnwarpEpipolarScattering", unwarpEpipolarScatteringPass);

            PostProcessingRenderPass lastPass = null;
            // Correct inscattering for pixels, for which no suitable interpolation sources were found
            if( m_sharedData.m_ScatteringInitAttribs.m_bCorrectScatteringAtDepthBreaks ){
                PostProcessingFixInscatteringAtDepthBreaksPass fixInscatteringAtDepthBreaksPass =
                        new PostProcessingFixInscatteringAtDepthBreaksPass(m_sharedData, m_sharedData.m_ScatteringInitAttribs.m_iDownscaleFactor == 1,
                                uiMaxStepsAlongRayAtDepthBreak0, colorTexture, depthStencilTexture);  // outputCount must be 2.
                fixInscatteringAtDepthBreaksPass.setDependency(0, reconstructCameraSpaceZ, 0);
                fixInscatteringAtDepthBreaksPass.setDependency(1, commonData.sceneColorTexture, 0);
                fixInscatteringAtDepthBreaksPass.setDependency(2, commonData.shadowMapTexture, 0);
                if(precomputedPointLightInscatteringPass != null)
                    fixInscatteringAtDepthBreaksPass.setDependency(3, precomputedPointLightInscatteringPass, 0);

                context.appendRenderPass("FixInscatteringAtDepthBreaksUnwrap", fixInscatteringAtDepthBreaksPass);

                lastPass = fixInscatteringAtDepthBreaksPass;
            }

            if( m_sharedData.m_ScatteringInitAttribs.m_iDownscaleFactor > 1 ){
                if( m_sharedData.m_ScatteringInitAttribs.m_bCorrectScatteringAtDepthBreaks ){
                    depthStencilTexture = m_sharedData.getScreenSizedDSV();
                    colorTexture = m_sharedData.getScreenSizedRTV();
                }else{
                    depthStencilTexture = null;
                    colorTexture = null;
                }

                PostProcessingUpscaleInscatteringRadiancePass upscaleInscatteringRadiancePass = new PostProcessingUpscaleInscatteringRadiancePass(m_sharedData, colorTexture, depthStencilTexture);
                upscaleInscatteringRadiancePass.setDependency(0, reconstructCameraSpaceZ, 0);
                upscaleInscatteringRadiancePass.setDependency(1, commonData.sceneColorTexture, 0);
                upscaleInscatteringRadiancePass.setDependency(2, unwarpEpipolarScatteringPass, 0);
                context.appendRenderPass("UpscaleInscatteringRadiance", upscaleInscatteringRadiancePass);

                // Correct inscattering for pixels, for which no suitable interpolation sources were found
                if( m_sharedData.m_ScatteringInitAttribs.m_bCorrectScatteringAtDepthBreaks ){
                    PostProcessingFixInscatteringAtDepthBreaksPass fixInscatteringAtDepthBreaksPass =
                            new PostProcessingFixInscatteringAtDepthBreaksPass(m_sharedData, true,
                                    uiMaxStepsAlongRayAtDepthBreak1, colorTexture, depthStencilTexture);  // outputCount must be 2.
                    fixInscatteringAtDepthBreaksPass.setDependency(0, reconstructCameraSpaceZ, 0);
                    fixInscatteringAtDepthBreaksPass.setDependency(1, commonData.sceneColorTexture, 0);
                    fixInscatteringAtDepthBreaksPass.setDependency(2, commonData.shadowMapTexture, 0);
                    if(precomputedPointLightInscatteringPass != null)
                        fixInscatteringAtDepthBreaksPass.setDependency(3, precomputedPointLightInscatteringPass, 0);

                    context.appendRenderPass("FixInscatteringAtDepthBreaksUpscale", fixInscatteringAtDepthBreaksPass);
                    lastPass = fixInscatteringAtDepthBreaksPass;
                }
            }

            // output to the screen
            if(isLastEffect() && lastPass != null){ // TODO we could optmise this step in further.
                PostProcessingDownsamplePass blitPass = new PostProcessingDownsamplePass(1, PostProcessingDownsamplePass.DOWMSAMPLE_FASTEST);
                blitPass.setDependency(0, lastPass, 0);
                blitPass.setOutputFixSize(0, colorTexture.getWidth(), colorTexture.getHeight());
                context.appendRenderPass("BlitInternalTexture", blitPass);
            }

        }else{
            // only for debugging...
            PostProcessingFixInscatteringAtDepthBreaksPass fixInscatteringAtDepthBreaksPass =
                    new PostProcessingFixInscatteringAtDepthBreaksPass(m_sharedData, true, m_sharedData.m_LightAttribs.m_uiShadowMapResolution, null, null);
            fixInscatteringAtDepthBreaksPass.setDependency(0, reconstructCameraSpaceZ, 0);
            fixInscatteringAtDepthBreaksPass.setDependency(1, commonData.sceneColorTexture, 0);
            fixInscatteringAtDepthBreaksPass.setDependency(2, commonData.shadowMapTexture, 0);
            if(precomputedPointLightInscatteringPass != null)
                fixInscatteringAtDepthBreaksPass.setDependency(3, precomputedPointLightInscatteringPass, 0);

            context.appendRenderPass("FixInscatteringAtDepthBreaks", fixInscatteringAtDepthBreaksPass);
        }
    }

    @Override
    protected void fillRenderPass(PostProcessing context, PostProcessingRenderPass sceneColorTexture, PostProcessingRenderPass sceneDepthTexture) {}

    @Override
    public String getEffectName() {
        return PostProcessing.VOLUMETRIC_LIGHTING;
    }

    @Override
    public int getPriority() {
        return PostProcessing.VOLUMETRIC_LIGHTING_PRIPORTY;
    }
}
