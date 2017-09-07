package jet.opengl.demos.nvidia.face.sample;

import com.nvidia.developer.opengl.app.NvInputHandler_CameraFly;

import org.lwjgl.util.vector.Vector3f;

import java.util.List;

import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/9/7.
 */

final class CSceneManjaladon implements CScene{
    CMesh						m_meshManjaladon;

    Texture2D m_pSrvDiffuse;
    Texture2D	m_pSrvNormal;
    Texture2D	m_pSrvSpec;
    Texture2D	m_pSrvDeepScatter;

    Material					m_mtl;

    NvInputHandler_CameraFly			m_camera;

    int							m_normalSize;

    @Override
    public void Init() {
        // Load meshes

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"Manjaladon\\manjaladon.obj"));
//        V_RETURN(LoadObjMesh(strPath, pDevice, &m_meshManjaladon));
        m_meshManjaladon = new CMesh();
        m_meshManjaladon.loadModel("Manjaladon\\manjaladon.obj");

        // Load textures

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"Manjaladon\\Manjaladon_d.bmp"));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvDiffuse));
        m_pSrvDiffuse = CScene.loadTexture("Manjaladon\\Manjaladon_d.bmp");

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"Manjaladon\\Manjaladon_n.bmp"));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvNormal, LT_Mipmap | LT_Linear));
        m_pSrvNormal = CScene.loadTexture("Manjaladon\\Manjaladon_n.bmp");

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"Manjaladon\\Manjaladon_s.bmp"));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvSpec));
        m_pSrvSpec = CScene.loadTexture("Manjaladon\\Manjaladon_s.bmp");

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"Manjaladon\\Manjaladon_subsurface.bmp"));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvDeepScatter));
        m_pSrvDeepScatter = CScene.loadTexture("Manjaladon\\Manjaladon_subsurface.bmp");

        // Set up materials

        m_mtl.m_shader = SHADER.Skin;
        m_mtl.m_aSrv[0] = m_pSrvDiffuse;		m_mtl.m_textureSlots[0] = CShaderManager.TEX_DIFFUSE0;
        m_mtl.m_aSrv[1] = m_pSrvNormal;			m_mtl.m_textureSlots[1] = CShaderManager.TEX_NORMAL;
        m_mtl.m_aSrv[2] = m_pSrvSpec;			m_mtl.m_textureSlots[2] = CShaderManager.TEX_SPEC;
        m_mtl.m_aSrv[3] = m_pSrvDeepScatter;	m_mtl.m_textureSlots[3] = CShaderManager.TEX_DEEP_SCATTER_COLOR;
        m_mtl.m_constants[0] = FaceWorkDemo.g_normalStrength;
        m_mtl.m_constants[1] = FaceWorkDemo.g_glossSkin;

        // Set up camera to orbit around the manjaladon

//        XMVECTOR posLookAt = XMLoadFloat3(&m_meshManjaladon.m_posCenter) + XMVectorSet(0.0f, 0.0f, 5.0f, 0.0f);
//        XMVECTOR posCamera = posLookAt + XMVectorSet(0.0f, 0.0f, 40.0f, 0.0f);
//        m_camera.SetViewParams(posCamera, posLookAt);
        m_camera = new NvInputHandler_CameraFly();
        m_camera.setPosition(new Vector3f(m_meshManjaladon.m_posCenter.x, m_meshManjaladon.m_posCenter.y+5, m_meshManjaladon.m_posCenter.z+40));

        // Pull out normal map texture size for SSS mip level calculations

        m_normalSize = CScene.GetTextureSize(m_pSrvNormal);
    }

    @Override
    public void Release() {
        m_meshManjaladon.dispose();

        CommonUtil.safeRelease(m_pSrvDiffuse);
        CommonUtil.safeRelease(m_pSrvNormal);
        CommonUtil.safeRelease(m_pSrvSpec);
        CommonUtil.safeRelease(m_pSrvDeepScatter);
    }

    @Override
    public NvInputHandler_CameraFly Camera() {
        return m_camera;
    }

    @Override
    public void GetBounds(Vector3f pPosMin, Vector3f pPosMax) {
        pPosMin.set(m_meshManjaladon.getPosMin());
        pPosMax.set(m_meshManjaladon.getPosMax());
    }

    @Override
    public void GetMeshesToDraw(List<MeshToDraw> pMeshesToDraw) {
        // Allow updating normal strength and gloss in real-time
        m_mtl.m_constants[0] = FaceWorkDemo.g_normalStrength;
        m_mtl.m_constants[1] = FaceWorkDemo.g_glossSkin;

        // Generate draw records

//        MeshToDraw aMtd[] =
//                {
//        { &m_mtl, &m_meshManjaladon, m_normalSize, m_meshManjaladon.m_uvScale, },
//        };
//        pMeshesToDraw->assign(&aMtd[0], &aMtd[dim(aMtd)]);
        pMeshesToDraw.add(new MeshToDraw(m_mtl, m_meshManjaladon, m_normalSize, m_meshManjaladon.m_uvScale));
    }
}
