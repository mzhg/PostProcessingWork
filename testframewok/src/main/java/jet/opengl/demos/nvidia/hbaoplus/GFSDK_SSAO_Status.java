package jet.opengl.demos.nvidia.hbaoplus;

public enum GFSDK_SSAO_Status {

	/** Success */
	GFSDK_SSAO_OK,              
	/** The header version number does not match the DLL version number */
    GFSDK_SSAO_VERSION_MISMATCH,
    /** One of the required argument pointers is NULL */
    GFSDK_SSAO_NULL_ARGUMENT,
    /** The projection matrix is not valid */
    GFSDK_SSAO_INVALID_PROJECTION_MATRIX,
    /** The world-to-view matrix is not valid (transposing it may help) */
    GFSDK_SSAO_INVALID_WORLD_TO_VIEW_MATRIX,
    /** The normal-texture resolution does not match the depth-texture resolution */
    GFSDK_SSAO_INVALID_NORMAL_TEXTURE_RESOLUTION,
    /** The normal-texture sample count does not match the depth-texture sample count */
    GFSDK_SSAO_INVALID_NORMAL_TEXTURE_SAMPLE_COUNT,
    /** One of the viewport dimensions (width or height) is 0 */
    GFSDK_SSAO_INVALID_VIEWPORT_DIMENSIONS,
    /** The viewport depth range is not a sub-range of [0.f,1.f] */
    GFSDK_SSAO_INVALID_VIEWPORT_DEPTH_RANGE,
    /** Failed to allocate memory on the heap */
    GFSDK_SSAO_MEMORY_ALLOCATION_FAILED,
    /** The depth-stencil resolution does not match the output render-target resolution */
    GFSDK_SSAO_INVALID_DEPTH_STENCIL_RESOLUTION,
    /** The depth-stencil sample count does not match the output render-target sample count */
    GFSDK_SSAO_INVALID_DEPTH_STENCIL_SAMPLE_COUNT,
    //
    // D3D-specific enums
    //
    /** The current D3D11 feature level is lower than 11_0 */
    GFSDK_SSAO_D3D_FEATURE_LEVEL_NOT_SUPPORTED,
    /** A resource-creation call has failed (running out of memory?) */
    GFSDK_SSAO_D3D_RESOURCE_CREATION_FAILED,
    /** CLAMP_TO_BORDER is used (implemented on D3D11 & GL, but not on D3D12) */
    GFSDK_SSAO_D3D12_UNSUPPORTED_DEPTH_CLAMP_MODE,
    /** One of the heaps provided to GFSDK_SSAO_CreateContext_D3D12 has an unexpected type */
    GFSDK_SSAO_D3D12_INVALID_HEAP_TYPE,
    /** One of the heaps provided to GFSDK_SSAO_CreateContext_D3D12 has insufficient descriptors */
    GFSDK_SSAO_D3D12_INSUFFICIENT_DESCRIPTORS,
    /** NodeMask has more than one bit set. HBAO+ only supports operation on one D3D12 device node. */
    GFSDK_SSAO_D3D12_INVALID_NODE_MASK,
    //
    // GL-specific enums
    //
    /** One of the input textures is not GL_TEXTURE_2D or GL_TEXTURE_2D_MULTISAMPLE */
    GFSDK_SSAO_GL_INVALID_TEXTURE_TARGET,
    /** One of the input texture objects has index 0 */
    GFSDK_SSAO_GL_INVALID_TEXTURE_OBJECT,
    /** A GL resource-creation call has failed (running out of memory?) */
    GFSDK_SSAO_GL_RESOURCE_CREATION_FAILED,
    /** One of the provided GL function pointers is NULL */
    GFSDK_SSAO_GL_NULL_FUNCTION_POINTER,
    /** A custom input viewport is enabled (not supported on GL) */
    GFSDK_SSAO_GL_UNSUPPORTED_VIEWPORT,
}
