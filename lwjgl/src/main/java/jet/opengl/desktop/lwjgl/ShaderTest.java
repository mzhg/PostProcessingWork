package jet.opengl.desktop.lwjgl;

import com.nvidia.developer.opengl.app.NvSampleApp;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.ShaderProgram;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.DebugTools;

/**
 * Created by mazhen'gui on 2017/12/28.
 */

final class ShaderTest extends NvSampleApp{

    static final int __format = 36385;

    @Override
    protected void initRendering() {
//        GLSLProgram program = GLSLProgram.createProgram("E:\\workspace\\StudioProjects\\android_opengl(studio)\\app\\src\\main\\assets\\fight404\\ParticleUpdateCS.comp", null);
        testActivityUniform();


        getGLContext().requestExit();
    }

    private void testActivityUniform(){
        GLSLProgram program = GLSLProgram.createProgram("Intel/AVSM/shaders/Default_MainVS.vert", "Intel/AVSM/shaders/Default_MainPS.frag", null);
//        ShaderProgram program = null;
//        try {
//            program = GLSLProgram.createShaderProgramFromFile("Intel/AVSM/shaders/Default_MainVS.vert", ShaderType.VERTEX);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        for(int i = 0; i < 2; i++){
            String name = gl.glGetActiveUniformBlockName(program.getProgram(), i);
            int idx = gl.glGetUniformBlockIndex(program.getProgram(), name);
            GLCheck.checkError();
            System.out.println(i + ": " + name + ", index = " + idx);
        }
    }

    private void testBinaryShader(){
        GLSLProgram program = new GLSLProgram();
        byte[] binary = DebugTools.loadBytes("E:/textures/binary_program.dat");
        program.setSourceFromBinary(binary, __format);
        GLCheck.checkError();
        program.printPrograminfo();
        program.enable();

        ShaderProgram shaderProgram = GLSLProgram.createShaderProgramFromBinary(CacheBuffer.wrap(binary), __format, GLenum.GL_COMPUTE_SHADER);
        System.out.println(shaderProgram.toString());
        shaderProgram.printPrograminfo();

//        int[] format = new int[1];
//        ByteBuffer bytes =  program.getProgramBinary(format);
        GLCheck.checkError();
//        byte[] content = new byte[bytes.remaining()];
//        bytes.get(content);

//        System.out.println("binaryformat: " + format[0]);
//        System.out.println(new String(content));

//        DebugTools.saveBinary(bytes, "E:/textures/binary_program.dat");
    }
}
