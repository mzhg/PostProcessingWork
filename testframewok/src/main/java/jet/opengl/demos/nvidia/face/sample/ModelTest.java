package jet.opengl.demos.nvidia.face.sample;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.models.obj.NvGLModel;

public class ModelTest extends NvSampleApp {

    private NvGLModel m_model;

    @Override
    protected void initRendering() {
        m_model = new NvGLModel();
    }
}
