package jet.opengl.demos.gpupro.rvi;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
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
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.Numeric;

// POINT_LIGHT
//   For direct illumination a sphere geometry is rendered deferred into the accumulation render-target
//   of the GBuffer. For indirect illumination the voxel-grid of the GLOBAL_ILLUM post-processor is
//   illuminated.
//   Since often for point- and spots-lights shadows can be abandoned without causing unpleasant visual
//   effects, in this demo point-lights do not cast shadows. However especially for large point-lights
//   shadow maps have to be used same as for directional lights. According to directional lights the
//   shadow maps, that have been generated for direct illumination, are reused for indirect illumination.
final class POINT_LIGHT extends ILIGHT implements Readable,ICONST {
    static final int SIZE = Vector4f.SIZE * 3 + Matrix4f.SIZE;
    // data for point-light uniform-buffer
    final Vector3f position = new Vector3f();
    float radius;
    final Vector4f color = new Vector4f();
    final Matrix4f worldMatrix = new Matrix4f();
    float multiplier;

    GLSLProgram lightShader;
    GLSLProgram[] lightGridShaders = new GLSLProgram[2];
    BufferGL uniformBuffer;
    Runnable backCullRS;
    Runnable frontCullRS;
    Runnable noDepthWriteDSS;
    Runnable noDepthTestDSS;
    Runnable blendBS;
    RENDER_TARGET_CONFIG rtConfig;
    boolean cameraInVolume;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        position.store(buf);
        buf.putFloat(radius);
        color.store(buf);
        worldMatrix.store(buf);
        buf.putFloat(multiplier);
        buf.putFloat(0);
        buf.putFloat(0);
        buf.putFloat(0);

