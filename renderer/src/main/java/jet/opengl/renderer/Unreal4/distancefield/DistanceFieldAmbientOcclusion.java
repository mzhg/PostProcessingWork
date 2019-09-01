package jet.opengl.renderer.Unreal4.distancefield;

import org.lwjgl.util.vector.Vector2i;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.renderer.Unreal4.scenes.FScene;
import jet.opengl.renderer.Unreal4.FViewInfo;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.RenderTexturePool;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.Numeric;

public class DistanceFieldAmbientOcclusion {
    public static final int GAODownsampleFactor = 2;

    static int GDistanceFieldAOTileSizeX = 16;
    static int GDistanceFieldAOTileSizeY = 16;

    public static final class Params{
        /** Whether to apply DFAO as indirect shadowing even to static indirect sources (lightmaps + stationary skylight + reflection captures) */
        public boolean GDistanceFieldAOApplyToStaticIndirect = false;
        /** Determines whether object distance fields are used to compute ambient occlusion.Only global distance field will be used when this option is disabled.*/
        public boolean GAOObjectDistanceField = true;

        /**
         * Defines the distance field AO method which allows to adjust for quality or performance.<br>
         * 0:off, 1:medium, 2:high (default)
         */
        public int GDistanceFieldAOQuality = 2;

        /** Whether to use the compute shader version of the distance field normal computation. */
        public boolean GAOComputeShaderNormalCalculation = false;

        public int GDistanceFieldAOTileSizeX;
    }

    private Params mParams;
    private boolean bListMemoryNextFrame;
    private boolean bListMeshDistanceFieldsMemoryNextFrame;

    public static final String FComputeDistanceFieldNormalCS = GlobalDistanceField.SHADER_PATH + "ComputeDistanceFieldNormalCS.comp";

    private GLSLProgram mFComputeDistanceFieldNormalCS;

    private boolean UseAOObjectDistanceField()
    {
        return mParams.GAOObjectDistanceField && mParams.GDistanceFieldAOQuality >= 2;
    }

    Vector2i GetBufferSizeForAO(FViewInfo View)
    {
//        return FIntPoint::DivideAndRoundDown(FSceneRenderTargets::Get_FrameConstantsOnly().GetBufferSizeXY(), GAODownsampleFactor);
        return new Vector2i(Numeric.divideAndRoundUp(View.ViewRect.width, GAODownsampleFactor), Numeric.divideAndRoundUp(View.ViewRect.height, GAODownsampleFactor));
    }

    public void RenderDFAOAsIndirectShadowing(FViewInfo View, FScene Scene,
//            FRHICommandListImmediate& RHICmdList,
	        /*const TRefCountPtr<IPooledRenderTarget>&*/TextureGL VelocityTexture,
            /*TRefCountPtr<IPooledRenderTarget>&*/TextureGL DynamicBentNormalAO,
                                                    boolean GAOGlobalDistanceField,
                                                    float GAOGlobalDFStartDistance)
    {
        if (mParams.GDistanceFieldAOApplyToStaticIndirect && ShouldRenderDistanceFieldAO())
        {
            // Use the skylight's max distance if there is one, to be consistent with DFAO shadowing on the skylight
		    final float OcclusionMaxDistance = 1_000_000; // TODO Scene.SkyLight && !Scene->SkyLight->bWantsStaticShadowing ? Scene->SkyLight->OcclusionMaxDistance : Scene->DefaultMaxDistanceFieldOcclusionDistance;
            RenderDistanceFieldLighting(/*RHICmdList,*/View, Scene, new FDistanceFieldAOParameters(OcclusionMaxDistance, 0, GAOGlobalDistanceField, GAOGlobalDFStartDistance), VelocityTexture, DynamicBentNormalAO, true, false);
        }
    }



    public static final FDistanceFieldVolumeTextureAtlas GDistanceFieldVolumeTextureAtlas = new FDistanceFieldVolumeTextureAtlas();

