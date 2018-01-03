package jet.opengl.demos.intel.assao;

import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector2i;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Vector4i;
import org.lwjgl.util.vector.Writable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import jet.opengl.demos.intel.va.VaASSAO;
import jet.opengl.demos.intel.va.VaAsset;
import jet.opengl.demos.intel.va.VaAssetPack;
import jet.opengl.demos.intel.va.VaBoundingSphere;
import jet.opengl.demos.intel.va.VaCameraBase;
import jet.opengl.demos.intel.va.VaCameraControllerBase;
import jet.opengl.demos.intel.va.VaCameraControllerFocusLocationsFlythrough;
import jet.opengl.demos.intel.va.VaCameraControllerFreeFlight;
import jet.opengl.demos.intel.va.VaCore;
import jet.opengl.demos.intel.va.VaDrawContext;
import jet.opengl.demos.intel.va.VaFileStream;
import jet.opengl.demos.intel.va.VaGBuffer;
import jet.opengl.demos.intel.va.VaLighting;
import jet.opengl.demos.intel.va.VaOrientedBoundingBox;
import jet.opengl.demos.intel.va.VaPostProcess;
import jet.opengl.demos.intel.va.VaRenderDeviceContext;
import jet.opengl.demos.intel.va.VaRenderMaterialManager;
import jet.opengl.demos.intel.va.VaRenderMesh;
import jet.opengl.demos.intel.va.VaRenderMeshDrawList;
import jet.opengl.demos.intel.va.VaRenderMeshManager;
import jet.opengl.demos.intel.va.VaRenderPassType;
import jet.opengl.demos.intel.va.VaRenderingGlobals;
import jet.opengl.demos.intel.va.VaRenderingModule;
import jet.opengl.demos.intel.va.VaRenderingModuleRegistrar;
import jet.opengl.demos.intel.va.VaShaderDefine;
import jet.opengl.demos.intel.va.VaSimpleShadowMap;
import jet.opengl.demos.intel.va.VaSky;
import jet.opengl.demos.intel.va.VaTexture;
import jet.opengl.demos.intel.va.VaViewport;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/11/22.
 */

abstract class ASSAODemo extends NvSampleApp implements VaRenderingModule {
    public static final int
            Sponza = 0,
            SponzaAndDragons = 1,
            Sibenik = 2,
            SibenikAndDragons = 3,
            LostEmpire = 4,

            MaxCount = 5;

    private String m_renderingModuleTypeName;

    Macro[] m_staticShaderMacros = null;
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

    private List<Vector4f>              m_displaySampleDisk;


    private boolean                     m_flythroughCameraEnabled;

    private int                         m_expandedSceneBorder;
    private final Vector2i              m_expandedSceneResolution = new Vector2i();

    private int                         m_frameIndex;

    private String                      m_screenshotCapturePath;

    protected final SSAODemoSettings    m_settings = new SSAODemoSettings();
    private GLFuncProvider              gl;

