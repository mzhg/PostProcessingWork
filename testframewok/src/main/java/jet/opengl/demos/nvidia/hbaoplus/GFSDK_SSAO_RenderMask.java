package jet.opengl.demos.nvidia.hbaoplus;

public interface GFSDK_SSAO_RenderMask {

	int 
			GFSDK_SSAO_DRAW_Z                              = (1 << 0),  // Linearize the input depths
		    GFSDK_SSAO_DRAW_AO                             = (1 << 1),  // Render AO based on pre-linearized depths
		    GFSDK_SSAO_DRAW_DEBUG_N                        = (1 << 2),  // Render the internal view normals (for debugging)
		    GFSDK_SSAO_DRAW_DEBUG_X                        = (1 << 3),  // Render the X component as grayscale
		    GFSDK_SSAO_DRAW_DEBUG_Y                        = (1 << 4),  // Render the Y component as grayscale
		    GFSDK_SSAO_DRAW_DEBUG_Z                        = (1 << 5),  // Render the Z component as grayscale
		    GFSDK_SSAO_RENDER_AO                           = GFSDK_SSAO_DRAW_Z | GFSDK_SSAO_DRAW_AO,
		    GFSDK_SSAO_RENDER_DEBUG_NORMAL                 = GFSDK_SSAO_DRAW_Z | GFSDK_SSAO_DRAW_DEBUG_N,
		    GFSDK_SSAO_RENDER_DEBUG_NORMAL_X               = GFSDK_SSAO_DRAW_Z | GFSDK_SSAO_DRAW_DEBUG_N | GFSDK_SSAO_DRAW_DEBUG_X,
		    GFSDK_SSAO_RENDER_DEBUG_NORMAL_Y               = GFSDK_SSAO_DRAW_Z | GFSDK_SSAO_DRAW_DEBUG_N | GFSDK_SSAO_DRAW_DEBUG_Y,
		    GFSDK_SSAO_RENDER_DEBUG_NORMAL_Z               = GFSDK_SSAO_DRAW_Z | GFSDK_SSAO_DRAW_DEBUG_N | GFSDK_SSAO_DRAW_DEBUG_Z;
}
