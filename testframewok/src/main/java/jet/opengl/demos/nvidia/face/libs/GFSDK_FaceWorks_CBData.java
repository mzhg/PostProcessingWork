package jet.opengl.demos.nvidia.face.libs;

/**
 * Shared constant buffer<p></p>
 * Include this struct in your constant buffer; it provides data to the SSS and deep scatter APIs.
 * This structure matches the corresponding struct in GFSDK_FaceWorks.hlsli.<p></p>
 * Created by mazhen'gui on 2017/9/4.
 */

public class GFSDK_FaceWorks_CBData {
    /** The opaque data used to communicate with shaders */
    public final float[] data = new float[12];
}
