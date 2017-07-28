package jet.opengl.demos.nvidia.waves;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;

import static jet.opengl.demos.nvidia.waves.HRESULT.E_FAIL;
import static jet.opengl.demos.nvidia.waves.HRESULT.S_FALSE;
import static jet.opengl.demos.nvidia.waves.HRESULT.S_OK;
import static jet.opengl.demos.nvidia.waves.Simulation_Util.gauss_map_resolution;
import static jet.opengl.demos.nvidia.waves.Simulation_Util.gauss_map_size;
import static jet.opengl.demos.nvidia.waves.nv_water_d3d_api.nv_water_d3d_api_d3d11;
import static jet.opengl.demos.nvidia.waves.nv_water_d3d_api.nv_water_d3d_api_undefined;

/**
 * Created by mazhen'gui on 2017/7/22.
 */

final class NVWaveWorks_FFT_Simulation_DirectCompute_Impl implements  NVWaveWorks_FFT_Simulation{

    /** 2 in-flight, one usable, one active */
    private static final int NumReadbackSlots = 4;
    /** 2 in-flight, one usable, one active */
    private static final int NumTimerSlots = 4;

    private NVWaveWorks_FFT_Simulation_Manager_DirectCompute_Impl m_pManager;

    private final GFSDK_WaveWorks_Detailed_Simulation_Params.Cascade m_params = new GFSDK_WaveWorks_Detailed_Simulation_Params.Cascade();

    private  int m_resolution;  // m_params.fft_resolution
    private  int m_half_resolution_plus_one;

    private boolean m_avoid_frame_depedencies = true; // if SLI, currently always true (performance issue)
    private boolean m_GaussAndOmegaInitialised;
    private boolean m_H0Dirty;

    private int m_active_readback_slot;			// i.e. not in-flight
    private int m_end_inflight_readback_slots;	// the first in-flight slot is always the one after active
    private boolean m_ReadbackInitialised;

    private long[] m_readback_kickIDs = new long[NumReadbackSlots];

    private long m_DisplacementMapVersion = GFSDK_WaveWorks.GFSDK_WaveWorks_InvalidKickID;

    private float[] m_timer_results = new float[NumTimerSlots];
    private long[] m_timer_kickIDs = new long[NumReadbackSlots];
    private int m_active_timer_slot;			// i.e. not in-flight
    private int m_end_inflight_timer_slots;		// the first in-flight slot is always the one after active
    private final D3D11Objects _11 = new D3D11Objects();
    private GLFuncProvider gl;

    HRESULT consumeAvailableTimerSlot(int[] slot, long kickID){
        if(m_active_timer_slot == m_end_inflight_timer_slots)
        {
            switch(m_d3dAPI)
            {
                case nv_water_d3d_api_d3d11:
                {
                    HRESULT hr = S_FALSE;

                    // No slots available - we must wait for the oldest in-flight timer to complete
                    int wait_slot = (m_active_timer_slot + 1) % NumTimerSlots;
                    int flag = 0;

//                    D3D11_QUERY_DATA_TIMESTAMP_DISJOINT disjoint;
//                    UINT64 start, end;
//                    do
//                    {
//                        hr =  m_d3d._11.m_context->GetData(m_d3d._11.m_frequency_queries[wait_slot], &disjoint, sizeof(disjoint), flag)
//                        | m_d3d._11.m_context->GetData(m_d3d._11.m_start_queries[wait_slot], &start, sizeof(start), flag)
//                        | m_d3d._11.m_context->GetData(m_d3d._11.m_end_queries[wait_slot], &end, sizeof(end), flag);
//                    } while(S_FALSE == hr);

//                    if(hr == S_OK)
//                    {
//                        m_timer_results[wait_slot] = disjoint.Disjoint ? 0.0f : (end - start) * 1000.0f / disjoint.Frequency;
//                        m_active_timer_slot = wait_slot;
//                        m_timer_kickIDs[wait_slot] = kickID;
//                    }
//                    else
//                    {
//                        return hr;
//                    }
                }
                break;
            }
        }

        slot[0] = m_end_inflight_timer_slots;
        m_end_inflight_timer_slots++;  // TODO
        m_end_inflight_timer_slots %= NumTimerSlots;

        return S_OK;
    }

    HRESULT waitForAllInFlightTimers(){
        switch(m_d3dAPI)
        {
            case nv_water_d3d_api_d3d11:
            {
                // The slot after the active slot is always the first in-flight slot
                for (int slot = m_active_timer_slot; m_end_inflight_timer_slots != (slot %= NumTimerSlots);slot++)
                {
//                    while(_11.m_context->GetData(m_d3d._11.m_frequency_queries[slot], nullptr, 0, 0))  TODO
                    ;
                }
            }
            break;
        }

        return S_OK;
    }

    void add_displacements_float16_d3d11(	Texture2D buffer,
                                             Vector2f[] inSamplePoints,
                                             Vector4f[] outDisplacements,
                                             int numSamples,
                                             float multiplier
    ){
        assert(nv_water_d3d_api_d3d11 == m_d3dAPI);

//        D3D11_MAPPED_SUBRESOURCE msr;
//        m_d3d._11.m_context->Map(buffer, 0, D3D11_MAP_READ, 0, &msr);
//        const BYTE* pRB = reinterpret_cast<BYTE*>(msr.pData);
//        GFSDK_WaveWorks_Simulation_Util::add_displacements_float16(m_params, pRB, msr.RowPitch, inSamplePoints, outDisplacements, numSamples, multiplier);
//        m_d3d._11.m_context->Unmap(buffer, 0);  TODO
    }

