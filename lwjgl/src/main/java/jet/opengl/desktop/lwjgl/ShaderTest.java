package jet.opengl.desktop.lwjgl;

import com.nvidia.developer.opengl.app.NvSampleApp;

import jet.opengl.postprocessing.common.GLCheck;
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

        getGLContext().requestExit();
    }
}
