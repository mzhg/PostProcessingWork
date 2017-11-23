package jet.opengl.demos.intel.assao;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2i;
import org.lwjgl.util.vector.Vector4f;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.demos.intel.va.VaASSAO;
import jet.opengl.demos.intel.va.VaAssetPack;
import jet.opengl.demos.intel.va.VaCameraBase;
import jet.opengl.demos.intel.va.VaCameraControllerFocusLocationsFlythrough;
import jet.opengl.demos.intel.va.VaCameraControllerFreeFlight;
import jet.opengl.demos.intel.va.VaGBuffer;
import jet.opengl.demos.intel.va.VaLighting;
import jet.opengl.demos.intel.va.VaPostProcess;
import jet.opengl.demos.intel.va.VaPostProcessTonemap;
import jet.opengl.demos.intel.va.VaRenderMesh;
import jet.opengl.demos.intel.va.VaRenderMeshDrawList;
import jet.opengl.demos.intel.va.VaRenderingGlobals;
import jet.opengl.demos.intel.va.VaRenderingModuleImpl;
import jet.opengl.demos.intel.va.VaShaderDefine;
import jet.opengl.demos.intel.va.VaSimpleShadowMap;
import jet.opengl.demos.intel.va.VaSky;
import jet.opengl.demos.intel.va.VaTexture;

/**
 * Created by mazhen'gui on 2017/11/22.
 */

public final class ASSAODemo extends VaRenderingModuleImpl {
    public static final int
            Sponza = 0,
            SponzaAndDragons = 1,
            Sibenik = 2,
            SibenikAndDragons = 3,
            LostEmpire = 4,

            MaxCount = 5;

    private VaCameraBase    m_camera;
    private VaCameraControllerFreeFlight m_cameraFreeFlightController;
    private VaCameraControllerFocusLocationsFlythrough m_flythroughCameraControllerSponza;
    private VaCameraControllerFocusLocationsFlythrough m_flythroughCameraControllerSibenik;
    private VaCameraControllerFocusLocationsFlythrough m_flythroughCameraControllerLostEmpire;

//    shared_ptr<vaRenderDevice>              m_renderDevice;
//    shared_ptr<vaApplication>               m_application;

    private VaSky m_sky;
    private VaRenderingGlobals m_renderingGlobals;

    private VaSimpleShadowMap m_simpleShadowMap;


    private final VaRenderMeshDrawList m_meshDrawList = new VaRenderMeshDrawList();

    private VaGBuffer                   m_GBuffer;
    private VaLighting                  m_lighting;
    private VaPostProcess               m_postProcess;
    private VaPostProcessTonemap        m_postProcessTonemap;
    private List<VaRenderMesh>          m_sceneMeshes;
    private List<Matrix4f>              m_sceneMeshesTransforms;

    private VaASSAO                     m_SSAOEffect_DevelopmentVersion;
    private ASSAOWrapper            	m_SSAOEffect;
    private ExternalSSAOWrapper         m_SSAOEffect_External;

    private int                         m_loadedSceneChoice;

    private float[]                     m_shaderDebugData = new float[VaShaderDefine.SHADERGLOBAL_DEBUG_FLOAT_OUTPUT_COUNT ];

    private VaAssetPack                 m_assetsDragon;
    private VaAssetPack                 m_assetsSibenik;
    private VaAssetPack                 m_assetsSponza;
    private VaAssetPack                 m_assetsLostEmpire;


    private boolean                     m_triggerCompareDevNonDev;
    private VaTexture                   m_comparerReferenceTexture;
    private VaTexture                   m_comparerCurrentTexture;

    private final List<Vector4f>        m_displaySampleDisk = new ArrayList<>();


    private boolean                     m_flythroughCameraEnabled;

    private int                         m_expandedSceneBorder;
    private final Vector2i              m_expandedSceneResolution = new Vector2i();

    private int                         m_frameIndex;

    private String                      m_screenshotCapturePath;


    public static final class SSAODemoSettings
    {
        int                      SceneChoice;
        boolean                  UseDeferred;
        boolean                  ShowWireframe;
        boolean                  EnableSSAO;
        int                      SSAOSelectedVersionIndex;
        boolean                  DebugShowOpaqueSSAO;
        boolean                  DisableTexturing;
        boolean                  ExpandDrawResolution;       // to handle SSAO artifacts around screen borders
        float                    CameraYFov;
        boolean                  UseSimpleUI;

        SSAODemoSettings( )
        {
            SceneChoice                 = SponzaAndDragons;
            ShowWireframe               = false;
            EnableSSAO                  = true;
            SSAOSelectedVersionIndex    = 1;
            UseDeferred                 = true;
            DebugShowOpaqueSSAO         = false;
            DisableTexturing            = false;
            ExpandDrawResolution        = false;
            CameraYFov                  = 90.0f /*/ 360.0f * VA_PIf*/;
            UseSimpleUI                  = true;
        }

    };

    public static final class GPUProfilingResults
    {
        float                                   TotalTime;
        float                                   RegularShadowmapCreate;
        float                                   SceneOpaque;
        float                                   NonShadowedTransparencies;
        float                                   ShadowedTransparencies;
        float                                   MiscDebugCanvas;
        float                                   MiscImgui;
    };
}
