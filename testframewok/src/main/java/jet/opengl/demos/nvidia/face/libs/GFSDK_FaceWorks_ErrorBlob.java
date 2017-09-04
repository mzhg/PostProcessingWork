package jet.opengl.demos.nvidia.face.libs;

/**
 * Error blob, for returning verbose error messages.<br>
 * Functions that take/return an error blob will allocate storage for the error message if
 * necessary, using the custom allocator if present, and fill in m_msg.<p>
 * Created by mazhen'gui on 2017/9/4.
 */

public class GFSDK_FaceWorks_ErrorBlob {
    public String m_msg;
}
