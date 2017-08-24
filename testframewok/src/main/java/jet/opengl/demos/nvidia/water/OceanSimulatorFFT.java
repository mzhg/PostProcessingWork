package jet.opengl.demos.nvidia.water;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import jet.opengl.demos.nvidia.waves.GFSDK_WaveWorks_Detailed_Simulation_Params;
import jet.opengl.demos.nvidia.waves.NVWaveWorks_FFT_Simulation_Timings;
import jet.opengl.demos.nvidia.waves.Simulation_Util;
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
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.DebugTools;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/8/23.
 */

public class OceanSimulatorFFT implements WaterWaveSimulator {

    /** 2 in-flight, one usable, one active */
    private static final int NumReadbackSlots = 1;
    /** 2 in-flight, one usable, one active */
    private static final int NumTimerSlots = 1;

    private final GFSDK_WaveWorks_Detailed_Simulation_Params.Cascade m_params = new GFSDK_WaveWorks_Detailed_Simulation_Params.Cascade();

    private  int m_resolution;  // m_params.fft_resolution
    private  int m_half_resolution_plus_one;

    private boolean m_avoid_frame_depedencies = true; // if SLI, currently always true (performance issue)
    private boolean m_GaussAndOmegaInitialised;
    private boolean m_H0Dirty;

    private final D3D11Objects _11 = new D3D11Objects();
    private GLFuncProvider gl;
    private boolean m_bPrintOnce;

    OceanSimulatorFFT(GFSDK_WaveWorks_Detailed_Simulation_Params.Cascade params){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_params.set(params);
        allocateAllResources();
    }

    public void updateParams(GFSDK_WaveWorks_Detailed_Simulation_Params.Cascade params) {
        boolean reallocate = false;

        if(params.fft_resolution != m_params.fft_resolution ||
                params.readback_displacements != m_params.readback_displacements)
        {
            reallocate = true;
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
            allocateAllResources();
        }
    }

    public void getTimings(NVWaveWorks_FFT_Simulation_Timings timings) {
//        timings.GPU_simulation_time = m_timer_results[m_active_timer_slot];
        timings.GPU_FFT_simulation_time = 0.0f;
    }

    public Texture2D GetDisplacementMapD3D11() {
        return _11.m_srv_Displacement;
    }

    private int CreateBuffer(int target, int size, int usage){
        int buffer = gl.glGenBuffer();
        gl.glBindBuffer(target, buffer);
        gl.glBufferData(target, size, usage);
        gl.glBindBuffer(target, 0);
        return buffer;
    }

    private void UpdateSubresource(int target, int buffer, float[] data){
        if(buffer == 0)
            throw new IllegalArgumentException("Invalid buffer: 0.");
        FloatBuffer content = CacheBuffer.wrap(data);
        gl.glBindBuffer(target, buffer);
        gl.glBufferSubData(target, 0, content);
        GLCheck.checkError();
        gl.glBindBuffer(target, 0);
    }

    private void UpdateSubresource(int target, int buffer, Buffer data){
        if(buffer == 0)
            throw new IllegalArgumentException("Invalid buffer: 0.");
        gl.glBindBuffer(target, buffer);
        gl.glBufferSubData(target, 0, data);GLCheck.checkError();
        gl.glBindBuffer(target, 0);
    }

    void allocateAllResources(){
        m_resolution = m_params.fft_resolution;
        m_half_resolution_plus_one = m_resolution / 2 + 1;

        int gauss_size = m_resolution * m_resolution;
        int h0_size = (m_resolution + 1) * (m_resolution + 1);
        int omega_size = m_half_resolution_plus_one * m_half_resolution_plus_one;
        int htdt_size = m_half_resolution_plus_one * m_resolution;

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
        System.out.println("The length of the m_buffer_Gauss is "+gauss_size * Vector2f.SIZE);

        // omega
//                buffer_desc.ByteWidth = omega_size * sizeof(float);
//                buffer_desc.StructureByteStride = sizeof(float);
//                V_RETURN(device->CreateBuffer(&buffer_desc, nullptr, &m_d3d._11.m_buffer_Omega));
        _11.m_buffer_Omega = CreateBuffer(GLenum.GL_SHADER_STORAGE_BUFFER, omega_size * 4, GLenum.GL_DYNAMIC_COPY);
        System.out.println("The length of the m_buffer_Omega is " + omega_size * 4);

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
        _11.m_fbo_displacement = gl.glGenFramebuffer();
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, _11.m_fbo_displacement);
        gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, _11.m_texture_Displacement.getTarget(), _11.m_texture_Displacement.getTexture(), 0);
        gl.glReadBuffer(GLenum.GL_COLOR_ATTACHMENT0);
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER,0);

        // constant buffer
