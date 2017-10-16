package jet.opengl.demos.nvidia.lightning;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/8/30.
 */

final class Scene implements Disposeable{
    static final int NumTargets = 4;
    private final PerFrame m_per_frame = new PerFrame();

//    mutable ID3D10Device*		m_device;
//    mutable ID3D10Effect*		m_effect;

    private GLSLProgram m_tech_scene;
    private BufferGL    m_per_frame_buffer;

    /*mutable Effect::FloatVariable m_time;

    mutable Effect::MatrixVariable m_world;

    mutable Effect::MatrixVariable m_view;
    mutable Effect::MatrixVariable m_projection;

    mutable Effect::MatrixVariable m_world_view;
    mutable Effect::MatrixVariable m_world_view_projection;

    mutable Effect::ShaderResourceVariable m_tex_diffuse;*/


    private final Vector4f[] m_target_positions = new Vector4f[NumTargets];
    private final Matrix4f[] m_target_transforms = new Matrix4f[NumTargets];

    private XMesh			m_scene_mesh;
    private XMesh			m_target_mesh;
    private boolean         m_printOnce;

    private Texture2D       m_sceneColorTex;
    private Texture2D       m_sceneDepthTex;
    private int             m_sceneFBO;
    private GLFuncProvider  gl;

//    private VertexArrayObject m_scene_layout;

    Scene(){
        m_tech_scene = LightningSeed.createProgram("SceneVS.vert", null, "ScenePS.frag", null);
        m_tech_scene.setName("SceneRender");
        final String shader_path = "nvidia/lightning/shaders/";
        final String model_path = "nvidia/lightning/models/";
        final String textures_path = "nvidia/lightning/textures/";
        String[] scene_textures = {
                textures_path+"mud.jpg",
                textures_path+"wood.jpg",
                textures_path+"stone_wall.jpg",
                textures_path+"stone_wall.jpg",
                textures_path+"cloth.jpg",
                textures_path+"hieroglyphics.jpg",
                textures_path+"stone.jpg",
                textures_path+"brushed_gold.jpg",
                textures_path+"bark.jpg",
                textures_path+"wood.jpg",
                textures_path+"brushed_gold.jpg",
                textures_path+"stone_wall.jpg"
        };
        m_scene_mesh = new XMesh(model_path + "Scene", 12, scene_textures);

        String[] chain_textures = {textures_path + "mud.jpg"};
        m_target_mesh = new XMesh(model_path+"chain_target",1, chain_textures);

        for(int i = 0; i < NumTargets; i++){
            m_target_positions[i] = new Vector4f();
            m_target_transforms[i] = new Matrix4f();
        }

        m_per_frame_buffer = new BufferGL();
        m_per_frame_buffer.initlize(GLenum.GL_UNIFORM_BUFFER, PerFrame.SIZE, null, GLenum.GL_DYNAMIC_COPY);

        gl = GLFuncProviderFactory.getGLFuncProvider();
    }