    private boolean RenderDistanceFieldLighting(//FRHICommandListImmediate& RHICmdList,
                                                FViewInfo View,FScene Scene,
                                             FDistanceFieldAOParameters Parameters,
	                                         TextureGL VelocityTexture,
                                             TextureGL OutDynamicBentNormalAO,
                                             boolean bModulateToSceneColor,
                                             boolean bVisualizeAmbientOcclusion){
//        check(RHICmdList.IsOutsideRenderPass());

//        SCOPED_DRAW_EVENT(RHICmdList, RenderDistanceFieldLighting);

        //@todo - support multiple views
//	    const FViewInfo& View = Views[0];
//        FSceneRenderTargets& SceneContext = FSceneRenderTargets::Get(RHICmdList);

        if (/*SupportsDistanceFieldAO(View.GetFeatureLevel(), View.GetShaderPlatform())
                && Views.Num() == 1
                // ViewState is used to cache tile intersection resources which have to be sized based on the view
                && View.State
                &&*/ View.IsPerspectiveProjection())
        {
//            QUICK_SCOPE_CYCLE_COUNTER(STAT_RenderDistanceFieldLighting);

            if (GDistanceFieldVolumeTextureAtlas.VolumeTextureRHI!=null && Scene.DistanceFieldSceneData.NumObjectsInBuffer != 0)
            {
//                check(!Scene->DistanceFieldSceneData.HasPendingOperations());

//                SCOPED_DRAW_EVENT(RHICmdList, DistanceFieldLighting);

                GenerateBestSpacedVectors();

                if (bListMemoryNextFrame)
                {
                    bListMemoryNextFrame = false;
//                    ListDistanceFieldLightingMemory(View, *this);
                }

                if (bListMeshDistanceFieldsMemoryNextFrame)
                {
                    bListMeshDistanceFieldsMemoryNextFrame = false;
                    GDistanceFieldVolumeTextureAtlas.ListMeshDistanceFields();
                }

                if (UseAOObjectDistanceField())
                {
//                    CullObjectsToView(RHICmdList, Scene, View, Parameters, GAOCulledObjectBuffers);  TODO need another processing.
                }

                TextureGL /*TRefCountPtr<IPooledRenderTarget>*/ DistanceFieldNormal;

                {
				    final Vector2i BufferSize = GetBufferSizeForAO(View);
                    /*FPooledRenderTargetDesc Desc(FPooledRenderTargetDesc::Create2DDesc(BufferSize, PF_FloatRGBA, FClearValueBinding::Transparent, TexCreate_None, TexCreate_RenderTargetable | TexCreate_UAV, false));
                    Desc.Flags |= GFastVRamConfig.DistanceFieldNormal;
                    GRenderTargetPool.FindFreeElement(RHICmdList, Desc, DistanceFieldNormal, TEXT("DistanceFieldNormal"));*/
                    DistanceFieldNormal = RenderTexturePool.getInstance().findFreeElement(BufferSize.x, BufferSize.y, GLenum.GL_RGBA16F);
                    DistanceFieldNormal.setName("DistanceFieldNormal");
                }

                /*ComputeDistanceFieldNormal(RHICmdList, Views, DistanceFieldNormal->GetRenderTargetItem(), Parameters);

                // Intersect objects with screen tiles, build lists
                if (UseAOObjectDistanceField())
                {
                    BuildTileObjectLists(RHICmdList, Scene, Views, DistanceFieldNormal->GetRenderTargetItem(), Parameters);
                }

                GVisualizeTexture.SetCheckPoint(RHICmdList, DistanceFieldNormal);

                TRefCountPtr<IPooledRenderTarget> BentNormalOutput;

                RenderDistanceFieldAOScreenGrid(
                        RHICmdList,
                        View,
                        Parameters,
                        VelocityTexture,
                        DistanceFieldNormal,
                        BentNormalOutput);

                if (IsTransientResourceBufferAliasingEnabled() && UseAOObjectDistanceField())
                {
                    GAOCulledObjectBuffers.Buffers.DiscardTransientResource();

                    FTileIntersectionResources* TileIntersectionResources = ((FSceneViewState*)View.State)->AOTileIntersectionResources;
                    TileIntersectionResources->DiscardTransientResource();
                }

                RenderCapsuleShadowsForMovableSkylight(RHICmdList, BentNormalOutput);

                GVisualizeTexture.SetCheckPoint(RHICmdList, BentNormalOutput);

                if (bVisualizeAmbientOcclusion)
                {
                    SceneContext.BeginRenderingSceneColor(RHICmdList, ESimpleRenderTargetMode::EExistingColorAndDepth, FExclusiveDepthStencil::DepthRead_StencilRead);
                }
                else
                {
                    FRHIRenderPassInfo RPInfo(	SceneContext.GetSceneColorSurface(),
                        ERenderTargetActions::Load_Store,
                        SceneContext.GetSceneDepthSurface(),
                        EDepthStencilTargetActions::LoadDepthStencil_StoreStencilNotDepth,
                        FExclusiveDepthStencil::DepthRead_StencilWrite);
                    RHICmdList.BeginRenderPass(RPInfo, TEXT("DistanceFieldAO"));
                }

                // Upsample to full resolution, write to output in case of debug AO visualization or scene color modulation (standard upsampling is done later together with sky lighting and reflection environment)
                if (bModulateToSceneColor || bVisualizeAmbientOcclusion)
                {
                    UpsampleBentNormalAO(RHICmdList, Views, BentNormalOutput, bModulateToSceneColor && !bVisualizeAmbientOcclusion);
                }

                OutDynamicBentNormalAO = BentNormalOutput;

                if (bVisualizeAmbientOcclusion)
                {
                    SceneContext.FinishRenderingSceneColor(RHICmdList);
                }
                else
                {
                    RHICmdList.EndRenderPass();
                    RHICmdList.CopyToResolveTarget(OutDynamicBentNormalAO->GetRenderTargetItem().TargetableTexture, OutDynamicBentNormalAO->GetRenderTargetItem().ShaderResourceTexture, FResolveParams());
                }*/

                return true;
            }
        }

        return false;
    }

