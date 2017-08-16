package jet.opengl.demos.nvidia.waves;

import com.nvidia.developer.opengl.utils.Pool;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.buffer.AttribDesc;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CachaRes;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/7/25.
 */

public class GFSDK_WaveWorks_Quadtree implements Disposeable{
    static final int nvrm_unused = -1;
    private final GFSDK_WaveWorks_Quadtree_Params m_params = new GFSDK_WaveWorks_Quadtree_Params();

    private NVWaveWorks_Mesh m_pMesh;

    // Quad-tree LOD, 0 to 9 (1x1 ~ 256x256)
    private int m_lods;

    private final Vector3f m_eyePos = new Vector3f();

    private float m_geomorphCoeff;

    // Margin for frustum culling routines
    private float frustum_cull_margin;

    private final ArrayList<AllocQuad> m_allocated_patches_list = new ArrayList<>();  // We can use LinkedHashmap to instead the ArrayList.
    private final ArrayList<QuadNode> m_unsorted_render_list = new ArrayList<>();
    private final ArrayList<QuadNode> m_render_roots_list = new ArrayList<>();

    // We sort the render list approx front to back, in order to maximise any depth-rejection benefits
    private final ArrayList<QuadNode> m_sorted_render_list = new ArrayList<>();

    // Pattern lookup array. Filled at init time.
    private final QuadRenderParam[][][][][] m_mesh_patterns = new QuadRenderParam[9][3][3][3][3];

    // Stats
    private final GFSDK_WaveWorks_Quadtree_Stats m_stats = new GFSDK_WaveWorks_Quadtree_Stats();

    // D3D API handling
    private nv_water_d3d_api m_d3dAPI = nv_water_d3d_api.nv_water_d3d_api_undefined;

    private final Union m_d3d = new Union();
    private GLFuncProvider gl;

    private final Vector4f[] m_bbox_verts = new Vector4f[8];
    private final Matrix4f m_tempMat = new Matrix4f();
    private final QuadCoord[] m_sub_quad_coords = new QuadCoord[4];
    private final Pool<QuadNode> g_QuadNodePool = new Pool<>(()->new QuadNode());
    private final vs_cbuffer m_vs_cbuffer = new vs_cbuffer();
    private final hs_cbuffer m_hs_cbuffer = new hs_cbuffer();

    public GFSDK_WaveWorks_Quadtree(){
        for(int i = 0; i < m_bbox_verts.length; i++)
            m_bbox_verts[i] = new Vector4f();

        for(int i = 0; i <m_sub_quad_coords.length; i++)
            m_sub_quad_coords[i] = new QuadCoord();
    }

    // Pick a proper mesh pattern according to the adjacent patches.
    private QuadRenderParam selectMeshPattern(QuadNode quad_node){
        // Check 4 adjacent quad.
//        gfsdk_float2 point_left = quad_node.bottom_left + gfsdk_make_float2(-m_params.min_patch_length * 0.5f, quad_node.length * 0.5f);
        float point_x = quad_node.bottom_left_x - m_params.min_patch_length * 0.5f;
        float point_y = quad_node.bottom_left_y + quad_node.length * 0.5f;
        int left_adj_index = searchLeaf(m_render_roots_list, m_unsorted_render_list, point_x, point_y);

//        gfsdk_float2 point_right = quad_node.bottom_left + gfsdk_make_float2(quad_node.length + m_params.min_patch_length * 0.5f, quad_node.length * 0.5f);
        point_x = quad_node.bottom_left_x + quad_node.length + m_params.min_patch_length * 0.5f;
        point_y = quad_node.bottom_left_y + quad_node.length * 0.5f;
        int right_adj_index = searchLeaf(m_render_roots_list, m_unsorted_render_list, point_x, point_y);

//        gfsdk_float2 point_bottom = quad_node.bottom_left + gfsdk_make_float2(quad_node.length * 0.5f, -m_params.min_patch_length * 0.5f);
        point_x = quad_node.bottom_left_x + quad_node.length * 0.5f;
        point_y = quad_node.bottom_left_y - m_params.min_patch_length * 0.5f;
        int bottom_adj_index = searchLeaf(m_render_roots_list, m_unsorted_render_list, point_x, point_y);

//        gfsdk_float2 point_top = quad_node.bottom_left + gfsdk_make_float2(quad_node.length * 0.5f, quad_node.length + m_params.min_patch_length * 0.5f);
        point_x = quad_node.bottom_left_x + quad_node.length * 0.5f;
        point_y = quad_node.bottom_left_y + quad_node.length + m_params.min_patch_length * 0.5f;
        int top_adj_index = searchLeaf(m_render_roots_list, m_unsorted_render_list, point_x, point_y);

        int left_type = 0;
        if (left_adj_index >= 0 && m_unsorted_render_list.get(left_adj_index).length > quad_node.length * 0.999f)
        {
            QuadNode adj_node = m_unsorted_render_list.get(left_adj_index);
            float scale = adj_node.length / quad_node.length * (m_params.mesh_dim >> quad_node.lod) / (m_params.mesh_dim >> adj_node.lod);
            if (scale > 3.999f)
                left_type = 2;
            else if (scale > 1.999f)
                left_type = 1;
        }

        int right_type = 0;
        if (right_adj_index >= 0 && m_unsorted_render_list.get(right_adj_index).length > quad_node.length * 0.999f)
        {
            QuadNode adj_node = m_unsorted_render_list.get(right_adj_index);
            float scale = adj_node.length / quad_node.length * (m_params.mesh_dim >> quad_node.lod) / (m_params.mesh_dim >> adj_node.lod);
            if (scale > 3.999f)
                right_type = 2;
            else if (scale > 1.999f)
                right_type = 1;
        }

        int bottom_type = 0;
        if (bottom_adj_index >= 0 && m_unsorted_render_list.get(bottom_adj_index).length > quad_node.length * 0.999f)
        {
            QuadNode adj_node = m_unsorted_render_list.get(bottom_adj_index);
            float scale = adj_node.length / quad_node.length * (m_params.mesh_dim >> quad_node.lod) / (m_params.mesh_dim >> adj_node.lod);
            if (scale > 3.999f)
                bottom_type = 2;
            else if (scale > 1.999f)
                bottom_type = 1;
        }

        int top_type = 0;
        if (top_adj_index >= 0 && m_unsorted_render_list.get(top_adj_index).length > quad_node.length * 0.999f)
        {
            QuadNode adj_node = m_unsorted_render_list.get(top_adj_index);
            float scale = adj_node.length / quad_node.length * (m_params.mesh_dim >> quad_node.lod) / (m_params.mesh_dim >> adj_node.lod);
            if (scale > 3.999f)
                top_type = 2;
            else if (scale > 1.999f)
                top_type = 1;
        }

        // Check lookup table, [L][R][B][T]
        return m_mesh_patterns[quad_node.lod][left_type][right_type][bottom_type][top_type];
    }

