package jet.opengl.renderer.Unreal4.views;

import java.util.ArrayList;
import java.util.HashMap;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.util.Recti;
import jet.opengl.postprocessing.util.StackLong;
import jet.opengl.renderer.Unreal4.EMeshPass;
import jet.opengl.renderer.Unreal4.FPrimitiveSceneInfo;
import jet.opengl.renderer.Unreal4.FPrimitiveViewRelevance;
import jet.opengl.renderer.Unreal4.FTranslucenyPrimCount;
import jet.opengl.renderer.Unreal4.FVisibleLightViewInfo;
import jet.opengl.renderer.Unreal4.distancefield.FGlobalDistanceFieldInfo;
import jet.opengl.renderer.Unreal4.scenes.FLODMask;
import jet.opengl.renderer.Unreal4.scenes.FSceneView;
import jet.opengl.renderer.Unreal4.scenes.FSceneViewState;
import jet.opengl.renderer.Unreal4.utils.TBitArray;

/** A FSceneView with additional state used by the scene renderer. */
public class FViewInfo extends FSceneView {
    /* Final position of the view in the final render target (in pixels), potentially scaled by ScreenPercentage */
    public final Recti ViewRect = new Recti();

    /**
     * The view's state, or NULL if no state exists.
     * This should be used internally to the renderer module to avoid having to cast View.State to an FSceneViewState*
     */
    public FSceneViewState ViewState;

    /** Cached view uniform shader parameters, to allow recreating the view uniform buffer without having to fill out the entire struct. */
    public FViewUniformShaderParameters CachedViewUniformShaderParameters;

    /** A map from primitive ID to a boolean visibility value. */
    public final TBitArray PrimitiveVisibilityMap = new TBitArray();

    /** Bit set when a primitive is known to be unoccluded. */
    public final TBitArray PrimitiveDefinitelyUnoccludedMap = new TBitArray();

    /** A map from primitive ID to a boolean is fading value. */
    public final TBitArray PotentiallyFadingPrimitiveMap = new TBitArray();

    /** Primitive fade uniform buffers, indexed by packed primitive index. */
    public final ArrayList<BufferGL> PrimitiveFadeUniformBuffers = new ArrayList<>();

    /**  Bit set when a primitive has a valid fade uniform buffer. */
    public final TBitArray PrimitiveFadeUniformBufferMap = new TBitArray();

    /** One frame dither fade in uniform buffer. */
    public BufferGL DitherFadeInUniformBuffer;

    /** One frame dither fade out uniform buffer. */
    public BufferGL DitherFadeOutUniformBuffer;

    /** A map from primitive ID to the primitive's view relevance. */
    public final ArrayList<FPrimitiveViewRelevance> PrimitiveViewRelevanceMap = new ArrayList<>();

    /** A map from static mesh ID to a boolean visibility value. */
    public final TBitArray StaticMeshVisibilityMap = new TBitArray();

    /** A map from static mesh ID to a boolean dithered LOD fade out value. */
    public final TBitArray StaticMeshFadeOutDitheredLODMap = new TBitArray();

    /** A map from static mesh ID to a boolean dithered LOD fade in value. */
    public final TBitArray StaticMeshFadeInDitheredLODMap = new TBitArray();

    /** Will only contain relevant primitives for view and/or shadow */
    public final ArrayList<FLODMask> PrimitivesLODMask = new ArrayList<>();

    /** An array of batch element visibility masks, valid only for meshes
     set visible in StaticMeshVisibilityMap. */
    public final StackLong StaticMeshBatchVisibility = new StackLong();

    /** The dynamic primitives with simple lights visible in this view. */
    public final ArrayList<FPrimitiveSceneInfo> VisibleDynamicPrimitivesWithSimpleLights = new ArrayList<>();

    /** Number of dynamic primitives visible in this view. */
    public int NumVisibleDynamicPrimitives;

    /** Number of dynamic editor primitives visible in this view. */
    public int NumVisibleDynamicEditorPrimitives;

    /** Number of dynamic mesh elements per mesh pass (inside FViewInfo::DynamicMeshElements). */
    public final int[] NumVisibleDynamicMeshElements = new int[EMeshPass.values().length];

    /** List of visible primitives with dirty indirect lighting cache buffers */
    public final ArrayList<FPrimitiveSceneInfo> DirtyIndirectLightingCacheBufferPrimitives = new ArrayList<>();

