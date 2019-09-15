package jet.opengl.renderer.Unreal4.views;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector2i;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.BoundingBox;
import jet.opengl.postprocessing.util.Recti;
import jet.opengl.postprocessing.util.StackInt;
import jet.opengl.postprocessing.util.StackLong;
import jet.opengl.renderer.Unreal4.FPreviousViewInfo;
import jet.opengl.renderer.Unreal4.atmosphere.FAtmosphereUniformShaderParameters;
import jet.opengl.renderer.Unreal4.heightfield.FHeightfieldLightingViewInfo;
import jet.opengl.renderer.Unreal4.mesh.EMeshPass;
import jet.opengl.renderer.Unreal4.FForwardLightingViewResources;
import jet.opengl.renderer.Unreal4.FMobileCSMVisibilityInfo;
import jet.opengl.renderer.Unreal4.FOcclusionQueryBatcher;
import jet.opengl.renderer.Unreal4.FParallelMeshDrawCommandPass;
import jet.opengl.renderer.Unreal4.FPrimitiveSceneInfo;
import jet.opengl.renderer.Unreal4.FPrimitiveUniformShaderParameters;
import jet.opengl.renderer.Unreal4.FPrimitiveViewRelevance;
import jet.opengl.renderer.Unreal4.FTranslucenyPrimCount;
import jet.opengl.renderer.Unreal4.FVisibleLightViewInfo;
import jet.opengl.renderer.Unreal4.UE4Engine;
import jet.opengl.renderer.Unreal4.capture.FReflectionCaptureShaderData;
import jet.opengl.renderer.Unreal4.distancefield.FGlobalDistanceFieldInfo;
import jet.opengl.renderer.Unreal4.mesh.FBatchedElements;
import jet.opengl.renderer.Unreal4.mesh.FDynamicPrimitiveResource;
import jet.opengl.renderer.Unreal4.mesh.FMeshBatch;
import jet.opengl.renderer.Unreal4.mesh.FMeshBatchAndRelevance;
import jet.opengl.renderer.Unreal4.mesh.FMeshDecalBatch;
import jet.opengl.renderer.Unreal4.mesh.FMeshPassMask;
import jet.opengl.renderer.Unreal4.mesh.FSimpleElementCollector;
import jet.opengl.renderer.Unreal4.mesh.FVolumetricMeshBatch;
import jet.opengl.renderer.Unreal4.scenes.FLODMask;
import jet.opengl.renderer.Unreal4.scenes.FSceneView;
import jet.opengl.renderer.Unreal4.scenes.FSceneViewInitOptions;
import jet.opengl.renderer.Unreal4.scenes.FSceneViewState;
import jet.opengl.renderer.Unreal4.utils.FSceneRenderTargets;
import jet.opengl.renderer.Unreal4.utils.ICustomVisibilityQuery;
import jet.opengl.renderer.Unreal4.utils.TBitArray;
import jet.opengl.renderer.Unreal4.volumetricfog.FVolumetricFogViewResources;

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
    public final ArrayList<FMeshDecalBatch> MeshDecalBatches = new ArrayList<>();

    /** Mesh batches with a volumetric material. */
    public final ArrayList<FVolumetricMeshBatch> VolumetricMeshBatches = new ArrayList<>();

    /** A map from light ID to a boolean visibility value. */
    public final ArrayList<FVisibleLightViewInfo> VisibleLightInfos = new ArrayList<>();

    /** The view's batched elements. */
    public final FBatchedElements BatchedViewElements = new FBatchedElements();

    /** The view's batched elements, above all other elements, for gizmos that should never be occluded. */
    public final FBatchedElements TopBatchedViewElements = new FBatchedElements();

    /** The view's mesh elements. */
    public  final ArrayList<FMeshBatch> ViewMeshElements = new ArrayList<>();

    /** The view's mesh elements for the foreground (editor gizmos and primitives )*/
    public final ArrayList<FMeshBatch> TopViewMeshElements = new ArrayList<>();

    /** The dynamic resources used by the view elements. */
    public final ArrayList<FDynamicPrimitiveResource> DynamicResources = new ArrayList<>();

    /** Gathered in initviews from all the primitives with dynamic view relevance, used in each mesh pass. */
    public final ArrayList<FMeshBatchAndRelevance> DynamicMeshElements = new ArrayList<>();

    /* [PrimitiveIndex] = end index index in DynamicMeshElements[], to support GetDynamicMeshElementRange(). Contains valid values only for visible primitives with bDynamicRelevance. */
    public final StackInt DynamicMeshEndIndices = new StackInt();

    /* Mesh pass relevance for gathered dynamic mesh elements. */
    public final ArrayList<FMeshPassMask> DynamicMeshElementsPassRelevance = new ArrayList<>();

    /** Gathered in UpdateRayTracingWorld from all the primitives with dynamic view relevance, used in each mesh pass. */
    public final ArrayList<FMeshBatchAndRelevance> RayTracedDynamicMeshElements;

    public final ArrayList<FMeshBatchAndRelevance> DynamicEditorMeshElements;

    public final FSimpleElementCollector SimpleElementCollector = new FSimpleElementCollector();

    public final FSimpleElementCollector EditorSimpleElementCollector = new FSimpleElementCollector();

    /** Tracks dynamic primitive data for upload to GPU Scene, when enabled. */
    public final ArrayList<FPrimitiveUniformShaderParameters> DynamicPrimitiveShaderData = new ArrayList<>();

    public BufferGL OneFramePrimitiveShaderDataBuffer;

    public final ArrayList<FParallelMeshDrawCommandPass > ParallelMeshDrawCommandPasses = new ArrayList<>();

    // Used by mobile renderer to determine whether static meshes will be rendered with CSM shaders or not.
    public final FMobileCSMVisibilityInfo MobileCSMVisibilityInfo = new FMobileCSMVisibilityInfo();

    // Primitive CustomData
