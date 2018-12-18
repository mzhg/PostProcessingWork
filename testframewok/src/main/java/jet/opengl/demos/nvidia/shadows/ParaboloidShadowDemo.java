package jet.opengl.demos.nvidia.shadows;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.VisualDepthTextureProgram;
import jet.opengl.postprocessing.texture.AttachType;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
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
        float normalScale = 1;
    }

    private static final class FrameCB{
        static final int SIZE = Matrix4f.SIZE * 3 + Vector4f.SIZE * 2;

        final Matrix4f g_ViewProj = new Matrix4f();
        final Matrix4f g_Model = new Matrix4f();
        final Matrix4f g_LightViewProj = new Matrix4f();
        final Vector4f g_LightPos = new Vector4f();

        float g_LightZNear;
        float g_LightZFar;
        float g_NormScale;

        ByteBuffer wrap(ByteBuffer buffer){
            g_ViewProj.store(buffer);
            g_Model.store(buffer);
            g_LightViewProj.store(buffer);
            g_LightPos.store(buffer);

            buffer.putFloat(g_LightZNear);
            buffer.putFloat(g_LightZFar);
            buffer.putFloat(g_NormScale);
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
    private int m_psamDefault;

    private VisualDepthTextureProgram m_ShadowVisitor;
    private boolean m_visualShadow;
    private int m_shadowSlice;

    @Override
    public void initUI() {
        m_frameCB.g_LightPos.set(0,2, 0);
        mTweakBar.addValue("LightPosX:", createControl("x", m_frameCB.g_LightPos), -9.9f, +9.9f);
        mTweakBar.addValue("LightPosY:", createControl("y", m_frameCB.g_LightPos), -4.9f, +4.9f);
        mTweakBar.addValue("LightPosZ:", createControl("z", m_frameCB.g_LightPos), -4.9f, +4.9f);

        mTweakBar.addPadding();
        mTweakBar.addValue("Visual Shadow", createControl("m_visualShadow"));
        mTweakBar.addValue("Shadow Slice", createControl("m_shadowSlice"), 0, 1);
    }

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
            outScene.normalScale = -1;

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
                float scale = Numeric.random(0.8f, 1.4f);
                obj.transform.scale(scale, scale, scale);

                obj.color.x = Numeric.random(0.6f, 1.0f);
                obj.color.y = Numeric.random(0.6f, 1.0f);
                obj.color.z = Numeric.random(0.6f, 1.0f);

                instances.add(obj);
            }
        }

        m_frameBuffer = new BufferGL();
        m_frameBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, FrameCB.SIZE, null, GLenum.GL_DYNAMIC_READ);

        String root ="nvidia\\ParaboloidShadowDemo\\shaders\\";
        m_DepthGSProgram = GLSLProgram.createProgram(root+"ShadowVS.vert", root+"SceneGS.frag",   "Scenes\\Cube16\\shaders\\Dummy_PS.frag", null);
        m_RenderProgram = GLSLProgram.createProgram(root+"SceneVS.vert", root + "ScenePS.frag", null);

        Texture2DDesc desc = new Texture2DDesc(1024, 1024, GLenum.GL_DEPTH_COMPONENT32F);
        desc.arraySize = 2;

        m_ShadowMap = TextureUtils.createTexture2D(desc, null);
        m_FBO =new RenderTargets();

        m_cmpSampler = SamplerUtils.getDepthComparisonSampler();
        m_psamDefault = SamplerUtils.getDefaultSampler();

        GLCheck.checkError();
        m_transformer.setTranslation(0, -2, 0);
        m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);

        try {
            m_ShadowVisitor = new VisualDepthTextureProgram(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void display() {
        m_frameCB.g_LightZNear = 0.1f;
        m_frameCB.g_LightZFar = 20.f;
        m_frameCB.g_LightViewProj.setIdentity();
        m_frameCB.g_LightViewProj.m30 =-m_frameCB.g_LightPos.x;
        m_frameCB.g_LightViewProj.m31 =-m_frameCB.g_LightPos.y;
        m_frameCB.g_LightViewProj.m32 =-m_frameCB.g_LightPos.z;

        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDepthFunc(GLenum.GL_LESS);

        gl.glClearDepthf(1);
        gl.glClearColor(0,0,0,0);
        FloatBuffer ones = CacheBuffer.wrap(1.f,1.f, 1.f, 1.f);
//        gl.glClearTexImage(m_ShadowMap.getTexture(), 0, GLenum.GL_DEPTH_COMPONENT, GLenum.GL_FLOAT, ones);

        // generate the shadow map
        m_FBO.bind();
        TextureAttachDesc attachDesc = new TextureAttachDesc();
        attachDesc.type = AttachType.TEXTURE;
        m_FBO.setRenderTexture(m_ShadowMap, attachDesc);
        gl.glViewportIndexedf(0,0,0, m_ShadowMap.getWidth(), m_ShadowMap.getHeight());
        gl.glViewportIndexedf(1,0,0, m_ShadowMap.getWidth(), m_ShadowMap.getHeight());

        gl.glClear(GLenum.GL_DEPTH_BUFFER_BIT);
        GLCheck.checkError();
        renderScene(true);

        if(m_visualShadow){
            showShadownMap();
            return;
        }

        // Update the camera informations
        Matrix4f.perspective(60, (float)getGLContext().width()/getGLContext().height(), 0.1f, 50.f, m_proj);
        m_transformer.getModelViewMat(m_frameCB.g_ViewProj);
        Matrix4f.mul(m_proj, m_frameCB.g_ViewProj, m_frameCB.g_ViewProj);

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glViewport(0,0, getGLContext().width(), getGLContext().height());
        gl.glClear(GLenum.GL_DEPTH_BUFFER_BIT|GLenum.GL_COLOR_BUFFER_BIT);
        GLCheck.checkError();
        renderScene(false);

        gl.glBindTextureUnit(0, 0);
        gl.glBindSampler(0, 0);

        GLCheck.checkError();
    }

    private void renderScene(boolean isShaow){
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, m_frameBuffer.getBuffer());

        if(isShaow){
            m_DepthGSProgram.enable();
        }else{
            m_RenderProgram.enable();
            gl.glBindTextureUnit(0, m_ShadowMap.getTexture());
            gl.glBindSampler(0, m_cmpSampler);
        }
        for(Instance obj : instances){
            m_frameCB.g_Model.load(obj.transform);
            m_frameCB.g_NormScale = obj.normalScale;
            m_frameCB.g_LightPos.w = obj.normalScale;
            if(!isShaow){
                int index = m_RenderProgram.getUniformLocation("g_Color");
                if(index >= 0){
                    gl.glUniform4f(index, obj.color.x,obj.color.y, obj.color.z, 1);
                }
            }

            ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(FrameCB.SIZE);
            m_frameCB.wrap(buffer).flip();
            m_frameBuffer.update(0, buffer);

            obj.shape.bind();
            obj.shape.draw(GLenum.GL_TRIANGLES);
            obj.shape.unbind();
        }
    }

    private void showShadownMap() {
        Texture2D shadowMap = m_ShadowMap;
        VisualDepthTextureProgram visualProgram = m_ShadowVisitor;

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glViewport(0, 0, getGLContext().width(), getGLContext().height());
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glClearColor(0, 0, 0, 0);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);

        visualProgram.enable();
        visualProgram.setUniforms(m_frameCB.g_LightZNear, m_frameCB.g_LightZFar, m_shadowSlice, 1.0f);
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(shadowMap.getTarget(), shadowMap.getTexture());
        gl.glBindSampler(0, m_psamDefault);
        GLCheck.checkError();
        gl.glBindVertexArray(0);
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

        visualProgram.disable();
    }
}
