package jet.opengl.demos.gpupro.rvi;

import com.nvidia.developer.opengl.utils.BoundingBox;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.volumetricLighting.LightType;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

// DIRECTIONAL_LIGHT
//   For direct illumination a full-screen quad is rendered deferred into the accumulation render-target
//   of the GBuffer. In order to cast shadows a shadow map is generated. For indirect illumination the
//   voxel-grid of the GLOBAL_ILLUM post-processor is illuminated, whereby the shadow map, that was generated
//   for direct illumination, is reused.
final class DIRECTIONAL_LIGHT extends ILIGHT implements ICONST{
    static final int SIZE = Vector4f.SIZE*3 + Matrix4f.SIZE * 2;

    // data for directional light uniform-buffer
    final Vector3f direction = new Vector3f();
    float multiplier;
    final Vector4f color = new Vector4f();
    final Matrix4f shadowViewProjMatrix = new Matrix4f();
    final Matrix4f shadowViewProjTexMatrix = new Matrix4f();
    float invShadowMapSize;

    GLSLProgram lightShader;
    GLSLProgram shadowMapShader;
    GLSLProgram[] lightGridShaders = new GLSLProgram[2];
    BufferGL uniformBuffer;
    Runnable noneCullRS;
    Runnable backCullRS;
    Runnable defaultDSS;
    Runnable noDepthTestDSS;
    Runnable noColorBS;
    Runnable blendBS;
    RENDER_TARGET_CONFIG rtConfig;

    final Matrix4f shadowTexMatrix = new Matrix4f();
    final Matrix4f shadowProjMatrix = new Matrix4f();
    float frustumRadius;
    float frustumRatio;

    private GLFuncProvider gl;