//    public final ArrayList<FMemStackBase> PrimitiveCustomDataMemStack = new ArrayList<>(); // Size == 1 global stack + 1 per visibility thread (if multithread)

    /** Parameters for exponential height fog. */
    public final Vector4f ExponentialFogParameters = new Vector4f();
    public final Vector4f ExponentialFogParameters2 = new Vector4f();
    public final Vector3f ExponentialFogColor = new Vector3f();
    public float FogMaxOpacity;
    public final Vector4f ExponentialFogParameters3 = new Vector4f();
    public final Vector2f SinCosInscatteringColorCubemapRotation = new Vector2f();

    public TextureGL FogInscatteringColorCubemap;
    public final Vector3f FogInscatteringTextureParameters = new Vector3f();

    /** Parameters for directional inscattering of exponential height fog. */
    public boolean bUseDirectionalInscattering;
    public float DirectionalInscatteringExponent;
    public float DirectionalInscatteringStartDistance;
    public final Vector3f InscatteringLightDirection = new Vector3f();
    public final Vector4f DirectionalInscatteringColor = new Vector4f();

    /** Translucency lighting volume properties. */
    public final Vector4f[] TranslucencyLightingVolumeMin = new Vector4f[FInstancedViewUniformShaderParameters.TVC_MAX];
    private final float[] TranslucencyVolumeVoxelSize = new float[FInstancedViewUniformShaderParameters.TVC_MAX];
    public final Vector3f[] TranslucencyLightingVolumeSize = new Vector3f[FInstancedViewUniformShaderParameters.TVC_MAX];

    /** Number of samples in the temporal AA sequqnce */
    public int TemporalJitterSequenceLength;

    /** Index of the temporal AA jitter in the sequence. */
    public int TemporalJitterIndex;

    /** Temporal AA jitter at the pixel scale. */
    public final Vector2f TemporalJitterPixels = new Vector2f();

    /** Whether view state may be updated with this view. */
    public boolean bViewStateIsReadOnly;

    /** true if all PrimitiveVisibilityMap's bits are set to false. */
    public boolean bHasNoVisiblePrimitive;

    /** true if the view has at least one mesh with a translucent material. */
    public boolean bHasTranslucentViewMeshElements;
    /** Indicates whether previous frame transforms were reset this frame for any reason. */
    public boolean bPrevTransformsReset;
    /** Whether we should ignore queries from last frame (useful to ignoring occlusions on the first frame after a large camera movement). */
    public boolean bIgnoreExistingQueries;
    /** Whether we should submit new queries this frame. (used to disable occlusion queries completely. */
    public boolean bDisableQuerySubmissions;
    /** Whether we should disable distance-based fade transitions for this frame (usually after a large camera movement.) */
    public boolean bDisableDistanceBasedFadeTransitions;
    /** Whether the view has any materials that use the global distance field. */
    public boolean bUsesGlobalDistanceField;
    public boolean bUsesLightingChannels;
    public boolean bTranslucentSurfaceLighting;
    /** Whether the view has any materials that read from scene depth. */
    public boolean bUsesSceneDepth;


    /** Whether fog should only be computed on rendered opaque pixels or not. */
    public boolean bFogOnlyOnRenderedOpaque;

    /**
     * true if the scene has at least one decal. Used to disable stencil operations in the mobile base pass when the scene has no decals.
     * TODO: Right now decal visibility is computed right before rendering them. Ideally it should be done in InitViews and this flag should be replaced with list of visible decals
     */
    public boolean bSceneHasDecals;
    /**
     * true if the scene has at least one mesh with a material tagged as sky.
     * This is used to skip the sky rendering part during the SkyAtmosphere pass on non mobile platforms.
     */
    public boolean bSceneHasSkyMaterial;
    /** Bitmask of all shading models used by primitives in this view */
    public int ShadingModelMaskInView;

    // Previous frame view info to use for this view.
    public final FPreviousViewInfo PrevViewInfo = new FPreviousViewInfo();

    /* The GPU nodes on which to render this view. */
