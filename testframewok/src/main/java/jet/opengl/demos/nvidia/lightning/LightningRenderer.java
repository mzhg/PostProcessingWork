package jet.opengl.demos.nvidia.lightning;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2i;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jet.opengl.demos.nvidia.rain.TransformFeedbackObject;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.OpenGLProgram;
import jet.opengl.postprocessing.shader.FullscreenProgram;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CachaRes;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.Recti;

/**
 * Created by mazhen'gui on 2017/8/28.
 */

final class LightningRenderer {
    static final int D3D10_VIEWPORT_AND_SCISSORRECT_OBJECT_COUNT_PER_PIPELINE = 1;
    static final int DecimationLevels = 2;				// number of downsampling steps

    static int BackBufferFormat = GLenum.GL_RGBA8;

    final Recti[] m_viewports = new Recti[D3D10_VIEWPORT_AND_SCISSORRECT_OBJECT_COUNT_PER_PIPELINE];

    static final int UNIFORM_PERFRAME = 0;
    static final int UNIFORM_LIGHT_APPEARANCE = 1;
    static final int UNIFORM_LIGHT_CHAIN = 3;
    static final int UNIFORM_LIGHT_STRUCTURE = 2;


//    ID3D10Device*	m_device;
//    ID3D10Effect*	m_effect;
    int m_back_buffer_sample_desc;

    GLSLProgram m_tech_bolt_out;
    GLSLProgram	m_tech_lines_out;

    GLSLProgram	m_tech_add_buffer;

    GLSLProgram	m_tech_blur_buffer_horizontal;
    GLSLProgram	m_tech_blur_buffer_vertical;

    BufferGL  m_constants_lightning_appearance;

    GLSLProgram	m_tech_down_sample_2x2;

    final Set<LightningSeed> m_lightning_seeds = new HashSet<>();
    int				m_max_vertices;
    private int m_subdivision_level;
    private boolean m_fork;
    private float m_animation_speed;
    private GLFuncProvider gl;
    private RenderTargets m_RenderTarget;
    private int m_read_fbo;
    private int m_write_fbo;
    private final PerFrame m_per_frame = new PerFrame();

    /*Effect::FloatVariable m_time;

    Effect::BoolVariable m_fork;
    Effect::IntVariable  m_subdivision_level;
    Effect::FloatVariable	m_animation_speed;
    Effect::FloatVariable	m_charge;

    Effect::MatrixVariable m_world;
    Effect::MatrixVariable m_view;
    Effect::MatrixVariable m_projection;
    Effect::MatrixVariable m_world_view;
    Effect::MatrixVariable m_world_view_projection;*/

    final Vector2i[] m_down_sample_buffer_sizes = new Vector2i[DecimationLevels];
    Texture2D[] m_down_sample_buffers;

    /*Effect::ShaderResourceVariable m_buffer;
    Effect::ShaderResourceVariable m_gradient;
    Effect::Vector2Variable	m_buffer_texel_size;
    Effect::Vector3Variable		m_blur_sigma;*/

    Texture2D	m_gradient_texture;
    Texture2D	m_gradient_texture_srv;

    Texture2D	m_original_lightning_buffer;

    Texture2D	m_lightning_buffer0;
    Texture2D	m_lightning_buffer1;

    Texture2D	m_small_lightning_buffer0;
    Texture2D	m_small_lightning_buffer1;


    Texture2D			m_scene_depth_stencil_view;
    Texture2D			m_scene_render_target_view;

    TransformFeedbackObject/*Geometry::SimpleVertexBuffer<SubdivideVertex>**/	m_subdivide_buffer0;
    TransformFeedbackObject/*Geometry::SimpleVertexBuffer<SubdivideVertex>**/	m_subdivide_buffer1;
    VertexArrayObject m_subdivide_layout;
    private BufferGL m_per_frame_buffer;
    private int m_dummy_vao;