    /** Maps a single primitive to it's per view translucent self shadow uniform buffer. */
    public final HashMap<Integer, BufferGL> TranslucentSelfShadowUniformBufferMap = new HashMap<>();

    /** View dependent global distance field clipmap info. */
    public final FGlobalDistanceFieldInfo GlobalDistanceFieldInfo = new FGlobalDistanceFieldInfo();

    /** Count of translucent prims for this view. */
    public FTranslucenyPrimCount TranslucentPrimCount = new FTranslucenyPrimCount();

    public boolean bHasDistortionPrimitives;
    public boolean bHasCustomDepthPrimitives;

    /** Mesh batches with for mesh decal rendering. */
    TArray<FMeshDecalBatch, SceneRenderingAllocator> MeshDecalBatches;

    /** Mesh batches with a volumetric material. */
    TArray<FVolumetricMeshBatch, SceneRenderingAllocator> VolumetricMeshBatches;

    /** A map from light ID to a boolean visibility value. */
    public final ArrayList<FVisibleLightViewInfo> VisibleLightInfos = new ArrayList<>();

    /** The view's batched elements. */
    FBatchedElements BatchedViewElements;

    /** The view's batched elements, above all other elements, for gizmos that should never be occluded. */
    FBatchedElements TopBatchedViewElements;

    /** The view's mesh elements. */
    TIndirectArray<FMeshBatch> ViewMeshElements;

    /** The view's mesh elements for the foreground (editor gizmos and primitives )*/
    TIndirectArray<FMeshBatch> TopViewMeshElements;

    /** The dynamic resources used by the view elements. */
    TArray<FDynamicPrimitiveResource*> DynamicResources;

    /** Gathered in initviews from all the primitives with dynamic view relevance, used in each mesh pass. */
    TArray<FMeshBatchAndRelevance,SceneRenderingAllocator> DynamicMeshElements;

    /* [PrimitiveIndex] = end index index in DynamicMeshElements[], to support GetDynamicMeshElementRange(). Contains valid values only for visible primitives with bDynamicRelevance. */
    TArray<uint32, SceneRenderingAllocator> DynamicMeshEndIndices;

    /* Mesh pass relevance for gathered dynamic mesh elements. */
    TArray<FMeshPassMask, SceneRenderingAllocator> DynamicMeshElementsPassRelevance;

    /** Gathered in UpdateRayTracingWorld from all the primitives with dynamic view relevance, used in each mesh pass. */
    TArray<FMeshBatchAndRelevance, SceneRenderingAllocator> RayTracedDynamicMeshElements;

    TArray<FMeshBatchAndRelevance,SceneRenderingAllocator> DynamicEditorMeshElements;

    FSimpleElementCollector SimpleElementCollector;

    FSimpleElementCollector EditorSimpleElementCollector;

    /** Tracks dynamic primitive data for upload to GPU Scene, when enabled. */
    TArray<FPrimitiveUniformShaderParameters> DynamicPrimitiveShaderData;

    FRWBufferStructured OneFramePrimitiveShaderDataBuffer;

    TStaticArray<FParallelMeshDrawCommandPass, EMeshPass::Num> ParallelMeshDrawCommandPasses;

    // Used by mobile renderer to determine whether static meshes will be rendered with CSM shaders or not.
    FMobileCSMVisibilityInfo MobileCSMVisibilityInfo;

    // Primitive CustomData
    TArray<FMemStackBase, SceneRenderingAllocator> PrimitiveCustomDataMemStack; // Size == 1 global stack + 1 per visibility thread (if multithread)

    /** Parameters for exponential height fog. */
    FVector4 ExponentialFogParameters;
    FVector4 ExponentialFogParameters2;
    FVector ExponentialFogColor;
    float FogMaxOpacity;
    FVector4 ExponentialFogParameters3;
    FVector2D SinCosInscatteringColorCubemapRotation;

    UTexture* FogInscatteringColorCubemap;
    FVector FogInscatteringTextureParameters;

    /** Parameters for directional inscattering of exponential height fog. */
    bool bUseDirectionalInscattering;
    float DirectionalInscatteringExponent;
    float DirectionalInscatteringStartDistance;
    FVector InscatteringLightDirection;
    FLinearColor DirectionalInscatteringColor;