    private boolean ShouldRenderDistanceFieldAO()
    {
        /*return ViewFamily.EngineShowFlags.DistanceFieldAO
                && !ViewFamily.EngineShowFlags.VisualizeDistanceFieldAO
                && !ViewFamily.EngineShowFlags.VisualizeDistanceFieldGI
                && !ViewFamily.EngineShowFlags.VisualizeMeshDistanceFields
                && !ViewFamily.EngineShowFlags.VisualizeGlobalDistanceField;*/
        return true;
    }

    /*void ListDistanceFieldLightingMemory(FViewInfo View, FSceneRenderer& SceneRenderer)
    {
#if !NO_LOGGING
	const FScene* Scene = (const FScene*)View.Family->Scene;
        UE_LOG(LogRenderer, Log, TEXT("Shared GPU memory (excluding render targets)"));

        if (Scene->DistanceFieldSceneData.NumObjectsInBuffer > 0)
        {
            UE_LOG(LogRenderer, Log, TEXT("   Scene Object data %.3fMb"), Scene->DistanceFieldSceneData.ObjectBuffers->GetSizeBytes() / 1024.0f / 1024.0f);
        }

        UE_LOG(LogRenderer, Log, TEXT("   %s"), *GDistanceFieldVolumeTextureAtlas.GetSizeString());
        extern FString GetObjectBufferMemoryString();
        UE_LOG(LogRenderer, Log, TEXT("   %s"), *GetObjectBufferMemoryString());
        UE_LOG(LogRenderer, Log, TEXT(""));
        UE_LOG(LogRenderer, Log, TEXT("Distance Field AO"));

        UE_LOG(LogRenderer, Log, TEXT("   Temporary cache %.3fMb"), GTemporaryIrradianceCacheResources.GetSizeBytes() / 1024.0f / 1024.0f);
        UE_LOG(LogRenderer, Log, TEXT("   Culled objects %.3fMb"), GAOCulledObjectBuffers.Buffers.GetSizeBytes() / 1024.0f / 1024.0f);

        FTileIntersectionResources* TileIntersectionResources = ((FSceneViewState*)View.State)->AOTileIntersectionResources;

        if (TileIntersectionResources)
        {
            UE_LOG(LogRenderer, Log, TEXT("   Tile Culled objects %.3fMb"), TileIntersectionResources->GetSizeBytes() / 1024.0f / 1024.0f);
        }

        FAOScreenGridResources* ScreenGridResources = ((FSceneViewState*)View.State)->AOScreenGridResources;

        if (ScreenGridResources)
        {
            UE_LOG(LogRenderer, Log, TEXT("   Screen grid temporaries %.3fMb"), ScreenGridResources->GetSizeBytesForAO() / 1024.0f / 1024.0f);
        }

        UE_LOG(LogRenderer, Log, TEXT(""));
        UE_LOG(LogRenderer, Log, TEXT("Ray Traced Distance Field Shadows"));

        for (TSparseArray<FLightSceneInfoCompact>::TConstIterator LightIt(Scene->Lights); LightIt; ++LightIt)
        {
		const FLightSceneInfoCompact& LightSceneInfoCompact = *LightIt;
            FLightSceneInfo* LightSceneInfo = LightSceneInfoCompact.LightSceneInfo;

            FVisibleLightInfo& VisibleLightInfo = SceneRenderer.VisibleLightInfos[LightSceneInfo->Id];

            for (int32 ShadowIndex = 0; ShadowIndex < VisibleLightInfo.ShadowsToProject.Num(); ShadowIndex++)
            {
                FProjectedShadowInfo* ProjectedShadowInfo = VisibleLightInfo.ShadowsToProject[ShadowIndex];

                if (ProjectedShadowInfo->bRayTracedDistanceField && LightSceneInfo->TileIntersectionResources)
                {
                    UE_LOG(LogRenderer, Log, TEXT("   Light Tile Culled objects %.3fMb"), LightSceneInfo->TileIntersectionResources->GetSizeBytes() / 1024.0f / 1024.0f);
                }
            }
        }

        extern void ListGlobalDistanceFieldMemory();
        ListGlobalDistanceFieldMemory();

        UE_LOG(LogRenderer, Log, TEXT(""));
        UE_LOG(LogRenderer, Log, TEXT("Distance Field GI"));

        if (Scene->DistanceFieldSceneData.SurfelBuffers)
        {
            UE_LOG(LogRenderer, Log, TEXT("   Scene surfel data %.3fMb"), Scene->DistanceFieldSceneData.SurfelBuffers->GetSizeBytes() / 1024.0f / 1024.0f);
        }

        if (Scene->DistanceFieldSceneData.InstancedSurfelBuffers)
        {
            UE_LOG(LogRenderer, Log, TEXT("   Instanced scene surfel data %.3fMb"), Scene->DistanceFieldSceneData.InstancedSurfelBuffers->GetSizeBytes() / 1024.0f / 1024.0f);
        }

        if (ScreenGridResources)
        {
            UE_LOG(LogRenderer, Log, TEXT("   Screen grid temporaries %.3fMb"), ScreenGridResources->GetSizeBytesForGI() / 1024.0f / 1024.0f);
        }

        extern void ListDistanceFieldGIMemory(const FViewInfo& View);
        ListDistanceFieldGIMemory(View);
#endif // !NO_LOGGING
    }*/

