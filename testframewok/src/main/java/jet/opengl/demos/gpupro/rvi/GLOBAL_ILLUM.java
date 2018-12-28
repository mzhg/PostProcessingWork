package jet.opengl.demos.gpupro.rvi;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.SamplerUtils;

// GLOBAL_ILLUM
//   This post-processor performs "Rasterized Voxel based Dynamic Global Illumination".
//   Actually the technique can be divided into 5 steps:
//   1. In an initial step a voxel-representation of the scene geometry is created by
//      utilizing the hardware rasterizer.
//   2. For each light the low resolution voxel-grid is illuminated and from the illuminated
//      voxels virtual point-lights (represented as spherical harmonics) are created.
//   3. The virtual point-lights are propagated iteratively until the required light distribution
//      is achieved. Thereby the previously generated voxel-grid is utilized to perform geometric
//      occlusion for the propagated light.
//   4. With the help of the normal-/ depth-buffer (of the Gbuffer) each visible pixel is illuminated,
//      thereby generating the actual indirect illumination.
//   5. Finally the voxel grid is cleared.
//   The voxel- as well as the virtual point-light grid are consisting of 32x32x32 cells. In order to
//   cover the entire scene, therefore two cascades are used, a fine and a coarse resolution grid.
final class GLOBAL_ILLUM extends IPOST_PROCESSOR implements ICONST{
    // commonly used objects
    DX11_RENDER_TARGET sceneRT;              // GBuffer
    Runnable blendBS;                // use additive blending
    BufferGL gridUniformBuffer;   // uniform buffer with information about the grids

    // objects used for generating the voxel-grids
    DX11_RENDER_TARGET gridRT;               // simple 64x64 RGB8 render-target
    RENDER_TARGET_CONFIG[] gridRTCs = new RENDER_TARGET_CONFIG[2]; // render-target configs for FINE_GRID/ COARSE_GRID
    DX11_STRUCTURED_BUFFER[] gridSBs = new DX11_STRUCTURED_BUFFER[2];// structured buffers for FINE_GRID/ COARSE_GRID
    GLSLProgram[] gridFillShaders = new GLSLProgram[2]; // shaders for FINE_GRID/ COARSE_GRID
    Runnable gridRS;         // default rasterizer state (no culling, solid mode)
    Runnable gridDSS;        // no depth-write/ -test depth-stencil state
    Runnable gridBS;	     // default blend state (blending disabled)

    // objects used for illuminating the voxel-grids
    DX11_RENDER_TARGET[][] lightRTs = new DX11_RENDER_TARGET[2][2];	// two 32x32x32 RGBA16F render-targets for each FINE_GRID/ COARSE_GRID
    int[] currentLightRTIndices = new int[2];    // keep track of currently set render-target for FINE_GRID/ COARSE_GRID

    // objects used for the light propagation
    RENDER_TARGET_CONFIG lightPropagateRTC;  // render-target config for using the compute shader
    GLSLProgram[][] lightPropagateShaders=new GLSLProgram[2][2]; // shaders for FINE_GRID/ COARSE_GRID (with and without occlusion)

    // objects used for generating the indirect illumination
    RENDER_TARGET_CONFIG outputRTC;          // only render into the accumulation render-target of the GBuffer
    GLSLProgram globalIllumShader;           // default shader for generating textured indirect illumination
    GLSLProgram globalIllumNoTexShader;      // shader for visualizing indirect illumination only (without texturing)
    Runnable stencilTestDSS;                 // only illuminate actual geometry, not the sky

    // objects used for visualizing the voxel-grids
    GLSLProgram gridVisShader;               // shader for voxel-grid visualization

    // objects used for clearing the voxel-grids
    DX11_RENDER_TARGET clearRT;              // empty render-target
    RENDER_TARGET_CONFIG clearRTC;           // render-target config to configure above render-target for the compute shader
    GLSLProgram clearShader;                 // shader to clear both voxel-grids (FINE_GRID/ COARSE_GRID)

    // data for grid uniform-buffer
    Matrix4f[] gridViewProjMatrices = new Matrix4f[6];        // viewProjMatrices for generating the voxel-grids
    Vector4f gridCellSizes = new Vector4f();                  // (inverse) sizes of grid-cells (FINE_GRID/ COARSE_GRID)
    Vector4f[] gridPositions = new Vector4f[2];                // center of FINE_GRID/ COARSE_GRID
    Vector4f[] snappedGridPositions = new Vector4f[2];         // center of FINE_GRID/ COARSE_GRID, snapped to the corresponding grid-cell extents