//                buffer_desc.ByteWidth = 128;
//                buffer_desc.Usage = D3D11_USAGE_DYNAMIC;
//                buffer_desc.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
//                buffer_desc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
//                buffer_desc.MiscFlags = 0;
//                buffer_desc.StructureByteStride = 0;
//
//                V_RETURN(device->CreateBuffer(&buffer_desc, NULL, &m_d3d._11.m_buffer_constants));
        _11.m_buffer_constants = CreateBuffer(GLenum.GL_UNIFORM_BUFFER, ConstantBuffer.SIZE, GLenum.GL_DYNAMIC_DRAW);

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
//                        _11.m_readback_buffers[slot] = TextureUtils.createTexture2D(texture_desc, null);
                _11.m_readback_buffers[slot] = new BufferGL();
                _11.m_readback_buffers[slot].initlize(GLenum.GL_PIXEL_PACK_BUFFER,
                        texture_desc.width * texture_desc.height * (int)TextureUtils.measureSizePerPixel(texture_desc.format), null, GLenum.GL_DYNAMIC_READ);
//                        _11.m_readback_queries[slot] = gl.glGenQuery();  TODO we use syn to instead of the D3D11_QUERY_EVENT
                _11.m_readback_rowpitchs[slot] = texture_desc.width * (int)TextureUtils.measureSizePerPixel(texture_desc.format);
            }
            _11.m_active_readback_buffer = null;
        }

        // timers
