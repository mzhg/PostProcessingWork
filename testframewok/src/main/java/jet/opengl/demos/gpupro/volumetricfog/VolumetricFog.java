package jet.opengl.demos.gpupro.volumetricfog;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector3i;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture3D;
import jet.opengl.postprocessing.texture.Texture3DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.Numeric;

class VolumetricFog implements Disposeable {

    private MaterialSetupCS materialSetupCS;
    private InjectShadowedLocalLightProgram injectProg;
    private VolumetricFogLightScatteringCS lightScatteringCS;
    private VolumetricFogFinalIntegrationCS finalIntegrationCS;

    private Texture3D VBufferA;
    private Texture3D VBufferB;
    private TextureGL LightFunctionTexture;
    private Texture3D pTex3DLightScattering;
    private Texture3D pTex3DFinalLightScattering;

    private final Vector3i volumetricFogGridSize = new Vector3i();

    private Params params;
    private int VolumetricFogGridInjectionGroupSize = 4;
    private boolean m_princeOnce;

    private final MaterialSetupParams materialParams = new MaterialSetupParams();
    private GLFuncProvider gl;

    private int frameNumber;
    public void renderVolumetricFog(Params params){
        this.params = params;
        frameNumber++;

        initlizeResources();

        final int viewWith = params.sceneColor.getWidth();
        final int viewHeight = params.sceneColor.getHeight();
        Vector3i volumetricFogGridSize = getVolumetricFogGridSize(viewWith, viewHeight);
        ReadableVector3f GridZParams = GetVolumetricFogGridZParams(params.NearClippingDistance, params.VolumetricFogDistance, volumetricFogGridSize.z);

        //@DW - graph todo
        //SCOPED_DRAW_EVENT(RHICmdList, VolumetricFog);

        ReadableVector3f FrameJitterOffsetValue = VolumetricFogTemporalRandom(frameNumber);

        FVolumetricFogIntegrationParameterData IntegrationData = new FVolumetricFogIntegrationParameterData();
        IntegrationData.FrameJitterOffsetValues= new Vector4f[16];
//        IntegrationData.FrameJitterOffsetValues.AddZeroed(16);
        IntegrationData.FrameJitterOffsetValues[0] = new Vector4f(VolumetricFogTemporalRandom(frameNumber), 0);

        for (int FrameOffsetIndex = 1; FrameOffsetIndex < params.GVolumetricFogHistoryMissSupersampleCount; FrameOffsetIndex++)
        {
            IntegrationData.FrameJitterOffsetValues[FrameOffsetIndex] =new Vector4f(VolumetricFogTemporalRandom(frameNumber - FrameOffsetIndex), 0);
        }

        final boolean bUseTemporalReprojection = params. GVolumetricFogTemporalReprojection/*
                    && View.ViewState*/;

        IntegrationData.bTemporalHistoryIsValid =
                bUseTemporalReprojection
                        /*&& !View.bCameraCut   todo
                        && !View.bPrevTransformsReset
                        && ViewFamily.bRealtimeUpdate
                        && View.ViewState->LightScatteringHistory*/;

        Matrix4f LightFunctionWorldToShadow = new Matrix4f();

//        FRDGBuilder GraphBuilder(RHICmdListImmediate);

        //@DW - register GWhiteTexture as a graph external for when there's no light function - later a shader is going to bind it whether we rendered to it or not
//			const FRDGTexture* LightFunctionTexture = GraphBuilder.RegisterExternalTexture(GSystemTextures.WhiteDummy);
        boolean bUseDirectionalLightShadowing = RenderLightFunctionForVolumetricFog(
//                GraphBuilder,
//                View,
                volumetricFogGridSize,
                params.VolumetricFogDistance,
                LightFunctionWorldToShadow,
                LightFunctionTexture);

        /*const uint32 Flags = TexCreate_ShaderResource | TexCreate_RenderTargetable | TexCreate_UAV | TexCreate_ReduceMemoryWithTilingMode;
        FPooledRenderTargetDesc VolumeDesc(FPooledRenderTargetDesc::CreateVolumeDesc(VolumetricFogGridSize.X, VolumetricFogGridSize.Y, VolumetricFogGridSize.Z, PF_FloatRGBA, FClearValueBinding::Black, TexCreate_None, Flags, false));
        FPooledRenderTargetDesc VolumeDescFastVRAM = VolumeDesc;
        VolumeDescFastVRAM.Flags |= GFastVRamConfig.VolumetricFog;

        //@DW - Explicit creation of graph resource handles
        //@DW - Passing these around in a struct to ease manual wiring
        IntegrationData.VBufferA = GraphBuilder.CreateTexture(VolumeDescFastVRAM, TEXT("VBufferA"));
        IntegrationData.VBufferB = GraphBuilder.CreateTexture(VolumeDescFastVRAM, TEXT("VBufferB"));
        IntegrationData.VBufferA_UAV = GraphBuilder.CreateUAV(FRDGTextureUAVDesc(IntegrationData.VBufferA));
        IntegrationData.VBufferB_UAV = GraphBuilder.CreateUAV(FRDGTextureUAVDesc(IntegrationData.VBufferB));*/

        if(VBufferA == null || VBufferA.getWidth() != volumetricFogGridSize.x || VBufferA.getHeight() != volumetricFogGridSize.y || VBufferA.getDepth() != volumetricFogGridSize.z){
            SAFE_RELEASE(VBufferA);
            SAFE_RELEASE(VBufferB);
            SAFE_RELEASE(pTex3DLightScattering);
            SAFE_RELEASE(pTex3DFinalLightScattering);

            Texture3DDesc desc = new Texture3DDesc(volumetricFogGridSize.x, volumetricFogGridSize.y, volumetricFogGridSize.z, 1, GLenum.GL_RGBA16F);
            VBufferA = TextureUtils.createTexture3D(desc, null);
            VBufferB = TextureUtils.createTexture3D(desc, null);
            pTex3DLightScattering = TextureUtils.createTexture3D(desc, null);
            pTex3DFinalLightScattering = TextureUtils.createTexture3D(desc, null);

            VBufferA.setName("VBufferA");
            VBufferB.setName("VBufferB");
            pTex3DLightScattering.setName("LightScattering");
        }

        IntegrationData.VBufferARenderTarget = VBufferA;
        IntegrationData.VBufferBRenderTarget = VBufferB;

        {
            /*FVolumetricFogMaterialSetupCS::FParameters* PassParameters = GraphBuilder.AllocParameters<FVolumetricFogMaterialSetupCS::FParameters>();
            PassParameters->GlobalAlbedo = FogInfo.VolumetricFogAlbedo;
            PassParameters->GlobalEmissive = FogInfo.VolumetricFogEmissive;
            PassParameters->GlobalExtinctionScale = FogInfo.VolumetricFogExtinctionScale;

            PassParameters->RWVBufferA = IntegrationData.VBufferA_UAV;
            PassParameters->RWVBufferB = IntegrationData.VBufferB_UAV;

            FFogUniformParameters FogUniformParameters;
            SetupFogUniformParameters(View, FogUniformParameters);
            PassParameters->FogUniformParameters = CreateUniformBufferImmediate(FogUniformParameters, UniformBuffer_SingleDraw);
            PassParameters->View = View.ViewUniformBuffer;

            auto ComputeShader = View.ShaderMap->GetShader< FVolumetricFogMaterialSetupCS >();
            ClearUnusedGraphResources(ComputeShader, PassParameters);

            //@DW - this pass only reads external textures, we don't have any graph inputs
            GraphBuilder.AddPass(
                    RDG_EVENT_NAME("InitializeVolumeAttributes"),
                    PassParameters,
                    ERenderGraphPassFlags::Compute,
                    [PassParameters, &View, VolumetricFogGridSize, IntegrationData, ComputeShader](FRHICommandListImmediate& RHICmdList)
                {
					const FIntVector NumGroups = FIntVector::DivideAndRoundUp(VolumetricFogGridSize, VolumetricFogGridInjectionGroupSize);

            RHICmdList.SetComputeShader(ComputeShader->GetComputeShader());

            ComputeShader->SetParameters(RHICmdList, View, IntegrationData);

            SetShaderParameters(RHICmdList, ComputeShader, ComputeShader->GetComputeShader(), *PassParameters);
            DispatchComputeShader(RHICmdList, ComputeShader, NumGroups.X, NumGroups.Y, NumGroups.Z);
            UnsetShaderUAVs(RHICmdList, ComputeShader, ComputeShader->GetComputeShader());
				});*/

            final int numGroupX = Numeric.divideAndRoundUp(volumetricFogGridSize.x, VolumetricFogGridInjectionGroupSize);
            final int numGroupY = Numeric.divideAndRoundUp(volumetricFogGridSize.y, VolumetricFogGridInjectionGroupSize);
            final int numGroupZ = Numeric.divideAndRoundUp(volumetricFogGridSize.z, VolumetricFogGridInjectionGroupSize);

            materialSetupCS.enable();
            MaterialSetupParams setupParams = buildMaterialSetupParameters();
            setupParams.VolumetricFog_GridSize.set(volumetricFogGridSize.x, volumetricFogGridSize.y, volumetricFogGridSize.z);
            setupParams.VolumetricFog_GridZParams.set(GridZParams);
            materialSetupCS.applyParameters(setupParams, IntegrationData);

            gl.glDispatchCompute(numGroupX, numGroupY, numGroupZ);
            materialSetupCS.unbind();

            VoxelizeFogVolumePrimitives(
//                    GraphBuilder,
//                    View,
                    IntegrationData,
                    volumetricFogGridSize,
                    GridZParams,
                    params.VolumetricFogDistance);

            if(!m_princeOnce)
                materialSetupCS.printPrograminfo();
        }

//        const FRDGTexture* LocalShadowedLightScattering = GraphBuilder.RegisterExternalTexture(GSystemTextures.VolumetricBlackDummy);
        Texture3D LocalShadowedLightScattering =
        RenderLocalLightsForVolumetricFog(/*GraphBuilder, View,*/ bUseTemporalReprojection, IntegrationData, /*FogInfo,*/ volumetricFogGridSize, GridZParams/*, VolumeDescFastVRAM, LocalShadowedLightScattering*/);

//        IntegrationData.LightScattering = GraphBuilder.CreateTexture(VolumeDesc, TEXT("LightScattering"));
//        IntegrationData.LightScatteringUAV = GraphBuilder.CreateUAV(FRDGTextureUAVDesc(IntegrationData.LightScattering));
        IntegrationData.LightScatteringRenderTarget =pTex3DLightScattering;

        {
            /*TVolumetricFogLightScatteringCS::FParameters* PassParameters = GraphBuilder.AllocParameters<TVolumetricFogLightScatteringCS::FParameters>();

            PassParameters->VBufferA = IntegrationData.VBufferA;
            PassParameters->VBufferB = IntegrationData.VBufferB;
            PassParameters->LocalShadowedLightScattering = LocalShadowedLightScattering;
            PassParameters->LightFunctionTexture = LightFunctionTexture;
            PassParameters->RWLightScattering = IntegrationData.LightScatteringUAV;

				const bool bUseGlobalDistanceField = UseGlobalDistanceField() && Scene->DistanceFieldSceneData.NumObjectsInBuffer > 0;

				const bool bUseDistanceFieldSkyOcclusion =
                ViewFamily.EngineShowFlags.AmbientOcclusion
                        && Scene->SkyLight
                        && Scene->SkyLight->bCastShadows
                        && Scene->SkyLight->bCastVolumetricShadow
                        && ShouldRenderDistanceFieldAO()
                        && SupportsDistanceFieldAO(View.GetFeatureLevel(), View.GetShaderPlatform())
                        && bUseGlobalDistanceField
                        && Views.Num() == 1
                        && View.IsPerspectiveProjection();

            TVolumetricFogLightScatteringCS::FPermutationDomain PermutationVector;
            PermutationVector.Set< TVolumetricFogLightScatteringCS::FTemporalReprojection >(bUseTemporalReprojection);
            PermutationVector.Set< TVolumetricFogLightScatteringCS::FDistanceFieldSkyOcclusion >(bUseDistanceFieldSkyOcclusion);

            auto ComputeShader = View.ShaderMap->GetShader< TVolumetricFogLightScatteringCS >(PermutationVector);
            ClearUnusedGraphResources(ComputeShader, PassParameters);

            GraphBuilder.AddPass(
                    RDG_EVENT_NAME("LightScattering %dx%dx%d %s %s",
                            VolumetricFogGridSize.X,
                            VolumetricFogGridSize.Y,
                            VolumetricFogGridSize.Z,
                            bUseDistanceFieldSkyOcclusion ? TEXT("DFAO") : TEXT(""),
                            PassParameters->LightFunctionTexture ? TEXT("LF") : TEXT("")),
                    PassParameters,
                    ERenderGraphPassFlags::Compute,
                    [PassParameters, ComputeShader, &View, this, FogInfo, bUseTemporalReprojection, VolumetricFogGridSize, IntegrationData, bUseDirectionalLightShadowing, bUseDistanceFieldSkyOcclusion, LightFunctionWorldToShadow](FRHICommandListImmediate& RHICmdList)
                {
                        UnbindRenderTargets(RHICmdList);
					const FIntVector NumGroups = FIntVector::DivideAndRoundUp(VolumetricFogGridSize, VolumetricFogGridInjectionGroupSize);

            RHICmdList.SetComputeShader(ComputeShader->GetComputeShader());

            FTextureRHIParamRef LightScatteringHistoryTexture = bUseTemporalReprojection && View.ViewState->LightScatteringHistory.IsValid()
                ? View.ViewState->LightScatteringHistory->GetRenderTargetItem().ShaderResourceTexture
						: GBlackVolumeTexture->TextureRHI;

            ComputeShader->SetParameters(RHICmdList, View, IntegrationData, FogInfo, LightScatteringHistoryTexture, bUseDirectionalLightShadowing, LightFunctionWorldToShadow);

            SetShaderParameters(RHICmdList, ComputeShader, ComputeShader->GetComputeShader(), *PassParameters);
            DispatchComputeShader(RHICmdList, ComputeShader, NumGroups.X, NumGroups.Y, NumGroups.Z);
            UnsetShaderUAVs(RHICmdList, ComputeShader, ComputeShader->GetComputeShader());
				});*/

            final boolean bUseGlobalDistanceField = false; /* UseGlobalDistanceField() && Scene->DistanceFieldSceneData.NumObjectsInBuffer > 0*/;
            final boolean bUseDistanceFieldSkyOcclusion = false;
            VolumetricFogLightScatteringCS program = findLightScatteringCS(bUseGlobalDistanceField, bUseDistanceFieldSkyOcclusion);
            program.enable();

            final int numGroupX = Numeric.divideAndRoundUp(volumetricFogGridSize.x, VolumetricFogGridInjectionGroupSize);
            final int numGroupY = Numeric.divideAndRoundUp(volumetricFogGridSize.y, VolumetricFogGridInjectionGroupSize);
            final int numGroupZ = Numeric.divideAndRoundUp(volumetricFogGridSize.z, VolumetricFogGridInjectionGroupSize);
            // TODO Setup parameters.

        }

//        const FRDGTexture* IntegratedLightScattering = GraphBuilder.CreateTexture(VolumeDesc, TEXT("IntegratedLightScattering"));
//			const FRDGTextureUAV* IntegratedLightScatteringUAV = GraphBuilder.CreateUAV(FRDGTextureUAVDesc(IntegratedLightScattering));
        Texture3D IntegratedLightScattering = pTex3DFinalLightScattering;
        Texture3D IntegratedLightScatteringUAV = pTex3DFinalLightScattering;

        {
            /*FVolumetricFogFinalIntegrationCS::FParameters* PassParameters = GraphBuilder.AllocParameters<FVolumetricFogFinalIntegrationCS::FParameters>();
            PassParameters->LightScattering = IntegrationData.LightScattering;
            PassParameters->RWIntegratedLightScattering = IntegratedLightScatteringUAV;

            GraphBuilder.AddPass(
                    RDG_EVENT_NAME("FinalIntegration"),
                    PassParameters,
                    ERenderGraphPassFlags::Compute,
                    [PassParameters, &View, VolumetricFogGridSize, IntegrationData, this](FRHICommandListImmediate& RHICmdList)
                {
					const FIntVector NumGroups = FIntVector::DivideAndRoundUp(VolumetricFogGridSize, VolumetricFogIntegrationGroupSize);

            auto ComputeShader = View.ShaderMap->GetShader< FVolumetricFogFinalIntegrationCS >();
            RHICmdList.SetComputeShader(ComputeShader->GetComputeShader());
            ComputeShader->SetParameters(RHICmdList, View, IntegrationData);

            SetShaderParameters(RHICmdList, ComputeShader, ComputeShader->GetComputeShader(), *PassParameters);
            DispatchComputeShader(RHICmdList, ComputeShader, NumGroups.X, NumGroups.Y, 1);
            UnsetShaderUAVs(RHICmdList, ComputeShader, ComputeShader->GetComputeShader());
				});*/

            final int numGroupX = Numeric.divideAndRoundUp(volumetricFogGridSize.x, VolumetricFogGridInjectionGroupSize);
            final int numGroupY = Numeric.divideAndRoundUp(volumetricFogGridSize.y, VolumetricFogGridInjectionGroupSize);
            final int numGroupZ = Numeric.divideAndRoundUp(volumetricFogGridSize.z, VolumetricFogGridInjectionGroupSize);

            finalIntegrationCS.enable();
            // TODO setup the uniforms.
        }

        /*GraphBuilder.QueueTextureExtraction(IntegratedLightScattering, &View.VolumetricFogResources.IntegratedLightScattering);

        if (bUseTemporalReprojection)  todo
        {
            GraphBuilder.QueueTextureExtraction(IntegrationData.LightScattering, &View.ViewState->LightScatteringHistory);
        }
        else if (View.ViewState)
        {
            View.ViewState->LightScatteringHistory = NULL;
        }*/
    }

