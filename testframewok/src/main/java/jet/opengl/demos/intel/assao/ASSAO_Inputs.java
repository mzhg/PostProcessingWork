package jet.opengl.demos.intel.assao;

import org.lwjgl.util.vector.Matrix4f;

class ASSAO_Inputs {

	// Output scissor rect - used to draw AO effect to a sub-rectangle, for example, for performance reasons when using wider-than-screen depth input to avoid close-to-border artifacts.
    int                             ScissorLeft;
    int                             ScissorTop;
    int                             ScissorRight;
    int                             ScissorBottom;

    // Custom viewports not supported yet; this is here for future support. ViewportWidth and ViewportHeight must match or be smaller than source depth and normalmap sizes.
    int                             ViewportX;
    int                             ViewportY;
    int                             ViewportWidth;
    int                             ViewportHeight;

    // Requires a projection matrix created with xxxPerspectiveFovLH or equivalent, not tested (yet) for right-handed
    // coordinates and will likely break for ortho projections.
    final Matrix4f                  ProjectionMatrix = new Matrix4f();

//#ifdef INTEL_SSAO_ENABLE_NORMAL_WORLD_TO_VIEW_CONVERSION
//    // In case normals are in world space, matrix used to convert them to viewspace
//    ASSAO_Float4x4                  NormalsWorldToViewspaceMatrix;
//#endif

    boolean                         MatricesRowMajorOrder;
    float                           CameraFar;
    float                           CameraNear;

    // Used for expanding UINT normals from [0, 1] to [-1, 1] if needed.
    float                           NormalsUnpackMul;
    float                           NormalsUnpackAdd;

    boolean                         DrawOpaque;

    ASSAO_Inputs( )
    {
        ScissorLeft                 = 0;
        ScissorTop                  = 0;
        ScissorRight                = 0;
        ScissorBottom               = 0;
        ViewportX                   = 0;
        ViewportY                   = 0;
        ViewportWidth               = 0;
        ViewportHeight              = 0;
        MatricesRowMajorOrder       = true;
        DrawOpaque                  = false;
        NormalsUnpackMul            = 2.0f;
        NormalsUnpackAdd            = -1.0f;
//#ifdef INTEL_SSAO_ENABLE_NORMAL_WORLD_TO_VIEW_CONVERSION
//        NormalsWorldToViewspaceMatrix.SetIdentity();
//#endif
    }
}