    // create a triangle strip mesh for water surface.
    private HRESULT initGeometry(){
        CommonUtil.safeRelease(m_pMesh);

        m_lods = 0;

        m_params.mesh_dim = Math.min(Math.max(8, m_params.mesh_dim), 256);


        int mesh_dim = m_params.mesh_dim;
        // Added check for tessellation friendly flag: if we don't use tessellation,
        // then we don't need to decrease mesh density
        if((m_d3dAPI == nv_water_d3d_api.nv_water_d3d_api_d3d11 || m_d3dAPI == nv_water_d3d_api.nv_water_d3d_api_gnm) && (m_params.use_tessellation == true))
        {
            m_params.mesh_dim = Math.min(Math.max(32, m_params.mesh_dim), 256);
            mesh_dim = m_params.mesh_dim / 4;
        }

        for (int i = mesh_dim; i > 1; i >>= 1)
            m_lods ++;
        System.out.println("m_lods = " + m_lods);

        int num_vert = (mesh_dim + 1) * (mesh_dim + 1);

        // --------------------------------- Vertex Buffer -------------------------------
        water_quadtree_vertex[] vertex_array = new water_quadtree_vertex[num_vert];

        int i, j;
        for (i = 0; i <= mesh_dim; i++)
        {
            for (j = 0; j <= mesh_dim; j++)
            {
                vertex_array[i * (mesh_dim + 1) + j] = new water_quadtree_vertex();
                vertex_array[i * (mesh_dim + 1) + j].index_x = (float)j;
                vertex_array[i * (mesh_dim + 1) + j].index_y = (float)i;
            }
        }

        // --------------------------------- Index Buffer -------------------------------

        // The index numbers for all mesh LODs (up to 256x256)
        final int index_size_lookup[] = {0, 0, 0, 23328, 131544, 596160, 2520072, 10348560, 41930136};

//        memset(&m_mesh_patterns[0][0][0][0][0], 0, sizeof(m_mesh_patterns));

        // Generate patch meshes. Each patch contains two parts: the inner mesh which is a regular
        // grids in a triangle list. The boundary mesh is constructed w.r.t. the edge degrees to
        // meet water-tight requirement.
        int[] index_array = new int[index_size_lookup[m_lods]];
        int offset = 0;
        int level_size = mesh_dim;

        Rect inner_rect = new Rect();
        Rect outer_rect = new Rect();
        // Enumerate patterns
        for (int level = 0; level <= m_lods - 3; level ++)
        {
            int left_degree = level_size;

            for (int left_type = 0; left_type < 3; left_type ++)
            {
                int right_degree = level_size;

                for (int right_type = 0; right_type < 3; right_type ++)
                {
                    int bottom_degree = level_size;

                    for (int bottom_type = 0; bottom_type < 3; bottom_type ++)
                    {
                        int top_degree = level_size;

                        for (int top_type = 0; top_type < 3; top_type ++)
                        {
//                            QuadRenderParam* pattern = &m_mesh_patterns[level][left_type][right_type][bottom_type][top_type];
                            QuadRenderParam pattern = new QuadRenderParam();

                            // Inner mesh (triangle list)

                            inner_rect.left   = left_type;
                            inner_rect.right  = level_size - right_type;
                            inner_rect.bottom = bottom_type;
                            inner_rect.top    = level_size - top_type;

                            int num_new_indices = generateInnerMesh(inner_rect, mesh_dim, !m_params.use_tessellation, index_array, offset);

                            pattern.inner_start_index = offset;
                            pattern.num_inner_faces = num_new_indices / 3;
                            offset += num_new_indices;

                            // Boundary mesh (triangle list)
                            int l_degree = (left_degree   == level_size) ? 0 : left_degree;
                            int r_degree = (right_degree  == level_size) ? 0 : right_degree;
                            int b_degree = (bottom_degree == level_size) ? 0 : bottom_degree;
                            int t_degree = (top_degree    == level_size) ? 0 : top_degree;

//                            RECT outer_rect = {0, level_size, level_size, 0};
                            outer_rect.left = 0;
                            outer_rect.top = level_size;
                            outer_rect.right = level_size;
                            outer_rect.bottom = 0;
                            num_new_indices = generateBoundaryMesh(l_degree, r_degree, b_degree, t_degree, outer_rect, mesh_dim, index_array, offset);

                            pattern.boundary_start_index = offset;
                            pattern.num_boundary_faces = num_new_indices / 3;
                            offset += num_new_indices;

                            top_degree /= 2;

                            m_mesh_patterns[level][left_type][right_type][bottom_type][top_type] = pattern;
                        }
                        bottom_degree /= 2;
                    }
                    right_degree /= 2;
                }
                left_degree /= 2;
            }
            level_size /= 2;
        }

        assert(offset == index_size_lookup[m_lods]);
        if(offset != index_size_lookup[m_lods]){
            System.err.println("initGeometry: Inner Error");
        }

//        #if WAVEWORKS_ENABLE_GRAPHICS
        // --------------------------------- Initialise mesh -------------------------------
        HRESULT hr;
        switch(m_d3dAPI)
        {
            /*
            #if WAVEWORKS_ENABLE_D3D11
            case nv_water_d3d_api_d3d11:
            {
                const D3D11_INPUT_ELEMENT_DESC grid_layout[] = {
                    { "POSITION", 0, DXGI_FORMAT_R32G32_FLOAT, 0, 0, D3D11_INPUT_PER_VERTEX_DATA, 0 }
            };
                const UINT num_layout_elements = sizeof(grid_layout)/sizeof(grid_layout[0]);


                V_RETURN(NVWaveWorks_Mesh::CreateD3D11(	m_d3d._11.m_pd3d11Device,
                    grid_layout, num_layout_elements,
                    SM5::g_GFSDK_WAVEWORKS_VERTEX_INPUT_Sig, sizeof(SM5::g_GFSDK_WAVEWORKS_VERTEX_INPUT_Sig),
                    sizeof(vertex_array[0]), vertex_array, num_vert,
                    index_array, index_size_lookup[m_lods],
                &m_pMesh
                ));
            }
            break;
            #endif
            #if WAVEWORKS_ENABLE_GNM
            case nv_water_d3d_api_gnm:
            {
                V_RETURN(NVWaveWorks_Mesh::CreateGnm(
                    sizeof(vertex_array[0]), vertex_array, num_vert,
                    index_array, index_size_lookup[m_lods],
                &m_pMesh
                ));
            }
            break;
            #endif
            #if WAVEWORKS_ENABLE_GL
            */
            case nv_water_d3d_api_gl2:
            case nv_water_d3d_api_d3d11:
            {
                AttribDesc attribute_descs[] =
                {
                        new AttribDesc(0, 2, GLenum.GL_FLOAT, false, 0, 0)	// vPos
                };

                NVWaveWorks_Mesh[] out_mesh = new NVWaveWorks_Mesh[1];
                ByteBuffer vertex_buffer = CacheBuffer.getCachedByteBuffer(vertex_array.length * water_quadtree_vertex.SIZE);
                for(water_quadtree_vertex vertex : vertex_array){
                    vertex.store(vertex_buffer);
                }
                vertex_buffer.flip();

                if(m_d3dAPI == nv_water_d3d_api.nv_water_d3d_api_d3d11){
                    hr = NVWaveWorks_Mesh.CreateD3D11(	attribute_descs,
                            1, //sizeof(attribute_descs)/sizeof(attribute_descs[0]),
                            8, vertex_buffer, num_vert,
                            index_array, index_size_lookup[m_lods],
                    out_mesh
                    );
                }else{
                    hr = NVWaveWorks_Mesh.CreateGL2(	attribute_descs,
                            1, //sizeof(attribute_descs)/sizeof(attribute_descs[0]),
                            8, vertex_buffer, num_vert,
                            index_array, index_size_lookup[m_lods],
                            out_mesh);
                }

                m_pMesh = out_mesh[0];
                if(hr != HRESULT.S_OK) return hr;

            }
            break;
//            #endif
            default:
                // Unexpected API
                return HRESULT.E_FAIL;
        };
//        assert(m_pMesh);
//        #endif // WAVEWORKS_ENABLE_GRAPHICS
//
//        SAFE_DELETE_ARRAY(vertex_array);
//        SAFE_DELETE_ARRAY(index_array);

        return HRESULT.S_OK;
    }

    // Used to debug
    public void init(GFSDK_WaveWorks_Quadtree_Params params){
        initD3D11(params);
    }

    HRESULT initD3D11(GFSDK_WaveWorks_Quadtree_Params params/*, ID3D11Device* pD3DDevice*/){
//        #if WAVEWORKS_ENABLE_D3D11
        HRESULT hr;

        gl = GLFuncProviderFactory.getGLFuncProvider();
        if(nv_water_d3d_api.nv_water_d3d_api_d3d11 != m_d3dAPI)
        {
            releaseD3DObjects();
        }
//        else if(m_d3d._11.m_pd3d11Device != pD3DDevice)
//        {
//            releaseD3DObjects();
//        }

        if(nv_water_d3d_api.nv_water_d3d_api_undefined == m_d3dAPI)
        {
            // Only accept true DX11 devices if use_tessellation is set to true
//            D3D_FEATURE_LEVEL FeatureLevel = pD3DDevice->GetFeatureLevel();
//            if((FeatureLevel < D3D_FEATURE_LEVEL_11_0) && (m_params.use_tessellation == true))
//            {
//                return E_FAIL;
//            }
            m_d3dAPI = nv_water_d3d_api.nv_water_d3d_api_d3d11;
//            m_d3d._11.m_pd3d11Device = pD3DDevice;
//            m_d3d._11.m_pd3d11Device->AddRef();

            hr = allocateD3DObjects();
            if(hr != HRESULT.S_OK)
                return hr;
        }

        return reinit(params);
//        #else
//        return E_FAIL;
//        #endif
    }
    HRESULT initGnm(GFSDK_WaveWorks_Quadtree_Params param) { return HRESULT.E_FAIL;}
    HRESULT initGL2(GFSDK_WaveWorks_Quadtree_Params params, int Program){
        HRESULT hr;

        gl = GLFuncProviderFactory.getGLFuncProvider();
        if(nv_water_d3d_api.nv_water_d3d_api_gl2 != m_d3dAPI)
        {
            releaseD3DObjects();
        }

        if(nv_water_d3d_api.nv_water_d3d_api_undefined == m_d3dAPI)
        {
            m_d3dAPI = nv_water_d3d_api.nv_water_d3d_api_gl2;
            hr = allocateD3DObjects(); if(hr != HRESULT.S_OK) return hr;
        }
        m_d3d._GL2.m_pGL2QuadtreeProgram = Program;
        return reinit(params);
    }


    // API-independent init
    HRESULT reinit(GFSDK_WaveWorks_Quadtree_Params params){
        HRESULT hr;

        boolean reinitGeometry = false;
        if(null == m_pMesh || params.mesh_dim != m_params.mesh_dim || params.use_tessellation != m_params.use_tessellation)
        {
            reinitGeometry = true;
        }
        m_params.set(params);

        if(reinitGeometry)
        {
            hr = initGeometry();  if(hr != HRESULT.S_OK) return hr;
        }

        return HRESULT.S_OK;
    }

    private static final class Rect
    {
        int left;
        int top;
        int right;
        int bottom;
    }

    private static final class MeshIdx {
        final Rect vert_rect;
        final int mesh_dim;

        public MeshIdx(Rect vert_rect, int mesh_dim) {
            this.vert_rect = vert_rect;
            this.mesh_dim = mesh_dim;
        }

        int index_2d(int x, int y){
            return (((y) + vert_rect.bottom) * (mesh_dim + 1) + (x) + vert_rect.left);
        }
    }

