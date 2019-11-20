package jet.opengl.demos.nvidia.waves.ocean;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.IntBuffer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture3D;
import jet.opengl.postprocessing.texture.Texture3DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Recti;

final class OceanPSM implements OceanConst{
    private static final int kPSMLayersPerSlice = 2;

    private GLSLProgram m_pFX;
    private GLSLProgram m_pPropagatePSMTechnique;
    private GLSLProgram m_pRenderPSMToUITechnique;

//    ID3DX11EffectShaderResourceVariable* m_pPSMMapVariable;
//    ID3DX11EffectUnorderedAccessViewVariable* m_pPSMPropagationMapUAVVariable;

    // D3D resources
//    ID3D11Device* m_pd3dDevice;
    private Texture3D m_pPSMSRV;
    private Texture3D m_pPSMUAV;
    private Texture3D m_pPSMRTV;

    private int m_PSM_w;
    private int m_PSM_h;
    private int m_PSM_d;
    private int m_PSM_num_slices;

    private final Vector3f m_PsmBoundsMin = new Vector3f(-1.f,-1.f,-1.f);
    private final Vector3f m_PsmBoundsMax = new Vector3f(1.f,1.f,1.f);

    private final Matrix4f m_matPSMView = new Matrix4f();
    private final Matrix4f m_matPSMProj = new Matrix4f();
    private final Matrix4f m_matWorldToPSMUV = new Matrix4f();

    // State save/restore
//    D3D11_VIEWPORT m_saved_viewports[D3D11_VIEWPORT_AND_SCISSORRECT_OBJECT_COUNT_PER_PIPELINE];
//    UINT m_num_saved_viewports;
    private final Recti m_saved_viewports = new Recti();

    private GLFuncProvider gl;
    void init(	ReadableVector3f psm_bounds_min,
                     ReadableVector3f psm_bounds_max,
                     int nominal_res
    ){
        m_PsmBoundsMin.set(psm_bounds_min);
        m_PsmBoundsMax.set(psm_bounds_max);

        final Vector3f psm_bounds_extents = Vector3f.sub(psm_bounds_max, psm_bounds_min, null);
        final float world_volume_of_psm_bounds = psm_bounds_extents.x * psm_bounds_extents.y * psm_bounds_extents.z;
        final float voxel_volume = world_volume_of_psm_bounds / (nominal_res*nominal_res*nominal_res);
        final float voxel_length = (float) Math.pow(voxel_volume,1/3.f);

        m_PSM_w = (int)Math.ceil(psm_bounds_extents.x/voxel_length);
        m_PSM_h = (int)Math.ceil(psm_bounds_extents.y/voxel_length);
        m_PSM_d = (int)Math.ceil(psm_bounds_extents.z/voxel_length);
        m_PSM_num_slices = (m_PSM_d/kPSMLayersPerSlice)-1;

        gl = GLFuncProviderFactory.getGLFuncProvider();

        /*SAFE_RELEASE(m_pFX);
        ID3DXBuffer* pEffectBuffer = NULL;
        V_RETURN(LoadFile(TEXT(".\\Media\\ocean_psm_d3d11.fxo"), &pEffectBuffer));
        V_RETURN(D3DX11CreateEffectFromMemory(pEffectBuffer->GetBufferPointer(), pEffectBuffer->GetBufferSize(), 0, m_pd3dDevice, &m_pFX));
        pEffectBuffer->Release();*/

        m_pPropagatePSMTechnique = ShaderManager.getInstance().getProgram("PropagatePSMTech");
        m_pRenderPSMToUITechnique = ShaderManager.getInstance().getProgram("RenderPSMToUITech");

//        m_pPSMPropagationMapUAVVariable = m_pFX->GetVariableByName("g_PSMPropagationMapUAV")->AsUnorderedAccessView();
//        m_pPSMMapVariable = m_pFX->GetVariableByName("g_PSMMap")->AsShaderResource();

        final int DXGI_FORMAT_R16G16_TYPELESS = GLenum.GL_RG16;
        // Create PSM texture
        {
            Texture3DDesc texDesc = new Texture3DDesc();
            texDesc.format = DXGI_FORMAT_R16G16_TYPELESS;
            texDesc.width = m_PSM_w;
            texDesc.height = m_PSM_h;
            texDesc.mipLevels = 1;
            texDesc.depth = m_PSM_d/kPSMLayersPerSlice;
            // texDesc.SampleDesc.Count = 1;
            // texDesc.SampleDesc.Quality = 0;
//            texDesc.Usage = D3D11_USAGE_DEFAULT;
//            texDesc.BindFlags = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_RENDER_TARGET | D3D11_BIND_UNORDERED_ACCESS;
//            texDesc.CPUAccessFlags = 0;
//            texDesc.MiscFlags = 0;

            /*ID3D11Texture3D* pTex = NULL;
            V_RETURN(m_pd3dDevice->CreateTexture3D(&texDesc, NULL, &pTex));

            D3D11_SHADER_RESOURCE_VIEW_DESC srvDesc;
            srvDesc.ViewDimension = D3D11_SRV_DIMENSION_TEXTURE3D;
            srvDesc.Texture3D.MostDetailedMip = 0;
            srvDesc.Texture3D.MipLevels = texDesc.MipLevels;
            srvDesc.Format = DXGI_FORMAT_R16G16_UNORM;
            V_RETURN(m_pd3dDevice->CreateShaderResourceView(pTex, &srvDesc, &m_pPSMSRV) );

            D3D11_RENDER_TARGET_VIEW_DESC rtvDesc;
            rtvDesc.ViewDimension = D3D11_RTV_DIMENSION_TEXTURE3D;
            rtvDesc.Texture3D.MipSlice = 0;
            rtvDesc.Texture3D.FirstWSlice = 0;
            rtvDesc.Texture3D.WSize = texDesc.Depth;
            rtvDesc.Format = DXGI_FORMAT_R16G16_UNORM;
            V_RETURN(m_pd3dDevice->CreateRenderTargetView(pTex, &rtvDesc, &m_pPSMRTV) );

            D3D11_UNORDERED_ACCESS_VIEW_DESC uavDesc;
            uavDesc.ViewDimension = D3D11_UAV_DIMENSION_TEXTURE3D;
            uavDesc.Texture3D.MipSlice = 0;
            uavDesc.Texture3D.FirstWSlice = 0;
            uavDesc.Texture3D.WSize = texDesc.Depth;
            uavDesc.Format = DXGI_FORMAT_R32_UINT;
            V_RETURN(m_pd3dDevice->CreateUnorderedAccessView(pTex, &uavDesc, &m_pPSMUAV) );

            SAFE_RELEASE(pTex);*/

            m_pPSMSRV = m_pPSMRTV = m_pPSMUAV = TextureUtils.createTexture3D(texDesc, null);
        }
    }