//                D3D11_QUERY_DESC disjoint_query_desc = {D3D11_QUERY_TIMESTAMP_DISJOINT, 0};
//                D3D11_QUERY_DESC timestamp_query_desc = {D3D11_QUERY_TIMESTAMP, 0};
        for(int slot = 0; slot != NumTimerSlots; ++slot)
        {
//                    device->CreateQuery(&disjoint_query_desc, m_d3d._11.m_frequency_queries + slot);
//                    device->CreateQuery(&timestamp_query_desc, m_d3d._11.m_start_queries + slot);
//                    device->CreateQuery(&timestamp_query_desc, m_d3d._11.m_end_queries + slot);

            _11.m_frequency_queries = gl.glGenQuery();
            _11.m_start_queries = gl.glGenQuery();
            _11.m_end_queries = gl.glGenQuery();

        }

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
//                V_RETURN(device->CreateComputeShader(g_ComputeH0, sizeof(g_ComputeH0), NULL, &m_d3d._11.m_update_h0_shader));
//                V_RETURN(device->CreateComputeShader(g_ComputeRows, sizeof(g_ComputeRows), NULL, &m_d3d._11.m_row_shader));
//                V_RETURN(device->CreateComputeShader(g_ComputeColumns, sizeof(g_ComputeColumns), NULL, &m_d3d._11.m_column_shader));
        _11.m_update_h0_shader=create("ComputeH0.comp");
        _11.m_row_shader=create("ComputeRows.comp");
        _11.m_column_shader=create("ComputeColumns.comp");

        // Remaining allocations are deferred, in order to ensure that they occur on the host's simulation thread
        m_GaussAndOmegaInitialised = false;
        m_H0Dirty = true;
    }

    // Create compute shader from the file
    private static final GLSLProgram create(String filename){
        try {
            CharSequence computeSrc = ShaderLoader.loadShaderFile(WaterWaveSimulator.SHADER_PATH + filename, false);
            ShaderSourceItem cs_item = new ShaderSourceItem(computeSrc, ShaderType.COMPUTE);
            GLSLProgram program =  GLSLProgram.createFromShaderItems(cs_item);
            int dot = filename.lastIndexOf('.');
            if(dot > 0)
                program.setName(filename.substring(0, dot));
            else
                program.setName(filename);
            return program;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    void releaseAllResources(){
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
//                    gl.glDeleteQuery(_11.m_readback_queries[slot]);
        }

        for(int slot = 0; slot != NumTimerSlots; ++slot)
        {
            gl.glDeleteQuery(_11.m_frequency_queries);
            gl.glDeleteQuery(_11.m_start_queries);
            gl.glDeleteQuery(_11.m_end_queries);
        }

        CommonUtil.safeRelease(_11.m_update_h0_shader);
        CommonUtil.safeRelease(_11.m_row_shader);
        CommonUtil.safeRelease(_11.m_column_shader);
    }


    private static int g_init_count;
    void initGaussAndOmegaLoadData(){
        final String FILE_PATH = "E:\\textures\\WaveWorks\\";
        byte[] gauss = DebugTools.loadBytes(FILE_PATH + "gauss" + g_init_count + ".dat");
        byte[] omega = DebugTools.loadBytes(FILE_PATH + "omega" + g_init_count + ".dat");

        System.out.println("gauss.length = " + gauss.length);
        System.out.println("omega.length = " + omega.length);

        ByteBuffer gauss_buffer = CacheBuffer.wrap(gauss);
        UpdateSubresource(GLenum.GL_SHADER_STORAGE_BUFFER, _11.m_buffer_Gauss, gauss_buffer);
        ByteBuffer omega_buffer = CacheBuffer.wrap(omega);
        UpdateSubresource(GLenum.GL_SHADER_STORAGE_BUFFER, _11.m_buffer_Omega, omega_buffer);

        m_GaussAndOmegaInitialised = true;
        m_H0Dirty = true;
        g_init_count++;
    }

    void initGaussAndOmega(){

        int omega_width = m_resolution + 4;
        int gauss_width = Simulation_Util.gauss_map_resolution + 4;

        float[] gauss = new float[Simulation_Util.gauss_map_size * 2];
        float[] omega = new float[omega_width * (m_resolution + 1)];

        Simulation_Util.init_gauss(m_params, gauss);
        Simulation_Util.init_omega(m_params, omega);

        // copy actually used gauss window around center of max resolution buffer
        // note that we need to generate full resolution to maintain pseudo-randomness
        int gauss_src = /*gauss +*/ (Simulation_Util.gauss_map_resolution - m_resolution) / 2 * (1 + gauss_width);
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

//                CD3D11_BOX gauss_box = CD3D11_BOX(0, 0, 0, gauss_size * sizeof(float2), 1, 1);
//                m_d3d._11.m_context->UpdateSubresource(m_d3d._11.m_buffer_Gauss, 0, &gauss_box, gauss, 0, 0);
        FloatBuffer gauss_buffer = CacheBuffer.wrap(gauss, 0, gauss_size * 2);
        UpdateSubresource(GLenum.GL_SHADER_STORAGE_BUFFER, _11.m_buffer_Gauss, gauss_buffer);
//                CD3D11_BOX omega_box = CD3D11_BOX(0, 0, 0, omega_size * sizeof(float), 1, 1);
//                m_d3d._11.m_context->UpdateSubresource(m_d3d._11.m_buffer_Omega, 0, &omega_box, omega, 0, 0);
        FloatBuffer omega_buffer = CacheBuffer.wrap(omega, 0, omega_size);
        UpdateSubresource(GLenum.GL_SHADER_STORAGE_BUFFER, _11.m_buffer_Omega, omega_buffer);

//        SAFE_DELETE_ARRAY(gauss);
//        SAFE_DELETE_ARRAY(omega);

        m_GaussAndOmegaInitialised = true;
        m_H0Dirty = true;
    }

    private static float sqr(float s){ return s*s;}

    private boolean firstFrame;
    //    private static
    private void updateConstantBuffer(double simTime) {
        final float twoPi = 6.28318530718f;
        final float gravity = 9.810f;
        final float sqrtHalf = 0.707106781186f;
        final float euler = 2.71828182846f;

        float fftNorm = (float) Math.pow(m_resolution, -0.25f);
        float philNorm = euler / m_params.fft_period;
        float gravityScale = sqr(gravity / sqr(m_params.wind_speed));

        constant_buffer.m_resolution = m_resolution;
        constant_buffer.m_resolution_plus_one = m_resolution + 1;
        constant_buffer.m_half_resolution = m_resolution / 2;
        constant_buffer.m_half_resolution_plus_one = m_resolution / 2 + 1;
        constant_buffer.m_resolution_plus_one_squared_minus_one = (int) (sqr(m_resolution + 1)) - 1;
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

        if(!firstFrame){
            byte[] bytes = DebugTools.loadBytes("E:/textures/WaveWorks/time.dat");
            constant_buffer.m_time = (float) Numeric.getDouble(bytes, 0);
            firstFrame = true;
        }

//                D3D11_MAPPED_SUBRESOURCE map;
//                m_d3d._11.m_context->Map(m_d3d._11.m_buffer_constants, 0, D3D11_MAP_WRITE_DISCARD, 0, &map);
//                memcpy(map.pData, &constant_buffer, sizeof(constant_buffer));
//                m_d3d._11.m_context->Unmap(m_d3d._11.m_buffer_constants, 0);

        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(ConstantBuffer.SIZE);
        constant_buffer.store(buffer).flip();

        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, _11.m_buffer_constants);
        gl.glBufferSubData(GLenum.GL_UNIFORM_BUFFER,0, buffer);
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, 0);

        if(m_bPrintOnce == false){
            System.out.println("constant_buffer_" + g_save_id + ":\n" + constant_buffer);
        }
    }

    private static int g_save_id = 0;
    void kick(/*Graphics_Context* pGC,*/ double dSimTime, long kickID){

        if(!m_GaussAndOmegaInitialised)
        {
            GLCheck.checkError();
            initGaussAndOmegaLoadData();
            GLCheck.checkError();
        }

        final double fModeSimTime = dSimTime * m_params.time_scale;

//                ID3D11DeviceContext* context = m_d3d._11.m_context;

//                context->Begin(m_d3d._11.m_frequency_queries[timerSlot]);
//                context->End(m_d3d._11.m_start_queries[timerSlot]);
        gl.glQueryCounter(_11.m_start_queries, GLenum.GL_TIMESTAMP);
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
//                    context->ClearUnorderedAccessViewFloat(m_d3d._11.m_uav_Displacement, zeros);
            gl.glClearTexImage(_11.m_uav_Displacement.getTexture(), 0, TextureUtils.measureFormat(_11.m_uav_Displacement.getFormat()),
                    TextureUtils.measureDataType(_11.m_uav_Displacement.getFormat()), (ByteBuffer) null);
        }
        GLCheck.checkError();
        if(m_H0Dirty)
        {
//                    context->CSSetShader(m_d3d._11.m_update_h0_shader, NULL, 0);
//                    context->CSSetUnorderedAccessViews(0, 1, &m_d3d._11.m_uav_H0, NULL);
//                    context->CSSetShaderResources(0, 1, &m_d3d._11.m_srv_Gauss);
//                    context->Dispatch(1, m_resolution, 1);
            _11.m_update_h0_shader.enable();
            if(_11.m_srv_Gauss == 0)
                throw new IllegalArgumentException();

            if(_11.m_uav_H0 == 0)
                throw new IllegalArgumentException();

            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, _11.m_srv_Gauss);  // read-only
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 2, _11.m_uav_H0);  // write-only
            gl.glDispatchCompute(1, m_resolution,1);
            gl.glMemoryBarrier(GLenum.GL_SHADER_STORAGE_BARRIER_BIT);

            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, 0);
            gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 2, 0);
            m_H0Dirty = false;

            if(!m_bPrintOnce){
                System.out.println("m_resolution = " + m_resolution);
                _11.m_update_h0_shader.printPrograminfo();
                Simulation_Util.saveTextData("ComputeH0_" + g_save_id + ".txt", GLenum.GL_SHADER_STORAGE_BUFFER, _11.m_uav_H0, GLenum.GL_RG32F);
                Simulation_Util.saveTextData("GaussGL.txt", GLenum.GL_SHADER_STORAGE_BUFFER, _11.m_srv_Gauss, GLenum.GL_RG32F);
            }
        }

