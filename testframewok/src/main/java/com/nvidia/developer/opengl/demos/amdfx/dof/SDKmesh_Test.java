package com.nvidia.developer.opengl.demos.amdfx.dof;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.demos.amdfx.common.AMD_Camera;
import com.nvidia.developer.opengl.demos.amdfx.common.AMD_Mesh;
import com.nvidia.developer.opengl.models.DrawMode;
import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.QuadricBuilder;
import com.nvidia.developer.opengl.models.QuadricMesh;
import com.nvidia.developer.opengl.models.QuadricSphere;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;
import java.nio.ByteBuffer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.DebugTools;

/**
 * Created by mazhen'gui on 2017/6/27.
 */

public class SDKmesh_Test extends NvSampleApp {
    AMD_Mesh m_mesh;
    ModelProgram m_modelProgram;

    GLVAO        m_Sphere;
    Texture2D    m_texture;

    final S_MODEL_DESC        g_ModelDesc = new S_MODEL_DESC();
    final AMD_Camera          g_ViewDesc = new AMD_Camera();
    final CalcDOFParams       g_DOFParams = new CalcDOFParams();

    int            g_framebuffer;
    Texture2D      g_appColorBuffer;
    Texture2D      g_appDepthBuffer;
    Texture2D      g_appDofSurface;
    GLSLProgram    g_d3dFullScreenProgram = null;

    GLSLProgram g_pCalcCoc  = null;
    GLSLProgram g_pDebugCoc = null;


    int g_d3dCalcDofCb = 0;

    Texture2D g_appCoCTexture;

    static final int MAX_DOF_RADIUS =  64;
    float        g_FocalLength   = 190.0f;  // in mm
    float        g_FocalDistance = 14.61f;  // in meters
    float        g_sensorWidth   = 100.0f;  // in mm
    float        g_fStop         = 1.8f;
    float        g_forceCoc      = 0.0f;
    int          g_maxRadius     = 57;
    int          g_scale_factor  = 30;
    int          g_box_scale_factor = 24;

    boolean      g_debug_Scene = false;

    //--------------------------------------------------------------------------------------
// D3D11 Common Rendering Interfaces
//--------------------------------------------------------------------------------------
    int g_d3dViewerCB = 0;


    //--------------------------------------------------------------------------------------
// D3D11 Model Rendering Interfaces
//--------------------------------------------------------------------------------------
//    ID3D11InputLayout*  g_d3dModelIL = NULL;
//    ID3D11VertexShader* g_d3dModelVS = NULL;
//    ID3D11PixelShader*  g_d3dModelPS = NULL;
    int          g_d3dModelCB = 0;
    GLSLProgram  g_d3dModelProgram = null;

    DepthOfFieldFXDesc g_AMD_DofFX_Desc;
    GLFuncProvider gl;

    private enum DepthOfFieldMode
    {
        DOF_Disabled                   /*= 0*/,
        DOF_BoxFastFilterSpread        /*= 1*/,
        DOF_FastFilterSpread           /*= 2*/,
        DOF_QuarterResFastFilterSpread /*= 3*/,
    };

    DepthOfFieldMode g_depthOfFieldMode = DepthOfFieldMode.DOF_FastFilterSpread;

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glBindVertexArray(0);
        m_mesh = new AMD_Mesh();
        m_mesh.Create("amdfx/DepthOfFieldFX/models/Tank", "TankScene.sdkmesh", true);

        QuadricBuilder builder = new QuadricBuilder();
        builder.setXSteps(50).setYSteps(50);
        builder.setPostionLocation(0);
        builder.setNormalLocation(1);
        builder.setTexCoordLocation(2);

        builder.setDrawMode(DrawMode.FILL);
        builder.setCenterToOrigin(true);
        m_Sphere = new QuadricMesh(builder, new QuadricSphere()).getModel().genVAO();

