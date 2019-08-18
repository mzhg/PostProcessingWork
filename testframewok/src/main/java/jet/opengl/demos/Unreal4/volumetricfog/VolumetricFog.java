package jet.opengl.demos.Unreal4.volumetricfog;


import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector3i;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jet.opengl.demos.Unreal4.UE4LightCollections;
import jet.opengl.demos.Unreal4.UE4LightInfo;
import jet.opengl.demos.intel.va.VaBoundingSphere;
import jet.opengl.demos.nvidia.shadows.ShadowMapGenerator;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.volumetricLighting.LightType;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture3D;
import jet.opengl.postprocessing.texture.Texture3DDesc;
import jet.opengl.postprocessing.texture.TextureCube;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.BoundingBox;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

class VolumetricFog implements Disposeable {
    public static final int VISUAL_ACCUMULATION = 0;
    public static final int VISUAL_BLEND = 1;
    public static final int VISUAL_SCATTERING =2;

    public enum ScatteringTexture{
        DENSITY,
        GLOBAL_EMISSIVE,
        LOCAL_SHADOWED,
        LIGHT_SCATTERING,
        FINAL_SCATTERING
    }

    static final float WORLD_MAX = 2097152.0f;				/* Maximum size of the world */
    static final float HALF_WORLD_MAX = (WORLD_MAX * 0.5f);		/* Half the*/

    private MaterialSetupCS materialSetupCS;
//    private InjectShadowedLocalLightProgram injectProg;
    private HashMap<FPermutationDomain, InjectShadowedLocalLightProgram> injectProgs = new HashMap<>();
    private HashMap<IntegratedKey, VolumetricFogLightScatteringCS> lightScatteringCS = new HashMap<>();
    private List<UE4LightInfo> m_localShadowedLights = new ArrayList<>();

    private VolumetricFogFinalIntegrationCS finalIntegrationCS;
    private VolumetricFogShadingProgram shadingProgram;

    private Texture3D VBufferA;
    private Texture3D VBufferB;
    private TextureGL LightFunctionTexture;
    private Texture3D pTex3DLocalShadowedLightScattering;
    private Texture3D[] pTex3DLightScattering = new Texture3D[2];
    private Texture3D pTex3DFinalLightScattering;
    private int pinpog;

    private final Vector3i volumetricFogGridSize = new Vector3i();

    private Params params;
    private int VolumetricFogGridInjectionGroupSize = 4;
    private int VolumetricFogIntegrationGroupSize = 8;
    private boolean m_princeOnce;

    private final MaterialSetupParams materialParams = new MaterialSetupParams();
    private final InjectLocalLightParameters injectLocalLightParams = new InjectLocalLightParameters();
    private final FDeferredLightData lightData = new FDeferredLightData();
    private final LightScatteringParameters lightScatteringParams = new LightScatteringParameters();
    private final LocalLightData localLightData = new LocalLightData(1);

    private GLFuncProvider gl;
    private int m_FBO;
    private int m_GCircleRasterizeVertexBuffer;
    private int m_GCircleRasterizeIndexBuffer;
    private int m_lightDataBuffer;
    private int m_pLinearSampler;
    private int m_pComapreSampler;
    private int m_pPointSampler;
    private int m_pLinearSamplerBorder;

    static final int NumVertices = 8;
    final String prefix = "gpupro/VolumetricFog/shaders/";

    private final Matrix4f m_UnjitteredPrevWorldToClip = new Matrix4f();

    private TextureGL m_DirectionShadowMap;
    private TextureGL m_DirectionStaticShadowMap;

    private int m_forwardRenderBuffer;

    private int m_ViewWidth, m_ViewHeight;

    public void onResize(int width, int height){
        m_ViewWidth = width;
        m_ViewHeight = height;
    }

    private int frameNumber = 10;
    public void renderVolumetricFog(Params params){
        this.params = params;
        frameNumber++;

        final int curr = current();
        final int history = history();

        if(!m_princeOnce){
            // The first frame
            Matrix4f.mul(params.proj, params.view, m_UnjitteredPrevWorldToClip);
        }

//        final int current

        initlizeResources();

        final int viewWith = params.sceneColor.getWidth();
        final int viewHeight = params.sceneColor.getHeight();
        Vector3i volumetricFogGridSize = getVolumetricFogGridSize(viewWith, viewHeight);
        ReadableVector3f GridZParams = GetVolumetricFogGridZParams(params.NearClippingDistance, params.VolumetricFogDistance, volumetricFogGridSize.z);

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
                bUseTemporalReprojection && hasHistory;
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
            SAFE_RELEASE(pTex3DLightScattering[0]);
            SAFE_RELEASE(pTex3DLightScattering[1]);
            SAFE_RELEASE(pTex3DFinalLightScattering);

            Texture3DDesc desc = new Texture3DDesc(volumetricFogGridSize.x, volumetricFogGridSize.y, volumetricFogGridSize.z, 1, GLenum.GL_RGBA16F);
            VBufferA = TextureUtils.createTexture3D(desc, null);
            VBufferB = TextureUtils.createTexture3D(desc, null);
            pTex3DLightScattering[0] = TextureUtils.createTexture3D(desc, null);
            pTex3DLightScattering[1] = TextureUtils.createTexture3D(desc, null);
            pTex3DFinalLightScattering = TextureUtils.createTexture3D(desc, null);

            VBufferA.setName("VBufferA");
            VBufferB.setName("VBufferB");
            pTex3DLightScattering[0].setName("LightScattering0");
            pTex3DLightScattering[1].setName("LightScattering1");
        }

        IntegrationData.VBufferARenderTarget = VBufferA;
        IntegrationData.VBufferBRenderTarget = VBufferB;

        gl.glClearTexImage(VBufferA.getTexture(), 0, GLenum.GL_RGBA, GLenum.GL_HALF_FLOAT, null);
        gl.glClearTexImage(VBufferB.getTexture(), 0, GLenum.GL_RGBA, GLenum.GL_HALF_FLOAT, null);
        gl.glClearTexImage(pTex3DLightScattering[curr].getTexture(), 0, GLenum.GL_RGBA, GLenum.GL_HALF_FLOAT, null);
        gl.glClearTexImage(pTex3DFinalLightScattering.getTexture(), 0, GLenum.GL_RGBA, GLenum.GL_HALF_FLOAT, null);

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
            GLCheck.checkError();
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
        IntegrationData.LightScatteringRenderTarget =pTex3DLightScattering[curr];
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
            VolumetricFogLightScatteringCS program = getLightScatteringCS(bUseTemporalReprojection, bUseDistanceFieldSkyOcclusion);
            program.enable();
            LightScatteringParameters scatteringParams = buildLightScattering(IntegrationData);
            program.apply(scatteringParams);

            GLSLUtil.setFloat(program, "g_CameraFar", params.cameraFar);
            GLSLUtil.setFloat(program, "g_CameraNear", params.cameraNear);