    /** Translucency lighting volume properties. */
    FVector TranslucencyLightingVolumeMin[TVC_MAX];
    float TranslucencyVolumeVoxelSize[TVC_MAX];
    FVector TranslucencyLightingVolumeSize[TVC_MAX];

    /** Number of samples in the temporal AA sequqnce */
    int32 TemporalJitterSequenceLength;

    /** Index of the temporal AA jitter in the sequence. */
    int32 TemporalJitterIndex;

    /** Temporal AA jitter at the pixel scale. */
    FVector2D TemporalJitterPixels;

    /** Whether view state may be updated with this view. */
    uint32 bViewStateIsReadOnly : 1;

    /** true if all PrimitiveVisibilityMap's bits are set to false. */
    uint32 bHasNoVisiblePrimitive : 1;

    /** true if the view has at least one mesh with a translucent material. */
    uint32 bHasTranslucentViewMeshElements : 1;
    /** Indicates whether previous frame transforms were reset this frame for any reason. */
    uint32 bPrevTransformsReset : 1;
    /** Whether we should ignore queries from last frame (useful to ignoring occlusions on the first frame after a large camera movement). */
    uint32 bIgnoreExistingQueries : 1;
    /** Whether we should submit new queries this frame. (used to disable occlusion queries completely. */
    uint32 bDisableQuerySubmissions : 1;
    /** Whether we should disable distance-based fade transitions for this frame (usually after a large camera movement.) */
    uint32 bDisableDistanceBasedFadeTransitions : 1;
    /** Whether the view has any materials that use the global distance field. */
    uint32 bUsesGlobalDistanceField : 1;
    uint32 bUsesLightingChannels : 1;
    uint32 bTranslucentSurfaceLighting : 1;
    /** Whether the view has any materials that read from scene depth. */
    uint32 bUsesSceneDepth : 1;


    /** Whether fog should only be computed on rendered opaque pixels or not. */
    uint32 bFogOnlyOnRenderedOpaque : 1;

    /**
     * true if the scene has at least one decal. Used to disable stencil operations in the mobile base pass when the scene has no decals.
     * TODO: Right now decal visibility is computed right before rendering them. Ideally it should be done in InitViews and this flag should be replaced with list of visible decals
     */
    uint32 bSceneHasDecals : 1;
    /**
     * true if the scene has at least one mesh with a material tagged as sky.
     * This is used to skip the sky rendering part during the SkyAtmosphere pass on non mobile platforms.
     */
    uint32 bSceneHasSkyMaterial : 1;
    /** Bitmask of all shading models used by primitives in this view */
    uint16 ShadingModelMaskInView;

    // Previous frame view info to use for this view.
    FPreviousViewInfo PrevViewInfo;

    /** The GPU nodes on which to render this view. */
    FRHIGPUMask GPUMask;

    /** An intermediate number of visible static meshes.  Doesn't account for occlusion until after FinishOcclusionQueries is called. */
    int32 NumVisibleStaticMeshElements;

    /** Frame's exposure. Always > 0. */
    float PreExposure;

    /** Mip bias to apply in material's samplers. */
    float MaterialTextureMipBias;

    /** Precomputed visibility data, the bits are indexed by VisibilityId of a primitive component. */
	const uint8* PrecomputedVisibilityData;

    FOcclusionQueryBatcher IndividualOcclusionQueries;
    FOcclusionQueryBatcher GroupedOcclusionQueries;

    // Hierarchical Z Buffer
    TRefCountPtr<IPooledRenderTarget> HZB;

    int32 NumBoxReflectionCaptures;
    int32 NumSphereReflectionCaptures;
    float FurthestReflectionCaptureDistance;
    TUniformBufferRef<FReflectionCaptureShaderData> ReflectionCaptureUniformBuffer;

    // Sky / Atmosphere textures (transient owned by this view info) and pointer to constants owned by SkyAtmosphere proxy.
    TRefCountPtr<IPooledRenderTarget> SkyAtmosphereCameraAerialPerspectiveVolume;
    TRefCountPtr<IPooledRenderTarget> SkyAtmosphereViewLutTexture;
	const FAtmosphereUniformShaderParameters* SkyAtmosphereUniformShaderParameters;

    /** Used when there is no view state, buffers reallocate every frame. */
    TUniquePtr<FForwardLightingViewResources> ForwardLightingResourcesStorage;

    FVolumetricFogViewResources VolumetricFogResources;

