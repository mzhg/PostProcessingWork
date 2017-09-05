package jet.opengl.demos.nvidia.face.sample;

import java.util.HashMap;

import jet.opengl.demos.nvidia.face.libs.GFSDK_FaceWorks_CBData;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.shader.ShaderProgram;

/**
 * Created by mazhen'gui on 2017/9/5.
 */

final class CShaderManager {
    ShaderProgram m_pPsCopy;
    ShaderProgram		m_pPsCreateVSM;
    ShaderProgram	m_pVsCurvature;
    ShaderProgram		m_pPsCurvature;
    ShaderProgram		m_pPsThickness;
    ShaderProgram		m_pPsGaussian;
    ShaderProgram		m_pPsHair;
    ShaderProgram	m_pVsScreen;
    ShaderProgram	m_pVsShadow;
    ShaderProgram	m_pVsSkybox;
    ShaderProgram		m_pPsSkybox;
    ShaderProgram	m_pVsTess;
    ShaderProgram		m_pHsTess;
    ShaderProgram	m_pDsTess;
    ShaderProgram	m_pVsWorld;

    int	m_pSsPointClamp;
    int	m_pSsBilinearClamp;
    int	m_pSsTrilinearRepeat;
    int	m_pSsTrilinearRepeatAniso;
    int	m_pSsPCF;

//    ID3D11InputLayout *		m_pInputLayout;

    BufferGL m_pCbufDebug;
    BufferGL m_pCbufFrame;
    BufferGL m_pCbufShader;

    final HashMap<Integer, ShaderProgram> m_mapSkinFeaturesToShader = new HashMap<>();
    final HashMap<Integer, ShaderProgram> m_mapEyeFeaturesToShader = new HashMap<>();

    CShaderManager();

    HRESULT Init(ID3D11Device * pDevice);

    void InitFrame(
            ID3D11DeviceContext * pCtx,
            const CbufDebug * pCbufDebug,
            const CbufFrame * pCbufFrame,
            ID3D11ShaderResourceView * pSrvCubeDiffuse,
            ID3D11ShaderResourceView * pSrvCubeSpec,
            ID3D11ShaderResourceView * pSrvCurvatureLUT,
            ID3D11ShaderResourceView * pSrvShadowLUT);

    void BindShadowTextures(
            ID3D11DeviceContext * pCtx,
            ID3D11ShaderResourceView * pSrvShadowMap,
            ID3D11ShaderResourceView * pSrvVSM);

    ShaderProgram			GetSkinShader(ID3D11Device * pDevice, SHDFEATURES features);
    ShaderProgram			GetEyeShader(ID3D11Device * pDevice, SHDFEATURES features);

    void BindCopy(
            ID3D11DeviceContext * pCtx,
            ID3D11ShaderResourceView * pSrvSrc,
            const DirectX::XMFLOAT4X4 & matTransformColor);
    void BindCreateVSM(
            ID3D11DeviceContext * pCtx,
            ID3D11ShaderResourceView * pSrvSrc);
    void BindCurvature(
            ID3D11DeviceContext * pCtx,
            float curvatureScale,
            float curvatureBias);
    void BindThickness(
            ID3D11DeviceContext* pCtx,
            GFSDK_FaceWorks_CBData* pFaceWorksCBData);
    void BindGaussian(
            ID3D11DeviceContext * pCtx,
            ID3D11ShaderResourceView * pSrvSrc,
            float blurX,
            float blurY);
    void BindShadow(
            ID3D11DeviceContext * pCtx,
            const DirectX::XMFLOAT4X4 & matWorldToClipShadow);
    void BindSkybox(
            ID3D11DeviceContext * pCtx,
            ID3D11ShaderResourceView * pSrvSkybox,
            const DirectX::XMFLOAT4X4 & matClipToWorldAxes);

    void BindMaterial(
            ID3D11DeviceContext * pCtx,
            SHDFEATURES features,
            const Material * pMtl);
    void UnbindTess(ID3D11DeviceContext * pCtx);

    void DiscardRuntimeCompiledShaders();
    void Release();

    private void CompileShader(
            ID3D11Device * pDevice,
            const char * source,
            ShaderProgram* ppPsOut);
    private void CreateSkinShader(ID3D11Device * pDevice, SHDFEATURES features);
    private void CreateEyeShader(ID3D11Device * pDevice, SHDFEATURES features);
}