    void beginRenderToPSM(Matrix4f matPSM/*, ID3D11DeviceContext* pDC*/){
//        D3DXMatrixInverse(&m_matPSMView,NULL,&matPSM);
        Matrix4f.invert(matPSM, m_matPSMView);

        // We use proj to scale and translate to PSM from local space
        final Vector3f PSM_centre_local = Vector3f.linear(m_PsmBoundsMax,0.5f, m_PsmBoundsMin, 0.5f, null);
        final Vector3f PSM_extents_local = Vector3f.sub(m_PsmBoundsMax, m_PsmBoundsMin, null);
        /*D3DXMATRIX TranslationComponent, ScalingComponent;
        D3DXMatrixTranslation(&TranslationComponent, -PSM_centre_local.x, -PSM_centre_local.y, m_PsmBoundsMax.z);		// Using z-max because effective light-dir is pointing down
        D3DXMatrixScaling(&ScalingComponent, 2.f/PSM_extents_local.x, 2.f/PSM_extents_local.y, 1.f/PSM_extents_local.z);
        m_matPSMProj = TranslationComponent * ScalingComponent;*/
        m_matPSMProj.setIdentity();
        m_matPSMProj.scale(2.f/PSM_extents_local.x, 2.f/PSM_extents_local.y, 1.f/PSM_extents_local.z);
        m_matPSMProj.translate(-PSM_centre_local.x, -PSM_centre_local.y, m_PsmBoundsMax.z);

        /*D3DXMATRIX matProjToUV;
        D3DXMatrixTranslation(&matProjToUV,0.5f,0.5f,0.f);
        matProjToUV._11 = 0.5f;
        matProjToUV._22 = -0.5f;
        m_matWorldToPSMUV = m_matPSMView * m_matPSMProj * matProjToUV; TODO*/
        Matrix4f.mul(m_matPSMProj, m_matPSMView, m_matWorldToPSMUV);

        // Save rt setup to restore shortly...
        /*m_num_saved_viewports = sizeof(m_saved_viewports)/sizeof(m_saved_viewports[0]);
        pDC->RSGetViewports( &m_num_saved_viewports, m_saved_viewports);*/
        IntBuffer viewport = CacheBuffer.getCachedIntBuffer(4);
        gl.glGetIntegerv(GLenum.GL_VIEWPORT, viewport);
        m_saved_viewports.load(viewport);


        /*pDC->OMSetRenderTargets(1, &m_pPSMRTV, NULL);  todo
	    const D3D11_VIEWPORT opacityViewport = {0.f, 0.f, FLOAT(m_PSM_w), FLOAT(m_PSM_h), 0.f, 1.f };
        pDC->RSSetViewports(1, &opacityViewport);

	    const D3DXVECTOR4 PSMClearColor(1.f,1.f,1.f,1.f);
        pDC->ClearRenderTargetView(m_pPSMRTV, (FLOAT*)&PSMClearColor);*/
    }