    // Generate boundary mesh for a patch. Return the number of generated indices
    private int generateBoundaryMesh(int left_degree, int right_degree, int bottom_degree, int top_degree,
                                     Rect vert_rect, int mesh_dim, int[] output, int offset)
    {
        int i, j;
        int counter = offset;
        int width = vert_rect.right - vert_rect.left;
        int height = vert_rect.top - vert_rect.bottom;

        int b_step = bottom_degree !=0 ? width / bottom_degree : 0;
        int r_step = right_degree !=0 ? height / right_degree : 0;
        int t_step = top_degree !=0 ? width / top_degree : 0;
        int l_step = left_degree !=0 ? height / left_degree : 0;

        MeshIdx meshIdx = new MeshIdx(vert_rect, mesh_dim);
        // Triangle list for bottom boundary
        if (b_step > 0)
        {
            final int b_min = b_step < l_step ? b_step : 0;
            final int b_max = b_step < r_step ? width - b_step : width;
            for (i = b_min; i < b_max; i += b_step)
            {
                output[counter++] = meshIdx.index_2d(i, 0);
                output[counter++] = meshIdx.index_2d(i + b_step, 0);
                output[counter++] = meshIdx.index_2d(i + b_step / 2, b_step / 2);

                if(i != 0 || b_step != l_step) {
                    for (j = 0; j < b_step / 2; j ++)
                    {
                        output[counter++] = meshIdx.index_2d(i, 0);
                        output[counter++] = meshIdx.index_2d(i + j + 1, b_step / 2);
                        output[counter++] = meshIdx.index_2d(i + j, b_step / 2);
                    }
                }

                if(i != width - b_step || b_step != r_step) {
                    for (j = b_step / 2; j < b_step; j ++)
                    {
                        output[counter++] = meshIdx.index_2d(i + b_step, 0);
                        output[counter++] = meshIdx.index_2d(i + j + 1, b_step / 2);
                        output[counter++] = meshIdx.index_2d(i + j, b_step / 2);
                    }
                }
            }
        }

        // Right boundary
        if (r_step > 0)
        {
            final int r_min = r_step < b_step ? r_step : 0;
            final int r_max = r_step < t_step ? height - r_step : height;
            for (i = r_min; i < r_max; i += r_step)
            {
                output[counter++] = meshIdx.index_2d(width, i);
                output[counter++] = meshIdx.index_2d(width, i + r_step);
                output[counter++] = meshIdx.index_2d(width - r_step / 2, i + r_step / 2);

                if(i != 0 || r_step != b_step) {
                    for (j = 0; j < r_step / 2; j ++)
                    {
                        output[counter++] = meshIdx.index_2d(width, i);
                        output[counter++] = meshIdx.index_2d(width - r_step / 2, i + j + 1);
                        output[counter++] = meshIdx.index_2d(width - r_step / 2, i + j);
                    }
                }

                if(i != height - r_step || r_step != t_step) {
                    for (j = r_step / 2; j < r_step; j ++)
                    {
                        output[counter++] = meshIdx.index_2d(width, i + r_step);
                        output[counter++] = meshIdx.index_2d(width - r_step / 2, i + j + 1);
                        output[counter++] = meshIdx.index_2d(width - r_step / 2, i + j);
                    }
                }
            }
        }

        // Top boundary
        if (t_step > 0)
        {
            final int t_min = t_step < l_step ? t_step : 0;
            final int t_max = t_step < r_step ? width - t_step : width;
            for (i = t_min; i < t_max; i += t_step)
            {
                output[counter++] = meshIdx.index_2d(i, height);
                output[counter++] = meshIdx.index_2d(i + t_step / 2, height - t_step / 2);
                output[counter++] = meshIdx.index_2d(i + t_step, height);

                if(i != 0 || t_step != l_step) {
                    for (j = 0; j < t_step / 2; j ++)
                    {
                        output[counter++] = meshIdx.index_2d(i, height);
                        output[counter++] = meshIdx.index_2d(i + j, height - t_step / 2);
                        output[counter++] = meshIdx.index_2d(i + j + 1, height - t_step / 2);
                    }
                }

                if(i != width - t_step || t_step != r_step) {
                    for (j = t_step / 2; j < t_step; j ++)
                    {
                        output[counter++] = meshIdx.index_2d(i + t_step, height);
                        output[counter++] = meshIdx.index_2d(i + j, height - t_step / 2);
                        output[counter++] = meshIdx.index_2d(i + j + 1, height - t_step / 2);
                    }
                }
            }
        }

        // Left boundary
        if (l_step > 0)
        {
            final int l_min = l_step < b_step ? l_step : 0;
            final int l_max = l_step < t_step ? height - l_step : height;
            for (i = l_min; i < l_max; i += l_step)
            {
                output[counter++] = meshIdx.index_2d(0, i);
                output[counter++] = meshIdx.index_2d(l_step / 2, i + l_step / 2);
                output[counter++] = meshIdx.index_2d(0, i + l_step);

                if(i != 0 || l_step != b_step) {
                    for (j = 0; j < l_step / 2; j ++)
                    {
                        output[counter++] = meshIdx.index_2d(0, i);
                        output[counter++] = meshIdx.index_2d(l_step / 2, i + j);
                        output[counter++] = meshIdx.index_2d(l_step / 2, i + j + 1);
                    }
                }

                if(i != height - l_step || l_step != t_step) {
                    for (j = l_step / 2; j < l_step; j ++)
                    {
                        output[counter++] = meshIdx.index_2d(0, i + l_step);
                        output[counter++] = meshIdx.index_2d(l_step / 2, i + j);
                        output[counter++] = meshIdx.index_2d(l_step / 2, i + j + 1);
                    }
                }
            }
        }

        return counter - offset;
    }

    // Generate inner mesh for a patch. Return the number of generated indices
    int generateInnerMesh(Rect vert_rect, int mesh_dim, boolean generate_diamond_grid, int[] output, int offset)
    {
        int i, j;
        int counter = offset;
        int width = vert_rect.right - vert_rect.left;
        int height = vert_rect.top - vert_rect.bottom;

        MeshIdx meshIdx = new MeshIdx(vert_rect, mesh_dim);
        for (i = 0; i < height; i++)
        {
            for (j = 0; j < width; j++)
            {

                if(((i + j + vert_rect.left + vert_rect.bottom) % 2 == 0) || (!generate_diamond_grid))
                {
                    output[counter++] = meshIdx.index_2d(j, i);
                    output[counter++] = meshIdx.index_2d(j + 1, i);
                    output[counter++] = meshIdx.index_2d(j + 1, i + 1);
                    output[counter++] = meshIdx.index_2d(j, i);
                    output[counter++] = meshIdx.index_2d(j + 1, i + 1);
                    output[counter++] = meshIdx.index_2d(j, i + 1);
                }

                else
                {
                    output[counter++] = meshIdx.index_2d(j + 1, i);
                    output[counter++] = meshIdx.index_2d(j + 1, i + 1);
                    output[counter++] = meshIdx.index_2d(j, i + 1);
                    output[counter++] = meshIdx.index_2d(j, i);
                    output[counter++] = meshIdx.index_2d(j + 1, i);
                    output[counter++] = meshIdx.index_2d(j, i + 1);
                }
            }
        }

        return counter - offset;
    }

    private boolean checkNodeVisibility(QuadNode quad_node, Matrix4f mat_view_proj, float sea_level, float margin)
    {
        // Transform corners to clip space and building bounding box
        Vector4f[] bbox_verts = m_bbox_verts;
        Vector4f[] bbox_verts_transformed = m_bbox_verts;
        Vector4f bbox_verts0 = bbox_verts[0];
        bbox_verts0.set(quad_node.bottom_left_x - margin, quad_node.bottom_left_y - margin, sea_level - margin, 1);
//        bbox_verts[1] = bbox_verts[0] + gfsdk_make_float4(quad_node.length + 2.0f * margin, 0, 0, 0);
//        bbox_verts[2] = bbox_verts[0] + gfsdk_make_float4(quad_node.length + 2.0f * margin, quad_node.length + 2.0f * margin, 0, 0);
//        bbox_verts[3] = bbox_verts[0] + gfsdk_make_float4(0, quad_node.length + 2.0f * margin, 0, 0);
        Vector4f bbox_verts1 = bbox_verts[1];
        bbox_verts1.x = bbox_verts0.x + quad_node.length + 2.0f * margin;
        bbox_verts1.y = bbox_verts0.y;
        bbox_verts1.z = bbox_verts0.z;
        bbox_verts1.w = bbox_verts0.w;
        Vector4f bbox_verts2 = bbox_verts[2];
        bbox_verts2.x = bbox_verts0.x + quad_node.length + 2.0f * margin;
        bbox_verts2.y = bbox_verts0.y + quad_node.length + 2.0f * margin;
        bbox_verts2.z = bbox_verts0.z;
        bbox_verts2.w = bbox_verts0.w;
        Vector4f bbox_verts3 = bbox_verts[3];
        bbox_verts3.x = bbox_verts0.x;
        bbox_verts3.y = bbox_verts0.y + quad_node.length + 2.0f * margin;
        bbox_verts3.z = bbox_verts0.z;
        bbox_verts3.w = bbox_verts0.w;

//        bbox_verts[4] = bbox_verts[0] + gfsdk_make_float4(0, 0, margin * 2.0f, 0);
//        bbox_verts[5] = bbox_verts[1] + gfsdk_make_float4(0, 0, margin * 2.0f, 0);
//        bbox_verts[6] = bbox_verts[2] + gfsdk_make_float4(0, 0, margin * 2.0f, 0);
//        bbox_verts[7] = bbox_verts[3] + gfsdk_make_float4(0, 0, margin * 2.0f, 0);
        for(int i = 0; i < 4; i++){
            Vector4f src = bbox_verts[i];
            Vector4f dst = bbox_verts[i + 4];
            dst.x = src.x;
            dst.y = src.y;
            dst.z = src.z + margin * 2.0f;
            dst.w = src.w;
        }


//        gfsdk_float4x4 mat_view_proj;
//        mat4Mat4Mul(mat_view_proj,matProj,matView);

//        vec4Mat4Mul(bbox_verts_transformed[0], bbox_verts[0], mat_view_proj);
//        vec4Mat4Mul(bbox_verts_transformed[1], bbox_verts[1], mat_view_proj);
//        vec4Mat4Mul(bbox_verts_transformed[2], bbox_verts[2], mat_view_proj);
//        vec4Mat4Mul(bbox_verts_transformed[3], bbox_verts[3], mat_view_proj);
//        vec4Mat4Mul(bbox_verts_transformed[4], bbox_verts[4], mat_view_proj);
//        vec4Mat4Mul(bbox_verts_transformed[5], bbox_verts[5], mat_view_proj);
//        vec4Mat4Mul(bbox_verts_transformed[6], bbox_verts[6], mat_view_proj);
//        vec4Mat4Mul(bbox_verts_transformed[7], bbox_verts[7], mat_view_proj);
        for(int i = 0; i < 8; i++){
            Matrix4f.transform(mat_view_proj, bbox_verts[i], bbox_verts_transformed[i]);
        }

        if (bbox_verts_transformed[0].x < -bbox_verts_transformed[0].w && bbox_verts_transformed[1].x < -bbox_verts_transformed[1].w && bbox_verts_transformed[2].x < -bbox_verts_transformed[2].w && bbox_verts_transformed[3].x < -bbox_verts_transformed[3].w &&
            bbox_verts_transformed[4].x < -bbox_verts_transformed[4].w && bbox_verts_transformed[5].x < -bbox_verts_transformed[5].w && bbox_verts_transformed[6].x < -bbox_verts_transformed[6].w && bbox_verts_transformed[7].x < -bbox_verts_transformed[7].w)
            return false;

        if (bbox_verts_transformed[0].x > bbox_verts_transformed[0].w && bbox_verts_transformed[1].x > bbox_verts_transformed[1].w && bbox_verts_transformed[2].x > bbox_verts_transformed[2].w && bbox_verts_transformed[3].x > bbox_verts_transformed[3].w &&
            bbox_verts_transformed[4].x > bbox_verts_transformed[4].w && bbox_verts_transformed[5].x > bbox_verts_transformed[5].w && bbox_verts_transformed[6].x > bbox_verts_transformed[6].w && bbox_verts_transformed[7].x > bbox_verts_transformed[7].w)
            return false;

        if (bbox_verts_transformed[0].y < -bbox_verts_transformed[0].w && bbox_verts_transformed[1].y < -bbox_verts_transformed[1].w && bbox_verts_transformed[2].y < -bbox_verts_transformed[2].w && bbox_verts_transformed[3].y < -bbox_verts_transformed[3].w &&
            bbox_verts_transformed[4].y < -bbox_verts_transformed[4].w && bbox_verts_transformed[5].y < -bbox_verts_transformed[5].w && bbox_verts_transformed[6].y < -bbox_verts_transformed[6].w && bbox_verts_transformed[7].y < -bbox_verts_transformed[7].w)
            return false;

        if (bbox_verts_transformed[0].y > bbox_verts_transformed[0].w && bbox_verts_transformed[1].y > bbox_verts_transformed[1].w && bbox_verts_transformed[2].y > bbox_verts_transformed[2].w && bbox_verts_transformed[3].y > bbox_verts_transformed[3].w &&
            bbox_verts_transformed[4].y > bbox_verts_transformed[4].w && bbox_verts_transformed[5].y > bbox_verts_transformed[5].w && bbox_verts_transformed[6].y > bbox_verts_transformed[6].w && bbox_verts_transformed[7].y > bbox_verts_transformed[7].w)
            return false;

        if (bbox_verts_transformed[0].z < -bbox_verts_transformed[0].w && bbox_verts_transformed[1].z < -bbox_verts_transformed[1].w && bbox_verts_transformed[2].z < -bbox_verts_transformed[2].w && bbox_verts_transformed[3].z < -bbox_verts_transformed[3].w &&
            bbox_verts_transformed[4].z < -bbox_verts_transformed[4].w && bbox_verts_transformed[5].z < -bbox_verts_transformed[5].w && bbox_verts_transformed[6].z < -bbox_verts_transformed[6].w && bbox_verts_transformed[7].z < -bbox_verts_transformed[7].w)
            return false;

//        if (bbox_verts_transformed[0].z < 0.f && bbox_verts_transformed[1].z < 0.f && bbox_verts_transformed[2].z < 0.f && bbox_verts_transformed[3].z < 0.f &&
//            bbox_verts_transformed[4].z < 0.f && bbox_verts_transformed[5].z < 0.f && bbox_verts_transformed[6].z < 0.f && bbox_verts_transformed[7].z < 0.f)
//            return false;

        if (bbox_verts_transformed[0].z > bbox_verts_transformed[0].w && bbox_verts_transformed[1].z > bbox_verts_transformed[1].w && bbox_verts_transformed[2].z > bbox_verts_transformed[2].w && bbox_verts_transformed[3].z > bbox_verts_transformed[3].w &&
            bbox_verts_transformed[4].z > bbox_verts_transformed[4].w && bbox_verts_transformed[5].z > bbox_verts_transformed[5].w && bbox_verts_transformed[6].z > bbox_verts_transformed[6].w && bbox_verts_transformed[7].z > bbox_verts_transformed[7].w)
            return false;

        return true;
    }