            LocalLightData localLightData = buildLocalLightData();
            ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(localLightData.size);
            localLightData.store(buffer).flip();
            gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, m_forwardRenderBuffer);
            gl.glBufferSubData(GLenum.GL_UNIFORM_BUFFER,0, buffer);

            if(!m_princeOnce)
            {
                GLSLUtil.assertUniformBuffer(program, "_ForwardLightData",0, localLightData.size);
                if(m_DirectionShadowMap == null)System.out.println("m_DirectionShadowMap is null" );
            }

            gl.glBindTextureUnit(0, m_DirectionShadowMap != null ? m_DirectionShadowMap.getTexture() : 0);  // ShadowDepthTexture
            gl.glBindSampler(0, m_pLinearSamplerBorder);
            gl.glBindTextureUnit(3, m_DirectionShadowMap != null ? m_DirectionShadowMap.getTexture() : 0); // DirectionalLightShadowmapAtlas
            gl.glBindSampler(3, m_pPointSampler);
            gl.glBindTextureUnit(5, LightFunctionTexture != null ? LightFunctionTexture.getTexture() : 0); // LightFunctionTexture
            gl.glBindSampler(5, m_pLinearSampler);
            if(history >=0){
                TextureGL LightScatteringHistory = pTex3DLightScattering[history];
                gl.glBindTextureUnit(6,  LightScatteringHistory.getTexture()); // LightScatteringHistory
                gl.glBindSampler(6, m_pLinearSampler);
            }else{
                gl.glBindTextureUnit(6,  0); // LightScatteringHistory
            }

            gl.glBindImageTexture(0, LocalShadowedLightScattering!= null ? LocalShadowedLightScattering.getTexture():0, 0,true, 0, GLenum.GL_READ_ONLY, GLenum.GL_RGBA16F);
            gl.glBindImageTexture(1, IntegrationData.VBufferARenderTarget.getTexture(), 0,true, 0, GLenum.GL_READ_ONLY, GLenum.GL_RGBA16F);
            gl.glBindImageTexture(2, IntegrationData.VBufferBRenderTarget.getTexture(), 0,true, 0, GLenum.GL_READ_ONLY, GLenum.GL_RGBA16F);
            gl.glBindImageTexture(3, IntegrationData.LightScatteringRenderTarget.getTexture(), 0,true, 0, GLenum.GL_WRITE_ONLY, GLenum.GL_RGBA16F);

            final int numGroupX = Numeric.divideAndRoundUp(volumetricFogGridSize.x, VolumetricFogGridInjectionGroupSize);
            final int numGroupY = Numeric.divideAndRoundUp(volumetricFogGridSize.y, VolumetricFogGridInjectionGroupSize);
            final int numGroupZ = Numeric.divideAndRoundUp(volumetricFogGridSize.z, VolumetricFogGridInjectionGroupSize);

            gl.glDispatchCompute(numGroupX, numGroupY, numGroupZ);

            if(!m_princeOnce) program.printPrograminfo();

            gl.glBindTextureUnit(0,  0);
            gl.glBindTextureUnit(3,  0);
            gl.glBindTextureUnit(5,  0);
            gl.glBindTextureUnit(6,  0);
            gl.glBindSampler(0, 0);
            gl.glBindSampler(3, 0);
            gl.glBindSampler(5, 0);

            gl.glBindImageTexture(0, 0, 0,false, 0, GLenum.GL_READ_ONLY, GLenum.GL_RGBA16F);
            gl.glBindImageTexture(1, 0, 0,false, 0, GLenum.GL_READ_ONLY, GLenum.GL_RGBA16F);
            gl.glBindImageTexture(2, 0, 0,false, 0, GLenum.GL_READ_ONLY, GLenum.GL_RGBA16F);
            gl.glBindImageTexture(3, 0, 0,false, 0, GLenum.GL_WRITE_ONLY, GLenum.GL_RGBA16F);
        }

//        const FRDGTexture* IntegratedLightScattering = GraphBuilder.CreateTexture(VolumeDesc, TEXT("IntegratedLightScattering"));
//			const FRDGTextureUAV* IntegratedLightScatteringUAV = GraphBuilder.CreateUAV(FRDGTextureUAVDesc(IntegratedLightScattering));
        Texture3D IntegratedLightScattering = IntegrationData.LightScatteringRenderTarget;
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

            final int numGroupX = Numeric.divideAndRoundUp(volumetricFogGridSize.x, VolumetricFogIntegrationGroupSize);
            final int numGroupY = Numeric.divideAndRoundUp(volumetricFogGridSize.y, VolumetricFogIntegrationGroupSize);

            finalIntegrationCS.enable();