    private boolean RenderLightFunctionForVolumetricFog(Vector3i volumetricFogGridSize, float volumetricFogDistance, Matrix4f lightFunctionWorldToShadow, TextureGL lightFunctionTexture) {
//        throw new UnsupportedOperationException();
        return true;
    }

    private Texture3D RenderLocalLightsForVolumetricFog(boolean bUseTemporalReprojection, FVolumetricFogIntegrationParameterData integrationData, Vector3i volumetricFogGridSize, ReadableVector3f gridZParams) {
        throw new UnsupportedOperationException();
    }

    private void VoxelizeFogVolumePrimitives(FVolumetricFogIntegrationParameterData integrationData, Vector3i volumetricFogGridSize, ReadableVector3f gridZParams, float volumetricFogDistance) {
//        throw new UnsupportedOperationException();
    }

    private void initlizeResources(){
        if(gl == null)
            gl = GLFuncProviderFactory.getGLFuncProvider();

        if(materialSetupCS == null){
            final String prefix = "gpupro/VolumetricFog/shaders/";
            materialSetupCS = new MaterialSetupCS(prefix, VolumetricFogGridInjectionGroupSize);
            materialSetupCS.setName("MaterialSetupCS");
        }
    }

    private VolumetricFogLightScatteringCS findLightScatteringCS(boolean bUseGlobalDistanceField, boolean bUseDistanceFieldSkyOcclusion){
        throw new UnsupportedOperationException();
    }

