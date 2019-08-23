package jet.opengl.demos.Unreal4.atmosphere;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector3i;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.demos.Unreal4.UE4Engine;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.BufferUtils;
import jet.opengl.postprocessing.util.Numeric;

final class FAtmosphericFogSceneInfo {

    public static final int
    AP_Transmittance = 0,
    AP_Irradiance1 = 1,
    AP_Inscatter1 = 2,
    AP_ClearIrradiance = 3,
    AP_CopyInscatter1 = 4,
    AP_StartOrder = 5,
    AP_InscatterS = 6,
    AP_IrradianceN = 7,
    AP_InscatterN = 8,
    AP_CopyIrradiance = 9,
    AP_CopyInscatterN = 10,
    AP_EndOrder = 11,
    AP_CopyInscatterF = 12,
    AP_CopyInscatterFBack = 13,
    AP_MAX = 14;

    public static final float RadiusGround = 6360;
    public static final float RadiusAtmosphere = 6420;

    public float SunMultiplier = 1.f;
    public float FogMultiplier = 1.f;
    public float InvDensityMultiplier = 1;
    public float DensityOffset = 0.f;
    public float GroundOffset = -100000;  // -1km in defualt distance scale, 100k UU
    public float DistanceScale = 1;
    public float AltitudeScale = 1;
    public float RHeight;
    public float StartDistance = 15000;
    public float DistanceOffset;
    public float SunDiscScale = 1;
    public final Vector3f DefaultSunColor = new Vector3f(1,1,1);
    public final Vector4f DefaultSunDirection = new Vector4f();
    public int RenderFlag;
    public int InscatterAltitudeSampleNum;
    public FAtmosphereTextureResource TransmittanceResource;
    public FAtmosphereTextureResource IrradianceResource;
    public FAtmosphereTextureResource InscatterResource;

    /** Atmosphere pre-computation related data */
    public boolean bNeedRecompute;
    public boolean bPrecomputationStarted;
    public boolean bPrecomputationFinished;
    public boolean bPrecomputationAcceptedByGameThread;
    public int MaxScatteringOrder;
    public int AtmospherePhase;
    public int Atmosphere3DTextureIndex;
    public int AtmoshpereOrder = 2;
    public FAtmosphereTextures AtmosphereTextures;
    public ByteBuffer PrecomputeTransmittance;
    public ByteBuffer PrecomputeIrradiance;
    public ByteBuffer PrecomputeInscatter;

    private final FAtmospherePrecomputeParameters PrecomputeParameters;

    private RenderTargets mFBO;

    private boolean m_printOnce = false;

    /** Initialization constructor. */
    public FAtmosphericFogSceneInfo(FAtmospherePrecomputeParameters InPrecomputeParameters){
        PrecomputeParameters = InPrecomputeParameters;
        StartDistance *= DistanceScale * 0.00001f; // Convert to km in Atmospheric fog shader

        boolean bDisableSunDisk = false;
        boolean bDisableGroundScattering = false;
        // DistanceOffset is in km, no need to change...
//        DefaultSunColor = FLinearColor(InComponent->DefaultLightColor) * InComponent->DefaultBrightness;
//        RenderFlag |= (bDisableSunDisk) ? EAtmosphereRenderFlag::E_DisableSunDisk : EAtmosphereRenderFlag::E_EnableAll;
//        RenderFlag |= (bDisableGroundScattering) ? EAtmosphereRenderFlag::E_DisableGroundScattering : EAtmosphereRenderFlag::E_EnableAll;
//        // Should be same as UpdateAtmosphericFogTransform
//        GroundOffset += InComponent->GetComponentLocation().Z;
//        FMatrix WorldToLight = InComponent->GetComponentTransform().ToMatrixNoScale().InverseFast();
//        DefaultSunDirection = FVector(WorldToLight.M[0][0],WorldToLight.M[1][0],WorldToLight.M[2][0]);

//#if WITH_EDITORONLY_DATA
//        if (Component->PrecomputeCounter != UAtmosphericFogComponent::EValid)
//        {
//            bNeedRecompute = true;
//        }
//#endif
//        TransmittanceResource = Component->TransmittanceResource;
//        IrradianceResource = Component->IrradianceResource;
//        InscatterResource = Component->InscatterResource;
    }
//	~FAtmosphericFogSceneInfo();