//            FloatBuffer zeros = CacheBuffer.wrap(0f,0f,0f,0f);
//            gl.glClearTexImage(IntegratedLightScatteringUAV.getTexture(), 0, GLenum.GL_RGBA, GLenum.GL_FLOAT, zeros);

            GLSLUtil.setMat4(finalIntegrationCS, "UnjitteredClipToTranslatedWorld", materialParams.UnjitteredClipToTranslatedWorld);
            GLSLUtil.setFloat3(finalIntegrationCS, "View_PreViewTranslation", materialParams.View_PreViewTranslation);
            GLSLUtil.setFloat3(finalIntegrationCS, "VolumetricFog_GridSize", materialParams.VolumetricFog_GridSize);
            GLSLUtil.setFloat3(finalIntegrationCS, "VolumetricFog_GridZParams", materialParams.VolumetricFog_GridZParams);
            GLSLUtil.setFloat3(finalIntegrationCS, "WorldCameraOrigin", injectLocalLightParams.WorldCameraOrigin);
            GLSLUtil.setMat4(finalIntegrationCS, "g_ViewProj", materialParams.g_ViewProj);
            GLSLUtil.setFloat(finalIntegrationCS, "g_CameraFar", materialParams.cameraFar);
            GLSLUtil.setFloat(finalIntegrationCS, "g_CameraNear", materialParams.cameraNear);

            gl.glBindTextureUnit(0, IntegratedLightScattering.getTexture());
            gl.glBindSampler(0, m_pLinearSampler);
            gl.glBindImageTexture(0, IntegratedLightScatteringUAV.getTexture(), 0, true, 0, GLenum.GL_WRITE_ONLY, IntegratedLightScatteringUAV.getFormat());

            if(IntegratedLightScattering == IntegratedLightScatteringUAV)
                throw new RuntimeException();

            gl.glDispatchCompute(numGroupX, numGroupY, 1);

            gl.glBindTextureUnit(0, 0);
            gl.glBindSampler(0, 0);
            gl.glBindImageTexture(0, 0, 0, true, 0, GLenum.GL_WRITE_ONLY, IntegratedLightScatteringUAV.getFormat());

            if(!m_princeOnce)finalIntegrationCS.printPrograminfo();
        }

        Texture3D[] visualTextures = {
            VBufferA,
            VBufferB,
            LocalShadowedLightScattering,
            pTex3DLightScattering[curr],
            IntegratedLightScatteringUAV
        };

        if(params.VisualMode == VISUAL_SCATTERING){
            shadingScene(visualTextures[params.VisualTexture.ordinal()]);
        }else{
            shadingScene(IntegratedLightScatteringUAV);
        }


        if(bUseTemporalReprojection){
            // for the next frame
            Matrix4f.mul(params.proj, params.view, m_UnjitteredPrevWorldToClip);
        }

        m_princeOnce = true;
        recordHistory();
    }

    private void shadingScene(Texture3D finalLightScattering){
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_BLEND);

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glViewport(0,0, m_ViewWidth, m_ViewHeight);
        gl.glClearColor(0,0,0,0);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);

        shadingProgram.enable();
        GLSLUtil.setFloat3(shadingProgram, "VolumetricFog_GridSize", materialParams.VolumetricFog_GridSize);
        GLSLUtil.setFloat3(shadingProgram, "VolumetricFog_GridZParams", materialParams.VolumetricFog_GridZParams);
        GLSLUtil.setFloat(shadingProgram, "g_CameraFar", params.cameraFar);
        GLSLUtil.setFloat(shadingProgram, "g_CameraNear", params.cameraNear);
        GLSLUtil.setInt(shadingProgram, "g_VisualMode", params.VisualMode);
        GLSLUtil.setInt(shadingProgram, "g_ScatteringSlice", params.VisualSlice);
        GLSLUtil.setInt(shadingProgram, "g_Tonemap", params.Tonemapping ? 1 : 0);

        gl.glBindTextureUnit(0, params.sceneDepth.getTexture());
        gl.glBindTextureUnit(1, params.sceneColor.getTexture());
        gl.glBindTextureUnit(2, finalLightScattering.getTexture());
        gl.glBindSampler(0, m_pLinearSampler);
        gl.glBindSampler(1, m_pLinearSampler);
        gl.glBindSampler(2, m_pLinearSampler);

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

        for(int i = 0; i < 3; i++){
            gl.glBindTextureUnit(i,0);
            gl.glBindSampler(i, 0);
        }

        if(!m_princeOnce) shadingProgram.printPrograminfo();
    }

    private boolean RenderLightFunctionForVolumetricFog(Vector3i volumetricFogGridSize, float volumetricFogDistance, Matrix4f lightFunctionWorldToShadow, TextureGL lightFunctionTexture) {
//        throw new UnsupportedOperationException();
        return true;
    }

    private Texture3D RenderLocalLightsForVolumetricFog(boolean bUseTemporalReprojection, FVolumetricFogIntegrationParameterData integrationData, Vector3i volumetricFogGridSize, ReadableVector3f gridZParams) {
        /*TArray<const FLightSceneInfo*, SceneRenderingAllocator> LightsToInject;  todo

        for (TSparseArray<FLightSceneInfoCompact>::TConstIterator LightIt(Scene->Lights); LightIt; ++LightIt)
        {
		const FLightSceneInfoCompact& LightSceneInfoCompact = *LightIt;
		const FLightSceneInfo* LightSceneInfo = LightSceneInfoCompact.LightSceneInfo;

            if (LightSceneInfo->ShouldRenderLightViewIndependent()
                    && LightSceneInfo->ShouldRenderLight(View)
                    && LightNeedsSeparateInjectionIntoVolumetricFog(LightSceneInfo, VisibleLightInfos[LightSceneInfo->Id])
                    && LightSceneInfo->Proxy->GetVolumetricScatteringIntensity() > 0)
            {
			const FSphere LightBounds = LightSceneInfo->Proxy->GetBoundingSphere();

                if ((View.ViewMatrices.GetViewOrigin() - LightBounds.Center).SizeSquared() < (FogInfo.VolumetricFogDistance + LightBounds.W) * (FogInfo.VolumetricFogDistance + LightBounds.W))
                {
                    LightsToInject.Add(LightSceneInfo);
                }
            }
        }*/

        if (params.lightInfos.size() > 0)
        {
//            OutLocalShadowedLightScattering = GraphBuilder.CreateTexture(VolumeDesc, TEXT("LocalShadowedLightScattering"));
            if(pTex3DLocalShadowedLightScattering == null || pTex3DLocalShadowedLightScattering.getWidth() != volumetricFogGridSize.x ||
                pTex3DLocalShadowedLightScattering.getHeight() != volumetricFogGridSize.y || pTex3DLocalShadowedLightScattering.getDepth() != volumetricFogGridSize.z){
                SAFE_RELEASE(pTex3DLocalShadowedLightScattering);

                Texture3DDesc desc = new Texture3DDesc(volumetricFogGridSize.x, volumetricFogGridSize.y, volumetricFogGridSize.z, 1, GLenum.GL_RGBA16F);
                pTex3DLocalShadowedLightScattering = TextureUtils.createTexture3D(desc, null);
            }

            // Create the Vertex Buffer and the index buffer
            if(m_GCircleRasterizeVertexBuffer == 0){
                m_GCircleRasterizeVertexBuffer = gl.glGenBuffer();
                m_GCircleRasterizeIndexBuffer = gl.glGenBuffer();

                gl.glBindVertexArray(0);
                gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_GCircleRasterizeVertexBuffer);

                final int NumTriangles = NumVertices - 2;
                final int Size = NumVertices * /*sizeof(FScreenVertex)*/ Vector4f.SIZE;
                /*FRHIResourceCreateInfo CreateInfo;
                void* Buffer = nullptr;
                VertexBufferRHI = RHICreateAndLockVertexBuffer(Size, BUF_Static, CreateInfo, Buffer);
                FScreenVertex* DestVertex = (FScreenVertex*)Buffer;*/
                FloatBuffer DestVertex = CacheBuffer.getCachedFloatBuffer(Size/4);

		        final int NumRings = NumVertices;
		        final float RadiansPerRingSegment = Numeric.PI / (float)NumRings;

                // Boost the effective radius so that the edges of the circle approximation lie on the circle, instead of the vertices
                final float RadiusScale = 1.0f / (float)Math.cos(RadiansPerRingSegment);

                for (int VertexIndex = 0; VertexIndex < NumVertices; VertexIndex++)
                {
                    float Angle = VertexIndex / (float)(NumVertices - 1) * 2 * Numeric.PI;
                    // WriteToBoundingSphereVS only uses UV
//                    DestVertex[VertexIndex].Position = FVector2D(0, 0);
//                    DestVertex[VertexIndex].UV = FVector2D(RadiusScale * FMath::Cos(Angle) * .5f + .5f, RadiusScale * FMath::Sin(Angle) * .5f + .5f);
                    float U = RadiusScale * (float)Math.cos(Angle) * .5f + .5f;
                    float V = RadiusScale * (float)Math.sin(Angle) * .5f + .5f;
                    DestVertex.put(0).put(0);   // position
                    DestVertex.put(U).put(V);
                }

                DestVertex.flip();
                gl.glBufferData(GLenum.GL_ARRAY_BUFFER, DestVertex, GLenum.GL_STATIC_DRAW);

                // Create the Index buffer
                gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_GCircleRasterizeIndexBuffer);

                ShortBuffer Indices = CacheBuffer.getCachedShortBuffer(NumTriangles * 3);
//                Indices.Empty(NumTriangles * 3);

                for (int TriangleIndex = 0; TriangleIndex < NumTriangles; TriangleIndex++)
                {
                    int LeadingVertexIndex = TriangleIndex + 2;
                    Indices.put((short)0);
                    Indices.put((short) (LeadingVertexIndex - 1));
                    Indices.put((short) LeadingVertexIndex);
                }
                Indices.flip();
                gl.glBufferData(GLenum.GL_ARRAY_BUFFER, Indices, GLenum.GL_STATIC_DRAW);
                gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);

                m_lightDataBuffer = gl.glGenBuffer();
                gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, m_lightDataBuffer);
                gl.glBufferData(GLenum.GL_UNIFORM_BUFFER, FDeferredLightData.SIZE, GLenum.GL_DYNAMIC_COPY);
                gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, 0);

                GLCheck.checkError();
            }

            Texture3D OutLocalShadowedLightScattering = pTex3DLocalShadowedLightScattering;
            gl.glClearTexImage(OutLocalShadowedLightScattering.getTexture(), 0, GLenum.GL_RGBA, GLenum.GL_HALF_FLOAT, null);

            /*FRenderTargetParameters* PassParameters = GraphBuilder.AllocParameters<FRenderTargetParameters>();
            PassParameters->RenderTargets[0] = FRenderTargetBinding(OutLocalShadowedLightScattering, ERenderTargetLoadAction::EClear, ERenderTargetStoreAction::ENoAction);

            GraphBuilder.AddPass(
                    RDG_EVENT_NAME("ShadowedLights"),
                    PassParameters,
                    ERenderGraphPassFlags::None,
                    [PassParameters, &View, this, LightsToInject, VolumetricFogGridSize, GridZParams, bUseTemporalReprojection, IntegrationData, FogInfo](FRHICommandListImmediate& RHICmdList)
                {
		    });*/

            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_FBO);
            gl.glFramebufferTexture(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0,OutLocalShadowedLightScattering.getTexture(), 0);
            gl.glViewport(0,0, OutLocalShadowedLightScattering.getWidth(), OutLocalShadowedLightScattering.getHeight());
            gl.glDrawBuffers(GLenum.GL_COLOR_ATTACHMENT0);

            // Apply rendering states.
            gl.glDisable(GLenum.GL_CULL_FACE);
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            gl.glEnable(GLenum.GL_BLEND);
            gl.glBlendFuncSeparate(GLenum.GL_ONE, GLenum.GL_ONE, GLenum.GL_ZERO, GLenum.GL_ONE);
            // Applying the Vertex inputs.
            gl.glBindVertexArray(0);
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_GCircleRasterizeVertexBuffer);
            gl.glEnableVertexAttribArray(0);
            gl.glVertexAttribPointer(0, 2, GLenum.GL_FLOAT, false, Vector4f.SIZE, 0);
            gl.glEnableVertexAttribArray(1);
            gl.glVertexAttribPointer(1, 2, GLenum.GL_FLOAT, false, Vector4f.SIZE, 8);
            gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, m_GCircleRasterizeIndexBuffer);

            for (int LightIndex = 0; LightIndex < params.lightInfos.size(); LightIndex++)
            {
//				const FLightSceneInfo* LightSceneInfo = LightsToInject[LightIndex];
                UE4LightInfo LightSceneInfo = params.lightInfos.get(LightIndex);
//                FProjectedShadowInfo* ProjectedShadowInfo = GetShadowForInjectionIntoVolumetricFog(LightSceneInfo->Proxy, VisibleLightInfos[LightSceneInfo->Id]);
                if(LightSceneInfo.shadowmap == null || LightSceneInfo.type == LightType.DIRECTIONAL)
                    continue;

				final boolean bInverseSquared = true; //LightSceneInfo->Proxy->IsInverseSquared();
                final boolean bDynamicallyShadowed = true; //ProjectedShadowInfo != NULL;
				final VaBoundingSphere LightBounds = LightSceneInfo.boundingSphere;
				final Vector3i VolumeZBounds = CalculateVolumetricFogBoundsForLight(LightBounds, params.view, volumetricFogGridSize, gridZParams);

                if (VolumeZBounds.x < VolumeZBounds.y)
                {
                    /*TInjectShadowedLocalLightPS::FPermutationDomain PermutationVector;
                    PermutationVector.Set< TInjectShadowedLocalLightPS::FDynamicallyShadowed >(bDynamicallyShadowed);
                    PermutationVector.Set< TInjectShadowedLocalLightPS::FInverseSquared >(bInverseSquared);
                    PermutationVector.Set< TInjectShadowedLocalLightPS::FTemporalReprojection >(bUseTemporalReprojection);

                    auto VertexShader = View.ShaderMap->GetShader< FWriteToBoundingSphereVS >();
                    TOptionalShaderMapRef<FWriteToSliceGS> GeometryShader(View.ShaderMap);
                    auto PixelShader = View.ShaderMap->GetShader< TInjectShadowedLocalLightPS >(PermutationVector);*/

                    InjectShadowedLocalLightProgram injectLocalLightProgram = getInjectLocalLightProg(bDynamicallyShadowed, bInverseSquared, bUseTemporalReprojection);
                    injectLocalLightProgram.enable();
                    InjectLocalLightParameters injectParams = buildInjectLocalLightParameters(LightIndex, integrationData,VolumeZBounds.x);
                    injectParams.VolumetricFog_GridSize.x = volumetricFogGridSize.x;
                    injectParams.VolumetricFog_GridSize.y = volumetricFogGridSize.y;
                    injectParams.VolumetricFog_GridSize.z = volumetricFogGridSize.z;
                    injectParams.VolumetricFog_GridZParams.set(gridZParams);

                    injectLocalLightProgram.applyUniforms(injectParams);

                    FDeferredLightData lightData = buildLightData(LightIndex);
                    ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(FDeferredLightData.SIZE);
                    lightData.store(buffer).flip();
                    gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, m_lightDataBuffer);
                    gl.glBufferSubData(GLenum.GL_UNIFORM_BUFFER, 0, buffer);

                    if(!m_princeOnce){
                        GLSLUtil.assertUniformBuffer(injectLocalLightProgram, "LightData22", 0, FDeferredLightData.SIZE);
                    }

                    TextureGL shadowMap = LightSceneInfo.shadowmap;
                    if(shadowMap instanceof Texture2D){
                        gl.glBindTextureUnit(0, LightSceneInfo.shadowmap.getTexture());
                        gl.glBindSampler(0, m_pLinearSampler);
                        gl.glBindTextureUnit(1, 0);  // no cube shadow
                    }else if(shadowMap instanceof TextureCube){
                        gl.glBindTextureUnit(0, 0); // no 2D shadow
                        gl.glBindTextureUnit(1, LightSceneInfo.shadowmap.getTexture());  // Shadow
                        gl.glBindSampler(1, m_pComapreSampler);
                        gl.glBindTextureUnit(3, LightSceneInfo.shadowmap.getTexture());
                        gl.glBindSampler(3, m_pLinearSampler);
                    }
                    gl.glBindTextureUnit(2, 0);  // todo non static texture.
                    /*FGraphicsPipelineStateInitializer GraphicsPSOInit;
                    RHICmdList.ApplyCachedRenderTargets(GraphicsPSOInit);
                    GraphicsPSOInit.RasterizerState = TStaticRasterizerState<FM_Solid, CM_None>::GetRHI();
                    GraphicsPSOInit.DepthStencilState = TStaticDepthStencilState<false, CF_Always>::GetRHI();
                    // Accumulate the contribution of multiple lights
                    GraphicsPSOInit.BlendState = TStaticBlendState<CW_RGBA, BO_Add, BF_One, BF_One, BO_Add, BF_Zero, BF_One>::GetRHI();*/

                    /*GraphicsPSOInit.BoundShaderState.VertexDeclarationRHI = GScreenVertexDeclaration.VertexDeclarationRHI;
                    GraphicsPSOInit.BoundShaderState.VertexShaderRHI = GETSAFERHISHADER_VERTEX(VertexShader);
                    GraphicsPSOInit.BoundShaderState.GeometryShaderRHI = GETSAFERHISHADER_GEOMETRY(*GeometryShader);
                    GraphicsPSOInit.BoundShaderState.PixelShaderRHI = GETSAFERHISHADER_PIXEL(PixelShader);
                    GraphicsPSOInit.PrimitiveType = PT_TriangleList;*/

                    /*SetGraphicsPipelineState(RHICmdList, GraphicsPSOInit);
                    PixelShader->SetParameters(RHICmdList, View, IntegrationData, LightSceneInfo, FogInfo, ProjectedShadowInfo, bDynamicallyShadowed);
                    VertexShader->SetParameters(RHICmdList, View, IntegrationData, LightBounds, VolumeZBounds.X);*/

                    /*if (GeometryShader.IsValid())
                    {
                        GeometryShader->SetParameters(RHICmdList, VolumeZBounds.X);
                    }*/

//                    RHICmdList.SetStreamSource(0, GCircleRasterizeVertexBuffer.VertexBufferRHI, 0);
					final int NumInstances = VolumeZBounds.y - VolumeZBounds.x;
                    final int NumTriangles = NumVertices - 2;
//                    RHICmdList.DrawIndexedPrimitive(GCircleRasterizeIndexBuffer.IndexBufferRHI, 0, 0, FCircleRasterizeVertexBuffer::NumVertices, 0, NumTriangles, NumInstances);
                    gl.glDrawElementsInstanced(GLenum.GL_TRIANGLES, 3*NumTriangles, GLenum.GL_UNSIGNED_SHORT, 0, NumInstances);

                    if(!m_princeOnce)
                        injectLocalLightProgram.printPrograminfo();

                    GLCheck.checkError();
                }
            }

            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
            gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
            gl.glDisableVertexAttribArray(0);
            gl.glDisableVertexAttribArray(1);
            gl.glDisable(GLenum.GL_BLEND);
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);

            for(int i =0 ;i < 4; i++){
                gl.glBindTextureUnit(i, 0);
                gl.glBindSampler(i, 0);
            }

            return OutLocalShadowedLightScattering;
        }

        return null;
    }

    private void VoxelizeFogVolumePrimitives(FVolumetricFogIntegrationParameterData integrationData, Vector3i volumetricFogGridSize, ReadableVector3f gridZParams, float volumetricFogDistance) {
//        throw new UnsupportedOperationException();
    }

    private void initlizeResources(){
        if(gl == null) {
            gl = GLFuncProviderFactory.getGLFuncProvider();
            m_FBO = gl.glGenFramebuffer();

            SamplerDesc linearDesc = new SamplerDesc();
            linearDesc.wrapR = linearDesc.wrapS = linearDesc.wrapT = GLenum.GL_CLAMP_TO_EDGE;
            linearDesc.borderColor = 0xFFFFFFFF;  // White
            m_pLinearSampler = SamplerUtils.createSampler(linearDesc);
            m_pComapreSampler = SamplerUtils.getDepthComparisonSampler();

            linearDesc.magFilter = linearDesc.minFilter = GLenum.GL_NEAREST;
            linearDesc.wrapR = linearDesc.wrapS = linearDesc.wrapT = GLenum.GL_CLAMP_TO_BORDER;
            m_pPointSampler = SamplerUtils.createSampler(linearDesc);

            linearDesc.magFilter = linearDesc.minFilter = GLenum.GL_LINEAR;
            m_pLinearSamplerBorder = SamplerUtils.createSampler(linearDesc);
        }

        if(materialSetupCS == null){
            materialSetupCS = new MaterialSetupCS(prefix, VolumetricFogGridInjectionGroupSize);
            materialSetupCS.setName("MaterialSetupCS");

            finalIntegrationCS = new VolumetricFogFinalIntegrationCS(prefix, VolumetricFogIntegrationGroupSize);
            finalIntegrationCS.setName("FinalIntegrationCS");

            shadingProgram = new VolumetricFogShadingProgram(prefix);
            shadingProgram.setName("VolumetricFogShading");

            m_forwardRenderBuffer = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, m_forwardRenderBuffer);
            gl.glBufferData(GLenum.GL_UNIFORM_BUFFER, localLightData.size(), GLenum.GL_DYNAMIC_DRAW);
            gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, 0);
        }
    }

    private VolumetricFogLightScatteringCS getLightScatteringCS(boolean bUseGlobalDistanceField, boolean bUseDistanceFieldSkyOcclusion){
        IntegratedKey key = new IntegratedKey(bUseGlobalDistanceField, bUseDistanceFieldSkyOcclusion);

        VolumetricFogLightScatteringCS program = lightScatteringCS.get(key);
        if(program == null){
            program = new VolumetricFogLightScatteringCS(prefix, VolumetricFogGridInjectionGroupSize, key);
            program.setName("VolumetricFogLightScatteringCS");
            lightScatteringCS.put(key, program);
        }

        return program;
    }

    private InjectShadowedLocalLightProgram getInjectLocalLightProg(boolean bDynamicallyShadowed, boolean bInverseSquared, boolean bUseTemporalReprojection){
        FPermutationDomain key = new FPermutationDomain(bDynamicallyShadowed, bInverseSquared, bUseTemporalReprojection);

        InjectShadowedLocalLightProgram program = injectProgs.get(key);
        if(program == null){
            program = new InjectShadowedLocalLightProgram(prefix, bDynamicallyShadowed, bInverseSquared, bUseTemporalReprojection);
            injectProgs.put(key, program);
        }

        return program;
    }

    private static int ComputeZSliceFromDepth(float SceneDepth, ReadableVector3f GridZParams){
        return (int) (Numeric.log2(SceneDepth*GridZParams.getX()+GridZParams.getY())*GridZParams.getZ());
    }

    private Vector3i CalculateVolumetricFogBoundsForLight(VaBoundingSphere LightBounds, Matrix4f View, Vector3i VolumetricFogGridSize, ReadableVector3f GridZParams) {
        Vector3i VolumeZBounds = new Vector3i();

//        FVector ViewSpaceLightBoundsOrigin = View.ViewMatrices.GetViewMatrix().TransformPosition(LightBounds.Center);
        Vector3f ViewSpaceLightBoundsOrigin = Matrix4f.transformVector(View, LightBounds.Center, null);
        ViewSpaceLightBoundsOrigin.z *= -1;

        int FurthestSliceIndexUnclamped = ComputeZSliceFromDepth(ViewSpaceLightBoundsOrigin.z + LightBounds.Radius, GridZParams);
        int ClosestSliceIndexUnclamped = ComputeZSliceFromDepth(ViewSpaceLightBoundsOrigin.z - LightBounds.Radius, GridZParams);

        VolumeZBounds.x = Numeric.clamp(ClosestSliceIndexUnclamped, 0, VolumetricFogGridSize.z - 1);
        VolumeZBounds.y = Numeric.clamp(FurthestSliceIndexUnclamped, 0, VolumetricFogGridSize.z - 1);

        return VolumeZBounds;
    }

    private boolean hasHistory;
    private int current() { return pinpog;}
    private int history() {
        if(!hasHistory)
            return -1;

        return 1-pinpog;
    }
    private void recordHistory(){
        hasHistory = true;
        pinpog = 1-pinpog;
    }

    private Vector3i getVolumetricFogGridSize(int width, int height){
//        const FIntPoint VolumetricFogGridSizeXY = FIntPoint::DivideAndRoundUp(ViewRectSize, GVolumetricFogGridPixelSize);
//        return FIntVector(VolumetricFogGridSizeXY.X, VolumetricFogGridSizeXY.Y, GVolumetricFogGridSizeZ);

        volumetricFogGridSize.x = Numeric.divideAndRoundUp(width, params.GVolumetricFogGridPixelSize);
        volumetricFogGridSize.y = Numeric.divideAndRoundUp(height, params.GVolumetricFogGridPixelSize);
        volumetricFogGridSize.z = params.GVolumetricFogGridSizeZ;
        return volumetricFogGridSize;
    }

    private Vector3f GetVolumetricFogGridZParams(float NearPlane, float FarPlane, int GridSizeZ) {
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

    Vector3f VolumetricFogTemporalRandom(int FrameNumber) {
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
        // TODO See detial in the FogRender.cpp
//        final float factor = 1.0f/1000;  //TODO In UE4 the scale been used.
        final float factor = 1;
        materialParams.ExponentialFogParameters.y = Numeric.clamp(params.fogHeightFalloffset, 0, 2) * factor;
        materialParams.ExponentialFogParameters3.x = Numeric.clamp(params.fogDensity, 0.001f, 0.05f) * factor;
        materialParams.ExponentialFogParameters3.y = Math.max(0, params.startDistance);

        materialParams.g_ViewProj.load(params.proj);
        Matrix4f.mul(params.proj, params.view, materialParams.UnjitteredClipToTranslatedWorld);
        materialParams.UnjitteredClipToTranslatedWorld.invert();

        materialParams.GlobalAlbedo.set(params.GlobalAlbedo);
        materialParams.GlobalEmissive.set(params.GlobalEmissive);
        materialParams.GlobalExtinctionScale = params.GlobalExtinctionScale;
        materialParams.cameraFar = params.cameraFar;
        materialParams.cameraNear = params.cameraNear;

        return materialParams;
    }

    private InjectLocalLightParameters buildInjectLocalLightParameters(int lightIndex, FVolumetricFogIntegrationParameterData IntegrationData, int minZ){
        final UE4LightInfo light = this.params.lightInfos.get(lightIndex);
        final InjectLocalLightParameters params = injectLocalLightParams;
        float MaxSubjectZ = light.range;
        float MinSubjectZ = 0;
        params.DepthBiasParameters.set(0.0001f, 1/(MaxSubjectZ-MinSubjectZ));
        params.FrameJitterOffsets = IntegrationData.FrameJitterOffsetValues;
        params.HistoryMissSuperSampleCount = this.params.GVolumetricFogHistoryMissSupersampleCount;
        params.HistoryWeight = this.params.GVolumetricFogHistoryWeight;
        params.InvShadowmapResolution = 1.0f/light.shadowmap.getWidth();
        params.InverseSquaredLightDistanceBiasScale = this.params.GInverseSquaredLightDistanceBiasScale;
        params.MinZ = minZ;
        params.PhaseG = this.params.GVolumetricFogScatteringDistribution;
        params.ShadowInjectParams.set(0,0,1,0);  // TODO
        if(light.type == LightType.POINT){
            for(int i = 0; i < 6;i++){
                params.ShadowViewProjectionMatrices[i] = Matrix4f.mul(light.proj, light.cubeViews[i],params.ShadowViewProjectionMatrices[i]);
            }
        }else{
            Matrix4f.mul(light.proj, light.view, params.WorldToShadowMatrix);
        }
        params.ShadowmapMinMax.set(0,0,1,1);
        params.StaticShadowBufferSize.set(0,0); // no static shadow
        params.UnjitteredClipToTranslatedWorld.load(materialParams.UnjitteredClipToTranslatedWorld);
        params.UnjitteredPrevWorldToClip = m_UnjitteredPrevWorldToClip;
        params.ViewToVolumeClip.load(this.params.proj);
        Matrix4f.decompseRigidMatrix(this.params.view, params.WorldCameraOrigin, null, null, params.ViewForward);
        params.ViewForward.scale(-1);

        Matrix4f.transformVector(this.params.view, light.boundingSphere.Center, params.ViewSpaceBoundingSphere);
//        params.ViewSpaceBoundingSphere.z *= -1;
        params.ViewSpaceBoundingSphere.w = light.boundingSphere.Radius;
        params.g_ViewProj.load(materialParams.g_ViewProj);
        params.bStaticallyShadowed = false;
        params.cameraFar = this.params.cameraFar;
        params.cameraNear = this.params.cameraNear;

        return params;
    }

    private FDeferredLightData buildLightData(int lightIndex){
        final UE4LightInfo source = this.params.lightInfos.get(lightIndex);

        lightData.Position.set(source.position);
        lightData.InvRadius = 1.f/source.range;
        lightData.Color.set(source.color);
        lightData.FalloffExponent = 1;
        lightData.Direction.set(source.direction);
        lightData.VolumetricScatteringIntensity = source.intensity;
        lightData.Tangent.set(1,0,0);
        lightData.SoftSourceRadius = 0.01f;

        if(source.type == LightType.SPOT){
            final float ClampedInnerConeAngle = Numeric.clamp(source.spotAngle* 0.8f,0.0f,(float)Math.toRadians(89.0f));
            final float ClampedOuterConeAngle = Numeric.clamp(source.spotAngle,ClampedInnerConeAngle + 0.001f,(float)Math.toRadians(89.0f)+0.01f);
            float CosOuterCone = (float) Math.cos(ClampedOuterConeAngle);
            float CosInnerCone = (float) Math.cos(ClampedInnerConeAngle);
            float InvCosConeDifference = 1.0f / (CosInnerCone - CosOuterCone);
            lightData.SpotAngles.set(CosOuterCone, InvCosConeDifference);
            lightData.bSpotLight = true;
        }else{
            lightData.SpotAngles.set(-2, -1);
            lightData.bSpotLight = false;
        }

        lightData.SourceRadius = 0.1f;
        lightData.SourceLength = 0;
        lightData.SpecularScale = 1;
        lightData.ContactShadowLength = 0.1f;
        lightData.DistanceFadeMAD.set(1,0);
        lightData.ShadowMapChannelMask.set(0,0,0,0);
        lightData.ContactShadowLengthInWS = false;
        lightData.bInverseSquared = (source.type != LightType.DIRECTIONAL);
        lightData.bRadialLight = true;
        lightData.bRectLight = false;

        return lightData;
    }

    private LightScatteringParameters buildLightScattering(FVolumetricFogIntegrationParameterData IntegrationData){
        final LightScatteringParameters result = lightScatteringParams;
        result.DirectionalLightFunctionWorldToShadow.setIdentity();
        result.FrameJitterOffsets = IntegrationData.FrameJitterOffsetValues;
        result.HeightFogDirectionalLightInscatteringColor.set(0,0,0);
        result.HistoryMissSuperSampleCount = params.GVolumetricFogHistoryMissSupersampleCount;
        result.HistoryWeight = params.GVolumetricFogHistoryWeight;
        result.InverseSquaredLightDistanceBiasScale = params.GInverseSquaredLightDistanceBiasScale;
        result.PhaseG = params.GVolumetricFogScatteringDistribution;
        result.UnjitteredClipToTranslatedWorld = materialParams.UnjitteredClipToTranslatedWorld;
        result.UnjitteredPrevWorldToClip = m_UnjitteredPrevWorldToClip;
        result.UseDirectionalLightShadowing = true;
        result.UseHeightFogColors = false;
        result.View_PreViewTranslation = materialParams.View_PreViewTranslation;
        result.VolumetricFog_GridSize = materialParams.VolumetricFog_GridSize;
        result.VolumetricFog_GridZParams = materialParams.VolumetricFog_GridZParams;
        result.WorldCameraOrigin = injectLocalLightParams.WorldCameraOrigin;
        Matrix4f.decompseRigidMatrix(params.view, result.WorldCameraOrigin, null, null);
        result.g_ViewProj = materialParams.g_ViewProj;

        return result;
    }

    private LocalLightData buildLocalLightData(){
        final LocalLightData result = localLightData;
        result.DirectionalLightWorldToStaticShadow.setIdentity();
        result.DirectionalLightStaticShadowBufferSize.set(0,0,0,0);

        m_DirectionShadowMap = null;

        int NumLocalLightsFinal = 0;
        boolean bDirectionLightHandled =false;
        for(int lightIndex = 0; lightIndex < params.lightInfos.size(); lightIndex++){
            UE4LightInfo light = params.lightInfos.get(lightIndex);
            if(light.type == LightType.DIRECTIONAL){
                if(bDirectionLightHandled)
                    throw new UnsupportedOperationException("Can't include the two direction light.");

                result.HasDirectionalLight = true;
                result.DirectionalLightColor.set(light.color);
                result.DirectionalLightVolumetricScatteringIntensity = light.intensity;  //LightProxy->GetVolumetricScatteringIntensity();
                result.DirectionalLightDirection.set(light.direction);
                result.DirectionalLightShadowMapChannelMask = 0;
                result.DirectionalLightDistanceFadeMAD.set(1, 0); // TODO
                Matrix4f.mul(light.proj, light.view, result.DirectionalLightWorldToShadowMatrix[0]);
                result.CascadeEndDepths[0] = params.cameraFar;
                result.DirectionalLightShadowmapMinMax[0].set(0,0,1,1);

//                result.DirectionalLightShadowmapAtlas = ShadowInfo->RenderTargets.DepthTarget->GetRenderTargetItem().ShaderResourceTexture.GetReference();
                result.DirectionalLightDepthBias = 0.0001f;
                float shadowmapSizeX = light.shadowmap.getWidth();
                float shadowmapSizeY = light.shadowmap.getHeight();
                result.DirectionalLightShadowmapAtlasBufferSize.set(shadowmapSizeX, shadowmapSizeY, 1.0f / shadowmapSizeX, 1.0f / shadowmapSizeY);

                m_DirectionShadowMap = light.shadowmap;
                m_DirectionStaticShadowMap = null;  // TODO we havn't the static shadow map so far.

                bDirectionLightHandled = true;
            }else{ // Point or Spot light.
                result.HasDirectionalLight = false;

                if(light.shadowmap!=null){
                    // The light that cast shadow map has handle in the previouse pass.
                    continue;
                }

                Vector4f LightPositionAndInvRadius = result.ForwardLocalLightBuffer[NumLocalLightsFinal * 5 + 0];
                Vector4f LightColorAndFalloffExponent = result.ForwardLocalLightBuffer[NumLocalLightsFinal * 5 + 1];
                Vector4f LightDirectionAndShadowMapChannelMask = result.ForwardLocalLightBuffer[NumLocalLightsFinal * 5 + 3];
                Vector4f SpotAnglesAndSourceRadiusPacked = result.ForwardLocalLightBuffer[NumLocalLightsFinal * 5 + 2];
                Vector4f LightTangentAndSoftSourceRadius = result.ForwardLocalLightBuffer[NumLocalLightsFinal * 5 + 4];

                LightPositionAndInvRadius.set(light.position, 1.0f/light.range);
                LightColorAndFalloffExponent.set(light.color, 1);
                LightDirectionAndShadowMapChannelMask.set(light.direction, 0/**((float*)&ShadowMapChannelMaskPacked)*/);
                if(light.type == LightType.POINT){
                    SpotAnglesAndSourceRadiusPacked.set(-2, -1, 1, 0);
                }else{   //Spot light
                    final float ClampedInnerConeAngle = Numeric.clamp(light.spotAngle* 0.8f,0.0f,(float)Math.toRadians(89.0f));
                    final float ClampedOuterConeAngle = Numeric.clamp(light.spotAngle,ClampedInnerConeAngle + 0.001f,(float)Math.toRadians(89.0f)+0.01f);
                    float CosOuterCone = (float) Math.cos(ClampedOuterConeAngle);
                    float CosInnerCone = (float) Math.cos(ClampedInnerConeAngle);
                    float InvCosConeDifference = 1.0f / (CosInnerCone - CosOuterCone);
                    SpotAnglesAndSourceRadiusPacked.set(CosOuterCone, InvCosConeDifference, 1, 0);
                }
                LightTangentAndSoftSourceRadius.set(0.0f, 0.0f, 1.0f, 0.1f);

                /*float VolumetricScatteringIntensity = LightProxy->GetVolumetricScatteringIntensity();
                if (LightNeedsSeparateInjectionIntoVolumetricFog(LightSceneInfo, VisibleLightInfos[LightSceneInfo->Id]))
                {
                    // Disable this lights forward shading volumetric scattering contribution
                    VolumetricScatteringIntensity = 0;
                }*/

                // Pack both values into a single float to keep float4 alignment
                final short SourceLength16f = Numeric.convertFloatToHFloat(light.SourceLength); /*FFloat16(LightParameters.SourceLength)*/;
                final short VolumetricScatteringIntensity16f = Numeric.convertFloatToHFloat(light.VolumetricScatteringIntensity); // FFloat16(VolumetricScatteringIntensity);
                final int PackedWInt = Numeric.encode(SourceLength16f, VolumetricScatteringIntensity16f); //   ((uint32)SourceLength16f.Encoded) | ((uint32)VolumetricScatteringIntensity16f.Encoded << 16);
//                SpotAnglesAndSourceRadiusPacked.W = *(float*)&PackedWInt;
                SpotAnglesAndSourceRadiusPacked.w = Float.intBitsToFloat(PackedWInt);

                NumLocalLightsFinal++;
            }
        }

//                const FIntPoint LightGridSizeXY = FIntPoint::DivideAndRoundUp(View.ViewRect.Size(), GLightGridPixelSize);
        final int LightGridSizeX = Numeric.divideAndRoundUp(m_ViewWidth, params.GLightGridPixelSize);
        final int LightGridSizeY = Numeric.divideAndRoundUp(m_ViewHeight, params.GLightGridPixelSize);
        result.NumLocalLights = NumLocalLightsFinal;
//        result.NumReflectionCaptures = View.NumBoxReflectionCaptures + View.NumSphereReflectionCaptures;
//        result.NumGridCells = LightGridSizeXY.X * LightGridSizeXY.Y * GLightGridSizeZ;
        result.CulledGridSize.set(LightGridSizeX, LightGridSizeY, params.GLightGridSizeZ);
//        result.MaxCulledLightsPerCell = GMaxCulledLightsPerCell;
        result.LightGridPixelSizeShift = (int)Math.floor(Numeric.log2(params.GLightGridPixelSize));

        /*const FSphere BoundingSphere = LightProxy->GetBoundingSphere();  todo
        const float Distance = View.ViewMatrices.GetViewMatrix().TransformPosition(BoundingSphere.Center).Z + BoundingSphere.W;
        FurthestLight = FMath::Max(FurthestLight, Distance);*/
        float FurthestLight =  params.cameraFar;
        // Clamp far plane to something reasonable
        float FarPlane = FurthestLight; //Math.min(Math.max(FurthestLight, View.FurthestReflectionCaptureDistance), HALF_WORLD_MAX / 5.0f);
//        FVector ZParams = GetLightGridZParams(View.NearClippingDistance, FarPlane + 10.f);
//        result.LightGridZParams = ZParams;
        GetLightGridZParams(params.cameraNear, FarPlane, result.LightGridZParams);

        return result;
    }

    public boolean isPrintProgram()  {return !m_princeOnce;}

    private void GetLightGridZParams(float NearPlane, float FarPlane, Vector3f result)
    {
        // S = distribution scale
        // B, O are solved for given the z distances of the first+last slice, and the # of slices.
        //
        // slice = log2(z*B + O) * S

        // Don't spend lots of resolution right in front of the near plane
        double NearOffset = .095 * 100;
        // Space out the slices so they aren't all clustered at the near plane
        float S = 4.05f;

        double N = NearPlane + NearOffset;
        double F = FarPlane;

        float O = (float) ((F - N * Numeric.exp2((params.GLightGridSizeZ - 1) / S)) / (F - N));
        float B = (float) ((1 - O) / N);

        result.set(B, O, S);
    }

    private static final class FPermutationDomain{
        boolean bDynamicallyShadowed;
        boolean bInverseSquared;
        boolean bUseTemporalReprojection;

        public FPermutationDomain(boolean bDynamicallyShadowed, boolean bInverseSquared, boolean bUseTemporalReprojection) {
            this.bDynamicallyShadowed = bDynamicallyShadowed;
            this.bInverseSquared = bInverseSquared;
            this.bUseTemporalReprojection = bUseTemporalReprojection;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FPermutationDomain that = (FPermutationDomain) o;

            if (bDynamicallyShadowed != that.bDynamicallyShadowed) return false;
            if (bInverseSquared != that.bInverseSquared) return false;
            return bUseTemporalReprojection == that.bUseTemporalReprojection;
        }

        @Override
        public int hashCode() {
            int result = (bDynamicallyShadowed ? 1 : 0);
            result = 31 * result + (bInverseSquared ? 1 : 0);
            result = 31 * result + (bUseTemporalReprojection ? 1 : 0);
            return result;
        }
    }

    public static final class Params extends UE4LightCollections {
        public Texture2D sceneColor;
        public Texture2D sceneDepth;
        public TextureGL shadowMap;
        public float cameraNear;
        public float cameraFar;

        /** Height density factor, controls how the density increases as height decreases.
         * Smaller values make the visible transition larger.
         * Ranged [0.001, 2.0] in UE4 */
        public float fogHeightFalloffset = 0.02f;
        /** Global density factor for this fog. Ranged [0.001, 0.05] */
        public float fogDensity = 0.02f;
        /** Height offset, relative to the actor position Z. */
        public float startDistance = 0;

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

        /** Scales the amount added to the inverse squared falloff denominator.  This effectively removes the spike from inverse squared falloff that causes extreme aliasing. */
        public float GInverseSquaredLightDistanceBiasScale = 1.f;

        /** How much the history value should be weighted each frame.  This is a tradeoff between visible jittering and responsiveness.*/
        public float GVolumetricFogHistoryWeight = 0.9f;

        public float GVolumetricFogScatteringDistribution = 0.2f;
        /** Size of a cell in the light grid, in pixels. */
        public int GLightGridPixelSize = 64;

        /** Number of Z slices in the light grid. */
        public int GLightGridSizeZ = 32;

        public int VisualMode = VISUAL_BLEND;

        public boolean Tonemapping = false;

        public  int VisualSlice = 0;

        public ScatteringTexture VisualTexture = ScatteringTexture.FINAL_SCATTERING;

        public final Vector3f FogInscatteringColor = new Vector3f(0.0f, 0.427f,1f);


    }
}
