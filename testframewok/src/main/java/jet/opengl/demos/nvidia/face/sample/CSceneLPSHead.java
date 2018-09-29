package jet.opengl.demos.nvidia.face.sample;

import com.nvidia.developer.opengl.app.NvInputHandler_CameraFly;

import org.lwjgl.util.vector.Vector3f;

import java.util.List;

import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/9/7.
 */

final class CSceneLPSHead implements CScene {
    CMesh						m_meshHead;

    Texture2D m_pSrvDiffuseHead;
    Texture2D	m_pSrvNormalHead;
    Texture2D	m_pSrvSpecHead;
    Texture2D	m_pSrvDeepScatterHead;

    final Material	m_mtlHead = new Material();

    NvInputHandler_CameraFly			m_camera;

    int							m_normalHeadSize;

    @Override
    public void Init() {
        // Load meshes

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"LPSHead\\head.obj"));
//        V_RETURN(LoadObjMesh(strPath, pDevice, &m_meshHead));
        m_meshHead = new CMesh();
        m_meshHead.loadModel(MODEL_PATH+"LPSHead\\head.obj");

        // Load textures
//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"LPSHead\\lambertian.jpg"));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvDiffuseHead));
        m_pSrvDiffuseHead = CScene.loadTexture("LPSHead\\lambertian.jpg");

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"LPSHead\\normal.bmp"));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvNormalHead, LT_Mipmap | LT_Linear));
        m_pSrvNormalHead = CScene.loadTexture("LPSHead\\normal.bmp");

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"LPSHead\\deepscatter.bmp"));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvDeepScatterHead));
        m_pSrvDeepScatterHead = CScene.loadTexture("LPSHead\\deepscatter.bmp");

        // Create 1x1 spec texture

        m_pSrvSpecHead = CScene.Create1x1Texture(
                FaceWorkDemo.g_specReflectanceSkinDefault,
                FaceWorkDemo.g_specReflectanceSkinDefault,
                FaceWorkDemo.g_specReflectanceSkinDefault,
                true);

        // Set up materials

        m_mtlHead.m_shader = SHADER.Skin;
        m_mtlHead.m_aSrv[0] = m_pSrvDiffuseHead;		m_mtlHead.m_textureSlots[0] = CShaderManager.TEX_DIFFUSE0;
        m_mtlHead.m_aSrv[1] = m_pSrvNormalHead;			m_mtlHead.m_textureSlots[1] = CShaderManager.TEX_NORMAL;
        m_mtlHead.m_aSrv[2] = m_pSrvSpecHead;			m_mtlHead.m_textureSlots[2] = CShaderManager.TEX_SPEC;
        m_mtlHead.m_aSrv[3] = m_pSrvDeepScatterHead;	m_mtlHead.m_textureSlots[3] = CShaderManager.TEX_DEEP_SCATTER_COLOR;
        m_mtlHead.m_constants[0] = FaceWorkDemo.g_normalStrength;

        // Set up camera to orbit around the head

//        XMVECTOR posLookAt = XMLoadFloat3(&m_meshHead.m_posCenter) + XMVectorSet(0.0f, 3.0f, 0.0f, 0.0f);
//        XMVECTOR posCamera = posLookAt + XMVectorSet(0.0f, 0.0f, 60.0f, 0.0f);
//        m_camera.SetViewParams(posCamera, posLookAt);
        m_camera = new NvInputHandler_CameraFly();
        m_camera.setPosition(new Vector3f(m_meshHead.m_posCenter.x, m_meshHead.m_posCenter.y + 3,  m_meshHead.m_posCenter.z + 60));

        // Pull out normal map texture size for SSS mip level calculations

        m_normalHeadSize = CScene.GetTextureSize(m_pSrvNormalHead);
    }

    @Override
    public void Release() {
        m_meshHead.dispose();

        CommonUtil.safeRelease(m_pSrvDiffuseHead);
        CommonUtil.safeRelease(m_pSrvNormalHead);
        CommonUtil.safeRelease(m_pSrvSpecHead);
        CommonUtil.safeRelease(m_pSrvDeepScatterHead);
    }

    @Override
    public NvInputHandler_CameraFly Camera() {
        return m_camera;
    }

    @Override
    public void GetBounds(Vector3f pPosMin, Vector3f pPosMax) {
        pPosMin.set(m_meshHead.getPosMin());
        pPosMax.set(m_meshHead.getPosMax());
    }

    @Override
    public void GetMeshesToDraw(List<MeshToDraw> pMeshesToDraw) {
        // Allow updating normal strength and gloss in real-time
        m_mtlHead.m_constants[0] = FaceWorkDemo.g_normalStrength;
        m_mtlHead.m_constants[1] = FaceWorkDemo.g_glossSkin;

        // Generate draw records

//        MeshToDraw aMtd[] =
//                {
//        { &m_mtlHead, &m_meshHead, m_normalHeadSize, m_meshHead.m_uvScale, },
//        };
//
//        pMeshesToDraw->assign(&aMtd[0], &aMtd[dim(aMtd)]);
        pMeshesToDraw.add(new MeshToDraw(m_mtlHead, m_meshHead, m_normalHeadSize, m_meshHead.m_uvScale));
    }
}