    // Size of the HZB's mipmap 0
    // NOTE: the mipmap 0 is downsampled version of the depth buffer
    FIntPoint HZBMipmap0Size;

    /** Used by occlusion for percent unoccluded calculations. */
    float OneOverNumPossiblePixels;

    // Mobile gets one light-shaft, this light-shaft.
    FVector4 LightShaftCenter;
    FLinearColor LightShaftColorMask;
    FLinearColor LightShaftColorApply;
    bool bLightShaftUse;

    FHeightfieldLightingViewInfo HeightfieldLightingViewInfo;

    TShaderMap<FGlobalShaderType>* ShaderMap;

    bool bIsSnapshot;

    // Optional stencil dithering optimization during prepasses
    bool bAllowStencilDither;

    /** Custom visibility query for view */
    ICustomVisibilityQuery* CustomVisibilityQuery;

    TArray<FPrimitiveSceneInfo*, SceneRenderingAllocator> IndirectShadowPrimitives;

    FShaderResourceViewRHIRef PrimitiveSceneDataOverrideSRV;
    FShaderResourceViewRHIRef LightmapSceneDataOverrideSRV;

    FRWBufferStructured ShaderPrintValueBuffer;

    /**
     * Initialization constructor. Passes all parameters to FSceneView constructor
     */
    FViewInfo(const FSceneViewInitOptions& InitOptions);

    /**
     * Initialization constructor.
     * @param InView - copy to init with
     */
    explicit FViewInfo(const FSceneView* InView);

    /**
     * Destructor.
     */
	~FViewInfo();

#if DO_CHECK
    /** Verifies all the assertions made on members. */
    bool VerifyMembersChecks() const;
#endif

    /** Returns the size of view rect after primary upscale ( == only with secondary screen percentage). */
    FIntPoint GetSecondaryViewRectSize() const;

    /** Returns whether the view requires a secondary upscale. */
    bool RequiresSecondaryUpscale() const
    {
        return UnscaledViewRect.Size() != GetSecondaryViewRectSize();
    }

    /** Creates ViewUniformShaderParameters given a set of view transforms. */
    void SetupUniformBufferParameters(
            FSceneRenderTargets& SceneContext,
		const FViewMatrices& InViewMatrices,
		const FViewMatrices& InPrevViewMatrices,
            FBox* OutTranslucentCascadeBoundsArray,
            int32 NumTranslucentCascades,
            FViewUniformShaderParameters& ViewUniformShaderParameters) const;

    /** Recreates ViewUniformShaderParameters, taking the view transform from the View Matrices */
    inline void SetupUniformBufferParameters(
            FSceneRenderTargets& SceneContext,
            FBox* OutTranslucentCascadeBoundsArray,
            int32 NumTranslucentCascades,
            FViewUniformShaderParameters& ViewUniformShaderParameters) const
    {
        SetupUniformBufferParameters(SceneContext,
                ViewMatrices,
                PrevViewInfo.ViewMatrices,
                OutTranslucentCascadeBoundsArray,
                NumTranslucentCascades,
                ViewUniformShaderParameters);
    }

    void SetupDefaultGlobalDistanceFieldUniformBufferParameters(FViewUniformShaderParameters& ViewUniformShaderParameters) const;
    void SetupGlobalDistanceFieldUniformBufferParameters(FViewUniformShaderParameters& ViewUniformShaderParameters) const;
    void SetupVolumetricFogUniformBufferParameters(FViewUniformShaderParameters& ViewUniformShaderParameters) const;

    /** Initializes the RHI resources used by this view. */
    void InitRHIResources();

    /** Determines distance culling and fades if the state changes */
    bool IsDistanceCulled(float DistanceSquared, float MaxDrawDistance, float MinDrawDistance, const FPrimitiveSceneInfo* PrimitiveSceneInfo);

    /** Gets the eye adaptation render target for this view. Same as GetEyeAdaptationRT */
    IPooledRenderTarget* GetEyeAdaptation(FRHICommandList& RHICmdList) const;

    IPooledRenderTarget* GetEyeAdaptation() const
    {
        return GetEyeAdaptationRT();
    }

