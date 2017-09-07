package jet.opengl.demos.nvidia.face.sample;

import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/9/7.
 */

final class CBackground {

    Texture2D m_pSrvCubeEnv;
    Texture2D	m_pSrvCubeDiff;
    Texture2D	m_pSrvCubeSpec;
    float						m_exposure;

    void Init(
            String strCubeEnv,
            String strCubeDiff,
            String strCubeSpec
            ){
        Init(strCubeEnv, strCubeDiff, strCubeSpec, 1.0f);
    }

    void Init(
            String strCubeEnv,
            String strCubeDiff,
            String strCubeSpec,
            float exposure /*= 1.0f*/){
//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), strCubeEnv));
//        V_RETURN(LoadTexture(strPath, pDevice, &m_pSrvCubeEnv, LT_HDR | LT_Cubemap));
        m_pSrvCubeEnv = CScene.loadCubeTexture(strCubeEnv);

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), strCubeDiff));
//        V_RETURN(LoadTexture(strPath, pDevice, &m_pSrvCubeDiff, LT_HDR | LT_Cubemap));
        m_pSrvCubeDiff = CScene.loadCubeTexture(strCubeDiff);

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), strCubeSpec));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvCubeSpec, LT_Mipmap | LT_HDR | LT_Cubemap));
        m_pSrvCubeSpec = CScene.loadCubeTexture(strCubeSpec);

        m_exposure = exposure;
    }

    void Release(){
        CommonUtil.safeRelease(m_pSrvCubeEnv);
        CommonUtil.safeRelease(m_pSrvCubeDiff);
        CommonUtil.safeRelease(m_pSrvCubeSpec);
    }
}
