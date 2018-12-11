package jet.opengl.demos.gpupro.vct;

import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.shader.GLSLProgram;

final class PointLight {
    final Vector3f position = new Vector3f();
    final Vector3f color = new Vector3f(1,1,1);

    void Upload(GLSLProgram program, int index)  {
        final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

        int idx = gl.glGetUniformLocation(program.getProgram(), ("pointLights[" + index + "].position"));
        if(idx >=0) gl.glUniform3f(idx, position.x, position.y, position.z);

        idx = gl.glGetUniformLocation(program.getProgram(), ("pointLights[" + index + "].color"));
        if(idx >=0) gl.glUniform3f(idx, color.x, color.y, color.z);
    }
}