    public LightningRenderer(int back_buffer_sample_desc ){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_RenderTarget = new RenderTargets();
        m_back_buffer_sample_desc = back_buffer_sample_desc;
        m_read_fbo = gl.glGenFramebuffer();
        m_write_fbo = gl.glGenFramebuffer();
        m_dummy_vao = gl.glGenVertexArray();

        for(int i = 0; i < m_down_sample_buffer_sizes.length; i++)
            m_down_sample_buffer_sizes[i] = new Vector2i();

//        m_tech_bolt_out(m_effect->GetTechniqueByName("BoltOut")),
//        m_tech_lines_out(m_effect->GetTechniqueByName("ShowLines")),
        m_tech_lines_out = PathLightning.createProgram("LinesOutVS.vert", "LinesOutGS.gemo", "LinesOutPS.frag", null, "LinesOut");
        m_tech_bolt_out = PathLightning.createProgram("LinesOutVS.vert", "BoltOutGS.gemo", "BoltOutPS.frag", null, "BoltOut");

//        m_tech_add_buffer(m_effect->GetTechniqueByName("AddBuffer")),
//        m_tech_blur_buffer_horizontal(m_effect->GetTechniqueByName("BlurBufferHorizontal")),
//        m_tech_blur_buffer_vertical(m_effect->GetTechniqueByName("BlurBufferVertical")),
        try {
            m_tech_blur_buffer_horizontal = GLSLProgram.createFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert",
                                            "nvidia/lightning/shaders/BlurBufferPS.frag");
            m_tech_blur_buffer_vertical = m_tech_blur_buffer_horizontal;
        } catch (IOException e) {
            e.printStackTrace();
        }

//        m_tech_down_sample_2x2(m_effect->GetTechniqueByName("DownSample2x2")),
        m_tech_add_buffer = m_tech_down_sample_2x2 = new FullscreenProgram(false);

//        m_constants_lightning_appearance(m_effect, "LightningAppearance"),
//        m_constants_lightning_structure(m_effect, "LightningStructure"),
        m_constants_lightning_appearance = new BufferGL();
        m_constants_lightning_appearance.initlize(GLenum.GL_UNIFORM_BUFFER, LightningAppearance.SIZE, null, GLenum.GL_STREAM_DRAW);
        m_per_frame_buffer = new BufferGL();
        m_per_frame_buffer.initlize(GLenum.GL_UNIFORM_BUFFER, PerFrame.SIZE, null, GLenum.GL_STREAM_DRAW);

//        m_world(m_effect,"world"),
//        m_view(m_effect,"view"),
//        m_projection(m_effect,"projection"),
//
//        m_world_view(m_effect,"world_view"),
//        m_world_view_projection(m_effect,"world_view_projection"),
//
//        m_time(m_effect,"time"),
//
//        m_fork(m_effect, "Fork"),
//        m_subdivision_level(m_effect,"SubdivisionLevel"),
//
//
//        m_animation_speed(m_effect,"AnimationSpeed"),
//        m_charge(m_effect,"Charge"),
//
//        m_buffer(m_effect, "buffer"),
//        m_gradient(m_effect, "gradient"),
//        m_buffer_texel_size(m_effect, "buffer_texel_size"),
//        m_blur_sigma(m_effect,"BlurSigma"),

        m_subdivide_buffer0 = null;
        m_subdivide_buffer1 = null;
        m_subdivide_layout = null;

        /*Texture2DDesc desc = new Texture2DDesc(BackBufferFormat, )
        m_lightning_buffer0(m_device,BackBufferFormat,back_buffer_sample_desc),
        m_lightning_buffer1(m_device,BackBufferFormat,back_buffer_sample_desc),

        m_small_lightning_buffer0(m_device,BackBufferFormat,Utility::SampleDesc(1,0)),
        m_small_lightning_buffer1(m_device,BackBufferFormat,Utility::SampleDesc(1,0)),

        m_original_lightning_buffer(m_device,BackBufferFormat,Utility::SampleDesc(1,0)),*/

        m_gradient_texture = null;
        m_gradient_texture_srv = null;

        m_scene_depth_stencil_view = null;
        m_scene_render_target_view = null;

        m_down_sample_buffers = new Texture2D[DecimationLevels -1];
        GLCheck.checkError();

