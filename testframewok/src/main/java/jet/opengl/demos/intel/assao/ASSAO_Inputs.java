package jet.opengl.demos.intel.assao;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Writable;

import java.nio.ByteBuffer;

class ASSAO_Inputs implements Readable, Writable{

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

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        buf.putInt(ScissorLeft);
        buf.putInt(ScissorTop);
        buf.putInt(ScissorRight);
        buf.putInt(ScissorBottom);

        buf.putInt(ViewportX);
        buf.putInt(ViewportY);
        buf.putInt(ViewportWidth);
        buf.putInt(ViewportHeight);

        ProjectionMatrix.store(buf);
        buf.put((byte) (MatricesRowMajorOrder ? 1 : 0));
//        buf.putFloat(CameraFar);
//        buf.putFloat(CameraNear);
        buf.putFloat(NormalsUnpackMul);
        buf.putFloat(NormalsUnpackAdd);
        buf.put((byte) (DrawOpaque ? 1 : 0));
        return buf;
    }

    @Override
    public Writable load(ByteBuffer buf) {
        ScissorLeft = buf.getInt();
        ScissorTop = buf.getInt();
        ScissorRight = buf.getInt();
        ScissorBottom = buf.getInt();

        ViewportX = buf.getInt();
        ViewportY = buf.getInt();
        ViewportWidth = buf.getInt();
        ViewportHeight = buf.getInt();

        ProjectionMatrix.load(buf);

        MatricesRowMajorOrder = buf.get() != 0;
        NormalsUnpackMul = buf.getFloat();
        NormalsUnpackAdd = buf.getFloat();

        DrawOpaque = buf.get() != 0;
        return this;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ASSAO_Inputs{");
        sb.append("\nScissorLeft=").append(ScissorLeft);
        sb.append(", \nScissorTop=").append(ScissorTop);
        sb.append(", \nScissorRight=").append(ScissorRight);
        sb.append(", \nScissorBottom=").append(ScissorBottom);
        sb.append(", \nViewportX=").append(ViewportX);
        sb.append(", \nViewportY=").append(ViewportY);
        sb.append(", \nViewportWidth=").append(ViewportWidth);
        sb.append(", \nViewportHeight=").append(ViewportHeight);
        sb.append(", \nProjectionMatrix=").append(ProjectionMatrix);
        sb.append(", \nMatricesRowMajorOrder=").append(MatricesRowMajorOrder);
        sb.append(", \nCameraFar=").append(CameraFar);
        sb.append(", \nCameraNear=").append(CameraNear);
        sb.append(", \nNormalsUnpackMul=").append(NormalsUnpackMul);
        sb.append(", \nNormalsUnpackAdd=").append(NormalsUnpackAdd);
        sb.append(", \nDrawOpaque=").append(DrawOpaque);
        sb.append('\n').append('}');
        return sb.toString();
    }
}
