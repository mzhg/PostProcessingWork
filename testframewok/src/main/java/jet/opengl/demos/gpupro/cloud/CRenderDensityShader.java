package jet.opengl.demos.gpupro.cloud;

/**
 * Shader for rendering cloud density<p></p>
 * Created by mazhen'gui on 2017/7/6.
 */

final class CRenderDensityShader extends RenderTechnique{

    private SSceneParamter m_pSceneParam;

    CRenderDensityShader(SSceneParamter sceneParam) {
        super("CloudGridVS.vert", "CloudGridPS.frag");

        m_pSceneParam= sceneParam;
    }

    boolean Begin(CCloudGrid cloud){
        enable();

        if (m_pSceneParam != null /*&& m_pSceneParam->m_pCamera != NULL*/) {
            // world to projection transform
//            SetVSMatrix( pDev, VS_CONST_W2C, m_pSceneParam->m_pCamera->GetWorld2ProjMatrix() );
            setW2C(m_pSceneParam.m_viewMat);
            // view position
//            SetVSValue( pDev, VS_CONST_EYE, m_pSceneParam->m_pCamera->GetEyePt(), sizeof(FLOAT)*3 );
            setEye(m_pSceneParam.m_Eye);
        }
        if (cloud != null) {
            SVSParam param = new SVSParam();
            cloud.GetVSParam( param );
            // uv scale and offset parameter
//            SetVSValue( pDev, VS_CONST_UVPARAM, &param.vUVParam, sizeof(D3DXVECTOR4) );
            setUVParam(param.vUVParam);
            // xz position scale and offset parameter
//            SetVSValue( pDev, VS_CONST_XZPARAM, &param.vXZParam, sizeof(D3DXVECTOR4) );
            setXZParam(param.vXZParam);
            // height parameters
//            SetVSValue( pDev, VS_CONST_HEIGHT, &param.vHeight, sizeof(D3DXVECTOR2) );
            setHeight(param.vHeight);

            // cloud cover
//            FLOAT fCloudCover = pCloud->GetCurrentCloudCover();
//            SetPSValue( pDev, PS_CONST_COVER, &fCloudCover, sizeof(FLOAT)*1 );
            setCloudCover(cloud.GetCurrentCloudCover());
        }

        return true;
    }

}