    HRESULT addArchivedDisplacementsD3D11(	float coord,
                                              Vector2f[] inSamplePoints,
                                              Vector4f[] outDisplacements,
                                              int numSamples
    ){
        assert(nv_water_d3d_api_d3d11 == m_d3dAPI);

        if(null == _11.m_pReadbackFIFO)
        {
            // No FIFO, nothing to add
            return S_OK;
        }
        else if(0 == _11.m_pReadbackFIFO.range_count())
        {
            // No entries, nothing to add
            return S_OK;
        }

        final float coordMax = _11.m_pReadbackFIFO.range_count()-1;

        // Clamp coord to archived range
        float coord_clamped = coord;
        if(coord_clamped < 0.f)
            coord_clamped = 0.f;
        else if(coord_clamped > coordMax)
            coord_clamped = coordMax;

        // Figure out what interp is required
        final float coord_round = (float) Math.floor(coord_clamped);
        final float coord_frac = coord_clamped - coord_round;
        final int coord_lower = (int)coord_round;
        if(0.f != coord_frac)
        {
            final int coord_upper = coord_lower + 1;

            switch(m_d3dAPI)
            {
                case nv_water_d3d_api_d3d11:
                    add_displacements_float16_d3d11(_11.m_pReadbackFIFO.range_at(coord_lower).buffer, inSamplePoints, outDisplacements, numSamples, 1.f-coord_frac);
                    add_displacements_float16_d3d11(_11.m_pReadbackFIFO.range_at(coord_upper).buffer, inSamplePoints, outDisplacements, numSamples, coord_frac);
                    break;
            }
        }
        else
        {
            switch(m_d3dAPI)
            {
                case nv_water_d3d_api_d3d11:
                    add_displacements_float16_d3d11(_11.m_pReadbackFIFO.range_at(coord_lower).buffer, inSamplePoints, outDisplacements, numSamples, 1.f);
                    break;
            }
        }

        return S_OK;
    }

    // D3D API handling
    private nv_water_d3d_api m_d3dAPI = nv_water_d3d_api.nv_water_d3d_api_undefined;

    NVWaveWorks_FFT_Simulation_DirectCompute_Impl(NVWaveWorks_FFT_Simulation_Manager_DirectCompute_Impl pManager, GFSDK_WaveWorks_Detailed_Simulation_Params.Cascade params){
        for(int slot = 0; slot != NumReadbackSlots; ++slot)
        {
            m_readback_kickIDs[slot] = GFSDK_WaveWorks.GFSDK_WaveWorks_InvalidKickID;
        }
        m_active_readback_slot = 0;
        m_end_inflight_readback_slots = 1;

        for(int slot = 0; slot != NumTimerSlots; ++slot)
        {
            m_timer_kickIDs[slot] = GFSDK_WaveWorks.GFSDK_WaveWorks_InvalidKickID;
            m_timer_results[slot] = 0.f;
        }
        m_active_timer_slot = 0;
        m_end_inflight_timer_slots = 1;

        m_pManager = pManager;
        m_params.set(params);
    }

    @Override
    public boolean initD3D11() {
        HRESULT hr;
        gl = GLFuncProviderFactory.getGLFuncProvider();

        if(nv_water_d3d_api_d3d11 != m_d3dAPI)
        {
            releaseAll();
        }

        if(nv_water_d3d_api_undefined == m_d3dAPI)
        {
            m_d3dAPI = nv_water_d3d_api_d3d11;
//            memset(&m_d3d._11, 0, sizeof(m_d3d._11));
//
//            m_d3d._11.m_device = pD3DDevice;
//            m_d3d._11.m_device->AddRef();
//            m_d3d._11.m_device->GetImmediateContext(&m_d3d._11.m_context);
//
//            V_RETURN(allocateAllResources());

            hr = allocateAllResources();
            if(hr != S_OK)
                return false;
        }

        return true;
    }

    @Override
    public boolean initNoGraphics() {
        return false;
    }

    @Override
    public HRESULT reinit(GFSDK_WaveWorks_Detailed_Simulation_Params.Cascade params) {
        HRESULT hr;

        boolean reallocate = false;

        if(params.fft_resolution != m_params.fft_resolution ||
                params.readback_displacements != m_params.readback_displacements)
        {
            reallocate = true;

            // We're reallocating, which breaks various lockstep/synchronization assumptions...
            hr = m_pManager.beforeReallocateSimulation();
            if(hr != S_OK)
                return hr;
        }

        if(	params.fft_period != m_params.fft_period )
        {
            m_GaussAndOmegaInitialised = false;
        }

        if(	params.wave_amplitude != m_params.wave_amplitude ||
                params.wind_speed != m_params.wind_speed ||
                params.wind_dir.x != m_params.wind_dir.y ||
                params.wind_dir.x != m_params.wind_dir.y ||
                params.wind_dependency != m_params.wind_dependency ||
                params.small_wave_fraction != m_params.small_wave_fraction ||
                params.window_in != m_params.window_in ||
                params.window_out != m_params.window_out )
        {
            m_H0Dirty = true;
        }

        m_params.set(params);

        if(reallocate)
        {
            releaseAllResources();
            hr = allocateAllResources();
            if(hr != S_OK)
                return hr;
        }

        return S_OK;
    }

    @Override
    public HRESULT addDisplacements(Vector2f[] inSamplePoints, Vector4f[] outDisplacements, int numSamples) {
        if(!getReadbackCursor(null))
        {
            return S_OK;
        }

        switch(m_d3dAPI)
        {
            case nv_water_d3d_api_d3d11:
                add_displacements_float16_d3d11(_11.m_active_readback_buffer, inSamplePoints, outDisplacements, numSamples, 1.f);
                break;
        }

        return S_OK;
    }

    @Override
    public HRESULT addArchivedDisplacements(float coord, Vector2f[] inSamplePoints, Vector4f[] outDisplacements, int numSamples) {
        switch(m_d3dAPI)
        {
            case nv_water_d3d_api_d3d11:
                return addArchivedDisplacementsD3D11(coord, inSamplePoints, outDisplacements, numSamples);
            default:
                return E_FAIL;
        }
    }

    @Override
    public HRESULT getTimings(NVWaveWorks_FFT_Simulation_Timings timings) {
        timings.GPU_simulation_time = m_timer_results[m_active_timer_slot];
        timings.GPU_FFT_simulation_time = 0.0f;
        return S_OK;
    }

    @Override
    public long getDisplacementMapVersion() {
        return 0;
    }

    @Override
    public Texture2D GetDisplacementMapD3D11() {
        assert(m_d3dAPI == nv_water_d3d_api_d3d11);
        return _11.m_srv_Displacement;
    }