    void ComputeDistanceFieldNormal(/*FRHICommandListImmediate& RHICmdList,*/ FViewInfo View, Texture2D DistanceFieldNormal, FDistanceFieldAOParameters Parameters)
    {
        if (mParams.GAOComputeShaderNormalCalculation)
        {
            // #todo-renderpasses remove once everything is converted to renderpasses
//            UnbindRenderTargets(RHICmdList);

//            for (int ViewIndex = 0; ViewIndex < Views.Num(); ViewIndex++)
            {
//			    const FViewInfo& View = Views[ViewIndex];

                int GroupSizeX = Numeric.divideAndRoundUp(View.ViewRect.width / GAODownsampleFactor, GDistanceFieldAOTileSizeX);
                int GroupSizeY = Numeric.divideAndRoundUp(View.ViewRect.height / GAODownsampleFactor, GDistanceFieldAOTileSizeY);

                {
//                    SCOPED_DRAW_EVENT(RHICmdList, ComputeNormalCS);
                    /*TShaderMapRef<FComputeDistanceFieldNormalCS> ComputeShader(View.ShaderMap);
                    RHICmdList.SetComputeShader(ComputeShader->GetComputeShader());
                    ComputeShader->SetParameters(RHICmdList, View, DistanceFieldNormal, Parameters);
                    DispatchComputeShader(RHICmdList, *ComputeShader, GroupSizeX, GroupSizeY, 1);
                    ComputeShader->UnsetParameters(RHICmdList, DistanceFieldNormal);*/

                    if(mFComputeDistanceFieldNormalCS == null){
                        Macro[] macros = {
                            new Macro("DOWNSAMPLE_FACTOR", GAODownsampleFactor),
                            new Macro("THREADGROUP_SIZEX", GDistanceFieldAOTileSizeX),
                            new Macro("THREADGROUP_SIZEY", GDistanceFieldAOTileSizeY),
                        };
                        mFComputeDistanceFieldNormalCS = GLSLProgram.createProgram(FComputeDistanceFieldNormalCS, macros);
                    }

                    // TODO Shader resources.
                    mFComputeDistanceFieldNormalCS.enable();
                    GLFuncProviderFactory.getGLFuncProvider().glDispatchCompute(GroupSizeX, GroupSizeY,1);
                }
            }
        }
        else
        {
            /*FRHIRenderPassInfo RPInfo(DistanceFieldNormal.TargetableTexture, ERenderTargetActions::Clear_Store);
            TransitionRenderPassTargets(RHICmdList, RPInfo);
            RHICmdList.BeginRenderPass(RPInfo, TEXT("ComputeDistanceFieldNormal"));*/
            GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();


            /*{
                FGraphicsPipelineStateInitializer GraphicsPSOInit;
                RHICmdList.ApplyCachedRenderTargets(GraphicsPSOInit);

                GraphicsPSOInit.RasterizerState = TStaticRasterizerState<FM_Solid, CM_None>::GetRHI();
                GraphicsPSOInit.DepthStencilState = TStaticDepthStencilState<false, CF_Always>::GetRHI();
                GraphicsPSOInit.BlendState = TStaticBlendState<>::GetRHI();
                GraphicsPSOInit.PrimitiveType = PT_TriangleList;

                for (int32 ViewIndex = 0; ViewIndex < Views.Num(); ViewIndex++)
                {
				const FViewInfo& View = Views[ViewIndex];

                    SCOPED_DRAW_EVENT(RHICmdList, ComputeNormal);

                    RHICmdList.SetViewport(0, 0, 0.0f, View.ViewRect.Width() / GAODownsampleFactor, View.ViewRect.Height() / GAODownsampleFactor, 1.0f);

                    TShaderMapRef<FPostProcessVS> VertexShader(View.ShaderMap);
                    TShaderMapRef<FComputeDistanceFieldNormalPS> PixelShader(View.ShaderMap);

                    GraphicsPSOInit.BoundShaderState.VertexDeclarationRHI = GFilterVertexDeclaration.VertexDeclarationRHI;
                    GraphicsPSOInit.BoundShaderState.VertexShaderRHI = GETSAFERHISHADER_VERTEX(*VertexShader);
                    GraphicsPSOInit.BoundShaderState.PixelShaderRHI = GETSAFERHISHADER_PIXEL(*PixelShader);

                    SetGraphicsPipelineState(RHICmdList, GraphicsPSOInit);

                    PixelShader->SetParameters(RHICmdList, View, Parameters);

                    DrawRectangle(
                            RHICmdList,
                            0, 0,
                            View.ViewRect.Width() / GAODownsampleFactor, View.ViewRect.Height() / GAODownsampleFactor,
                            0, 0,
                            View.ViewRect.Width(), View.ViewRect.Height(),
                            FIntPoint(View.ViewRect.Width() / GAODownsampleFactor, View.ViewRect.Height() / GAODownsampleFactor),
                            FSceneRenderTargets::Get(RHICmdList).GetBufferSizeXY(),
					*VertexShader);
                }
            }
            RHICmdList.EndRenderPass();

            RHICmdList.TransitionResource(EResourceTransitionAccess::EReadable, DistanceFieldNormal.TargetableTexture);*/
        }
    }

