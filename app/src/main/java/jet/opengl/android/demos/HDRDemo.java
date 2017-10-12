package jet.opengl.android.demos;

import com.nvidia.developer.opengl.app.NvAppBase;

import jet.opengl.android.common.OpenGLBaseActivity;

/**
 * Created by mazhen'gui on 2017/10/12.
 */

public class HDRDemo extends OpenGLBaseActivity{
    @Override
    protected NvAppBase getRenderListener() {
        return new jet.opengl.demos.postprocessing.hdr.HDRDemo();
    }
}