    void reshape(int width, int height){
        if(m_sceneColorTex == null || m_sceneColorTex.getWidth() != width || m_sceneColorTex.getHeight() != height){
            CommonUtil.safeRelease(m_sceneColorTex);
            CommonUtil.safeRelease(m_sceneDepthTex);

            Texture2DDesc tex_desc = new Texture2DDesc(width, height, GLenum.GL_RGBA8);
            m_sceneColorTex = TextureUtils.createTexture2D(tex_desc, null);
            tex_desc.format = GLenum.GL_DEPTH_COMPONENT16;
            m_sceneDepthTex  = TextureUtils.createTexture2D(tex_desc, null);

            if(m_sceneFBO == 0){
                m_sceneFBO = gl.glGenFramebuffer();
            }
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_sceneFBO);
            gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, m_sceneColorTex.getTarget(), m_sceneColorTex.getTexture(), 0);
            gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_ATTACHMENT, m_sceneDepthTex.getTarget(), m_sceneDepthTex.getTexture(), 0);
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        }
    }

    Texture2D getSceneColorTex() { return m_sceneColorTex;}
    Texture2D getSceneDepthTex() { return m_sceneDepthTex;}

    private void RenderTarget(Matrix4f transform){
        ShaderMatrices(transform,m_per_frame.view, m_per_frame.projection);
        m_tech_scene.enable();
        bindbuffer();
        m_target_mesh.draw( /*m_device, m_tech_scene, m_tex_diffuse*/ );
    }

    private void ShaderMatrices(Matrix4f world, Matrix4f view, Matrix4f projection){
        m_per_frame.world.load(world);
        m_per_frame.view.load(view);
        m_per_frame.projection.load(projection);

        Matrix4f.mul(view, world, m_per_frame.world_view);
        Matrix4f.mul(projection, m_per_frame.world_view, m_per_frame.world_view_projection);
    }

    void Time(float time){
        m_per_frame.time = time;

        float s[/*NumTargets*/] = {1.00f, -0.5f, 0.5f, 2.0f};
        float r[/*NumTargets*/] = {7.0f, 9.0f, 11.0f, 13.0f};
        float d[/*NumTargets*/] = {5.0f, 10.0f, 15.0f, 20.0f};


        for(int i = 0 ; i < NumTargets; ++i)
        {
            float angle = s[i] * time;

            m_target_positions[i].set( (float)(r[i] * Math.sin(angle)),d[i], (float)(r[i] * Math.cos(angle)),1);

//            D3DXMATRIX rotation;
//            D3DXMatrixRotationY(&rotation,angle);
//
//            D3DXMATRIX translation;
//            D3DXMatrixTranslation(&translation,0,d[i],r[i]);
//
//            D3DXMATRIX tumble;
//
            float t_a = 10 * time;
//            D3DXVECTOR3 axis(sin(t_a), cos(t_a), -sin(t_a));
//            D3DXMatrixRotationAxis(&tumble,&axis, angle);
//
//            m_target_transforms[i] = tumble * translation * rotation  ;
            m_target_transforms[i].setIdentity();
            m_target_transforms[i].rotate(angle, Vector3f.Y_AXIS);
            m_target_transforms[i].translate(0,d[i],r[i]);
            m_target_transforms[i].rotate(angle, (float)Math.sin(t_a), (float)Math.cos(t_a), -(float)Math.sin(t_a));
        }
    }
    void Matrices(Matrix4f world, Matrix4f view, Matrix4f projection){
        ShaderMatrices(world, view,projection);
    }

    void bindbuffer(){
        ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(PerFrame.SIZE);
        m_per_frame.store(bytes).flip();
        m_per_frame_buffer.update(0, bytes);
        GLFuncProviderFactory.getGLFuncProvider().glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, m_per_frame_buffer.getBuffer());
    }

    void Render(){
//        m_device->IASetInputLayout(m_scene_layout);
//        m_device->IASetPrimitiveTopology(D3D10_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_sceneFBO);
        gl.glViewport(0,0,m_sceneColorTex.getWidth(), m_sceneColorTex.getHeight());
        gl.glClearColor(0.0f, 0.0f, 0.3f, 1.0f);
        gl.glClearDepthf(1.0f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);

        gl.glDisable(GLenum.GL_BLEND);
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDepthFunc(GLenum.GL_LESS);

        m_tech_scene.enable();
        Matrices(Matrix4f.IDENTITY, m_per_frame.view, m_per_frame.projection);
        bindbuffer();

        m_scene_mesh.draw( /*m_device, m_tech_scene, m_tex_diffuse*/ );

        for(int i = 0; i < NumTargets; ++i)
            RenderTarget(m_target_transforms[i]);
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);

        GLCheck.checkError();

        if(!m_printOnce){
            m_tech_scene.printPrograminfo();
            m_printOnce = true;
        }
    }

     void resoveSceneTex(){
         Texture2D source = m_sceneColorTex;
        gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, m_sceneFBO);
        gl.glFramebufferTexture2D(GLenum.GL_READ_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, source.getTarget(), source.getTexture(), 0);
        gl.glReadBuffer(GLenum.GL_COLOR_ATTACHMENT0);

        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, 0);
        gl.glBlitFramebuffer(0,0,source.getWidth(),source.getHeight(),
                0,0,source.getWidth(),source.getHeight(),
                GLenum.GL_COLOR_BUFFER_BIT, GLenum.GL_NEAREST);

        gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, 0);
        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, 0);
        GLCheck.checkError();
    }

    private final Vector4f m_default_pos = new Vector4f(0,0,0,1);
    Vector4f TargetPosition( int which){
        if(which < NumTargets)
        {
            return m_target_positions[which];
        }
        else
        {
            return m_default_pos;
        }
    }

    @Override
    public void dispose() {
        CommonUtil.safeRelease(m_tech_scene);
        CommonUtil.safeRelease(m_per_frame_buffer);
        CommonUtil.safeRelease(m_scene_mesh);
        CommonUtil.safeRelease(m_target_mesh);
    }
}