    @Override
    public int GetDisplacementMapGL2() {
        return 0;
    }

    private int CreateBuffer(int target, int size, int usage){
        int buffer = gl.glGenBuffer();
        gl.glBindBuffer(target, buffer);
        gl.glBufferData(target, size, usage);
        gl.glBindBuffer(target, 0);
        return buffer;
    }

    private void UpdateSubresource(int target, int buffer, float[] data){
        FloatBuffer content = CacheBuffer.wrap(data);
        gl.glBindBuffer(target, buffer);
        gl.glBufferSubData(target, 0, content);
        gl.glBindBuffer(target, 0);
    }

    HRESULT allocateAllResources(){
        HRESULT hr;

        m_resolution = m_params.fft_resolution;
        m_half_resolution_plus_one = m_resolution / 2 + 1;

        int gauss_size = m_resolution * m_resolution;
        int h0_size = (m_resolution + 1) * (m_resolution + 1);
        int omega_size = m_half_resolution_plus_one * m_half_resolution_plus_one;
        int htdt_size = m_half_resolution_plus_one * m_resolution;

        switch(m_d3dAPI)
        {
            case nv_water_d3d_api_d3d11:
            {
//                ID3D11Device* device = m_d3d._11.m_device;

//                D3D11_BUFFER_DESC buffer_desc;
//                memset(&buffer_desc, 0, sizeof(buffer_desc));
//                buffer_desc.BindFlags = D3D11_BIND_SHADER_RESOURCE;
//                buffer_desc.Usage = D3D11_USAGE_DEFAULT;
//                buffer_desc.MiscFlags = D3D11_RESOURCE_MISC_BUFFER_STRUCTURED;

                // Gauss
//                buffer_desc.ByteWidth = gauss_size * sizeof(float2);
//                buffer_desc.StructureByteStride = sizeof(float2);
//                V_RETURN(device->CreateBuffer(&buffer_desc, nullptr, &m_d3d._11.m_buffer_Gauss));
                _11.m_buffer_Gauss = CreateBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, gauss_size * Vector2f.SIZE, GLenum.GL_DYNAMIC_COPY);

                // omega
//                buffer_desc.ByteWidth = omega_size * sizeof(float);
//                buffer_desc.StructureByteStride = sizeof(float);
//                V_RETURN(device->CreateBuffer(&buffer_desc, nullptr, &m_d3d._11.m_buffer_Omega));
                _11.m_buffer_Omega = CreateBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, omega_size * 4, GLenum.GL_DYNAMIC_COPY);

//                buffer_desc.BindFlags = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS;

                // H(0)
//                buffer_desc.ByteWidth = h0_size * sizeof(float2);
//                buffer_desc.StructureByteStride = sizeof(float2);
//                V_RETURN(device->CreateBuffer(&buffer_desc, nullptr, &m_d3d._11.m_buffer_H0));
                _11.m_buffer_H0 = CreateBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, h0_size * Vector2f.SIZE, GLenum.GL_DYNAMIC_COPY);

                // H(t), D(t)
//                buffer_desc.ByteWidth = htdt_size * sizeof(float2);
//                buffer_desc.StructureByteStride = sizeof(float2);
//                V_RETURN(device->CreateBuffer(&buffer_desc, nullptr, &m_d3d._11.m_buffer_Ht));
                _11.m_buffer_Ht = CreateBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, htdt_size * Vector2f.SIZE, GLenum.GL_DYNAMIC_COPY);

//                buffer_desc.ByteWidth = htdt_size * sizeof(float4);
//                buffer_desc.StructureByteStride = sizeof(float4);
//                V_RETURN(device->CreateBuffer(&buffer_desc, nullptr, &m_d3d._11.m_buffer_Dt));
                _11.m_buffer_Dt = CreateBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, htdt_size * Vector4f.SIZE, GLenum.GL_DYNAMIC_COPY);

                // Create displacement maps
                Texture2DDesc texture_desc = new Texture2DDesc();
                texture_desc.width = m_resolution;
                texture_desc.height = m_resolution;
                texture_desc.mipLevels = 1;
                texture_desc.arraySize = 1;
                texture_desc.format = GLenum.GL_RGBA16F;  //DXGI_FORMAT_R16G16B16A16_FLOAT;
//                texture_desc.SampleDesc = kNoSample;
//                texture_desc.Usage = D3D11_USAGE_DEFAULT;
//                texture_desc.BindFlags = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS;
//                texture_desc.CPUAccessFlags = 0;
//                texture_desc.MiscFlags = 0;
//                V_RETURN(device->CreateTexture2D(&texture_desc, NULL, &m_d3d._11.m_texture_Displacement));
                _11.m_texture_Displacement = TextureUtils.createTexture2D(texture_desc, null);


                // constant buffer
//                buffer_desc.ByteWidth = 128;
//                buffer_desc.Usage = D3D11_USAGE_DYNAMIC;
//                buffer_desc.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
//                buffer_desc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
//                buffer_desc.MiscFlags = 0;
//                buffer_desc.StructureByteStride = 0;
//
//                V_RETURN(device->CreateBuffer(&buffer_desc, NULL, &m_d3d._11.m_buffer_constants));
                _11.m_buffer_constants = CreateBuffer(GLenum.GL_UNIFORM_BUFFER, ConstantBuffer.SIZE, GLenum.GL_DYNAMIC_READ);

                if(m_params.readback_displacements)
                {
//                    texture_desc.Usage = D3D11_USAGE_STAGING;
//                    texture_desc.BindFlags = 0;
//                    texture_desc.CPUAccessFlags = D3D11_CPU_ACCESS_READ;
//
//                    D3D11_QUERY_DESC event_query_desc = {D3D11_QUERY_EVENT, 0};

                    for(int slot = 0; slot != NumReadbackSlots; ++slot)
                    {
//                        V_RETURN(device->CreateTexture2D(&texture_desc, nullptr, m_d3d._11.m_readback_buffers + slot));
//                        V_RETURN(device->CreateQuery(&event_query_desc, m_d3d._11.m_readback_queries + slot));
                        _11.m_readback_buffers[slot] = TextureUtils.createTexture2D(texture_desc, null); // TODO
                        _11.m_readback_queries[slot] = gl.glGenQuery();

                        m_readback_kickIDs[slot] = GFSDK_WaveWorks.GFSDK_WaveWorks_InvalidKickID;
                    }
                    m_active_readback_slot = 0;
                    m_end_inflight_readback_slots = 1;
                    _11.m_active_readback_buffer = null;

                    final int num_readback_FIFO_entries = m_params.num_readback_FIFO_entries;
                    if(num_readback_FIFO_entries > 0)
                    {
                        _11.m_pReadbackFIFO = new CircularFIFO<>(num_readback_FIFO_entries, ()->new ReadbackFIFOSlot());
                        for(int i = 0; i != _11.m_pReadbackFIFO.capacity(); ++i)
                        {
                            ReadbackFIFOSlot slot = _11.m_pReadbackFIFO.raw_at(i);
//                            V_RETURN(device->CreateTexture2D(&texture_desc, nullptr, &slot.buffer));
                            slot.buffer = TextureUtils.createTexture2D(texture_desc, null);
                            slot.kickID = GFSDK_WaveWorks.GFSDK_WaveWorks_InvalidKickID;
                        }
                    }

                    m_ReadbackInitialised = true;
                }

                // timers