    // helper variables
    float[] gridHalfExtents = new float[2];             // half extents of cubic FINE_GRID/ COARSE_GRID
    final Matrix4f[] gridProjMatrices = new Matrix4f[2];// orthographic projection matrices for FINE_GRID/ COARSE_GRID
    GlobalIllumMode mode =GlobalIllumMode.DEFAULT_GIM;
    boolean useOcclusion = true;

    private final Matrix4f m_View = new Matrix4f();
    private final Matrix4f m_Proj = new Matrix4f();
    private GLFuncProvider gl;

    GLOBAL_ILLUM(){
        name = "GLOBAL_ILLUM";
    }

    @Override
    boolean Create() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        // commonly used objects
        {
            sceneRT = DX11_RENDERER.getInstance().GetRenderTarget(GBUFFER_RT_ID);
            if(sceneRT == null)
                return false;

            /*BLEND_DESC blendDesc;
            blendDesc.blend = true;
            blendBS = DX11_RENDERER.getInstance().CreateBlendState(blendDesc);*/
            blendBS = ()->
            {
                gl.glEnable(GLenum.GL_BLEND);
                gl.glBlendFunc(GLenum.GL_ONE, GLenum.GL_ONE);
            };

           /* UNIFORM_LIST uniformList;  todo
            uniformList.AddElement("gridViewProjMatrices",MAT4_DT,6);
            uniformList.AddElement("gridCellSizes",VEC4_DT);
            uniformList.AddElement("gridPositions",VEC4_DT,2);
            uniformList.AddElement("snappedGridPositions",VEC4_DT,2);
            gridUniformBuffer = DX11_RENDERER.getInstance().CreateUniformBuffer(CUSTOM_UB_BP,uniformList);
            if(!gridUniformBuffer)
                return false;*/

            gridHalfExtents[GridType.FINE_GRID.ordinal()] = 1000.0f;
            gridHalfExtents[GridType.COARSE_GRID.ordinal()] = 1600.0f;

            gridCellSizes.x = gridHalfExtents[GridType.FINE_GRID.ordinal()]/16.0f;
            gridCellSizes.y = 1.0f/gridCellSizes.x;
            gridCellSizes.z = gridHalfExtents[GridType.COARSE_GRID.ordinal()]/16.0f;
            gridCellSizes.w = 1.0f/gridCellSizes.z;

            // for generating the voxel-grids, the scene geometry is rendered with orthographic projection
            for(int i=0;i<2;i++)
            {
//                gridProjMatrices[i].SetOrtho(-gridHalfExtents[i],gridHalfExtents[i],-gridHalfExtents[i],
//                        gridHalfExtents[i],0.2f,2.0f*gridHalfExtents[i]);

                gridProjMatrices[i] = Matrix4f.ortho(-gridHalfExtents[i],gridHalfExtents[i],-gridHalfExtents[i],
                        gridHalfExtents[i],0.2f,2.0f*gridHalfExtents[i], gridProjMatrices[i]);
            }
        }