    // Test 16 points on the quad and find out the biggest one.
    private static float sample_pos[/*16*/][/*2*/] =
    {
        {0, 0},
        {0, 1},
        {1, 0},
        {1, 1},
        {0.5f, 0.333f},
        {0.25f, 0.667f},
        {0.75f, 0.111f},
        {0.125f, 0.444f},
        {0.625f, 0.778f},
        {0.375f, 0.222f},
        {0.875f, 0.556f},
        {0.0625f, 0.889f},
        {0.5625f, 0.037f},
        {0.3125f, 0.37f},
        {0.8125f, 0.704f},
        {0.1875f, 0.148f},
    };

    private static float estimateGridCoverage(QuadNode quad_node,
                                    GFSDK_WaveWorks_Quadtree_Params quad_tree_param,
                                    Matrix4f matProj,
                                    float screen_area,
                                    ReadableVector3f eye_point
    )
    {
        // Estimate projected area

        float grid_len_world = quad_node.length / quad_tree_param.mesh_dim;

        float max_area_proj = 0;
        for (int i = 0; i < 16; i++)
        {
//            Vector3f eye_vec = Vector3f.sub(test_point,eye_point, null);

            float x = quad_node.bottom_left_x + quad_node.length * sample_pos[i][0];
            float y = quad_node.bottom_left_y + quad_node.length * sample_pos[i][1];
            float z = quad_tree_param.sea_level;

            float eye_vec_x = x - eye_point.getX();
            float eye_vec_y = y - eye_point.getY();
            float eye_vec_z = z - eye_point.getZ();
            float dist2 = Vector3f.lengthSquare(eye_vec_x, eye_vec_y, eye_vec_z);

            float area_world = grid_len_world * grid_len_world;// * abs(eye_point.z) / sqrt(nearest_sqr_dist);
            float area_proj = area_world * matProj.m00 * matProj.m11 / dist2;

            if (max_area_proj < area_proj)
                max_area_proj = area_proj;
        }

        float pixel_coverage = max_area_proj * screen_area * 0.25f;

        return pixel_coverage;
    }

    private static boolean isLeaf(QuadNode quad_node)
    {
        return (quad_node.sub_node[0] < 0 && quad_node.sub_node[1] < 0 && quad_node.sub_node[2] < 0 && quad_node.sub_node[3] < 0);
    }

    private static int searchLeaf(List<QuadNode> root_node_list, List<QuadNode> node_list, float point_x, float point_y)
    {
        int index = -1;

        QuadNode node = null;

        boolean foundRoot = false;
//        const std::vector<QuadNode>::const_iterator rootEndIt = root_node_list.end();
//        for(std::vector<QuadNode>::const_iterator it = root_node_list.begin(); it != rootEndIt; ++it)
        for(QuadNode it : root_node_list)
        {
            if (point_x >= it.bottom_left_x && point_x <= it.bottom_left_x + it.length &&
                point_y >= it.bottom_left_y && point_y <= it.bottom_left_y + it.length)
            {
                node = it;
                foundRoot = true;
                break;
            }
        }

        if(!foundRoot)
            return -1;

        while (!isLeaf(node))
        {
            boolean found = false;

            for (int i = 0; i < 4; i++)
            {
                index = node.sub_node[i];
                if (index < 0)
                    continue;

                QuadNode sub_node = node_list.get(index);
                if (point_x >= sub_node.bottom_left_x && point_x <= sub_node.bottom_left_x + sub_node.length &&
                    point_y >= sub_node.bottom_left_y && point_y <= sub_node.bottom_left_y + sub_node.length)
                {
                    assert(node.length > sub_node.length);
                    node = sub_node;
                    found = true;
                    break;
                }
            }

            if (!found)
                return -1;
        }

        return index;
    }


    HRESULT setFrustumCullMargin (float margin){
        frustum_cull_margin = margin;
        return HRESULT.S_OK;
    }

    @CachaRes
    HRESULT buildRenderList(	//Graphics_Context* pGC,
                                Matrix4f matView,
                                Matrix4f matProj,
                                Vector2f pViewportDims){
        HRESULT hr;

        float viewportW = 0;
        float viewportH = 0;

        long tStart = -1, tStop = -1;

        if(m_params.enable_CPU_timers)
        {
            // tying thread to core #0 to ensure OS doesn't reallocathe thread to other cores which might corrupt QueryPerformanceCounter readings
            Simulation_Util.tieThreadToCore(0);
            // getting the timestamp
            tStart = System.nanoTime();
        }

//        #if WAVEWORKS_ENABLE_GRAPHICS
        switch(m_d3dAPI)
        {
//            #if WAVEWORKS_ENABLE_D3D11
//            case nv_water_d3d_api_d3d11:
//            {
//                ID3D11DeviceContext* pDC_d3d11 = pGC->d3d11();
//
//                D3D11_VIEWPORT vp;
//                UINT NumViewports = 1;
//                pDC_d3d11->RSGetViewports(&NumViewports,&vp);
//                viewportW = vp.Width;
//                viewportH = vp.Height;
//
//                break;
//            }
//            #endif
            /*#if WAVEWORKS_ENABLE_GNM
            case nv_water_d3d_api_gnm:
            {
                assert(pViewportDims);
                viewportW = pViewportDims->x;
                viewportH = pViewportDims->y;

                break;
            }
            #endif*/
//            #if WAVEWORKS_ENABLE_GL
            case nv_water_d3d_api_gl2:
            case nv_water_d3d_api_d3d11:
            {
                IntBuffer vp = CacheBuffer.getCachedIntBuffer(4);
                gl.glGetIntegerv( GLenum.GL_VIEWPORT, vp);
                viewportW = vp.get(2);
                viewportH = vp.get(3);
                if(!m_printOnce){
                    System.out.printf("Viewpot = (%f, %f).\n", viewportW, viewportH);
                }

                break;
            }
//            #endif
            default:
                // Unexpected API
                return HRESULT.E_FAIL;
        }
//        #endif // WAVEWORKS_ENABLE_GRAPHICS

        // Compute eye point
//        gfsdk_float4x4 inv_mat_view;
//        gfsdk_float4 vec_original = {0,0,0,1};
//        gfsdk_float4 vec_transformed;
//        mat4Inverse(inv_mat_view, matView);
//        vec4Mat4Mul(vec_transformed, vec_original, inv_mat_view);
//        gfsdk_float3 eyePoint = gfsdk_make_float3(vec_transformed.x,vec_transformed.y,vec_transformed.z);
//        m_eyePos[0] = vec_transformed.x;
//        m_eyePos[1] = vec_transformed.y;
//        m_eyePos[2] = vec_transformed.z;
        Matrix4f.decompseRigidMatrix(matView, m_eyePos, null, null);

        // Compute geomorphing coefficient
        final float geomorphing_degree = Math.max(0.f,Math.min(m_params.geomorphing_degree,1.f));
        m_geomorphCoeff = (float) (geomorphing_degree * 2.f * Math.sqrt(m_params.upper_grid_coverage/(matProj.m00 * matProj.m11 * viewportW * viewportH)));

        if(m_allocated_patches_list.isEmpty())
        {
            hr = buildRenderListAuto(matView,matProj,m_eyePos,viewportW,viewportH);
            if(hr != HRESULT.S_OK) return hr;
        }
        else
        {
            hr = buildRenderListExplicit(matView,matProj,m_eyePos,viewportW,viewportH);
            if(hr != HRESULT.S_OK) return hr;
        }

        // Sort the resulting list front-to-back
        sortRenderList();

        if(m_params.enable_CPU_timers)
        {
            // getting the timestamp and calculating time
            tStop = System.nanoTime();
            m_stats.CPU_quadtree_update_time = (tStop - tStart) / 1000_000;
        }
        else
        {
            m_stats.CPU_quadtree_update_time = 0;
        }

        if(!m_printOnce){
            System.out.println("Quadtree params: \n" + m_params);
            System.out.println("MatView:" + matView);
            System.out.println("MatProj:" + matProj);
        }

        return HRESULT.S_OK;
    }