        try {
            m_texture = TextureUtils.createTexture2DFromFile("OS/textures/background.jpg", true);

            final String shaderPath = "shader_libs/";
            g_d3dFullScreenProgram = GLSLProgram.createFromFiles(shaderPath + "PostProcessingDefaultScreenSpaceVS.vert",
                    shaderPath + "PostProcessingDefaultScreenSpacePS.frag");
            g_d3dFullScreenProgram.enable();
            g_d3dFullScreenProgram.setTextureUniform("g_InputTex", 0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        m_transformer.setTranslation(0,0,-10);
        m_modelProgram = new ModelProgram();
        compileShaders();

        g_ModelDesc.m_Position    .set(0.0f, 0.0f, 0.0f, 1.0f);
        g_ModelDesc.m_Orientation .set(0.0f, 1.0f, 0.0f, 0.0f);
        g_ModelDesc.m_Scale       .set(0.001f, 0.001f, 0.001f, 1.0f);
        g_ModelDesc.m_Ambient     .set(0.1f, 0.1f, 0.1f, 1.0f);
        g_ModelDesc.m_Diffuse     .set(1.0f, 1.0f, 1.0f, 1.0f);
        g_ModelDesc.m_Specular    .set(0.5f, 0.5f, 0.0f, 1.0f);


        g_d3dModelCB = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, g_d3dModelCB);
        gl.glBufferData(GLenum.GL_UNIFORM_BUFFER, S_MODEL_DESC.SIZE, GLenum.GL_STREAM_DRAW);

        g_d3dViewerCB = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, g_d3dViewerCB);
        gl.glBufferData(GLenum.GL_UNIFORM_BUFFER, AMD_Camera.SIZE, GLenum.GL_STREAM_DRAW);
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, 0);

        g_d3dCalcDofCb = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, g_d3dCalcDofCb);
        gl.glBufferData(GLenum.GL_UNIFORM_BUFFER, CalcDOFParams.SIZE, GLenum.GL_STREAM_DRAW);
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, 0);

        g_AMD_DofFX_Desc = new DepthOfFieldFXDesc();
        g_AMD_DofFX_Desc.m_screenSize.x   = getGLContext().width();
        g_AMD_DofFX_Desc.m_screenSize.y   = getGLContext().height();
        DEPTHOFFIELDFX_RETURN_CODE amdResult = DepthOfFieldFX.DepthOfFieldFX_Initialize(g_AMD_DofFX_Desc);
        if (amdResult != DEPTHOFFIELDFX_RETURN_CODE.DEPTHOFFIELDFX_RETURN_CODE_SUCCESS)
        {
            throw new IllegalStateException();
        }
    }

    @Override
    public void display() {
        if(!g_debug_Scene) {
            updateBuffers();

            renderScene();

            calculateCOC();

            // unbind the resources.
            gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, 0);
            gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 1, 0);
        }

        depthOfFiledPass();

        blitResult(g_appDofSurface);
    }

    private void updateBuffers(){
        Matrix4f view =  m_transformer.getModelViewMat(g_ModelDesc.m_WorldViewProjection);
        Matrix4f.decompseRigidMatrix(view, g_ViewDesc.m_Position,null,null);
        Matrix4f.mul(g_ViewDesc.m_Projection, view, g_ModelDesc.m_WorldViewProjection);

        ByteBuffer data = CacheBuffer.getCachedByteBuffer(S_MODEL_DESC.SIZE);
        g_ModelDesc.store(data).flip();
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, g_d3dModelCB);
        gl.glBufferSubData(GLenum.GL_UNIFORM_BUFFER, 0, data);

        data = CacheBuffer.getCachedByteBuffer(AMD_Camera.SIZE);
        g_ViewDesc.store(data).flip();
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, g_d3dViewerCB);
        gl.glBufferSubData(GLenum.GL_UNIFORM_BUFFER, 0, data);
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, 0);

        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, g_d3dModelCB);
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 1, g_d3dViewerCB);
    }

    private void renderScene(){
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, g_framebuffer);
        gl.glClearColor(0.176f, 0.196f, 0.667f,0);
        gl.glClearDepthf(1.0f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);

        g_d3dModelProgram.enable();
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glDisable(GLenum.GL_BLEND);
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDepthFunc(GLenum.GL_LESS);