    private Vector3i getVolumetricFogGridSize(int width, int height){
//        const FIntPoint VolumetricFogGridSizeXY = FIntPoint::DivideAndRoundUp(ViewRectSize, GVolumetricFogGridPixelSize);
//        return FIntVector(VolumetricFogGridSizeXY.X, VolumetricFogGridSizeXY.Y, GVolumetricFogGridSizeZ);

        volumetricFogGridSize.x = Numeric.divideAndRoundUp(width, params.GVolumetricFogGridPixelSize);
        volumetricFogGridSize.y = Numeric.divideAndRoundUp(height, params.GVolumetricFogGridPixelSize);
        volumetricFogGridSize.z = params.GVolumetricFogGridSizeZ;
        return volumetricFogGridSize;
    }

    private Vector3f GetVolumetricFogGridZParams(float NearPlane, float FarPlane, int GridSizeZ)
    {
        // S = distribution scale
        // B, O are solved for given the z distances of the first+last slice, and the # of slices.
        //
        // slice = log2(z*B + O) * S

        // Don't spend lots of resolution right in front of the near plane
        double NearOffset = .095 * 100;
        // Space out the slices so they aren't all clustered at the near plane
        double S = params.GVolumetricFogDepthDistributionScale;

        double N = NearPlane + NearOffset;
        double F = FarPlane;

        double O = (F - N * Numeric.exp2((GridSizeZ - 1) / S)) / (F - N);
        double B = (1 - O) / N;

        double O2 = (Numeric.exp2((GridSizeZ - 1) / S) - F / N) / (-F / N + 1);

        float FloatN = (float)N;
        float FloatF = (float)F;
        float FloatB = (float)B;
        float FloatO = (float)O;
        float FloatS = (float)S;

        float NSlice = (float)Numeric.log2(FloatN*FloatB + FloatO) * FloatS;
        float NearPlaneSlice = (float)Numeric.log2(NearPlane*FloatB + FloatO) * FloatS;
        float FSlice = (float)Numeric.log2(FloatF*FloatB + FloatO) * FloatS;
        // y = log2(z*B + O) * S
        // f(N) = 0 = log2(N*B + O) * S
        // 1 = N*B + O
        // O = 1 - N*B
        // B = (1 - O) / N

        // f(F) = GLightGridSizeZ - 1 = log2(F*B + O) * S
        // exp2((GLightGridSizeZ - 1) / S) = F*B + O
        // exp2((GLightGridSizeZ - 1) / S) = F * (1 - O) / N + O
        // exp2((GLightGridSizeZ - 1) / S) = F / N - F / N * O + O
        // exp2((GLightGridSizeZ - 1) / S) = F / N + (-F / N + 1) * O
        // O = (exp2((GLightGridSizeZ - 1) / S) - F / N) / (-F / N + 1)

        return new Vector3f((float)B, (float)O, (float)S);
    }