    static boolean bGenerated = false;
    private final Vector3f[] OriginalSpacedVectors9 = new Vector3f[SpacedVectors9.length];
    void GenerateBestSpacedVectors()
    {
        boolean bApplyRepulsion = false;

        if (bApplyRepulsion && !bGenerated)
        {
            bGenerated = true;

//            FVector OriginalSpacedVectors9[ARRAY_COUNT(SpacedVectors9)];

            for (int i = 0; i < OriginalSpacedVectors9.length; i++)
            {
                if(OriginalSpacedVectors9[i] == null){
                    OriginalSpacedVectors9[i] = new Vector3f(SpacedVectors9[i]);
                }else{
                    OriginalSpacedVectors9[i].set(SpacedVectors9[i]);
                }
            }

            float CosHalfAngle = 1 - 1.0f / OriginalSpacedVectors9.length;
            // Used to prevent self-shadowing on a plane
            float AngleBias = .03f;
            float MinAngle = (float) (Math.acos(CosHalfAngle) + AngleBias);
            float MinZ = (float) Math.sin(MinAngle);

            // Relaxation iterations by repulsion
            for (int Iteration = 0; Iteration < 10000; Iteration++)
            {
                for (int i = 0; i < OriginalSpacedVectors9.length; i++)
                {
//                    FVector Force(0.0f, 0.0f, 0.0f);
                    float ForceX = 0;
                    float ForceY = 0;
                    float ForceZ = 0;

                    for (int j = 0; j < OriginalSpacedVectors9.length; j++)
                    {
                        if (i != j)
                        {
//                            FVector Distance = OriginalSpacedVectors9[i] - OriginalSpacedVectors9[j];
                            float DistanceX = OriginalSpacedVectors9[i].x - OriginalSpacedVectors9[j].x;
                            float DistanceY = OriginalSpacedVectors9[i].y - OriginalSpacedVectors9[j].y;
                            float DistanceZ = OriginalSpacedVectors9[i].z - OriginalSpacedVectors9[j].z;
//                            float Dot = OriginalSpacedVectors9[i] | OriginalSpacedVectors9[j];
                            float Dot = Vector3f.dot(OriginalSpacedVectors9[i] , OriginalSpacedVectors9[j]);

                            float DistanceLength = Vector3f.lengthSquare(DistanceX, DistanceY, DistanceZ);
                            if(DistanceLength > 0.00001){
                                DistanceLength = (float) Math.sqrt(DistanceLength);
                                DistanceX /= DistanceLength;
                                DistanceY /= DistanceLength;
                                DistanceZ /= DistanceLength;
                            }else{
                                DistanceX = DistanceY = DistanceZ = 0;
                            }

                            if (Dot > 0)
                            {
                                // Repulsion force
//                                Force += .001f * Distance.GetSafeNormal() * Dot * Dot * Dot * Dot;
                                ForceX += .001f * DistanceX * Dot * Dot * Dot * Dot;
                                ForceY += .001f * DistanceY * Dot * Dot * Dot * Dot;
                                ForceZ += .001f * DistanceZ * Dot * Dot * Dot * Dot;
                            }
                        }
                    }

                    /*FVector NewPosition = OriginalSpacedVectors9[i] + Force;
                    NewPosition.Z = FMath::Max(NewPosition.Z, MinZ);
                    NewPosition = NewPosition.GetSafeNormal();
                    OriginalSpacedVectors9[i] = NewPosition;*/

                    OriginalSpacedVectors9[i].x += ForceX;
                    OriginalSpacedVectors9[i].y += ForceY;
                    OriginalSpacedVectors9[i].z += ForceZ;
                    OriginalSpacedVectors9[i].z = Math.max(OriginalSpacedVectors9[i].z, MinZ);
                    OriginalSpacedVectors9[i].normalise();
                }
            }

            for (int i = 0; i < OriginalSpacedVectors9.length; i++)
            {
//                UE_LOG(LogDistanceField, Log, TEXT("FVector(%f, %f, %f),"), OriginalSpacedVectors9[i].X, OriginalSpacedVectors9[i].Y, OriginalSpacedVectors9[i].Z);
            }

            int temp = 0;
        }

        boolean bBruteForceGenerateConeDirections = false;

        /*if (bBruteForceGenerateConeDirections)
        {
            FVector BestSpacedVectors9[9];
            float BestCoverage = 0;
            // Each cone covers an area of ConeSolidAngle = HemisphereSolidAngle / NumCones
            // HemisphereSolidAngle = 2 * PI
            // ConeSolidAngle = 2 * PI * (1 - cos(ConeHalfAngle))
            // cos(ConeHalfAngle) = 1 - 1 / NumCones
            float CosHalfAngle = 1 - 1.0f / (float)ARRAY_COUNT(BestSpacedVectors9);
            // Prevent self-intersection in sample set
            float MinAngle = FMath::Acos(CosHalfAngle);
            float MinZ = FMath::Sin(MinAngle);
            FRandomStream RandomStream(123567);

            // Super slow random brute force search
            for (int i = 0; i < 1000000; i++)
            {
                FVector CandidateSpacedVectors[ARRAY_COUNT(BestSpacedVectors9)];

                for (int j = 0; j < ARRAY_COUNT(CandidateSpacedVectors); j++)
                {
                    FVector NewSample;

                    // Reject invalid directions until we get a valid one
                    do
                    {
                        NewSample = GetUnitVector2(RandomStream);
                    }
                    while (NewSample.Z <= MinZ);

                    CandidateSpacedVectors[j] = NewSample;
                }

                float Coverage = 0;
                int NumSamples = 10000;

                // Determine total cone coverage with monte carlo estimation
                for (int sample = 0; sample < NumSamples; sample++)
                {
                    FVector NewSample;

                    do
                    {
                        NewSample = GetUnitVector2(RandomStream);
                    }
                    while (NewSample.Z <= 0);

                    bool bIntersects = false;

                    for (int j = 0; j < ARRAY_COUNT(CandidateSpacedVectors); j++)
                    {
                        if (FVector::DotProduct(CandidateSpacedVectors[j], NewSample) > CosHalfAngle)
                        {
                            bIntersects = true;
                            break;
                        }
                    }

                    Coverage += bIntersects ? 1 / (float)NumSamples : 0;
                }

                if (Coverage > BestCoverage)
                {
                    BestCoverage = Coverage;

                    for (int j = 0; j < ARRAY_COUNT(CandidateSpacedVectors); j++)
                    {
                        BestSpacedVectors9[j] = CandidateSpacedVectors[j];
                    }
                }
            }

            int32 temp = 0;
        }*/
    }