    /** Gets one of two eye adaptation render target for this view.
     * NB: will return null in the case that the internal view state pointer
     * (for the left eye in the stereo case) is null.
     */
    IPooledRenderTarget* GetEyeAdaptationRT(FRHICommandList& RHICmdList) const;
    IPooledRenderTarget* GetEyeAdaptationRT() const;
    IPooledRenderTarget* GetLastEyeAdaptationRT(FRHICommandList& RHICmdList) const;

    /**Swap the order of the two eye adaptation targets in the double buffer system */
    void SwapEyeAdaptationRTs(FRHICommandList& RHICmdList) const;

    /** Tells if the eyeadaptation texture exists without attempting to allocate it. */
    bool HasValidEyeAdaptation() const;

    /** Informs sceneinfo that eyedaptation has queued commands to compute it at least once and that it can be used */
    void SetValidEyeAdaptation() const;

    /** Get the last valid exposure value for eye adapation. */
    float GetLastEyeAdaptationExposure() const;

    /** Get the last valid average scene luminange for eye adapation (exposure compensation curve). */
    float GetLastAverageSceneLuminance() const;

    /** Informs sceneinfo that tonemapping LUT has queued commands to compute it at least once */
    void SetValidTonemappingLUT() const;

    /** Gets the tonemapping LUT texture, previously computed by the CombineLUTS post process,
     * for stereo rendering, this will force the post-processing to use the same texture for both eyes*/
	const FTextureRHIRef* GetTonemappingLUTTexture() const;

    /** Gets the rendertarget that will be populated by CombineLUTS post process
     * for stereo rendering, this will force the post-processing to use the same render target for both eyes*/
    FSceneRenderTargetItem* GetTonemappingLUTRenderTarget(FRHICommandList& RHICmdList, const int32 LUTSize, const bool bUseVolumeLUT, const bool bNeedUAV, const bool bNeedFloatOutput) const;

    /** Instanced stereo and multi-view only need to render the left eye. */
    bool ShouldRenderView() const
    {
        if (bHasNoVisiblePrimitive)
        {
            return false;
        }
        else if (!bIsInstancedStereoEnabled && !bIsMobileMultiViewEnabled)
        {
            return true;
        }
        else if (bIsInstancedStereoEnabled && StereoPass != eSSP_RIGHT_EYE)
        {
            return true;
        }
        else if (bIsMobileMultiViewEnabled && StereoPass != eSSP_RIGHT_EYE && Family && Family->Views.Num() > 1)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    inline FVector GetPrevViewDirection() const { return PrevViewInfo.ViewMatrices.GetViewMatrix().GetColumn(2); }

    /** Create a snapshot of this view info on the scene allocator. */
    FViewInfo* CreateSnapshot() const;

    /** Destroy all snapshots before we wipe the scene allocator. */
    static void DestroyAllSnapshots();

    // Get the range in DynamicMeshElements[] for a given PrimitiveIndex
    // @return range (start is inclusive, end is exclusive)
    FInt32Range GetDynamicMeshElementRange(uint32 PrimitiveIndex) const;

    /** Set the custom data associated with a primitive scene info.	*/
    void SetCustomData(const FPrimitiveSceneInfo* InPrimitiveSceneInfo, void* InCustomData);

    /** Custom Data Memstack functions.	*/
    FORCEINLINE FMemStackBase& GetCustomDataGlobalMemStack() { return PrimitiveCustomDataMemStack[0]; }
    FORCEINLINE FMemStackBase& AllocateCustomDataMemStack()
    {
        // Don't reallocate since we keep references in FRelevancePacket.
        check(PrimitiveCustomDataMemStack.GetSlack() > 0);
        return *new(PrimitiveCustomDataMemStack) FMemStackBase(0);
    }

    // Cache of TEXTUREGROUP_World to create view's samplers on render thread.
    // may not have a valid value if FViewInfo is created on the render thread.
    private ESamplerFilter WorldTextureGroupSamplerFilter;
    private boolean bIsValidWorldTextureGroupSamplerFilter;

    private FSceneViewState GetEffectiveViewState() const;

    /** Initialization that is common to the constructors. */
    private void Init();

    /** Calculates bounding boxes for the translucency lighting volume cascades. */
    private void CalcTranslucencyLightingVolumeBounds(FBox* InOutCascadeBoundsArray, int32 NumCascades) const;

    /** Sets the sky SH irradiance map coefficients. */
    private void SetupSkyIrradianceEnvironmentMapConstants(FVector4* OutSkyIrradianceEnvironmentMap) const;
}
