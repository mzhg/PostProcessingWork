package jet.opengl.demos.nvidia.sparkles;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.utils.NvImage;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.StringTokenizer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.FileUtils;

/**
 * Created by mazhen'gui on 2017/10/21.
 */
public class SparklesDemo extends NvSampleApp {
    private GLSLProgram sparklesRenderProgram;
    private SphereScene mScene;

    private BufferGL frame_buffer;
    private BufferGL ramdom_data;

    private GLFuncProvider gl;
    private final FrameData frameData = new FrameData();
    private Texture2D m_star_tex;

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();

        float[] values = loadRandomData();
        ramdom_data = new BufferGL();
        ramdom_data.initlize(GLenum.GL_UNIFORM_BUFFER, values.length * 4, CacheBuffer.wrap(values), GLenum.GL_STATIC_READ);

        frame_buffer = new BufferGL();
        frame_buffer.initlize(GLenum.GL_UNIFORM_BUFFER, FrameData.SIZE, null, GLenum.GL_DYNAMIC_READ);
        frame_buffer.unbind();

        try {
            int star_tex = NvImage.uploadTextureFromDDSFile("nvidia/sparkles/star.dds");
            m_star_tex = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, star_tex);
        } catch (IOException e) {
            e.printStackTrace();
        }

        sparklesRenderProgram = createSparklesRenderProgram();
        sparklesRenderProgram.printPrograminfo();

        mScene = new SphereScene();
        mScene.setNVApp(this);
        mScene.initScene();

        getGLContext().setSwapInterval(0);
    }

    @Override
    public void initUI() {
        mScene.onCreateUI(mTweakBar);
    }

    @Override
    public void display() {
        mScene.draw(true, true);
        GLCheck.checkError();

        mScene.resoveMultisampleTexture(GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT);
        GLCheck.checkError();

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 1, ramdom_data.getBuffer());

        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(mScene.getSceneDepthTex().getTarget(), mScene.getSceneDepthTex().getTexture());
        gl.glActiveTexture(GLenum.GL_TEXTURE1);
        gl.glBindTexture(m_star_tex.getTarget(), m_star_tex.getTexture());
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDepthMask(false);
        gl.glEnable(GLenum.GL_BLEND);
        gl.glBlendFuncSeparate(GLenum.GL_SRC_COLOR, GLenum.GL_ONE, GLenum.GL_ZERO, GLenum.GL_ONE);
        sparklesRenderProgram.enable();
        frameData.lightPos.set(mScene.getLightPos());

        // Two sphere need render

        mScene.getViews(frameData.world, true);
        frameData.viewProj.load(mScene.getSceneData().getViewProjMatrix());
        Matrix4f.getNormalMatrix(frameData.world, frameData.worldIT);
        Matrix4f.mul(frameData.viewProj, frameData.world, frameData.worldViewProj);
        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(FrameData.SIZE);
        frameData.store(buffer).flip();
        frame_buffer.update(0, buffer);
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, frame_buffer.getBuffer());
        mScene.getSphere().bind();
        mScene.getSphere().draw(GLenum.GL_TRIANGLES, 5);
        mScene.getSphere().unbind();

        mScene.getViews(frameData.world, false);
        frameData.viewProj.load(mScene.getSceneData().getViewProjMatrix());
        Matrix4f.getNormalMatrix(frameData.world, frameData.worldIT);
        Matrix4f.mul(frameData.viewProj, frameData.world, frameData.worldViewProj);
        buffer = CacheBuffer.getCachedByteBuffer(FrameData.SIZE);
        frameData.store(buffer).flip();
        frame_buffer.update(0, buffer);
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, frame_buffer.getBuffer());
        mScene.getSphere().bind();
        mScene.getSphere().draw(GLenum.GL_TRIANGLES, 5);
        mScene.getSphere().unbind();

        gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(mScene.getSceneDepthTex().getTarget(),0);
        gl.glDepthMask(true);
        gl.glDisable(GLenum.GL_BLEND);

        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 1, 0);
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, 0);

        GLCheck.checkError();
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <=0 || height <=0){
            return;
        }

        mScene.onResize(width, height);
        frameData.near_far_scrratio = (float)width/height;
    }

    @Override
    public void onDestroy() {
        mScene.dispose();
    }

    private static GLSLProgram createSparklesRenderProgram(){
        final String path = "nvidia/sparkles/";
        ShaderSourceItem vs_item = new ShaderSourceItem();
        ShaderSourceItem gs_item = null;
        ShaderSourceItem ps_item = null;

        try {
            vs_item.source = ShaderLoader.loadShaderFile(path + "SparklesVS.vert", false);
            vs_item.type = ShaderType.VERTEX;

            gs_item = new ShaderSourceItem();
            gs_item.source = ShaderLoader.loadShaderFile(path + "SparklesGS.gemo", false);
            gs_item.type = ShaderType.GEOMETRY;

            ps_item = new ShaderSourceItem();
            ps_item.source = ShaderLoader.loadShaderFile(path + "SparklesPS.frag", false);
            ps_item.type = ShaderType.FRAGMENT;
        } catch (IOException e) {
            e.printStackTrace();
        }

        GLSLProgram program = new GLSLProgram();
        program.setSourceFromStrings(vs_item, gs_item, ps_item);
        program.setName("SparklesRender");
        return program;
    }

    private static float[] loadRandomData(){
        final String path = "nvidia/sparkles/";
        try (BufferedReader in = new BufferedReader(new InputStreamReader(FileUtils.open(path + "random_barycentrics.txt")))){
            float[] values = new float[256*4];
            String line;
            int idx = 0;

            while ((line = in.readLine()) != null){
                StringTokenizer tokenizer = new StringTokenizer(line, "{}, \n");
                while (tokenizer.hasMoreElements()){
                    float f = Float.parseFloat(tokenizer.nextToken());
                    values[idx++] = f;
                    if(idx % 4 == 3){
                        idx++;
                    }
                }
            }

            return values;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static final class FrameData implements Readable{
        static final int SIZE = Matrix4f.SIZE * 4 + Vector4f.SIZE * 3;
        final Matrix4f worldViewProj = new Matrix4f();
        final Matrix4f viewProj = new Matrix4f();
        final Matrix4f world = new Matrix4f();
        final Matrix4f worldIT = new Matrix4f();
        final Vector3f eyePos = new Vector3f();
        float Shininess = 80.0f;

        float minSZ = 0.0f;
        float maxSZ = 0.3f;
        float LODScale = 30.0f;
        float near_far_scrratio;

//        final Vector3f lightPos = new Vector3f(20.0f, 4, -10.0f);
        final Vector3f lightPos = new Vector3f(5,5,10);

        @Override
        public ByteBuffer store(ByteBuffer buf) {
            worldViewProj.store(buf);
            viewProj.store(buf);
            world.store(buf);
            worldIT.store(buf);
            eyePos.store(buf);
            buf.putFloat(Shininess);
            buf.putFloat(minSZ);
            buf.putFloat(maxSZ);
            buf.putFloat(LODScale);
            buf.putFloat(near_far_scrratio);
            lightPos.store(buf);
            buf.putInt(0);
            return buf;
        }
    }
}