    HRESULT flushRenderList(	//Graphics_Context* pGC,
                                int[] pShaderInputRegisterMappings,
                                GFSDK_WaveWorks_Savestate pSavestateImpl){
        HRESULT hr;

        // Zero counters
        m_stats.num_patches_drawn = 0;

//        #if WAVEWORKS_ENABLE_D3D11
        // Fetch DC, if D3D11
//        ID3D11DeviceContext* pDC_d3d11 = NULL;
//        if(nv_water_d3d_api_d3d11 == m_d3dAPI)
//        {
//            pDC_d3d11 = pGC->d3d11();
//        }
//        #endif

//        #if WAVEWORKS_ENABLE_GNM
//        // Fetch Gnmx ctx, if gnm
//        sce::Gnmx::LightweightGfxContext* gfxContext_gnm = NULL;
//        if(nv_water_d3d_api_gnm == m_d3dAPI)
//        {
//            gfxContext_gnm = pGC->gnm();
//        }
//        #endif

        // Preserve state, if necessary
        if(m_sorted_render_list.size() > 0&& null != pSavestateImpl)
        {
            hr = m_pMesh.PreserveState( pSavestateImpl);

//            #if WAVEWORKS_ENABLE_GRAPHICS
            switch(m_d3dAPI)
            {
//                #if WAVEWORKS_ENABLE_D3D11
                case nv_water_d3d_api_d3d11:
                {
                    final int reg = pShaderInputRegisterMappings[ShaderInputD3D11_vs_buffer];
                    if(reg != nvrm_unused)
                    {
//                        V_RETURN(pSavestateImpl.PreserveD3D11PixelShaderConstantBuffer(/*pDC_d3d11,*/ reg));  TODO
                    }
                }
                break;
//                #endif
//                #if WAVEWORKS_ENABLE_GL
//                case nv_water_d3d_api_gl2:
//                {
//                    // no savestate implementation in GL
//                }
//                break;
//                #endif
                default:
                    break;
            }
//            #endif // WAVEWORKS_ENABLE_GRAPHICS
        }

//        #if WAVEWORKS_ENABLE_GNM
//        GFSDK_WaveWorks_GnmxWrap* gnmxWrap = GFSDK_WaveWorks_GNM_Util::getGnmxWrap();
//        gnmxWrap->pushMarker(*gfxContext_gnm, "GFSDK_WaveWorks_Quadtree::flushRenderList");
//        #endif

        // We assume the center of the water surface is at (0, 0, 0).
        for (int i = 0; i < m_sorted_render_list.size(); i++)
        {
            QuadNode node = m_sorted_render_list.get(i);

            if (!isLeaf(node))
                continue;

            // Check adjacent patches and select mesh pattern
            QuadRenderParam render_param = selectMeshPattern(node);

            // Find the right LOD to render
            int level_size = m_params.mesh_dim >> node.lod;

            Matrix4f matLocalWorld = m_tempMat;
            matLocalWorld.setIdentity();
            matLocalWorld.m00 = node.length / level_size;
            matLocalWorld.m11 = node.length / level_size;
            matLocalWorld.m22 = 0;
            matLocalWorld.m30 = node.bottom_left_x;
            matLocalWorld.m31 = node.bottom_left_y;
            matLocalWorld.m32 = m_params.sea_level;

            NVWaveWorks_Mesh.PrimitiveType prim_type = NVWaveWorks_Mesh.PrimitiveType.PT_TriangleList;
            if(m_d3dAPI == nv_water_d3d_api.nv_water_d3d_api_d3d11 || m_d3dAPI == nv_water_d3d_api.nv_water_d3d_api_gnm)
            {
                if(m_params.use_tessellation)
                {
                    prim_type = NVWaveWorks_Mesh.PrimitiveType.PT_PatchList_3;
                    // decrease mesh density when using tessellation
                    matLocalWorld.m00 *= 4.0f;
                    matLocalWorld.m11 *= 4.0f;
                }
            }

            int[] pMeshShaderInputMapping = null;

//            #if WAVEWORKS_ENABLE_GRAPHICS
//            gfsdk_float4 eyePos = gfsdk_make_float4(m_eyePos[0],m_eyePos[1],m_eyePos[2],1.f);

//            #if WAVEWORKS_ENABLE_GL
            int meshShaderInputMapping = nvrm_unused;
//            #endif

            final float morph_distance_constant = m_geomorphCoeff * level_size / node.length;
//            gfsdk_float4 morphParam = gfsdk_make_float4(morph_distance_constant,0.f,0.f,node.morph_sign);
            switch(m_d3dAPI)
            {
//                #if WAVEWORKS_ENABLE_D3D11
                case nv_water_d3d_api_d3d11:
                {
//                    final int regvs = pShaderInputRegisterMappings[ShaderInputD3D11_vs_buffer];
//                    if(regvs != nvrm_unused)
                    {
                        {
//                            D3D11_CB_Updater<vs_cbuffer> cbu(pDC_d3d11,m_d3d._11.m_pd3d11VertexShaderCB);
//                            memcpy(&cbu.cb().g_matLocalWorld, &matLocalWorld, sizeof(cbu.cb().g_matLocalWorld));
//                            memcpy(&cbu.cb().g_vsEyePos, &eyePos, sizeof(cbu.cb().g_vsEyePos));
//                            memcpy(&cbu.cb().g_MorphParam, &morphParam, sizeof(cbu.cb().g_MorphParam));
                            matLocalWorld.store(m_vs_cbuffer.g_matLocalWorld, 0);
                            m_vs_cbuffer.g_vsEyePos[0] = m_eyePos.x;
                            m_vs_cbuffer.g_vsEyePos[1] = m_eyePos.y;
                            m_vs_cbuffer.g_vsEyePos[2] = m_eyePos.z;
                            m_vs_cbuffer.g_vsEyePos[3] = 1; // m_eyePos[3];
                            m_vs_cbuffer.g_MorphParam[0] = morph_distance_constant;
                            m_vs_cbuffer.g_MorphParam[1] = 0.f;
                            m_vs_cbuffer.g_MorphParam[2] = 0.f;
                            m_vs_cbuffer.g_MorphParam[3] = node.morph_sign;

                            ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(vs_cbuffer.SIZE);
                            m_vs_cbuffer.store(bytes).flip();
                            UpdateSubresource(GLenum.GL_UNIFORM_BUFFER, m_d3d._11.m_pd3d11VertexShaderCB, bytes);
                        }
//                        pDC_d3d11->VSSetConstantBuffers(regvs, 1, &m_d3d._11.m_pd3d11VertexShaderCB);
                        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 3, m_d3d._11.m_pd3d11VertexShaderCB);
                    }
//                    final int reghs = pShaderInputRegisterMappings[ShaderInputD3D11_hs_buffer];
//                    if(reghs != nvrm_unused)
                    {
                        {
//                            D3D11_CB_Updater<hs_cbuffer> cbu(pDC_d3d11,m_d3d._11.m_pd3d11HullShaderCB);
//                            memcpy(&cbu.cb().g_eyePos, &m_eyePos, sizeof(m_eyePos));
//                            memset(&cbu.cb().g_tessellationParams,0,sizeof(cbu.cb().g_tessellationParams));
//                            memcpy(&cbu.cb().g_tessellationParams, &m_params.tessellation_lod, sizeof(m_params.tessellation_lod));

                            m_hs_cbuffer.g_eyePos[0] = m_eyePos.x;
                            m_hs_cbuffer.g_eyePos[1] = m_eyePos.y;
                            m_hs_cbuffer.g_eyePos[2] = m_eyePos.z;
                            m_hs_cbuffer.g_eyePos[3] = 1; //m_eyePos.w;
                            m_hs_cbuffer.g_tessellationParams[0] = m_params.tessellation_lod;

                            ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(hs_cbuffer.SIZE);
                            m_hs_cbuffer.store(bytes).flip();
                            UpdateSubresource(GLenum.GL_UNIFORM_BUFFER, m_d3d._11.m_pd3d11HullShaderCB, bytes);
                        }
//                        pDC_d3d11->HSSetConstantBuffers(reghs, 1, &m_d3d._11.m_pd3d11HullShaderCB);
                        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 2, m_d3d._11.m_pd3d11HullShaderCB);
                    }
                }
                break;
//                #endif
                /*#if WAVEWORKS_ENABLE_GNM
                case nv_water_d3d_api_gnm:
                {
                    const UINT regvs = pShaderInputRegisterMappings[ShaderInputD3D11_vs_buffer];
                    if(regvs != nvrm_unused)
                    {
                        vs_cbuffer* pVSCB = (vs_cbuffer*)gnmxWrap->allocateFromCommandBuffer(*gfxContext_gnm, sizeof(vs_cbuffer), Gnm::kEmbeddedDataAlignment4);
                        memcpy(&pVSCB->g_matLocalWorld, &matLocalWorld, sizeof(pVSCB->g_matLocalWorld));
                        memcpy(&pVSCB->g_vsEyePos, &eyePos, sizeof(pVSCB->g_vsEyePos));
                        memcpy(&pVSCB->g_MorphParam, &morphParam, sizeof(pVSCB->g_MorphParam));

                        Gnm::Buffer buffer;
                        buffer.initAsConstantBuffer(pVSCB, sizeof(vs_cbuffer));
                        buffer.setResourceMemoryType(Gnm::kResourceMemoryTypeRO);
                        gnmxWrap->setConstantBuffers(*gfxContext_gnm, m_params.use_tessellation ? Gnm::kShaderStageLs : Gnm::kShaderStageVs, regvs, 1, &buffer);
                    }
                    const UINT reghs = pShaderInputRegisterMappings[ShaderInputD3D11_hs_buffer];
                    if(reghs != nvrm_unused)
                    {
                        hs_cbuffer* pHSCB = (hs_cbuffer*)gnmxWrap->allocateFromCommandBuffer(*gfxContext_gnm, sizeof(vs_cbuffer), Gnm::kEmbeddedDataAlignment4);
                        memcpy(&pHSCB->g_eyePos, &m_eyePos, sizeof(m_eyePos));
                        memset(&pHSCB->g_tessellationParams,0,sizeof(pHSCB->g_tessellationParams));
                        memcpy(&pHSCB->g_tessellationParams, &m_params.tessellation_lod, sizeof(m_params.tessellation_lod));

                        Gnm::Buffer buffer;
                        buffer.initAsConstantBuffer(pHSCB, sizeof(hs_cbuffer));
                        buffer.setResourceMemoryType(Gnm::kResourceMemoryTypeRO);
                        gnmxWrap->setConstantBuffers(*gfxContext_gnm, Gnm::kShaderStageHs, reghs, 1, &buffer);
                    }
                }
                break;
                #endif*/
//                #if WAVEWORKS_ENABLE_GL
                case nv_water_d3d_api_gl2:
                {
                    final int rm_g_matLocalWorld = pShaderInputRegisterMappings[ShaderInputGL2_g_matLocalWorld];
                    final int rm_g_vsEyePos = pShaderInputRegisterMappings[ShaderInputGL2_g_vsEyePos];
                    final int rm_g_MorphParam = pShaderInputRegisterMappings[ShaderInputGL2_g_MorphParam];
                    final int rm_attr_vPos = pShaderInputRegisterMappings[ShaderInputGL2_attr_vPos];

//                    GLfloat mlv[12];
//                    mlv[0] = matLocalWorld._11;
//                    mlv[1] = matLocalWorld._12;
//                    mlv[2] = matLocalWorld._13;
//                    mlv[3] = matLocalWorld._14;
//                    mlv[4] = matLocalWorld._21;
//                    mlv[5] = matLocalWorld._22;
//                    mlv[6] = matLocalWorld._23;
//                    mlv[7] = matLocalWorld._24;
//                    mlv[8] = matLocalWorld._31;
//                    mlv[9] = matLocalWorld._32;
//                    mlv[10]= matLocalWorld._33;
//                    mlv[11]= matLocalWorld._34;

                    if(rm_g_matLocalWorld != nvrm_unused)
//                        gl.glUniformMatrix3x4fv(rm_g_matLocalWorld, 1, GL_FALSE, (GLfloat*)mlv);
                        gl.glUniformMatrix4fv(rm_g_matLocalWorld, false, CacheBuffer.wrap(matLocalWorld));
                    if(rm_g_vsEyePos != nvrm_unused)
                        gl.glUniform4f(rm_g_vsEyePos, m_eyePos.x, m_eyePos.y, m_eyePos.z, 1);
                    if(rm_g_MorphParam != nvrm_unused)
                        gl.glUniform4f(rm_g_MorphParam, morph_distance_constant,0.f,0.f,node.morph_sign);
                    if(rm_attr_vPos != nvrm_unused) {
                        meshShaderInputMapping = rm_attr_vPos;
                        pMeshShaderInputMapping = new int[]{meshShaderInputMapping};
                    }
                }
                break;
//                #endif
                default:
                    // Unexpected API
                    return HRESULT.E_FAIL;
            }
//            #endif // WAVEWORKS_ENABLE_GRAPHICS

