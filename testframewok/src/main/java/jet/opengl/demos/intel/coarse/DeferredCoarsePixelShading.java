package jet.opengl.demos.intel.coarse;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.models.sdkmesh.SDKmesh;
import com.nvidia.developer.opengl.utils.NvImage;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;

import jet.opengl.demos.amdfx.common.CFirstPersonCamera;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureCube;
import jet.opengl.postprocessing.texture.TextureUtils;

public class DeferredCoarsePixelShading extends NvSampleApp {

    // Constants
    static final float kLightRotationSpeed = 0.05f;
    static final float kSliderFactorResolution = 10000.0f;

    /** SCENE_SELECTION */
    static final int
    POWER_PLANT_SCENE = 0,
    SPONZA_SCENE =1;

    App gApp = null;

    CFirstPersonCamera gViewerCamera;

    SDKmesh gMeshOpaque;
    SDKmesh gMeshAlpha;
    final Matrix4f gWorldMatrix = new Matrix4f();
    TextureCube gSkyboxSRV = null;

    // DXUT GUI stuff
    /*CDXUTDialogResourceManager gDialogResourceManager;
    CD3DSettingsDlg gD3DSettingsDlg;
    CDXUTDialog gHUD[HUD_NUM];
    CDXUTCheckBox* gAnimateLightCheck = 0;
    CDXUTComboBox* gMSAACombo = 0;
    CDXUTComboBox* gSceneSelectCombo = 0;
    CDXUTComboBox* gCullTechniqueCombo = 0;
    CDXUTSlider* gLightsSlider = 0;
    CDXUTTextHelper* gTextHelper = 0;*/

    float gAspectRatio;
    /*bool gDisplayUI = true;
    bool gZeroNextFrameTime = true;*/

    // Any UI state passed directly to rendering shaders
    final UIConstants gUIConstants = new UIConstants();

    void InitApp(/*ID3D11Device* d3dDevice*/)
    {
        DestroyApp();

        // Get current UI settings
        int msaaSamples = 4; // PtrToUint(gMSAACombo->GetSelectedData());
        gApp = new App(/*d3dDevice,*/ 256, msaaSamples);

        // Initialize with the current surface description
//        gApp->OnD3D11ResizedSwapChain(d3dDevice, DXUTGetDXGIBackBufferSurfaceDesc());

        // Zero out the elapsed time for the next frame
//        gZeroNextFrameTime = true;
    }


    void DestroyApp()
    {
        gApp.dispose();
        gApp = null;
    }

    void LoadSkybox(String fileName)
    {
        /*ID3D11Resource* resource = nullptr;
        HRESULT hr;
        // StephanieB5: All the texture files provide with sample files are DDS files
        hr = CreateDDSTextureFromFile(d3dDevice, fileName, &resource, nullptr, 0, nullptr);
        assert(SUCCEEDED(hr));

        d3dDevice->CreateShaderResourceView(resource, 0, &gSkyboxSRV);
        resource->Release();*/

        try {
            int cubeMap = NvImage.uploadTextureFromDDSFile(fileName);
            gSkyboxSRV = TextureUtils.createTextureCube(GLenum.GL_TEXTURE_CUBE_MAP, cubeMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void InitScene(/*ID3D11Device* d3dDevice*/)
    {
        DestroyScene();

        Vector3f cameraEye = new Vector3f(0.0f, 0.0f, 0.0f);
        Vector3f cameraAt = new Vector3f(0.0f, 0.0f, 0.0f);
        float sceneScaling = 1.0f;
        /*XMVECTOR sceneTranslation = XMVectorSet(0.0f, 0.0f, 0.0f, 0.0f);
        bool zAxisUp = false;

//        SCENE_SELECTION scene = static_cast<SCENE_SELECTION>(PtrToUlong(gSceneSelectCombo->GetSelectedData()));
        int scene = POWER_PLANT_SCENE;
        switch (scene) {
            case POWER_PLANT_SCENE: {
                gMeshOpaque.Create(d3dDevice, L"media\\powerplant\\powerplant.sdkmesh");
                LoadSkybox(d3dDevice, L"media\\Skybox\\Clouds.dds");
                cameraEye = XMVectorSet(100.0f, 5.0f, 5.0f, 0.0f);
                sceneScaling = 1.0f;
                // sceneScaling == 1.0f so no need to rescale cameraEye nor cameraAt
            } break;

            case SPONZA_SCENE: {
                gMeshOpaque.Create(d3dDevice, L"media\\Sponza\\sponza_dds.sdkmesh");
                LoadSkybox(d3dDevice, L"media\\Skybox\\Clouds.dds");
                cameraEye = XMVectorSet(1200.0f, 200.0f, 100.0f, 0.0f);
                sceneScaling = 0.05f;
                cameraEye *= sceneScaling;
                // cameraAt is a zero vector and is unaffected by scaling
            } break;
        };

        gWorldMatrix = XMMatrixScaling(sceneScaling, sceneScaling, sceneScaling);
        if (zAxisUp) {
            XMMATRIX m = XMMatrixRotationX(-XM_PI / 2.0f);
            gWorldMatrix *= m;
        }
        {
            XMMATRIX t = XMMatrixTranslationFromVector(sceneTranslation);
            gWorldMatrix *= t;
        }

        gViewerCamera.SetViewParams(cameraEye, cameraAt);
        gViewerCamera.SetScalers(0.01f, 10.0f);
        gViewerCamera.FrameMove(0.0f);

        // Zero out the elapsed time for the next frame
        gZeroNextFrameTime = true;*/

        throw new UnsupportedOperationException();
    }


    void DestroyScene()
    {
        /*gMeshOpaque.Destroy();
        gMeshAlpha.Destroy();
        SAFE_RELEASE(gSkyboxSRV);*/
    }
}