    boolean Create(ReadableVector3f direction, ReadableVector4f color, float multiplier){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        this.direction.set(direction);
        this.direction.normalise();

        this.color.set(color);
        this.multiplier = multiplier;
        hasShadow = true;

        frustumRatio = 0.18f; // portion of complete shadow frustum, that will be used

        shadowTexMatrix.set(0.5f,0.0f,0.0f,0.0f,
                0.0f,0.5f,0.0f,0.0f,
                0.0f,0.0f,1.0f,0.0f,
                0.5f,0.5f,0.0f,1.0f);

        CalculateFrustum();
        CalculateMatrices();

        // shader for direct illumination
        lightShader = // DEMO::resourceManager->LoadShader("shaders/dirLight.sdr");
            GLOBAL_ILLUM.createProgram("dirLight", true, false, null);
        if(lightShader == null)
            return false;

        // shader for shadow map generation
        shadowMapShader = // DEMO::resourceManager->LoadShader("shaders/shadowMapDir.sdr");
                GLOBAL_ILLUM.createProgram("shadowMapDir", false, false, null);
        if(shadowMapShader == null)
            return false;

        final int FINE_GRID = GridType.FINE_GRID.ordinal();
        final int COARSE_GRID = GridType.COARSE_GRID.ordinal();

        // shader for illumination of fine resolution voxel-grid
        lightGridShaders[FINE_GRID] = // DEMO::resourceManager->LoadShader("shaders/dirLightGrid.sdr",1); // (Permutation 1 = FINE_GRID)
                GLOBAL_ILLUM.createProgram("shadowMapDir", false, false, null);
        if(lightGridShaders[FINE_GRID] == null)
            return false;

        // shader for illumination of coarse resolution voxel-grid
        lightGridShaders[COARSE_GRID] = // DEMO::resourceManager->LoadShader("shaders/dirLightGrid.sdr");
                GLOBAL_ILLUM.createProgram("dirLightGrid", true, false, null);
        if(lightGridShaders[COARSE_GRID] == null)
            return false;

        /*UNIFORM_LIST uniformList;
        uniformList.AddElement("direction",VEC3_DT);
        uniformList.AddElement("multiplier",FLOAT_DT);
        uniformList.AddElement("color",VEC4_DT);
        uniformList.AddElement("shadowViewProjMatrix",MAT4_DT);
        uniformList.AddElement("shadowViewProjTexMatrix",MAT4_DT);
        uniformList.AddElement("invShadowMapSize",FLOAT_DT);
        uniformBuffer = DX11_RENDERER.getInstance().CreateUniformBuffer(LIGHT_UB_BP,uniformList);
        if(!uniformBuffer)
            return false;*/

        uniformBuffer = new BufferGL();
        uniformBuffer.initlize(GLenum.GL_ARRAY_BUFFER, SIZE, null, GLenum.GL_STATIC_DRAW);

        UpdateUniformBuffer();

        /*RASTERIZER_DESC rasterDesc;
        noneCullRS = DX11_RENDERER.getInstance().CreateRasterizerState(rasterDesc);
        if(!noneCullRS)
            return false;*/

        noneCullRS = ()->
        {
            gl.glDisable(GLenum.GL_CULL_FACE);
        };

        /*rasterDesc.cullMode = BACK_CULL;
        backCullRS = DX11_RENDERER.getInstance().CreateRasterizerState(rasterDesc);
        if(!backCullRS)
            return false;*/
        backCullRS = ()->
        {
            gl.glEnable(GLenum.GL_CULL_FACE);
            gl.glCullFace(GLenum.GL_BACK);
            gl.glFrontFace(GLenum.GL_CCW);
        };

        /*DEPTH_STENCIL_DESC depthStencilDesc;
        defaultDSS = DX11_RENDERER.getInstance().CreateDepthStencilState(depthStencilDesc);
        if(!defaultDSS)
            return false;*/
        defaultDSS = ()->
        {
            gl.glEnable(GLenum.GL_DEPTH_TEST);
            gl.glDepthFunc(GLenum.GL_LESS);
            gl.glDepthMask(true);
            gl.glDisable(GLenum.GL_STENCIL_TEST);
        };

        /*depthStencilDesc.depthTest = false;
        depthStencilDesc.depthMask = false;

        // only illuminate actual geometry, not sky
        depthStencilDesc.stencilTest = true;
        depthStencilDesc.stencilRef = 1;
        depthStencilDesc.stencilPassOp = KEEP_STENCIL_OP;

        noDepthTestDSS = DX11_RENDERER.getInstance().CreateDepthStencilState(depthStencilDesc);
        if(!noDepthTestDSS)
            return false;*/

        noDepthTestDSS = ()->
        {
           gl.glDisable(GLenum.GL_DEPTH_TEST);
           gl.glEnable(GLenum.GL_STENCIL_TEST);
           gl.glStencilFunc(GLenum.GL_ALWAYS, 1, 0xFF);
           gl.glStencilOp(GLenum.GL_KEEP, GLenum.GL_INCR, GLenum.GL_KEEP);
           gl.glStencilMask(0xFF);
        };

        /*BLEND_DESC blendDesc;
        blendDesc.colorMask = 0;
        noColorBS = DX11_RENDERER.getInstance().CreateBlendState(blendDesc);
        if(!noColorBS)
            return false;*/
        noColorBS = ()->
        {
            gl.glDisable(GLenum.GL_BLEND);
            gl.glColorMask(false, false, false,false);
        };

        /*blendDesc.colorMask = ALL_COLOR_MASK;
        blendDesc.blend = true;
        blendBS = DX11_RENDERER.getInstance().CreateBlendState(blendDesc);
        if(!blendBS)
            return false;*/
        blendBS = ()->
        {
            gl.glEnable(GLenum.GL_BLEND);
            gl.glColorMask(true, true, true, true);
            gl.glBlendFunc(GLenum.GL_ONE, GLenum.GL_ONE);
        };

        // render direct illumination only into accumulation render-target of GBuffer
        RT_CONFIG_DESC rtcDesc = new RT_CONFIG_DESC();
        rtcDesc.numColorBuffers = 1;
        rtConfig = DX11_RENDERER.getInstance().CreateRenderTargetConfig(rtcDesc);
        if(rtConfig == null)
            return false;

        // cache pointer to GLOBAL_ILLUM post-processor
        globalIllumPP = (GLOBAL_ILLUM)DX11_RENDERER.getInstance().GetPostProcessor("GLOBAL_ILLUM");
        if(globalIllumPP == null)
            return false;

        index = DX11_RENDERER.getInstance().GetNumLights();

        return true;
    }

