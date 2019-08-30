package jet.opengl.renderer.Unreal4;

import org.lwjgl.util.vector.Vector2i;

import java.util.ArrayList;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.renderer.Unreal4.api.ERHIFeatureLevel;
import jet.opengl.renderer.Unreal4.api.EShaderPlatform;
import jet.opengl.renderer.Unreal4.api.RHIDefinitions;

/**
 * Used as the scope for scene rendering functions.
 * It is initialized in the game thread by FSceneViewFamily::BeginRender, and then passed to the rendering thread.
 * The rendering thread calls Render(), and deletes the scene renderer when it returns.
 */
public abstract class FSceneRenderer implements Disposeable {
    final static EShaderPlatform[] GShaderPlatformForFeatureLevel = {
            EShaderPlatform.SP_OPENGL_SM5,
            EShaderPlatform.SP_OPENGL_SM5,
            EShaderPlatform.SP_OPENGL_SM5,
            EShaderPlatform.SP_OPENGL_SM5,
    };

    /** The scene being rendered. */
    FScene Scene;

    /** The view family being rendered.  This references the Views array. */
    FSceneViewFamily ViewFamily;

    /** The views being rendered. */
    final ArrayList<FViewInfo> Views = new ArrayList<>();

    final FMeshElementCollector MeshCollector = new FMeshElementCollector();

    final FMeshElementCollector RayTracingCollector = new FMeshElementCollector();

    /** Information about the visible lights. */
    final ArrayList<FVisibleLightInfo> VisibleLightInfos = new ArrayList<>();

    /** Array of dispatched parallel shadow depth passes. */
    final ArrayList<FParallelMeshDrawCommandPass> DispatchedShadowDepthPasses = new ArrayList<>();

    final FSortedShadowMaps SortedShadowsForShadowDepthPass = new FSortedShadowMaps();

    /** If a freeze request has been made */
    boolean bHasRequestedToggleFreeze;

    /** True if precomputed visibility was used when rendering the scene. */
    boolean bUsedPrecomputedVisibility;

    /** Lights added if wholescenepointlight shadow would have been rendered (ignoring r.SupportPointLightWholeSceneShadows). Used for warning about unsupported features. */
    final ArrayList<String> UsedWholeScenePointLightNames = new ArrayList<>();

    /** Feature level being rendered */
    int FeatureLevel = ERHIFeatureLevel.SM5;
//    EShaderPlatform ShaderPlatform;

    /**
     * The width in pixels of the stereo view family being rendered. This may be different than FamilySizeX if
     * we're using adaptive resolution stereo rendering. In that case, FamilySizeX represents the maximum size of
     * the family to ensure the backing render targets don't change between frames as the view size varies.
     */
    int InstancedStereoWidth;

    /* Only used if we are going to delay the deletion of the scene renderer until later. */
//    FMemMark* RootMark;

    FSceneRenderer(FSceneViewFamily InViewFamily/*,FHitProxyConsumer* HitProxyConsumer*/){

    }

    @Override
    public void dispose() {
        // To prevent keeping persistent references to single frame buffers, clear any such reference at this point.
        ClearPrimitiveSingleFramePrecomputedLightingBuffers();

        if(Scene != null)
        {
            // Destruct the projected shadow infos.
            for(TSparseArray<FLightSceneInfoCompact>::TConstIterator LightIt(Scene->Lights);LightIt;++LightIt)
            {
                if( VisibleLightInfos.IsValidIndex(LightIt.GetIndex()) )
                {
                    FVisibleLightInfo& VisibleLightInfo = VisibleLightInfos[LightIt.GetIndex()];
                    for(int32 ShadowIndex = 0;ShadowIndex < VisibleLightInfo.MemStackProjectedShadows.Num();ShadowIndex++)
                    {
                        // FProjectedShadowInfo's in MemStackProjectedShadows were allocated on the rendering thread mem stack,
                        // Their memory will be freed when the stack is freed with no destructor call, so invoke the destructor explicitly
                        VisibleLightInfo.MemStackProjectedShadows[ShadowIndex]->~FProjectedShadowInfo();
                    }
                }
            }
        }

        // Manually release references to TRefCountPtrs that are allocated on the mem stack, which doesn't call dtors
        SortedShadowsForShadowDepthPass.Release();
    }

    //    virtual ~FSceneRenderer();

    // FSceneRenderer interface

    abstract void Render(/*FRHICommandListImmediate& RHICmdList*/);
    void RenderHitProxies(/*FRHICommandListImmediate& RHICmdList*/) {}

