package jet.opengl.demos.gpupro.clustered;

import com.nvidia.developer.opengl.app.NvSampleApp;

import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.util.DebugTools;

public class HybridRendererDemo extends NvSampleApp {

    @Override
    protected void initRendering() {
        CharSequence source = DebugTools.loadText("E:\\SDK\\HybridRenderingEngine\\assets\\shaders\\ComputeShaders\\clusterCullLightShader.comp");
        GLSLProgram program = new GLSLProgram();
        program.setSourceFromStrings((new ShaderSourceItem(source.toString(), ShaderType.COMPUTE)));

        program.printPrograminfo();

        getGLContext().requestExit();
    }
}
