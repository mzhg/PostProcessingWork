package jet.opengl.demos.nvidia.face.sample;

import com.nvidia.developer.opengl.app.NvInputHandler_CameraFly;

import org.lwjgl.util.vector.Vector3f;

import java.util.List;

import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/9/7.
 */

final class CSceneHand implements CScene{
    CMesh						m_meshHand;

    Texture2D   m_pSrvDiffuse;
    Texture2D	m_pSrvNormalFlat;
    Texture2D	m_pSrvSpec;
    Texture2D	m_pSrvDeepScatter;

    Material					m_mtl;

    NvInputHandler_CameraFly			m_camera;

    @Override
    public void Init() {
        // Load meshes

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"hand01.obj"));
//        V_RETURN(LoadObjMesh(strPath, pDevice, &m_meshHand));
        m_meshHand = new CMesh();
        m_meshHand.loadModel("hand01.obj");

        // Create 1x1 textures

        m_pSrvDiffuse = CScene.Create1x1Texture(0.773f, 0.540f, 0.442f);	// caucasian skin color
        m_pSrvNormalFlat = CScene.Create1x1Texture(0.5f, 0.5f, 1.0f, true);
        m_pSrvSpec = CScene.Create1x1Texture(0.05f, 0.05f, 0.05f, true);
        m_pSrvDeepScatter = CScene.Create1x1Texture(1.0f, 0.25f, 0.0f);

        // Set up material

        m_mtl.m_shader = SHADER.Skin;
        m_mtl.m_aSrv[0] = m_pSrvDiffuse;		m_mtl.m_textureSlots[0] = CShaderManager.TEX_DIFFUSE0;
        m_mtl.m_aSrv[1] = m_pSrvNormalFlat;		m_mtl.m_textureSlots[1] = CShaderManager.TEX_NORMAL;
        m_mtl.m_aSrv[2] = m_pSrvSpec;			m_mtl.m_textureSlots[2] = CShaderManager.TEX_SPEC;
        m_mtl.m_aSrv[3] = m_pSrvDeepScatter;	m_mtl.m_textureSlots[3] = CShaderManager.TEX_DEEP_SCATTER_COLOR;
        m_mtl.m_constants[0] = FaceWorkDemo.g_normalStrength;
        m_mtl.m_constants[1] = FaceWorkDemo.g_glossSkin;

        // Set up camera to orbit around the hand

//        XMVECTOR posLookAt = XMLoadFloat3(&m_meshHand.m_posCenter);
//        XMVECTOR posCamera = posLookAt + XMVectorSet(0.0f, 0.0f, 60.0f, 0.0f);
//        m_camera.SetViewParams(posCamera, posLookAt);
        m_camera = new NvInputHandler_CameraFly();
        m_camera.setPosition(new Vector3f(m_meshHand.m_posCenter.x, m_meshHand.m_posCenter.y, m_meshHand.m_posCenter.z + 60));
    }

    @Override
    public void Release() {
        m_meshHand.dispose();

        CommonUtil.safeRelease(m_pSrvDiffuse);
        CommonUtil.safeRelease(m_pSrvNormalFlat);
        CommonUtil.safeRelease(m_pSrvSpec);
        CommonUtil.safeRelease(m_pSrvDeepScatter);
    }

    @Override
    public NvInputHandler_CameraFly Camera() {
        return m_camera;
    }

    @Override
    public void GetBounds(Vector3f pPosMin, Vector3f pPosMax) {
        pPosMax.set(m_meshHand.getPosMax());
        pPosMin.set(m_meshHand.getPosMin());
    }

    @Override
    public void GetMeshesToDraw(List<MeshToDraw> pMeshesToDraw) {
        assert(pMeshesToDraw != null);

        // Allow updating normal strength and gloss in real-time
        m_mtl.m_constants[0] = FaceWorkDemo.g_normalStrength;
        m_mtl.m_constants[1] = FaceWorkDemo.g_glossSkin;

        // Generate draw records

//        MeshToDraw aMtd[] =
//                {
//                        { &m_mtl, &m_meshHand, 0, 1.0f, },
//        };
//        pMeshesToDraw->assign(&aMtd[0], &aMtd[dim(aMtd)]);
        pMeshesToDraw.add(new MeshToDraw(m_mtl, m_meshHand, 0, 1.0f));
    }
}