        /*m_num_scissor_rects(0),
        m_num_viewports(0),
        m_max_vertices(0)*/

        for(int i = 0; i < m_viewports.length; i++){
            m_viewports[i] = new Recti();
        }
    }


    public PathLightning		CreatePathLightning(List<LightningPathSegment> segments, int pattern_mask, int subdivisions){
        PathLightning result = new PathLightning(segments, pattern_mask, subdivisions);
        AddLightningSeed(result);
        return result;
    }

    public ChainLightning		CreateChainLightning(int pattern_mask, int subdivisions){
        ChainLightning result = new ChainLightning( pattern_mask, subdivisions);
        AddLightningSeed(result);
        return result;
    }

    public void DestroyLightning(LightningSeed seed){
        RemoveLightningSeed(seed);
    }

    public void SetTime(float time){
        m_per_frame.time =time;
    }
    public void SetMatrices(Matrix4f world, Matrix4f view, Matrix4f projection){
        m_per_frame.world.load(world);
        m_per_frame.view.load(view);
        m_per_frame.projection.load(projection);

        Matrix4f.mul(view, world, m_per_frame.world_view);
        Matrix4f.mul(projection, m_per_frame.world_view, m_per_frame.world_view_projection);
    }

    public void OnRenderTargetResize(int width, int height, Texture2D render_target_view, Texture2D depth_stencil_view){
        if (m_lightning_buffer0 == null || m_lightning_buffer0.getWidth() != width || m_lightning_buffer0.getHeight() != height || m_lightning_buffer0.getSampleCount() != m_back_buffer_sample_desc) {

            Texture2DDesc desc = new Texture2DDesc(width, height, BackBufferFormat);
            desc.sampleCount = m_back_buffer_sample_desc;

            CommonUtil.safeRelease(m_lightning_buffer0);
            CommonUtil.safeRelease(m_lightning_buffer1);

            m_lightning_buffer0 = TextureUtils.createTexture2D(desc, null);
            m_lightning_buffer1 = TextureUtils.createTexture2D(desc, null);

            desc.sampleCount = 1;
            m_original_lightning_buffer = TextureUtils.createTexture2D(desc, null);

            BuildDownSampleBuffers(width, height);

            m_scene_depth_stencil_view = render_target_view;
            m_scene_render_target_view = depth_stencil_view;

//            m_buffer_texel_size =  D3DXVECTOR2(1.0f /width, 1.0f /height);
        }
    }

    public void Begin(){
//        m_device->ClearRenderTargetView(m_lightning_buffer0.RenderTargetView(),D3DXVECTOR4(0,0,0,0));
//        {
//            ID3D10RenderTargetView* views[] = {m_lightning_buffer0.RenderTargetView()};
//            m_device->OMSetRenderTargets(1, views, m_scene_depth_stencil_view);
//        }
        m_RenderTarget.bind();
        TextureGL[] views = {m_lightning_buffer0, m_scene_depth_stencil_view};
        m_RenderTarget.setRenderTextures(views, null);
        gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(0.0f, 0.0f,0.0f, 0.0f));
        gl.glViewport(0,0, m_lightning_buffer0.getWidth(), m_lightning_buffer0.getHeight());

        BuildSubdivisionBuffers();

        bindPerFrameBuffer();  // for the time variable
    }

    void bindPerFrameBuffer(){
        ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(PerFrame.SIZE);
        m_per_frame.store(bytes).flip();
        m_per_frame_buffer.update(0, bytes);
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, UNIFORM_PERFRAME, m_per_frame_buffer.getBuffer());
    }

    public void Render(LightningSeed seed, LightningAppearance appearance, float charge, float animation_speed, boolean as_lines, String debugStr){
        {
//            UINT offset[1] = {0};
//            ID3D10Buffer* zero[1] = {0};
//            m_device->SOSetTargets(1,zero,offset);
        }

//        m_charge = charge;
        m_animation_speed = animation_speed;
//        m_constants_lightning_appearance = appearance;
        ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(LightningAppearance.SIZE);
        appearance.store(bytes).flip();
        m_constants_lightning_appearance.update(0, bytes);
        m_constants_lightning_appearance.unbind();
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, UNIFORM_LIGHT_APPEARANCE, m_constants_lightning_appearance.getBuffer());

        seed.SetConstants();

        TransformFeedbackObject subdivided = Subdivide(seed, debugStr);

