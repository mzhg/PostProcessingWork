package jet.opengl.desktop.lwjgl;

import com.nvidia.developer.opengl.app.NvSampleApp;

import java.io.IOException;
import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderProgram;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.DebugTools;
import jet.opengl.postprocessing.util.FileLoader;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.NvImage;

/**
 * Created by mazhen'gui on 2017/12/28.
 */

final class ShaderTest extends NvSampleApp{

    static final int __format = 36385;

    private GLSLProgram mGUIProg;
    private BufferGL uniformBuffer;
    private BufferGL vertex;
    private BufferGL indices;
    private Texture2D fontTex;

    private GLFuncProvider gl;
    private  int primiveCount;

    @Override
    protected void initRendering() {
//        GLSLProgram program = GLSLProgram.createProgram("E:\\workspace\\StudioProjects\\android_opengl(studio)\\app\\src\\main\\assets\\fight404\\ParticleUpdateCS.comp", null);
//        testActivityUniform();
        testDefualtVSShaders();

        int[] size = new int[1];
        final String root = "E:\\textures\\GameWork\\";
        mGUIProg = testShader();
        vertex = createBufferFromData(root + "Vertex.dat", size);
        indices = createBufferFromData(root + "Indice.dat", size);

        primiveCount = size[0]/2;

        uniformBuffer = new BufferGL();

        ByteBuffer uniformData = CacheBuffer.getCachedByteBuffer(32);
//        vec2 scale = 2.0/vec2(1920.0, 1055.0);
//	vec2 offset = vec2(-1);
        uniformData.putFloat(2f/1920f);
        uniformData.putFloat(-2f/1055f);
        uniformData.putFloat(-1f);
        uniformData.putFloat(1f);
        uniformData.putLong(0);
        uniformData.putLong(0);
        uniformData.flip();

        uniformBuffer.initlize(GLenum.GL_ARRAY_BUFFER, uniformData.remaining(), uniformData, GLenum.GL_STATIC_DRAW);

        ByteBuffer textureData = DebugTools.loadBinary(root+"fontTexture.data");
        ByteBuffer textureSize = DebugTools.loadBinary(root+"fontTextureDim.data");

        Texture2DDesc desc = new Texture2DDesc(textureSize.getInt(), textureSize.getInt(), GLenum.GL_R8);
        TextureDataDesc data = new TextureDataDesc(GLenum.GL_RED, GLenum.GL_UNSIGNED_BYTE, textureData);
        fontTex = TextureUtils.createTexture2D(desc, data);

        gl = GLFuncProviderFactory.getGLFuncProvider();
    }

    @Override
    public void display() {
        final int screenWidth = getGLContext().width();
        final int screenHeight = getGLContext().height();
        gl.glViewport(0,0, screenWidth, screenHeight);

        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_CULL_FACE);

        gl.glClearColor(0,0,0,0);
        gl.glClearDepthf(1.f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);

        mGUIProg.enable();
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, uniformBuffer.getBuffer());
        gl.glBindTextureUnit(0, fontTex.getTexture());

        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, vertex.getBuffer());
        gl.glVertexAttribPointer(0, 2, GLenum.GL_FLOAT, false, 20, 0);
        gl.glVertexAttribPointer(1, 2, GLenum.GL_FLOAT, false, 20, 8);
        gl.glVertexAttribPointer(2, 4, GLenum.GL_UNSIGNED_BYTE, true, 20, 16);

        gl.glEnableVertexAttribArray(0);
        gl.glEnableVertexAttribArray(1);
        gl.glEnableVertexAttribArray(2);

        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, indices.getBuffer());

        gl.glDrawElements(GLenum.GL_TRIANGLES, primiveCount, GLenum.GL_UNSIGNED_SHORT, 0);

        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER,0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
        gl.glDisableVertexAttribArray(0);
        gl.glDisableVertexAttribArray(1);
        gl.glDisableVertexAttribArray(2);
    }

    private GLSLProgram testShader(){
        FileLoader old = FileUtils.g_IntenalFileLoader;
        FileUtils.setIntenalFileLoader(FileLoader.g_DefaultFileLoader);
        final String root = "E:\\workspace\\VSProjects\\GraphicsWork\\Media\\Framework\\Shaders\\";
        GLSLProgram program = GLSLProgram.createProgram(root + "GuiVS.vert", root + "GuiPS.frag", null);
        program.printPrograminfo();

        System.out.println("gFont = " + program.getUniformLocation("gFont"));
        System.out.println("guiImage = " + program.getUniformLocation("guiImage"));

        String file = "E:\\workspace\\hg\\java\\lwjgl\\assets\\sky\\sky_cube.dds";
        try {
            NvImage.uploadTextureFromDDSFile(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        GLCheck.checkError();

        FileUtils.setIntenalFileLoader(old);

        return program;
    }

    private void testBreeveShaders(){
        FileLoader old = FileUtils.g_IntenalFileLoader;
        FileUtils.setIntenalFileLoader(FileLoader.g_DefaultFileLoader);

        Macro[] macros = {
          new Macro("GW_OGL4", 1),
        };
        GLSLProgram prog = GLSLProgram.createProgram("E:\\workspace\\VSProjects\\Breeze\\Media\\Framework\\Shaders\\SkinningCS.comp", macros);
        prog.printOnce();

        FileUtils.setIntenalFileLoader(old);
    }

    private void testDefualtVSShaders(){
        FileLoader old = FileUtils.g_IntenalFileLoader;
        FileUtils.setIntenalFileLoader(FileLoader.g_DefaultFileLoader);

        Macro[] macros = {
                new Macro("BINDLESS_TEXTURE_REQUIRED", 1),
                new Macro("GW_OGL4", 1),
        };
        GLSLProgram prog = GLSLProgram.createProgram("E:\\workspace\\VSProjects\\Breeze\\Media\\Shading\\DefaultVS.vert", null, macros);
        prog.printOnce();

        FileUtils.setIntenalFileLoader(old);
    }

    private void testActivityUniform(){
        GLSLProgram program = GLSLProgram.createProgram("Intel/AVSM/shaders/Default_MainVS.vert", "Intel/AVSM/shaders/Default_MainPS.frag", null);

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        for(int i = 0; i < 2; i++){
            String name = gl.glGetActiveUniformBlockName(program.getProgram(), i);
            int idx = gl.glGetUniformBlockIndex(program.getProgram(), name);
            GLCheck.checkError();
            System.out.println(i + ": " + name + ", index = " + idx);
        }
    }

    private BufferGL createBufferFromData(String filename, int[] size){
        ByteBuffer data = DebugTools.loadBinary(filename);
        size[0] = data.remaining();

        BufferGL buffer = new BufferGL();
        buffer.initlize(GLenum.GL_ARRAY_BUFFER, data.remaining(), data, GLenum.GL_STATIC_DRAW);

        return buffer;
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