    void PrecomputeTextures(/*FRHICommandListImmediate& RHICmdList, const FViewInfo* View, FSceneViewFamily* ViewFamily*/){
//        check(Component != NULL);
        if (AtmosphereTextures == null)
        {
            AtmosphereTextures = new FAtmosphereTextures(PrecomputeParameters);
        }

        if (bPrecomputationAcceptedByGameThread)
        {
            // we finished everything and so now can start a new one if another one came in
            bPrecomputationStarted = false;
            bPrecomputationFinished = false;
            bPrecomputationAcceptedByGameThread = false;
        }

        if (bNeedRecompute && !bPrecomputationStarted)
        {
            StartPrecompute();
        }

        // Atmosphere
        if (bPrecomputationStarted && !bPrecomputationFinished)
        {
            PrecomputeAtmosphereData(/*RHICmdList, View, *ViewFamily*/);

            switch(AtmospherePhase)
            {
                case AP_Inscatter1:
                case AP_CopyInscatter1:
                case AP_CopyInscatterF:
                case AP_CopyInscatterFBack:
                case AP_InscatterN:
                case AP_CopyInscatterN:
                case AP_InscatterS:
                    Atmosphere3DTextureIndex++;
                    if (Atmosphere3DTextureIndex >= PrecomputeParameters.InscatterAltitudeSampleNum)
                    {
                        AtmospherePhase++;
                        Atmosphere3DTextureIndex = 0;
                    }
                    break;
                default:
                    AtmospherePhase++;
                    break;
            }

            if (AtmospherePhase == AP_EndOrder)
            {
                AtmospherePhase = AP_StartOrder;
                AtmoshpereOrder++;
            }

            if (AtmospherePhase == AP_StartOrder)
            {
                if (AtmoshpereOrder > MaxScatteringOrder)
                {
                    if (PrecomputeParameters.DensityHeight > 0.678f) // Fixed artifacts only for some value
                    {
                        AtmospherePhase = AP_CopyInscatterF;
                    }
                    else
                    {
                        AtmospherePhase = AP_MAX;
                    }
                    AtmoshpereOrder = 2;
                }
            }

            if (AtmospherePhase >= AP_MAX)
            {
                AtmospherePhase = 0;
                Atmosphere3DTextureIndex = 0;
                AtmoshpereOrder = 2;

                // Save precomputed data to bulk data
                {
                    /*FIntPoint Extent = AtmosphereTextures->AtmosphereTransmittance->GetDesc().Extent;
                    int32 TotalByte = sizeof(FColor) * Extent.X * Extent.Y;
                    PrecomputeTransmittance.Lock(LOCK_READ_WRITE);
                    FColor* TransmittanceData = (FColor*)PrecomputeTransmittance.Realloc(TotalByte);
                    ReadPixelsPtr(RHICmdList, AtmosphereTextures->AtmosphereTransmittance, TransmittanceData, FIntRect(0, 0, Extent.X, Extent.Y));
                    PrecomputeTransmittance.Unlock();*/

                    PrecomputeTransmittance = ReadPixelsPtr(AtmosphereTextures.AtmosphereTransmittance);
                }

                {
                    /*FIntPoint Extent = AtmosphereTextures->AtmosphereIrradiance->GetDesc().Extent;
                    int32 TotalByte = sizeof(FColor) * Extent.X * Extent.Y;
                    PrecomputeIrradiance.Lock(LOCK_READ_WRITE);
                    FColor* IrradianceData = (FColor*)PrecomputeIrradiance.Realloc(TotalByte);
                    ReadPixelsPtr(RHICmdList, AtmosphereTextures->AtmosphereIrradiance, IrradianceData, FIntRect(0, 0, Extent.X, Extent.Y));
                    PrecomputeIrradiance.Unlock();*/

                    PrecomputeIrradiance = ReadPixelsPtr(AtmosphereTextures.AtmosphereIrradiance);
                }

                {
                    /*int32 SizeX = Component->PrecomputeParams.InscatterMuSNum * Component->PrecomputeParams.InscatterNuNum;
                    int32 SizeY = Component->PrecomputeParams.InscatterMuNum;
                    int32 SizeZ = Component->PrecomputeParams.InscatterAltitudeSampleNum;
                    int32 TotalByte = sizeof(FFloat16Color) * SizeX * SizeY * SizeZ;
                    PrecomputeInscatter.Lock(LOCK_READ_WRITE);
                    FFloat16Color* InscatterData = (FFloat16Color*)PrecomputeInscatter.Realloc(TotalByte);
                    Read3DPixelsPtr(RHICmdList, AtmosphereTextures->AtmosphereInscatter, InscatterData, FIntRect(0, 0, SizeX, SizeY), FIntPoint(0, SizeZ));
                    PrecomputeInscatter.Unlock();*/

                    PrecomputeInscatter = Read3DPixelsPtr(AtmosphereTextures.AtmosphereInscatter);
                }

                // Delete render targets
//                delete AtmosphereTextures;
                AtmosphereTextures.ReleaseDynamicRHI();
                AtmosphereTextures = null;

                // Save to bulk data is done
                bPrecomputationFinished = true;
//                Component->GameThreadServiceRequest.Increment();
            }
        }
    }

    void StartPrecompute(){
        bNeedRecompute = false;
        bPrecomputationStarted = true;
//        check(!bPrecomputationFinished);
//        check(!bPrecomputationAcceptedByGameThread);
        AtmospherePhase = 0;
        Atmosphere3DTextureIndex = 0;
        AtmoshpereOrder = 2;
    }

    private static Vector3i getExtent(TextureGL tex){
        return new Vector3i(tex.getWidth(), tex.getHeight(), tex.getDepth());
    }

    /** Atmosphere pre-computation related functions */
    private Vector3i GetTextureSize(){
        switch(AtmospherePhase)
        {
            case AP_Transmittance:
                return getExtent(AtmosphereTextures.AtmosphereTransmittance);
            case AP_ClearIrradiance:
            case AP_CopyIrradiance:
            case AP_Irradiance1:
            case AP_IrradianceN:
                return getExtent(AtmosphereTextures.AtmosphereIrradiance);
            case AP_Inscatter1:
            case AP_CopyInscatter1:
            case AP_CopyInscatterF:
            case AP_CopyInscatterFBack:
            case AP_InscatterN:
            case AP_CopyInscatterN:
            case AP_InscatterS:
                return getExtent(AtmosphereTextures.AtmosphereInscatter);
        }
        return getExtent(AtmosphereTextures.AtmosphereTransmittance);
    }

    private void DrawQuad(/*FRHICommandList& RHICmdList, const FIntRect& ViewRect, FShader* VertexShader*/){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
    }

    private float GetLayerValue(int Layer, Vector4f DhdH){
//        float AtmosphereR;
        double R = Layer / Math.max(PrecomputeParameters.InscatterAltitudeSampleNum - 1.f, 1.f);
        R = R * R;
        R = Math.sqrt(RadiusGround * RadiusGround + R * (RadiusAtmosphere * RadiusAtmosphere - RadiusGround * RadiusGround)) + (Layer == 0 ? 0.01 : (Layer == PrecomputeParameters.InscatterAltitudeSampleNum - 1 ? -0.001 : 0.0));
        double DMin = RadiusAtmosphere - R;
        double DMax = Math.sqrt(R * R - RadiusGround * RadiusGround) + Math.sqrt(RadiusAtmosphere * RadiusAtmosphere - RadiusGround * RadiusGround);
        double DMinP = R - RadiusGround;
        double DMaxP = Math.sqrt(R * R - RadiusGround * RadiusGround);
//        AtmosphereR = R;
        DhdH.set((float)DMin, (float)DMax, (float)DMinP, (float)DMaxP);

        return (float)R;
    }

    private static final String SHADER_PATH = UE4Engine.SHADER_PATH + "Atmosphere/";

    private static final String FAtmospherePrecomputeVS = "shader_libs/PostProcessingDefaultScreenSpaceVS.vert";
    private static final String FAtmosphereTransmittancePS = SHADER_PATH + "AtmospherePrecomputeTransmittancePS.frag";
    private static final String FAtmosphereIrradiance1PS = SHADER_PATH + "AtmospherePrecomputeIrradiance1PS.frag";
    private static final String FAtmospherePrecomputeInscatterVS = SHADER_PATH + "AtmospherePrecomputeMainVS.vert";
    private static final String FAtmosphereGS = SHADER_PATH + "AtmosphereGS.gemo";
    private static final String FAtmosphereInscatter1PS = SHADER_PATH + "Inscatter1PS.gemo";
    private static final String FAtmosphereCopyInscatter1PS = SHADER_PATH + "CopyInscatter1PS.frag";
    private static final String FAtmosphereInscatterSPS = SHADER_PATH + "InscatterSPS.frag";
    private static final String FAtmosphereIrradianceNPS = SHADER_PATH + "AtmospherePrecomputeIrradianceNPS.frag";
    private static final String FAtmosphereInscatterNPS = SHADER_PATH + "InscatterNPS.frag";
    private static final String FAtmosphereCopyIrradiancePS = SHADER_PATH + "AtmospherePrecomputeCopyIrradiancePS.frag";
    private static final String FAtmosphereCopyInscatterNPS = SHADER_PATH + "CopyInscatterNPS.frag";
    private static final String FAtmosphereCopyInscatterFPS = SHADER_PATH + "CopyInscatterFPS.frag";
    private static final String FAtmosphereCopyInscatterFBackPS = SHADER_PATH + "CopyInscatterFBackPS.frag";