        // objects used for generating the voxel-grids
        {
            final int TEX_FORMAT_RGB8 = GLenum.GL_RGB8;
            gridRT = DX11_RENDERER.getInstance().CreateRenderTarget(64,64,1,TEX_FORMAT_RGB8, false, 1, 0);
            if(gridRT == null)
                return false;

            gridSBs[GridType.FINE_GRID.ordinal()] = DX11_RENDERER.getInstance().CreateStructuredBuffer(CUSTOM_SB0_BP,32*32*32,6*4);
            if(gridSBs[GridType.FINE_GRID.ordinal()] == null)
                return false;
            gridSBs[GridType.COARSE_GRID.ordinal()] = DX11_RENDERER.getInstance().CreateStructuredBuffer(CUSTOM_SB1_BP,32*32*32,6*4);
            if(gridSBs[GridType.COARSE_GRID.ordinal()] == null)
                return false;

            RT_CONFIG_DESC desc;
            desc.numStructuredBuffers = 1;
            desc.structuredBuffers[0] = gridSBs[GridType.FINE_GRID.ordinal()];
            gridRTCs[GridType.FINE_GRID.ordinal()] = DX11_RENDERER.getInstance().CreateRenderTargetConfig(desc);
            if(gridRTCs[GridType.FINE_GRID.ordinal()] == null)
                return false;
            desc.numStructuredBuffers = 1;
            desc.structuredBuffers[0] = gridSBs[GridType.COARSE_GRID.ordinal()];
            gridRTCs[GridType.COARSE_GRID.ordinal()] = DX11_RENDERER.getInstance().CreateRenderTargetConfig(desc);
            if(gridRTCs[GridType.COARSE_GRID.ordinal()] == null)
                return false;

            gridFillShaders[GridType.FINE_GRID.ordinal()] = DEMO::resourceManager->LoadShader("shaders/gridFill.sdr",1); // (Permutation 1 = FINE_GRID)
            if(gridFillShaders[GridType.FINE_GRID.ordinal()] == null)
                return false;
            gridFillShaders[GridType.COARSE_GRID.ordinal()] = DEMO::resourceManager->LoadShader("shaders/gridFill.sdr");
            if(gridFillShaders[GridType.COARSE_GRID.ordinal()] == null)
                return false;

            /*RASTERIZER_DESC rasterDesc;
            gridRS = DX11_RENDERER.getInstance().CreateRasterizerState(rasterDesc);
            if(!gridRS)
                return false;*/
            gridRS = ()->
            {
                gl.glDisable(GLenum.GL_CULL_FACE);
            };

            // disable depth-write and depth-test, in order to fully "voxelize" scene geometry
            /*DEPTH_STENCIL_DESC depthStencilDesc;
            depthStencilDesc.depthTest = false;
            depthStencilDesc.depthMask = false;
            gridDSS = DX11_RENDERER.getInstance().CreateDepthStencilState(depthStencilDesc);
            if(!gridDSS)
                return false;*/
            gridDSS = ()->
            {
               gl.glDisable(GLenum.GL_DEPTH_TEST);
               gl.glDisable(GLenum.GL_STENCIL_TEST);
               gl.glDepthMask(false);
            };

            // disable color-write since instead of outputting the rasterized voxel information into the bound render-target,
            // it will be written into a 3D structured buffer
            /*BLEND_DESC blendDesc;
            blendDesc.colorMask = 0;
            gridBS = DX11_RENDERER.getInstance().CreateBlendState(blendDesc);
            if(!gridBS)
                return false;*/
            gridBS = ()->
            {
                gl.glDisable(GLenum.GL_BLEND);
                gl.glColorMask(false, false, false, false);
            };
        }

        // objects used for illuminating the voxel-grids
        {
            // Create for each grid resolution a 32x32x32 2D texture array with 3 attached render-targets (one for each second order spherical
            // harmonics coefficients for each color channel). Since this render-targets will be further used for the light propagation step,
            // for each grid resolution two MRTs are required (for iterative rendering).
            for(int i=0;i<2;i++)
            {
                final int TEX_FORMAT_RGBA16F = GLenum.GL_RGBA16F;
                lightRTs[GridType.FINE_GRID.ordinal()][i] = DX11_RENDERER.getInstance().CreateRenderTarget(32,32,32,TEX_FORMAT_RGBA16F,false,3,SamplerUtils.getDefaultSampler());
                if(lightRTs[GridType.FINE_GRID.ordinal()][i] == null)
                    return false;
                lightRTs[GridType.COARSE_GRID.ordinal()][i] = DX11_RENDERER.getInstance().CreateRenderTarget(32,32,32,TEX_FORMAT_RGBA16F,false,3,SamplerUtils.getDefaultSampler());
                if(lightRTs[GridType.COARSE_GRID.ordinal()][i] == null)
                    return false;
            }
        }

