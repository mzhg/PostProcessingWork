package jet.opengl.demos.amdfx.geometry;

import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.IntBuffer;
import java.util.Map;

import jet.opengl.demos.amdfx.common.CFirstPersonCamera;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StackFloat;
import jet.opengl.postprocessing.util.StackInt;

public class GeometryFXDemo extends NvSampleApp {

    //--------------------------------------------------------------------------------------
// Global variables
//--------------------------------------------------------------------------------------
    private final CFirstPersonCamera g_Camera = new CFirstPersonCamera();                        //
//    CDXUTDialogResourceManager g_DialogResourceManager; // manager for shared resources of dialogs
//    CD3DSettingsDlg g_SettingsDlg;                      // Device settings dialog
//    CDXUTTextHelper *g_pTxtHelper = NULL;

// depth buffer data
    private Texture2D g_depthStencilTexture;
    private Texture2D g_colorTexture;

    /*int warmupFrames;
    std::string benchmarkFilename;
    std::string meshFileName;
    std::string cameraName;*/
    private GLSLProgram fullscreenPs;
    private BufferGL fullscreenConstantBuffer;
    private Texture2D resolutionDependentResources;

    private boolean enableFiltering = true;
    private boolean instrumentIndirectRender;
    private int windowWidth;
    private int windowHeight;
    private boolean generateGeometry =true;
    private int geometryChunkSize = 65535;
    private int geometryChunkSizeVariance = 16384;
    private float frustumCoverage = 0.9f;
    private float frontfaceCoverage = 0.5f;
    private boolean useCameraForBenchmark;
    private boolean emulateMultiIndirectDraw;
    private int shadowMapResolution = 1024;

    private int pipelineStatsTrianglesIn;
    private int pipelineStatsTrianglesOut;
    private int pipelineStatsClustersIn;
    private int pipelineStatsClustersOut;

    private int enabledFilters = 0xFFFFFFFF;

    private boolean benchmarkMode;
    private String meshFileName = "E:\\SDK\\GeometryFX\\amd_geometryfx_sample\\media\\";

    private GeometryFX_Filter staticMeshRenderer_;
    private MeshHandle[] meshHandles_;

    private RenderTargets m_FBO;
    private GLFuncProvider gl;

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        // Setup the camera's view parameters
        /*g_Camera.SetViewParams(
                XMVectorSet(-0.409059107f, -0.047107596f,  0.101811841f, 0.0f),
                XMVectorSet( 0.553191245f, -0.239557669f, -0.090638265f, 0.0f));*/
        if (shadowMapResolution == -1)
        {
            // Setup the camera's projection parameters
//            float fAspectRatio = pBackBufferSurfaceDesc->Width / (FLOAT)pBackBufferSurfaceDesc->Height;
//            g_Camera.SetProjParams(XM_PI / 4, fAspectRatio, 0.1f, 512.0f);
        }
        else
        {
//            g_Camera.SetProjParams(XM_PI / 4, 1.0f, 0.1f, 512.0f);
        }
//        g_Camera.SetScalers(0.005f, 0.5f);
//        g_Camera.SetRotateButtons(true, false, false);
        // Create AMD_SDK resources here
//        g_HUD.OnCreateDevice(pd3dDevice);
//        g_MagnifyTool.OnCreateDevice(pd3dDevice);

