package jet.opengl.demos.nvidia.waves;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import jet.opengl.postprocessing.buffer.AttribDesc;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/7/25.
 */

public class GFSDK_WaveWorks_Quadtree implements Disposeable{
    private final GFSDK_WaveWorks_Quadtree_Params m_params = new GFSDK_WaveWorks_Quadtree_Params();

    private NVWaveWorks_Mesh m_pMesh;

    // Quad-tree LOD, 0 to 9 (1x1 ~ 256x256)
    private int m_lods;

    private final float[] m_eyePos = new float[4];

    private float m_geomorphCoeff;

    // Margin for frustum culling routines
    private float frustum_cull_margin;

    private final ArrayList<AllocQuad> m_allocated_patches_list = new ArrayList<>();
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

    // Pick a proper mesh pattern according to the adjacent patches.
    private QuadRenderParam selectMeshPattern(QuadNode quad_node);

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
                        new AttribDesc(0, 2, GLenum.GL_FLOAT, false, 4, 0)	// vPos
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
                            4, vertex_buffer, num_vert,
                            index_array, index_size_lookup[m_lods],
                    out_mesh
                    );
                }else{
                    hr = NVWaveWorks_Mesh.CreateGL2(	attribute_descs,
                            1, //sizeof(attribute_descs)/sizeof(attribute_descs[0]),
                            4, vertex_buffer, num_vert,
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

    GFSDK_WaveWorks_Quadtree();
    ~GFSDK_WaveWorks_Quadtree();

    HRESULT initD3D11(const GFSDK_WaveWorks_Quadtree_Params params/*, ID3D11Device* pD3DDevice*/){
//        #if WAVEWORKS_ENABLE_D3D11
        HRESULT hr;

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

    HRESULT setFrustumCullMargin (float margin);

    HRESULT buildRenderList(	Graphics_Context* pGC,
                                const gfsdk_float4x4& matView,
                                const gfsdk_float4x4& matProj,
                                const gfsdk_float2* pViewportDims
    );

    HRESULT flushRenderList(	Graphics_Context* pGC,
                                const UINT* pShaderInputRegisterMappings,
                                GFSDK_WaveWorks_Savestate* pSavestateImpl
    );

    HRESULT allocPatch(INT x, INT y, UINT lod, BOOL enabled);
    HRESULT freePatch(INT x, INT y, UINT lod);

    HRESULT getStats(GFSDK_WaveWorks_Quadtree_Stats& stats) const;

    static HRESULT getShaderInputCountD3D11();
    static HRESULT getShaderInputDescD3D11(UINT inputIndex, GFSDK_WaveWorks_ShaderInput_Desc* pDesc);
    static HRESULT getShaderInputCountGnm();
    static HRESULT getShaderInputDescGnm(UINT inputIndex, GFSDK_WaveWorks_ShaderInput_Desc* pDesc);
    static HRESULT getShaderInputCountGL2();
    static HRESULT getShaderInputDescGL2(UINT inputIndex, GFSDK_WaveWorks_ShaderInput_Desc* pDesc);

    // Rendering list
    private int buildNodeList(	QuadNode quad_node,
                          float NumPixelsInViewport,
                          Matrix4f matView,
                          Matrix4f matProj,
                          ReadableVector3f eyePoint,
                          QuadCoord quad_coords){

    }

    private HRESULT buildRenderListAuto(	Matrix4f matView,
                                            Matrix4f matProj,
                                            ReadableVector3f eyePoint,
                                            float viewportW,
                                            float viewportH){

    }

    private HRESULT buildRenderListExplicit(	Matrix4f matView,
                                                Matrix4f matProj,
                                                ReadableVector3f eyePoint,
                                                float viewportW,
                                                float viewportH){

    }


    private void sortRenderList(){

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
    }

    private static final class AllocQuad implements Comparable<AllocQuad>
    {
        final QuadCoord coords = new QuadCoord();
        boolean enabled;

        @Override
        public int compareTo(AllocQuad rhs) {
            return coords.compareTo(rhs.coords);
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
        static final int SIZE = (12+4+4)* 4;
        final float[] g_matLocalWorld = new float[12];
        final float[] g_vsEyePos = new float[4];
        final float[] g_MorphParam = new float[4];
    };

    private static final class hs_cbuffer
    {
        static final int SIZE = (4+4)*4;
        final float[] g_eyePos =new float[4];
        final float[] g_tessellationParams =new float[4];
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
