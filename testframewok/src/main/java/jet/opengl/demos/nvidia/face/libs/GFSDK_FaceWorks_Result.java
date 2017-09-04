package jet.opengl.demos.nvidia.face.libs;

/**
 * Created by mazhen'gui on 2017/9/4.
 */

public enum GFSDK_FaceWorks_Result {
    /** Everything ok */
    OK,				
    /** A required argument is NULL, or not in the valid range */
    InvalidArgument,
    /** Couldn't allocate memory */
    OutOfMemory,	
    /** Header version doesn't match DLL version */
    VersionMismatch,
}