//    FRHIGPUMask GPUMask;

    /** An intermediate number of visible static meshes.  Doesn't account for occlusion until after FinishOcclusionQueries is called. */
    public int NumVisibleStaticMeshElements;

    /** Frame's exposure. Always > 0. */
    public float PreExposure;

    /** Mip bias to apply in material's samplers. */
    public float MaterialTextureMipBias;

    /** Precomputed visibility data, the bits are indexed by VisibilityId of a primitive component. */
    public byte[] PrecomputedVisibilityData;

    public FOcclusionQueryBatcher IndividualOcclusionQueries;
    public FOcclusionQueryBatcher GroupedOcclusionQueries;

    // Hierarchical Z Buffer
    public Texture2D HZB;

    public int NumBoxReflectionCaptures;
    public int NumSphereReflectionCaptures;
    public float FurthestReflectionCaptureDistance;
    public FReflectionCaptureShaderData ReflectionCaptureUniformBuffer;

    // Sky / Atmosphere textures (transient owned by this view info) and pointer to constants owned by SkyAtmosphere proxy.
    public TextureGL SkyAtmosphereCameraAerialPerspectiveVolume;
    public TextureGL SkyAtmosphereViewLutTexture;
	public FAtmosphereUniformShaderParameters SkyAtmosphereUniformShaderParameters;

    /** Used when there is no view state, buffers reallocate every frame. */
    public FForwardLightingViewResources ForwardLightingResourcesStorage;

    public FVolumetricFogViewResources VolumetricFogResources;

    // Size of the HZB's mipmap 0
    // NOTE: the mipmap 0 is downsampled version of the depth buffer
    public final Vector2i HZBMipmap0Size = new Vector2i();

    /** Used by occlusion for percent unoccluded calculations. */
    public float OneOverNumPossiblePixels;

    // Mobile gets one light-shaft, this light-shaft.
    public final Vector4f LightShaftCenter = new Vector4f();
    public final Vector4f LightShaftColorMask = new Vector4f();
    public final Vector4f LightShaftColorApply = new Vector4f();
    public boolean bLightShaftUse;

    public final FHeightfieldLightingViewInfo HeightfieldLightingViewInfo = new FHeightfieldLightingViewInfo();