    Vector3f VolumetricFogTemporalRandom(int FrameNumber)
    {
        // Center of the voxel
        Vector3f RandomOffsetValue = new Vector3f(.5f, .5f, .5f);

        if (params.GVolumetricFogJitter && params.GVolumetricFogTemporalReprojection)
        {
            RandomOffsetValue.set(TemporalHalton(FrameNumber & 1023, 2), TemporalHalton(FrameNumber & 1023, 3), TemporalHalton(FrameNumber & 1023, 5));
        }

        return RandomOffsetValue;
    }

    private static float TemporalHalton(int Index, int Base)
    {
        float Result = 0.0f;
        float InvBase = 1.0f / Base;
        float Fraction = InvBase;
        while (Index > 0)
        {
            Result += (Index % Base) * Fraction;
            Index /= Base;
            Fraction *= InvBase;
        }
        return Result;
    }

    @Override
    public void dispose() {

    }

    private MaterialSetupParams buildMaterialSetupParameters(){
        // FogStruct_ExponentialFogParameters3.x : Fog Density
        // FogStruct_ExponentialFogParameters3.y : Start distance to the camera.
        // FogStruct_ExponentialFogParameters.y :may be the Fog Height falloff
        // FogStruct_ExponentialFogParameters2.z : Extinction Scale
        // FogStruct_ExponentialFogParameters2.y : View distance to the camera.
        // FogStruct_ExponentialFogParameters2.y : Static Lighting Scattering Intensity.

        materialParams.ExponentialFogParameters.y = params.fogHeightFalloffset;
        materialParams.ExponentialFogParameters3.x = params.fogDensity;
        materialParams.ExponentialFogParameters3.y = params.startDistance;

        Matrix4f.mul(params.proj, params.view, materialParams.g_ViewProj);
        Matrix4f.invert(materialParams.g_ViewProj, materialParams.UnjitteredClipToTranslatedWorld);

        materialParams.GlobalAlbedo.set(params.GlobalAlbedo);
        materialParams.GlobalEmissive.set(params.GlobalEmissive);
        materialParams.GlobalExtinctionScale = params.GlobalExtinctionScale;

        return materialParams;
    }