    void endRenderToPSM(/*ID3D11DeviceContext* pDC*/){
        // Restore original viewports
//        pDC->RSSetViewports(m_num_saved_viewports, m_saved_viewports);
        gl.glViewport(m_saved_viewports.x, m_saved_viewports.y, m_saved_viewports.width, m_saved_viewports.height);

        // Release RTV
        /*ID3D11RenderTargetView* pNullRTV = NULL;
        pDC->OMSetRenderTargets(1, &pNullRTV, NULL);*/

        // Propagate opacity
//        m_pPSMPropagationMapUAVVariable->SetUnorderedAccessView(m_pPSMUAV);
//        m_pPropagatePSMTechnique->GetPassByIndex(0)->Apply(0, pDC);

        gl.glBindImageTexture(0, m_pPSMUAV.getTexture(), 0, true, 0, GLenum.GL_WRITE_ONLY, GLenum.GL_R32UI);

        final int PSMPropagationCSBlockSize = 16;
	    final int num_groups_w = 1 + (m_PSM_w-1)/PSMPropagationCSBlockSize;
        final int num_groups_h = 1 + (m_PSM_h-1)/PSMPropagationCSBlockSize;
        gl.glDispatchCompute(num_groups_w,num_groups_h,1);

        // Release inputs
//        m_pPSMPropagationMapUAVVariable->SetUnorderedAccessView(NULL);
//        m_pPropagatePSMTechnique->GetPassByIndex(0)->Apply(0, pDC);

        gl.glBindImageTexture(0, 0, 0, false, 0, GLenum.GL_WRITE_ONLY, GLenum.GL_R32UI);
    }

    void setWriteParams(OceanPSMParams params){
//        gl.glUniform1f(params.m_pPSMSlicesVariable, m_PSM_num_slices);
        params.g_PSMSlices = m_PSM_num_slices;
    }
    void setReadParams(OceanPSMParams params, Vector3f tint_color){
        /*gl.glUniform1f(params.m_pPSMSlicesVariable, m_PSM_num_slices);
        gl.glUniform3f(params.m_pPSMTintVariable, tint_color.getX(), tint_color.getY(), tint_color.getZ());
        gl.glBindTextureUnit(params.m_pPSMMapVariable, m_pPSMSRV.getTexture());*/

        params.g_PSMSlices = m_PSM_num_slices;
        params.g_PSMMap = m_pPSMSRV;
        params.g_PSMTint = tint_color;
    }

    void clearReadParams(OceanPSMParams params){
    }

//    void renderToUI(ID3D11DeviceContext* pDC);

    TextureGL getPSMSRV() { return m_pPSMSRV; }

	Matrix4f getPSMView() { return m_matPSMView; }
    Matrix4f getPSMProj() { return m_matPSMProj; }
    Matrix4f getWorldToPSMUV() { return m_matWorldToPSMUV; }

    int getNumSlices()  { return m_PSM_num_slices; }

    private void renderParticles(/*ID3D11DeviceContext* pDC,*/ GLSLProgram pTech){
        throw new UnsupportedOperationException();
    }

}
