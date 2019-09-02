package jet.opengl.demos.gpupro.vct;

import com.nvidia.developer.opengl.app.NvSampleApp;

import jet.opengl.postprocessing.common.GLCheck;

public class VoxelConeTracingDemo extends NvSampleApp {
    /// <summary> The scene to update and render. </summary>
    Scene  scene;

    /// <summary> The graphical context that is used for rendering the current scene. </summary>
    VoxelConeTracingRenderer graphics;

    @Override
    protected void initRendering() {
        scene = new GlassScene(getInputTransformer());
        scene.onActive();

        graphics = new VoxelConeTracingRenderer();
        graphics.onCreate();
    }

    @Override
    public void display() {
        scene.update(getFrameDeltaTime());

        graphics.render(scene, getGLContext().width(), getGLContext().height(),
                VoxelConeTracingRenderer.RenderingMode.VOXEL_CONE_TRACING);

        GLCheck.checkError();
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <=0){
            return;
        }

        scene.onResize(width, height);
        graphics.onResize(width, height);
    }
}
