package jet.opengl.demos.intel.va;

/**
 * Created by mazhen'gui on 2017/11/18.
 */

public interface VaCameraControllerBase {

    void                                        CameraAttached( VaCameraBase camera );
    void                                        CameraTick( float deltaTime, VaCameraBase  camera, boolean hasFocus );
}
