package jet.opengl.demos.nvidia.waves.test;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.models.DrawMode;
import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.QuadricBuilder;
import com.nvidia.developer.opengl.models.QuadricMesh;
import com.nvidia.developer.opengl.models.QuadricPlane;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/8/24.
 */

public class SumSinWaveDemo extends NvSampleApp {

    private GLVAO m_waterSurface;
    private GLSLProgram m_simulateProgram;
    private final Matrix4f m_model = new Matrix4f();
    private final Matrix4f m_proj = new Matrix4f();
    private int m_uniformBuffer;
    private final UniformBuffer m_constBuffer = new UniformBuffer();
    private GLFuncProvider gl;

    @Override
    protected void initRendering() {
        getGLContext().setSwapInterval(0);
        getGLContext().setAppTitle("SumSinWaveDemo");
        gl = GLFuncProviderFactory.getGLFuncProvider();

        final int planeSize = 256;
        QuadricBuilder builder = new QuadricBuilder();
        builder.setXSteps(planeSize).setYSteps(planeSize);
        builder.setPostionLocation(0);
        builder.setNormalLocation(1);
        builder.setTexCoordLocation(2);
        builder.setDrawMode(DrawMode.FILL);
        builder.setCenterToOrigin(true);

        m_waterSurface = new QuadricMesh(builder, new QuadricPlane(planeSize,planeSize)).getModel().genVAO();
        m_model.m30 -= planeSize/2;
        m_model.m32 -= planeSize/2;

        final String shader_path = "nvidia/WaveWorks/shaders/";
        m_simulateProgram = GLSLProgram.createProgram(shader_path + "SumSinWaveVS.vert", shader_path + "WavePS.frag", null);

        m_uniformBuffer = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, m_uniformBuffer);
        gl.glBufferData(GLenum.GL_UNIFORM_BUFFER, UniformBuffer.SIZE, GLenum.GL_STREAM_DRAW);
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, 0);

        m_transformer.setTranslation(-10, -20, -10);
        m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);

        m_constBuffer.params[0].amplitude = 0.5f;
        m_constBuffer.params[0].f = 5;
        m_constBuffer.params[0].k = 3f;
        m_constBuffer.params[0].w = 0.3f;
        m_constBuffer.params[0].dirX = 0.8f;
        m_constBuffer.params[0].dirY = 0.6f;

        m_constBuffer.params[1].amplitude = 0.45f;
        m_constBuffer.params[1].f = 4;
        m_constBuffer.params[1].k = 0.7f;
        m_constBuffer.params[1].w = 0.7f;
        m_constBuffer.params[1].dirX = 0.6f;
        m_constBuffer.params[1].dirY = 0.8f;

        m_constBuffer.params[2].amplitude = 0.4f;
        m_constBuffer.params[2].f = 4;
        m_constBuffer.params[2].k = 0.5f;
        m_constBuffer.params[2].w = 0.5f;
        m_constBuffer.params[2].dirX = 0.8f;
        m_constBuffer.params[2].dirY = 0.6f;

        m_constBuffer.params[3].amplitude = 0.35f;
        m_constBuffer.params[3].f = 3;
        m_constBuffer.params[3].k = 0.3f;
        m_constBuffer.params[3].w = 0.3f;
        m_constBuffer.params[3].dirX = 0.6f;
        m_constBuffer.params[3].dirY = 0.8f;
    }

    @Override
    public void display() {
        gl.glViewport(0,0, getGLContext().width(), getGLContext().height());
        gl.glClearColor(0,0,0,1);
        gl.glClearDepthf(1.f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);

        Matrix4f modelViewProj = m_constBuffer.modelViewProj;
        m_transformer.getModelViewMat(modelViewProj);
        Matrix4f.mul(modelViewProj, m_model, modelViewProj);
        Matrix4f.mul(m_proj, modelViewProj, modelViewProj);
        m_constBuffer.time += getFrameDeltaTime();

        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(UniformBuffer.SIZE);
        m_constBuffer.store(buffer).flip();

        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, m_uniformBuffer);
        gl.glBufferSubData(GLenum.GL_UNIFORM_BUFFER, 0, buffer);
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, 0);
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, m_uniformBuffer);

        gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_LINE);
        m_waterSurface.bind();
        m_simulateProgram.enable();
        m_waterSurface.draw(GLenum.GL_TRIANGLES);
        m_waterSurface.unbind();
        gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <=0 || height <= 0)
            return;

        Matrix4f.perspective(60, (float)width/height, 0.1f, 1000.f, m_proj);
    }

    private static final class WaveParameters implements Readable{
        final static int SIZE = Vector4f.SIZE * 2;
        float amplitude;
        float f; // power.
        float k; // angular wave number
        float w; //  angular frequency
        float dirX;  // dir.xy: direction; dir.zw: unused
        float dirY;  // dir.xy: direction; dir.zw: unused

        @Override
        public ByteBuffer store(ByteBuffer buf){
            buf.putFloat(amplitude);
            buf.putFloat(f);
            buf.putFloat(k);
            buf.putFloat(w);
            buf.putFloat(dirX);
            buf.putFloat(dirY);
            buf.putFloat(0);
            buf.putFloat(0);

            return buf;
        }
    }

    private static final class UniformBuffer{
        final static int SIZE = WaveParameters.SIZE * 4 + Matrix4f.SIZE + Vector4f.SIZE;
        final WaveParameters[] params = new WaveParameters[4];
        final Matrix4f modelViewProj = new Matrix4f();
        float time;

        UniformBuffer(){
            for(int i = 0; i < params.length; i++){
                params[i] = new WaveParameters();
            }
        }

        ByteBuffer store(ByteBuffer buf){
            CacheBuffer.put(buf, params);
            modelViewProj.store(buf);
            buf.putFloat(time);
            buf.putFloat(0);
            buf.putFloat(0);
            buf.putFloat(0);

            return buf;
        }
    }
}
