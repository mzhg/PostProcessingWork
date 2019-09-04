package jet.opengl.renderer.Unreal4.views;

/**
 * Define view modes to get specific show flag settings (some on, some off and some are not altered)
 * Don't change the order, the ID is serialized with the editor
 */
public enum EViewModeIndex {
    /** Wireframe w/ brushes. */
    VMI_BrushWireframe /*= 0 UMETA(DisplayName = "Brush Wireframe")*/,
    /** Wireframe w/ BSP. */
    VMI_Wireframe /*= 1 UMETA(DisplayName = "Wireframe")*/,
    /** Unlit. */
    VMI_Unlit /*= 2 UMETA(DisplayName = "Unlit")*/,
    /** Lit. */
    VMI_Lit /*= 3 UMETA(DisplayName = "Lit")*/,
    VMI_Lit_DetailLighting /*= 4 UMETA(DisplayName = "Detail Lighting")*/,
    /** Lit wo/ materials. */
    VMI_LightingOnly /*= 5 UMETA(DisplayName = "Lighting Only")*/,
    /** Colored according to light count. */
    VMI_LightComplexity /*= 6 UMETA(DisplayName = "Light Complexity")*/,
    /** Colored according to shader complexity. */
    VMI_ShaderComplexity /*= 8 UMETA(DisplayName = "Shader Complexity")*/,
    /** Colored according to world-space LightMap texture density. */
    VMI_LightmapDensity /*= 9 UMETA(DisplayName = "Lightmap Density")*/,
    /** Colored according to light count - showing lightmap texel density on texture mapped objects. */
    VMI_LitLightmapDensity /*= 10 UMETA(DisplayName = "Lit Lightmap Density")*/,
    VMI_ReflectionOverride /*= 11 UMETA(DisplayName = "Reflections")*/,
    VMI_VisualizeBuffer /*= 12 UMETA(DisplayName = "Buffer Visualization")*/,
    //	VMI_VoxelLighting = 13,

    /** Colored according to stationary light overlap. */
    VMI_StationaryLightOverlap /*= 14 UMETA(DisplayName = "Stationary Light Overlap")*/,

    VMI_CollisionPawn /*= 15 UMETA(DisplayName = "Player Collision")*/,
    VMI_CollisionVisibility /*= 16 UMETA(DisplayName = "Visibility Collision")*/,
    //VMI_UNUSED = 17,
    /** Colored according to the current LOD index. */
    VMI_LODColoration /*= 18 UMETA(DisplayName = "Mesh LOD Coloration")*/,
    /** Colored according to the quad coverage. */
    VMI_QuadOverdraw /*= 19 UMETA(DisplayName = "Quad Overdraw")*/,
    /** Visualize the accuracy of the primitive distance computed for texture streaming. */
    VMI_PrimitiveDistanceAccuracy /*= 20 UMETA(DisplayName = "Primitive Distance")*/,
    /** Visualize the accuracy of the mesh UV densities computed for texture streaming. */
    VMI_MeshUVDensityAccuracy /*= 21  UMETA(DisplayName = "Mesh UV Density")*/,
    /** Colored according to shader complexity, including quad overdraw. */
    VMI_ShaderComplexityWithQuadOverdraw /*= 22 UMETA(DisplayName = "Shader Complexity & Quads")*/,
    /** Colored according to the current HLOD index. */
    VMI_HLODColoration /*= 23  UMETA(DisplayName = "Hierarchical LOD Coloration")*/,
    /** Group item for LOD and HLOD coloration*/
    VMI_GroupLODColoration /*= 24  UMETA(DisplayName = "Group LOD Coloration")*/,
    /** Visualize the accuracy of the material texture scales used for texture streaming. */
    VMI_MaterialTextureScaleAccuracy /*= 25 UMETA(DisplayName = "Material Texture Scales")*/,
    /** Compare the required texture resolution to the actual resolution. */
    VMI_RequiredTextureResolution /*= 26 UMETA(DisplayName = "Required Texture Resolution")*/,

    // Ray tracing modes

    /** Run path tracing pipeline */
    VMI_PathTracing /*= 27 UMETA(DisplayName = "Path Tracing")*/,
    /** Run ray tracing debug pipeline */
    VMI_RayTracingDebug /*= 28 UMETA(DisplayName = "Ray Tracing Debug")*/,

    VMI_Unknown
}
