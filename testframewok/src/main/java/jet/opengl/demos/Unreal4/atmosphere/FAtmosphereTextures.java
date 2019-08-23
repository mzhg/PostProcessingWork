package jet.opengl.demos.Unreal4.atmosphere;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.Texture3DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;

final class FAtmosphereTextures {

    // Final result
    public TextureGL AtmosphereTransmittance;
    public TextureGL AtmosphereIrradiance;
    public TextureGL AtmosphereInscatter;

    // Intermediate result
    public TextureGL AtmosphereDeltaE;
    public TextureGL AtmosphereDeltaSR;
    public TextureGL AtmosphereDeltaSM;
    public TextureGL AtmosphereDeltaJ;

    public final FAtmospherePrecomputeParameters PrecomputeParams;

    public FAtmosphereTextures(FAtmospherePrecomputeParameters InPrecomputeParams)
    {
        PrecomputeParams = InPrecomputeParams;
//        InitResource();

        InitDynamicRHI();
    }

	/*~FAtmosphereTextures()

    {

        ReleaseResource();

    }*/

    // FRenderResource interface.
    public void InitDynamicRHI() {
//        check(PrecomputeParams != NULL);
        // Allocate atmosphere precompute textures...
        {
            /*FRHICommandListImmediate& RHICmdList = FRHICommandListExecutor::GetImmediateCommandList();
            // Transmittance
            FIntPoint GTransmittanceTexSize(PrecomputeParams->TransmittanceTexWidth, PrecomputeParams->TransmittanceTexHeight);
            FPooledRenderTargetDesc TransmittanceDesc(FPooledRenderTargetDesc::Create2DDesc(GTransmittanceTexSize, PF_FloatRGBA, FClearValueBinding::Black, TexCreate_None, TexCreate_RenderTargetable, false));
            GRenderTargetPool.FindFreeElement(RHICmdList, TransmittanceDesc, AtmosphereTransmittance, TEXT("AtmosphereTransmittance"));*/

            Texture2DDesc Tex2DDesc = new Texture2DDesc(PrecomputeParams.TransmittanceTexWidth, PrecomputeParams.TransmittanceTexHeight, GLenum.GL_RGBA16F);
            AtmosphereTransmittance = TextureUtils.createTexture2D(Tex2DDesc, null);
            AtmosphereTransmittance.setName("AtmosphereTransmittance");

            {
			    /*const FSceneRenderTargetItem& TransmittanceTarget = AtmosphereTransmittance->GetRenderTargetItem();

                FRHIRenderPassInfo RPInfo(TransmittanceTarget.TargetableTexture, ERenderTargetActions::Clear_Store);
                RHICmdList.BeginRenderPass(RPInfo, TEXT("ClearTransmittance"));
                RHICmdList.EndRenderPass();
                RHICmdList.CopyToResolveTarget(TransmittanceTarget.TargetableTexture, TransmittanceTarget.ShaderResourceTexture, FResolveParams());*/
            }

            // Irradiance
            /*FIntPoint GIrradianceTexSize(PrecomputeParams->IrradianceTexWidth, PrecomputeParams->IrradianceTexHeight);
            FPooledRenderTargetDesc IrradianceDesc(FPooledRenderTargetDesc::Create2DDesc(GIrradianceTexSize, PF_FloatRGBA, FClearValueBinding::Black, TexCreate_None, TexCreate_RenderTargetable, false));
            GRenderTargetPool.FindFreeElement(RHICmdList, IrradianceDesc, AtmosphereIrradiance, TEXT("AtmosphereIrradiance"));*/

            Tex2DDesc.width = PrecomputeParams.IrradianceTexWidth;
            Tex2DDesc.height = PrecomputeParams.IrradianceTexHeight;
            AtmosphereIrradiance = TextureUtils.createTexture2D(Tex2DDesc, null);
            AtmosphereIrradiance.setName("AtmosphereIrradiance");
            {
			    /*const FSceneRenderTargetItem& IrradianceTarget = AtmosphereIrradiance->GetRenderTargetItem();

                FRHIRenderPassInfo RPInfo(IrradianceTarget.TargetableTexture, ERenderTargetActions::Clear_Store);
                RHICmdList.BeginRenderPass(RPInfo, TEXT("ClearIrradiance"));
                RHICmdList.EndRenderPass();
                RHICmdList.CopyToResolveTarget(IrradianceTarget.TargetableTexture, IrradianceTarget.ShaderResourceTexture, FResolveParams());*/
            }
            // DeltaE
//            GRenderTargetPool.FindFreeElement(RHICmdList, IrradianceDesc, AtmosphereDeltaE, TEXT("AtmosphereDeltaE"));
            AtmosphereDeltaE = TextureUtils.createTexture2D(Tex2DDesc, null);
            AtmosphereDeltaE.setName("AtmosphereDeltaE");

            // 3D Texture
            // Inscatter
//            FPooledRenderTargetDesc InscatterDesc(FPooledRenderTargetDesc::CreateVolumeDesc(PrecomputeParams->InscatterMuSNum * PrecomputeParams->InscatterNuNum, PrecomputeParams->InscatterMuNum, PrecomputeParams->InscatterAltitudeSampleNum, PF_FloatRGBA, FClearValueBinding::None, TexCreate_None, TexCreate_ShaderResource | TexCreate_RenderTargetable, false));
//            GRenderTargetPool.FindFreeElement(RHICmdList, InscatterDesc, AtmosphereInscatter, TEXT("AtmosphereInscatter"));
            Texture3DDesc InscatterDesc = new Texture3DDesc(PrecomputeParams.InscatterMuSNum * PrecomputeParams.InscatterNuNum, PrecomputeParams.InscatterMuNum, PrecomputeParams.InscatterAltitudeSampleNum, 1, GLenum.GL_RGBA16F);
            AtmosphereInscatter = TextureUtils.createTexture3D(InscatterDesc, null);
            AtmosphereInscatter.setName("AtmosphereInscatter");

            // DeltaSR
//            GRenderTargetPool.FindFreeElement(RHICmdList, InscatterDesc, AtmosphereDeltaSR, TEXT("AtmosphereDeltaSR"));
            AtmosphereDeltaSR = TextureUtils.createTexture3D(InscatterDesc, null);
            AtmosphereDeltaSR.setName("AtmosphereDeltaSR");

            // DeltaSM
//            GRenderTargetPool.FindFreeElement(RHICmdList, InscatterDesc, AtmosphereDeltaSM, TEXT("AtmosphereDeltaSM"));
            AtmosphereDeltaSM = TextureUtils.createTexture3D(InscatterDesc, null);
            AtmosphereDeltaSM.setName("AtmosphereDeltaSM");

            // DeltaJ
//            GRenderTargetPool.FindFreeElement(RHICmdList, InscatterDesc, AtmosphereDeltaJ, TEXT("AtmosphereDeltaJ"));
            AtmosphereDeltaJ = TextureUtils.createTexture3D(InscatterDesc, null);
            AtmosphereDeltaJ.setName("AtmosphereDeltaJ");
        }
    }

    public void ReleaseDynamicRHI() {
        AtmosphereTransmittance.dispose();
        AtmosphereIrradiance.dispose();
        AtmosphereDeltaE.dispose();

        AtmosphereInscatter.dispose();
        AtmosphereDeltaSR.dispose();
        AtmosphereDeltaSM.dispose();
        AtmosphereDeltaJ.dispose();

    }
}
