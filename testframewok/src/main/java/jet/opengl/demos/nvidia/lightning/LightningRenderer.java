package jet.opengl.demos.nvidia.lightning;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.Recti;
import jet.opengl.postprocessing.util.StackInt;

/**
 * Created by mazhen'gui on 2017/8/28.
 */

final class LightningRenderer {
    static final int D3D10_VIEWPORT_AND_SCISSORRECT_OBJECT_COUNT_PER_PIPELINE = 16;
    static final int DecimationLevels = 2;				// number of downsampling steps

    static int BackBufferFormat = GLenum.GL_RGBA8;

    final Recti[] m_viewports = new Recti[D3D10_VIEWPORT_AND_SCISSORRECT_OBJECT_COUNT_PER_PIPELINE];
    final Recti[] m_scissor_rects = new Recti[D3D10_VIEWPORT_AND_SCISSORRECT_OBJECT_COUNT_PER_PIPELINE];
    int m_num_viewports;
    int m_num_scissor_rects;


//    ID3D10Device*	m_device;
//    ID3D10Effect*	m_effect;
    int m_back_buffer_sample_desc;

    GLSLProgram m_tech_bolt_out;
    GLSLProgram	m_tech_lines_out;

    GLSLProgram	m_tech_add_buffer;

    GLSLProgram	m_tech_blur_buffer_horizontal;
    GLSLProgram	m_tech_blur_buffer_vertical;

    BufferGL  m_constants_lightning_appearance;
    BufferGL  m_constants_lightning_structure;

    GLSLProgram	m_tech_down_sample_2x2;

    Set<LightningSeed> m_lightning_seeds;
    int				m_max_vertices;

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

    int[] m_down_sample_buffer_sizes;
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

    BufferGL/*Geometry::SimpleVertexBuffer<SubdivideVertex>**/	m_subdivide_buffer0;
    BufferGL/*Geometry::SimpleVertexBuffer<SubdivideVertex>**/	m_subdivide_buffer1;
    VertexArrayObject m_subdivide_layout;

    public LightningRenderer(int back_buffer_sample_desc ){
        m_back_buffer_sample_desc = back_buffer_sample_desc;
//        m_tech_bolt_out(m_effect->GetTechniqueByName("BoltOut")),
//        m_tech_lines_out(m_effect->GetTechniqueByName("ShowLines")),

//        m_tech_add_buffer(m_effect->GetTechniqueByName("AddBuffer")),
//        m_tech_blur_buffer_horizontal(m_effect->GetTechniqueByName("BlurBufferHorizontal")),
//        m_tech_blur_buffer_vertical(m_effect->GetTechniqueByName("BlurBufferVertical")),


//        m_tech_down_sample_2x2(m_effect->GetTechniqueByName("DownSample2x2")),

//        m_constants_lightning_appearance(m_effect, "LightningAppearance"),
//        m_constants_lightning_structure(m_effect, "LightningStructure"),
        m_constants_lightning_appearance = new BufferGL();
        m_constants_lightning_appearance.initlize(GLenum.GL_UNIFORM_BUFFER, LightningAppearance.SIZE, null, GLenum.GL_STREAM_DRAW);
        m_constants_lightning_structure = new BufferGL();
        m_constants_lightning_structure.initlize(GLenum.GL_UNIFORM_BUFFER, LightningStructure.SIZE, null, GLenum.GL_STREAM_DRAW);

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

        m_down_sample_buffer_sizes = new int[DecimationLevels];
        m_down_sample_buffers = new Texture2D[DecimationLevels -1];

        /*m_num_scissor_rects(0),
        m_num_viewports(0),
        m_max_vertices(0)*/
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

    }
    public void SetMatrices(Matrix4f world, Matrix4f view, Matrix4f projection);
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

    public void Begin();
    public void Render(LightningSeed seed, LightningAppearance appearance, float charge, float animation_speed, boolean as_lines);
    public void End(boolean glow, Vector3f blur_sigma);

    private void	BuildSubdivisionBuffers();
    private BufferGL		Subdivide(LightningSeed seed){

    }

    private void	BuildDownSampleBuffers(int w, int h);
    private void	DownSample(Texture2D buffer);

    private void	BuildGradientTexture();
    private void	SaveViewports();
    private void	ResizeViewport(int w, int h);
    private void	RestoreViewports();


    private void	AddLightningSeed(LightningSeed seed){
        m_lightning_seeds.add(seed);
    }
    private void	RemoveLightningSeed(LightningSeed seed){
        m_lightning_seeds.remove(seed);
    }

    private void DrawQuad(GLSLProgram technique){

    }
}