//                D3D11_QUERY_DESC disjoint_query_desc = {D3D11_QUERY_TIMESTAMP_DISJOINT, 0};
//                D3D11_QUERY_DESC timestamp_query_desc = {D3D11_QUERY_TIMESTAMP, 0};
                for(int slot = 0; slot != NumTimerSlots; ++slot)
                {
//                    device->CreateQuery(&disjoint_query_desc, m_d3d._11.m_frequency_queries + slot);
//                    device->CreateQuery(&timestamp_query_desc, m_d3d._11.m_start_queries + slot);
//                    device->CreateQuery(&timestamp_query_desc, m_d3d._11.m_end_queries + slot);

                    _11.m_frequency_queries[slot] = gl.glGenQuery();
                    _11.m_start_queries[slot] = gl.glGenQuery();
                    _11.m_end_queries[slot] = gl.glGenQuery();

                    m_timer_kickIDs[slot] = GFSDK_WaveWorks.GFSDK_WaveWorks_InvalidKickID;
                    m_timer_results[slot] = 0.f;
                }
                m_active_timer_slot = 0;
                m_end_inflight_timer_slots = 1;

                // shader resource views
//                D3D11_SHADER_RESOURCE_VIEW_DESC srv_desc;
//                srv_desc.Format = DXGI_FORMAT_UNKNOWN;
//                srv_desc.ViewDimension = D3D_SRV_DIMENSION_BUFFER;
//                srv_desc.Buffer.FirstElement = 0;
//
//                srv_desc.Buffer.NumElements = gauss_size;
//                V_RETURN(device->CreateShaderResourceView(_11.m_buffer_Gauss, &srv_desc, &m_d3d._11.m_srv_Gauss));
                _11.m_srv_Gauss = _11.m_buffer_Gauss;

//                srv_desc.Buffer.NumElements = omega_size;
//                V_RETURN(device->CreateShaderResourceView(m_d3d._11.m_buffer_Omega, &srv_desc, &m_d3d._11.m_srv_Omega));
                _11.m_srv_Omega = _11.m_buffer_Omega;

//                srv_desc.Buffer.NumElements = h0_size;
//                V_RETURN(device->CreateShaderResourceView(m_d3d._11.m_buffer_H0, &srv_desc, &m_d3d._11.m_srv_H0));
                _11.m_srv_H0 = _11.m_buffer_H0;

//                srv_desc.Buffer.NumElements = htdt_size;
//                V_RETURN(device->CreateShaderResourceView(m_d3d._11.m_buffer_Ht, &srv_desc, &m_d3d._11.m_srv_Ht));
//                V_RETURN(device->CreateShaderResourceView(m_d3d._11.m_buffer_Dt, &srv_desc, &m_d3d._11.m_srv_Dt));
//                V_RETURN(device->CreateShaderResourceView(m_d3d._11.m_texture_Displacement, NULL, &m_d3d._11.m_srv_Displacement));
                _11.m_srv_Ht = _11.m_buffer_Ht;
                _11.m_srv_Dt = _11.m_buffer_Dt;
                _11.m_srv_Displacement = _11.m_texture_Displacement;

                // unordered access view
//                D3D11_UNORDERED_ACCESS_VIEW_DESC uav_desc;
//                uav_desc.Format = DXGI_FORMAT_UNKNOWN;
//                uav_desc.ViewDimension = D3D11_UAV_DIMENSION_BUFFER;
//                uav_desc.Buffer.FirstElement = 0;
//                uav_desc.Buffer.Flags = 0;
//                uav_desc.Buffer.NumElements = h0_size;
//                V_RETURN(device->CreateUnorderedAccessView(m_d3d._11.m_buffer_H0, &uav_desc, &m_d3d._11.m_uav_H0));
                _11.m_uav_H0 = _11.m_buffer_H0;

//                uav_desc.Buffer.NumElements = htdt_size;
//                V_RETURN(device->CreateUnorderedAccessView(m_d3d._11.m_buffer_Ht, &uav_desc, &m_d3d._11.m_uav_Ht));
//                V_RETURN(device->CreateUnorderedAccessView(m_d3d._11.m_buffer_Dt, &uav_desc, &m_d3d._11.m_uav_Dt));
//                V_RETURN(device->CreateUnorderedAccessView(m_d3d._11.m_texture_Displacement, NULL, &m_d3d._11.m_uav_Displacement));
                _11.m_uav_Ht = _11.m_buffer_Ht;
                _11.m_uav_Dt = _11.m_buffer_Dt;
                _11.m_uav_Displacement = _11.m_texture_Displacement;

                // shaders