        return buf;
    }

    private GLFuncProvider gl;

    boolean Create(ReadableVector3f position, float radius, ReadableVector4f color, float multiplier) {
        this.position.set(position);
        this.radius = radius;
        this.color.set(color);
        this.multiplier = multiplier;
        hasShadow = false;
        cameraInVolume = false;

        gl = GLFuncProviderFactory.getGLFuncProvider();

        CalculateMatrices();

        final int FINE_GRID = GridType.FINE_GRID.ordinal();
        final int COARSE_GRID = GridType.COARSE_GRID.ordinal();

        // shader for direct illumination
        lightShader =  // DEMO::resourceManager->LoadShader("shaders/pointLight.sdr");
                GLOBAL_ILLUM.createProgram("pointLight", false, false, null);
        if(lightShader == null)
            return false;

        // shader for illumination of fine resolution voxel-grid
        lightGridShaders[FINE_GRID] = // DEMO::resourceManager->LoadShader("shaders/pointLightGrid.sdr",1); // (Permutation 1 = FINE_GRID)
                GLOBAL_ILLUM.createProgram("pointLightGrid", true, false, CommonUtil.toArray(new Macro("FINE_GRID", 1)));
        if(lightGridShaders[FINE_GRID] == null)
            return false;

        // shader for illumination of coarse resolution voxel-grid
        lightGridShaders[COARSE_GRID] = // DEMO::resourceManager->LoadShader("shaders/pointLightGrid.sdr");
                GLOBAL_ILLUM.createProgram("pointLightGrid", true, false, null);
        if(lightGridShaders[COARSE_GRID] == null)
            return false;

        /*UNIFORM_LIST uniformList;
        uniformList.AddElement("position",VEC3_DT);
        uniformList.AddElement("radius",FLOAT_DT);
        uniformList.AddElement("color",VEC4_DT);
        uniformList.AddElement("worldMatrix",MAT4_DT);
        uniformList.AddElement("multiplier",FLOAT_DT);
        uniformBuffer = DX11_RENDERER.getInstance().CreateUniformBuffer(LIGHT_UB_BP,uniformList);
        if(!uniformBuffer)
            return false;*/
        uniformBuffer = new BufferGL();
        uniformBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, SIZE, null, GLenum.GL_STREAM_READ);

        UpdateUniformBuffer();

        performUpdate = false;

        /*RASTERIZER_DESC rasterDesc;
        rasterDesc.cullMode = BACK_CULL;
        backCullRS = DX11_RENDERER.getInstance().CreateRasterizerState(rasterDesc);
        if(!backCullRS)
            return false;*/

        backCullRS = ()->
        {
            gl.glEnable(GLenum.GL_CULL_FACE);
            gl.glFrontFace(GLenum.GL_CCW);
            gl.glCullFace(GLenum.GL_BACK);
        };

        /*rasterDesc.cullMode = FRONT_CULL;
        frontCullRS = DX11_RENDERER.getInstance().CreateRasterizerState(rasterDesc);
        if(!frontCullRS)
            return false;*/

        frontCullRS = ()->
        {
            gl.glEnable(GLenum.GL_CULL_FACE);
            gl.glFrontFace(GLenum.GL_CCW);
            gl.glCullFace(GLenum.GL_FRONT);
        };

        /*DEPTH_STENCIL_DESC depthStencilDesc;
        depthStencilDesc.depthMask = false;

        // only illuminate actual geometry, not sky
        depthStencilDesc.stencilTest = true;
        depthStencilDesc.stencilRef = 1;
        depthStencilDesc.stencilPassOp = KEEP_STENCIL_OP;

        noDepthWriteDSS = DX11_RENDERER.getInstance().CreateDepthStencilState(depthStencilDesc);
        if(!noDepthWriteDSS)
            return false;*/

        noDepthWriteDSS = ()->
        {
            gl.glEnable(GLenum.GL_DEPTH_TEST);
            gl.glDepthMask(false);
            gl.glDepthFunc(GLenum.GL_LEQUAL);

            gl.glEnable(GLenum.GL_STENCIL_TEST);
            gl.glStencilMask(0xFF);
            gl.glStencilOp(GLenum.GL_KEEP, GLenum.GL_INCR, GLenum.GL_KEEP);
            gl.glStencilFunc(GLenum.GL_ALWAYS, 1, 0xFF);
        };

        /*depthStencilDesc.depthTest = false;
        noDepthTestDSS = DX11_RENDERER.getInstance().CreateDepthStencilState(depthStencilDesc);
        if(!noDepthTestDSS)
            return false;*/

        noDepthTestDSS = ()->
        {
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            gl.glEnable(GLenum.GL_STENCIL_TEST);
            gl.glStencilMask(0xFF);
            gl.glStencilOp(GLenum.GL_KEEP, GLenum.GL_INCR, GLenum.GL_KEEP);
            gl.glStencilFunc(GLenum.GL_ALWAYS, 1, 0xFF);
        };

        /*BLEND_DESC blendDesc;
        blendDesc.blend = true;
        blendBS = DX11_RENDERER.getInstance().CreateBlendState(blendDesc);
        if(!blendBS)
            return false;*/
        blendBS = ()->
        {
            gl.glEnable(GLenum.GL_BLEND);
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
    LightType GetLightType() {
        return LightType.POINT;
    }

    @Override
    void Update() {
        if(!active)
            return;
        cameraInVolume = IsSphereInVolume(DX11_RENDERER.getInstance().GetCamera(MAIN_CAMERA_ID).GetPosition(),10.0f);
        CalculateMatrices();
        UpdateUniformBuffer();
        performUpdate = false;
    }

    void SetPosition(ReadableVector3f position)
    {
        if(this.position.equals(position))
            return;
        this.position.set(position);
        performUpdate = true;
    }

    void SetRadius(float radius)
    {
//        if(IS_EQUAL(this->radius,radius))
        if(Numeric.isClose(this.radius, radius, 0.001f))
            return;
        this.radius = radius;
        performUpdate = true;
    }

    void SetColor(ReadableVector4f color)
    {
        if(this.color.equals(color))
            return;
        this.color.set(color);
        performUpdate = true;
    }

    void SetMultiplier(float multiplier)
    {
        if(this.multiplier ==multiplier)
        return;
        this.multiplier = multiplier;
        performUpdate = true;
    }

    Vector3f GetPosition() { return position;}

    @Override
    void SetupShadowMapSurface(SURFACE surface) { }

    @Override
    void AddLitSurface() {
        if(!active)
            return;
//        MESH *sphereMesh = DX11_RENDERER.getInstance().GetMesh(UNIT_SPHERE_MESH_ID);  todo
        SURFACE surface = new SURFACE();
        surface.renderTarget = DX11_RENDERER.getInstance().GetRenderTarget(GBUFFER_RT_ID);
        surface.renderTargetConfig = rtConfig;
        surface.renderOrder = RenderOrder.ILLUM_RO;
        surface.camera = DX11_RENDERER.getInstance().GetCamera(MAIN_CAMERA_ID);
        surface.vertexBuffer = null; // sphereMesh->vertexBuffer;
        surface.indexBuffer = null; // sphereMesh->indexBuffer;
        surface.primitiveType = 0; // sphereMesh->primitiveType;
        surface.firstIndex = 0;
        surface.numElements = 0; //sphereMesh->indexBuffer->GetIndexCount();
        surface.colorTexture = DX11_RENDERER.getInstance().GetRenderTarget(GBUFFER_RT_ID).GetTexture(1); // albedoGloss
        surface.normalTexture = DX11_RENDERER.getInstance().GetRenderTarget(GBUFFER_RT_ID).GetTexture(2); // normalDepth
        surface.light = this;

        // When camera not in light volume, do depth-testing + back-face culling, otherwise disable
        // depth-testing and do front-face culling.
        if(!cameraInVolume)
        {
            surface.rasterizerState = backCullRS;
            surface.depthStencilState = noDepthWriteDSS;
        }
        else
        {
            surface.rasterizerState = frontCullRS;
            surface.depthStencilState = noDepthTestDSS;
        }

        surface.blendState = blendBS;
        surface.shader = lightShader;
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
            surface.shader = lightGridShaders[i];
            globalIllumPP.SetupLightGridSurface(surface,GridType.values()[i]);
            DX11_RENDERER.getInstance().AddSurface(surface);
        }
    }

    @Override
    BufferGL GetUniformBuffer() {  return uniformBuffer; }

    @Override
    void CalculateMatrices() {
        if(!performUpdate)
            return;

        // slightly increase radius to compensate for low tessellation of the used sphere geometry
        float dilatedRadius = radius+10.0f;

        // calculate worldMatrix of sphere geometry
        /*VECTOR3D scale(dilatedRadius,dilatedRadius,dilatedRadius);
        MATRIX4X4 transMatrix,scaleMatrix;
        transMatrix.SetTranslation(position);
        scaleMatrix.SetScale(scale);
        worldMatrix = transMatrix*scaleMatrix;*/

        worldMatrix.setTranslate(position.x, position.y, position.z);
        worldMatrix.scale(dilatedRadius,dilatedRadius,dilatedRadius);
    }

    @Override
    void UpdateUniformBuffer() {
        if(!performUpdate)
            return;

        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(SIZE);
        store(buffer).flip();

        uniformBuffer.update(0, buffer);
    }

    boolean IsSphereInVolume(ReadableVector3f position,float radius)
    {
        float distance = //(this->position-position).GetLength();
                    Vector3f.distance(this.position, position);
        if(distance<=(this.radius+radius))
            return true;
        return false;
    }
}