        Create(/*pd3dDevice*/);
        GLCheck.checkError();
    }

    @Override
    public void display() {
//        m_FBO.bind();
//        m_FBO.setRenderTextures(CommonUtil.toArray(g_colorTexture, g_depthStencilTexture), null);
        gl.glViewport(0,0, g_colorTexture.getWidth(), g_colorTexture.getHeight());
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glClearColor(0.176f, 0.196f, 0.667f, 0.0f);
        gl.glClearDepthf(1.f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);
        GLCheck.checkError();

//        auto pApp = static_cast<Application *>(pUserContext);
        OnFrameBegin(/*pd3dImmediateContext,*/ g_Camera);
        OnFrameRender(/*pd3dImmediateContext,*/ g_Camera, g_colorTexture);
        OnFrameEnd();GLCheck.checkError();
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <=0 || height <= 0)
            return;

        final float fov = (float) Math.toDegrees(Numeric.PI/4);
        if (shadowMapResolution == -1)
        {
            // Setup the camera's projection parameters
            float fAspectRatio = width / (float)height;
            g_Camera.SetProjParams(fov, fAspectRatio, 0.1f, 512.0f);
        }
        else
        {
            g_Camera.SetProjParams(fov, 1.0f,0.1f, 512.0f);
        }

        if(g_depthStencilTexture != null) {
            g_depthStencilTexture.dispose();
            g_colorTexture.dispose();
        }

        Texture2DDesc desc = new Texture2DDesc(width, height, GLenum.GL_DEPTH_COMPONENT32F);
        g_depthStencilTexture = TextureUtils.createTexture2D(desc, null);

        desc.format = GLenum.GL_RGBA8;
        g_colorTexture = TextureUtils.createTexture2D(desc, null);


        CreateResolutionDependentResources(/*pd3dDevice,*/ width,
                height, 1);
    }

    void GenerateGeometryChunk(final int faceCount, StackFloat vertices, StackInt indices) {
        final int quadCount = faceCount / 2;
        final int rows = (int)(Math.sqrt(quadCount));
        final int fullColumns = (int)(Math.floor((float)(quadCount) / rows));
        final int columns = (int)(Math.ceil((float)(quadCount) / rows));

        final float XM_PI = Numeric.PI;

        for (int i = 0; i < fullColumns + 1; ++i)
        {
            for (int j = 0; j < rows + 1; ++j)
            {
                vertices.push(i);
                vertices.push(j);
                vertices.push((float) (4 * Math.sin(i * XM_PI / rows * 3) * Math.cos(j * XM_PI / rows * 4)));
            }
        }

        for (int i = fullColumns + 1; i < columns + 1; ++i)
        {
            for (int j = 0; j < (quadCount - fullColumns * rows + 1); ++j)
            {
                vertices.push(i);
                vertices.push(j);
                vertices.push(0);
            }
        }

        for (int i = 0; i < fullColumns; ++i)
        {
            for (int j = 0; j < rows; ++j)
            {
                indices.push(j + i * (rows + 1));
                indices.push(j + 1 + i * (rows + 1));
                indices.push(j + (i + 1) * (rows + 1));

                indices.push(j + 1 + i * (rows + 1));
                indices.push(j + 1 + (i + 1) * (rows + 1));
                indices.push(j + (i + 1) * (rows + 1));
            }
        }

        for (int i = fullColumns; i < columns; ++i)
        {
            for (int j = 0; j < (quadCount - fullColumns * rows); ++j)
            {
                indices.push(j + i * (rows + 1));
                indices.push(j + 1 + i * (rows + 1));
                indices.push(j + (i + 1) * (rows + 1));

                indices.push(j + 1 + i * (rows + 1));
                indices.push(j + 1 + (i + 1) * (rows + 1));
                indices.push(j + (i + 1) * (rows + 1));
            }
        }
    }

