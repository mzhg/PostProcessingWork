package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector2f;

import java.io.IOException;

import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Quadtree;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Quadtree_Params;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Quadtree_Stats;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Savestate;
import jet.opengl.demos.nvidia.waves.wavework.GFSDK_WaveWorks_Simulation;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/7/27.
 */

final class OceanSurface implements Disposeable{

    GFSDK_WaveWorks_Quadtree m_hOceanQuadTree;

    // D3D objects
//    ID3D11Device*			m_pd3dDevice;

//    ID3DX11Effect*			m_pOceanFX;
    GLSLProgram m_pRenderSurfaceTechnique;
    OceanSurfaceShadedProgram		m_pRenderSurfaceShadedWithShorelinePass;
//	ID3DX11EffectPass*		m_pRenderSurfaceWireframeWithShorelinePass;

    VertexArrayObject m_pQuadLayout;
    VertexArrayObject		m_pRayContactLayout;
    GLSLProgram m_pRenderRayContactTechnique;
    GLSLProgram m_pRenderRaysTechnique;
    BufferGL m_pContactVB;
    BufferGL			m_pContactIB;

    final int[] m_pQuadTreeShaderInputMappings_Shore;
    final int[] m_pSimulationShaderInputMappings_Shore;
    private GLFuncProvider gl;
    private final Matrix4f topDownMatrix = new Matrix4f();
    private final SampleD3D11 context;

    public OceanSurface(SampleD3D11 context){
        this.context = context;

        int NumQuadtreeShaderInputs = GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_GetShaderInputCountD3D11();
        int NumSimulationShaderInputs = GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetShaderInputCountD3D11();
        m_pQuadTreeShaderInputMappings_Shore = new int [NumQuadtreeShaderInputs];
        m_pSimulationShaderInputMappings_Shore = new int [NumSimulationShaderInputs];
    }

    DistanceField pDistanceFieldModule; // Not owned!
    void AttachDistanceFieldModule( DistanceField pDistanceField ) { pDistanceFieldModule = pDistanceField; }

    public void init() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
//        TCHAR path[MAX_PATH];
//
//        V_RETURN(DXUTFindDXSDKMediaFileCch(path, MAX_PATH, TEXT("ocean_surface_d3d11.fxo")));
//        V_RETURN(D3DX11CreateEffectFromFile(path, 0, m_pd3dDevice, &m_pOceanFX));
//
//
//        // Hook up the shader mappings
//        m_pRenderSurfaceTechnique = m_pOceanFX->GetTechniqueByName("RenderOceanSurfTech");
//        m_pRenderSurfaceShadedWithShorelinePass = m_pRenderSurfaceTechnique->GetPassByName("Pass_Solid_WithShoreline");
        final String shader_path = "nvidia/WaveWorks/shaders/";
        m_pRenderSurfaceShadedWithShorelinePass =/*GLSLProgram.createProgram(shader_path + "OceanWaveVS.vert", shader_path + "OceanWaveHS.gltc",
                shader_path+"OceanWaveDS.glte", shader_path+"SolidWireGS.gemo", shader_path+"OceanWaveShorePS.frag",
                CommonUtil.toArray(new Macro("GFSDK_WAVEWORKS_USE_TESSELLATION", 1)))*/
                new OceanSurfaceShadedProgram();
        m_pRenderSurfaceShadedWithShorelinePass.setName("RenderOceanWave");

        try {
            m_pRenderRayContactTechnique = GLSLProgram.createFromFiles(shader_path + "ContactVS.vert", shader_path + "RayContactPS.frag");
            m_pRenderRaysTechnique = GLSLProgram.createFromFiles(shader_path + "RayVS.vert", shader_path + "RayContactPS.frag");
        } catch (IOException e) {
            e.printStackTrace();
        }

//        D3DX11_PASS_SHADER_DESC passShaderDesc;
//
//        V_RETURN(m_pRenderSurfaceShadedWithShorelinePass->GetVertexShaderDesc(&passShaderDesc));
//        ID3D11ShaderReflection* pShadedShoreReflectionVS = GetReflection(passShaderDesc);
//
//        V_RETURN(m_pRenderSurfaceShadedWithShorelinePass->GetHullShaderDesc(&passShaderDesc));
//        ID3D11ShaderReflection* pShadedShoreReflectionHS = GetReflection(passShaderDesc);
//
//        V_RETURN(m_pRenderSurfaceShadedWithShorelinePass->GetDomainShaderDesc(&passShaderDesc));
//        ID3D11ShaderReflection* pShadedShoreReflectionDS = GetReflection(passShaderDesc);
//
//        V_RETURN(m_pRenderSurfaceShadedWithShorelinePass->GetPixelShaderDesc(&passShaderDesc));
//        ID3D11ShaderReflection* pShadedShoreReflectionPS = GetReflection(passShaderDesc);

