package jet.opengl.demos.nvidia.shadows;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.models.DrawMode;
import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.Model;
import com.nvidia.developer.opengl.models.ModelGenerator;
import com.nvidia.developer.opengl.models.QuadricBuilder;
import com.nvidia.developer.opengl.models.QuadricMesh;
import com.nvidia.developer.opengl.models.QuadricSphere;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

public class ParaboloidShadowDemo extends NvSampleApp {

    private GLVAO m_sphere;
    private GLVAO m_cube;

    private static final class Instance{
        GLVAO shape;
        final Matrix4f transform = new Matrix4f();
        Vector3f color = new Vector3f();
    }

    private static final class FrameCB{
        static final int SIZE = Matrix4f.SIZE * 3 + Vector4f.SIZE * 2;

        final Matrix4f g_ViewProj = new Matrix4f();
        final Matrix4f g_Model = new Matrix4f();
        final Matrix4f g_LightViewProj = new Matrix4f();
        final Vector4f g_LightPos = new Vector4f();

        float g_LightZNear;
        float g_LightZFar;

        ByteBuffer wrap(ByteBuffer buffer){
            g_ViewProj.store(buffer);
            g_Model.store(buffer);
            g_LightViewProj.store(buffer);
            g_LightPos.store(buffer);

            buffer.putFloat(g_LightZNear);
            buffer.putFloat(g_LightZFar);
            buffer.putFloat(0);
            buffer.putFloat(0);

            return buffer;
        }
    }

    private final Matrix4f m_proj = new Matrix4f();

    private List<Instance> instances = new ArrayList<>();
    private FrameCB m_frameCB = new FrameCB();
    private BufferGL m_frameBuffer;

    private GLSLProgram m_DepthGSProgram;
    private GLSLProgram m_RenderProgram;

    private Texture2D m_ShadowMap;
    private RenderTargets m_FBO;

    private GLFuncProvider gl;
    private int m_cmpSampler;

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();

        QuadricBuilder builder = new QuadricBuilder();
        builder.setXSteps(50).setYSteps(50);
        builder.setPostionLocation(0);
        builder.setNormalLocation(1);
        builder.setGenTexCoord(false);
        builder.setAutoGenTexCoord(false);
        builder.setDrawMode(DrawMode.FILL);
        builder.setCenterToOrigin(true);
        Model sphere = new QuadricMesh(builder, new QuadricSphere(1)).getModel();

        m_sphere = sphere.genVAO();
        m_cube = ModelGenerator.genCube(1, true, false,false).genVAO();

        {
            Instance outScene = new Instance();
            outScene.color.set(0.7f, 0.7f, 0.7f);
            outScene.shape = m_cube;
            outScene.transform.setTranslate(0, 5, 0);
            outScene.transform.scale(20,10,10);

            instances.add(outScene);
        }

        {
            for(int i = 0; i < 8; i++){
                Instance obj = new Instance();
                float z,x;
                if( (i%2) == 0){
                    obj.shape = m_sphere;
                }else{
                    obj.shape = m_cube;
                }

                int col = i/4;
                int row = i%4;

                z = -2.5f + 5 * col;
                x = -7.5f + 5 * row;
                obj.transform.setTranslate(x, 0.5f, z);
                obj.color.x = Numeric.random(0.6f, 1.0f);
                obj.color.y = Numeric.random(0.6f, 1.0f);
                obj.color.z = Numeric.random(0.6f, 1.0f);

                instances.add(obj);
            }
        }

        m_frameBuffer = new BufferGL();
        m_frameBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, FrameCB.SIZE, null, GLenum.GL_DYNAMIC_READ);

        String root ="nvidia\\ParaboloidShadowDemo\\shaders\\";
        m_DepthGSProgram = GLSLProgram.createProgram(root+"SceneVS.vert", root+"SceneGS.frag",   "Scenes\\Cube16\\shaders\\Dummy_PS.frag", null);
        m_RenderProgram = GLSLProgram.createProgram(root+"SceneVS.vert", root + "ScenePS.frag", null);

        Texture2DDesc desc = new Texture2DDesc(1024, 1024, GLenum.GL_DEPTH_COMPONENT32F);
        desc.arraySize = 2;

        m_ShadowMap = TextureUtils.createTexture2D(desc, null);
        m_FBO =new RenderTargets();

        m_cmpSampler = SamplerUtils.getDepthComparisonSampler();
    }

    @Override
    public void display() {
        m_frameCB.g_LightZNear = 0.1f;
        m_frameCB.g_LightZFar = 50.f;

        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDepthFunc(GLenum.GL_LESS);

        gl.glClearDepthf(1);
        gl.glClearColor(0,0,0,0);

        // generate the shadow map
        m_FBO.bind();
        m_FBO.setRenderTexture(m_ShadowMap, null);
        gl.glViewportIndexedf(0,0,0, m_ShadowMap.getWidth(), m_ShadowMap.getHeight());
        gl.glViewportIndexedf(1,0,0, m_ShadowMap.getWidth(), m_ShadowMap.getHeight());

        gl.glClear(GLenum.GL_DEPTH_BUFFER_BIT);

        renderScene(true);

        // Update the camera informations
        Matrix4f.perspective(60, (float)getGLContext().width()/getGLContext().height(), 0.1f, 50.f, m_proj);
        m_transformer.getModelViewMat(m_frameCB.g_ViewProj);
        Matrix4f.mul(m_proj, m_frameCB.g_ViewProj, m_frameCB.g_ViewProj);

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glViewport(0,0, m_ShadowMap.getWidth(), m_ShadowMap.getHeight());
        gl.glClear(GLenum.GL_DEPTH_BUFFER_BIT|GLenum.GL_COLOR_BUFFER_BIT);

        renderScene(false);

        gl.glBindTextureUnit(0, 0);
        gl.glBindSampler(0, 0);
    }

    private void renderScene(boolean isShaow){
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, m_frameBuffer.getBuffer());
        if(isShaow){
            m_frameCB.g_ViewProj.setIdentity();
        }

        if(isShaow){
            m_DepthGSProgram.enable();
        }else{
            m_RenderProgram.enable();
            gl.glBindTextureUnit(0, m_ShadowMap.getTexture());
            gl.glBindSampler(0, m_cmpSampler);
        }
        for(Instance obj : instances){
            m_frameCB.g_Model.load(obj.transform);
            if(!isShaow){
                int index = m_RenderProgram.getUniformLocation("g_Color");
                if(index >= 0){
                    gl.glUniform4f(index, obj.color.x,obj.color.y, obj.color.z, 1);
                }

                ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(FrameCB.SIZE);
                m_frameCB.wrap(buffer).flip();
                m_frameBuffer.update(0, buffer);

                obj.shape.draw(GLenum.GL_TRIANGLES);
            }
        }
    }
}