    private GLSLProgram mFAtmosphereTransmittancePS;
    private GLSLProgram mFAtmosphereIrradiance1PS;
    private GLSLProgram mFAtmosphereInscatter1PS;
    private GLSLProgram mFAtmosphereCopyInscatter1PS;
    private GLSLProgram mFAtmosphereInscatterSPS;
    private GLSLProgram mFAtmosphereIrradianceNPS;
    private GLSLProgram mFAtmosphereInscatterNPS;
    private GLSLProgram mFAtmosphereCopyIrradiancePS;
    private GLSLProgram mFAtmosphereCopyInscatterNPS;
    private GLSLProgram mFAtmosphereCopyInscatterFPS;
    private GLSLProgram mFAtmosphereCopyInscatterFBackPS;

    private void RenderAtmosphereShaders(/*FRHICommandList& RHICmdList, FGraphicsPipelineStateInitializer& GraphicsPSOInit, const FViewInfo& View, const FIntRect& ViewRect*/){
        final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        if(mFBO == null)
            mFBO = new RenderTargets();

        mFBO.bind();
        switch (AtmospherePhase)
        {
            case AP_Transmittance:
            {
//			    const FSceneRenderTargetItem& DestRenderTarget = AtmosphereTextures->AtmosphereTransmittance->GetRenderTargetItem();
//                FRHIRenderPassInfo RPInfo(DestRenderTarget.TargetableTexture, MakeRenderTargetActions(ERenderTargetLoadAction::ELoad, ERenderTargetStoreAction::EStore));
                mFBO.setRenderTexture(AtmosphereTextures.AtmosphereTransmittance, null);  // TODO need a precisely specified.
                gl.glViewport(0, 0, AtmosphereTextures.AtmosphereTransmittance.getWidth(), AtmosphereTextures.AtmosphereTransmittance.getHeight());
//                RHICmdList.BeginRenderPass(RPInfo, TEXT("AP_Transmittance"));
                {
                    /*RHICmdList.ApplyCachedRenderTargets(GraphicsPSOInit);

                    TShaderMapRef<FAtmospherePrecomputeVS> VertexShader(ShaderMap);
                    TShaderMapRef<FAtmosphereTransmittancePS> PixelShader(ShaderMap);

                    GraphicsPSOInit.BoundShaderState.VertexDeclarationRHI = GFilterVertexDeclaration.VertexDeclarationRHI;
                    GraphicsPSOInit.BoundShaderState.VertexShaderRHI = GETSAFERHISHADER_VERTEX(*VertexShader);
                    GraphicsPSOInit.BoundShaderState.PixelShaderRHI = GETSAFERHISHADER_PIXEL(*PixelShader);
                    GraphicsPSOInit.PrimitiveType = PT_TriangleList;

                    SetGraphicsPipelineState(RHICmdList, GraphicsPSOInit);
                    PixelShader->SetParameters(RHICmdList, View);*/
                    if(mFAtmosphereTransmittancePS == null){
                        mFAtmosphereTransmittancePS = GLSLProgram.createProgram(FAtmospherePrecomputeVS, FAtmosphereTransmittancePS, null);
                        mFAtmosphereTransmittancePS.setName("FAtmosphereTransmittancePS");
                    }

                    mFAtmosphereTransmittancePS.enable();

                    DrawQuad(/*RHICmdList, ViewRect, *VertexShader*/);

                    if(!m_printOnce)
                        mFAtmosphereTransmittancePS.printPrograminfo();
                }
//                RHICmdList.EndRenderPass();
//                RHICmdList.CopyToResolveTarget(DestRenderTarget.TargetableTexture, DestRenderTarget.ShaderResourceTexture, FResolveParams());
            }
            break;
            case AP_Irradiance1:
            {
			    /*const FSceneRenderTargetItem& DestRenderTarget = AtmosphereTextures->AtmosphereDeltaE->GetRenderTargetItem();
                FRHIRenderPassInfo RPInfo(DestRenderTarget.TargetableTexture, MakeRenderTargetActions(ERenderTargetLoadAction::ELoad, ERenderTargetStoreAction::EStore));
                RHICmdList.BeginRenderPass(RPInfo, TEXT("AP_Transmittance"));*/
                mFBO.setRenderTexture(AtmosphereTextures.AtmosphereDeltaE, null);  // TODO need a precisely specified.
                gl.glViewport(0, 0, AtmosphereTextures.AtmosphereDeltaE.getWidth(), AtmosphereTextures.AtmosphereDeltaE.getHeight());

                {
                    /*RHICmdList.ApplyCachedRenderTargets(GraphicsPSOInit);

                    TShaderMapRef<FAtmospherePrecomputeVS> VertexShader(ShaderMap);
                    TShaderMapRef<FAtmosphereIrradiance1PS> PixelShader(ShaderMap);

                    GraphicsPSOInit.BoundShaderState.VertexDeclarationRHI = GFilterVertexDeclaration.VertexDeclarationRHI;
                    GraphicsPSOInit.BoundShaderState.VertexShaderRHI = GETSAFERHISHADER_VERTEX(*VertexShader);
                    GraphicsPSOInit.BoundShaderState.PixelShaderRHI = GETSAFERHISHADER_PIXEL(*PixelShader);
                    GraphicsPSOInit.PrimitiveType = PT_TriangleList;

                    SetGraphicsPipelineState(RHICmdList, GraphicsPSOInit);

                    PixelShader->SetParameters(RHICmdList, AtmosphereTextures); TODO */

                    if(mFAtmosphereIrradiance1PS == null){
                        mFAtmosphereIrradiance1PS = GLSLProgram.createProgram(FAtmospherePrecomputeVS, FAtmosphereIrradiance1PS, null);
                        mFAtmosphereIrradiance1PS.setName("FAtmosphereIrradiance1PS");
                    }

                    DrawQuad(/*RHICmdList, ViewRect, *VertexShader*/);

                    if(!m_printOnce)
                        mFAtmosphereIrradiance1PS.printPrograminfo();
                }
//                RHICmdList.EndRenderPass();
//                RHICmdList.CopyToResolveTarget(DestRenderTarget.TargetableTexture, DestRenderTarget.ShaderResourceTexture, FResolveParams());
            }
            break;
            case AP_Inscatter1:
            {
                int Layer = Atmosphere3DTextureIndex;
                {
                    TextureGL _RenderTargets[] =
                    {
                        AtmosphereTextures.AtmosphereDeltaSR, //->GetRenderTargetItem().TargetableTexture,
                        AtmosphereTextures.AtmosphereDeltaSM, //->GetRenderTargetItem().TargetableTexture,
                    };

                    /*FRHIRenderPassInfo RPInfo(2, RenderTargets, MakeRenderTargetActions(ERenderTargetLoadAction::ELoad, ERenderTargetStoreAction::EStore));
                    RHICmdList.BeginRenderPass(RPInfo, TEXT("AP_Inscatter"));*/
                    mFBO.setRenderTextures(_RenderTargets, null);  // todo
                    gl.glViewport(0,0, AtmosphereTextures.AtmosphereDeltaSR.getWidth(), AtmosphereTextures.AtmosphereDeltaSR.getHeight());
                    {
                        /*RHICmdList.ApplyCachedRenderTargets(GraphicsPSOInit);

                        TShaderMapRef<FAtmospherePrecomputeInscatterVS> VertexShader(ShaderMap);
                        TOptionalShaderMapRef<FAtmosphereGS> GeometryShader(ShaderMap);
                        TShaderMapRef<FAtmosphereInscatter1PS> PixelShader(ShaderMap);

                        GraphicsPSOInit.BoundShaderState.VertexDeclarationRHI = GFilterVertexDeclaration.VertexDeclarationRHI;
                        GraphicsPSOInit.BoundShaderState.VertexShaderRHI = GETSAFERHISHADER_VERTEX(*VertexShader);
                        GraphicsPSOInit.BoundShaderState.GeometryShaderRHI = GETSAFERHISHADER_GEOMETRY(*GeometryShader);
                        GraphicsPSOInit.BoundShaderState.PixelShaderRHI = GETSAFERHISHADER_PIXEL(*PixelShader);
                        GraphicsPSOInit.PrimitiveType = PT_TriangleList;

                        SetGraphicsPipelineState(RHICmdList, GraphicsPSOInit);*/

                        if(mFAtmosphereInscatter1PS == null){
                            mFAtmosphereInscatter1PS = GLSLProgram.createProgram(FAtmospherePrecomputeInscatterVS, FAtmosphereGS, FAtmosphereInscatter1PS, null);
                            mFAtmosphereInscatter1PS.setName("FAtmosphereInscatter1PS");
                        }

                        //
                        float r;
                        Vector4f DhdH = new Vector4f();
                        r = GetLayerValue(Layer, DhdH);
                        /*VertexShader->SetParameters(RHICmdList, Layer);  todo
                        if (GeometryShader.IsValid())
                        {
                            GeometryShader->SetParameters(RHICmdList, Layer);
                        }
                        PixelShader->SetParameters(RHICmdList, View, r, DhdH, AtmosphereTextures);*/
                        DrawQuad(/*RHICmdList, ViewRect, *VertexShader*/);

                        if(!m_printOnce)
                            mFAtmosphereInscatter1PS.printPrograminfo();
                    }
//                    RHICmdList.EndRenderPass();
                    if (Atmosphere3DTextureIndex == PrecomputeParameters.InscatterAltitudeSampleNum - 1)
                    {
                        /*RHICmdList.CopyToResolveTarget(AtmosphereTextures->AtmosphereDeltaSR->GetRenderTargetItem().TargetableTexture,
                                AtmosphereTextures->AtmosphereDeltaSR->GetRenderTargetItem().ShaderResourceTexture, FResolveParams());
                        RHICmdList.CopyToResolveTarget(AtmosphereTextures->AtmosphereDeltaSM->GetRenderTargetItem().TargetableTexture,
                                AtmosphereTextures->AtmosphereDeltaSM->GetRenderTargetItem().ShaderResourceTexture, FResolveParams());*/
                    }
                }
            }
            break;
            case AP_ClearIrradiance:
            {
			    /*const FSceneRenderTargetItem& DestRenderTarget = AtmosphereTextures->AtmosphereIrradiance->GetRenderTargetItem();
                ensure(DestRenderTarget.TargetableTexture->GetClearColor() == FLinearColor::Black);

                FRHIRenderPassInfo RPInfo(DestRenderTarget.TargetableTexture, ERenderTargetActions::Clear_Store);
                RHICmdList.BeginRenderPass(RPInfo, TEXT("AP_ClearIrradiance"));
                RHICmdList.EndRenderPass();
                RHICmdList.CopyToResolveTarget(DestRenderTarget.TargetableTexture, DestRenderTarget.ShaderResourceTexture, FResolveParams());*/
			    gl.glClearTexImage(AtmosphereTextures.AtmosphereIrradiance.getTexture(), 0, GLenum.GL_RGBA, GLenum.GL_FLOAT, null);

            }
            break;
            case AP_CopyInscatter1:
            {
                int Layer = Atmosphere3DTextureIndex;
                {
				    /*const FSceneRenderTargetItem& DestRenderTarget = AtmosphereTextures->AtmosphereInscatter->GetRenderTargetItem();
                    FRHIRenderPassInfo RPInfo(DestRenderTarget.TargetableTexture, ERenderTargetActions::Load_Store);
                    RHICmdList.BeginRenderPass(RPInfo, TEXT("AP_CopyInscatter1"));*/
				    mFBO.setRenderTexture(AtmosphereTextures.AtmosphereInscatter, null); // todo
                    gl.glViewport(0,0, AtmosphereTextures.AtmosphereInscatter.getWidth(), AtmosphereTextures.AtmosphereInscatter.getHeight());
                    {
//                        RHICmdList.ApplyCachedRenderTargets(GraphicsPSOInit);

                        /*TShaderMapRef<FAtmospherePrecomputeInscatterVS> VertexShader(ShaderMap);
                        TOptionalShaderMapRef<FAtmosphereGS> GeometryShader(ShaderMap);
                        TShaderMapRef<FAtmosphereCopyInscatter1PS> PixelShader(ShaderMap);

                        GraphicsPSOInit.BoundShaderState.VertexDeclarationRHI = GFilterVertexDeclaration.VertexDeclarationRHI;
                        GraphicsPSOInit.BoundShaderState.VertexShaderRHI = GETSAFERHISHADER_VERTEX(*VertexShader);
                        GraphicsPSOInit.BoundShaderState.GeometryShaderRHI = GETSAFERHISHADER_GEOMETRY(*GeometryShader);
                        GraphicsPSOInit.BoundShaderState.PixelShaderRHI = GETSAFERHISHADER_PIXEL(*PixelShader);
                        GraphicsPSOInit.PrimitiveType = PT_TriangleList;

                        SetGraphicsPipelineState(RHICmdList, GraphicsPSOInit);*/

                        if(mFAtmosphereCopyInscatter1PS == null){
                            mFAtmosphereCopyInscatter1PS = GLSLProgram.createProgram(FAtmospherePrecomputeInscatterVS, FAtmosphereGS, FAtmosphereCopyInscatter1PS, null);
                            mFAtmosphereCopyInscatter1PS.setName("FAtmosphereCopyInscatter1PS");
                        }

                        float r;
                        Vector4f DhdH = new Vector4f();
                        r = GetLayerValue(Layer, DhdH);
                        /*VertexShader->SetParameters(RHICmdList, Layer);  todo
                        if (GeometryShader.IsValid())
                        {
                            GeometryShader->SetParameters(RHICmdList, Layer);
                        }
                        PixelShader->SetParameters(RHICmdList, View, r, DhdH, Layer, AtmosphereTextures);*/
                        DrawQuad(/*RHICmdList, ViewRect, *VertexShader*/);

                        if(!m_printOnce)
                            mFAtmosphereCopyInscatter1PS.printPrograminfo();
                    }
                    /*RHICmdList.EndRenderPass();
                    if (Atmosphere3DTextureIndex == Component->PrecomputeParams.InscatterAltitudeSampleNum - 1)
                    {
                        RHICmdList.CopyToResolveTarget(DestRenderTarget.TargetableTexture, DestRenderTarget.ShaderResourceTexture, FResolveParams());
                    }*/
                }
            }
            break;
            case AP_InscatterS:
            {
                int Layer = Atmosphere3DTextureIndex;
                {
				    /*const FSceneRenderTargetItem& DestRenderTarget = AtmosphereTextures->AtmosphereDeltaJ->GetRenderTargetItem();
                    FRHIRenderPassInfo RPInfo(DestRenderTarget.TargetableTexture, ERenderTargetActions::Load_Store);
                    RHICmdList.BeginRenderPass(RPInfo, TEXT("InscatterS"));*/
                    mFBO.setRenderTexture(AtmosphereTextures.AtmosphereDeltaJ, null); // todo
                    gl.glViewport(0,0, AtmosphereTextures.AtmosphereDeltaJ.getWidth(), AtmosphereTextures.AtmosphereDeltaJ.getHeight());
                    {
                        /*RHICmdList.ApplyCachedRenderTargets(GraphicsPSOInit);

                        TShaderMapRef<FAtmospherePrecomputeInscatterVS> VertexShader(ShaderMap);
                        TOptionalShaderMapRef<FAtmosphereGS> GeometryShader(ShaderMap);
                        TShaderMapRef<FAtmosphereInscatterSPS> PixelShader(ShaderMap);

                        GraphicsPSOInit.BoundShaderState.VertexDeclarationRHI = GFilterVertexDeclaration.VertexDeclarationRHI;
                        GraphicsPSOInit.BoundShaderState.VertexShaderRHI = GETSAFERHISHADER_VERTEX(*VertexShader);
                        GraphicsPSOInit.BoundShaderState.GeometryShaderRHI = GETSAFERHISHADER_GEOMETRY(*GeometryShader);
                        GraphicsPSOInit.BoundShaderState.PixelShaderRHI = GETSAFERHISHADER_PIXEL(*PixelShader);
                        GraphicsPSOInit.PrimitiveType = PT_TriangleList;

                        SetGraphicsPipelineState(RHICmdList, GraphicsPSOInit);*/

                        if(mFAtmosphereInscatterSPS == null){
                            mFAtmosphereInscatterSPS = GLSLProgram.createProgram(FAtmospherePrecomputeInscatterVS, FAtmosphereGS, FAtmosphereInscatterSPS, null);
                            mFAtmosphereInscatterSPS.setName("FAtmosphereInscatterSPS");
                        }

                        float r;
                        Vector4f DhdH = new Vector4f();
                        r= GetLayerValue(Layer,  DhdH);
                        /*VertexShader->SetParameters(RHICmdList, Layer);  todo
                        if (GeometryShader.IsValid())
                        {
                            GeometryShader->SetParameters(RHICmdList, Layer);
                        }
                        PixelShader->SetParameters(RHICmdList, View, r, DhdH, AtmoshpereOrder == 2 ? 1.f : 0.f, AtmosphereTextures);*/
                        DrawQuad(/*RHICmdList, ViewRect, *VertexShader*/);

                        if(!m_printOnce)
                            mFAtmosphereInscatterSPS.printPrograminfo();
                    }
                    /*RHICmdList.EndRenderPass();
                    if (Atmosphere3DTextureIndex == Component->PrecomputeParams.InscatterAltitudeSampleNum - 1)
                    {
                        RHICmdList.CopyToResolveTarget(DestRenderTarget.TargetableTexture, DestRenderTarget.ShaderResourceTexture, FResolveParams());
                    }*/
                }
            }
            break;
            case AP_IrradianceN:
            {
			    /*const FSceneRenderTargetItem& DestRenderTarget = AtmosphereTextures->AtmosphereDeltaE->GetRenderTargetItem();
                FRHIRenderPassInfo RPInfo(DestRenderTarget.TargetableTexture, ERenderTargetActions::Load_Store);
                RHICmdList.BeginRenderPass(RPInfo, TEXT("IrradianceN"));*/
                mFBO.setRenderTexture(AtmosphereTextures.AtmosphereDeltaE, null); // todo
                gl.glViewport(0,0, AtmosphereTextures.AtmosphereDeltaE.getWidth(), AtmosphereTextures.AtmosphereDeltaE.getHeight());
                {
                    /*RHICmdList.ApplyCachedRenderTargets(GraphicsPSOInit);

                    TShaderMapRef<FAtmospherePrecomputeVS> VertexShader(ShaderMap);
                    TShaderMapRef<FAtmosphereIrradianceNPS> PixelShader(ShaderMap);

                    GraphicsPSOInit.BoundShaderState.VertexDeclarationRHI = GFilterVertexDeclaration.VertexDeclarationRHI;
                    GraphicsPSOInit.BoundShaderState.VertexShaderRHI = GETSAFERHISHADER_VERTEX(*VertexShader);
                    GraphicsPSOInit.BoundShaderState.PixelShaderRHI = GETSAFERHISHADER_PIXEL(*PixelShader);
                    GraphicsPSOInit.PrimitiveType = PT_TriangleList;

                    SetGraphicsPipelineState(RHICmdList, GraphicsPSOInit);

                    PixelShader->SetParameters(RHICmdList, View, AtmoshpereOrder == 2 ? 1.f : 0.f, AtmosphereTextures);*/
                    if(mFAtmosphereIrradianceNPS == null){
                        mFAtmosphereIrradianceNPS = GLSLProgram.createProgram(FAtmospherePrecomputeVS, FAtmosphereIrradianceNPS, null);
                        mFAtmosphereIrradianceNPS.setName("FAtmosphereIrradianceNPS");
                    }

                    DrawQuad(/*RHICmdList, ViewRect, *VertexShader*/);

                    if(!m_printOnce)
                        mFAtmosphereIrradianceNPS.printPrograminfo();
                }
                /*RHICmdList.EndRenderPass();
                RHICmdList.CopyToResolveTarget(DestRenderTarget.TargetableTexture, DestRenderTarget.ShaderResourceTexture, FResolveParams());*/
            }
            break;

            case AP_InscatterN:
            {
                int Layer = Atmosphere3DTextureIndex;
                {
				    /*const FSceneRenderTargetItem& DestRenderTarget = AtmosphereTextures->AtmosphereDeltaSR->GetRenderTargetItem();
                    FRHIRenderPassInfo RPInfo(DestRenderTarget.TargetableTexture, ERenderTargetActions::Load_Store);
                    RHICmdList.BeginRenderPass(RPInfo, TEXT("InscatterN"));*/
                    mFBO.setRenderTexture(AtmosphereTextures.AtmosphereDeltaSR, null); // todo
                    gl.glViewport(0,0, AtmosphereTextures.AtmosphereDeltaSR.getWidth(), AtmosphereTextures.AtmosphereDeltaSR.getHeight());

                    {
                        /*RHICmdList.ApplyCachedRenderTargets(GraphicsPSOInit);

                        TShaderMapRef<FAtmospherePrecomputeInscatterVS> VertexShader(ShaderMap);
                        TOptionalShaderMapRef<FAtmosphereGS> GeometryShader(ShaderMap);
                        TShaderMapRef<FAtmosphereInscatterNPS> PixelShader(ShaderMap);

                        GraphicsPSOInit.BoundShaderState.VertexDeclarationRHI = GFilterVertexDeclaration.VertexDeclarationRHI;
                        GraphicsPSOInit.BoundShaderState.VertexShaderRHI = GETSAFERHISHADER_VERTEX(*VertexShader);
                        GraphicsPSOInit.BoundShaderState.GeometryShaderRHI = GETSAFERHISHADER_GEOMETRY(*GeometryShader);
                        GraphicsPSOInit.BoundShaderState.PixelShaderRHI = GETSAFERHISHADER_PIXEL(*PixelShader);
                        GraphicsPSOInit.PrimitiveType = PT_TriangleList;

                        SetGraphicsPipelineState(RHICmdList, GraphicsPSOInit);*/

                        if(mFAtmosphereInscatterNPS == null){
                            mFAtmosphereInscatterNPS = GLSLProgram.createProgram(FAtmospherePrecomputeInscatterVS, FAtmosphereGS, FAtmosphereInscatterNPS, null);
                            mFAtmosphereInscatterNPS.setName("FAtmosphereInscatterNPS");
                        }

                        float r;
                        Vector4f DhdH = new Vector4f();
                        r= GetLayerValue(Layer,  DhdH);
                        /*VertexShader->SetParameters(RHICmdList, Layer);  todo
                        if (GeometryShader.IsValid())
                        {
                            GeometryShader->SetParameters(RHICmdList, Layer);
                        }
                        PixelShader->SetParameters(RHICmdList, View, r, DhdH, AtmoshpereOrder == 2 ? 1.f : 0.f, AtmosphereTextures);*/
                        DrawQuad(/*RHICmdList, ViewRect, *VertexShader*/);

                        if(!m_printOnce)
                            mFAtmosphereInscatterNPS.printPrograminfo();
                    }
                    /*RHICmdList.EndRenderPass();
                    if (Atmosphere3DTextureIndex == Component->PrecomputeParams.InscatterAltitudeSampleNum - 1)
                    {
                        RHICmdList.CopyToResolveTarget(DestRenderTarget.TargetableTexture, DestRenderTarget.ShaderResourceTexture, FResolveParams());
                    }*/
                }
            }
            break;
            case AP_CopyIrradiance:
            {
			    /*const FSceneRenderTargetItem& DestRenderTarget = AtmosphereTextures->AtmosphereIrradiance->GetRenderTargetItem();
                FRHIRenderPassInfo RPInfo(DestRenderTarget.TargetableTexture, ERenderTargetActions::Load_Store);
                RHICmdList.BeginRenderPass(RPInfo, TEXT("CopyIrradiance"));*/
                mFBO.setRenderTexture(AtmosphereTextures.AtmosphereIrradiance, null); // todo
                gl.glViewport(0,0, AtmosphereTextures.AtmosphereIrradiance.getWidth(), AtmosphereTextures.AtmosphereIrradiance.getHeight());
                {
                    /*RHICmdList.ApplyCachedRenderTargets(GraphicsPSOInit);
                    GraphicsPSOInit.BlendState = TStaticBlendState<CW_RGBA, BO_Add, BF_One, BF_One, BO_Add, BF_One, BF_One>::GetRHI();

                    TShaderMapRef<FAtmospherePrecomputeVS> VertexShader(ShaderMap);
                    TShaderMapRef<FAtmosphereCopyIrradiancePS> PixelShader(ShaderMap);

                    GraphicsPSOInit.BoundShaderState.VertexDeclarationRHI = GFilterVertexDeclaration.VertexDeclarationRHI;
                    GraphicsPSOInit.BoundShaderState.VertexShaderRHI = GETSAFERHISHADER_VERTEX(*VertexShader);
                    GraphicsPSOInit.BoundShaderState.PixelShaderRHI = GETSAFERHISHADER_PIXEL(*PixelShader);
                    GraphicsPSOInit.PrimitiveType = PT_TriangleList;

                    SetGraphicsPipelineState(RHICmdList, GraphicsPSOInit);

                    PixelShader->SetParameters(RHICmdList, AtmosphereTextures);  todo*/

                    if(mFAtmosphereCopyIrradiancePS == null){
                        mFAtmosphereCopyIrradiancePS = GLSLProgram.createProgram(FAtmospherePrecomputeVS, FAtmosphereCopyIrradiancePS, null);
                        mFAtmosphereCopyIrradiancePS.setName("FAtmosphereCopyIrradiancePS");
                    }

                    mFAtmosphereCopyIrradiancePS.enable();
                    DrawQuad(/*RHICmdList, ViewRect, *VertexShader*/);

                    if(!m_printOnce)
                        mFAtmosphereCopyIrradiancePS.printPrograminfo();
                }

                /*RHICmdList.EndRenderPass();
                RHICmdList.CopyToResolveTarget(DestRenderTarget.TargetableTexture, DestRenderTarget.ShaderResourceTexture, FResolveParams());
                GraphicsPSOInit.BlendState = TStaticBlendState<>::GetRHI();*/
            }
            break;

            case AP_CopyInscatterN:
            {
                int Layer = Atmosphere3DTextureIndex;
                {
				    /*const FSceneRenderTargetItem& DestRenderTarget = AtmosphereTextures->AtmosphereInscatter->GetRenderTargetItem();
                    FRHIRenderPassInfo RPInfo(DestRenderTarget.TargetableTexture, ERenderTargetActions::Load_Store);
                    RHICmdList.BeginRenderPass(RPInfo, TEXT("CopyInscatterN"));*/
                    mFBO.setRenderTexture(AtmosphereTextures.AtmosphereInscatter, null); // todo
                    gl.glViewport(0,0, AtmosphereTextures.AtmosphereInscatter.getWidth(), AtmosphereTextures.AtmosphereInscatter.getHeight());

                    {
                        /*RHICmdList.ApplyCachedRenderTargets(GraphicsPSOInit);

                        GraphicsPSOInit.BlendState = TStaticBlendState<CW_RGBA, BO_Add, BF_One, BF_One, BO_Add, BF_One, BF_One>::GetRHI();

                        TShaderMapRef<FAtmospherePrecomputeInscatterVS> VertexShader(ShaderMap);
                        TOptionalShaderMapRef<FAtmosphereGS> GeometryShader(ShaderMap);
                        TShaderMapRef<FAtmosphereCopyInscatterNPS> PixelShader(ShaderMap);

                        GraphicsPSOInit.BoundShaderState.VertexDeclarationRHI = GFilterVertexDeclaration.VertexDeclarationRHI;
                        GraphicsPSOInit.BoundShaderState.VertexShaderRHI = GETSAFERHISHADER_VERTEX(*VertexShader);
                        GraphicsPSOInit.BoundShaderState.GeometryShaderRHI = GETSAFERHISHADER_GEOMETRY(*GeometryShader);
                        GraphicsPSOInit.BoundShaderState.PixelShaderRHI = GETSAFERHISHADER_PIXEL(*PixelShader);
                        GraphicsPSOInit.PrimitiveType = PT_TriangleList;

                        SetGraphicsPipelineState(RHICmdList, GraphicsPSOInit);*/
                        if(mFAtmosphereCopyInscatterNPS == null){
                            mFAtmosphereCopyInscatterNPS = GLSLProgram.createProgram(FAtmospherePrecomputeInscatterVS, FAtmosphereGS, FAtmosphereCopyInscatterNPS, null);
                            mFAtmosphereCopyInscatterNPS.setName("FAtmosphereCopyInscatterNPS");
                        }

                        float r;
                        Vector4f DhdH = new Vector4f();
                        r = GetLayerValue(Layer, DhdH);
                        /*VertexShader->SetParameters(RHICmdList, Layer);
                        if (GeometryShader.IsValid())
                        {
                            GeometryShader->SetParameters(RHICmdList, Layer);
                        }
                        PixelShader->SetParameters(RHICmdList, View, r, DhdH, Layer, AtmosphereTextures); todo*/
                        mFAtmosphereCopyInscatterNPS.enable();
                        DrawQuad(/*RHICmdList, ViewRect, *VertexShader*/);

                        if(!m_printOnce)
                            mFAtmosphereCopyInscatterNPS.printPrograminfo();
                    }
                    /*RHICmdList.EndRenderPass();
                    if (Atmosphere3DTextureIndex == Component->PrecomputeParams.InscatterAltitudeSampleNum - 1)
                    {
                        RHICmdList.CopyToResolveTarget(DestRenderTarget.TargetableTexture, DestRenderTarget.ShaderResourceTexture, FResolveParams());
                    }*/
                }

//                GraphicsPSOInit.BlendState = TStaticBlendState<>::GetRHI();
            }
            break;

            case AP_CopyInscatterF:
            {
                int Layer = Atmosphere3DTextureIndex;
                {
				    /*const FSceneRenderTargetItem& DestRenderTarget = AtmosphereTextures->AtmosphereDeltaSR->GetRenderTargetItem();
                    FRHIRenderPassInfo RPInfo(DestRenderTarget.TargetableTexture, ERenderTargetActions::Load_Store);
                    RHICmdList.BeginRenderPass(RPInfo, TEXT("CopyInscatterF"));*/

                    mFBO.setRenderTexture(AtmosphereTextures.AtmosphereDeltaSR, null); // todo
                    gl.glViewport(0,0, AtmosphereTextures.AtmosphereDeltaSR.getWidth(), AtmosphereTextures.AtmosphereDeltaSR.getHeight());
                    {
                        /*RHICmdList.ApplyCachedRenderTargets(GraphicsPSOInit);

                        TShaderMapRef<FAtmospherePrecomputeInscatterVS> VertexShader(ShaderMap);
                        TOptionalShaderMapRef<FAtmosphereGS> GeometryShader(ShaderMap);
                        TShaderMapRef<FAtmosphereCopyInscatterFPS> PixelShader(ShaderMap);

                        GraphicsPSOInit.BoundShaderState.VertexDeclarationRHI = GFilterVertexDeclaration.VertexDeclarationRHI;
                        GraphicsPSOInit.BoundShaderState.VertexShaderRHI = GETSAFERHISHADER_VERTEX(*VertexShader);
                        GraphicsPSOInit.BoundShaderState.GeometryShaderRHI = GETSAFERHISHADER_GEOMETRY(*GeometryShader);
                        GraphicsPSOInit.BoundShaderState.PixelShaderRHI = GETSAFERHISHADER_PIXEL(*PixelShader);
                        GraphicsPSOInit.PrimitiveType = PT_TriangleList;

                        SetGraphicsPipelineState(RHICmdList, GraphicsPSOInit);*/
                        if(mFAtmosphereCopyInscatterFPS == null){
                            mFAtmosphereCopyInscatterFPS = GLSLProgram.createProgram(FAtmospherePrecomputeInscatterVS, FAtmosphereGS, FAtmosphereCopyInscatterFPS, null);
                            mFAtmosphereCopyInscatterFPS.setName("FAtmosphereCopyInscatterFPS");
                        }

                        float r;
                        Vector4f DhdH = new Vector4f();
                        r = GetLayerValue(Layer, DhdH);
                        /*VertexShader->SetParameters(RHICmdList, Layer);  todo
                        if (GeometryShader.IsValid())
                        {
                            GeometryShader->SetParameters(RHICmdList, Layer);
                        }
                        PixelShader->SetParameters(RHICmdList, r, DhdH, Layer, AtmosphereTextures);*/

                        mFAtmosphereCopyInscatterFPS.enable();
                        DrawQuad(/*RHICmdList, ViewRect, *VertexShader*/);

                        if(!m_printOnce)
                            mFAtmosphereCopyInscatterFPS.printPrograminfo();
                    }
                    /*RHICmdList.EndRenderPass();
                    if (Atmosphere3DTextureIndex == Component->PrecomputeParams.InscatterAltitudeSampleNum - 1)
                    {
                        RHICmdList.CopyToResolveTarget(DestRenderTarget.TargetableTexture, DestRenderTarget.ShaderResourceTexture, FResolveParams());
                    }*/
                }
            }
            break;
            case AP_CopyInscatterFBack:
            {
                int Layer = Atmosphere3DTextureIndex;
                {
				    /*const FSceneRenderTargetItem& DestRenderTarget = AtmosphereTextures->AtmosphereInscatter->GetRenderTargetItem();
                    FRHIRenderPassInfo RPInfo(DestRenderTarget.TargetableTexture, ERenderTargetActions::Load_Store);
                    RHICmdList.BeginRenderPass(RPInfo, TEXT("CopyInscatterFBack"));*/

                    mFBO.setRenderTexture(AtmosphereTextures.AtmosphereInscatter, null); // todo
                    gl.glViewport(0,0, AtmosphereTextures.AtmosphereInscatter.getWidth(), AtmosphereTextures.AtmosphereInscatter.getHeight());
                    {
                        /*RHICmdList.ApplyCachedRenderTargets(GraphicsPSOInit);

                        TShaderMapRef<FAtmospherePrecomputeInscatterVS> VertexShader(ShaderMap);
                        TOptionalShaderMapRef<FAtmosphereGS> GeometryShader(ShaderMap);
                        TShaderMapRef<FAtmosphereCopyInscatterFBackPS> PixelShader(ShaderMap);

                        GraphicsPSOInit.BoundShaderState.VertexDeclarationRHI = GFilterVertexDeclaration.VertexDeclarationRHI;
                        GraphicsPSOInit.BoundShaderState.VertexShaderRHI = GETSAFERHISHADER_VERTEX(*VertexShader);
                        GraphicsPSOInit.BoundShaderState.GeometryShaderRHI = GETSAFERHISHADER_GEOMETRY(*GeometryShader);
                        GraphicsPSOInit.BoundShaderState.PixelShaderRHI = GETSAFERHISHADER_PIXEL(*PixelShader);
                        GraphicsPSOInit.PrimitiveType = PT_TriangleList;

                        SetGraphicsPipelineState(RHICmdList, GraphicsPSOInit);*/
                        if(mFAtmosphereCopyInscatterFBackPS == null){
                            mFAtmosphereCopyInscatterFBackPS = GLSLProgram.createProgram(FAtmospherePrecomputeInscatterVS, FAtmosphereGS, FAtmosphereCopyInscatterFBackPS, null);
                            mFAtmosphereCopyInscatterFBackPS.setName("FAtmosphereCopyInscatterFBackPS");
                        }

                        float r;
                        Vector4f DhdH = new Vector4f();
                        r = GetLayerValue(Layer, DhdH);
                        /*VertexShader->SetParameters(RHICmdList, Layer);
                        if (GeometryShader.IsValid())
                        {
                            GeometryShader->SetParameters(RHICmdList, Layer);
                        }
                        PixelShader->SetParameters(RHICmdList, r, DhdH, Layer, AtmosphereTextures);*/

                        mFAtmosphereCopyInscatterFBackPS.enable();
                        DrawQuad(/*RHICmdList, ViewRect, *VertexShader*/);

                        if(!m_printOnce)
                            mFAtmosphereCopyInscatterFBackPS.printPrograminfo();
                    }
                    /*RHICmdList.EndRenderPass();
                    if (Atmosphere3DTextureIndex == Component->PrecomputeParams.InscatterAltitudeSampleNum - 1)
                    {
                        RHICmdList.CopyToResolveTarget(DestRenderTarget.TargetableTexture, DestRenderTarget.ShaderResourceTexture, FResolveParams());
                    }*/
                }
            }
            break;

        }
    }