//                V_RETURN(device->CreateComputeShader(g_ComputeH0, sizeof(g_ComputeH0), NULL, &m_d3d._11.m_update_h0_shader));  TODO
//                V_RETURN(device->CreateComputeShader(g_ComputeRows, sizeof(g_ComputeRows), NULL, &m_d3d._11.m_row_shader));TODO
//                V_RETURN(device->CreateComputeShader(g_ComputeColumns, sizeof(g_ComputeColumns), NULL, &m_d3d._11.m_column_shader));TODO
                _11.m_update_h0_shader=create("ComputeH0.comp");
                _11.m_row_shader=create("ComputerRows.comp");
                _11.m_column_shader=create("ComputeColumns.comp");

            }
            break;
        }

        // Remaining allocations are deferred, in order to ensure that they occur on the host's simulation thread
        m_GaussAndOmegaInitialised = false;
        m_H0Dirty = true;

        m_DisplacementMapVersion = GFSDK_WaveWorks.GFSDK_WaveWorks_InvalidKickID;

        return S_OK;
    }

    // Create compute shader from the file
    private static final GLSLProgram create(String filename){
        try {
            CharSequence computeSrc = ShaderLoader.loadShaderFile("shader_libs/" + filename, false);
            ShaderSourceItem cs_item = new ShaderSourceItem(computeSrc, ShaderType.COMPUTE);
            return GLSLProgram.createFromShaderItems(cs_item);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    void releaseAllResources(){
        waitForAllInFlightReadbacks();
        waitForAllInFlightTimers();

        switch(m_d3dAPI)
        {
            case nv_water_d3d_api_d3d11:
            {
                gl.glDeleteBuffer(_11.m_buffer_Gauss);
                gl.glDeleteBuffer(_11.m_buffer_Omega);
                gl.glDeleteBuffer(_11.m_buffer_H0);
                gl.glDeleteBuffer(_11.m_buffer_Ht);
                gl.glDeleteBuffer(_11.m_buffer_Dt);
                CommonUtil.safeRelease(_11.m_texture_Displacement);
                gl.glDeleteBuffer(_11.m_buffer_constants);

                gl.glDeleteBuffer(_11.m_srv_Gauss);
                gl.glDeleteBuffer(_11.m_srv_Omega);
                gl.glDeleteBuffer(_11.m_srv_H0);
                gl.glDeleteBuffer(_11.m_srv_Ht);
                gl.glDeleteBuffer(_11.m_srv_Dt);
                CommonUtil.safeRelease(_11.m_srv_Displacement);

                gl.glDeleteBuffer(_11.m_uav_H0);
                gl.glDeleteBuffer(_11.m_uav_Ht);
                gl.glDeleteBuffer(_11.m_uav_Dt);
                CommonUtil.safeRelease(_11.m_uav_Displacement);

                for(int slot = 0; slot != NumReadbackSlots; ++slot)
                {
                    CommonUtil.safeRelease(_11.m_readback_buffers[slot]);
                    gl.glDeleteQuery(_11.m_readback_queries[slot]);
                }

                if(_11.m_pReadbackFIFO != null)
                {
                    for(int i = 0; i != _11.m_pReadbackFIFO.capacity(); ++i)
                    {
                        CommonUtil.safeRelease(_11.m_pReadbackFIFO.raw_at(i).buffer);
                    }
                    _11.m_pReadbackFIFO = null;
                }

                for(int slot = 0; slot != NumTimerSlots; ++slot)
                {
                    gl.glDeleteQuery(_11.m_frequency_queries[slot]);
                    gl.glDeleteQuery(_11.m_start_queries[slot]);
                    gl.glDeleteQuery(_11.m_end_queries[slot]);
                }

                CommonUtil.safeRelease(_11.m_update_h0_shader);
                CommonUtil.safeRelease(_11.m_row_shader);
                CommonUtil.safeRelease(_11.m_column_shader);
            }
            break;
        }

        m_ReadbackInitialised = false;
    }

    void releaseAll(){
        releaseAllResources();

        switch(m_d3dAPI)
        {
            case nv_water_d3d_api_d3d11:
            {
//                SAFE_RELEASE(m_d3d._11.m_device);
//                SAFE_RELEASE(m_d3d._11.m_context);
            }
            break;
        }

        m_d3dAPI = nv_water_d3d_api_undefined;
    }

    HRESULT initGaussAndOmega(){

        int omega_width = m_resolution + 4;
        int gauss_width = gauss_map_resolution + 4;

        float[] gauss = new float[gauss_map_size * 2];
        float[] omega = new float[omega_width * (m_resolution + 1)];

        Simulation_Util.init_gauss(m_params, gauss);
        Simulation_Util.init_omega(m_params, omega);

        // copy actually used gauss window around center of max resolution buffer
        // note that we need to generate full resolution to maintain pseudo-randomness
        int gauss_src = /*gauss +*/ (gauss_map_resolution - m_resolution) / 2 * (1 + gauss_width);
        for(int i=0; i<m_resolution; ++i)
//        memmove(gauss + i * m_resolution, gauss_src + i * gauss_width, m_resolution * sizeof(float2));
        {
            System.arraycopy(gauss, (gauss_src + i * gauss_width) * 2, gauss, (i * m_resolution) * 2, m_resolution * 2);
        }
        // strip unneeded padding
        for(int i=0; i<m_half_resolution_plus_one; ++i)
//        memmove(omega + i * m_half_resolution_plus_one, omega + i * omega_width, m_half_resolution_plus_one * sizeof(float));
            System.arraycopy(omega, i * omega_width, omega, i * m_half_resolution_plus_one, m_half_resolution_plus_one);

        int gauss_size = m_resolution * m_resolution;
        int omega_size = m_half_resolution_plus_one * m_half_resolution_plus_one;

        switch(m_d3dAPI)
        {
            case nv_water_d3d_api_d3d11:
            {
//                CD3D11_BOX gauss_box = CD3D11_BOX(0, 0, 0, gauss_size * sizeof(float2), 1, 1);
//                m_d3d._11.m_context->UpdateSubresource(m_d3d._11.m_buffer_Gauss, 0, &gauss_box, gauss, 0, 0);
                UpdateSubresource(GLenum.GL_SHADER_STORAGE_BUFFER, _11.m_buffer_Gauss, gauss);
//                CD3D11_BOX omega_box = CD3D11_BOX(0, 0, 0, omega_size * sizeof(float), 1, 1);
//                m_d3d._11.m_context->UpdateSubresource(m_d3d._11.m_buffer_Omega, 0, &omega_box, omega, 0, 0);
                UpdateSubresource(GLenum.GL_SHADER_STORAGE_BUFFER, _11.m_buffer_Omega, omega);
            }
            break;
        }

//        SAFE_DELETE_ARRAY(gauss);
//        SAFE_DELETE_ARRAY(omega);

        m_GaussAndOmegaInitialised = true;
        m_H0Dirty = true;

        return S_OK;
    }
    void updateConstantBuffer(double simTime) {
        final float twoPi = 6.28318530718f;
        final float gravity = 9.810f;
        final float sqrtHalf = 0.707106781186f;
        final float euler = 2.71828182846f;

        float fftNorm = (float) Math.pow(m_resolution, -0.25f);
        float philNorm = euler / m_params.fft_period;
        float gravityScale = (float) Math.sqrt(gravity / Math.sqrt(m_params.wind_speed));

        constant_buffer.m_resolution = m_resolution;
        constant_buffer.m_resolution_plus_one = m_resolution + 1;
        constant_buffer.m_half_resolution = m_resolution / 2;
        constant_buffer.m_half_resolution_plus_one = m_resolution / 2 + 1;
        constant_buffer.m_resolution_plus_one_squared_minus_one = (int) (Math.sqrt(m_resolution + 1)) - 1;
        for(int i=0; (1 << i) <= m_resolution; ++i)
            constant_buffer.m_32_minus_log2_resolution = 32 - i;
        constant_buffer.m_window_in = m_params.window_in;
        constant_buffer.m_window_out = m_params.window_out;
        constant_buffer.m_wind_dir.set(m_params.wind_dir);
        constant_buffer.m_wind_dir.normalise();
        constant_buffer.m_frequency_scale = twoPi / m_params.fft_period;
        constant_buffer.m_linear_scale = fftNorm * philNorm * sqrtHalf * m_params.wave_amplitude;
        constant_buffer.m_wind_scale = (float) -Math.sqrt(1 - m_params.wind_dependency);
        constant_buffer.m_root_scale = -0.5f * gravityScale;
        constant_buffer.m_power_scale = (float) (-0.5f / gravityScale * Math.sqrt(m_params.small_wave_fraction));
        constant_buffer.m_time = (float) simTime;
        constant_buffer.m_choppy_scale = m_params.choppy_scale;

        switch(m_d3dAPI)
        {
            case nv_water_d3d_api_d3d11:
            {
//                D3D11_MAPPED_SUBRESOURCE map;
//                m_d3d._11.m_context->Map(m_d3d._11.m_buffer_constants, 0, D3D11_MAP_WRITE_DISCARD, 0, &map);
//                memcpy(map.pData, &constant_buffer, sizeof(constant_buffer));
//                m_d3d._11.m_context->Unmap(m_d3d._11.m_buffer_constants, 0);

                ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(ConstantBuffer.SIZE);
                constant_buffer.store(buffer).flip();

                gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, _11.m_buffer_constants);
                gl.glBufferSubData(GLenum.GL_UNIFORM_BUFFER,0, buffer);
                gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, 0);
            }
            break;
        }
    }

    HRESULT kick(/*Graphics_Context* pGC,*/ double dSimTime, long kickID){
        HRESULT hr;

        if(!m_GaussAndOmegaInitialised)
        {
            hr = initGaussAndOmega();
            if(hr != HRESULT.S_OK)
                return hr;
        }

        final double fModeSimTime = dSimTime * m_params.time_scale;

        int[] timerSlot = new int[1];
        hr = consumeAvailableTimerSlot(timerSlot,kickID);
        if(hr != HRESULT.S_OK)
            return hr;

        int[] readbackSlot = new int[1];
        hr = consumeAvailableReadbackSlot(readbackSlot,kickID);
        if(hr != HRESULT.S_OK)
            return hr;

        switch(m_d3dAPI)
        {
            case nv_water_d3d_api_d3d11:
            {
//                ID3D11DeviceContext* context = m_d3d._11.m_context;

//                context->Begin(m_d3d._11.m_frequency_queries[timerSlot]);
//                context->End(m_d3d._11.m_start_queries[timerSlot]);  TODO

                updateConstantBuffer(fModeSimTime);
//                context->CSSetConstantBuffers(0, 1, &m_d3d._11.m_buffer_constants);
                gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, _11.m_buffer_constants);
                if(m_avoid_frame_depedencies)
                {
//                    float zeros[4] = {};
				/* todo: structured buffers have unknown format, therefore can't be cleared
				if(m_H0Dirty)
					context->ClearUnorderedAccessViewFloat(m_d3d._11.m_uav_H0, zeros);
				context->ClearUnorderedAccessViewFloat(m_d3d._11.m_uav_Ht, zeros);
				context->ClearUnorderedAccessViewFloat(m_d3d._11.m_uav_Dt, zeros);
				*/
//                    context->ClearUnorderedAccessViewFloat(m_d3d._11.m_uav_Displacement, zeros);  TODO
                }

                if(m_H0Dirty)
                {
//                    context->CSSetShader(m_d3d._11.m_update_h0_shader, NULL, 0);
//                    context->CSSetUnorderedAccessViews(0, 1, &m_d3d._11.m_uav_H0, NULL);  TODO
//                    context->CSSetShaderResources(0, 1, &m_d3d._11.m_srv_Gauss);  TODO
//                    context->Dispatch(1, m_resolution, 1);
                    _11.m_update_h0_shader.enable();
                    // TODO
                    gl.glDispatchCompute(1, m_resolution,1);

                    m_H0Dirty = false;
                }

//                context->CSSetShader(m_d3d._11.m_row_shader, NULL, 0);
//                ID3D11UnorderedAccessView* row_uavs[] = { m_d3d._11.m_uav_Ht, m_d3d._11.m_uav_Dt };
//                context->CSSetUnorderedAccessViews(0, 2, row_uavs, NULL);
//                ID3D11ShaderResourceView* row_srvs[] = { m_d3d._11.m_srv_H0, m_d3d._11.m_srv_Omega };
//                context->CSSetShaderResources(0, 2, row_srvs);
//                context->Dispatch(1, m_half_resolution_plus_one, 1);

                _11.m_row_shader.enable();
                // TODO
                gl.glDispatchCompute(1, m_half_resolution_plus_one, 1);

//                context->CSSetShader(m_d3d._11.m_column_shader, NULL, 0);
//                ID3D11UnorderedAccessView* column_uavs[] = { m_d3d._11.m_uav_Displacement, NULL };
//                context->CSSetUnorderedAccessViews(0, 2, column_uavs, NULL);
//                ID3D11ShaderResourceView* column_srvs[] = { m_d3d._11.m_srv_Ht, m_d3d._11.m_srv_Dt };
//                context->CSSetShaderResources(0, 2, column_srvs);
//                context->Dispatch(1, m_resolution, 1);
                _11.m_column_shader.enable();
                // TODO
                gl.glDispatchCompute(1, m_resolution, 1);

                // unbind
//                ID3D11ShaderResourceView* null_srvs[2] = {};
//                context->CSSetShaderResources(0, 2, null_srvs);
//                ID3D11UnorderedAccessView* null_uavs[2] = {};
//                context->CSSetUnorderedAccessViews(0, 2, null_uavs, NULL);
//                context->CSSetShader(NULL, NULL, 0);

                if(m_ReadbackInitialised)
                {
//                    context->CopyResource(m_d3d._11.m_readback_buffers[readbackSlot], m_d3d._11.m_texture_Displacement);  TODO
//                    context->End(m_d3d._11.m_readback_queries[readbackSlot]);  TODO
                }

//                context->End(m_d3d._11.m_end_queries[timerSlot]);  TODO
//                context->End(m_d3d._11.m_frequency_queries[timerSlot]);  TODO
            }
            break;
        }

        // Update displacement map version
        m_DisplacementMapVersion = kickID;

        return S_OK;
    }

    HRESULT collectSingleReadbackResult(boolean blocking){
        if(!m_ReadbackInitialised)
        {
            return S_FALSE;
        }

        final int wait_slot = (m_active_readback_slot + 1) % NumReadbackSlots;

        // Just consume one readback result per check (per function name!)
        if(wait_slot != m_end_inflight_readback_slots)
        {
            if(blocking)
            {
//                while(m_d3d._11.m_context->GetData(m_d3d._11.m_readback_queries[wait_slot], nullptr, 0, 0));  TODO
//                m_active_readback_slot = wait_slot;
//                m_d3d._11.m_active_readback_buffer = m_d3d._11.m_readback_buffers[m_active_readback_slot];
                return S_OK;
            }
            else
            {
                final HRESULT query_result =S_OK;  //_11.m_context->GetData(m_d3d._11.m_readback_queries[wait_slot], nullptr, 0, 0); TODO
                if(S_OK == query_result)
                {
                    m_active_readback_slot = wait_slot;
//                    m_d3d._11.m_active_readback_buffer = m_d3d._11.m_readback_buffers[m_active_readback_slot];
                    return S_OK;
                }
                else  if(query_result ==E_FAIL )
                {
                    return E_FAIL;
                }
            }
        }

        // Nothing in-flight, or else not ready yet
        return S_FALSE;
    }

    boolean getReadbackCursor(long[] pKickID){
        if(!m_params.readback_displacements || !m_ReadbackInitialised)
        {
            return false;
        }

        if(GFSDK_WaveWorks.GFSDK_WaveWorks_InvalidKickID == m_readback_kickIDs[m_active_readback_slot])
        {
            // No results yet
            return false;
        }

        if(pKickID != null)
        {
            pKickID[0] = m_readback_kickIDs[m_active_readback_slot];
        }

        return true;
    }

    boolean hasReadbacksInFlight() {
        if(!m_params.readback_displacements || !m_ReadbackInitialised)
        {
            return false;
        }

        int begin_inflight_readback_slots = (m_active_readback_slot + 1) % NumReadbackSlots;
        return begin_inflight_readback_slots != m_end_inflight_readback_slots;
    }

    HRESULT canCollectSingleReadbackResultWithoutBlocking(){
        if(!m_ReadbackInitialised)
        {
            return S_FALSE;
        }

        final int wait_slot = (m_active_readback_slot + 1) % NumReadbackSlots;
        if(wait_slot == m_end_inflight_readback_slots)
        {
            // Nothing in-flight...
            return S_FALSE;
        }

        // Do the query
        HRESULT query_result =  S_OK;
//                m_d3d._11.m_context->GetData(m_d3d._11.m_readback_queries[wait_slot], nullptr, 0, 0);  TODO

        if(S_OK == query_result)
        {
            // Whaddyaknow, it's ready!
            return S_OK;
        }
        else if(S_FALSE == query_result)
        {
            // Not ready
            return S_FALSE;
        }
        else
        {
            // Fail
            return E_FAIL;
        }
    }

    HRESULT resetReadbacks(){
        HRESULT hr;

        if(!m_ReadbackInitialised)
        {
            // Nothing to reset
            return S_OK;
        }

        hr = waitForAllInFlightReadbacks();
        if(hr != S_OK)  return hr;

        m_active_readback_slot = 0;
        m_end_inflight_readback_slots = 1;
        m_readback_kickIDs[m_active_readback_slot] = GFSDK_WaveWorks.GFSDK_WaveWorks_InvalidKickID;

        switch(m_d3dAPI)
        {
            case nv_water_d3d_api_d3d11:
            {
                _11.m_active_readback_buffer = null;
            }
            break;
        }

        return S_OK;
    }

    HRESULT archiveDisplacements(){
        long[] kickID = {GFSDK_WaveWorks.GFSDK_WaveWorks_InvalidKickID};
        if(getReadbackCursor(kickID) && _11.m_pReadbackFIFO != null)
        {
            // We avoid big memcpys by swapping pointers, specifically we will either evict a FIFO entry or else use a free one and
            // swap it with one of the slots used for in-flight readbacks
            //
            // First job is to check whether the FIFO already contains this result. We know that if it does contain this result,
            // it will be the last one pushed on...
            if(_11.m_pReadbackFIFO.range_count() > 0)
            {
                if(kickID[0] == _11.m_pReadbackFIFO.range_at(0).kickID)
                {
                    // It is an error to archive the same results twice...
                    return E_FAIL;
                }
            }

            // Assuming the current results have not been archived, the next-up readback buffer should match the one we are serving up
            // for addDisplacements...
            assert(_11.m_active_readback_buffer == _11.m_readback_buffers[m_active_readback_slot]);

            ReadbackFIFOSlot slot = _11.m_pReadbackFIFO.consume_one();
            _11.m_readback_buffers[m_active_readback_slot] = slot.buffer;
            slot.buffer = _11.m_active_readback_buffer;
            slot.kickID = kickID[0];
        }

        return S_OK;
    }

    HRESULT consumeAvailableReadbackSlot(int[] slot, long kickID){
        if(!m_ReadbackInitialised)
            return HRESULT.S_OK;

        if(m_active_readback_slot == m_end_inflight_readback_slots)
        {
            switch(m_d3dAPI)
            {
                case nv_water_d3d_api_d3d11:
                {
                    HRESULT hr = HRESULT.S_FALSE;

                    // No slots available - we must wait for the oldest in-flight readback to complete
                    int wait_slot = (m_active_readback_slot + 1) % NumReadbackSlots;
                    int flag = 0;
                    do
                    {
//                        hr = m_d3d._11.m_context->GetData(m_d3d._11.m_readback_queries[wait_slot], nullptr, 0, flag);  TODO
                    } while(S_FALSE == hr);

                    if(hr == S_OK)
                    {
                        m_active_readback_slot = wait_slot;
                        _11.m_active_readback_buffer = _11.m_readback_buffers[m_active_readback_slot];
                    }
                    else
                    {
                        return hr;
                    }
                }
                break;
            }
        }

        slot[0] = m_end_inflight_readback_slots;
        m_end_inflight_readback_slots++; // TODO
        m_end_inflight_readback_slots %= NumReadbackSlots;
        m_readback_kickIDs[slot[0]] = kickID;

        return S_OK;
    }

    HRESULT waitForAllInFlightReadbacks(){
        HRESULT hr;

        // Consume the readbacks
        int wait_slot = (m_active_readback_slot + 1) % NumReadbackSlots;
        while(wait_slot != m_end_inflight_readback_slots)
        {
            hr = collectSingleReadbackResult(true);
            if(hr != S_OK)
                return hr;
            wait_slot = (m_active_readback_slot + 1) % NumReadbackSlots;
        }

        return S_OK;
    }

    private static final class D3D11Objects
    {
//        ID3D11Device* m_device;
//        ID3D11DeviceContext* m_context;

        // The Gauss distribution used to generated H0 (size: N x N).
        int m_buffer_Gauss;
        // Angular frequency (size: N/2+1 x N/2+1).
        int m_buffer_Omega;
        // Initial height field H(0) generated by Phillips spectrum & Gauss distribution (size: N+1 x N+1).
        int m_buffer_H0;
        // Height field H(t) in frequency domain, updated each frame (size: N/2+1 x N).
        int m_buffer_Ht;
        // Choppy fields Dx(t) and Dy(t), updated each frame (size: N/2+1 x N).
        int m_buffer_Dt;
        // Displacement/choppy field (size: N x N).
        Texture2D m_texture_Displacement;
        // per-frame constants (todo: only time is updated every frame, worth splitting?)
        int m_buffer_constants;

        int m_srv_Gauss;
        int m_srv_H0;
        int m_srv_Ht;
        int m_srv_Dt;
        int m_srv_Omega;
        Texture2D m_srv_Displacement;	// (ABGR32F)

        int m_uav_H0;
        int m_uav_Ht;
        int m_uav_Dt;
        Texture2D m_uav_Displacement;

        // readback staging
        Texture2D[] m_readback_buffers = new Texture2D[NumReadbackSlots];
        int[] m_readback_queries = new int[NumReadbackSlots];
        Texture2D m_active_readback_buffer;


        CircularFIFO<ReadbackFIFOSlot> m_pReadbackFIFO;

        // timers
        int[] m_frequency_queries =new int[NumTimerSlots];
        int[] m_start_queries =new int[NumTimerSlots];
        int[] m_end_queries =new int[NumTimerSlots];

        // Shaders
        GLSLProgram m_update_h0_shader;
        GLSLProgram m_row_shader;
        GLSLProgram m_column_shader;
    }

    private static final class ReadbackFIFOSlot
    {
        long kickID;
        Texture2D buffer;
    }

    private static final class ConstantBuffer{
        static final int SIZE = 20 * 4;

        int m_resolution;
        int m_resolution_plus_one;
        int m_half_resolution;
        int m_half_resolution_plus_one;

        int m_resolution_plus_one_squared_minus_one;
        int m_32_minus_log2_resolution;

        float m_window_in;
        float m_window_out;

        final Vector2f m_wind_dir = new Vector2f();
        float m_frequency_scale;
        float m_linear_scale;

        float m_wind_scale;
        float m_root_scale;
        float m_power_scale;

        float m_time;

        float m_choppy_scale;

        ByteBuffer store(ByteBuffer buf){
            buf.putInt(m_resolution);
            buf.putInt(m_resolution_plus_one);
            buf.putInt(m_half_resolution);
            buf.putInt(m_half_resolution_plus_one);
            buf.putInt(m_resolution_plus_one_squared_minus_one);
            buf.putInt(m_32_minus_log2_resolution);

            buf.putFloat(m_window_in);
            buf.putFloat(m_window_out);
            m_wind_dir.store(buf);

            buf.putFloat(m_frequency_scale);
            buf.putFloat(m_linear_scale);
            buf.putFloat(m_wind_scale);
            buf.putFloat(m_root_scale);
            buf.putFloat(m_power_scale);
            buf.putFloat(m_time);
            buf.putFloat(m_choppy_scale);
            buf.putFloat(0);
            buf.putFloat(0);
            buf.putFloat(0);

            return buf;
        }
    }

    private final ConstantBuffer constant_buffer = new ConstantBuffer();
}