//        gl.glActiveTexture(GLenum.GL_TEXTURE0);
//        gl.glBindTexture(m_texture.getTarget(), m_texture.getTexture());
        m_mesh.Render();
    }

    private void calculateCOC(){
        CalcDOFParams pParams = g_DOFParams;
        pParams.focalLength   = g_FocalLength / 1000.0f;
        pParams.focusDistance = g_FocalDistance;
        pParams.fStop         = g_fStop;
        pParams.ScreenParamsX = getGLContext().width();
        pParams.ScreenParamsY = getGLContext().height();
        pParams.zNear         = g_ViewDesc.m_NearPlane;
        pParams.zFar          = g_ViewDesc.m_FarPlane;
        pParams.maxRadius     = g_maxRadius;
        pParams.forceCoc      = g_forceCoc;

        ByteBuffer data = CacheBuffer.getCachedByteBuffer(CalcDOFParams.SIZE);
        pParams.store(data);
        data.flip();

        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, g_d3dCalcDofCb);
        gl.glBufferSubData(GLenum.GL_UNIFORM_BUFFER, 0, data);
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, 0);
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, g_d3dCalcDofCb);

        g_pCalcCoc.enable();

        int threadCountX = (getGLContext().width() + 7) / 8;
        int threadCountY = (getGLContext().height() + 7) / 8;
//        pd3dContext->Dispatch(threadCountX, threadCountY, 1);
//        TIMER_End();

        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(g_appDepthBuffer.getTarget(), g_appDepthBuffer.getTexture());
        gl.glBindImageTexture(0, g_appCoCTexture.getTexture(),0, false, 0, GLenum.GL_READ_WRITE, g_appCoCTexture.getFormat());

        gl.glDispatchCompute(threadCountX, threadCountY, 1);

//        pd3dContext->CSSetUnorderedAccessViews(0, 1, &pNullUAV, NULL);
//        pd3dContext->CSSetShaderResources(0, 1, &pNullSRV);
        gl.glBindTexture(g_appDepthBuffer.getTarget(), 0);
        gl.glBindImageTexture(0, 0,0, false, 0, GLenum.GL_READ_WRITE, g_appCoCTexture.getFormat());
    }

    private void depthOfFiledPass(){
        g_AMD_DofFX_Desc.m_scaleFactor = g_scale_factor;

        switch (g_depthOfFieldMode)
        {
            case DOF_BoxFastFilterSpread:
                g_AMD_DofFX_Desc.m_scaleFactor = g_box_scale_factor;
                DepthOfFieldFX.DepthOfFieldFX_RenderBox(g_AMD_DofFX_Desc);
                break;
            case DOF_FastFilterSpread:
                DepthOfFieldFX.DepthOfFieldFX_Render(g_AMD_DofFX_Desc);
                break;
            case DOF_QuarterResFastFilterSpread:
                DepthOfFieldFX.DepthOfFieldFX_RenderQuarterRes(g_AMD_DofFX_Desc);
                break;
            case DOF_Disabled:
            default:
//                pd3dContext->CopyResource(g_appDofSurface._t2d, g_appColorBuffer._t2d);
                gl.glCopyImageSubData(g_appColorBuffer.getTexture(), g_appColorBuffer.getTarget(), 0, 0,0,0,
                        g_appDofSurface.getTexture(), g_appDofSurface.getTarget(), 0,0,0,0,
                        g_appColorBuffer.getWidth(), g_appColorBuffer.getHeight(), 1);
                break;
        }
    }

    private void blitResult(Texture2D texture){
        /*gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, g_framebuffer);
        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, 0);
        gl.glBlitFramebuffer(0,0, getGLContext().width(), getGLContext().height(),
                0,0, getGLContext().width(), getGLContext().height(),
                GLenum.GL_COLOR_BUFFER_BIT, GLenum.GL_NEAREST);
        gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, 0);*/

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        g_d3dFullScreenProgram.enable();
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(texture.getTarget(), texture.getTexture());
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_CULL_FACE);

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <= 0)
            return;

        g_ViewDesc.m_NearPlane = 0.1f;
        g_ViewDesc.m_FarPlane = 100.0f;
        Matrix4f.perspective(60, (float)width/height, g_ViewDesc.m_NearPlane, g_ViewDesc.m_FarPlane, g_ViewDesc.m_Projection);
        gl.glViewport(0,0, width, height);

        if(g_appColorBuffer != null && (g_appColorBuffer.getWidth() == width) && (g_appColorBuffer.getHeight() == height))
            return;

        TextureDataDesc dataDesc = null;
        final String path = "E:/textures/DepthOfField/";

        // App specific resources
        // scene render target
        CommonUtil.safeRelease(g_appColorBuffer);
