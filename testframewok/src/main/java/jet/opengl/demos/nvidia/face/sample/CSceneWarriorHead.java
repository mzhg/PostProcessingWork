package jet.opengl.demos.nvidia.face.sample;

import com.nvidia.developer.opengl.app.NvInputHandler_CameraFly;

import org.lwjgl.util.vector.Vector3f;

import java.util.List;

import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/9/7.
 */

final class CSceneWarriorHead implements CScene{
    CMesh						m_meshHead;
    CMesh						m_meshEyeL;
    CMesh						m_meshEyeR;
    CMesh						m_meshLashes;

    Texture2D m_pSrvDiffuseHead;
    Texture2D	m_pSrvNormalHead;
    Texture2D	m_pSrvSpecHead;
    Texture2D	m_pSrvDeepScatterHead;

    Texture2D	m_pSrvDiffuseEyeSclera;
    Texture2D	m_pSrvNormalEyeSclera;
    Texture2D	m_pSrvDiffuseEyeIris;

    Texture2D	m_pSrvDiffuseLashes;

    Material					m_mtlHead;
    Material					m_mtlEye;
    Material					m_mtlLashes;

    NvInputHandler_CameraFly			m_camera;

    int							m_normalHeadSize;
    int							m_normalEyeSize;

    @Override
    public void Init() {
        // Load meshes

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"WarriorHead\\WarriorHead.obj"));
//        V_RETURN(LoadObjMesh(strPath, pDevice, &m_meshHead));
        m_meshHead = new CMesh();
        m_meshHead.loadModel("WarriorHead\\WarriorHead.obj");

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"WarriorHead\\EyeL.obj"));
//        V_RETURN(LoadObjMesh(strPath, pDevice, &m_meshEyeL));
        m_meshEyeL = new CMesh();
        m_meshEyeL.loadModel("WarriorHead\\EyeL.obj");

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"WarriorHead\\EyeR.obj"));
//        V_RETURN(LoadObjMesh(strPath, pDevice, &m_meshEyeR));
        m_meshEyeR = new CMesh();
        m_meshEyeR.loadModel("WarriorHead\\EyeR.obj");

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"WarriorHead\\Lashes.obj"));
//        V_RETURN(LoadObjMesh(strPath, pDevice, &m_meshLashes));
        m_meshLashes = new CMesh();
        m_meshLashes.loadModel("WarriorHead\\Lashes.obj");

        // Load textures

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"WarriorHead\\diffuse.bmp"));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvDiffuseHead));
        m_pSrvDiffuseHead = CScene.loadTexture("WarriorHead\\diffuse.bmp");

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"WarriorHead\\normal.bmp"));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvNormalHead, LT_Mipmap | LT_Linear));
        m_pSrvNormalHead = CScene.loadTexture("WarriorHead\\normal.bmp");

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"WarriorHead\\deepscatter.bmp"));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvDeepScatterHead));
        m_pSrvDeepScatterHead = CScene.loadTexture("WarriorHead\\deepscatter.bmp");

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"WarriorHead\\eyeHazel.bmp"));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvDiffuseEyeSclera));
        m_pSrvDiffuseEyeSclera = CScene.loadTexture("WarriorHead\\eyeHazel.bmp");

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"DigitalIra\\eyeballNormalMap.bmp"));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvNormalEyeSclera, LT_Mipmap | LT_Linear));
        m_pSrvNormalEyeSclera = CScene.loadTexture("WarriorHead\\eyeballNormalMap.bmp");

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"WarriorHead\\eyeHazel.bmp"));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvDiffuseEyeIris));
        m_pSrvDiffuseEyeIris = CScene.loadTexture("WarriorHead\\eyeHazel.bmp");

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"WarriorHead\\lashes.bmp"));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvDiffuseLashes));
        m_pSrvDiffuseLashes = CScene.loadTexture("WarriorHead\\lashes.bmp");

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
        m_mtlHead.m_constants[1] = FaceWorkDemo.g_glossSkin;

        m_mtlEye.m_shader = SHADER.Eye;
        m_mtlEye.m_aSrv[0] = m_pSrvDiffuseEyeSclera;	m_mtlEye.m_textureSlots[0] = CShaderManager.TEX_DIFFUSE0;
        m_mtlEye.m_aSrv[1] = m_pSrvDiffuseEyeIris;		m_mtlEye.m_textureSlots[1] = CShaderManager.TEX_DIFFUSE1;
        m_mtlEye.m_aSrv[2] = m_pSrvNormalEyeSclera;		m_mtlEye.m_textureSlots[2] = CShaderManager.TEX_NORMAL;
        m_mtlEye.m_constants[0] = 0.05f;	// normal strength
        m_mtlEye.m_constants[1] = FaceWorkDemo.g_specReflectanceEye;
        m_mtlEye.m_constants[2] = FaceWorkDemo.g_glossEye;
        m_mtlEye.m_constants[4] = FaceWorkDemo.g_rgbDeepScatterEye[0];
        m_mtlEye.m_constants[5] = FaceWorkDemo.g_rgbDeepScatterEye[1];
        m_mtlEye.m_constants[6] = FaceWorkDemo.g_rgbDeepScatterEye[2];
        m_mtlEye.m_constants[7] = 0.200f;	// Radius of iris in iris texture (in UV units)
        m_mtlEye.m_constants[8] = 0.205f;	// Radius of iris in schlera texture (in UV units)
        m_mtlEye.m_constants[9] = 30.0f;	// Controls hardness/softness of iris edge
        m_mtlEye.m_constants[10] = 0.0f;	// How much the iris is dilated

        m_mtlLashes.m_shader = SHADER.Hair;
        m_mtlLashes.m_aSrv[0] = m_pSrvDiffuseLashes;	m_mtlLashes.m_textureSlots[0] = CShaderManager.TEX_DIFFUSE0;
        m_mtlLashes.m_constants[0] = FaceWorkDemo.g_specReflectanceHair;
        m_mtlLashes.m_constants[1] = FaceWorkDemo.g_glossHair;

        // Set up camera to orbit around the head