        // objects used for the light propagation
        {
            // configure corresponding render-target, to perform light propagation in the compute shader
            RT_CONFIG_DESC desc;
            desc.computeTarget = true;
            desc.numColorBuffers = 3;
            lightPropagateRTC = DX11_RENDERER.getInstance().CreateRenderTargetConfig(desc);
            if(!lightPropagateRTC)
                return false;

            lightPropagateShaders[GridType.FINE_GRID.ordinal()][0] = DEMO::resourceManager->LoadShader("shaders/lightPropagate.sdr",1); // (Permutation 1 = FINE_GRID)
            if(lightPropagateShaders[0]==null)
                return false;
            lightPropagateShaders[GridType.FINE_GRID.ordinal()][1] = DEMO::resourceManager->LoadShader("shaders/lightPropagate.sdr",3); // (Permutation 3 = FINE_GRID + USE_OCCLUSION)
            if(lightPropagateShaders[1]==null)
                return false;
            lightPropagateShaders[GridType.COARSE_GRID.ordinal()][0] = DEMO::resourceManager->LoadShader("shaders/lightPropagate.sdr");
            if(lightPropagateShaders[0]==null)
                return false;
            lightPropagateShaders[GridType.COARSE_GRID.ordinal()][1] = DEMO::resourceManager->LoadShader("shaders/lightPropagate.sdr",2); // (Permutation 2 = USE_OCCLUSION)
            if(lightPropagateShaders[1]==null)
                return false;
        }

        // objects used for generating the indirect illumination
        {
            // only render into the accumulation render-target of the GBuffer
            RT_CONFIG_DESC desc;
            desc.numColorBuffers = 1;
            outputRTC = DX11_RENDERER.getInstance().CreateRenderTargetConfig(desc);
            if(!outputRTC)
                return false;

            globalIllumShader = DEMO::resourceManager->LoadShader("shaders/globalIllum.sdr");
            if(!globalIllumShader)
                return false;
            globalIllumNoTexShader = DEMO::resourceManager->LoadShader("shaders/globalIllum.sdr",1); // (Permutation 1 = NO_TEXTURE)
            if(!globalIllumNoTexShader)
                return false;

            // only illuminate actual scene geometry, not sky
            /*DEPTH_STENCIL_DESC depthStencilDesc;
            depthStencilDesc.stencilTest = true;
            depthStencilDesc.stencilRef = 1;
            depthStencilDesc.stencilPassOp = KEEP_STENCIL_OP;
            stencilTestDSS = DX11_RENDERER.getInstance().CreateDepthStencilState(depthStencilDesc);
            if(!stencilTestDSS)
                return false;*/

            stencilTestDSS = ()->
            {
                gl.glEnable(GLenum.GL_DEPTH_TEST);
                gl.glDepthMask(true);
                gl.glDepthFunc(GLenum.GL_LEQUAL);

                gl.glEnable(GLenum.GL_STENCIL_TEST);
                gl.glStencilFunc(GLenum.GL_ALWAYS, 1, 0xFF);
                gl.glStencilOp(GLenum.GL_KEEP, GLenum.GL_INCR, GLenum.GL_KEEP);
                gl.glStencilMask(0xFF);
            };
        }

        // objects used for visualizing the voxel-grids
        {
            gridVisShader = DEMO::resourceManager->LoadShader("shaders/gridVis.sdr");
            if(!gridVisShader)
                return false;
        }

        // objects used for clearing the voxel-grids
        {
            /*clearRT = DX11_RENDERER.getInstance().CreateRenderTarget(0,0,0,TEX_FORMAT_NONE,false,0);
            if(clearRT == null)  todo
                return false;*/

            // use compute shader for clearing the voxel-grids
            RT_CONFIG_DESC desc = new RT_CONFIG_DESC();
            desc.computeTarget = true;
            desc.numStructuredBuffers = 2;
            desc.structuredBuffers[0] = gridSBs[GridType.FINE_GRID.ordinal()];
            desc.structuredBuffers[1] = gridSBs[GridType.COARSE_GRID.ordinal()];
            clearRTC = DX11_RENDERER.getInstance().CreateRenderTargetConfig(desc);
            if(clearRTC == null)
                return false;

            clearShader = DEMO::resourceManager->LoadShader("shaders/gridClear.sdr");
            if(clearShader == null)
                return false;
        }

        Update();