    // Sample set restricted to not self-intersect a surface based on cone angle .475882232
    // Coverage of hemisphere = 0.755312979
    final static Vector3f SpacedVectors9[] =
    {
            new Vector3f(-0.573257625f, 0.625250816f, 0.529563010f),
            new Vector3f(0.253354192f, -0.840093017f, 0.479640961f),
            new Vector3f(-0.421664953f, -0.718063235f, 0.553700149f),
            new Vector3f(0.249163717f, 0.796005428f, 0.551627457f),
            new Vector3f(0.375082791f, 0.295851320f, 0.878512800f),
            new Vector3f(-0.217619032f, 0.00193520682f, 0.976031899f),
            new Vector3f(-0.852834642f, 0.0111727007f, 0.522061586f),
            new Vector3f(0.745701790f, 0.239393353f, 0.621787369f),
            new Vector3f(-0.151036426f, -0.465937436f, 0.871831656f)
    };

    // Generated from SpacedVectors9 by applying repulsion forces until convergence
    final static Vector3f RelaxedSpacedVectors9[] =
    {
            new Vector3f(-0.467612f, 0.739424f, 0.484347f),
            new Vector3f(0.517459f, -0.705440f, 0.484346f),
            new Vector3f(-0.419848f, -0.767551f, 0.484347f),
            new Vector3f(0.343077f, 0.804802f, 0.484347f),
            new Vector3f(0.364239f, 0.244290f, 0.898695f),
            new Vector3f(-0.381547f, 0.185815f, 0.905481f),
            new Vector3f(-0.870176f, -0.090559f, 0.484347f),
            new Vector3f(0.874448f, 0.027390f, 0.484346f),
            new Vector3f(0.032967f, -0.435625f, 0.899524f)
    };
}