//        XMVECTOR posLookAt = XMLoadFloat3(&m_meshHead.m_posCenter);
//        XMVECTOR posCamera = posLookAt + XMVectorSet(0.0f, 0.0f, 60.0f, 0.0f);
//        m_camera.SetViewParams(posCamera, posLookAt);
        m_camera = new NvInputHandler_CameraFly();
        m_camera.setPosition(new Vector3f(m_meshHead.m_posCenter.x, m_meshHead.m_posCenter.y,m_meshHead.m_posCenter.z + 60));

        // Pull out normal map texture sizes for SSS mip level calculations

        m_normalHeadSize = CScene.GetTextureSize(m_pSrvNormalHead);
        m_normalEyeSize = CScene.GetTextureSize(m_pSrvNormalEyeSclera);
    }

    @Override
    public void Release() {
        m_meshHead.dispose();
        m_meshEyeL.dispose();
        m_meshEyeR.dispose();
        m_meshLashes.dispose();

        CommonUtil.safeRelease(m_pSrvDiffuseHead);
        CommonUtil.safeRelease(m_pSrvNormalHead);
        CommonUtil.safeRelease(m_pSrvSpecHead);
        CommonUtil.safeRelease(m_pSrvDeepScatterHead);

        CommonUtil.safeRelease(m_pSrvDiffuseEyeSclera);
        CommonUtil.safeRelease(m_pSrvNormalEyeSclera);
        CommonUtil.safeRelease(m_pSrvDiffuseEyeIris);

        CommonUtil.safeRelease(m_pSrvDiffuseLashes);
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
        m_mtlEye.m_constants[2] = FaceWorkDemo.g_glossEye;

        m_mtlEye.m_constants[7] = FaceWorkDemo.g_irisRadiusSource;
        m_mtlEye.m_constants[8] = FaceWorkDemo.g_irisRadiusDest;
        m_mtlEye.m_constants[9] = FaceWorkDemo.g_irisEdgeHardness;
        m_mtlEye.m_constants[10] = FaceWorkDemo.g_irisDilation;

        m_mtlLashes.m_constants[0] = FaceWorkDemo.g_specReflectanceHair;
        m_mtlLashes.m_constants[1] = FaceWorkDemo.g_glossHair;

        // Generate draw records

//        MeshToDraw aMtd[] =
//                {
//        { &m_mtlHead, &m_meshHead, m_normalHeadSize, m_meshHead.m_uvScale, },
//        { &m_mtlEye, &m_meshEyeL, m_normalEyeSize, m_meshEyeL.m_uvScale, },
//        { &m_mtlEye, &m_meshEyeR, m_normalEyeSize, m_meshEyeR.m_uvScale, },
//        { &m_mtlLashes, &m_meshLashes, },
//        };
//        pMeshesToDraw->assign(&aMtd[0], &aMtd[dim(aMtd)]);
        pMeshesToDraw.add(new MeshToDraw(m_mtlHead, m_meshHead, m_normalHeadSize, m_meshHead.m_uvScale));
        pMeshesToDraw.add(new MeshToDraw(m_mtlEye, m_meshEyeL, m_normalEyeSize, m_meshEyeL.m_uvScale));
        pMeshesToDraw.add(new MeshToDraw(m_mtlEye, m_meshEyeR, m_normalEyeSize, m_meshEyeR.m_uvScale));
        pMeshesToDraw.add(new MeshToDraw(m_mtlLashes, m_meshLashes));
    }
}
