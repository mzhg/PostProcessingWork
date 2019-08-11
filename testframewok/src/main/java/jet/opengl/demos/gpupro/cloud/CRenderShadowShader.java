package jet.opengl.demos.gpupro.cloud;


import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.util.BoundingBox;

/**
 * Created by mazhen'gui on 2017/7/6.
 */

final class CRenderShadowShader extends  RenderTechnique{
    private SSceneParamter m_pSceneParam;
    private final BoundingBox bbGroundInView = new BoundingBox();
    private final BoundingBox bbCloudInView = new BoundingBox();
    private final BoundingBox bound = new BoundingBox();

    private final Matrix4f mW2Light = new Matrix4f();
    private final Matrix4f mProj = new Matrix4f();  // TODO not pure proj matrix.
    private final Matrix4f m_mW2SProj = new Matrix4f();
    private final Matrix4f m_bias  = new Matrix4f();
    private final Matrix4f m_mW2S  = new Matrix4f();

    CRenderShadowShader(SSceneParamter sceneParam) {
        super("CloudGridVS.vert", "CloudGridPS.frag", "SHAODW");
        m_pSceneParam = sceneParam;

        m_bias.set(0.5f,  0.0f, 0.0f, 0.0f,
                   0.0f,  0.5f, 0.0f, 0.0f,
                   0.0f,  0.0f, 0.5f, 0.0f,
                   0.5f,  0.5f, 0.5f, 1.0f);
    }

    Matrix4f GetW2ShadowMapMatrix() { return m_mW2S;}

    //--------------------------------------------------------------------------------------
// CRenderShadowShader::Update
//   Compute volume of the shadow
//--------------------------------------------------------------------------------------
    void Update(BoundingBox pGround, BoundingBox pCloud)
    {
        // look at the scene from light
//        D3DXVECTOR3 vLight( 0.0f, 0.0f, 0.0f );
//        D3DXVECTOR3 vUp( 0.0f, 1.0f, 0.0f );
//        D3DXVECTOR3 vAt;
//        D3DXVec3Add( &vAt, &vLight, &m_pSceneParam->m_vLightDir );
//        D3DXMATRIX mW2Light;
//        D3DXMatrixLookAtLH( &mW2Light, &vLight, &vAt, &vUp );
        Matrix4f.lookAt(0,0,0,  m_pSceneParam.m_vLightDir.x, m_pSceneParam.m_vLightDir.y, m_pSceneParam.m_vLightDir.z,
                        0,1,0, mW2Light);

        // transform ground and cloud bounding box to the light coordinate
//        SBoundingBox bbGroundInView;
//        SBoundingBox bbCloudInView;
//        pGround->Transform( bbGroundInView, &mW2Light );
//        pCloud->Transform( bbCloudInView, &mW2Light );
        BoundingBox.transform(mW2Light, pGround, bbGroundInView);
        BoundingBox.transform(mW2Light, pCloud, bbCloudInView);

        // minimize bounding box
        // The view frustom should be take into account as well.
//        SBoundingBox bound;
//        D3DXVec3Minimize( &bound._max, &bbGroundInView._max, &bbCloudInView._max );
//        D3DXVec3Maximize( &bound._min, &bbGroundInView._min, &bbCloudInView._min );
        bound._max.x = Math.min(bbGroundInView._max.x, bbCloudInView._max.x);
        bound._max.y = Math.min(bbGroundInView._max.y, bbCloudInView._max.y);
        bound._max.z = Math.min(bbGroundInView._max.z, bbCloudInView._max.z);

        bound._min.x = Math.max(bbGroundInView._min.x, bbCloudInView._min.x);
        bound._min.y = Math.max(bbGroundInView._min.y, bbCloudInView._min.y);
        bound._min.z = Math.max(bbGroundInView._min.z, bbCloudInView._min.z);
        bound._min.z = bbCloudInView._min.z;

        // if there is a valid volume
        if (bound._min.x < bound._max.x && bound._min.y < bound._max.y && bound._min.z < bound._max.z) {
            Vector3f vCenter = new Vector3f();
            Vector3f vDiag = new Vector3f();
            bound.center(vCenter);
//            D3DXVec3Subtract( &vDiag, &bound._max, &bound._min );
            Vector3f.sub(bound._max, bound._min, vDiag);

            // Move the view position to the center of the bounding box.
            // z is located behined the volume.
            Vector3f vEye = vCenter;
            vEye.z = vCenter.z - 0.5f * vDiag.z;
//            D3DXVECTOR3 vMove;
//            D3DXVec3Subtract( &vMove, &vLight, &vEye );
//            D3DXMATRIX mTrans;
//            D3DXMatrixTranslation( &mTrans, vMove.x, vMove.y, vMove.z );
            float vMoveX = 0-vEye.x;
            float vMoveY = 0-vEye.y;
            float vMoveZ = 0-vEye.z;

            // Orthogonal projection matrix
//            D3DXMATRIX mProj;
//            D3DXMatrixOrthoLH( &mProj, vDiag.x, vDiag.y, 0.0f, vDiag.z );
            Matrix4f.ortho(-vDiag.x/2, vDiag.x/2, -vDiag.y/2, -vDiag.y/2, 0.0f, vDiag.z, mProj);
            mProj.translate(vMoveX, vMoveY, vMoveZ);

            // Compute world to shadow map projection matrix
//            D3DXMatrixMultiply( &m_mW2SProj, &mW2Light, &mTrans );
//            D3DXMatrixMultiply( &m_mW2SProj, &m_mW2SProj, &mProj );
            Matrix4f.mul(mProj, mW2Light, m_mW2SProj);

            // Compute world to shadowmap texture coordinate matrix
//            D3DXMATRIX mProj2Tex(
//                    0.5f,  0.0f, 0.0f, 0.0f,
//                    0.0f, -0.5f, 0.0f, 0.0f,
//                    0.0f,  0.0f, 1.0f, 0.0f,
//                    0.5f,  0.5f, 0.0f, 1.0f );
//            D3DXMatrixMultiply( &m_mW2S, &m_mW2SProj, &mProj2Tex );
            Matrix4f.mul(m_bias, m_mW2SProj, m_mW2S);
        }
    }


    //--------------------------------------------------------------------------------------
// Setup shaders and shader constants.
//--------------------------------------------------------------------------------------
    boolean Begin(CCloudGrid cloud){
        enable();

        if (m_pSceneParam != null /*&& m_pSceneParam->m_pCamera != NULL*/) {
            // world to projection transform
//            SetVSMatrix( pDev, VS_CONST_W2C, m_pSceneParam->m_pCamera->GetWorld2ProjMatrix() );
            setW2C(m_pSceneParam.m_viewProj);
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