            // Render
            int mesh_dim = m_params.mesh_dim;
            int num_vert = (mesh_dim + 1) * (mesh_dim + 1);
            if (render_param.num_inner_faces > 0)
            {
//                V_RETURN();
                hr = m_pMesh.Draw(/*pGC,*/ prim_type, 0, 0, num_vert, render_param.inner_start_index, render_param.num_inner_faces, pMeshShaderInputMapping);
                if(hr != HRESULT.S_OK) return hr;
            }
            if (render_param.num_boundary_faces > 0)
            {
                hr = m_pMesh.Draw(prim_type, 0, 0, num_vert, render_param.boundary_start_index, render_param.num_boundary_faces, pMeshShaderInputMapping);
                if(hr != HRESULT.S_OK) return hr;
            }
            ++m_stats.num_patches_drawn;
        }

        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 2, 0);  // unbind the eypos_buffer
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 3, 0);  // unbind the gemo_buffer.

        LogUtil.i(LogUtil.LogType.DEFAULT, "render count = " + m_stats.num_patches_drawn);
//        #if WAVEWORKS_ENABLE_GNM
//        gnmxWrap->popMarker(*gfxContext_gnm);
//        #endif

        m_printOnce = true;
        return HRESULT.S_OK;
    }

    public interface QuadRendererListener{
        void onQuadDraw(GFSDK_WaveWorks_Quadtree_Params params, Matrix4f modelMat);
    }

    public void renderShaded(Matrix4f matView, Matrix4f matProj, QuadRendererListener renderer){
        buildRenderList(matView, matProj, null);
        m_stats.num_patches_drawn = 0;

        // We assume the center of the water surface is at (0, 0, 0).
        for (int i = 0; i < m_sorted_render_list.size(); i++)
        {
            QuadNode node = m_sorted_render_list.get(i);

            if (!isLeaf(node))
                continue;

            // Check adjacent patches and select mesh pattern
            QuadRenderParam render_param = selectMeshPattern(node);

            // Find the right LOD to render
            int level_size = m_params.mesh_dim >> node.lod;

            Matrix4f matLocalWorld = m_tempMat;
            matLocalWorld.setIdentity();
            matLocalWorld.m00 = node.length / level_size;
            matLocalWorld.m11 = node.length / level_size;
            matLocalWorld.m22 = 0;
            matLocalWorld.m30 = node.bottom_left_x;
            matLocalWorld.m31 = node.bottom_left_y;
            matLocalWorld.m32 = m_params.sea_level;

            NVWaveWorks_Mesh.PrimitiveType prim_type = NVWaveWorks_Mesh.PrimitiveType.PT_TriangleList;
//            #endif // WAVEWORKS_ENABLE_GRAPHICS

            renderer.onQuadDraw(m_params, matLocalWorld);

            // Render
            int mesh_dim = m_params.mesh_dim;
            int num_vert = (mesh_dim + 1) * (mesh_dim + 1);
            if (render_param.num_inner_faces > 0)
            {
                m_pMesh.Draw(prim_type, 0, 0, num_vert, render_param.inner_start_index, render_param.num_inner_faces, null);
            }
            if (render_param.num_boundary_faces > 0)
            {
                m_pMesh.Draw(prim_type, 0, 0, num_vert, render_param.boundary_start_index, render_param.num_boundary_faces, null);
            }
            ++m_stats.num_patches_drawn;
        }
//        LogUtil.i(LogUtil.LogType.DEFAULT, "render count = " + m_stats.num_patches_drawn);
        m_printOnce = true;
    }

    HRESULT allocPatch(int x, int y, int lod, boolean enabled){
        final AllocQuad  quad = new AllocQuad(new QuadCoord(x,y,lod), enabled);

//        typedef std::vector<AllocQuad>::iterator it_type;
//        const it_type endIt = m_allocated_patches_list.end();
//        const std::pair<it_type, it_type> er = std::equal_range(m_allocated_patches_list.begin(), endIt, quad);
//        if(er.first != er.second)
//        {
//            // Already in the list - that's an error
//            return E_FAIL;
//        }

        long result = CommonUtil.equal_range(m_allocated_patches_list, quad);
        int first = Numeric.decodeFirst(result);
        int second = Numeric.decodeSecond(result);

        if(first != second){
            // Already in the list - that's an error
            return HRESULT.E_FAIL;
        }

//        m_allocated_patches_list.insert(er.first, quad);
        m_allocated_patches_list.add(quad);
        return HRESULT.S_OK;
    }

    HRESULT freePatch(int x, int y, int lod){
        final AllocQuad dummy_quad = new AllocQuad( new QuadCoord(x,y,lod), true );

//        typedef std::vector<AllocQuad>::iterator it_type;
//        const it_type endIt = m_allocated_patches_list.end();
//        const std::pair<it_type, it_type> er = std::equal_range(m_allocated_patches_list.begin(), endIt, dummy_quad);

        long result = CommonUtil.equal_range(m_allocated_patches_list, dummy_quad);
        int first = Numeric.decodeFirst(result);
        int second = Numeric.decodeSecond(result);
        if(first == second)
        {
            // Not in the list - that's an error
            return  HRESULT.E_FAIL;
        }

        m_allocated_patches_list.remove(dummy_quad);
        return HRESULT.S_OK;
    }

    HRESULT getStats(GFSDK_WaveWorks_Quadtree_Stats stats) {
        stats.set(m_stats);
        return HRESULT.S_OK;
    }

    static int getShaderInputCountD3D11() { return NumShaderInputsD3D11;}
    static HRESULT getShaderInputDescD3D11(int inputIndex, GFSDK_WaveWorks_ShaderInput_Desc pDesc){
        if(inputIndex >= NumShaderInputsD3D11)
            return HRESULT.E_FAIL;

        pDesc.set(ShaderInputD3D11Descs[inputIndex]);

        return HRESULT.S_OK;
    }
    static int getShaderInputCountGnm() { return 0;}
    static HRESULT getShaderInputDescGnm(int inputIndex, GFSDK_WaveWorks_ShaderInput_Desc pDesc){
        return HRESULT.E_FAIL;
    }

    static int getShaderInputCountGL2() { return NumShaderInputsGL2;}
    static HRESULT getShaderInputDescGL2(int inputIndex, GFSDK_WaveWorks_ShaderInput_Desc pDesc){
        if(inputIndex >= NumShaderInputsGL2)
            return HRESULT.E_FAIL;

        pDesc.set(ShaderInputGL2Descs[inputIndex]);

        return HRESULT.S_OK;
    }

    private QuadNode node(float left_x, float left_y, float len, int lod, float morph_sign){
        QuadNode node = g_QuadNodePool.obtain();
        node.set(left_x, left_y, len, lod);
        node.morph_sign = morph_sign;
        return node;
    }

    private boolean m_printOnce;

    // Rendering list
    private int buildNodeList(	QuadNode quad_node,
                          float NumPixelsInViewport,
                          Matrix4f matView,
                          Matrix4f matProj,
                          ReadableVector3f eyePoint,
                          QuadCoord quad_coords){
        // Check if the node is disabled
        if(quad_coords != null && !m_allocated_patches_list.isEmpty())
        {
//            typedef std::vector<AllocQuad>::iterator it_type;
//            const it_type endIt = m_allocated_patches_list.end();
//            const AllocQuad dummy_quad = { *quad_coords, TRUE };
//            const std::pair<it_type, it_type> er = std::equal_range(m_allocated_patches_list.begin(), endIt, dummy_quad);
//            CommonUtil.safeRelease();
//            if(er.first != er.second)
//            {
//                if(!er.first->enabled)
//                return -2;
//            }

            AllocQuad dummy_quad = new AllocQuad(quad_coords, true);
            long result = CommonUtil.equal_range(m_allocated_patches_list, dummy_quad);
            int first = Numeric.decodeFirst(result);
            int second = Numeric.decodeFirst(result);

            if(first != second){
                if(!m_allocated_patches_list.get(first).enabled){
                    return -2;
                }
            }
        }

        Matrix4f mat_proj_view = Matrix4f.mul(matProj, matView, m_tempMat);
        // Check against view frustum
        if (!checkNodeVisibility(quad_node, /*matView, matProj,*/mat_proj_view, m_params.sea_level, frustum_cull_margin))
            return -1;

        // Estimate the min grid coverage
        float min_coverage = estimateGridCoverage(quad_node, m_params, matProj, NumPixelsInViewport, eyePoint);
        //float geomorphing_degree = max(0.f,min(m_params.geomorphing_degree,1.f));

        // Recursively attatch sub-nodes.
        boolean visible = true;
        if (min_coverage > m_params.upper_grid_coverage && quad_node.length > m_params.min_patch_length)
        {
            QuadCoord[] sub_quad_coords = m_sub_quad_coords;
            QuadCoord sub_quad_coords_0 = null;
            QuadCoord sub_quad_coords_1 = null;
            QuadCoord sub_quad_coords_2 = null;
            QuadCoord sub_quad_coords_3 = null;
            if(quad_coords != null)
            {
                sub_quad_coords[0].x = 2 * quad_coords.x;
                sub_quad_coords[0].y = 2 * quad_coords.y;
                sub_quad_coords[0].lod = quad_coords.lod - 1;
                sub_quad_coords_0 = sub_quad_coords[0];

                sub_quad_coords[1].x = sub_quad_coords[0].x + 1;
                sub_quad_coords[1].y = sub_quad_coords[0].y;
                sub_quad_coords[1].lod = sub_quad_coords[0].lod;
                sub_quad_coords_1 = sub_quad_coords[1];

                sub_quad_coords[2].x = sub_quad_coords[0].x + 1;
                sub_quad_coords[2].y = sub_quad_coords[0].y + 1;
                sub_quad_coords[2].lod = sub_quad_coords[0].lod;
                sub_quad_coords_2 = sub_quad_coords[2];

                sub_quad_coords[3].x = sub_quad_coords[0].x;
                sub_quad_coords[3].y = sub_quad_coords[0].y + 1;
                sub_quad_coords[3].lod = sub_quad_coords[0].lod;
                sub_quad_coords_3 = sub_quad_coords[3];
            }

            // Flip the morph sign on each change of level
            final float sub_morph_sign = -1.f * quad_node.morph_sign;

            // Recursive rendering for sub-quads.
            QuadNode sub_node_0 = node(quad_node.bottom_left_x, quad_node.bottom_left_y, quad_node.length / 2, 0, sub_morph_sign);
            quad_node.sub_node[0] = buildNodeList(sub_node_0, NumPixelsInViewport, matView, matProj, eyePoint, sub_quad_coords_0);
            if(quad_node.sub_node[0] < 0) g_QuadNodePool.free(sub_node_0);

            QuadNode sub_node_1 = node(quad_node.bottom_left_x + quad_node.length/2, quad_node.bottom_left_y, quad_node.length / 2, 0, sub_morph_sign);
            quad_node.sub_node[1] = buildNodeList(sub_node_1, NumPixelsInViewport, matView, matProj, eyePoint, sub_quad_coords_1);
            if(quad_node.sub_node[1] < 0) g_QuadNodePool.free(sub_node_1);

            QuadNode sub_node_2 = node(quad_node.bottom_left_x + quad_node.length/2, quad_node.bottom_left_y + quad_node.length/2, quad_node.length / 2, 0, sub_morph_sign);
            quad_node.sub_node[2] = buildNodeList(sub_node_2, NumPixelsInViewport, matView, matProj, eyePoint, sub_quad_coords_2);
            if(quad_node.sub_node[2] < 0) g_QuadNodePool.free(sub_node_2);

            QuadNode sub_node_3 = node(quad_node.bottom_left_x, quad_node.bottom_left_y + quad_node.length/2, quad_node.length / 2, 0, sub_morph_sign);
            quad_node.sub_node[3] = buildNodeList(sub_node_3, NumPixelsInViewport, matView, matProj, eyePoint, sub_quad_coords_3);
            if(quad_node.sub_node[3] < 0) g_QuadNodePool.free(sub_node_3);

            // If all the sub-nodes are invisible, then we need to revise our original assessment
            // that the current node was visible
            visible = !isLeaf(quad_node);
        }

        if (visible)
        {
            // Estimate mesh LOD - we don't use 1x1, 2x2 or 4x4 patch. So the highest level is m_lods - 3.
            int lod = 0;
            for (lod = 0; lod < m_lods - 3; lod++)
            {
                if (min_coverage > m_params.upper_grid_coverage)
                    break;
                quad_node.morph_sign *= -1.f;
                min_coverage *= 4;
            }

            quad_node.lod = lod;
        }
        else
            return -1;

        // Insert into the list
        int position = m_unsorted_render_list.size();
        m_unsorted_render_list.add(quad_node);

        return position;
    }

    private HRESULT buildRenderListAuto(	Matrix4f matView,
                                            Matrix4f matProj,
                                            ReadableVector3f eyePoint,
                                            float viewportW,
                                            float viewportH){
        // Centre the top-level node on the nearest largest-patch boundary
        final float patch_length = m_params.min_patch_length;
        final float root_patch_length = patch_length * (0x00000001 << m_params.auto_root_lod);
        final float centreX = (float) (root_patch_length * Math.floor(eyePoint.getX()/root_patch_length + 0.5f));
        final float centreY = (float) (root_patch_length * Math.floor(eyePoint.getY()/root_patch_length + 0.5f));
        if(!m_printOnce){
            System.out.println("path_length = " + patch_length);
            System.out.println("root_patch_length = " + root_patch_length);
            System.out.printf("center = (%f, %f).\n", centreX, centreY);
        }

        // Build rendering list

        g_QuadNodePool.freeAll(m_unsorted_render_list);
//        g_QuadNodePool.freeAll(m_render_roots_list);

        m_unsorted_render_list.clear();
        m_render_roots_list.clear();
        QuadNode root_node00 = node(centreX, centreY, root_patch_length, 0, 1.f);
        QuadNode root_node01 = node(centreX, centreY - root_patch_length, root_patch_length, 0, 1.f);
        QuadNode root_node10 = node(centreX - root_patch_length, centreY, root_patch_length, 0, 1.f);
        QuadNode root_node11 = node(centreX - root_patch_length, centreY - root_patch_length, root_patch_length, 0, 1.f);

        if(buildNodeList(root_node00, viewportW * viewportH, matView, matProj, eyePoint, null) >= 0)
            m_render_roots_list.add(root_node00);
        else
            g_QuadNodePool.free(root_node00);

        if(buildNodeList(root_node01, viewportW * viewportH, matView, matProj, eyePoint, null) >= 0)
            m_render_roots_list.add(root_node01);
        else
            g_QuadNodePool.free(root_node01);

        if(buildNodeList(root_node10, viewportW * viewportH, matView, matProj, eyePoint, null) >= 0)
            m_render_roots_list.add(root_node10);
        else
            g_QuadNodePool.free(root_node10);

        if(buildNodeList(root_node11, viewportW * viewportH, matView, matProj, eyePoint, null) >= 0)
            m_render_roots_list.add(root_node11);
        else
            g_QuadNodePool.free(root_node11);

        return HRESULT.S_OK;
    }

    private HRESULT buildRenderListExplicit(	Matrix4f matView,
                                                Matrix4f matProj,
                                                ReadableVector3f eyePoint,
                                                float viewportW,
                                                float viewportH){
        assert(!m_allocated_patches_list.isEmpty());

        g_QuadNodePool.freeAll(m_unsorted_render_list);
//        g_QuadNodePool.freeAll(m_render_roots_list);

        m_unsorted_render_list.clear();
        m_render_roots_list.clear();

        // Use the first lod as the root lod
        final int root_lod = m_allocated_patches_list.get(0).coords.lod;
        final float root_patch_length = m_params.min_patch_length * (float)(0x00000001 << root_lod);
//        const std::vector<AllocQuad>::const_iterator endIt = m_allocated_patches_list.end();
//        for(std::vector<AllocQuad>::const_iterator it = m_allocated_patches_list.begin(); it != endIt; ++it)
        for(AllocQuad it : m_allocated_patches_list)
        {
            // Stop when we encounter the first non-root lod
            if(root_lod != it.coords.lod)
                break;

//            const gfsdk_float2 patch_offset = gfsdk_make_float2(root_patch_length * float(it->coords.x), root_patch_length * float(it->coords.y));
            float patch_offset_x = root_patch_length * (it.coords.x);
            float patch_offset_y = root_patch_length * (it.coords.y);
            QuadNode root_node = node(m_params.patch_origin.x + patch_offset_x, m_params.patch_origin.y + patch_offset_y, root_patch_length, 0,1.f);
            final int ix = buildNodeList(root_node, viewportW * viewportH, matView, matProj, eyePoint, it.coords);
            if(ix >= 0)
                m_render_roots_list.add(root_node);
            else
                g_QuadNodePool.free(root_node);
        }

        return HRESULT.S_OK;
    }


    private void sortRenderList(){
//        m_sorted_render_list = m_unsorted_render_list;
//        std::sort(m_sorted_render_list.begin(), m_sorted_render_list.end(), compareQuadNodeLength);

        m_sorted_render_list.clear();
        m_sorted_render_list.addAll(m_unsorted_render_list);
        m_sorted_render_list.sort(null);

        if(!m_printOnce){
            System.out.println("m_unsorted_render_list.size() = " + m_unsorted_render_list.size());
        }
    }

    private void releaseD3DObjects(){
        CommonUtil.safeRelease(m_pMesh);

//        #if WAVEWORKS_ENABLE_GRAPHICS
        switch(m_d3dAPI)
        {
//            #if WAVEWORKS_ENABLE_D3D11
            case nv_water_d3d_api_d3d11:
            {
                gl.glDeleteBuffer(m_d3d._11.m_pd3d11VertexShaderCB);
                gl.glDeleteBuffer(m_d3d._11.m_pd3d11HullShaderCB);
//                SAFE_RELEASE(m_d3d._11.m_pd3d11Device);
                m_d3dAPI = nv_water_d3d_api.nv_water_d3d_api_undefined;
            }
            break;
//            #endif
//            #if WAVEWORKS_ENABLE_GNM
            case nv_water_d3d_api_gnm:
            {
                m_d3dAPI = nv_water_d3d_api.nv_water_d3d_api_undefined;
            }
            break;
//            #endif
            default:
                break;
//            #if WAVEWORKS_ENABLE_GL
            case nv_water_d3d_api_gl2:
            {
                // nothing to release
                m_d3dAPI = nv_water_d3d_api.nv_water_d3d_api_undefined;
            }
            break;
//            #endif
        }
//        #endif // WAVEWORKS_ENABLE_GRAPHICS
    }

    private HRESULT allocateD3DObjects(){
//        #if WAVEWORKS_ENABLE_GRAPHICS
        switch(m_d3dAPI)
        {
//            #if WAVEWORKS_ENABLE_D3D11
            case nv_water_d3d_api_d3d11:
            {
                HRESULT hr;
                gl.glDeleteBuffer(m_d3d._11.m_pd3d11VertexShaderCB);
                gl.glDeleteBuffer(m_d3d._11.m_pd3d11HullShaderCB);

//                D3D11_BUFFER_DESC vscbDesc;
//                vscbDesc.ByteWidth = sizeof(vs_cbuffer);
//                vscbDesc.Usage = D3D11_CB_CREATION_USAGE;
//                vscbDesc.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
//                vscbDesc.CPUAccessFlags = D3D11_CB_CREATION_CPU_ACCESS_FLAGS;
//                vscbDesc.MiscFlags = 0;
//                vscbDesc.StructureByteStride = 0;
//                V_RETURN(m_d3d._11.m_pd3d11Device->CreateBuffer(&vscbDesc, NULL, &m_d3d._11.m_pd3d11VertexShaderCB));

                m_d3d._11.m_pd3d11VertexShaderCB = CreateBuffer(GLenum.GL_UNIFORM_BUFFER, vs_cbuffer.SIZE, GLenum.GL_DYNAMIC_READ);

//                D3D11_BUFFER_DESC hscbDesc;
//                hscbDesc.ByteWidth = sizeof(hs_cbuffer);
//                hscbDesc.Usage = D3D11_CB_CREATION_USAGE;
//                hscbDesc.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
//                hscbDesc.CPUAccessFlags = D3D11_CB_CREATION_CPU_ACCESS_FLAGS;
//                hscbDesc.MiscFlags = 0;
//                hscbDesc.StructureByteStride = 0;
//                V_RETURN(m_d3d._11.m_pd3d11Device->CreateBuffer(&hscbDesc, NULL, &m_d3d._11.m_pd3d11HullShaderCB));
                m_d3d._11.m_pd3d11HullShaderCB = CreateBuffer(GLenum.GL_UNIFORM_BUFFER, hs_cbuffer.SIZE, GLenum.GL_DYNAMIC_READ);
            }
            break;
//            #endif
//            #if WAVEWORKS_ENABLE_GNM
            case nv_water_d3d_api_gnm:
            {
                // nothing to do
            }
            break;
//            #endif
//            #if WAVEWORKS_ENABLE_GL
            case nv_water_d3d_api_gl2:
            {
                // nothing to do
            }
            break;
//            #endif
            default:
                // Unexpected API
                return HRESULT.E_FAIL;
        }
//        #endif // WAVEWORKS_ENABLE_GRAPHICS

        return HRESULT.S_OK;
    }

    private int CreateBuffer(int target, int size, int usage){
        int buffer = gl.glGenBuffer();
        gl.glBindBuffer(target, buffer);
        gl.glBufferData(target, size, usage);
        gl.glBindBuffer(target, 0);
        return buffer;
    }

    private void UpdateSubresource(int target, int buffer, Buffer data){
        gl.glBindBuffer(target, buffer);
        gl.glBufferSubData(target, 0, data);
        gl.glBindBuffer(target, 0);
    }

    @Override
    public void dispose() {

    }

    // List of allocated patches
    private static final class QuadCoord implements Comparable<QuadCoord>
    {
        int x;
        int y;
        int lod;

        QuadCoord(){}

        public QuadCoord(int x, int y, int lod) {
            this.x = x;
            this.y = y;
            this.lod = lod;
        }

        @Override
        public int compareTo(QuadCoord rhs) {
            // NB: We reverse the direction of the lod order, this causes the largest quads to sort
            // to the start of the collection, where we can use them as traversal roots
            if(lod > rhs.lod)
                return 1;
            else if(lod < rhs.lod)
                return -1;

            if(x < rhs.x)
                return 1;
            else if(x > rhs.x)
                return -1;

            if(y < rhs.y)
                return 1;
            else if(y < rhs.y)
                return -1;
            else
                return 0;
        }

        @Override
        public boolean equals(Object o) {
            if(o instanceof QuadCoord){
                QuadCoord other = (QuadCoord)o;
                return x == other.x && y == other.y && lod == other.lod;
            }

            return false;
        }
    }

    private static final class AllocQuad implements Comparable<AllocQuad>
    {
        QuadCoord coords;
        boolean enabled;

        public AllocQuad(QuadCoord coords, boolean enabled) {
            this.enabled = enabled;
            this.coords = coords;
        }

        @Override
        public int compareTo(AllocQuad rhs) {
            return coords.compareTo(rhs.coords);
        }

        @Override
        public boolean equals(Object o) {
            if(o instanceof AllocQuad){
                AllocQuad other = (AllocQuad)o;
                return coords.equals(other.coords) /*&& enabled == other.enabled*/;
            }

            return false;
        }
    }

    private static final class D3D11Objects
    {
//        ID3D11Device* m_pd3d11Device;
//        ID3D11Buffer* m_pd3d11VertexShaderCB;
//        ID3D11Buffer* m_pd3d11HullShaderCB;

        int m_pd3d11VertexShaderCB;
        int m_pd3d11HullShaderCB;
    }

    private static final class GL2Objects
    {
        int m_pGL2QuadtreeProgram;
        final int[] m_pGL2UniformLocations = new int[3];
    }

    private static final class Union{
        final D3D11Objects _11 = new D3D11Objects();
        final GL2Objects _GL2 = new GL2Objects();
    }

    private static final int
            ShaderInputD3D11_vs_buffer = 0,
            ShaderInputD3D11_hs_buffer = 1,
            NumShaderInputsD3D11 = 2;

    private static final int
            ShaderInputGL2_g_matLocalWorld = 0,
            ShaderInputGL2_g_vsEyePos =1,
            ShaderInputGL2_g_MorphParam = 2,
            ShaderInputGL2_attr_vPos = 3,
            NumShaderInputsGL2 = 4;

    private static final  GFSDK_WaveWorks_ShaderInput_Desc ShaderInputD3D11Descs[/*NumShaderInputsD3D11*/] = {
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.VertexShader_ConstantBuffer, "geom_buffer", 0 ),
            new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.HullShader_ConstantBuffer, "eyepos_buffer", 0 )
    };

    private static final GFSDK_WaveWorks_ShaderInput_Desc ShaderInputGL2Descs[/*NumShaderInputsGL2*/] = {
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_VertexShader_UniformLocation, "g_matLocalWorld", 0 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_VertexShader_UniformLocation, "g_vsEyePos",      0 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_VertexShader_UniformLocation, "g_MorphParam",    0 ),
        new GFSDK_WaveWorks_ShaderInput_Desc( GFSDK_WaveWorks_ShaderInput_Desc.GL_AttribLocation,               "vPos",            0 )
    };

    private static final class vs_cbuffer
    {
        static final int SIZE = (16+4+4)* 4;
        final float[] g_matLocalWorld = new float[16];
        final float[] g_vsEyePos = new float[4];
        final float[] g_MorphParam = new float[4];

        ByteBuffer store(ByteBuffer buf){
            CacheBuffer.put(buf, g_matLocalWorld);
            CacheBuffer.put(buf, g_vsEyePos);
            CacheBuffer.put(buf, g_MorphParam);

            return buf;
        }
    };

    private static final class hs_cbuffer
    {
        static final int SIZE = (4+4)*4;
        final float[] g_eyePos =new float[4];
        final float[] g_tessellationParams =new float[4];

        ByteBuffer store(ByteBuffer buf){
            CacheBuffer.put(buf, g_eyePos);
            CacheBuffer.put(buf, g_tessellationParams);

            return buf;
        }
    };

    private static final class water_quadtree_vertex
    {
        static final int SIZE = 8;
        float index_x;
        float index_y;

        void store(ByteBuffer buf){
            buf.putFloat(index_x).putFloat(index_y);
        }
    };

}