    public static final class Params{
        public Texture2D sceneColor;
        public Texture2D sceneDepth;
        public Texture2D shadowMap;

        public float fogHeightFalloffset = 1;

        public float fogDensity = 0.02f;

        public float startDistance = 10;

        public Matrix4f view;
        public Matrix4f proj;

        public final Vector3f GlobalAlbedo = new Vector3f(1,1,1);
        public final Vector3f GlobalEmissive = new Vector3f();
        public float GlobalExtinctionScale = 1;

        public int GVolumetricFogGridPixelSize = 16;
        public int GVolumetricFogGridSizeZ = 64;
        public float GVolumetricFogDepthDistributionScale = 32.f;
        public float NearClippingDistance = 0.1f;
        public float VolumetricFogDistance = 1000.f;

        /** Whether to apply jitter to each frame's volumetric fog computation, achieving temporal super sampling.*/
        public boolean GVolumetricFogJitter = true;
        /** Whether to use temporal reprojection on volumetric fog.*/
        public boolean GVolumetricFogTemporalReprojection = true;

        /**
         * Number of lighting samples to compute for voxels whose history value is not available.
         * This reduces noise when panning or on camera cuts, but introduces a variable cost to volumetric fog computation.  Valid range [1, 16].
         */
        public int GVolumetricFogHistoryMissSupersampleCount = 4;

    }
}