//                context->CSSetShader(m_d3d._11.m_row_shader, NULL, 0);
//                ID3D11UnorderedAccessView* row_uavs[] = { m_d3d._11.m_uav_Ht, m_d3d._11.m_uav_Dt };
//                context->CSSetUnorderedAccessViews(0, 2, row_uavs, NULL);
//                ID3D11ShaderResourceView* row_srvs[] = { m_d3d._11.m_srv_H0, m_d3d._11.m_srv_Omega };
//                context->CSSetShaderResources(0, 2, row_srvs);
//                context->Dispatch(1, m_half_resolution_plus_one, 1);

        _11.m_row_shader.enable();
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, _11.m_srv_H0);
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 2, _11.m_srv_Omega);
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 3, _11.m_uav_Ht);
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 4, _11.m_uav_Dt);

        gl.glDispatchCompute(1, m_half_resolution_plus_one, 1);
        gl.glMemoryBarrier(GLenum.GL_SHADER_STORAGE_BARRIER_BIT);

        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, 0);
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 2, 0);
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 3, 0);
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 4, 0);
        GLCheck.checkError();
        if(!m_bPrintOnce){
            _11.m_row_shader.printPrograminfo();
            Simulation_Util.saveTextData("ComputeRow_Ht_" + g_save_id + ".txt", GLenum.GL_SHADER_STORAGE_BUFFER, _11.m_uav_Ht, GLenum.GL_RG32F);
            Simulation_Util.saveTextData("ComputeRow_Dt_" + g_save_id + ".txt", GLenum.GL_SHADER_STORAGE_BUFFER, _11.m_uav_Dt, GLenum.GL_RG32F);
        }

