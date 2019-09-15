package jet.opengl.renderer.Unreal4.mesh;

import java.util.ArrayList;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.renderer.Unreal4.UE4Engine;
import jet.opengl.renderer.Unreal4.scenes.ESceneDepthPriorityGroup;
import jet.opengl.renderer.Unreal4.utils.FHitProxyId;

/**
 * A batch of mesh elements, all with the same material and vertex buffer
 */
public class FMeshBatch {
    public final ArrayList<FMeshBatchElement> Elements = new ArrayList<>();

    /* Mesh Id in a primitive. Used for stable sorting of draws belonging to the same primitive. **/
    public short MeshIdInPrimitive;

    /** LOD index of the mesh, used for fading LOD transitions. */
    public byte LODIndex = UE4Engine.INDEX_NONE;
    public byte SegmentIndex;

//#if !(UE_BUILD_SHIPPING || UE_BUILD_TEST)
    /** Conceptual LOD index used for the LOD Coloration visualization. */
    public byte VisualizeLODIndex = UE4Engine.INDEX_NONE;
//#endif

    /** Conceptual HLOD index used for the HLOD Coloration visualization. */
    public byte VisualizeHLODIndex = UE4Engine.INDEX_NONE;

    public boolean ReverseCulling;
    public boolean bDisableBackfaceCulling;

    /**
     * Pass feature relevance flags.  Allows a proxy to submit fast representations for passes which can take advantage of it,
     * for example separate index buffer for depth-only rendering since vertices can be merged based on position and ignore UV differences.

#if RHI_RAYTRACING
    uint32 CastRayTracedShadow : 1;	// Whether it casts ray traced shadow.
#endif*/

    public boolean CastShadow = true;	// Whether it can be used in shadow renderpasses.
    public boolean bUseForMaterial = true;	// Whether it can be used in renderpasses requiring material outputs.
    public boolean bUseForDepthPass = true;	// Whether it can be used in depth pass.
    public boolean bUseAsOccluder = true;	// Hint whether this mesh is a good occluder.
    public boolean bWireframe;
    // e.g. PT_TriangleList(default), PT_LineList, ..
    public int Type = GLenum.GL_TRIANGLES;
    // e.g. SDPG_World (default), SDPG_Foreground
    public ESceneDepthPriorityGroup DepthPriorityGroup = ESceneDepthPriorityGroup.SDPG_World /*: SDPG_NumBits*/;

    /** Whether view mode overrides can be applied to this mesh eg unlit, wireframe. */
    public boolean bCanApplyViewModeOverrides;

    /**
     * Whether to treat the batch as selected in special viewmodes like wireframe.
     * This is needed instead of just Proxy->IsSelected() because some proxies do weird things with selection highlighting, like FStaticMeshSceneProxy.
     */
    public boolean bUseWireframeSelectionColoring;

    /**
     * Whether the batch should receive the selection outline.
     * This is useful for proxies which support selection on a per-mesh batch basis.
     * They submit multiple mesh batches when selected, some of which have bUseSelectionOutline enabled.
     */
    public boolean bUseSelectionOutline = true;

    /** Whether the mesh batch can be selected through editor selection, aka hit proxies. */
    public boolean bSelectable = true;

    /** Whether the mesh batch needs VertexFactory->GetStaticBatchElementVisibility to be called each frame to determine which elements of the batch are visible. */
    public boolean bRequiresPerElementVisibility;

    /** Whether the mesh batch should apply dithered LOD. */
    public boolean bDitheredLODTransition;

    /** Whether the mesh batch can be rendered to virtual textures. */
    public boolean bRenderToVirtualTexture ;
    /** What virtual texture material type this mesh batch should be rendered with. */
    public int RuntimeVirtualTextureMaterialType /*: ERuntimeVirtualTextureMaterialType_NumBits*/;

    // can be NULL
//	public FLightCacheInterface LCI;

    /** Vertex factory for rendering, required. */
	public BufferGL VertexFactory;

    /** Material proxy for rendering, required. */
//	public FMaterialRenderProxy MaterialRenderProxy;

    /** The current hit proxy ID being rendered. */
    public final FHitProxyId BatchHitProxyId = new FHitProxyId();

    /** This is the threshold that will be used to know if we should use this mesh batch or use one with no tessellation enabled */
    float TessellationDisablingShadowMapMeshSize;

    public FMeshBatch(){
        Elements.add(new FMeshBatchElement());
    }

    public void Set(FMeshBatch ohs){
        throw new UnsupportedOperationException();
    }

    public boolean IsTranslucent(int InFeatureLevel)
    {
        // Note: blend mode does not depend on the feature level we are actually rendering in.
//        return IsTranslucentBlendMode(MaterialRenderProxy->GetMaterial(InFeatureLevel)->GetBlendMode());
        throw new UnsupportedOperationException();
    }

    // todo: can be optimized with a single function that returns multiple states (Translucent, Decal, Masked)
    public boolean IsDecal(int InFeatureLevel)
    {
        // Note: does not depend on the feature level we are actually rendering in.
		/*const FMaterial* Mat = MaterialRenderProxy->GetMaterial(InFeatureLevel);
        return Mat->IsDeferredDecal();*/

        throw new UnsupportedOperationException();
    }

    public boolean CastsDeepShadow(/*ERHIFeatureLevel::Type InFeatureLevel*/)
    {
		/*const FMaterial* Mat = MaterialRenderProxy->GetMaterial(ERHIFeatureLevel::SM5);
        return Mat->GetShadingModels().HasOnlyShadingModel(EMaterialShadingModel::MSM_Hair);*/

        throw new UnsupportedOperationException();
    }

    public boolean IsMasked(int InFeatureLevel)
    {
        // Note: blend mode does not depend on the feature level we are actually rendering in.
//        return MaterialRenderProxy->GetMaterial(InFeatureLevel)->IsMasked();

        throw new UnsupportedOperationException();
    }

    /** Converts from an int32 index into a int8 */
    public static byte QuantizeLODIndex(int NewLODIndex)
    {
//        checkSlow(NewLODIndex >= SCHAR_MIN && NewLODIndex <= SCHAR_MAX);
        return (byte)NewLODIndex;
    }

    public int GetNumPrimitives()
    {
        int Count = 0;
        for (int ElementIdx = 0; ElementIdx < Elements.size(); ++ElementIdx)
        {
            Count += Elements.get(ElementIdx).GetNumPrimitives();
        }
        return Count;
    }

    public boolean HasAnyDrawCalls()
    {
        for (int ElementIdx = 0; ElementIdx < Elements.size(); ++ElementIdx)
        {
            FMeshBatchElement element = Elements.get(ElementIdx);
            if (element.GetNumPrimitives() > 0 || element.IndirectArgsBuffer != null)
            {
                return true;
            }
        }
        return false;
    }

//    public void PreparePrimitiveUniformBuffer(const FPrimitiveSceneProxy* PrimitiveSceneProxy, ERHIFeatureLevel::Type FeatureLevel);

}