    @Override
    LightType GetLightType() { return LightType.DIRECTIONAL; }

    @Override
    void Update() {
        if(!active)
            return;
        CalculateMatrices();
        UpdateUniformBuffer();
    }

    void SetDirection(Vector3f direction)
    {
        this.direction.set(direction);
        this.direction.normalise();
    }

    // 4,18, 14忘打卡

    @Override
    void SetupShadowMapSurface(SURFACE surface) {
        surface.renderTarget = DX11_RENDERER.getInstance().GetRenderTarget(SHADOW_MAP_RT_ID);
        surface.renderOrder = RenderOrder.SHADOW_RO;
        surface.light = this;
        surface.rasterizerState = backCullRS;
        surface.depthStencilState = defaultDSS;
        surface.blendState = noColorBS;
        surface.shader = shadowMapShader;
    }

    @Override
    void AddLitSurface() {
        if(!active)
            return;
//        MESH *screenQuadMesh = DX11_RENDERER.getInstance().GetMesh(SCREEN_QUAD_MESH_ID);
        SURFACE surface = new SURFACE();
        surface.renderTarget = DX11_RENDERER.getInstance().GetRenderTarget(GBUFFER_RT_ID);
        surface.renderTargetConfig = rtConfig;
        surface.renderOrder = RenderOrder.ILLUM_RO;
        surface.camera = DX11_RENDERER.getInstance().GetCamera(MAIN_CAMERA_ID);
        surface.vertexBuffer = null; // screenQuadMesh->vertexBuffer;
        surface.primitiveType = GLenum.GL_TRIANGLES; // screenQuadMesh->primitiveType;
        surface.firstIndex = 0;
        surface.numElements = 3; //screenQuadMesh->vertexBuffer->GetVertexCount();
        surface.colorTexture = DX11_RENDERER.getInstance().GetRenderTarget(GBUFFER_RT_ID).GetTexture(1); // albedoGloss
        surface.normalTexture = DX11_RENDERER.getInstance().GetRenderTarget(GBUFFER_RT_ID).GetTexture(2); // normalDepth
        surface.specularTexture = DX11_RENDERER.getInstance().GetRenderTarget(SHADOW_MAP_RT_ID).GetDepthStencilTexture(); // shadow map
        surface.light = this;
        surface.rasterizerState = noneCullRS;
        surface.depthStencilState = noDepthTestDSS;
        surface.blendState = blendBS;
        surface.shader = lightShader;
        surface.renderMode = RenderMode.NON_INDEXED_RM;
        DX11_RENDERER.getInstance().AddSurface(surface);
    }

    @Override
    void AddGridSurfaces() {
        if(!active)
            return;

        // illuminate fine and coarse resolution voxel-grid of GLOBAL_ILLUM post-processor
        for(int i=0;i<2;i++)
        {
            SURFACE surface = new SURFACE();
            surface.renderOrder = RenderOrder.GRID_ILLUM_RO;
            surface.light = this;
            surface.colorTexture = DX11_RENDERER.getInstance().GetRenderTarget(SHADOW_MAP_RT_ID).GetDepthStencilTexture();
            surface.shader = lightGridShaders[i];
            globalIllumPP.SetupLightGridSurface(surface,GridType.values()[i]);
            DX11_RENDERER.getInstance().AddSurface(surface);
        }
    }

    @Override
    BufferGL GetUniformBuffer() {
        return uniformBuffer;
    }

    @Override
    void CalculateMatrices() { }

    @Override
    void UpdateUniformBuffer() {
        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(SIZE);
        store(buffer);
        buffer.flip();

        uniformBuffer.update(0, buffer);
    }