//                context->CSSetShader(m_d3d._11.m_column_shader, NULL, 0);
//                ID3D11UnorderedAccessView* column_uavs[] = { m_d3d._11.m_uav_Displacement, NULL };
//                context->CSSetUnorderedAccessViews(0, 2, column_uavs, NULL);
//                ID3D11ShaderResourceView* column_srvs[] = { m_d3d._11.m_srv_Ht, m_d3d._11.m_srv_Dt };
//                context->CSSetShaderResources(0, 2, column_srvs);
//                context->Dispatch(1, m_resolution, 1);
        _11.m_column_shader.enable();
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, _11.m_srv_Ht);
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 2, _11.m_srv_Dt);
        gl.glBindImageTexture(0, _11.m_uav_Displacement.getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, _11.m_uav_Displacement.getFormat());

        gl.glDispatchCompute(1, m_resolution, 1);
        gl.glMemoryBarrier(GLenum.GL_SHADER_STORAGE_BARRIER_BIT|GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

        if(!m_bPrintOnce){
            _11.m_column_shader.printPrograminfo();
            Simulation_Util.saveTextData("Displacement_" + g_save_id + ".txt", _11.m_uav_Displacement);
        }

        // unbind
//                ID3D11ShaderResourceView* null_srvs[2] = {};
//                context->CSSetShaderResources(0, 2, null_srvs);
//                ID3D11UnorderedAccessView* null_uavs[2] = {};
//                context->CSSetUnorderedAccessViews(0, 2, null_uavs, NULL);
//                context->CSSetShader(NULL, NULL, 0);
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, 0);
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, 0);
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 2, 0);
        gl.glBindImageTexture(0, 0, 0, false, 0, GLenum.GL_WRITE_ONLY, _11.m_uav_Displacement.getFormat());
        GLCheck.checkError();


//                context->End(m_d3d._11.m_end_queries[timerSlot]);
//                context->End(m_d3d._11.m_frequency_queries[timerSlot]);  TODO There is no frequency_query in OpenGL
        GLCheck.checkError();
        gl.glQueryCounter(_11.m_end_queries, GLenum.GL_TIMESTAMP);
        GLCheck.checkError();

        g_save_id++;
        m_bPrintOnce = true;
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
        BufferGL[] m_readback_buffers = new BufferGL[NumReadbackSlots];
        final int[] m_readback_rowpitchs = new int[NumReadbackSlots];
        final long[] m_readback_queries = new long[NumReadbackSlots];
        BufferGL m_active_readback_buffer;
        int m_active_readback_rowPitch;

        // timers
        int m_frequency_queries;
        int m_start_queries;
        int m_end_queries;

        int m_fbo_displacement;

        // Shaders
        GLSLProgram m_update_h0_shader;
        GLSLProgram m_row_shader;
        GLSLProgram m_column_shader;
    }

    private static final class ReadbackFIFOSlot
    {
        long kickID;
        BufferGL buffer;
        int rowPitch;
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
            buf.putInt(0);
            buf.putInt(0);

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

            return buf;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("ConstantBuffer{");
            sb.append("m_resolution=").append(m_resolution);
            sb.append("\n m_resolution_plus_one=").append(m_resolution_plus_one);
            sb.append("\n m_half_resolution=").append(m_half_resolution);
            sb.append("\n m_half_resolution_plus_one=").append(m_half_resolution_plus_one);
            sb.append("\n m_resolution_plus_one_squared_minus_one=").append(m_resolution_plus_one_squared_minus_one);
            sb.append("\n m_32_minus_log2_resolution=").append(m_32_minus_log2_resolution);
            sb.append("\n m_window_in=").append(m_window_in);
            sb.append("\n m_window_out=").append(m_window_out);
            sb.append("\n m_wind_dir=").append(m_wind_dir);
            sb.append("\n m_frequency_scale=").append(m_frequency_scale);
            sb.append("\n m_linear_scale=").append(m_linear_scale);
            sb.append("\n m_wind_scale=").append(m_wind_scale);
            sb.append("\n m_root_scale=").append(m_root_scale);
            sb.append("\n m_power_scale=").append(m_power_scale);
            sb.append("\n m_time=").append(m_time);
            sb.append("\n m_choppy_scale=").append(m_choppy_scale);
            sb.append('}');
            return sb.toString();
        }
    }

    private final ConstantBuffer constant_buffer = new ConstantBuffer();

    @Override
    public Texture2D getDisplacementMap() {
        return null;
    }

    @Override
    public Texture2D getGradMap() {
        return null;
    }

    @Override
    public Texture2D getNormalMap() {
        return null;
    }

    @Override
    public void updateSimulation(float time) {

    }

    @Override
    public void dispose() {

    }
}