        int NumQuadtreeShaderInputs = GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_GetShaderInputCountD3D11();
        int NumSimulationShaderInputs = GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_GetShaderInputCountD3D11();

        for(int i = 0; i != NumQuadtreeShaderInputs; ++i)
        {
//            GFSDK_WaveWorks_ShaderInput_Desc inputDesc;  TODO
//            GFSDK_WaveWorks_Quadtree_GetShaderInputDescD3D11(i, &inputDesc);
//            m_pQuadTreeShaderInputMappings_Shore[i] = GetShaderInputRegisterMapping(pShadedShoreReflectionVS, pShadedShoreReflectionHS, pShadedShoreReflectionDS, pShadedShoreReflectionPS, inputDesc);
        }

        for(int i = 0; i != NumSimulationShaderInputs; ++i)
        {
//            GFSDK_WaveWorks_ShaderInput_Desc inputDesc;  TODO
//            GFSDK_WaveWorks_Simulation_GetShaderInputDescD3D11(i, &inputDesc);
//            m_pSimulationShaderInputMappings_Shore[i] = GetShaderInputRegisterMapping(pShadedShoreReflectionVS, pShadedShoreReflectionHS, pShadedShoreReflectionDS, pShadedShoreReflectionPS, inputDesc);
        }

//        pShadedShoreReflectionVS->Release();
//        pShadedShoreReflectionPS->Release();
//        pShadedShoreReflectionHS->Release();
//        pShadedShoreReflectionDS->Release();