        return true;
    }

    @Override
    DX11_RENDER_TARGET GetOutputRT() {
        return sceneRT;
    }

    @Override
    void AddSurfaces() {

    }

    // configures surface for generating the voxel-grid
    void SetupGridSurface(SURFACE &surface,GridType gridType);

    // configures surface for illuminating the voxel-grid
    void SetupLightGridSurface(SURFACE &surface,GridType gridType);

    void SetGlobalIllumMode(GlobalIllumMode mode)
    {
        this.mode = mode;
    }

    boolean IsOcclusionEnabled()
    {
        return useOcclusion;
    }

    void EnableOcclusion(boolean enable)
    {
        useOcclusion = enable;
    }

    private void Update(){
        // update data for each grid
        for(int i=0;i<2;i++)
            UpdateGrid(i);

        // update uniform-buffer
        /*float *uniformBufferData = gridViewProjMatrices[0];  todo
        gridUniformBuffer->Update(uniformBufferData);*/
    }

    private void UpdateGrid(int gridType){
//        CAMERA *camera = DEMO::renderer->GetCamera(MAIN_CAMERA_ID);

        // calculate center of grid
        Vector3f tmpGridPosition = new Vector3f();
        Vector3f direction = new Vector3f();
        Matrix4f.decompseRigidMatrix(m_View, tmpGridPosition, null, null, direction);
        direction.scale(-1);
        if(gridType==GridType.FINE_GRID.ordinal()) {
//            tmpGridPosition = camera -> GetPosition() + (camera -> GetDirection() * 0.5f * gridHalfExtents[FINE_GRID]);
            tmpGridPosition.x += direction.x * 0.5f + gridHalfExtents[gridType];
            tmpGridPosition.y += direction.y * 0.5f + gridHalfExtents[gridType];
            tmpGridPosition.z += direction.z * 0.5f + gridHalfExtents[gridType];

        }else {
//            tmpGridPosition = camera -> GetPosition() + (camera -> GetDirection() * (gridHalfExtents[COARSE_GRID] - 0.5f * gridHalfExtents[FINE_GRID]));

            tmpGridPosition.x += direction.x * (gridHalfExtents[GridType.COARSE_GRID.ordinal()] - 0.5f * gridHalfExtents[GridType.FINE_GRID.ordinal()]);
            tmpGridPosition.y += direction.y * (gridHalfExtents[GridType.COARSE_GRID.ordinal()] - 0.5f * gridHalfExtents[GridType.FINE_GRID.ordinal()]);
            tmpGridPosition.z += direction.z * (gridHalfExtents[GridType.COARSE_GRID.ordinal()] - 0.5f * gridHalfExtents[GridType.FINE_GRID.ordinal()]);
        }
        gridPositions[gridType].set(tmpGridPosition);

        // calculate center of grid that is snapped to the grid-cell extents
        Vector3f tmpSnappedGridPosition;
        tmpSnappedGridPosition = tmpGridPosition;
        tmpSnappedGridPosition.scale(gridCellSizes.get(gridType*2+1));
//        tmpSnappedGridPosition.Floor();
        tmpSnappedGridPosition.x = (float) Math.floor(tmpSnappedGridPosition.x);
        tmpSnappedGridPosition.y = (float) Math.floor(tmpSnappedGridPosition.y);
        tmpSnappedGridPosition.z = (float) Math.floor(tmpSnappedGridPosition.z);
        tmpSnappedGridPosition.scale(gridCellSizes.get(gridType*2));
        snappedGridPositions[gridType].set(tmpSnappedGridPosition);

        // back to front viewProjMatrix
        {
            /*VECTOR3D translate = -tmpSnappedGridPosition-VECTOR3D(0.0f,0.0f,gridHalfExtents[gridType]);
            MATRIX4X4 gridTransMatrix;
            gridTransMatrix.SetTranslation(translate);
            gridViewProjMatrices[gridType*3] = gridProjMatrices[gridType]*gridTransMatrix;*/

            float translateX = -tmpSnappedGridPosition.x;
            float translateY = -tmpSnappedGridPosition.y;
            float translateZ = -tmpSnappedGridPosition.z-gridHalfExtents[gridType];

            gridViewProjMatrices[gridType*3].load(gridProjMatrices[gridType]);
            gridViewProjMatrices[gridType*3].translate(translateX, translateY, translateZ);  // TODO
        }

        // right to left viewProjMatrix
        {
            /*VECTOR3D translate = -tmpSnappedGridPosition-VECTOR3D(gridHalfExtents[gridType],0.0f,0.0f);
            MATRIX4X4 gridTransMatrix;
            gridTransMatrix.SetTranslation(translate);
            MATRIX4X4 gridXRotMatrix;
            gridXRotMatrix.SetRotation(VECTOR3D(0.0f,1.0f,0.0f),90.0f);
            MATRIX4X4 gridViewMatrix;
            gridViewMatrix = gridXRotMatrix*gridTransMatrix;
            gridViewProjMatrices[gridType*3+1] = gridProjMatrices[gridType]*gridViewMatrix;*/

            float translateX = -tmpSnappedGridPosition.x-gridHalfExtents[gridType];
            float translateY = -tmpSnappedGridPosition.y;
            float translateZ = -tmpSnappedGridPosition.z;

            gridViewProjMatrices[gridType*3+1].load(gridProjMatrices[gridType]);
            gridViewProjMatrices[gridType*3+1].rotate((float)Math.toRadians(90), Vector3f.Y_AXIS);
            gridViewProjMatrices[gridType*3+1].translate(translateX, translateY, translateZ);
        }

        // top to down viewProjMatrix
        {
            /*VECTOR3D translate = -tmpSnappedGridPosition-VECTOR3D(0.0f,gridHalfExtents[gridType],0.0f);
            MATRIX4X4 gridTransMatrix;
            gridTransMatrix.SetTranslation(translate);
            MATRIX4X4 gridYRotMatrix;
            gridYRotMatrix.SetRotation(VECTOR3D(1.0f,0.0f,0.0f),90.0f);
            MATRIX4X4 gridViewMatrix;
            gridViewMatrix = gridYRotMatrix*gridTransMatrix;
            gridViewProjMatrices[gridType*3+2] = gridProjMatrices[gridType]*gridViewMatrix;*/

            float translateX = -tmpSnappedGridPosition.x;
            float translateY = -tmpSnappedGridPosition.y-gridHalfExtents[gridType];
            float translateZ = -tmpSnappedGridPosition.z;

            gridViewProjMatrices[gridType*3+2].load(gridProjMatrices[gridType]);
            gridViewProjMatrices[gridType*3+2].rotate((float)Math.toRadians(90), Vector3f.X_AXIS);
            gridViewProjMatrices[gridType*3+2].translate(translateX, translateY, translateZ);
        }
    }

    // performs illumination of the voxel-grids
    private void PerformGridLightingPass(){
        // let each active light illuminate the voxel-grids
        for(int i=0;i<DX11_RENDERER.getInstance().GetNumLights();i++)
        {
            ILIGHT light = DX11_RENDERER.getInstance().GetLight(i);
            if(light.IsActive())
                light.AddGridSurfaces();
        }
    }

    // performs propagation of virtual point-lights
    private void PerformLightPropagatePass(int index,GridType gridType){
        // Propagate virtual point-lights to 6 neighbor grid-cells in the compute shader. In the
        // first iteration no occlusion is used, in order to initially let the light distribute.
        // From the second iteration on we use the geometry occlusion, in order to avoid light leaking.
        SURFACE surface;
        surface.renderTarget = lightRTs[gridType][1-currentLightRTIndices[gridType]];
        surface.renderTargetConfig = lightPropagateRTC;
        surface.renderOrder = GLOBAL_ILLUM_RO;
        if((useOcclusion)&&(index>0))
        {
            surface.shader = lightPropagateShaders[gridType][1];
            surface.customSBs[gridType] = gridSBs[gridType];
        }
        else
            surface.shader = lightPropagateShaders[gridType][0];
        surface.customTextures[0] = lightRTs[gridType][currentLightRTIndices[gridType]]->GetTexture();
        surface.customTextures[1] = lightRTs[gridType][currentLightRTIndices[gridType]]->GetTexture(1);
        surface.customTextures[2] = lightRTs[gridType][currentLightRTIndices[gridType]]->GetTexture(2);
        surface.vertexBuffer = NULL;
        surface.rasterizerState = NULL;
        surface.depthStencilState = NULL;
        surface.blendState = NULL;
        surface.renderMode = COMPUTE_RM;
        surface.numThreadGroupsX = 4;
        surface.numThreadGroupsY = 4;
        surface.numThreadGroupsZ = 4;
        DEMO::renderer->AddSurface(surface);

        currentLightRTIndices[gridType] = 1-currentLightRTIndices[gridType];
    }

    // perform actual indirect illumination
    private void PerformGlobalIllumPass();

    // visualizes voxel-grids
    private void PerformGridVisPass();

    // clears voxel-grids
    private void PerformClearPass();
}