    /** Creates a scene renderer based on the current feature level. */
    static FSceneRenderer CreateSceneRenderer(FSceneViewFamily InViewFamily/*, FHitProxyConsumer* HitProxyConsumer*/){

    }

    /** Setups FViewInfo::ViewRect according to ViewFamilly's ScreenPercentageInterface. */
    void PrepareViewRectsForRendering(){

    }

    /** Setups each FViewInfo::GPUMask. */
    void ComputeViewGPUMasks(/*FRHIGPUMask RenderTargetGPUMask*/){ }

    /** Update the rendertarget with each view results.*/
    void DoCrossGPUTransfers(/*FRHICommandListImmediate& RHICmdList, FRHIGPUMask RenderTargetGPUMask*/){}

    boolean DoOcclusionQueries(int InFeatureLevel) {
        return !RHIDefinitions.IsMobilePlatform(GShaderPlatformForFeatureLevel[InFeatureLevel])
                /*&& CVarAllowOcclusionQueries.GetValueOnRenderThread() != 0*/;
    }
    /** Issues occlusion queries. */
    void BeginOcclusionTests(/*FRHICommandListImmediate& RHICmdList,*/ boolean bRenderQueries){

    }

    // fences to make sure the rhi thread has digested the occlusion query renders before we attempt to read them back async
//    static FGraphEventRef OcclusionSubmittedFence[FOcclusionQueryHelpers::MaxBufferedOcclusionFrames];
    /** Fences occlusion queries. */
    void FenceOcclusionTests(/*FRHICommandListImmediate& RHICmdList*/){

    }
    /** Waits for the occlusion fence. */
    void WaitOcclusionTests(/*FRHICommandListImmediate& RHICmdList*/){

    }

    boolean ShouldDumpMeshDrawCommandInstancingStats() { return bDumpMeshDrawCommandInstancingStats; }

    /** bound shader state for occlusion test prims */
//    static FGlobalBoundShaderState OcclusionTestBoundShaderState;

    /**
     * Whether or not to composite editor objects onto the scene as a post processing step
     *
     * @param View The view to test against
     *
     * @return true if compositing is needed
     */
    static boolean ShouldCompositeEditorPrimitives(FViewInfo View);

    /** the last thing we do with a scene renderer, lots of cleanup related to the threading **/
    static void WaitForTasksClearSnapshotsAndDeleteSceneRenderer(/*FRHICommandListImmediate& RHICmdList,*/ FSceneRenderer SceneRenderer, boolean bWaitForTasks /*= true*/);
    static void DelayWaitForTasksClearSnapshotsAndDeleteSceneRenderer(/*FRHICommandListImmediate& RHICmdList,*/ FSceneRenderer SceneRenderer);

    /** Apply the ResolutionFraction on ViewSize, taking into account renderer's requirements. */
    static Vector2i ApplyResolutionFraction(
		 FSceneViewFamily ViewFamily, Vector2i UnscaledViewSize, float ResolutionFraction){
        Vector2i ViewSize = new Vector2i();

        // CeilToInt so tha view size is at least 1x1 if ResolutionFraction == FSceneViewScreenPercentageConfig::kMinResolutionFraction.
        ViewSize.x = (int) Math.ceil(UnscaledViewSize.x * ResolutionFraction);
        ViewSize.y = (int) Math.ceil(UnscaledViewSize.y * ResolutionFraction);

        // Mosaic needs the viewport height to be a multiple of 2.
        if (ViewFamily.GetFeatureLevel() <= ERHIFeatureLevel.ES3_1 && IsMobileHDRMosaic())
        {
            ViewSize.y = ViewSize.y + (1 & ViewSize.y);
        }

//        check(ViewSize.GetMin() > 0);

        return ViewSize;
    }

    /** Quantize the ViewRect.Min according to various renderer's downscale requirements. */
    static Vector2i QuantizeViewRectMin(Vector2i ViewRectMin){
        Vector2i Out = new Vector2i();
//        QuantizeSceneBufferSize(ViewRectMin, Out);
        return Out;
    }

    /** Get the desired buffer size from the view family's ResolutionFraction upperbound.
     * Can be called on game thread or render thread.
     */
    static Vector2i GetDesiredInternalBufferSize(FSceneViewFamily ViewFamily);

    /** Exposes renderer's privilege to fork view family's screen percentage interface. */
    static ISceneViewFamilyScreenPercentage* ForkScreenPercentageInterface(
		const ISceneViewFamilyScreenPercentage* ScreenPercentageInterface, FSceneViewFamily& ForkedViewFamily)
    {
        return ScreenPercentageInterface->Fork_GameThread(ForkedViewFamily);
    }

