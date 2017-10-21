package jet.opengl.demos.nvidia.lightning;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.core.OpenGLProgram;
import jet.opengl.postprocessing.shader.GLSLProgram;

/**
 * Created by mazhen'gui on 2017/10/16.
 */

final class SubdivideProgram implements OpenGLProgram{

    private GLSLProgram program;
    private int animationSpeedIdx;
    private int forkIdx;
    private int subdivisionLevelIdx;
    private GLFuncProvider gl;

    public SubdivideProgram(GLSLProgram program){
        this.program = program;

        animationSpeedIdx = program.getUniformLocation("AnimationSpeed");
        forkIdx = program.getUniformLocation("Fork");
        subdivisionLevelIdx = program.getUniformLocation("SubdivisionLevel");
        gl = GLFuncProviderFactory.getGLFuncProvider();
    }

    public void setAnimationSpeed(float speed){
        if(animationSpeedIdx >=0){
            gl.glUniform1f(animationSpeedIdx, speed);
        }
    }

    public void setFork(boolean flag){
        if(forkIdx >=0){
            gl.glUniform1i(forkIdx, flag?1:0);
        }
    }

    public void setSubdivisionLevel(int level){
        if(subdivisionLevelIdx >=0){
            gl.glUniform1ui(subdivisionLevelIdx, level);
        }
    }

    @Override
    public void dispose() {
        program.dispose();
    }

    @Override
    public int getProgram() {
        return program.getProgram();
    }

    @Override
    public void setName(String name) {
        program.setName(name);
    }

    @Override
    public String getName() {
        return program.getName();
    }
}
