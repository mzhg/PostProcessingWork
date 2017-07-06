package jet.opengl.demos.gpupro.cloud;

import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

/**
 * Created by mazhen'gui on 2017/7/6.
 */

public class CloudDemo extends NvSampleApp {

    CGround                     g_Ground;               // ground object
    CSkyPlane                   g_skyPlane;             // sky object
    CCloud                      g_cloud;                // cloud
    SSceneParamter               g_sceneParam;           // light and scattering parameters
    float                       g_fTime;                // time of a day

    private GLFuncProvider gl;

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        g_sceneParam = new SSceneParamter();
        // light
        g_sceneParam.m_vLightDir.set( 0.0f, -1.0f, 0.0f );
        g_sceneParam.m_vLightColor.set( 1.2f, 1.2f, 1.2f );
        g_sceneParam.m_vAmbientLight.set( 0.3f, 0.35f, 0.4f );
        // scattering
        g_sceneParam.m_vRayleigh.set( 0.3f, 0.45f, 6.5f );
        g_sceneParam.m_vMie.set( 0.3f, 0.3f, 0.3f );
        g_sceneParam.m_fG = 0.7f;
        g_sceneParam.m_fLightScale = 8.0f;
        g_sceneParam.m_fAmbientScale = 0.1f;
        // sky scattering
        g_sceneParam.m_fEarthRadius = 21600000.0f;
        g_sceneParam.m_fAtomosHeight = 30000.0f;
        g_sceneParam.m_fCloudHeight = 600.0f;
        // Time of the day.
        g_fTime = 0.5f;
        g_sceneParam.setTime( g_fTime );

        // Create cloud
        g_cloud = new CCloud();
        g_cloud.Create(g_sceneParam, getGLContext().width(), getGLContext().height());

        // Create Sky

        g_skyPlane.Create(g_sceneParam );

        // Create ground object
        g_Ground.create(g_sceneParam, "res\\GroundHeight.bmp",
                g_cloud.GetShadowMap(), g_cloud.GetWorld2ShadowMatrix() );

        // Setup the camera's view parameters
//        D3DXVECTOR3 vecEye;
//        D3DXVECTOR3 vecAt( 14000.0f, 0.0f, 12730.8f );
//        vecEye = vecAt + D3DXVECTOR3( -10.0f, 5.0f, -10.0f );
        m_transformer.setTranslation(-(14000.0f - 10.0f), -5.0f, -(12730.8f-10.0f));
//        g_Camera.SetViewParams( &vecEye, &vecAt );
//        g_Camera.SetGround( &g_Ground );
    }

    @Override
    public void display() {
        float fElapsedTime = getFrameDeltaTime();
        g_cloud.Update( fElapsedTime,g_Ground.GetBoundingBox() );

        // change scattering parameter accordint to cloud cover
        float fCloudCover = (g_cloud.GetCurrentCloudCover() - 0.7f) / (1.0f - 0.7f);
        fCloudCover = Math.max( 0.0f, fCloudCover );
        // mie scattering is caused by vapor.
        float fMie = 0.05f * (1.0f - fCloudCover) + 1.5f * fCloudCover;
        g_sceneParam.m_vMie.set( fMie, fMie, fMie );
        // rayleigh scattering
        float fRayleigh =  0.9f*fCloudCover + 1.0f*(1.0f-fCloudCover);
//        D3DXVECTOR3 vFineRayleigh( 0.05f, 0.15f, 1.5f );
//        D3DXVec3Scale( &g_sceneParam.m_vRayleigh, &vFineRayleigh, fRayleigh );
        g_sceneParam.m_vRayleigh.x = 0.05f * fRayleigh;
        g_sceneParam.m_vRayleigh.y = 0.15f * fRayleigh;
        g_sceneParam.m_vRayleigh.z = 1.50f * fRayleigh;
        // ambient
        Vector3f vFineAmbient = new Vector3f( 0.3f, 0.35f, 0.4f );
        Vector3f vCloudyAmbient = new Vector3f( 0.35f, 0.35f, 0.35f );
//        D3DXVec3Lerp( &g_sceneParam.m_vAmbientLight, &vFineAmbient, &vCloudyAmbient, fCloudCover );

        Vector3f.mix(vFineAmbient, vCloudyAmbient, fCloudCover, g_sceneParam.m_vAmbientLight);

        // when cloudy, ambient term of scattering is risen
        g_sceneParam.m_fAmbientScale = 0.5f * (1.0f - fCloudCover) + 1.0f * fCloudCover;

        // ------------------ Render Scene -----------------------------------
        // Render the scene
        // Clear all sampler
//        for (UINT i = 0; i < 8; ++i) {
//            pd3dDevice->SetTexture( i, NULL );
//            pd3dDevice->SetSamplerState( i, D3DSAMP_MAGFILTER, D3DTEXF_LINEAR );
//            pd3dDevice->SetSamplerState( i, D3DSAMP_MINFILTER, D3DTEXF_LINEAR );
//            pd3dDevice->SetSamplerState( i, D3DSAMP_MIPFILTER, D3DTEXF_LINEAR );
//        }

        // Render shadowmap, density and blur
        g_cloud.PrepareCloudTextures( /*pd3dDevice*/ );

        // Pass 3 : Draw scene

        // Clear the render target and the zbuffer
//        V( pd3dDevice->Clear( 0, NULL, D3DCLEAR_TARGET | D3DCLEAR_ZBUFFER, D3DCOLOR_ARGB( 0, 45, 50, 170 ), 1.0f, 0 ) );
        gl.glClearColor(45f/255, 50f/255, 70f/255, 0);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);

        // Draw Ground
        g_Ground.Draw( /*pd3dDevice*/ );

        // Draw sky plane
        g_skyPlane.Draw( /*pd3dDevice*/ );

        // Draw clouds
//        pd3dDevice->SetRenderState(D3DRS_ALPHABLENDENABLE, TRUE);
//        pd3dDevice->SetRenderState(D3DRS_ALPHATESTENABLE, TRUE);
//        pd3dDevice->SetRenderState(D3DRS_SRCBLEND, D3DBLEND_SRCALPHA);
//        pd3dDevice->SetRenderState(D3DRS_DESTBLEND, D3DBLEND_INVSRCALPHA);

        g_cloud.DrawFinalQuad( /*pd3dDevice*/ );

//        pd3dDevice->SetRenderState(D3DRS_ALPHABLENDENABLE, FALSE);


//        DXUT_BeginPerfEvent( DXUT_PERFEVENTCOLOR, L"HUD / Stats" ); // These events are to help PIX identify what the code is doing
//        RenderText();
//        V( g_HUD.OnRender( fElapsedTime ) );
//        V( g_SampleUI.OnRender( fElapsedTime ) );
//        DXUT_EndPerfEvent();
//
//        V( pd3dDevice->EndScene() );
    }
}