    static int GetRefractionQuality(FSceneViewFamily ViewFamily);

    /** Size of the family. */
    protected final Vector2i FamilySize = new Vector2i();

    protected boolean bDumpMeshDrawCommandInstancingStats;

    // Shared functionality between all scene renderers

    protected void InitDynamicShadows(/*FRHICommandListImmediate& RHICmdList,*/ FGlobalDynamicIndexBuffer DynamicIndexBuffer, FGlobalDynamicVertexBuffer DynamicVertexBuffer, FGlobalDynamicReadBuffer DynamicReadBuffer);

    protected void SetupMeshPass(FViewInfo View, FExclusiveDepthStencil::Type BasePassDepthStencilAccess, FViewCommands ViewCommands);

    protected boolean RenderShadowProjections(/*FRHICommandListImmediate& RHICmdList,*/ FLightSceneInfo LightSceneInfo, Texture2D ScreenShadowMaskTexture, boolean bProjectingForForwardShading, boolean bMobileModulatedProjections);

    /** Finds a matching cached preshadow, if one exists. */
    protected FProjectedShadowInfo GetCachedPreshadow(
            FLightPrimitiveInteraction InParentInteraction,
            FProjectedShadowInitializer Initializer,
            FBoxSphereBounds Bounds,
        int InResolutionX);

    /** Creates a per object projected shadow for the given interaction. */
    protected void CreatePerObjectProjectedShadow(
//            FRHICommandListImmediate& RHICmdList,
            FLightPrimitiveInteraction* Interaction,
            bool bCreateTranslucentObjectShadow,
            bool bCreateInsetObjectShadow,
		const TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& ViewDependentWholeSceneShadows,
            TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& OutPreShadows);

    /** Creates shadows for the given interaction. */
    protected void SetupInteractionShadows(
            FRHICommandListImmediate& RHICmdList,
            FLightPrimitiveInteraction* Interaction,
            FVisibleLightInfo& VisibleLightInfo,
            bool bStaticSceneOnly,
		const TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& ViewDependentWholeSceneShadows,
            TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& PreShadows);

    /** Generates FProjectedShadowInfos for all wholesceneshadows on the given light.*/
    protected void AddViewDependentWholeSceneShadowsForView(
            TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& ShadowInfos,
            TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& ShadowInfosThatNeedCulling,
            FVisibleLightInfo& VisibleLightInfo,
            FLightSceneInfo& LightSceneInfo);

    protected void AllocateShadowDepthTargets(FRHICommandListImmediate& RHICmdList);

    protected void AllocatePerObjectShadowDepthTargets(FRHICommandListImmediate& RHICmdList, TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& Shadows);

    protected void AllocateCachedSpotlightShadowDepthTargets(FRHICommandListImmediate& RHICmdList, TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& CachedShadows);

    protected void AllocateCSMDepthTargets(FRHICommandListImmediate& RHICmdList, const TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& WholeSceneDirectionalShadows);

    protected void AllocateRSMDepthTargets(FRHICommandListImmediate& RHICmdList, const TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& RSMShadows);

    protected void AllocateOnePassPointLightDepthTargets(FRHICommandListImmediate& RHICmdList, const TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& WholeScenePointShadows);

    protected void AllocateTranslucentShadowDepthTargets(FRHICommandListImmediate& RHICmdList, TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& TranslucentShadows);

    /**
     * Used by RenderLights to figure out if projected shadows need to be rendered to the attenuation buffer.
     * Or to render a given shadowdepth map for forward rendering.
     *
     * @param LightSceneInfo Represents the current light
     * @return true if anything needs to be rendered
     */
    protected boolean CheckForProjectedShadows(FLightSceneInfo LightSceneInfo) const;

    /** Gathers the list of primitives used to draw various shadow types */
    protected void GatherShadowPrimitives(
		const TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& PreShadows,
		const TArray<FProjectedShadowInfo*, SceneRenderingAllocator>& ViewDependentWholeSceneShadows,
        bool bReflectionCaptureScene);

    protected void RenderShadowDepthMaps(FRHICommandListImmediate& RHICmdList);
    protected void RenderShadowDepthMapAtlases(FRHICommandListImmediate& RHICmdList);

    /**
     * Creates a projected shadow for all primitives affected by a light.
     * @param LightSceneInfo - The light to create a shadow for.
     */
    protected void CreateWholeSceneProjectedShadow(FLightSceneInfo* LightSceneInfo, uint32& NumPointShadowCachesUpdatedThisFrame, uint32& NumSpotShadowCachesUpdatedThisFrame);

