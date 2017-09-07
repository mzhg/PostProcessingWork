package jet.opengl.demos.nvidia.face.sample;

import com.nvidia.developer.opengl.app.NvInputHandler_CameraFly;

import org.lwjgl.util.vector.Vector3f;

import java.util.List;

import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/9/7.
 */

final class CSceneDigitalIra implements CScene {
    CMesh						m_meshHead;
    CMesh						m_meshEyeL;
    CMesh						m_meshEyeR;
    CMesh						m_meshLashes;
    CMesh						m_meshBrows;

    Texture2D m_pSrvDiffuseHead;
    Texture2D	m_pSrvNormalHead;
    Texture2D	m_pSrvSpecHead;
    Texture2D	m_pSrvDeepScatterHead;

    Texture2D	m_pSrvDiffuseEyeSclera;
    Texture2D	m_pSrvNormalEyeSclera;
    Texture2D	m_pSrvDiffuseEyeIris;

    Texture2D	m_pSrvDiffuseLashes;
    Texture2D	m_pSrvDiffuseBrows;

    Material					m_mtlHead;
    Material					m_mtlEye;
    Material					m_mtlLashes;
    Material					m_mtlBrows;

    NvInputHandler_CameraFly			m_camera;

    int							m_normalHeadSize;
    int							m_normalEyeSize;
    
    @Override
    public void Init() {
        m_meshHead = new CMesh();
        m_meshHead.loadModel(MODEL_PATH + "DigitalIra\\HumanHead.obj");

        m_meshEyeL = new CMesh();
        m_meshEyeL.loadModel(MODEL_PATH+"DigitalIra\\EyeL.obj");

        m_meshEyeR = new CMesh();
        m_meshEyeR.loadModel(MODEL_PATH + "DigitalIra\\EyeR.obj");

        m_meshLashes =new CMesh();
        m_meshLashes.loadModel(MODEL_PATH + "DigitalIra\\Lashes.obj");

        m_meshBrows = new CMesh();
        m_meshBrows.loadModel(MODEL_PATH + "DigitalIra\\Brows.obj");

        // Load textures

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"DigitalIra\\00_diffuse_albedo.bmp"));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvDiffuseHead));
        m_pSrvDiffuseHead = CScene.loadTexture("DigitalIra\\00_diffuse_albedo.bmp");

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"DigitalIra\\00_specular_normal_tangent.bmp"));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvNormalHead, LT_Mipmap | LT_Linear));
        m_pSrvNormalHead = CScene.loadTexture("DigitalIra\\00_specular_normal_tangent.bmp");

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"DigitalIra\\00_specular_albedo.bmp"));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvSpecHead));
        m_pSrvSpecHead = CScene.loadTexture("DigitalIra\\00_specular_albedo.bmp");

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"DigitalIra\\HumanHead_deepscatter.bmp"));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvDeepScatterHead));
        m_pSrvSpecHead = CScene.loadTexture("DigitalIra\\00_specular_albedo.bmp");

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"DigitalIra\\sclera_col.bmp"));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvDiffuseEyeSclera));
        m_pSrvSpecHead = CScene.loadTexture("DigitalIra\\00_specular_albedo.bmp");

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"DigitalIra\\eyeballNormalMap.bmp"));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvNormalEyeSclera, LT_Mipmap | LT_Linear));
        m_pSrvNormalEyeSclera = CScene.loadTexture("DigitalIra\\eyeballNormalMap.bmp");

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"DigitalIra\\iris.bmp"));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvDiffuseEyeIris));
        m_pSrvDiffuseEyeIris = CScene.loadTexture("DigitalIra\\iris.bmp");

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"DigitalIra\\lashes.dds"));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvDiffuseLashes));
        m_pSrvDiffuseLashes = CScene.loadTexture("DigitalIra\\lashes.bmp");

