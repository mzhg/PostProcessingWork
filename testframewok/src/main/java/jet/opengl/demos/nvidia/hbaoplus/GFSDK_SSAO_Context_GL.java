package jet.opengl.demos.nvidia.hbaoplus;


import jet.opengl.postprocessing.common.Disposeable;

/** Note: The RenderAO, PreCreateFBOs and dispose entry points should not be called simultaneously from different threads. */
public interface GFSDK_SSAO_Context_GL extends GFSDK_SSAO_Context, Disposeable {

	public static GFSDK_SSAO_Context_GL createContextGL(){
		return new Renderer();
	}
	
	/**
	 * Renders SSAO.<p>
	 * 
	 * See {@link #renderAO(GFSDK_SSAO_InputData_GL, GFSDK_SSAO_Parameters, GFSDK_SSAO_Output_GL, int)}
	 * @param inputData
	 * @param parameters
	 * @param output
	 * @return
	 */
	public default GFSDK_SSAO_Status renderAO(GFSDK_SSAO_InputData_GL inputData, GFSDK_SSAO_Parameters parameters, GFSDK_SSAO_Output_GL output){
		return renderAO(inputData, parameters, output, GFSDK_SSAO_RenderMask.GFSDK_SSAO_RENDER_AO);
	}
	
	/**
	 * Renders SSAO.<p>
	 * Remarks:<ul>
	 * <li> Allocates internal GL framebuffer objects on first use, and re-allocates them when the depth-texture resolution changes.
	 * <li> All the relevant GL states are saved and restored internally when entering and exiting the call.
	 * <li> Setting RenderMask = GFSDK_SSAO_RENDER_DEBUG_NORMAL_Z can be useful to visualize the normals used for the AO rendering.
	 * <li> The current GL PolygonMode is assumed to be GL_FILL.
	 * <li> The OutputFBO cannot contain a texture bound in InputData.
	 * </ul>
	 * @param inputData
	 * @param parameters
	 * @param output
	 * @param renderMask
	 * @return <ul>
	 * <li> GFSDK_SSAO_NULL_ARGUMENT                        - One of the required argument pointers is NULL
	 * <li> GFSDK_SSAO_INVALID_PROJECTION_MATRIX            - The projection matrix is not valid
	 * <li> GFSDK_SSAO_INVALID_VIEWPORT_DEPTH_RANGE         - The viewport depth range is not a sub-range of [0.f,1.f]
	 * <li> GFSDK_SSAO_GL_INVALID_TEXTURE_TARGET            - One of the input textures is not GL_TEXTURE_2D or GL_TEXTURE_2D_MULTISAMPLE
	 * <li> GFSDK_SSAO_GL_INVALID_TEXTURE_OBJECT            - One of the input texture objects has index 0
	 * <li> GFSDK_SSAO_GL_RESOURCE_CREATION_FAILED          - A GL resource-creation call has failed (running out of memory?)
	 * <li> GFSDK_SSAO_GL_UNSUPPORTED_VIEWPORT              - A custom input viewport is enabled (not supported on GL)
	 * <li> GFSDK_SSAO_OK                                   - Success
	 * </ul>
	 */
	GFSDK_SSAO_Status renderAO(GFSDK_SSAO_InputData_GL inputData, GFSDK_SSAO_Parameters parameters, GFSDK_SSAO_Output_GL output, int renderMask);

	
	/**
	 * [Optional] Pre-creates all internal FBOs for RenderAO.<p>
	 * Remarks:<ul>
	 * <li> This call may be safely skipped since RenderAO creates its framebuffer objects on demand if they were not pre-created.
	 * <li> This call releases and re-creates the internal framebuffer objects if the provided resolution changes.
	 * </ul>
	 * 
	 * @param parameters
	 * @param viewportWidth
	 * @param viewportHeight
	 * @return <ul>
	 * <li> GFSDK_SSAO_NULL_ARGUMENT                        - One of the required argument pointers is NULL
	 * <li> GFSDK_SSAO_GL_RESOURCE_CREATION_FAILED          - A GL resource-creation call has failed (running out of memory?)
	 * <li> GFSDK_SSAO_OK                                   - Success
	 * </ul>
	 */
	GFSDK_SSAO_Status preCreateFBOs(GFSDK_SSAO_Parameters parameters, int viewportWidth, int viewportHeight);
	
	/**
	 * [Optional] Gets the library-internal ZNear and ZFar values derived from the input projection matrix.<p>
	 * Remarks:<ul>
	 * <li> HBAO+ supports all perspective projection matrices, with arbitrary ZNear and ZFar.
	 * <li> For reverse infinite projections, GetProjectionMatrixDepthRange should return ZNear=+INF and ZFar=0.f.
	 * </ul>
	 * @param InputData
	 * @param OutputDepthRange
	 * @return <ul>
	 * <li> GFSDK_SSAO_NULL_ARGUMENT                        - One of the required argument pointers is NULL
	 * <li> GFSDK_SSAO_INVALID_PROJECTION_MATRIX            - The projection matrix is not valid
	 * <li> GFSDK_SSAO_OK                                   - Success
	 * </ul>
	 */
	GFSDK_SSAO_Status getProjectionMatrixDepthRange(GFSDK_SSAO_InputData_GL InputData, GFSDK_SSAO_ProjectionMatrixDepthRange OutputDepthRange);
	
	/** Releases all GL resources created by the library. */
	void dispose();
}
