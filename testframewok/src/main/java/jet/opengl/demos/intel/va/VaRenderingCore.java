package jet.opengl.demos.intel.va;

import java.util.ArrayDeque;

import jet.opengl.postprocessing.common.Disposeable;

/**
 * Created by mazhen'gui on 2017/11/16.
 */

public final class VaRenderingCore implements Disposeable{
    private final ArrayDeque<String> m_assetSearchPaths = new ArrayDeque<>();
    private static VaRenderingCore g_Instance;

    private VaRenderingCore(){
        /*EmbeddedRenderingMedia.load();
        for( int i = 0; i < EmbeddedRenderingMedia.BINARY_EMBEDDER_ITEM_COUNT; i++ )
        {
            String name = EmbeddedRenderingMedia.BINARY_EMBEDDER_NAMES[i];
            byte[] data = EmbeddedRenderingMedia.BINARY_EMBEDDER_DATAS[i];
            int dataSize = EmbeddedRenderingMedia.BINARY_EMBEDDER_SIZES[i];
//            int64 timeStamp = EmbeddedRenderingMedia.BINARY_EMBEDDER_TIMES[i];

            VaFileTools.EmbeddedFilesRegister( name, data, dataSize*//*, timeStamp*//* );
        }*/
    }

    public static VaRenderingCore GetInstance(){
        if(g_Instance == null){
            Initialize();
        }

        return g_Instance;
    }

    @Override
    public void dispose() {
        VaRenderingModuleRegistrar.DeleteSingleton( );
    }

    // at the moment vaRenderingCore handles asset loading - if there's need in the future, this can be split into a separate class or even a separate module

    // pushBack (searched last) or pushFront (searched first)
    public void RegisterAssetSearchPath( String searchPath/*, boolean pushBack = true*/ ){
        String cleanedSearchPath = VaFileTools.CleanupPath( searchPath + "\\", false );
//        if( pushBack )
            m_assetSearchPaths.addLast( cleanedSearchPath );
//        else
//            m_assetSearchPaths.addFirst( cleanedSearchPath );
    }

    public String FindAssetFilePath( String assetFileName ){
//        for( int i = 0; i < m_assetSearchPaths.size( ); i++ )
        String fileName = assetFileName;
        for(String searchPath : m_assetSearchPaths)
        {
            String filePath = /*m_assetSearchPaths[i]*/searchPath + "\\" + fileName;
            if( VaFileTools.FileExists( filePath) )
            {
                return VaFileTools.CleanupPath( filePath, false );
            }
            if( VaFileTools.FileExists( ( VaCore.GetWorkingDirectory( ) + filePath ) ) )
            {
                return VaFileTools.CleanupPath( VaCore.GetWorkingDirectory( ) + filePath, false );
            }
        }

        if( VaFileTools.FileExists( ( VaCore.GetWorkingDirectory( ) + fileName ) ) )
        return VaFileTools.CleanupPath( VaCore.GetWorkingDirectory( ) + fileName, false );
//        if( VaFileTools.FileExists( ( VaCore.GetExecutableDirectory( ) + fileName ).c_str( ) ) )
//        return VaFileTools.CleanupPath( VaCore.GetExecutableDirectory( ) + fileName, false );
        if( VaFileTools.FileExists( fileName ) )
        return VaFileTools.CleanupPath( fileName, false );

        return "";
    }

    // Initialize the system - must be called before any other calls to the module
    static void Initialize( ){
        assert( !IsInitialized() );
        g_Instance = new VaRenderingCore();

        InitializePlatform( );
        OnAPIInitialized();
    }

    static void Deinitialize( ){
        assert( IsInitialized( ) );
        g_Instance.dispose();
        g_Instance = null;
    }

    static void  OnAPIInitialized( ){
        VaRenderingModuleRegistrar.CreateModuleTyped("vaRenderMaterialManager", null);
        VaRenderingModuleRegistrar.CreateModuleTyped("vaRenderMeshManager", null);
    }

    static void                             OnAPIAboutToBeDeinitialized( ){}

    static boolean                             IsInitialized( )                                            { return g_Instance != null; }

    private static void InitializePlatform( ){
        /*VA_RENDERING_MODULE_REGISTER( vaTexture, vaTextureDX11 );  TODEO
        VA_RENDERING_MODULE_REGISTER( vaTriangleMesh_PositionColorNormalTangentTexcoord1Vertex, vaTriangleMeshDX11_PositionColorNormalTangentTexcoord1Vertex );

        VA_RENDERING_MODULE_REGISTER( vaGPUTimer, vaGPUTimerDX11 );*/

        /*RegisterCanvasDX11( );
        RegisterSimpleShadowMapDX11( );
        RegisterSkyDX11( );
        RegisterASSAODX11( );
        RegisterSimpleParticleSystemDX11( );
        RegisterRenderingGlobals( );
        RegisterRenderMaterialDX11( );
        RegisterRenderMeshDX11( );
        RegisterGBufferDX11( );
        RegisterPostProcessDX11( );
        RegisterLightingDX11( );
        RegisterPostProcessTonemapDX11( );
        RegisterPostProcessBlurDX11( );*/

        VaRenderingModuleRegistrar.RegisterModule("vaSky", (params -> new VaSkyDX11(params)));
        VaRenderingModuleRegistrar.RegisterModule("vaRenderingGlobals", (params -> new VaRenderingGlobalsDX11(params)));
        VaRenderingModuleRegistrar.RegisterModule("vaGBuffer", (params -> new VaGBufferDX11(params)));
        VaRenderingModuleRegistrar.RegisterModule("vaLighting", (params -> new VaLightingDX11(params)));
        VaRenderingModuleRegistrar.RegisterModule("vaRenderMeshManager", (params -> new VaRenderMeshManagerDX11(params)));
        VaRenderingModuleRegistrar.RegisterModule("vaRenderMaterial", (params -> new VaRenderMaterialDX11(params)));
        VaRenderingModuleRegistrar.RegisterModule("vaRenderMaterialManager", (params -> new VaRenderMaterialManagerDX11(/*params*/)));
        VaRenderingModuleRegistrar.RegisterModule("vaRenderMesh::StandardTriangleMesh", (params -> new VaTriangleMeshDX11(params)));
        VaRenderingModuleRegistrar.RegisterModule("vaTexture", (params -> new VaTextureDX11(params)));
        VaRenderingModuleRegistrar.RegisterModule("vaRenderDeviceContext", (params -> new VaRenderDeviceContextDX11(params)));
    }
}