        {

            float vertex_data[/*5*4*/] =
            {0, 0, 0, 1,
                    1, 1, 0, 0,
                    0, 1, 1, 0,
                    -1, 1, 0, 0,
                    0, 1,-1, 0};
//            D3D11_BUFFER_DESC vBufferDesc;
//            vBufferDesc.ByteWidth = 5 * sizeof(XMFLOAT4);
//            vBufferDesc.Usage = D3D11_USAGE_DEFAULT;
//            vBufferDesc.BindFlags = D3D11_BIND_VERTEX_BUFFER;
//            vBufferDesc.CPUAccessFlags = 0;
//            vBufferDesc.MiscFlags = 0;
//            D3D11_SUBRESOURCE_DATA vBufferData;
//            vBufferData.pSysMem = vertex_data;
//            vBufferData.SysMemPitch = 0;
//            vBufferData.SysMemSlicePitch = 0;
//            V_RETURN(m_pd3dDevice->CreateBuffer(&vBufferDesc, &vBufferData, &m_pContactVB));
            m_pContactVB = new BufferGL();
            m_pContactVB.initlize(GLenum.GL_ARRAY_BUFFER, vertex_data.length*4, CacheBuffer.wrap(vertex_data), GLenum.GL_STATIC_DRAW);


            final byte indices[] = {0,1,2, 0,2,3, 0,3,4, 0,4,1};
//            D3D11_BUFFER_DESC iBufferDesc;
//            iBufferDesc.ByteWidth = sizeof(indices);
//            iBufferDesc.Usage = D3D11_USAGE_IMMUTABLE;
//            iBufferDesc.BindFlags = D3D11_BIND_INDEX_BUFFER;
//            iBufferDesc.CPUAccessFlags = 0;
//            iBufferDesc.MiscFlags = 0;
//            D3D11_SUBRESOURCE_DATA iBufferData;
//            iBufferData.pSysMem = indices;
//            iBufferData.SysMemPitch = 0;
//            iBufferData.SysMemSlicePitch = 0;
//            V_RETURN(m_pd3dDevice->CreateBuffer(&iBufferDesc, &iBufferData, &m_pContactIB));
            m_pContactIB = new BufferGL();
            m_pContactIB.initlize(GLenum.GL_ELEMENT_ARRAY_BUFFER, indices.length, CacheBuffer.wrap(indices), GLenum.GL_STATIC_DRAW);
        }
    }

    public void initQuadTree(GFSDK_WaveWorks_Quadtree_Params params) {
        if(null == m_hOceanQuadTree)
        {
            m_hOceanQuadTree = GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_CreateD3D11(params /*m_pd3dDevice,*/ );
        }
        else
        {
            GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_UpdateParams(m_hOceanQuadTree, params);
        }
    }

    // --------------------------------- Rendering routines -----------------------------------
    // Rendering
    void renderShaded(//		ID3D11DeviceContext* pDC,
                              Matrix4f matView,
                              Matrix4f matProj,
                              GFSDK_WaveWorks_Simulation hSim,
                              GFSDK_WaveWorks_Savestate hSavestate,
                              final ReadableVector2f windDir,
                              float steepness,
                              float amplitude,
                              float wavelength,
                              float speed,
                              float parallelness,
                              float totalTime){
        if( pDistanceFieldModule != null)
        {
            // Apply data tex SRV
            pDistanceFieldModule.GetWorldToTopDownTextureMatrix( topDownMatrix );

//            XMFLOAT4X4 tdmStore;
//            XMStoreFloat4x4(&tdmStore, topDownMatrix);

            m_pRenderSurfaceShadedWithShorelinePass.enable();
//            m_pOceanFX->GetVariableByName("g_WorldToTopDownTextureMatrix")->AsMatrix()->SetMatrix( (FLOAT*)&tdmStore );
            int index = m_pRenderSurfaceShadedWithShorelinePass.getUniformLocation("g_WorldToTopDownTextureMatrix");
            if(index >=0){
                gl.glUniformMatrix4fv(index, false, CacheBuffer.wrap(topDownMatrix));
            }
//            m_pOceanFX->GetVariableByName("g_GerstnerSteepness")->AsScalar()->SetFloat( steepness );
            index = m_pRenderSurfaceShadedWithShorelinePass.getUniformLocation("g_GerstnerSteepness");
            if(index>=0){
                gl.glUniform1f(index, steepness);
            }
//            m_pOceanFX->GetVariableByName("g_BaseGerstnerAmplitude")->AsScalar()->SetFloat( amplitude );
            index = m_pRenderSurfaceShadedWithShorelinePass.getUniformLocation("g_BaseGerstnerAmplitude");
            if(index>=0){
                gl.glUniform1f(index, amplitude);
            }

//            m_pOceanFX->GetVariableByName("g_BaseGerstnerWavelength")->AsScalar()->SetFloat( wavelength );

            index = m_pRenderSurfaceShadedWithShorelinePass.getUniformLocation("g_BaseGerstnerWavelength");
            if(index>=0){
                gl.glUniform1f(index, wavelength);
            }

//            m_pOceanFX->GetVariableByName("g_BaseGerstnerSpeed")->AsScalar()->SetFloat( speed );
            index = m_pRenderSurfaceShadedWithShorelinePass.getUniformLocation("g_BaseGerstnerSpeed");
            if(index>=0){
                gl.glUniform1f(index, speed);
            }
//            m_pOceanFX->GetVariableByName("g_BaseGerstnerParallelness")->AsScalar()->SetFloat( parallelness );
            index = m_pRenderSurfaceShadedWithShorelinePass.getUniformLocation("g_BaseGerstnerParallelness");
            if(index>=0){
                gl.glUniform1f(index, parallelness);
            }

//            m_pOceanFX->GetVariableByName("g_WindDirection")->AsVector()->SetFloatVector( (FLOAT*) &windDir );
            index = m_pRenderSurfaceShadedWithShorelinePass.getUniformLocation("g_WindDirection");
            if(index>=0){
                gl.glUniform2f(index, windDir.getX(), windDir.getY());
            }

//            m_pOceanFX->GetVariableByName("g_DataTexture")->AsShaderResource()->SetResource( pDistanceFieldModule->GetDataTextureSRV() ); TODO
//            m_pOceanFX->GetVariableByName("g_Time")->AsScalar()->SetFloat( totalTime );
            index = m_pRenderSurfaceShadedWithShorelinePass.getUniformLocation("g_Time");
            if(index>=0){
                gl.glUniform1f(index, totalTime);
            }

//            m_pRenderSurfaceShadedWithShorelinePass->Apply( 0, pDC );
            GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_SetRenderStateD3D11(hSim, /*pDC,*/ matView, m_pSimulationShaderInputMappings_Shore, hSavestate);
            GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_DrawD3D11(m_hOceanQuadTree, /*pDC,*/ matView, matProj, m_pQuadTreeShaderInputMappings_Shore, hSavestate);

//            m_pOceanFX->GetVariableByName("g_DataTexture")->AsShaderResource()->SetResource( NULL );
        }
//        GFSDK_WaveWorks.GFSDK_WaveWorks_Savestate_RestoreD3D11(hSavestate, pDC);

        if(!context.m_printOcen){
            m_pRenderSurfaceShadedWithShorelinePass.printPrograminfo();
        }
    }

    void renderShaded(IsParameters params,
                      GFSDK_WaveWorks_Simulation hSim,
                      GFSDK_WaveWorks_Savestate hSavestate){
        params.g_enableShoreEffects = 1;
//        params.g_Wireframe = true;
        if( pDistanceFieldModule != null)
        {
            // Apply data tex SRV
            pDistanceFieldModule.GetWorldToTopDownTextureMatrix( topDownMatrix );

//            XMFLOAT4X4 tdmStore;
//            XMStoreFloat4x4(&tdmStore, topDownMatrix);
            m_pRenderSurfaceShadedWithShorelinePass.enable();
//            m_pOceanFX->GetVariableByName("g_WorldToTopDownTextureMatrix")->AsMatrix()->SetMatrix( (FLOAT*)&tdmStore );
            m_pRenderSurfaceShadedWithShorelinePass.setUniforms(params);

            int index = m_pRenderSurfaceShadedWithShorelinePass.getUniformLocation("g_WorldToTopDownTextureMatrix");
            if(index >=0){
                gl.glUniformMatrix4fv(index, false, CacheBuffer.wrap(topDownMatrix));
            }
            if(params.g_Wireframe){
                gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_LINE);
            }
//
//            m_pRenderSurfaceShadedWithShorelinePass->Apply( 0, pDC );
            GFSDK_WaveWorks.GFSDK_WaveWorks_Simulation_SetRenderStateD3D11(hSim, params.g_ModelViewMatrix, m_pSimulationShaderInputMappings_Shore, hSavestate);
            GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_DrawD3D11(m_hOceanQuadTree, params.g_ModelViewMatrix, params.g_Projection, m_pQuadTreeShaderInputMappings_Shore, hSavestate);
            if(params.g_Wireframe) {
                gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
            }
//            m_pOceanFX->GetVariableByName("g_DataTexture")->AsShaderResource()->SetResource( NULL );
            GLCheck.checkError();
        }