//        hr = g_appColorBuffer.CreateSurface(pd3dDevice, pSurfaceDesc->Width, pSurfaceDesc->Height, pSurfaceDesc->SampleDesc.Count, 1, 1, DXGI_FORMAT_R8G8B8A8_TYPELESS, DXGI_FORMAT_R8G8B8A8_UNORM_SRGB,
//                DXGI_FORMAT_R8G8B8A8_UNORM_SRGB, DXGI_FORMAT_UNKNOWN, DXGI_FORMAT_UNKNOWN, DXGI_FORMAT_UNKNOWN, D3D11_USAGE_DEFAULT, false, 0, NULL, NULL, 0);
        if(g_debug_Scene){
            dataDesc = new TextureDataDesc(GLenum.GL_RGBA, GLenum.GL_UNSIGNED_BYTE, DebugTools.loadBinary(path + "colorBuffer.data"));
        }
        g_appColorBuffer = TextureUtils.createTexture2D(new Texture2DDesc(width, height, GLenum.GL_RGBA8), dataDesc);

        // scene depth buffer
        CommonUtil.safeRelease(g_appDepthBuffer);
//        hr = g_appDepthBuffer.CreateSurface(pd3dDevice, pSurfaceDesc->Width, pSurfaceDesc->Height, pSurfaceDesc->SampleDesc.Count, 1, 1, DXGI_FORMAT_R32_TYPELESS, DXGI_FORMAT_R32_FLOAT,
//                DXGI_FORMAT_UNKNOWN, DXGI_FORMAT_D32_FLOAT, DXGI_FORMAT_UNKNOWN, DXGI_FORMAT_UNKNOWN, D3D11_USAGE_DEFAULT, false, 0, NULL, NULL, 0);
        g_appDepthBuffer = TextureUtils.createTexture2D(new Texture2DDesc(width, height, GLenum.GL_DEPTH_COMPONENT32F), null);

        // circle of confusion target
        CommonUtil.safeRelease(g_appCoCTexture);
//        hr = g_appCoCTexture.CreateSurface(pd3dDevice, pSurfaceDesc->Width, pSurfaceDesc->Height, pSurfaceDesc->SampleDesc.Count, 1, 1, DXGI_FORMAT_R16_FLOAT, DXGI_FORMAT_R16_FLOAT, DXGI_FORMAT_UNKNOWN,
//                DXGI_FORMAT_UNKNOWN, DXGI_FORMAT_R16_FLOAT, DXGI_FORMAT_UNKNOWN, D3D11_USAGE_DEFAULT, false, 0, NULL, NULL, 0);
        if(g_debug_Scene){
            dataDesc = new TextureDataDesc(GLenum.GL_RED, GLenum.GL_HALF_FLOAT, DebugTools.loadBinary(path + "CoCTexture.data"));
        }
        g_appCoCTexture = TextureUtils.createTexture2D(new Texture2DDesc(width, height, GLenum.GL_R16F), dataDesc);

        // Depth Of Feild Result surface
        CommonUtil.safeRelease(g_appDofSurface);