    private void PrecomputeAtmosphereData(/*FRHICommandListImmediate& RHICmdList, const FViewInfo* View, FSceneViewFamily& ViewFamily*/){
        final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

        // Set the view family's render target/viewport.
        Vector3i TexSize = GetTextureSize();
//        FIntRect ViewRect(0, 0, TexSize.X, TexSize.Y);

//        FGraphicsPipelineStateInitializer GraphicsPSOInit;

        // turn off culling and blending
//        GraphicsPSOInit.RasterizerState = TStaticRasterizerState<FM_Solid, CM_None>::GetRHI();
//        GraphicsPSOInit.BlendState = TStaticBlendState<>::GetRHI();
        // turn off depth reads/writes
//        GraphicsPSOInit.DepthStencilState = TStaticDepthStencilState<false, CF_Always>::GetRHI();

//        RHICmdList.SetViewport(0, 0, 0.0f, TexSize.X, TexSize.Y, 0.0f);
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glDisable(GLenum.GL_BLEND);
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glViewport(0,0, TexSize.x, TexSize.y);
        RenderAtmosphereShaders(/*RHICmdList, GraphicsPSOInit, *View, ViewRect*/);
    }

    private ByteBuffer ReadPixelsPtr(/*FRHICommandListImmediate& RHICmdList,*/ TextureGL RenderTarget/*, ByteBuffer OutData, Recti InRect*/){
        ByteBuffer source = TextureUtils.getTextureData(RenderTarget.getTarget(), RenderTarget.getTexture(), 0, true);
        final int pixelCount = RenderTarget.getWidth() * RenderTarget.getHeight() * Math.max(1, RenderTarget.getDepth());

        ByteBuffer OutData = BufferUtils.createByteBuffer(pixelCount * 4);
        int index = 0;
        // Convert from FFloat16Color to FColor
        for (int i = 0; i < pixelCount; ++i)
        {
//            R = FMath::Clamp<uint8>(Data[i].R.GetFloat() * 255, 0, 255);
//            TempColor.G = FMath::Clamp<uint8>(Data[i].G.GetFloat() * 255, 0, 255);
//            TempColor.B = FMath::Clamp<uint8>(Data[i].B.GetFloat() * 255, 0, 255);
//            OutData[i] = TempColor;

            byte r = (byte)Numeric.clamp(Numeric.convertHFloatToFloat(source.getShort()) * 255, 0, 255);
            byte g = (byte)Numeric.clamp(Numeric.convertHFloatToFloat(source.getShort()) * 255, 0, 255);
            byte b = (byte)Numeric.clamp(Numeric.convertHFloatToFloat(source.getShort()) * 255, 0, 255);

            source.getShort();  // skip the alpha.

            OutData.put(index++, r);
            OutData.put(index++, g);
            OutData.put(index++, b);
            OutData.put(index++, (byte)255);
        }

        return OutData;
    }

    private ByteBuffer Read3DPixelsPtr(/*FRHICommandListImmediate& RHICmdList,*/ TextureGL RenderTarget/*, ByteBuffer OutData, FIntRect InRect, FIntPoint InZMinMax*/){
        return TextureUtils.getTextureData(RenderTarget.getTarget(), RenderTarget.getTexture(), 0, false);
//        OutData.put(source).flip();
    }
}