//        subdivided.BindToInputAssembler();
        m_subdivision_level = 0;

//        m_device->IASetInputLayout(m_subdivide_layout);
//        m_device->IASetPrimitiveTopology(D3D10_PRIMITIVE_TOPOLOGY_POINTLIST);

        GLSLProgram program;
        if(as_lines) {
            program = m_tech_lines_out;
            gl.glEnable(GLenum.GL_DEPTH_TEST);
            gl.glDisable(GLenum.GL_BLEND);
        }else {
            program = m_tech_bolt_out;
            gl.glEnable(GLenum.GL_DEPTH_TEST);
            gl.glEnable(GLenum.GL_BLEND);
            gl.glBlendFuncSeparate(GLenum.GL_ONE, GLenum.GL_ONE, GLenum.GL_ONE, GLenum.GL_ZERO);
        }

        program.enable();
//        m_device->Draw(seed->GetNumVertices(seed->GetSubdivisions()),0);
        int count = seed.GetNumVertices(seed.GetSubdivisions());
        subdivided.drawArrays(0, count);

        if(LightningDemo.canPrintLog()){
            System.out.print("LightningRenderer::Render::" + debugStr + "::Count::"+count + '\n');
            program.printPrograminfo();
        }

        GLCheck.checkError();
    }

    private void ResolveSubresource(Texture2D dest, Texture2D source){
        int old_fbo = gl.glGetInteger(GLenum.GL_FRAMEBUFFER_BINDING);
        gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, m_read_fbo);
        gl.glFramebufferTexture2D(GLenum.GL_READ_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, source.getTarget(), source.getTexture(), 0);
        gl.glReadBuffer(GLenum.GL_COLOR_ATTACHMENT0);

        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, m_write_fbo);
        gl.glFramebufferTexture2D(GLenum.GL_DRAW_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, dest.getTarget(), dest.getTexture(), 0);
        gl.glDrawBuffers(GLenum.GL_COLOR_ATTACHMENT0);

        gl.glBlitFramebuffer(0,0,source.getWidth(),source.getHeight(),
                0,0,dest.getWidth(),dest.getHeight(),
                GLenum.GL_COLOR_BUFFER_BIT, GLenum.GL_NEAREST);

        gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, 0);
        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, 0);
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, old_fbo);
        GLCheck.checkError();
    }

    public void End(boolean glow, Vector3f blur_sigma){
        gl.glDisable(GLenum.GL_DEPTH_TEST);

//        m_device->ResolveSubresource(m_original_lightning_buffer.Texture(),0,m_lightning_buffer0.Texture(),0,BackBufferFormat);
        ResolveSubresource(m_original_lightning_buffer, m_lightning_buffer0);
        if(glow)
        {
//            m_blur_sigma = blur_sigma;
            SaveViewports();

            DownSample(m_original_lightning_buffer);
            Vector2i last = m_down_sample_buffer_sizes[m_down_sample_buffer_sizes.length-1];
            ResizeViewport(last.x,last.y);
            RenderTargetPingPong ping_pong = new RenderTargetPingPong(m_small_lightning_buffer0, m_small_lightning_buffer1, null, m_RenderTarget);

            ping_pong.Apply(m_tech_blur_buffer_horizontal);
            ping_pong.Apply(m_tech_blur_buffer_vertical);

            RestoreViewports();
            GLCheck.checkError();
//            m_device->OMSetRenderTargets(1, &m_scene_render_target_view, m_scene_depth_stencil_view);
            if(m_scene_render_target_view == null && m_scene_depth_stencil_view == null){
                gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
            }else{
                m_RenderTarget.bind();
                m_RenderTarget.setRenderTextures(CommonUtil.toArray(m_scene_render_target_view, m_scene_depth_stencil_view), null);
            }

            Texture2D tex = ping_pong.LastTarget();
            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glBindTexture(tex.getTarget(), tex.getTexture());
            gl.glEnable(GLenum.GL_BLEND);
            gl.glBlendFuncSeparate(GLenum.GL_ONE, GLenum.GL_ONE, GLenum.GL_ZERO, GLenum.GL_ZERO);
            DrawQuad(m_tech_add_buffer);
        }
        else
        {
//            m_device->OMSetRenderTargets(1, &m_scene_render_target_view, m_scene_depth_stencil_view);
            if(m_scene_render_target_view == null && m_scene_depth_stencil_view == null){
                gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
            }else{
                m_RenderTarget.bind();
                m_RenderTarget.setRenderTextures(CommonUtil.toArray(m_scene_render_target_view, m_scene_depth_stencil_view), null);
            }
        }

//        m_buffer = m_original_lightning_buffer.ShaderResourceView();
        Texture2D tex = m_original_lightning_buffer;
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(tex.getTarget(), tex.getTexture());
        gl.glEnable(GLenum.GL_BLEND);
        gl.glBlendFuncSeparate(GLenum.GL_ONE, GLenum.GL_ONE, GLenum.GL_ONE, GLenum.GL_ZERO);
        DrawQuad(m_tech_add_buffer);
        GLCheck.checkError();
    }

    private void	BuildSubdivisionBuffers(){
        int max_segments = 0;

        for(LightningSeed it : m_lightning_seeds)
        {
            max_segments = Math.max(max_segments, it.GetMaxNumVertices());
        }

        if( (null != m_subdivide_buffer0) && (max_segments == m_subdivide_buffer0.getBufferSize(0)/SubdivideVertex.SIZE))
            return;

        CommonUtil.safeRelease(m_subdivide_buffer0);
        CommonUtil.safeRelease(m_subdivide_buffer1);
        System.out.println("max_segments = " + max_segments);

//        vector<SubdivideVertex> init_data(max_segments, SubdivideVertex());
//
//        D3D10_USAGE usage =  D3D10_USAGE_DEFAULT;
//        UINT flags = D3D10_BIND_VERTEX_BUFFER |  D3D10_BIND_STREAM_OUTPUT ;
//
//        m_subdivide_buffer0 = new Geometry::SimpleVertexBuffer<SubdivideVertex>(m_device,init_data,usage,flags);
//        m_subdivide_buffer1 = new Geometry::SimpleVertexBuffer<SubdivideVertex>(m_device,init_data,usage,flags);

        Runnable layout = ()->
        {
            gl.glEnableVertexAttribArray(0);
            gl.glVertexAttribPointer(0,3,GLenum.GL_FLOAT, false, SubdivideVertex.SIZE, 0);

            gl.glEnableVertexAttribArray(1);
            gl.glVertexAttribPointer(1,3,GLenum.GL_FLOAT, false, SubdivideVertex.SIZE, 12);

            gl.glEnableVertexAttribArray(2);
            gl.glVertexAttribPointer(2,3,GLenum.GL_FLOAT, false, SubdivideVertex.SIZE, 24);

            gl.glEnableVertexAttribArray(3);
            gl.glVertexAttribIPointer(3, 1, GLenum.GL_UNSIGNED_INT, SubdivideVertex.SIZE, 36);
        };

        m_subdivide_buffer0 = new TransformFeedbackObject(max_segments * SubdivideVertex.SIZE, layout);
        m_subdivide_buffer1 = new TransformFeedbackObject(max_segments * SubdivideVertex.SIZE, layout);
        GLCheck.checkError();
    }

    private void applySubdivideUniforms(OpenGLProgram program){
        if(program instanceof  SubdivideProgram) {
            SubdivideProgram _program = (SubdivideProgram) program;
            _program.setAnimationSpeed(m_animation_speed);
            _program.setFork(m_fork);
            _program.setSubdivisionLevel(m_subdivision_level);
        }
    }

    private TransformFeedbackObject		Subdivide(LightningSeed seed, String debugString){
        TransformFeedbackObject source = m_subdivide_buffer0;
        TransformFeedbackObject target = m_subdivide_buffer1;
        TransformFeedbackObject last_target = target;

//        m_device->IASetInputLayout(m_subdivide_layout);
//        m_device->IASetPrimitiveTopology(D3D10_PRIMITIVE_TOPOLOGY_POINTLIST);
//        m_subdivide_layout.bind();

//        target->BindToStreamOut();
        seed.GetFirstPassTechnique().enable();
        target.beginRecord(GLenum.GL_POINTS);

        m_subdivision_level = 0;
        m_fork = (seed.GetPatternMask() & (1 << 0))!=0;
        applySubdivideUniforms(seed.GetFirstPassTechnique());

        if(LightningDemo.canPrintLog())
            System.out.print(debugString + "::");

        seed.RenderFirstPass(false);
        {
//            UINT offset[1] = {0};
//            ID3D10Buffer* zero[1] = {0};
//            m_device->SOSetTargets(1,zero,offset);
            target.endRecord();
        }

        last_target = target;
//        swap(source,target);
        TransformFeedbackObject tmp = source;
        source = target;
        target = tmp;

        for(int i = 1; i < seed.GetSubdivisions(); ++i)
        {
//            source->BindToInputAssembler();
//            target->BindToStreamOut();

            seed.GetSubdivideTechnique().enable();
            target.beginRecord(GLenum.GL_POINTS);
            m_subdivision_level = i;
            m_fork = (seed.GetPatternMask() & (1 << i)) !=0;
            applySubdivideUniforms(seed.GetSubdivideTechnique());

//            seed.GetSubdivideTechnique()->GetPassByIndex(0)->Apply(0);
//            m_device->Draw( seed->GetNumVertices(i),0);
//            gl.glDrawArrays(GLenum.GL_POINTS, 0, seed.GetNumVertices(i));
            source.drawArrays(0, seed.GetNumVertices(i));
            {
//                UINT offset[1] = {0};
//                ID3D10Buffer* zero[1] = {0};
//                m_device->SOSetTargets(1,zero,offset);
                target.endRecord();
            }

            last_target = target;
//            swap(source,target);
            tmp = source;
            source = target;
            target = tmp;

            if(LightningDemo.canPrintLog()){
                System.out.print("LightningRenderer::Subdivide::" + debugString + "::" + i + ":\n");
                seed.GetSubdivideTechnique().printPrograminfo();
            }
        }

        {
//            UINT offset[1] = {0};
//            ID3D10Buffer* zero[1] = {0};
//            m_device->SOSetTargets(1,zero,offset);
//            m_device->IASetVertexBuffers(0,1,zero,offset,offset);

        }

        GLCheck.checkError();
        return last_target;
    }

    private void	BuildDownSampleBuffers(int w, int h){
        int width = w;
        int height = h;
        for(int i = 0; i < DecimationLevels; ++i)
        {
            width  >>= 1;
            height >>= 1;
//            SIZE s = {width, height};
            m_down_sample_buffer_sizes[i].set(width, height);
        }

        for(int i = 0; i < m_down_sample_buffers.length; ++i)
        {
            Vector2i s = m_down_sample_buffer_sizes[i];
            if(null == m_down_sample_buffers[i]) {
//                m_down_sample_buffers[i] = new Utility::ColorRenderBuffer
//                (m_device, s.cx, s.cy, BackBufferFormat, Utility::SampleDesc (1, 0));
                Texture2DDesc tex_desc = new Texture2DDesc(s.x, s.y, BackBufferFormat);
                m_down_sample_buffers[i] = TextureUtils.createTexture2D(tex_desc, null);
            }else{
                if(m_down_sample_buffers[i].getWidth() != s.x || m_down_sample_buffers[i].getHeight() != s.y){
                    m_down_sample_buffers[i].dispose();

                    Texture2DDesc tex_desc = new Texture2DDesc(s.x, s.y, BackBufferFormat);
                    m_down_sample_buffers[i] = TextureUtils.createTexture2D(tex_desc, null);
                }
            }
        }

        Vector2i last = m_down_sample_buffer_sizes[m_down_sample_buffer_sizes.length - 1];
        if(m_small_lightning_buffer0 ==null || m_small_lightning_buffer0.getWidth() != last.x || m_small_lightning_buffer0.getHeight() != last.y){
            CommonUtil.safeRelease(m_small_lightning_buffer0);
            CommonUtil.safeRelease(m_small_lightning_buffer1);

            Texture2DDesc tex_desc = new Texture2DDesc(last.x, last.y, BackBufferFormat);

            m_small_lightning_buffer0 = TextureUtils.createTexture2D(tex_desc, null);
            m_small_lightning_buffer1 = TextureUtils.createTexture2D(tex_desc, null);
        }

//        m_small_lightning_buffer0.Resize(m_down_sample_buffer_sizes.back().cx, m_down_sample_buffer_sizes.back().cy);
//        m_small_lightning_buffer1.Resize(m_down_sample_buffer_sizes.back().cx, m_down_sample_buffer_sizes.back().cy);
    }

    private void DownSample(Texture2D buffer){
//        vector<Utility::ColorRenderBuffer*> sources;
//        vector<Utility::ColorRenderBuffer*> targets;
//        sources.push_back(buffer);
//
//        copy(m_down_sample_buffers.begin(),m_down_sample_buffers.end(),back_inserter(sources));
//        copy(m_down_sample_buffers.begin(),m_down_sample_buffers.end(),back_inserter(targets));
//
//        targets.push_back(&m_small_lightning_buffer0);

        final Texture2D[] sources = new Texture2D[m_down_sample_buffers.length+1];
        final Texture2D[] targets = new Texture2D[m_down_sample_buffers.length+1];
        sources[0] = buffer;
//        System.arraycopy(m_down_sample_buffers, 0, sources, 1, m_down_sample_buffers.length);
//        System.arraycopy(m_down_sample_buffers, 0, targets, 0, m_down_sample_buffers.length);

        for(int i = 0; i < m_down_sample_buffers.length; i++){
            targets[i] = sources[i+1]= m_down_sample_buffers[i];
        }

        targets[m_down_sample_buffers.length] = m_small_lightning_buffer0;
        gl.glActiveTexture(GLenum.GL_TEXTURE0);

        for(int i = 0; i < sources.length; ++i)
        {
//            ID3D10RenderTargetView* view[] = { targets[i]->RenderTargetView()};
//            m_device->OMSetRenderTargets(1, const_cast<ID3D10RenderTargetView**> (view), 0);
            m_RenderTarget.bind();
            m_RenderTarget.setRenderTexture(targets[i], null);
            ResizeViewport(m_down_sample_buffer_sizes[i].x, m_down_sample_buffer_sizes[i].y );
            gl.glDisable(GLenum.GL_BLEND);

//            m_buffer = sources[i]->ShaderResourceView();
            gl.glBindTexture(sources[i].getTarget(), sources[i].getTexture());
            m_tech_down_sample_2x2.setName("Downsample");
            DrawQuad(m_tech_down_sample_2x2);
            if(LightningDemo.canPrintLog()){
                System.out.println("Downsample::" + i + "::");
                m_tech_down_sample_2x2.printPrograminfo();
            }
        }
    }

    private void	BuildGradientTexture(){
        int w = 512;
        int h = 512;
        final int mip_levels = 4;
//        D3D10_TEXTURE2D_DESC tex_desc = Utility::Texture2DDesc
//        (
//                w,
//                h,
//                mip_levels,
//                1,
//                DXGI_FORMAT_R8G8B8A8_UNORM,
//                Utility::SampleDesc(1,0),
//        D3D10_USAGE_DEFAULT,
//                D3D10_BIND_RENDER_TARGET | D3D10_BIND_SHADER_RESOURCE,
//                0,
//                D3D10_RESOURCE_MISC_GENERATE_MIPS
//        );
        Texture2DDesc tex_desc = new Texture2DDesc(w,h,GLenum.GL_RGBA8);
        tex_desc.mipLevels = (int) (Math.log(Math.max(w,h))/Math.log(2));

        byte[] data=new byte[4 * w * h];

        for(int y = 0; y < h; ++y)
        {
            int rowStart = y * w * 4;

            for(int x = 0; x < w; ++x)
            {
                float nx = x / (float)(w-1);
                float ny = y / (float)(h-1);

                float u = 2 * nx - 1;
                float v = 2 * ny - 1;

                double vv = Math.max(0,Math.min(1,1-Math.sqrt(u*u + v * v)));
                int value = (int)( Math.max(0,Math.min(255,vv * 255)));

                data[rowStart + x*4 + 0] = (byte) value;
                data[rowStart + x*4 + 1] = (byte) value;
                data[rowStart + x*4 + 2] = (byte) value;
                data[rowStart + x*4 + 3]  = (byte) 255;
            }
        }

//        D3D10_SUBRESOURCE_DATA sr[mip_levels];
//        for(unsigned int i = 0; i < mip_levels; ++i)
//        {
//            sr[i].pSysMem = &data[0];
//            sr[i].SysMemPitch = w * 4;
//            sr[i].SysMemSlicePitch = 4 * w * h;
//        }
//
//        HRESULT hr S_OK;
//        V(m_device->CreateTexture2D(&tex_desc,sr,&m_gradient_texture));
//        V(m_device->CreateShaderResourceView(m_gradient_texture,0,&m_gradient_texture_srv));
//
//        m_device->GenerateMips(m_gradient_texture_srv);
//        D3D10_TEXTURE2D_DESC result;
//        m_gradient_texture->GetDesc(&result);
        TextureDataDesc initData = new TextureDataDesc(GLenum.GL_RGBA, GLenum.GL_UNSIGNED_BYTE, data);
        m_gradient_texture_srv = m_gradient_texture = TextureUtils.createTexture2D(tex_desc, initData);
    }

    @CachaRes
    private void	SaveViewports(){
//        m_device->RSGetViewports(&m_num_viewports, 0);
//        m_device->RSGetViewports(&m_num_viewports, m_viewports);
//
//        m_device->RSGetScissorRects(&m_num_scissor_rects, 0);
//
//        if( 0 != m_num_scissor_rects)
//            m_device->RSGetScissorRects(&m_num_scissor_rects, m_scissor_rects);
        IntBuffer viewpoirt = CacheBuffer.getCachedIntBuffer(4);
        gl.glGetIntegerv(GLenum.GL_VIEWPORT, viewpoirt);
        m_viewports[0].set(viewpoirt.get(0), viewpoirt.get(1), viewpoirt.get(2), viewpoirt.get(3));
    }

    private void	ResizeViewport(int w, int h){
//        D3D10_VIEWPORT viewport = {0, 0, w, h, 0.0f, 1.0f};
//        D3D10_RECT	   scissor_rect = {0, 0, w, h};
//
//        m_device->RSSetViewports(1, &viewport);
//        m_device->RSSetScissorRects(1, &scissor_rect);
        gl.glViewport(0,0, w, h);
    }
    private void	RestoreViewports(){
//        m_device->RSSetViewports(m_num_viewports, m_viewports);
//        m_device->RSSetScissorRects(m_num_scissor_rects, m_scissor_rects);
        Recti viewpoirt = m_viewports[0];
        gl.glViewport(viewpoirt.x, viewpoirt.y, viewpoirt.width, viewpoirt.height);
    }

    private void	AddLightningSeed(LightningSeed seed){
        m_lightning_seeds.add(seed);
    }
    private void	RemoveLightningSeed(LightningSeed seed){
        m_lightning_seeds.remove(seed);
    }

    private void DrawQuad(GLSLProgram technique){
//        ID3D10Buffer* zero = 0;
//        UINT nought = 0;
//
//        m_device->IASetVertexBuffers(0,1,&zero,&nought,&nought);
//        m_device->IASetPrimitiveTopology(D3D10_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP);
//        m_device->IASetInputLayout(0);

//        for(UINT n = 0; n < Effect::NumPasses(technique); ++n)
        {
//            technique->GetPassByIndex(n)->Apply(0);
//            m_device->Draw(4,0);
            gl.glBindVertexArray(m_dummy_vao);
            technique.enable();
            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
            gl.glBindVertexArray(0);
        }
    }
}