//        GFSDK_WaveWorks.GFSDK_WaveWorks_Savestate_RestoreD3D11(hSavestate, pDC);

        if(!context.m_printOcen){
            m_pRenderSurfaceShadedWithShorelinePass.printPrograminfo();
        }

        for(int i = 15; i >=0; i--){
            gl.glActiveTexture(GLenum.GL_TEXTURE0 + i);
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
            gl.glBindSampler(i, 0);
        }

        for(int i = 4; i >=0; i--){
            gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, i, 0);
        }
    }

    void renderReadbacks(Matrix4f viewProj){

        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_pContactVB.getBuffer());
        // TODO layout
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, m_pContactIB.getBuffer());

        m_pRenderRayContactTechnique.enable();
        int contactPosition = m_pRenderRayContactTechnique.getUniformLocation("g_ContactPosition");
        for( int i = 0; i < SampleD3D11.NumMarkers; i++)
        {
//            contactPos = XMFLOAT4(context.g_readback_marker_positions[i].x, g_readback_marker_positions[i].y, g_readback_marker_positions[i].z, 0);
//            g_pOceanSurf->m_pOceanFX->GetVariableByName("g_ContactPosition")->AsVector()->SetFloatVector((FLOAT*)&contactPos);
//            g_pOceanSurf->m_pRenderRayContactTechnique->GetPassByIndex(0)->Apply(0, pContext);
            gl.glUniform4f(contactPosition, context.g_readback_marker_positions[i].x, context.g_readback_marker_positions[i].y, context.g_readback_marker_positions[i].z, 0);
//            pContext->DrawIndexed(12, 0, 0);
            gl.glDrawElements(GLenum.GL_TRIANGLES, 12, GLenum.GL_UNSIGNED_SHORT, 0);
        }

        // TODO disable vertex attribu pointer.

        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);

        if(context.m_printOcen == false){
            m_pRenderRayContactTechnique.setName("RenderReadbacks");
            m_pRenderRayContactTechnique.printPrograminfo();
        }
    }

    void renderRayContacts(Matrix4f viewProj){
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_pContactVB.getBuffer());
        // TODO layout
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, m_pContactIB.getBuffer());

        m_pRenderRayContactTechnique.enable();
        int contactPosition = m_pRenderRayContactTechnique.getUniformLocation("g_ContactPosition");
        for( int i = 0; i < SampleD3D11.NumMarkers; i++)
        {
//            contactPos = XMFLOAT4(context.g_readback_marker_positions[i].x, g_readback_marker_positions[i].y, g_readback_marker_positions[i].z, 0);
//            g_pOceanSurf->m_pOceanFX->GetVariableByName("g_ContactPosition")->AsVector()->SetFloatVector((FLOAT*)&contactPos);
//            g_pOceanSurf->m_pRenderRayContactTechnique->GetPassByIndex(0)->Apply(0, pContext);
            gl.glUniform4f(contactPosition, context.g_raycast_hitpoints[i].x, context.g_raycast_hitpoints[i].y, context.g_raycast_hitpoints[i].z, 0);
//            pContext->DrawIndexed(12, 0, 0);
            gl.glDrawElements(GLenum.GL_TRIANGLES, 12, GLenum.GL_UNSIGNED_SHORT, 0);
        }

        // TODO disable vertex attribu pointer.

        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);

        if(context.m_printOcen == false){
            m_pRenderRayContactTechnique.setName("RenderRayContact");
            m_pRenderRayContactTechnique.printPrograminfo();
        }
    }

    void renderRays(Matrix4f viewProj) {
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_pContactVB.getBuffer());
        // TODO layout
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, m_pContactIB.getBuffer());

        m_pRenderRaysTechnique.enable();
        int originPosition = m_pRenderRaysTechnique.getUniformLocation("g_OriginPosition");
        int rayDirection = m_pRenderRaysTechnique.getUniformLocation("g_RayDirection");
        for( int i = 0; i < SampleD3D11.NumMarkers; i++)
        {
//            XMStoreFloat4(&origPos, g_raycast_origins[i]);
//            g_pOceanSurf->m_pOceanFX->GetVariableByName("g_OriginPosition")->AsVector()->SetFloatVector((FLOAT*)&origPos);
//
//            XMVECTOR vecRayDir = g_raycast_directions[i] * 100.0f;
//            XMStoreFloat4(&rayDirection, vecRayDir);
//
//            g_pOceanSurf->m_pOceanFX->GetVariableByName("g_RayDirection")->AsVector()->SetFloatVector((FLOAT*) &rayDirection);
//            g_pOceanSurf->m_pRenderRayContactTechnique->GetPassByIndex(1)->Apply(0, pContext);
//            pContext->DrawIndexed(2, 0, 0);

            gl.glUniform4f(originPosition, context.g_raycast_origins[i].x, context.g_raycast_origins[i].y, context.g_raycast_origins[i].z, 0);
            gl.glUniform4f(rayDirection, context.g_raycast_directions[i].x*100, context.g_raycast_directions[i].y*100, context.g_raycast_directions[i].z*100, 0);
            gl.glDrawElements(GLenum.GL_TRIANGLES, 2, GLenum.GL_UNSIGNED_SHORT, 0);
        }

        // TODO disable vertex attribu pointer.

        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);

        if(context.m_printOcen == false){
            m_pRenderRaysTechnique.setName("RenderRays");
            m_pRenderRaysTechnique.printPrograminfo();
        }
    }

    void getQuadTreeStats(GFSDK_WaveWorks_Quadtree_Stats stats){
        GFSDK_WaveWorks.GFSDK_WaveWorks_Quadtree_GetStats(m_hOceanQuadTree, stats);
    }

    @Override
    public void dispose() {
        CommonUtil.safeRelease(m_pRenderSurfaceTechnique);
        CommonUtil.safeRelease(m_pRenderSurfaceShadedWithShorelinePass);
        CommonUtil.safeRelease(m_pQuadLayout);
        CommonUtil.safeRelease(m_pRayContactLayout);
        CommonUtil.safeRelease(m_pRenderRayContactTechnique);
        CommonUtil.safeRelease(m_pContactVB);
        CommonUtil.safeRelease(m_pContactIB);
    }


}