    public ASSAODemo(){
        m_camera = new VaCameraBase();

        m_camera.SetPosition(4.3f, 29.2f, 14.2f);
        m_camera.SetOrientationLookAt( new Vector3f(6.5f, 0.0f, 8.7f), Vector3f.Y_AXIS);

        m_camera.SetNearPlane( 0.1f );
        m_camera.SetFarPlane( 10000.0f );
        m_camera.SetYFOV( 90.0f / 360.0f * Numeric.PI);

        m_cameraFreeFlightController    = new VaCameraControllerFreeFlight();
        m_cameraFreeFlightController.SetMoveWhileNotCaptured( false );

        m_renderingGlobals  =  VA_RENDERING_MODULE_CREATE_UNIQUE( "vaRenderingGlobals" );

        m_sky               =  VA_RENDERING_MODULE_CREATE_UNIQUE( "vaSky" );
        m_sky.GetSettings().FogDistanceMin = 512.0f;

        m_GBuffer               = VA_RENDERING_MODULE_CREATE_UNIQUE( "vaGBuffer" );
        m_lighting              = VA_RENDERING_MODULE_CREATE_UNIQUE( "vaLighting" );
        m_postProcess           = VA_RENDERING_MODULE_CREATE_UNIQUE( "vaPostProcess" );

        m_SSAOEffect_DevelopmentVersion = VA_RENDERING_MODULE_CREATE_UNIQUE( "vaASSAO" );
        m_SSAOEffect                    = VA_RENDERING_MODULE_CREATE_UNIQUE( "ASSAOWrapper" );
        m_SSAOEffect_External           = VA_RENDERING_MODULE_CREATE_UNIQUE( "ExternalSSAOWrapper" );

        m_loadedSceneChoice = /**/MaxCount;

        m_assetsDragon      = new VaAssetPack("Dragon");
        m_assetsSibenik     = new VaAssetPack("Sibenik");
        m_assetsSponza      = new VaAssetPack("Sponza");
        m_assetsLostEmpire  = new VaAssetPack("LostEmpire");

        {
            try(VaFileStream fileIn = new VaFileStream()){
                if( fileIn.Open( /*vaCore::GetExecutableDirectory( )*/ "camerapos.data", VaFileStream.FileCreationMode.Open, VaFileStream.FileAccessMode.Default ) )
                {
                    m_camera.Load( fileIn );
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        m_camera.AttachController( m_cameraFreeFlightController );

        {
            try(VaFileStream fileIn = new VaFileStream()){
                if( fileIn.Open( /*vaCore::GetExecutableDirectory( ) +*/ "SSAODemoSettings.data", VaFileStream.FileCreationMode.Open, VaFileStream.FileAccessMode.Default ) )
                {
                    /*int32 size;
                    if( fileIn.ReadValue<int32>(size) && (size == (int32)sizeof( m_settings )) )
                    {
                        fileIn.ReadValue( m_settings );
                    }*/

                    int size = fileIn.ReadInt();
                    if(size == SSAODemoSettings.SIZE){
                        fileIn.ReadObject(size, m_settings);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        m_triggerCompareDevNonDev = false;

        m_flythroughCameraEnabled = false;

        m_screenshotCapturePath = "";
    }

    @Override
    public void onDestroy() {
        SaveCamera(-1 );

        {
            try(VaFileStream fileOut = new VaFileStream()){
                if( fileOut.Open( /*vaCore::GetExecutableDirectory( ) +*/ "SSAODemoSettings.data", VaFileStream.FileCreationMode.Create, VaFileStream.FileAccessMode.Default ) )
                {
                    /*fileOut.WriteValue<int32>( (int32)sizeof(m_settings) );
                    fileOut.WriteValue( m_settings );*/

                    fileOut.WriteInt(SSAODemoSettings.SIZE);
                    fileOut.WriteObject(m_settings.SIZE, m_settings);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // not needed but useful for debugging so I left it in
        m_meshDrawList.Reset();
        m_sceneMeshes.clear();
        m_sceneMeshesTransforms.clear();
    }

    public void Initialize( /*const std::shared_ptr<vaRenderDevice> & renderDevice, const std::shared_ptr<vaApplication> & application*/ ){

    }

    public VaCameraBase   GetCamera( )          { return m_camera; }
    public VaSky    GetSky( )                   { return m_sky; }

    public void OnStarted( ){
        SetupScene( );

        // load required assets
        EnsureLoaded( m_assetsDragon );
    }

    public void OnStopped( ){}
    public void OnBeforeStopped( ){}

    public void OnResized( int width, int height, boolean windowed ){    m_camera.SetViewportSize( width, height );}

    public void OnTick( float deltaTime ){
        if( deltaTime > 0.2f )
            deltaTime = 0.2f;

        {
            VaCameraControllerBase wantedCameraController = GetCameraController();
            if( m_camera.GetAttachedController( ) != wantedCameraController )
                m_camera.AttachController( wantedCameraController );
        }

        {
            final float minValidDelta = 0.0005f;
            if( deltaTime < minValidDelta )
            {
//                m_renderDevice.GetCanvas2D( ).DrawString( 300, 40, 0xFFFF2020, 0xFF000000, "WARNING, delta time too small, clamping" );  TODO
            }
            deltaTime = Math.max( deltaTime, minValidDelta ); // not correct above 1000 fps!
        }

        if( m_loadedSceneChoice != m_settings.SceneChoice )
        {
            m_loadedSceneChoice = m_settings.SceneChoice;

            m_sceneMeshes.clear();
            m_sceneMeshesTransforms.clear();

            final Matrix4f translation = CacheBuffer.getCachedMatrix();
            translation.setTranslate(0.0f, 0.0f, -0.1f);
            m_sceneMeshes.add( VaRenderMesh.CreatePlane( translation, 1000.0f, 1000.0f ) );
            m_sceneMeshesTransforms.add( Matrix4f.IDENTITY );

            switch( m_loadedSceneChoice )
            {
                case Sponza:
                {
                    EnsureLoaded( m_assetsSponza );
                    InsertAllToSceneMeshesList( m_assetsSponza, Matrix4f.IDENTITY );
                }
                break;

                case SponzaAndDragons:
                {
                    EnsureLoaded( m_assetsSponza );
                    InsertAllToSceneMeshesList( m_assetsSponza, Matrix4f.IDENTITY );

                    translation.setTranslate(-4.0f, 4.0f, 1.0f + 0.35f);
                    m_sceneMeshes.add( VaRenderMesh.CreateTetrahedron(translation, false ) );  m_sceneMeshesTransforms.add( Matrix4f.IDENTITY );
                    //m_sceneMeshes.push_back( vaRenderMesh::CreateCube(           vaMatrix4x4::Translation( -4.0f, 4.0f, 1.0f + 0.7f ), false ) );                                       m_sceneMeshesTransforms.push_back( Matrix4f.IDENTITY );
                    //m_sceneMeshes.push_back( vaRenderMesh::CreateOctahedron(     vaMatrix4x4::Translation( -2.0f, 4.0f, 1.0f + 1.0f ), false ) );                                       m_sceneMeshesTransforms.push_back( Matrix4f.IDENTITY );
                    //m_sceneMeshes.push_back( vaRenderMesh::CreateIcosahedron(    vaMatrix4x4::Translation(  0.0f, 4.0f, 1.0f + 0.85f ), false ) );                                      m_sceneMeshesTransforms.push_back( Matrix4f.IDENTITY );
                    //m_sceneMeshes.push_back( vaRenderMesh::CreateDodecahedron(   vaMatrix4x4::Translation(  2.0f, 4.0f, 1.0f + 0.94f ), false ) );                                      m_sceneMeshesTransforms.push_back( Matrix4f.IDENTITY );
                    //m_sceneMeshes.push_back( vaRenderMesh::CreateSphere(         vaMatrix4x4::Translation(  4.0f, 4.0f, 1.0f + 1.0f ), 1, false ) );                                    m_sceneMeshesTransforms.push_back( Matrix4f.IDENTITY );
                    //m_sceneMeshes.push_back( vaRenderMesh::CreateCylinder(       vaMatrix4x4::Translation(  6.0f, 4.0f, 1.0f + 1.02f ), 2.0f, 0.5f, 0.4f, 10, false, false ) );         m_sceneMeshesTransforms.push_back( Matrix4f.IDENTITY );
//                    m_sceneMeshes.add( VaRenderMesh.CreateTeapot(         vaMatrix4x4::Scaling( 0.5f, 0.5f, 0.5f ) * vaMatrix4x4::Translation(  4.0f, 4.0f, 1.0f + 0.5f ) ) );  m_sceneMeshesTransforms.push_back( Matrix4f.IDENTITY );

                    //InsertAllToSceneMeshesList( *m_assetsDragon, vaMatrix4x4::Scaling( 3.0f, 3.0f, 3.0f ) *  vaMatrix4x4::RotationZ( VA_PIf * 1.0f ) * vaMatrix4x4::Translation( -5.0f,  -4.7f, 1.8f ) );
                    //InsertAllToSceneMeshesList( *m_assetsDragon, vaMatrix4x4::Scaling( 2.5f, 2.5f, 2.5f ) *  vaMatrix4x4::RotationZ( VA_PIf * 1.0f ) * vaMatrix4x4::Translation( -4.0f,  -4.5f, 1.65f ) );
                    //InsertAllToSceneMeshesList( *m_assetsDragon, vaMatrix4x4::Scaling( 2.0f, 2.0f, 2.0f ) *  vaMatrix4x4::RotationZ( VA_PIf * 1.0f ) * vaMatrix4x4::Translation( -2.0f,  -4.5f, 1.5f ) );
                    //InsertAllToSceneMeshesList( *m_assetsDragon, vaMatrix4x4::Scaling( 1.5f, 1.5f, 1.5f ) *  vaMatrix4x4::RotationZ( VA_PIf * 1.0f ) * vaMatrix4x4::Translation( -0.0f,  -4.5f, 1.38f ) );

                    Matrix4f meshTransform = new Matrix4f();
                    meshTransform.setTranslate(-4.0f,  -5.3f, 1.38f);
                    meshTransform.rotate(Numeric.PI, Vector3f.Z_AXIS);
                    meshTransform.scale(1.5f);
                    InsertAllToSceneMeshesList(m_assetsDragon, meshTransform /*vaMatrix4x4::Scaling( 1.5f, 1.5f, 1.5f ) *  vaMatrix4x4::RotationZ( VA_PIf * 1.0f ) * vaMatrix4x4::Translation(  -4.0f,  -5.3f, 1.38f )*/ );
                    //InsertAllToSceneMeshesList( *m_assetsDragon, vaMatrix4x4::Scaling( 0.7f, 0.7f, 0.7f ) *  vaMatrix4x4::RotationZ( VA_PIf * 1.0f ) * vaMatrix4x4::Translation(  4.0f,  -4.5f, 1.19f ) );

                    meshTransform = new Matrix4f();
                    meshTransform.setTranslate(4.0f,  -5.3f, 1.11f);
                    meshTransform.rotate(Numeric.PI, Vector3f.Z_AXIS);
                    meshTransform.scale(0.5f);
                    InsertAllToSceneMeshesList(m_assetsDragon, meshTransform/*vaMatrix4x4::Scaling( 0.5f, 0.5f, 0.5f ) *  vaMatrix4x4::RotationZ( VA_PIf * 1.0f ) * vaMatrix4x4::Translation(  4.0f,  -5.3f, 1.11f )*/ );
                }
                break;

                case Sibenik:
                {
                    EnsureLoaded( m_assetsSibenik );
                    InsertAllToSceneMeshesList( m_assetsSibenik, Matrix4f.IDENTITY );
                } break;

                case SibenikAndDragons:
                {
                    EnsureLoaded( m_assetsSibenik );
                    InsertAllToSceneMeshesList( m_assetsSibenik, Matrix4f.IDENTITY );

                    Matrix4f meshTransform = new Matrix4f();
                    meshTransform.setTranslate(-14.0f,  0.0f, 1.2f);
                    meshTransform.rotate(Numeric.PI * 0.5f, Vector3f.Z_AXIS);
                    meshTransform.scale(4.0f);
                    InsertAllToSceneMeshesList( m_assetsDragon, meshTransform/*vaMatrix4x4::Scaling( 4.0f, 4.0f, 4.0f ) *  vaMatrix4x4::RotationZ( VA_PIf * 0.5f ) * vaMatrix4x4::Translation(  -14.0f,  0.0f, 1.2f )*/ );

                    meshTransform = new Matrix4f();
                    meshTransform.setTranslate(-17.6f, -5.4f, 1.08f);
                    meshTransform.rotate(Numeric.PI, Vector3f.Z_AXIS);
                    meshTransform.scale(3.0f);
                    InsertAllToSceneMeshesList( m_assetsDragon, meshTransform /*vaMatrix4x4::Scaling( 3.0f, 3.0f, 3.0f ) *  vaMatrix4x4::RotationZ( VA_PIf * 1.0f ) * vaMatrix4x4::Translation(  -17.6f, -5.4f, 1.08f )*/ );
                    //InsertAllToSceneMeshesList( *m_assetsDragon, vaMatrix4x4::Scaling( 3.0f, 3.0f, 3.0f ) *  vaMatrix4x4::RotationZ( -VA_PIf * 0.0f ) * vaMatrix4x4::Translation( -17.6f,  5.0f, 1.08f ) );
                    //InsertAllToSceneMeshesList( *m_assetsDragon, vaMatrix4x4::Scaling( 2.0f, 2.0f, 2.0f ) *  vaMatrix4x4::RotationZ( VA_PIf * 1.0f ) * vaMatrix4x4::Translation(  -14.0f, -4.5f, 0.75f ) );

                    meshTransform = new Matrix4f();
                    meshTransform.setTranslate(-14.0f,  5.5f, 0.75f);
                    meshTransform.rotate(-Numeric.PI, Vector3f.Z_AXIS);
                    meshTransform.scale(2.0f);
                    InsertAllToSceneMeshesList(m_assetsDragon, meshTransform /*vaMatrix4x4::Scaling( 2.0f, 2.0f, 2.0f ) *  vaMatrix4x4::RotationZ( -VA_PIf * 0.0f ) * vaMatrix4x4::Translation( -14.0f,  5.5f, 0.75f )*/ );
                } break;

                case LostEmpire:
                {
                    EnsureLoaded( m_assetsLostEmpire );
                    InsertAllToSceneMeshesList( m_assetsLostEmpire, Matrix4f.IDENTITY );
                } break;

                default:
                    break;
            }

            CacheBuffer.free(translation);
        }

        m_renderingGlobals.Tick( deltaTime );

        m_sky.Tick( deltaTime, m_lighting );
        m_camera.Tick( deltaTime, /*m_application->HasFocus( )*/ true);

        // calculate shadow map
        if( m_simpleShadowMap != null )
        {
            final float sceneRadius = 50.0f;
            VaBoundingSphere shadowMapArea = new VaBoundingSphere( new Vector3f( 0.0f, 0.0f, 10.0f ), sceneRadius );
            Vector3f lightDir = new Vector3f(m_sky.GetSunDir( )); lightDir.scale(-1);
            ReadableVector3f upRefDir = Vector3f.Z_AXIS; // vaVector3( 0.0f, 0.0f, 1.0f );
            if( Math.abs( Vector3f.dot( lightDir, upRefDir ) ) > 0.95f )
                upRefDir = Vector3f.Y_AXIS; // vaVector3( 0.0f, 1.0f, 0.0f );
            if( Math.abs( Vector3f.dot( lightDir, upRefDir ) ) > 0.95f )
                upRefDir = Vector3f.X_AXIS; // vaVector3( 1.0f, 0.0f, 0.0f );

            Vector3f lightAxisZ = lightDir;
            Vector3f lightAxisX = Vector3f.cross( upRefDir, lightDir, null  );  lightAxisX.normalise();
            Vector3f lightAxisY = Vector3f.cross( lightDir, lightAxisX, null ); lightAxisY.normalise();

            Matrix3f shadowAxis = new Matrix3f(
                    lightAxisX.x, lightAxisX.y, lightAxisX.z,
                    lightAxisY.x, lightAxisY.y, lightAxisY.z,
                    lightAxisZ.x, lightAxisZ.y, lightAxisZ.z );

            VaOrientedBoundingBox obb = new VaOrientedBoundingBox( shadowMapArea.Center,
                    new Vector3f( shadowMapArea.Radius, shadowMapArea.Radius, shadowMapArea.Radius * 2 ), shadowAxis );

            m_simpleShadowMap.UpdateArea( obb );

            if( m_settings.ShowWireframe )
            {
                /*vaDebugCanvas3DBase * canvas3D = GetCanvas3D( );  TODO
                canvas3D->DrawAxis( shadowMapArea.Center, shadowMapArea.Radius, &vaMatrix4x4( shadowAxis ) );
                canvas3D->DrawBox( obb, 0xFF00FF00, 0x10FFFFFF );*/
            }
        }

        // old data still queued? that's a bug!
        assert( m_meshDrawList.Count() == 0 );

        for( int i = 0; i < m_sceneMeshes.size( ); i++ )
        {
            Matrix4f transform = (m_sceneMeshesTransforms.size()>i)?(m_sceneMeshesTransforms.get(i)):(Matrix4f.IDENTITY);
            m_meshDrawList.Insert( m_sceneMeshes.get(i), transform ) ; //vaMatrix4x4::Translation( 0.0f, ( i - m_sceneMeshes.size( ) / 2.0f ) * 2.0f, ( i == 0 ) ? ( 0.0f ) : ( 1.0f ) ), 0, vaVector4( 0.0f, 0.0f, 0.0f, 0.0f ) );
        }

        /*if( m_application->HasFocus( ) && !vaInputMouseBase::GetCurrent( )->IsCaptured( ) )  TODO
        {
            static float notificationStopTimeout = 0.0f;
            notificationStopTimeout += deltaTime;

            vaInputKeyboardBase & keyboard = *vaInputKeyboardBase::GetCurrent( );
            if( keyboard.IsKeyDown( vaKeyboardKeys::KK_LEFT ) || keyboard.IsKeyDown( vaKeyboardKeys::KK_RIGHT ) || keyboard.IsKeyDown( vaKeyboardKeys::KK_UP ) || keyboard.IsKeyDown( vaKeyboardKeys::KK_DOWN ) ||
                    keyboard.IsKeyDown( ( vaKeyboardKeys )'W' ) || keyboard.IsKeyDown( ( vaKeyboardKeys )'S' ) || keyboard.IsKeyDown( ( vaKeyboardKeys )'A' ) ||
                    keyboard.IsKeyDown( ( vaKeyboardKeys )'D' ) || keyboard.IsKeyDown( ( vaKeyboardKeys )'Q' ) || keyboard.IsKeyDown( ( vaKeyboardKeys )'E' ) )
            {
                if( notificationStopTimeout > 3.0f )
                {
                    notificationStopTimeout = 0.0f;
                    vaLog::GetInstance().Add( vaVector4( 1.0f, 0.0f, 0.0f, 1.0f ), L"To switch into free flight (move&rotate) mode, use mouse right click." );
                }
            }
        }*/

        if( m_settings.UseSimpleUI )
        {
            m_settings.CameraYFov               = 90.0f / 360.0f * Numeric.PI;
            m_settings.SSAOSelectedVersionIndex = 1;
        }
    }

    public void OnRender( ){
        m_frameIndex++;
        VaRenderDeviceContext mainContext = null;//m_renderDevice.GetMainContext( );

        VaViewport mainViewportBackup   = new VaViewport(0,0, getGLContext().width(),  getGLContext().height()); // m_renderDevice->GetMainContext( )->GetViewport();
        VaViewport mainViewportExpanded = new VaViewport(0,0, getGLContext().width(),  getGLContext().height()); // m_renderDevice->GetMainContext( )->GetViewport();
        VaViewport mainViewport         = new VaViewport(0,0, getGLContext().width(),  getGLContext().height()); // m_renderDevice->GetMainContext( )->GetViewport();

        // current textures for sibenik are not good / complete
        VaRenderMaterialManager.GetInstance().SetTexturingDisabled( m_settings.DisableTexturing );
        if( m_settings.SceneChoice == SibenikAndDragons )
        {
            VaRenderMaterialManager.GetInstance().SetTexturingDisabled( true );
        }

        Vector4i scissorRectForSSAO = new Vector4i( 0, 0, 0, 0 );

        // update resolution and camera FOV if there's border expansion
        final int drawResolutionBorderExpansionFactor = 12; // will be expanded by Height / expansionFactor
        {
            if( m_settings.ExpandDrawResolution )
            {
                m_expandedSceneBorder = (Math.min( mainViewport.Width, mainViewport.Height ) / drawResolutionBorderExpansionFactor) / 2 * 2;
            }
            else
            {
                m_expandedSceneBorder = 0;
            }

            m_expandedSceneResolution.x = mainViewport.Width + m_expandedSceneBorder * 2;
            m_expandedSceneResolution.y = mainViewport.Height + m_expandedSceneBorder * 2;

            double yScaleDueToBorder = (m_expandedSceneResolution.y * 0.5) / (double)(mainViewport.Height * 0.5);

            double nonExpandedTan = Math.tan( m_settings.CameraYFov / 2.0 );
            float expandedFOV = (float)(Math.atan( nonExpandedTan * yScaleDueToBorder ) * 2.0);

            m_camera.SetYFOV( expandedFOV );
            m_camera.SetViewportSize( m_expandedSceneResolution.x, m_expandedSceneResolution.y );
            m_camera.Tick( 0.0f, false );  // re-tick for expanded focus

            mainViewport.Width  = m_expandedSceneResolution.x;
            mainViewport.Height = m_expandedSceneResolution.y;

            scissorRectForSSAO.x   = m_expandedSceneBorder;
            scissorRectForSSAO.y   = m_expandedSceneBorder;
            scissorRectForSSAO.z   = mainViewport.Width  - m_expandedSceneBorder;
            scissorRectForSSAO.w   = mainViewport.Height - m_expandedSceneBorder;

            mainViewportExpanded = mainViewport;

            UpdateTextures( mainViewport.Width, mainViewport.Height );
        }

        // update GBuffer resources if needed
        {
            VaDrawContext drawContext = new VaDrawContext(m_camera, mainContext, m_renderingGlobals, m_lighting );
            m_GBuffer.UpdateResources( drawContext, m_expandedSceneResolution.x, m_expandedSceneResolution.y );
        }

        // decide on the main render target / depth
        VaTexture mainColorRT   = m_GBuffer.GetOutputColor();  // m_renderDevice->GetMainChainColor();
        VaTexture mainDepthRT   = m_GBuffer.GetDepthBuffer();  // m_renderDevice->GetMainChainDepth();

        // clear the main render target / depth
        /*mainColorRT->ClearRTV( mainContext, vaVector4( 0.0f, 0.0f, 0.0f, 0.0f ) );
        mainDepthRT->ClearDSV( mainContext, true, m_camera->GetUseReversedZ()?(0.0f):(1.0f), false, 0 );*/
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f );
        gl.glClearDepthf(1.0f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);
        // set main render target / depth
//        mainContext.SetRenderTarget( mainColorRT, mainDepthRT, true );

//        vaMatrix4x4 viewProj = m_camera->GetViewMatrix( ) * m_camera->GetProjMatrix( );
        Matrix4f viewProj = null;  // TODO

        if( m_settings.UseDeferred )
        {
//            VA_SCOPE_CPUGPU_TIMER( Deferred, mainContext );
            VaDrawContext drawContext = new VaDrawContext(m_camera, mainContext, m_renderingGlobals, m_lighting );

            // this sets up global constants
            m_renderingGlobals.SetAPIGlobals( drawContext );

            // clear light accumulation (radiance) RT
            m_GBuffer.GetRadiance().ClearRTV( /*mainContext, vaVector4( 0.0f, 0.0f, 0.0f, 0.0f )*/ (Vector4f) Vector4f.ZERO);

            // Draw deferred elements into the GBuffer
            {
//                VA_SCOPE_CPUGPU_TIMER( GBufferDraw, mainContext );
                drawContext.PassType = VaRenderPassType.Deferred;

                // GBuffer textures
                VaTexture renderTargets[] = {m_GBuffer.GetAlbedo(), m_GBuffer.GetNormalMap() };

                // clear GBuffer
                for( int i = 0; i < renderTargets.length; i++ )
                    renderTargets[i].ClearRTV( /*mainContext, vaVector4( 0.0f, 0.0f, 0.0f, 0.0f )*/ (Vector4f) Vector4f.ZERO );

                mainContext.SetRenderTargets(renderTargets.length, renderTargets, mainDepthRT, true );
                VaRenderMeshManager. GetInstance( ).Draw( drawContext, m_meshDrawList );
            }

            // GBuffer processing
            {
//                VA_SCOPE_CPUGPU_TIMER( GBufferProcess, mainContext );
                drawContext = new VaDrawContext(m_camera, mainContext, m_renderingGlobals, m_lighting);

                // this sets up global constants
                m_renderingGlobals.SetAPIGlobals( drawContext );

                // set destination render target and no depth
                mainContext.SetRenderTarget( m_GBuffer.GetDepthBufferViewspaceLinear( ), null, true );
                m_GBuffer.DepthToViewspaceLinear( drawContext, mainDepthRT );
            }

            // Apply lighting
            {
//                VA_SCOPE_CPUGPU_TIMER( Lighting, mainContext );
                drawContext.PassType = VaRenderPassType.Unknown;

                if( m_simpleShadowMap != null )
                {
                    m_simpleShadowMap.StartUsing( drawContext );

                    if( m_simpleShadowMap.GetVolumeShadowMapPlugin( ) != null )
                        m_simpleShadowMap.GetVolumeShadowMapPlugin( ).StartUsing( drawContext, m_simpleShadowMap );
                }

                // this sets up global constants
                m_renderingGlobals.SetAPIGlobals( drawContext );

                // set destination render target and no depth
                mainContext.SetRenderTarget( m_GBuffer.GetRadiance( ), null, true );

                m_lighting.ApplyDirectionalAmbientLighting( drawContext, m_GBuffer );

                if( m_simpleShadowMap != null )
                {
                    if( m_simpleShadowMap.GetVolumeShadowMapPlugin( ) != null )
                        m_simpleShadowMap.GetVolumeShadowMapPlugin( ).StopUsing( drawContext, m_simpleShadowMap );
                    m_simpleShadowMap.StopUsing( drawContext );
                }
            }

            // restore main render target / depth
            mainContext.SetRenderTarget( mainColorRT, mainDepthRT, true );

            // Tonemap to final color
            {
//                VA_SCOPE_CPUGPU_TIMER( Lighting, mainContext );
                /*drawContext.PassType = VaRenderPassType.Unknown;
                m_renderingGlobals.SetAPIGlobals( drawContext );
                m_postProcessTonemap.Tonemap( drawContext, m_GBuffer.GetRadiance() );*/
            }
        }

        // Forward draw
        {
//            VA_SCOPE_CPUGPU_TIMER( ForwardDraw, mainContext );
            VaDrawContext drawContext = new VaDrawContext( m_camera, mainContext, m_renderingGlobals, m_lighting);

            if( m_simpleShadowMap != null )
            {
                m_simpleShadowMap.StartUsing( drawContext );

                if( m_simpleShadowMap.GetVolumeShadowMapPlugin() != null )
                    m_simpleShadowMap.GetVolumeShadowMapPlugin().StartUsing( drawContext, m_simpleShadowMap);
            }

            // Draw opaque stuff
            {
//                VA_SCOPE_CPUGPU_TIMER( Opaque, mainContext );

                //vaDrawContext drawContext( *m_camera.get( ), mainContext );
                drawContext.PassType          = VaRenderPassType.ForwardOpaque;

                // this sets up global constants
                m_renderingGlobals.SetAPIGlobals( drawContext );

                m_sky.Draw( drawContext );

                if( !m_settings.UseDeferred )
                    VaRenderMeshManager.GetInstance().Draw( drawContext, m_meshDrawList );
            }

            // Apply SSAO
            if( m_settings.EnableSSAO )
            {
                if( m_triggerCompareDevNonDev )
                {
                    m_triggerCompareDevNonDev = false;

                    m_comparerReferenceTexture.ClearRTV( /*mainContext,*/ new Vector4f( 1.0f, 1.0f, 1.0f, 1.0f ) );
                    mainContext.SetRenderTarget( m_comparerReferenceTexture, null, true );

                    if( m_settings.UseDeferred )
                        m_SSAOEffect_DevelopmentVersion.Draw( drawContext, drawContext.Camera.GetProjMatrix(), mainDepthRT, !m_settings.DebugShowOpaqueSSAO, m_GBuffer.GetNormalMap( ), scissorRectForSSAO );
                    else
                        m_SSAOEffect_DevelopmentVersion.Draw( drawContext, drawContext.Camera.GetProjMatrix(), mainDepthRT, !m_settings.DebugShowOpaqueSSAO, null, scissorRectForSSAO );

                    m_comparerCurrentTexture.ClearRTV( /*mainContext,*/ new Vector4f( 1.0f, 1.0f, 1.0f, 1.0f ) );
                    mainContext.SetRenderTarget( m_comparerCurrentTexture, null, true );

                    if( m_settings.UseDeferred )
                        m_SSAOEffect.Draw( drawContext, mainDepthRT, !m_settings.DebugShowOpaqueSSAO, m_GBuffer.GetNormalMap( ), scissorRectForSSAO );
                    else
                        m_SSAOEffect.Draw( drawContext, mainDepthRT, !m_settings.DebugShowOpaqueSSAO, null, scissorRectForSSAO );

                    Vector4f difference = m_postProcess.CompareImages( drawContext, m_comparerReferenceTexture, m_comparerCurrentTexture );

                    /*vaLog::GetInstance().Add( vaVector4( 1.0f, 0.0f, 0.0f, 1.0f ), "" );  TODO
                    vaLog::GetInstance().Add( vaVector4( 1.0f, 1.0f, 1.0f, 1.0f ), "IMGCOMPARE RESULTS:" );
                    if( difference.x == 0 )
                        vaLog::GetInstance().Add( vaVector4( 0.6f, 1.0f, 0.6f, 1.0f ), "    NO difference (all pixels are identical).", difference.x );
                    else
                        vaLog::GetInstance().Add( vaVector4( 1.0f, 0.6f, 0.6f, 1.0f ), "    MSE: %.7f, PSNR: %.2f", difference.x, difference.y );
                    vaLog::GetInstance().Add( vaVector4( 1.0f, 0.0f, 0.0f, 1.0f ), "" );*/

                    // restore main render target / depth
                    mainContext.SetRenderTarget( mainColorRT, mainDepthRT, true );
                }

                boolean skipSSAO = false;
                if( !skipSSAO )
                {
                    if( m_settings.SSAOSelectedVersionIndex == 0 )
                    {
//                        VA_SCOPE_CPUGPU_TIMER_DEFAULTSELECTED( ASSAO_DevVersion, mainContext );

                        // set destination render target and no depth
                        /*mainContext.SetRenderTarget( mainColorRT, null, true );

                        vaVector2i debugCursorPos = vaInputMouse::GetInstance( ).GetCursorClientPos() + vaVector2i( m_expandedSceneBorder, m_expandedSceneBorder );
                        m_SSAOEffect_DevelopmentVersion.SetDebugShowSamplesAtCursorPos( debugCursorPos );

                        if( m_settings.UseDeferred )
                            m_SSAOEffect_DevelopmentVersion->Draw( drawContext, drawContext.Camera.GetProjMatrix(), *mainDepthRT.get( ), !m_settings.DebugShowOpaqueSSAO, m_GBuffer->GetNormalMap( ).get( ), scissorRectForSSAO );
                        else
                            m_SSAOEffect_DevelopmentVersion->Draw( drawContext, drawContext.Camera.GetProjMatrix(), *mainDepthRT.get( ), !m_settings.DebugShowOpaqueSSAO, nullptr, scissorRectForSSAO );

                        if( m_SSAOEffect_DevelopmentVersion->GetDebugShowSamplesAtCursorEnabled() )
                        {
                            drawContext.Globals.UpdateDebugOutputFloats( drawContext );
                        }

                        // restore main render target / depth
                        mainContext.SetRenderTarget( mainColorRT, mainDepthRT, true );*/
                        throw new UnsupportedOperationException();
                    }
                    else if( m_settings.SSAOSelectedVersionIndex == 1 )
                    {
//                        VA_SCOPE_CPUGPU_TIMER_DEFAULTSELECTED( ASSAO, mainContext );

                        // set destination render target and no depth
                        mainContext.SetRenderTarget( mainColorRT, null, true );

                        if( m_settings.UseDeferred )
                            m_SSAOEffect.Draw( drawContext, mainDepthRT, !m_settings.DebugShowOpaqueSSAO, m_GBuffer.GetNormalMap( ), scissorRectForSSAO );
                        else
                            m_SSAOEffect.Draw( drawContext, mainDepthRT, !m_settings.DebugShowOpaqueSSAO, null, scissorRectForSSAO );

                        // restore main render target / depth
                        mainContext.SetRenderTarget( mainColorRT, mainDepthRT, true );
                    }
                    else if( m_settings.SSAOSelectedVersionIndex == 2 )
                    {
//                        VA_SCOPE_CPUGPU_TIMER_DEFAULTSELECTED( ExternalSSAO, mainContext );

                        // set destination render target and no depth
                        mainContext.SetRenderTarget( mainColorRT, null, true );

                        if( m_settings.UseDeferred )
                            m_SSAOEffect_External.Draw( drawContext, mainDepthRT, m_GBuffer.GetNormalMap( ), !m_settings.DebugShowOpaqueSSAO, scissorRectForSSAO );
                        else
                            m_SSAOEffect_External.Draw( drawContext, mainDepthRT, null, !m_settings.DebugShowOpaqueSSAO, scissorRectForSSAO );

                        // restore main render target / depth
                        mainContext.SetRenderTarget( mainColorRT, mainDepthRT, true );
                    }
                    else { assert( false ); }
                }
            }

            // Debug wireframe
            if( m_settings.ShowWireframe )
            {
//                VA_SCOPE_CPUGPU_TIMER( Wireframe, mainContext );
                drawContext.PassType = VaRenderPassType.ForwardDebugWireframe;

                // this sets up global constants
                m_renderingGlobals.SetAPIGlobals( drawContext );

                // m_scene->Draw( drawContext );
                // //for( size_t i = 0; i < frustumCulledTrees.size( ); i++ )
                // //    frustumCulledTrees[i]->Draw( drawContext );
                //
                // m_testObjectRenderer->DrawMeshes( drawContext, m_testObjects );
                VaRenderMeshManager.GetInstance().Draw( drawContext, m_meshDrawList );
            }

            if( m_simpleShadowMap != null )
            {
                if( m_simpleShadowMap.GetVolumeShadowMapPlugin( ) != null )
                    m_simpleShadowMap.GetVolumeShadowMapPlugin().StopUsing( drawContext, m_simpleShadowMap );
                m_simpleShadowMap.StopUsing( drawContext );
            }
        }

        m_meshDrawList.Reset();

        // GBuffer debug (but show only if deferred enabled)
        if( m_settings.UseDeferred )
        {
//            VA_SCOPE_CPUGPU_TIMER( GbufferDebug, mainContext );

            VaDrawContext drawContext = new VaDrawContext(m_camera, mainContext, m_renderingGlobals, m_lighting);
            // this sets up global constants
            m_renderingGlobals.SetAPIGlobals( drawContext );

            // remove depth
            mainContext.SetRenderTarget( mainColorRT, null, true );

            // draw debug stuff
            m_GBuffer.RenderDebugDraw( drawContext );

            // restore main render target / depth
            mainContext.SetRenderTarget( mainColorRT, mainDepthRT, true );
        }

        {
//            VA_SCOPE_CPU_TIMER( DebugCanvas3D );

            /*vaDrawContext drawContext( *m_camera.get( ), mainContext, *m_renderingGlobals.get( ), m_lighting.get( ) );
            // this sets up global constants
            m_renderingGlobals->SetAPIGlobals( drawContext );

            vaDebugCanvas3DBase * canvas3D = GetCanvas3D( );

            //canvas3D->DrawBox( m_debugBoxPos, m_debugBoxSize, 0xFF000000, 0x20FF0000 );
            canvas3D->DrawAxis( vaVector3( 0, 0, 0 ), 100.0f, NULL, 0.3f );
            m_renderDevice->DrawDebugCanvas3D( drawContext );*/
        }

        {
//            VA_SCOPE_CPU_TIMER( DebugCanvas2DAndStuff );

            /*if( m_settings.SSAOSelectedVersionIndex == 0 )
            {
                if( m_SSAOEffect_DevelopmentVersion->GetDebugShowSamplesAtCursorEnabled( ) )
                {
                    const float * debugData = nullptr;
                    int debugDataCount = 0;
                    m_renderingGlobals->GetShaderDebugFloatOutput( debugData, debugDataCount );

                    const int * debugDataInt = (const int *)debugData;

                    vaVector2i cursorPos( debugDataInt[0], debugDataInt[1] );
                    cursorPos.x -= m_expandedSceneBorder;
                    cursorPos.y -= m_expandedSceneBorder;

                    m_renderDevice->GetCanvas2D( )->DrawLine( (float)cursorPos.x - 500.0f, (float)cursorPos.y, (float)cursorPos.x - 2.0f, (float)cursorPos.y, 0x60FF0000 );
                    m_renderDevice->GetCanvas2D( )->DrawLine( (float)cursorPos.x + 500.0f, (float)cursorPos.y, (float)cursorPos.x + 2.0f, (float)cursorPos.y, 0x6000FF00 );
                    m_renderDevice->GetCanvas2D( )->DrawLine( (float)cursorPos.x, (float)cursorPos.y - 500.0f, (float)cursorPos.x, (float)cursorPos.y - 2.0f, 0x60FF0000 );
                    m_renderDevice->GetCanvas2D( )->DrawLine( (float)cursorPos.x, (float)cursorPos.y + 500.0f, (float)cursorPos.x, (float)cursorPos.y + 2.0f, 0x6000FF00 );

                    int count = vaMath::Clamp( debugDataInt[2], 0, debugDataCount/2-4 );
                    const float * samples = (const float *)&debugData[4];

                    float ratioX = (float)mainViewportExpanded.Width  / (float)mainViewport.Width ;
                    float ratioY = (float)mainViewportExpanded.Height / (float)mainViewport.Height;

                    for( int i = 0; i < count; i++ )
                    {
                        vaVector3 pt = *((vaVector3*)(&samples[i*5]));

                        pt.x -= m_expandedSceneBorder / (float)mainViewportExpanded.Width;
                        pt.y -= m_expandedSceneBorder / (float)mainViewportExpanded.Height;

                        pt.x *= ratioX * (float)mainViewport.Width;
                        pt.y *= ratioY * (float)mainViewport.Height;

                        pt.x = floor( pt.x ) + 0.5f;
                        pt.y = floor( pt.y ) + 0.5f;

                        int mip = (int)( pt.z + 0.5f );
                        uint32 color = 0xF0FF0000;
                        if( mip == 1 )  color = 0xF000A010;
                        if( mip == 2 )  color = 0xF00020FF;
                        if( mip == 3 )  color = 0xF0000000;

                        int rectSize = 3;
                        int rectOff = rectSize / 2;

                        m_renderDevice->GetCanvas2D( )->FillRectangle( pt.x - (float)rectOff, pt.y - (float)rectOff, (float)rectSize, (float)rectSize, color );

                        rectSize = 7;
                        rectOff = rectSize / 2;

                        float sampleWeight  = samples[i*5+3];
                        float sampleValue   = samples[i*5+4];

                        float rectSizeY = rectSize * sampleWeight;
                        m_renderDevice->GetCanvas2D( )->FillRectangle( pt.x - (float)rectOff + 2.0f + rectSize * 0.5f, pt.y - rectSizeY, (float)rectSize, (float)rectSizeY, vaVector4::ToBGRA( vaVector4( sampleValue, sampleValue, sampleValue, 1.0f ) ) );

                        m_renderDevice->GetCanvas2D( )->DrawRectangle( pt.x - (float)rectOff + 2.0f + rectSize * 0.5f, pt.y - rectSizeY, (float)0.5f, (float)rectSizeY, 0x800000FF );

                        m_renderDevice->GetCanvas2D()->DrawLine( pt.x, pt.y, pt.x + 1, pt.y + 1, 0xFF000000 );
                    }
                }
            }

            if( m_displaySampleDisk.size() != 0 )
            {
                uint32 colorBackground  = 0xC0FFFFFF;
                uint32 colorDisk        = 0xFF008000;
                uint32 colorDiskBk      = 0x80008000;
                uint32 colorCentre      = 0xFF000000;
                uint32 colorLine        = 0x50FF0000;
                uint32 colorText        = 0xFF000000;
                uint32 colorTextBk      = 0xFF808080;

                float drawRadius = 180.0f;

                vaVector2 rectSize( drawRadius * 2.4f, drawRadius * 2.4f );
                vaVector2 rectCentre( rectSize.x * 0.6f, rectSize.y * 0.6f );
                vaVector2 rectTopLeft( rectCentre.x - rectSize.x * 0.5f, rectCentre.y - rectSize.y * 0.5f );

                m_renderDevice->GetCanvas2D( )->FillRectangle( rectTopLeft.x, rectTopLeft.y, rectSize.x, rectSize.y, colorBackground );

                vaVector4 ptPrev;

                //m_currentPoissonDiskMinDist = vaMath::Clamp( m_currentPoissonDiskMinDist, 0.05f, 1.0f );
                float circleRadius = 0.025f;

                ptPrev = vaVector4( 0.0f, 0.0f, 0.0f, 0.0f );
                for( int i = 0; i < m_displaySampleDisk.size(); i++ )
                {
                    vaVector4 & pt = m_displaySampleDisk[i];

                    vaVector2 lineFrom  = vaVector2( rectCentre.x + drawRadius * ptPrev.x, rectCentre.y + drawRadius * ptPrev.y );
                    vaVector2 lineTo    = vaVector2( rectCentre.x + drawRadius * pt.x, rectCentre.y + drawRadius * pt.y );

                    vaVector2 lineDir = lineTo - lineFrom;
                    float lineLength = lineDir.Length();

                    if( (lineLength > VA_EPSf) && (i>0) )
                    {
                        lineDir /= lineLength;
                        float shortenAmount = vaMath::Min( circleRadius * drawRadius * 2.0f, lineLength * 0.25f );
                        lineFrom += lineDir * shortenAmount;
                        lineTo -= lineDir * shortenAmount;

                        m_renderDevice->GetCanvas2D()->DrawLine( lineFrom, lineTo, colorLine );
                        m_renderDevice->GetCanvas2D()->DrawLineArrowhead( lineFrom, lineTo, drawRadius * 0.03f, colorLine );

                        lineFrom += vaVector2( 1, 0 ); lineTo += vaVector2( 1, 0 );
                        m_renderDevice->GetCanvas2D()->DrawLine( lineFrom, lineTo, colorLine );
                        m_renderDevice->GetCanvas2D()->DrawLineArrowhead( lineFrom, lineTo, drawRadius * 0.03f, colorLine );
                        lineFrom += vaVector2( -1, 1 ); lineTo += vaVector2( -1, 1 );
                        m_renderDevice->GetCanvas2D()->DrawLine( lineFrom, lineTo, colorLine );
                        m_renderDevice->GetCanvas2D()->DrawLineArrowhead( lineFrom, lineTo, drawRadius * 0.03f, colorLine );
                        lineFrom += vaVector2( 1, 0 ); lineTo += vaVector2( 1, 0 );
                        m_renderDevice->GetCanvas2D()->DrawLine( lineFrom, lineTo, colorLine );
                        m_renderDevice->GetCanvas2D()->DrawLineArrowhead( lineFrom, lineTo, drawRadius * 0.03f, colorLine );
                    }

                    ptPrev = pt;
                }

                ptPrev = vaVector4( 0.0f, 0.0f, 0.0f, 0.0f );
                for( int i = 0; i < m_displaySampleDisk.size(); i++ )
                {
                    vaVector4 & pt = m_displaySampleDisk[i];

                    m_renderDevice->GetCanvas2D( )->DrawCircle( rectCentre.x + drawRadius * pt.x, rectCentre.y + drawRadius * pt.y, drawRadius * circleRadius, colorDiskBk, 1.0f );
                    m_renderDevice->GetCanvas2D( )->DrawCircle( rectCentre.x + drawRadius * pt.x, rectCentre.y + drawRadius * pt.y, drawRadius * circleRadius - 0.5f, colorDiskBk, 1.0f );
                    m_renderDevice->GetCanvas2D( )->DrawCircle( rectCentre.x + drawRadius * pt.x, rectCentre.y + drawRadius * pt.y, drawRadius * circleRadius - 1.0f, colorDisk, 1.0f );
                    m_renderDevice->GetCanvas2D( )->DrawCircle( rectCentre.x + drawRadius * pt.x, rectCentre.y + drawRadius * pt.y, drawRadius * circleRadius - 1.5f, colorDiskBk, 1.0f );
                    m_renderDevice->GetCanvas2D( )->DrawCircle( rectCentre.x + drawRadius * pt.x, rectCentre.y + drawRadius * pt.y, drawRadius * circleRadius - 1.9f, colorDiskBk, 1.0f );
                    //m_renderDevice->GetCanvas2D()->DrawLine( rectCentre.x + drawRadius * pt.x, rectCentre.y + drawRadius * pt.y, rectCentre.x + drawRadius * pt.x + 1, rectCentre.y + drawRadius * pt.y + 1, colorCentre );

                    m_renderDevice->GetCanvas2D( )->DrawString( (int)(rectCentre.x + drawRadius * pt.x + 5.0f), (int)(rectCentre.y + drawRadius * pt.y + 5.0f), colorText, colorTextBk, "%d", i );

                    ptPrev = pt;
                }
            }

            m_renderDevice->DrawDebugCanvas2D( );*/
        }

        if( m_screenshotCapturePath != null )
        {
            /*wstring directory;
            vaStringTools::SplitPath( m_screenshotCapturePath, &directory, nullptr, nullptr );
            vaFileTools::EnsureDirectoryExists( directory.c_str() );

            vaDrawContext drawContext( *m_camera.get( ), mainContext, *m_renderingGlobals.get( ) );
            m_postProcess->SaveTextureToPNGFile( drawContext, m_screenshotCapturePath, *mainColorRT.get( ) );*/
            m_screenshotCapturePath = null;
        }

        // restore display
//        mainContext.SetRenderTarget( m_renderDevice->GetMainChainColor(), m_renderDevice->GetMainChainDepth(), true );
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glViewport(0,0, getGLContext().width(), getGLContext().height());

        // restore camera border expansion hack
        {
            m_camera.SetYFOV( m_settings.CameraYFov );
            m_camera.SetViewportSize( mainViewportBackup.Width, mainViewportBackup.Height );
            m_camera.Tick( 0.0f, false );  // re-tick to restore
        }

        // Final apply to screen
        {
//            VA_SCOPE_CPUGPU_TIMER( FinalApply, mainContext );

            VaDrawContext drawContext = new VaDrawContext(m_camera, mainContext, m_renderingGlobals, m_lighting);
            // this sets up global constants
            m_renderingGlobals.SetAPIGlobals( drawContext );

            m_postProcess.StretchRect( drawContext, mainColorRT, new Vector4f( (float)m_expandedSceneBorder, (float)m_expandedSceneBorder, (float)m_expandedSceneBorder+mainViewport.Width, (float)m_expandedSceneBorder+mainViewport.Height ),
                    new Vector4f( 0.0f, 0.0f, (float)mainViewport.Width, (float)mainViewport.Height), true );

            mainViewport = mainViewportBackup;
        }
    }

//    public VaDebugCanvas2DBase                   GetCanvas2D( )                  { return m_renderDevice->GetCanvas2D( ); }
//    vaDebugCanvas3DBase *                   GetCanvas3D( )                  { return m_renderDevice->GetCanvas3D( ); }

    public SSAODemoSettings  GetSettings( )                  { return m_settings; }

    protected void SetupScene( ){
        m_sky.GetSettings().SunElevation   = Numeric.PI * 0.26f;
        m_sky.GetSettings().SunAzimuth     = -1.0f;

        m_lighting.SetAmbientLightIntensity( new Vector3f( 0.65f, 0.65f, 0.65f ) );
        m_lighting.SetDirectionalLightIntensity( new Vector3f( 0.4f, 0.4f, 0.4f ) );

        final float keyTime = 10.0f;

        m_flythroughCameraControllerSibenik = new VaCameraControllerFocusLocationsFlythrough( );
        {
            m_flythroughCameraControllerSibenik.SetFixedUp( true ); // so that we can seamlessly switch between flythrough and manual camera

            m_flythroughCameraControllerSibenik.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 16.820f, 0.289f, 5.752f ), new Quaternion( 0.591f, -0.564f, -0.398f, 0.417f ), keyTime ) );
            m_flythroughCameraControllerSibenik.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 17.244f, 2.268f, 9.419f ), new Quaternion( 0.797f, -0.436f, -0.201f, 0.367f ), keyTime ) );
            m_flythroughCameraControllerSibenik.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 17.244f, 2.268f, 9.419f ), new Quaternion( 0.392f, -0.275f, -0.504f, 0.719f ), keyTime ) );
            m_flythroughCameraControllerSibenik.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 14.309f, -1.641f, 3.127f ), new Quaternion( -0.445f, 0.581f, 0.541f, -0.415f ), keyTime ) );
            m_flythroughCameraControllerSibenik.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 14.043f, 0.294f, 2.762f ), new Quaternion( 0.524f, -0.505f, -0.476f, 0.494f ), keyTime ) );
            m_flythroughCameraControllerSibenik.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 6.251f, -0.121f, 4.470f ), new Quaternion( 0.616f, -0.589f, -0.362f, 0.379f ), keyTime ) );
            m_flythroughCameraControllerSibenik.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( -4.851f, -5.866f, 1.764f ), new Quaternion( -0.325f, 0.566f, 0.657f, -0.378f ), keyTime ) );
            m_flythroughCameraControllerSibenik.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( -12.964f, -6.167f, 2.231f ), new Quaternion( -0.053f, 0.735f, 0.674f, -0.048f ), keyTime ) );
            m_flythroughCameraControllerSibenik.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( -16.166f, -4.147f, 2.707f ), new Quaternion( 0.801f, -0.446f, -0.194f, 0.349f ), keyTime ) );
            m_flythroughCameraControllerSibenik.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( -18.692f, 2.723f, 1.157f ), new Quaternion( 0.513f, 0.343f, 0.438f, 0.654f ), keyTime ) );
            m_flythroughCameraControllerSibenik.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( -15.549f, 2.016f, 15.341f ), new Quaternion( 0.681f, 0.525f, 0.312f, 0.404f ), keyTime ) );
            m_flythroughCameraControllerSibenik.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 0.511f, -0.125f, 14.342f ), new Quaternion( 0.643f, 0.655f, 0.283f, 0.278f ), keyTime ) );
            m_flythroughCameraControllerSibenik.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 8.394f, 4.057f, 8.990f ), new Quaternion( 0.897f, 0.165f, 0.074f, 0.402f ), keyTime ) );
            m_flythroughCameraControllerSibenik.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 8.923f, 7.392f, 4.358f ), new Quaternion( 0.667f, -0.005f, -0.006f, 0.745f ), keyTime ) );
            m_flythroughCameraControllerSibenik.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 13.683f, 5.797f, 8.309f ), new Quaternion( 0.753f, -0.361f, -0.237f, 0.496f ), keyTime ) );
            m_flythroughCameraControllerSibenik.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 15.177f, 0.169f, 8.228f ), new Quaternion( 0.490f, -0.476f, -0.508f, 0.524f ), keyTime ) );
        }

        m_flythroughCameraControllerSponza = new VaCameraControllerFocusLocationsFlythrough( );
        {
            m_flythroughCameraControllerSponza.SetFixedUp( true ); // so that we can seamlessly switch between flythrough and manual camera

            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 9.142f, -0.315f, 3.539f ), new Quaternion( 0.555f, 0.552f, 0.439f, 0.441f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 11.782f, -0.078f, 1.812f ), new Quaternion( 0.463f, -0.433f, -0.528f, 0.565f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 5.727f, -1.077f, 2.716f ), new Quaternion( -0.336f, 0.619f, 0.624f, -0.339f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( -2.873f, 1.043f, 2.808f ), new Quaternion( 0.610f, -0.378f, -0.367f, 0.592f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( -7.287f, 1.254f, 2.598f ), new Quaternion( 0.757f, 0.004f, 0.003f, 0.654f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( -12.750f, 0.051f, 2.281f ), new Quaternion( 0.543f, 0.448f, 0.452f, 0.548f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( -14.431f, -3.854f, 2.411f ), new Quaternion( 0.556f, 0.513f, 0.443f, 0.481f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( -14.471f, -6.127f, 1.534f ), new Quaternion( 0.422f, 0.520f, 0.577f, 0.467f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( -8.438f, -5.876f, 4.094f ), new Quaternion( 0.391f, 0.784f, 0.432f, 0.215f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( -2.776f, -4.915f, 1.890f ), new Quaternion( 0.567f, 0.646f, 0.384f, 0.337f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( -1.885f, -4.796f, 2.499f ), new Quaternion( 0.465f, 0.536f, 0.532f, 0.462f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 1.569f, -4.599f, 3.303f ), new Quaternion( 0.700f, 0.706f, 0.079f, 0.078f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 4.799f, -5.682f, 3.353f ), new Quaternion( 0.037f, 0.900f, 0.434f, 0.018f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 7.943f, -5.405f, 3.416f ), new Quaternion( -0.107f, 0.670f, 0.725f, -0.115f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 11.445f, -3.276f, 3.319f ), new Quaternion( -0.455f, 0.589f, 0.529f, -0.409f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 12.942f, 2.277f, 3.367f ), new Quaternion( 0.576f, -0.523f, -0.423f, 0.465f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 12.662f, 3.895f, 4.186f ), new Quaternion( 0.569f, -0.533f, -0.428f, 0.457f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 8.688f, 4.170f, 4.107f ), new Quaternion( 0.635f, -0.367f, -0.340f, 0.588f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 6.975f, 1.525f, 4.299f ), new Quaternion( 0.552f, -0.298f, -0.369f, 0.685f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 5.497f, -0.418f, 7.013f ), new Quaternion( 0.870f, -0.124f, -0.067f, 0.473f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 9.520f, -2.108f, 6.619f ), new Quaternion( 0.342f, 0.599f, 0.629f, 0.359f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( 11.174f, 3.226f, 6.969f ), new Quaternion( -0.439f, 0.536f, 0.558f, -0.457f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( -2.807f, 5.621f, 7.026f ), new Quaternion( 0.694f, 0.013f, 0.014f, 0.720f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( -11.914f, 5.271f, 7.026f ), new Quaternion( 0.694f, 0.013f, 0.014f, 0.720f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( -12.168f, 1.401f, 7.235f ), new Quaternion( 0.692f, -0.010f, -0.011f, 0.722f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( -6.541f, 0.038f, 7.491f ), new Quaternion( 0.250f, -0.287f, -0.697f, 0.608f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( -6.741f, 0.257f, 2.224f ), new Quaternion( 0.511f, -0.465f, -0.487f, 0.535f ), keyTime ) );
            m_flythroughCameraControllerSponza.AddKey( new VaCameraControllerFocusLocationsFlythrough.Keyframe( new Vector3f( -10.913f, -0.020f, 2.766f ), new Quaternion( 0.511f, -0.471f, -0.487f, 0.529f ), keyTime ) );
        }

        m_flythroughCameraControllerLostEmpire = new VaCameraControllerFocusLocationsFlythrough( );
        {
            m_flythroughCameraControllerLostEmpire.SetFixedUp( true ); // so that we can seamlessly switch between flythrough and manual camera
        }
    }

    protected abstract void DrawDebugOverlay( VaDrawContext drawContext );

    protected void EnsureLoaded( VaAssetPack pack ){
        if( pack.Count( ) == 0 )
        {
            String assetsPath = VaCore.GetWorkingDirectory() + "Media/" + pack.Name() + ".apack";

            long startTime = System.currentTimeMillis();

            try(VaFileStream fileIn = new VaFileStream()){
                if( fileIn.Open( assetsPath, VaFileStream.FileCreationMode.Open ) )
                {
                    if( pack.Load( fileIn ) )
                    {
                        String log = "   loaded ok, " + pack.Count() + " assets.";
                        LogUtil.i(LogUtil.LogType.DEFAULT, log);
                    }
                    else
                    {
                        LogUtil.e(LogUtil.LogType.DEFAULT, "   error loading.");
                    }
                }
                else
                {
                    LogUtil.e(LogUtil.LogType.DEFAULT, "   error loading, asset file not found.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                long endTime = System.currentTimeMillis();
                LogUtil.i(LogUtil.LogType.DEFAULT, String.format("Loading '%s' taken time: %d millis.", assetsPath, endTime - startTime));
            }
        }
    }

    protected void InsertAllToSceneMeshesList( VaAssetPack pack, Matrix4f transform /*= Matrix4f.IDENTITY*/ ){
        for( int i = 0; i < pack.Count(); i++ )
        {
            VaAsset asset = pack.Get(i);
            if( asset.GetMesh( ) != null )
            {
                m_sceneMeshes.add( asset.GetMesh( ) );
                m_sceneMeshesTransforms.add( transform );
            }
        }
    }

    protected void UpdateTextures( int width, int height ){
        if( m_comparerReferenceTexture != null && m_comparerReferenceTexture.GetSizeX() == width && m_comparerReferenceTexture.GetSizeY() == height )
            return;

        VaGBuffer.BufferFormats gbufferFormats = m_GBuffer.GetFormats();

        m_comparerReferenceTexture = VaTexture.Create2D( VaTexture.R8G8B8A8_UNORM_SRGB, width, height, 1, 1, 1, VaTexture.BSF_ShaderResource | VaTexture.BSF_RenderTarget );
        m_comparerCurrentTexture   = VaTexture.Create2D( VaTexture.R8G8B8A8_UNORM_SRGB, width, height, 1, 1, 1, VaTexture.BSF_ShaderResource | VaTexture.BSF_RenderTarget );

    }

    public void LoadCamera( int index /*= -1*/ ){
        try(VaFileStream fileIn = new VaFileStream()){
            if( fileIn.Open( CameraFileName(index), VaFileStream.FileCreationMode.Open ) )
            {
                m_camera.Load( fileIn );
                m_camera.AttachController( m_cameraFreeFlightController );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // just for debug displaying purposes
    public void SetDisplaySampleDisk( List<Vector4f> displaySampleDisk /*= vector<vaVector4>() */) { m_displaySampleDisk = displaySampleDisk; }

    public void CaptureScreenshotNextFrame(String path ) { m_screenshotCapturePath = /*vaFileTools::GetAbsolutePath*/( path ); }

    public VaCameraControllerBase GetCameraController( ){
        if( m_flythroughCameraEnabled )
        {
            switch( m_settings.SceneChoice )
            {
                case (Sibenik):
                case (SibenikAndDragons):
                    return m_flythroughCameraControllerSibenik;
                case (Sponza):
                case (SponzaAndDragons):
                    return m_flythroughCameraControllerSponza;
                case (LostEmpire):
                    return m_flythroughCameraControllerLostEmpire;
                default:
                    return m_cameraFreeFlightController;
            }
        }
        else
        {
            return m_cameraFreeFlightController;
        }
    }


    void SaveCamera( int index )
    {
        try(VaFileStream fileOut = new VaFileStream()){
            if( fileOut.Open( CameraFileName(index), VaFileStream.FileCreationMode.Create, VaFileStream.FileAccessMode.Default ) )
            {
                m_camera.Save( fileOut );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static String CameraFileName( int index )
    {
        String fileName = /*vaCore::GetExecutableDirectory( ) +*/ "camerapos";
        if( index != -1 )
            fileName += /*VaStringTools::Format( L"_%d", index )*/ '_'+index;
        fileName += ".data";
        return fileName;
    }

    private static<T extends  VaRenderingModule> T VA_RENDERING_MODULE_CREATE_UNIQUE(String name){
        return (T) VaRenderingModuleRegistrar.CreateModule(name, null);
    }

    @Override
    public String GetRenderingModuleTypeName() {
        return m_renderingModuleTypeName;
    }

    @Override
    public void InternalRenderingModuleSetTypeName(String name) {
        m_renderingModuleTypeName = name;
    }

    public static final class SSAODemoSettings implements Writable, Readable
    {
        static final int SIZE = 4+3+4+3+4+1;
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

        @Override
        public Writable load(ByteBuffer buf) {
            SceneChoice = buf.getInt();
            UseDeferred = buf.get() != 0;
            ShowWireframe = buf.get() != 0;
            EnableSSAO = buf.get() != 0;
            SSAOSelectedVersionIndex = buf.getInt();
            DebugShowOpaqueSSAO = buf.get() != 0;
            DisableTexturing = buf.get() != 0;
            ExpandDrawResolution = buf.get() != 0;
            CameraYFov = buf.getFloat();
            UseSimpleUI = buf.get() != 0;
            return null;
        }

        @Override
        public ByteBuffer store(ByteBuffer buf) {
            final byte t = 1;
            final byte f = 0;
            buf.putInt(SceneChoice);
            buf.put(UseDeferred ? t : f);
            buf.put(ShowWireframe ? t : f);
            buf.put(EnableSSAO ? t : f);
            buf.putInt(SSAOSelectedVersionIndex);
            buf.put(DebugShowOpaqueSSAO ? t : f);
            buf.put(DisableTexturing ? t : f);
            buf.put(ExpandDrawResolution ? t : f);
            buf.putFloat(CameraYFov);
            buf.put(UseSimpleUI ? t : f);
            return buf;
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