//        V_RETURN(DXUTFindDXSDKMediaFileCch(strPath, dim(strPath), L"DigitalIra\\brows.dds"));
//        V_RETURN(LoadTexture(strPath, pDevice, pDeviceContext, &m_pSrvDiffuseBrows));
        m_pSrvDiffuseBrows = CScene.loadTexture("DigitalIra\\brows.bmp");

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
        m_mtlEye.m_constants[7] = 0.455f;	// Radius of iris in iris texture (in UV units);
        m_mtlEye.m_constants[8] = 0.230f;	// Radius of iris in schlera texture (in UV units);
        m_mtlEye.m_constants[9] = 30.0f;	// Controls hardness/softness of iris edge;
        m_mtlEye.m_constants[10] = 0.5f;	// How much the iris is dilated;

        m_mtlLashes.m_shader = SHADER.Hair;
        m_mtlLashes.m_aSrv[0] = m_pSrvDiffuseLashes;	m_mtlLashes.m_textureSlots[0] = CShaderManager.TEX_DIFFUSE0;
        m_mtlLashes.m_constants[0] = FaceWorkDemo.g_specReflectanceHair;
        m_mtlLashes.m_constants[1] = FaceWorkDemo.g_glossHair;

        m_mtlBrows.m_shader = SHADER.Hair;
        m_mtlBrows.m_aSrv[0] = m_pSrvDiffuseBrows;		m_mtlBrows.m_textureSlots[0] = CShaderManager.TEX_DIFFUSE0;
        m_mtlBrows.m_constants[0] = FaceWorkDemo.g_specReflectanceHair;
        m_mtlBrows.m_constants[1] = FaceWorkDemo.g_glossHair;

        // Set up camera to orbit around the head

//        XMVECTOR posLookAt = XMLoadFloat3(&m_meshHead.m_posCenter);
//        XMVECTOR posCamera = posLookAt + XMVectorSet(0.0f, 0.0f, 60.0f, 0.0f);
//        m_camera.SetViewParams(posCamera, posLookAt);
        m_camera = new NvInputHandler_CameraFly();
        m_camera.setPosition(new Vector3f(m_meshHead.m_posCenter.x, m_meshHead.m_posCenter.y + 60.0f, m_meshHead.m_posCenter.z));

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
        m_meshBrows.dispose();

        CommonUtil.safeRelease(m_pSrvDiffuseHead);
        CommonUtil.safeRelease(m_pSrvNormalHead);
        CommonUtil.safeRelease(m_pSrvSpecHead);
        CommonUtil.safeRelease(m_pSrvDeepScatterHead);

        CommonUtil.safeRelease(m_pSrvDiffuseEyeSclera);
        CommonUtil.safeRelease(m_pSrvNormalEyeSclera);
        CommonUtil.safeRelease(m_pSrvDiffuseEyeIris);

        CommonUtil.safeRelease(m_pSrvDiffuseLashes);
        CommonUtil.safeRelease(m_pSrvDiffuseBrows);
    }

    @Override
    public NvInputHandler_CameraFly Camera() {
        return m_camera;
    }

    @Override
    public void GetBounds(Vector3f pPosMin, Vector3f pPosMax) {
        pPosMax.set(m_meshHead.getPosMax());
        pPosMin.set(m_meshHead.getPosMin());
    }

    @Override
    public void GetMeshesToDraw(List<MeshToDraw> pMeshesToDraw) {
        assert(pMeshesToDraw != null);

        // Allow updating normal strength and gloss in real-time
        m_mtlHead.m_constants[0] = FaceWorkDemo.g_normalStrength;
        m_mtlHead.m_constants[1] = FaceWorkDemo.g_glossSkin;
        m_mtlEye.m_constants[2] = FaceWorkDemo.g_glossEye;
        m_mtlLashes.m_constants[0] = FaceWorkDemo.g_specReflectanceHair;
        m_mtlLashes.m_constants[1] = FaceWorkDemo.g_glossHair;
        m_mtlBrows.m_constants[0] = FaceWorkDemo.g_specReflectanceHair;
        m_mtlBrows.m_constants[1] = FaceWorkDemo.g_glossHair;

        // Generate draw records

//        MeshToDraw aMtd[] =
//                {
//        { m_mtlHead, m_meshHead, m_normalHeadSize, m_meshHead.m_uvScale, },
//        { m_mtlEye, m_meshEyeL, m_normalEyeSize, m_meshEyeL.m_uvScale, },
//        { m_mtlEye, m_meshEyeR, m_normalEyeSize, m_meshEyeR.m_uvScale, },
//        { m_mtlLashes, m_meshLashes},
//        { m_mtlBrows, m_meshBrows, },
//        };
//
//        pMeshesToDraw->assign(&aMtd[0], &aMtd[dim(aMtd)]);
        pMeshesToDraw.add(new MeshToDraw(m_mtlHead, m_meshHead, m_normalHeadSize, m_meshHead.m_uvScale));
        pMeshesToDraw.add(new MeshToDraw(m_mtlEye, m_meshEyeL, m_normalEyeSize, m_meshEyeL.m_uvScale));
        pMeshesToDraw.add(new MeshToDraw(m_mtlLashes, m_meshLashes));
        pMeshesToDraw.add(new MeshToDraw(m_mtlBrows, m_meshBrows));
    }
}
