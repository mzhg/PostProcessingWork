package jet.opengl.desktop.lwjgl;

import com.nvidia.developer.opengl.app.NvSampleApp;

import jet.opengl.postprocessing.shader.GLSLProgram;

/**
 * Created by mazhen'gui on 2017/12/28.
 */

final class ShaderTest extends NvSampleApp{

    @Override
    protected void initRendering() {
        GLSLProgram program = GLSLProgram.createProgram("E:\\workspace\\StudioProjects\\android_opengl(studio)\\app\\src\\main\\assets\\fight404\\ParticleUpdateCS.comp", null);
        program.printPrograminfo();

        getGLContext().requestExit();
    }
}