/**
 Create test geometry.
 */
    MeshHandle[] CreateGeometry(int chunkCount, int chunkSize, int chunkSizeVariance, GeometryFX_Filter meshManager)
    {
        StackFloat[] positions = new StackFloat[chunkCount];
        StackInt[] indices = new StackInt[chunkCount];

        StackInt vertexCountPerMesh = new StackInt();
        StackInt indexCountPerMesh = new StackInt();


        /*std::normal_distribution<float> distribution(
            static_cast<float>(chunkSize), static_cast<float>(chunkSizeVariance));
        std::mt19937 generator;*/

        for (int i = 0; i < chunkCount; ++i)
        {
            positions[i] = new StackFloat();
            indices[i] = new StackInt();

            float count = Numeric.normal_distribution(Numeric.random(), chunkSize, chunkSizeVariance);
            GenerateGeometryChunk(
                    Math.max(32, (int)count), positions[i], indices[i]);
            vertexCountPerMesh.push(positions[i].size() / 3);
            indexCountPerMesh.push(indices[i].size());
        }

        MeshHandle[] handles =
            meshManager.RegisterMeshes(chunkCount, vertexCountPerMesh.getData(), indexCountPerMesh.getData());

        for (int i = 0; i < chunkCount; ++i)
        {
            meshManager.SetMeshData(handles[i], positions[i].getData(), indices[i].getData());
        }

        GLCheck.checkError();
        return handles;
    }

    int GetMeshCount() { return meshHandles_.length; }

    private static int HandleOptioni(Map<String, String> options, String name) {
        /*if (options.find(name) != options.end())
        {
            variable = handler(options.find(name)->second);
            return true;
        }
        else
        {
            return false;
        }*/

        String value = options.get(name);
        if(value != null){
            return Integer.parseInt(value);
        }

        return 0;
    }

    private static float HandleOptionf(Map<String, String> options, String name) {
        String value = options.get(name);
        if(value != null){
            return Float.parseFloat(value);
        }

        return 0;
    }

    private static boolean HandleOptionb(Map<String, String> options, String name) {
        String value = options.get(name);
        if(value != null){
            return value.equals("true") || value.equals("yes");
        }

        return false;
    }

    private static String HandleOptions(Map<String, String> options, String name) {
        String value = options.get(name);
        return value;
    }

    void Setup(Map<String, String> options)
    {
        generateGeometry = HandleOptionb(options, "generate-geometry");
        frustumCoverage = HandleOptionf(options, "frustum-coverage");
        frontfaceCoverage = HandleOptionf(options, "frontface-coverage");
        geometryChunkSize = HandleOptioni(options, "geometry-chunk-size");
        geometryChunkSizeVariance = HandleOptioni(options, "geometry-chunk-size-variance");
        useCameraForBenchmark = HandleOptionb(options, "use-camera-for-benchmark");
        emulateMultiIndirectDraw = HandleOptionb(options, "emulate-multi-indirect-draw");
        shadowMapResolution = HandleOptioni(options, "resolution");

        if ((meshFileName = HandleOptions(options, "mesh")) == null)
        {
            meshFileName = "house.obj";
        }

        enabledFilters = HandleOptioni(options, "enabled-filters");
        enableFiltering = HandleOptionb(options, "enable-filtering");

        /*if (!HandleOption(options, "camera", cameraName))
        {
            cameraName = "camera.bin";
        }

        HandleOption(options, "benchmark", benchmarkMode);
        HandleOption(options, "benchmark-frames", benchmarkFrameCount);
        if (!HandleOption(options, "benchmark-filename", benchmarkFilename))
        {
            benchmarkFilename = "result.txt";
        }

        if (!HandleOption(options, "window-width", windowWidth))
        {
            windowWidth = 1024;
        }

        if (!HandleOption(options, "window-height", windowHeight))
        {
            windowHeight = 1024;
        }*/
    }

    private static final class CameraBlob
    {
        Vector3f eye = new Vector3f();
        Vector3f lookAt = new Vector3f();
        float nearClip, farClip;
    };

    /*void StoreViewProjection(const CBaseCamera &camera) const
    {
        CameraBlob cb;
        cb.eye = camera.GetEyePt();
        cb.lookAt = camera.GetLookAtPt();
        cb.nearClip = camera.GetNearClip();
        cb.farClip = camera.GetFarClip();

        AMD::GeometryFX_WriteBlobToFile(cameraName.c_str(), sizeof(cb), &cb);
    }

    void LoadViewProjection(CBaseCamera &camera)
    {
        const auto blob = AMD::GeometryFX_ReadBlobFromFile(cameraName.c_str());
        const CameraBlob *cb = reinterpret_cast<const CameraBlob *>(blob.data());
        camera.SetViewParams(cb->eye, cb->lookAt);
        camera.SetProjParams(camera.GetFOV(), camera.GetAspect(), cb->nearClip, cb->farClip);
    }*/

    // Create resolution-independent resources
    void Create(/*ID3D11Device *device*/)
    {
//        assert(device);

        GeometryFX_FilterDesc ci = new GeometryFX_FilterDesc();
//        ci.pDevice = device;
        ci.emulateMultiIndirectDraw = emulateMultiIndirectDraw;

        staticMeshRenderer_ = new GeometryFX_Filter(ci);

        if (generateGeometry)
        {
            meshHandles_ = CreateGeometry(
                    384, geometryChunkSize, geometryChunkSizeVariance, staticMeshRenderer_);
        }
        else
        {
            /*std::string pathToMesh = "..\\media\\" + meshFileName;  todo
            meshHandles_ =
                    LoadGeometry(pathToMesh.c_str(), *staticMeshRenderer_, geometryChunkSize);*/
        }

        /*D3D11_BUFFER_DESC desc = {};
        desc.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
        desc.ByteWidth = sizeof(FullscreenConstantBuffer);
        desc.Usage = D3D11_USAGE_DEFAULT;
        device->CreateBuffer(&desc, nullptr, &fullscreenConstantBuffer);*/

        fullscreenConstantBuffer = new BufferGL();
        fullscreenConstantBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, 16, null, GLenum.GL_DYNAMIC_COPY);

        CreateShaders();

        m_FBO = new RenderTargets();
    }

    private static final class FullscreenConstantBuffer {
        int windowWidth;
        int windowHeight;
        int shadowMapWidth;
        int shadowMapHeight;
    }

    void Blit(/*ID3D11DeviceContext *context,*/ Texture2D target)
    {
//        assert(context);
//        assert(target);

        // Set render resources
        /*ID3D11RenderTargetView *renderTargets[] = {target};
        context->OMSetRenderTargets(1, renderTargets, g_depthStencilTexture._dsv);
        context->IASetInputLayout(nullptr);
        context->VSSetShader(fullscreenVs, NULL, 0);
        context->PSSetShader(fullscreenPs, NULL, 0);*/

//        m_FBO.setRenderTextures(CommonUtil.toArray(target, g_depthStencilTexture), null);
//        gl.glViewport(0,0, g_depthStencilTexture.getWidth(), g_depthStencilTexture.getHeight());
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_CULL_FACE);
        fullscreenPs.enable();
        int index = fullscreenPs.getUniformLocation("g_Uniforms");

        IntBuffer buffer = CacheBuffer.wrap(shadowMapResolution, shadowMapResolution, getGLContext().width(), getGLContext().height());
        gl.glUniform4uiv(index, buffer);

        /*FullscreenConstantBuffer fcb = new FullscreenConstantBuffer();
        fcb.shadowMapWidth =    shadowMapResolution;
        fcb.shadowMapHeight =   shadowMapResolution;
        fcb.windowWidth = getGLContext().width();
        fcb.windowHeight = getGLContext().height();

        context->UpdateSubresource(
                fullscreenConstantBuffer.Get(), 0, nullptr, &fcb, sizeof(fcb), sizeof(fcb));

        ID3D11Buffer *buffers[] = {fullscreenConstantBuffer.Get()};

        context->PSSetConstantBuffers(0, 1, buffers);*/

        /*ID3D11ShaderResourceView *resources[] = { resolutionDependentResources.depthShaderView.Get() };
        context->PSSetShaderResources(0, 1, resources);
        context->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
        context->Draw(3, 0);*/

        gl.glBindTextureUnit(0, resolutionDependentResources.getTexture());
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
    }

    void OnFrameRender(CFirstPersonCamera camera, Texture2D renderTarget)
    {
        /*if (benchmarkMode)
        {
            if (useCameraForBenchmark)
            {
                LoadViewProjection(g_Camera);
            }
            benchmarkActive = true;
        }*/

        GeometryFX_FilterRenderOptions options = new GeometryFX_FilterRenderOptions();

        options.enableFiltering = enableFiltering;
        options.enabledFilters = enabledFilters;

        GeometryFX_FilterStatistics filterStatistics = new GeometryFX_FilterStatistics();
        if (instrumentIndirectRender)
        {
            options.statistics = filterStatistics;
        }

        int width, height;
        if (shadowMapResolution == -1)
        {
            width = 1024;
            height = 1024;
        }
        else
        {
            width = height = shadowMapResolution;
        }

        final float XM_PI = Numeric.PI;

//        TIMER_Begin(0, L"Depth pass");
        staticMeshRenderer_.BeginRender(
                /*context,*/ options, camera.GetViewMatrix(), camera.GetProjMatrix(), width, height);

        /*std::mt19937 generator;
        std::uniform_real_distribution<float> dis01(0.0f, 1.0f);
        std::normal_distribution<float> rotYdis((1 - frontfaceCoverage) * XM_PI, XM_PI / 180 * 8);*/

        final Matrix4f world = new Matrix4f();
        int i = 0;
        final int rows = (int)(Math.sqrt(meshHandles_.length));
        for (MeshHandle it : meshHandles_)
        {
            if (generateGeometry)
            {
                final float rotate = Numeric.normal_distribution(Numeric.random(), (1 - frontfaceCoverage) * XM_PI, XM_PI / 180 * 8);  //  XMMatrixRotationY(rotYdis(generator));
                final float scale = 1 / 1024.0f; //XMMatrixScaling(1 / 1024.0f, 1 / 1024.0f, 1 / 1024.0f);
                final Vector3f translate =
                    new Vector3f((1 - frustumCoverage) * 1.66f + i / rows / 16.0f - 0.66f,
                            i % rows / 16.0f - 0.66f, Numeric.random() * 0.001f);

                world.setTranslate(translate.x, translate.y, translate.z);
                world.scale(scale,scale, scale);
                world.rotate(rotate, Vector3f.Y_AXIS);

                staticMeshRenderer_.RenderMesh(it, /*rotate * scale * translate)*/world);
            }
            else
            {
                staticMeshRenderer_.RenderMesh(it, Matrix4f.IDENTITY);
            }
            ++i;
        }
        staticMeshRenderer_.EndRender();
//        TIMER_End();

        pipelineStatsTrianglesIn = (int)filterStatistics.trianglesProcessed;
        pipelineStatsTrianglesOut = (int)filterStatistics.trianglesRendered;
        pipelineStatsClustersIn = (int)filterStatistics.clustersProcessed;
        pipelineStatsClustersOut = (int)filterStatistics.clustersRendered;

        /*D3D11_VIEWPORT viewport = {};
        viewport.MaxDepth = 1.0f;
        viewport.Width = static_cast<float>(DXUTGetWindowWidth());
        viewport.Height = static_cast<float>(DXUTGetWindowHeight());
        context->RSSetViewports(1, &viewport);
*/
        Blit(/*context,*/ renderTarget);
    }

    void OnFrameEnd()
    {
        /*if (benchmarkMode && benchmarkActive)
        {
            if (warmupFrames-- > 0)
            {
                return;
            }

            const auto effectTime = TIMER_GetTime(Gpu, L"Depth pass");
            frameTimes.push_back(effectTime);

            if (frameTimes.size() == benchmarkFrameCount)
            {
                // Write out results, and exit
                std::ofstream result;
                result.open(benchmarkFilename.c_str(), std::ios_base::out | std::ios_base::trunc);
                for (std::vector<double>::const_iterator it = frameTimes.begin(),
                    end = frameTimes.end();
                it != end; ++it)
                {
                    result << *it << "\n";
                }
                result.close();
                exit(0);
            }
        }*/
    }

    void OnFrameBegin(/*ID3D11DeviceContext *context, const*/ CFirstPersonCamera camera)
    {
        pipelineStatsTrianglesIn = 0;
        pipelineStatsTrianglesOut = 0;
        pipelineStatsClustersIn = 0;
        pipelineStatsClustersOut = 0;

        /*context->ClearDepthStencilView(
                resolutionDependentResources.depthView.Get(), D3D11_CLEAR_DEPTH, 1.0f, 0);*/
        gl.glClearTexImage(resolutionDependentResources.getTexture(), 0, GLenum.GL_DEPTH_COMPONENT, GLenum.GL_FLOAT, CacheBuffer.wrap(1.f));

//        context->OMSetRenderTargets(0, nullptr, resolutionDependentResources.depthView.Get());
        m_FBO.bind();
        m_FBO.setRenderTexture(resolutionDependentResources, null);
        gl.glViewport(0,0, resolutionDependentResources.getWidth(),resolutionDependentResources.getHeight());

        if (shadowMapResolution != -1)
        {
            /*D3D11_VIEWPORT viewport = {};
            viewport.MaxDepth = 1.0f;
            viewport.Width = static_cast<float>(shadowMapResolution);
            viewport.Height = static_cast<float>(shadowMapResolution);
            context->RSSetViewports(1, &viewport);*/


        }
    }

    void Destroy()
    {
        /*SAFE_RELEASE(fullscreenPs);
        SAFE_RELEASE(fullscreenVs);

        fullscreenConstantBuffer.Reset();

        delete staticMeshRenderer_;*/
    }

    void CreateResolutionDependentResources(
            /*ID3D11Device *device, const*/ int width, int height, int sampleCount)
    {
//        assert(device);
        assert(width > 0);
        assert(height > 0);
        assert(sampleCount > 0);

        /*if (shadowMapResolution == -1)
        {
            resolutionDependentResources.Create(device, width, height, sampleCount);
        }
        else
        {
            resolutionDependentResources.Create(
                    device, shadowMapResolution, shadowMapResolution, sampleCount);
        }*/

        Texture2DDesc desc = new Texture2DDesc(width, height, GLenum.GL_DEPTH_COMPONENT24);
        resolutionDependentResources = TextureUtils.createTexture2D(desc, null);
    }

    void DestroyResolutionDependentResources()
    {
        resolutionDependentResources.dispose();
    }

    void CreateShaders() {
        /*CompileShader(DXUTGetD3D11Device(), (ID3D11DeviceChild **)&fullscreenVs,
                "..\\src\\Shaders\\GeometryFX_Sample.hlsl", AMD::ShaderType::Vertex, "FullscreenVS");

        CompileShader(DXUTGetD3D11Device(), (ID3D11DeviceChild **)&fullscreenPs,
                "..\\src\\Shaders\\GeometryFX_Sample.hlsl", AMD::ShaderType::Pixel, "FullscreenPS");*/

        final String root = "amdfx\\GeometryFX\\shaders\\";
        fullscreenPs = GLSLProgram.createProgram("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", root+ "Demo_FullscreenPS.frag", null);
    }
}