    /** Updates the preshadow cache, allocating new preshadows that can fit and evicting old ones. */
    protected void UpdatePreshadowCache(FSceneRenderTargets& SceneContext);

    /** Gets a readable light name for use with a draw event. */
    protected static void GetLightNameForDrawEvent(const FLightSceneProxy* LightProxy, FString& LightNameWithLevel);

    /** Gathers simple lights from visible primtives in the passed in views. */
    protected static void GatherSimpleLights(const FSceneViewFamily& ViewFamily, const TArray<FViewInfo>& Views, FSimpleLightArray& SimpleLights);

    /** Splits the gathered simple lights into arrays based on which view they should be rendered in */
    protected static void SplitSimpleLightsByView(const FSceneViewFamily& ViewFamily, const TArray<FViewInfo>& Views, const FSimpleLightArray& SimpleLights, FSimpleLightArray* SimpleLightsByView);

    /** Calculates projected shadow visibility. */
    protected void InitProjectedShadowVisibility(FRHICommandListImmediate& RHICmdList);

    /** Gathers dynamic mesh elements for all shadows. */
    protected void GatherShadowDynamicMeshElements(FGlobalDynamicIndexBuffer& DynamicIndexBuffer, FGlobalDynamicVertexBuffer& DynamicVertexBuffer, FGlobalDynamicReadBuffer& DynamicReadBuffer);

    /** Performs once per frame setup prior to visibility determination. */
    protected void PreVisibilityFrameSetup(FRHICommandListImmediate& RHICmdList);

    /** Computes which primitives are visible and relevant for each view. */
    protected void ComputeViewVisibility(FRHICommandListImmediate& RHICmdList, FExclusiveDepthStencil::Type BasePassDepthStencilAccess, FViewVisibleCommandsPerView& ViewCommandsPerView,
                               FGlobalDynamicIndexBuffer& DynamicIndexBuffer, FGlobalDynamicVertexBuffer& DynamicVertexBuffer, FGlobalDynamicReadBuffer& DynamicReadBuffer);

    /** Performs once per frame setup after to visibility determination. */
    protected void PostVisibilityFrameSetup(FILCUpdatePrimTaskData& OutILCTaskData);

    protected void GatherDynamicMeshElements(
            TArray<FViewInfo>& InViews,
		const FScene* InScene,
		const FSceneViewFamily& InViewFamily,
            FGlobalDynamicIndexBuffer& DynamicIndexBuffer,
            FGlobalDynamicVertexBuffer& DynamicVertexBuffer,
            FGlobalDynamicReadBuffer& DynamicReadBuffer,
		const FPrimitiveViewMasks& HasDynamicMeshElementsMasks,
		const FPrimitiveViewMasks& HasDynamicEditorMeshElementsMasks,
		const FPrimitiveViewMasks& HasViewCustomDataMasks,
            FMeshElementCollector& Collector);

    /** Initialized the fog constants for each view. */
    protected void InitFogConstants();

    /** Returns whether there are translucent primitives to be rendered. */
    protected boolean ShouldRenderTranslucency(ETranslucencyPass::Type TranslucencyPass) const;

    /** TODO: REMOVE if no longer needed: Copies scene color to the viewport's render target after applying gamma correction. */
    protected void GammaCorrectToViewportRenderTarget(FRHICommandList& RHICmdList, const FViewInfo* View, float OverrideGamma);

    /** Updates state for the end of the frame. */
    protected void RenderFinish(FRHICommandListImmediate& RHICmdList);

    protected void RenderCustomDepthPassAtLocation(FRHICommandListImmediate& RHICmdList, int32 Location);
    protected void RenderCustomDepthPass(FRHICommandListImmediate& RHICmdList);

    protected void OnStartRender(FRHICommandListImmediate& RHICmdList);

    /** Renders the scene's distortion */
    protected void RenderDistortion(FRHICommandListImmediate& RHICmdList);

    /** Returns the scene color texture multi-view is targeting. */
    FTextureRHIParamRef GetMultiViewSceneColor(const FSceneRenderTargets& SceneContext) const;

    protected void UpdatePrimitiveIndirectLightingCacheBuffers();
    protected void ClearPrimitiveSingleFrameIndirectLightingCacheBuffers();

    protected void RenderPlanarReflection(class FPlanarReflectionSceneProxy* ReflectionSceneProxy);

    protected void ResolveSceneColor(FRHICommandList& RHICmdList);

    private void ComputeFamilySize(){

    }
}
