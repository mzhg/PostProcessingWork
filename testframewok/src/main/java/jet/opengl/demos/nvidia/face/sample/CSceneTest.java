package jet.opengl.demos.nvidia.face.sample;

import com.nvidia.developer.opengl.app.NvInputHandler_CameraFly;

import org.lwjgl.util.vector.Vector3f;

import java.util.List;

import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/9/7.
 */

final class CSceneTest implements CScene{
    CMesh						m_meshPlanes;
    CMesh						m_meshShadower;
    CMesh[]						m_aMeshSpheres = new CMesh[7];

    Texture2D m_pSrvDiffuse;
    Texture2D	m_pSrvNormalFlat;
    Texture2D	m_pSrvSpec;
    Texture2D	m_pSrvDeepScatter;

    Material					m_mtl;

    NvInputHandler_CameraFly		m_camera;

    @Override
    public void Init() {
        // Load meshes

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"testPlanes.obj"));
//        V_RETURN(LoadObjMesh(strPath, pDevice, &m_meshPlanes));
        m_meshPlanes = new CMesh();
        m_meshPlanes.loadModel("testPlanes.obj");


//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"testShadowCaster.obj"));
//        V_RETURN(LoadObjMesh(strPath, pDevice, &m_meshShadower));
        m_meshShadower = new CMesh();
        m_meshShadower.loadModel("testShadowCaster.obj");

        final String aStrSphereNames[] =
        {
            "testSphere1mm.obj",
            "testSphere2mm.obj",
            "testSphere5mm.obj",
            "testSphere1cm.obj",
            "testSphere2cm.obj",
            "testSphere5cm.obj",
            "testSphere10cm.obj",
        };
//        static_assert(dim(aStrSphereNames) == dim(m_aMeshSpheres), "dimension mismatch between array aStrSphereNames and m_aMeshSpheres");

        for (int i = 0; i < m_aMeshSpheres.length; ++i)
        {
//            V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), aStrSphereNames[i]));
//            V_RETURN(LoadObjMesh(strPath, pDevice, &m_aMeshSpheres[i]));

            m_aMeshSpheres[i] = new CMesh();
            m_aMeshSpheres[i].loadModel(aStrSphereNames[i]);
        }

        // Create 1x1 textures

        m_pSrvDiffuse = CScene.Create1x1Texture(1.0f, 1.0f, 1.0f);
        m_pSrvNormalFlat = CScene.Create1x1Texture(0.5f, 0.5f, 1.0f, true);
        m_pSrvSpec = CScene.Create1x1Texture(0.05f, 0.05f, 0.05f, true);
        m_pSrvDeepScatter = CScene.Create1x1Texture(1.0f, 0.25f, 0.0f);

        // Set up material

        m_mtl.m_shader = SHADER.Skin;
        m_mtl.m_aSrv[0] = m_pSrvDiffuse;		m_mtl.m_textureSlots[0] = CShaderManager.TEX_DIFFUSE0;
        m_mtl.m_aSrv[1] = m_pSrvNormalFlat;		m_mtl.m_textureSlots[1] = CShaderManager.TEX_NORMAL;
        m_mtl.m_aSrv[2] = m_pSrvSpec;			m_mtl.m_textureSlots[2] = CShaderManager.TEX_SPEC;
        m_mtl.m_aSrv[2] = m_pSrvDeepScatter;	m_mtl.m_textureSlots[3] = CShaderManager.TEX_DEEP_SCATTER_COLOR;
        m_mtl.m_constants[0] = FaceWorkDemo.g_normalStrength;
        m_mtl.m_constants[1] = FaceWorkDemo.g_glossSkin;

        // Set up camera in a default location

        Vector3f posMin = new Vector3f(), posMax = new Vector3f();
        GetBounds(posMin, posMax);
//        XMVECTOR posLookAt = 0.5f * (XMLoadFloat3(&posMin) + XMLoadFloat3(&posMax));
        Vector3f posLookAt = Vector3f.mix(posMax, posMin, 0.5f, null);
//        XMVECTOR posCamera = posLookAt + XMVectorSet(0.0f, 0.0f, 60.0f, 0.0f);
        posLookAt.z += 60;
//        m_camera.SetViewParams(posCamera, posLookAt);
        m_camera = new NvInputHandler_CameraFly();
        m_camera.setPosition(posLookAt);

        // Adjust camera speed
//        m_camera.SetScalers(0.005f, 10.0f);
    }

    @Override
    public void Release() {
        m_meshPlanes.dispose();
        m_meshShadower.dispose();

        for (int i = 0; i < m_aMeshSpheres.length; ++i)
            m_aMeshSpheres[i].dispose();

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
        assert(pPosMin != null);
        assert(pPosMax != null);

//        XMVECTOR posMin = XMLoadFloat3(&m_meshPlanes.m_posMin);
//        XMVECTOR posMax = XMLoadFloat3(&m_meshPlanes.m_posMax);

//        posMin = XMVectorMin(posMin, XMLoadFloat3(&m_meshShadower.m_posMin));
//        posMax = XMVectorMax(posMax, XMLoadFloat3(&m_meshShadower.m_posMax));
        Vector3f.min(m_meshPlanes.m_posMin, m_meshShadower.m_posMin, pPosMin);
        Vector3f.max(m_meshPlanes.m_posMax, m_meshShadower.m_posMax, pPosMax);

        for (int i = 0; i < m_aMeshSpheres.length; ++i)
        {
//            posMin = XMVectorMin(posMin, XMLoadFloat3(&m_aMeshSpheres[i].m_posMin));
//            posMax = XMVectorMax(posMax, XMLoadFloat3(&m_aMeshSpheres[i].m_posMax));
            Vector3f.min(pPosMin, m_aMeshSpheres[i].m_posMin, pPosMin);
            Vector3f.max(pPosMax, m_aMeshSpheres[i].m_posMax, pPosMax);
        }

//        XMStoreFloat3(pPosMin, posMin);
//        XMStoreFloat3(pPosMax, posMax);
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
//                        { &m_mtl, &m_meshShadower, 0, 1.0f, },
//        { &m_mtl, &m_meshPlanes, 0, 1.0f, },
//        };
//        pMeshesToDraw->assign(&aMtd[0], &aMtd[dim(aMtd)]);
        pMeshesToDraw.add(new MeshToDraw(m_mtl, m_meshShadower,0, 1.0f));
        pMeshesToDraw.add(new MeshToDraw(m_mtl, m_meshPlanes, 0, 1.0f));

        for (int i = 0; i < m_aMeshSpheres.length; ++i)
        {
//            MeshToDraw mtd = { &m_mtl, &m_aMeshSpheres[i], 0, 1.0f };
//            pMeshesToDraw->push_back(mtd);
            pMeshesToDraw.add(new MeshToDraw(m_mtl, m_aMeshSpheres[i], 0, 1.0f));
        }
    }
}