    Vector3f GetDirection()
    {
        return direction;
    }

    void SetColor(ReadableVector4f color){
        this.color.set(color);
    }

    Vector4f GetColor()
    {
        return color;
    }

    void SetMultiplier(float multiplier){this.multiplier = multiplier;}

    float GetMultiplier()
    {
        return multiplier;
    }

    private void CalculateFrustum(){
        // get corners of camera frustum in view-space  
        CAMERA camera = DX11_RENDERER.getInstance().GetCamera(MAIN_CAMERA_ID);

        BoundingBox out = new BoundingBox();
        Vector3f f3PlaneCornerProjSpace = new Vector3f();
        Matrix4f cameraProjToView = camera.GetInvTransposeViewMatrix();

        for(int iClipPlaneCorner=0; iClipPlaneCorner < 8; ++iClipPlaneCorner) {
            f3PlaneCornerProjSpace.set((iClipPlaneCorner & 0x01) != 0 ? +1.f : -1.f,
                    (iClipPlaneCorner & 0x02) != 0 ? +1.f : -1.f,
                    // Since we use complimentary depth buffering,
                    // far plane has depth 0
                    (iClipPlaneCorner & 0x04) != 0 ? 1.f : -1.f);

            Matrix4f.transformCoord(cameraProjToView, f3PlaneCornerProjSpace, f3PlaneCornerProjSpace);  // Transform the position from projection to world space

            out.expandBy(f3PlaneCornerProjSpace);
        }

        Vector3f center = new Vector3f();

        for(int i = 0; i < 8; i++){
            out.corner(i, f3PlaneCornerProjSpace);
            Vector3f.add(center, f3PlaneCornerProjSpace, center);
        }

        center.scale(1.0f/8);
        frustumRadius = 0.0f;
        for(int j=0;j<8;j++)
        {
            out.corner(j, f3PlaneCornerProjSpace);
            float distance = Vector3f.distance(f3PlaneCornerProjSpace, center);
            if(distance>frustumRadius)
                frustumRadius = distance;
        }

        Matrix4f.lookAt(Vector3f.ZERO, direction, Vector3f.Y_AXIS, shadowViewProjMatrix);
        BoundingBox.transform(shadowViewProjMatrix, out, out);
        shadowProjMatrix.setIdentity();
        shadowProjMatrix.m30 = -(out._max.x+out._min.x) * 0.5f;
        shadowProjMatrix.m31 = -(out._max.y+out._min.y) * 0.5f;
        shadowProjMatrix.m32 = -out._max.z;
        Matrix4f.mul(shadowProjMatrix, shadowViewProjMatrix, shadowViewProjMatrix);  // view matrix

        float left = -(out._max.x-out._min.x) * 0.5f;
        float right = -left;
        float bottom = -(out._max.y-out._min.y) * 0.5f;
        float top =-bottom;
        float near = 0;
        float far = out._max.z-out._min.z;

        // calculate shadowProjMatrix
//        Matrix4f.ortho(-frustumRadius,frustumRadius,-frustumRadius,frustumRadius,0.2f,frustumRadius*2.0f, shadowProjMatrix);
        Matrix4f.ortho(left, right, bottom, top, near, far, shadowProjMatrix);
        Matrix4f.mul(shadowProjMatrix, shadowViewProjMatrix, shadowViewProjMatrix);
        Matrix4f.mul(shadowTexMatrix, shadowViewProjMatrix, shadowViewProjTexMatrix);

        float shadowMapSize = (float)DX11_RENDERER.getInstance().GetRenderTarget(SHADOW_MAP_RT_ID).GetWidth();
        invShadowMapSize = 1.0f/shadowMapSize;
    }

    ByteBuffer store(ByteBuffer buf) {
        direction.store(buf);
        buf.putFloat(multiplier);
        color.store(buf);
        shadowViewProjMatrix.store(buf);
        shadowViewProjTexMatrix.store(buf);
        buf.putFloat(invShadowMapSize);
        return null;
    }
}