//    TShaderMap<FGlobalShaderType>* ShaderMap;

    public boolean bIsSnapshot;

    // Optional stencil dithering optimization during prepasses
    public boolean bAllowStencilDither;

    /** Custom visibility query for view */
    public ICustomVisibilityQuery CustomVisibilityQuery;

    public final ArrayList<FPrimitiveSceneInfo> IndirectShadowPrimitives = new ArrayList<>();

    public TextureGL PrimitiveSceneDataOverrideSRV;
    public TextureGL LightmapSceneDataOverrideSRV;

    public BufferGL ShaderPrintValueBuffer;

    /**
     * Initialization constructor. Passes all parameters to FSceneView constructor
     */
    public FViewInfo(FSceneViewInitOptions InitOptions){
        super(InitOptions);
        throw new UnsupportedOperationException();
    }

    /**
     * Initialization constructor.
     * @param InView - copy to init with
     */
    public FViewInfo(FSceneView InView){
        throw new UnsupportedOperationException();
    }


    /** Verifies all the assertions made on members. */
    public boolean VerifyMembersChecks() {
        throw new UnsupportedOperationException();
    }

    /** Returns the size of view rect after primary upscale ( == only with secondary screen percentage). */
    public Vector2i GetSecondaryViewRectSize() {
        throw new UnsupportedOperationException();
    }

    /** Returns whether the view requires a secondary upscale. */
    public boolean RequiresSecondaryUpscale()
    {
//        return UnscaledViewRect.Size() != GetSecondaryViewRectSize();
        Vector2i SecondaryViewRectSize = GetSecondaryViewRectSize();
        return SecondaryViewRectSize.x != UnscaledViewRect.width || SecondaryViewRectSize.y != UnscaledViewRect.height;
    }

    /** Creates ViewUniformShaderParameters given a set of view transforms. */
    public void SetupUniformBufferParameters(
            FSceneRenderTargets SceneContext,
            FViewMatrices InViewMatrices,
            FViewMatrices InPrevViewMatrices,
            BoundingBox OutTranslucentCascadeBoundsArray,
            int NumTranslucentCascades,
            FViewUniformShaderParameters ViewUniformShaderParameters) {
        throw new UnsupportedOperationException();
    }

    /** Recreates ViewUniformShaderParameters, taking the view transform from the View Matrices */
    public final void SetupUniformBufferParameters(
            FSceneRenderTargets SceneContext,
            BoundingBox OutTranslucentCascadeBoundsArray,
            int NumTranslucentCascades,
            FViewUniformShaderParameters ViewUniformShaderParameters)
    {
        SetupUniformBufferParameters(SceneContext,
                ViewMatrices,
                PrevViewInfo.ViewMatrices,
                OutTranslucentCascadeBoundsArray,
                NumTranslucentCascades,
                ViewUniformShaderParameters);
    }

    public void SetupDefaultGlobalDistanceFieldUniformBufferParameters(FViewUniformShaderParameters ViewUniformShaderParameters) {
        throw new UnsupportedOperationException();
    }
    public void SetupGlobalDistanceFieldUniformBufferParameters(FViewUniformShaderParameters ViewUniformShaderParameters) {
        throw new UnsupportedOperationException();
    }
    public void SetupVolumetricFogUniformBufferParameters(FViewUniformShaderParameters ViewUniformShaderParameters) {
        throw new UnsupportedOperationException();
    }

    /** Initializes the RHI resources used by this view. */
    public void InitRHIResources(){
        throw new UnsupportedOperationException();
    }

    /** Determines distance culling and fades if the state changes */
    public boolean IsDistanceCulled(float DistanceSquared, float MaxDrawDistance, float MinDrawDistance, FPrimitiveSceneInfo PrimitiveSceneInfo){
        throw new UnsupportedOperationException();
    }

    /** Gets the eye adaptation render target for this view. Same as GetEyeAdaptationRT */
    public  Texture2D GetEyeAdaptationRI(/*FRHICommandList& RHICmdList*/) {
        throw new UnsupportedOperationException();
    }

    public Texture2D GetEyeAdaptation()
    {
        return GetEyeAdaptationRT();
    }

    /** Gets one of two eye adaptation render target for this view.
     * NB: will return null in the case that the internal view state pointer
     * (for the left eye in the stereo case) is null.

    public Texture2D  GetEyeAdaptationRI(FRHICommandList& RHICmdList) {
        throw new UnsupportedOperationException();
    }*/
    public Texture2D GetEyeAdaptationRT() {
        throw new UnsupportedOperationException();
    }
    /*IPooledRenderTarget* GetLastEyeAdaptationRT(FRHICommandList& RHICmdList) {
        throw new UnsupportedOperationException();
    }*/

    /**Swap the order of the two eye adaptation targets in the double buffer system */
    public void SwapEyeAdaptationRTs(/*FRHICommandList& RHICmdList*/) {
        throw new UnsupportedOperationException();
    }

    /** Tells if the eyeadaptation texture exists without attempting to allocate it. */
    public boolean HasValidEyeAdaptation(){
        throw new UnsupportedOperationException();
    }

    /** Informs sceneinfo that eyedaptation has queued commands to compute it at least once and that it can be used */
    public void SetValidEyeAdaptation() {
        throw new UnsupportedOperationException();
    }

    /** Get the last valid exposure value for eye adapation. */
    public float GetLastEyeAdaptationExposure() {
        throw new UnsupportedOperationException();
    }

    /** Get the last valid average scene luminange for eye adapation (exposure compensation curve). */
    public float GetLastAverageSceneLuminance() {
        throw new UnsupportedOperationException();
    }

    /** Informs sceneinfo that tonemapping LUT has queued commands to compute it at least once */
    public void SetValidTonemappingLUT() {
        throw new UnsupportedOperationException();
    }

    /** Gets the tonemapping LUT texture, previously computed by the CombineLUTS post process,
     * for stereo rendering, this will force the post-processing to use the same texture for both eyes*/
    public  Texture2D GetTonemappingLUTTexture() {
        throw new UnsupportedOperationException();
    }

    /** Gets the rendertarget that will be populated by CombineLUTS post process
     * for stereo rendering, this will force the post-processing to use the same render target for both eyes*/
    public FSceneRenderTargetItem GetTonemappingLUTRenderTarget(/*FRHICommandList& RHICmdList,*/ int LUTSize, boolean bUseVolumeLUT, boolean bNeedUAV, boolean bNeedFloatOutput) {
        throw new UnsupportedOperationException();
    }

    /** Instanced stereo and multi-view only need to render the left eye. */
    public boolean ShouldRenderView()
    {
        if (bHasNoVisiblePrimitive)
        {
            return false;
        }
        else if (!bIsInstancedStereoEnabled && !bIsMobileMultiViewEnabled)
        {
            return true;
        }
        else if (bIsInstancedStereoEnabled /*TODO && StereoPass != eSSP_RIGHT_EYE*/)
        {
            return true;
        }
        else if (bIsMobileMultiViewEnabled /*TODO && StereoPass != eSSP_RIGHT_EYE && Family && Family->Views.Num() > 1*/)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public ReadableVector3f GetPrevViewDirection()  { return PrevViewInfo.ViewMatrices.GetViewMatrix().GetColumn(2); }

    /** Create a snapshot of this view info on the scene allocator. */
    public FViewInfo CreateSnapshot() {
        throw new UnsupportedOperationException();
    }

    /** Destroy all snapshots before we wipe the scene allocator. */
    public static void DestroyAllSnapshots(){
        throw new UnsupportedOperationException();
    }

    // Get the range in DynamicMeshElements[] for a given PrimitiveIndex
    // @return range (start is inclusive, end is exclusive)
    public Vector2i GetDynamicMeshElementRange(int PrimitiveIndex) {
        throw new UnsupportedOperationException();
    }

    /** Set the custom data associated with a primitive scene info.	*/
    public void SetCustomData(FPrimitiveSceneInfo InPrimitiveSceneInfo, Object InCustomData){
        throw new UnsupportedOperationException();
    }

    /* Custom Data Memstack functions.
    public FMemStackBase GetCustomDataGlobalMemStack() { return PrimitiveCustomDataMemStack[0]; }
    public FMemStackBase AllocateCustomDataMemStack()
    {
        // Don't reallocate since we keep references in FRelevancePacket.
        UE4Engine.check(PrimitiveCustomDataMemStack.GetSlack() > 0);
        return *new(PrimitiveCustomDataMemStack) FMemStackBase(0);
    }
*/
    // Cache of TEXTUREGROUP_World to create view's samplers on render thread.
    // may not have a valid value if FViewInfo is created on the render thread.
    private ESamplerFilter WorldTextureGroupSamplerFilter;
    private boolean bIsValidWorldTextureGroupSamplerFilter;

    private FSceneViewState GetEffectiveViewState() {
        throw new UnsupportedOperationException();
    }

    /** Initialization that is common to the constructors. */
    private void Init(){
        throw new UnsupportedOperationException();
    }

    /** Calculates bounding boxes for the translucency lighting volume cascades. */
    private void CalcTranslucencyLightingVolumeBounds(BoundingBox InOutCascadeBoundsArray, int NumCascades) {
        throw new UnsupportedOperationException();
    }

    /** Sets the sky SH irradiance map coefficients. */
    private void SetupSkyIrradianceEnvironmentMapConstants(Vector4f OutSkyIrradianceEnvironmentMap){
        throw new UnsupportedOperationException();
    }
}