//        DXGI_FORMAT appDebugFormat = DXGI_FORMAT_R8G8B8A8_UNORM_SRGB;
//        hr = g_appDofSurface.CreateSurface(pd3dDevice, pSurfaceDesc->Width, pSurfaceDesc->Height, pSurfaceDesc->SampleDesc.Count, 1, 1, DXGI_FORMAT_R8G8B8A8_TYPELESS, appDebugFormat, appDebugFormat,
//                DXGI_FORMAT_UNKNOWN, DXGI_FORMAT_R8G8B8A8_UNORM, DXGI_FORMAT_UNKNOWN, D3D11_USAGE_DEFAULT, false, 0, NULL, NULL, 0);
        g_appDofSurface = TextureUtils.createTexture2D(new Texture2DDesc(width, height, GLenum.GL_RGBA8), null);

        g_AMD_DofFX_Desc.m_screenSize.set(width, height);
        g_AMD_DofFX_Desc.m_pCircleOfConfusionSRV = g_appCoCTexture;
        g_AMD_DofFX_Desc.m_pColorSRV             = g_appColorBuffer;
        g_AMD_DofFX_Desc.m_pResultUAV            = g_appDofSurface;
        g_AMD_DofFX_Desc.m_maxBlurRadius         = g_maxRadius;
        DepthOfFieldFX.DepthOfFieldFX_Resize(g_AMD_DofFX_Desc);

        if(g_framebuffer == 0){
            g_framebuffer = gl.glGenFramebuffer();
        }

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, g_framebuffer);
        gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, g_appColorBuffer.getTarget(), g_appColorBuffer.getTexture(), 0);
        gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_ATTACHMENT, g_appDepthBuffer.getTarget(), g_appDepthBuffer.getTexture(), 0);
        gl.glDrawBuffers(GLenum.GL_COLOR_ATTACHMENT0);
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
    }


    void compileShaders(){
        final String shaderPath = "amdfx/DepthOfFieldFX/shaders/";
        try {
            g_d3dModelProgram = GLSLProgram.createFromFiles(shaderPath + "SampleVS.vert", shaderPath + "SamplePS.frag");
            g_pCalcCoc = create(shaderPath + "CalcDOF.comp");
            g_pDebugCoc = create(shaderPath + "DebugVisDOF.comp");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        final String path = "E:/textures/DepthOfField/";

        final String[] sources = {"IntermediateHorizontalDX.dat", "IntermediateSetupDX.dat", "IntermediateVerticalDX.dat"};
        final String[] destions = {"IntermediateHorizontalDX.txt", "IntermediateSetupDX.txt", "IntermediateVerticalDX.txt"};

        try {
            for(int i = 0; i < 3; i++){
                DebugTools.convertBinaryToText(path+sources[i], GLenum.GL_RGBA32I, 128, path + destions[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void releaseShaders(){
        CommonUtil.safeRelease(g_d3dModelProgram);
        CommonUtil.safeRelease(g_pCalcCoc);
        CommonUtil.safeRelease(g_pDebugCoc);
    }

    private static final GLSLProgram create(String filename) throws IOException{
        CharSequence computeSrc = ShaderLoader.loadShaderFile(filename, false);
        ShaderSourceItem cs_item = new ShaderSourceItem(computeSrc, ShaderType.COMPUTE);
        return GLSLProgram.createFromShaderItems(cs_item);
    }

    private static final class ModelProgram extends GLSLProgram{
//        uniform mat4 m_WorldViewProjection;
//        uniform mat4 m_World;

        private final int worldViewProjectionIndex;
        private final int worldIndex;

        ModelProgram(){
            final String shaderPath = "amdfx/DepthOfFieldFX/shaders/";
            try {
                setSourceFromFiles(shaderPath + "MeshTestVS.vert", shaderPath + "MeshTestPS.frag");
            } catch (IOException e) {
                e.printStackTrace();
            }

            worldViewProjectionIndex = getUniformLocation("m_WorldViewProjection");
            worldIndex = getUniformLocation("m_World");

            if(worldIndex >= 0) {
                enable();
                gl.glUniformMatrix4fv(worldIndex, false, CacheBuffer.wrap(Matrix4f.IDENTITY));
            }
        }

        void setWorldViewProjection(Matrix4f wvp){
            gl.glUniformMatrix4fv(worldViewProjectionIndex, false, CacheBuffer.wrap(wvp));
        }
    }
}
