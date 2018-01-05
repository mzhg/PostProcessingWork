package jet.opengl.demos.intel.va;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.shader.ShaderSourceItem;

/**
 * Created by mazhen'gui on 2017/11/18.
 */
public final class VaDirectXShaderManager implements Disposeable{
    protected boolean m_settings;

    private final Map<VaShaderCacheKey, VaShaderCacheEntry> m_cache = new HashMap<>();
    private boolean                                     m_cacheLoaded;

    private final ArrayDeque<String>                    m_searchPaths = new ArrayDeque<>(8);

    private String                                      m_cacheFileDir;

    private final List<VaDirectXShader>                 m_allShaderList = new ArrayList<>();

    private static  VaDirectXShaderManager g_Instance;

    TT_Tracker< VaUIDObject >                m_objects;

    private Map<UUID, VaUIDObject/*, vaGUIDComparer*/ > m_objectsMap = new HashMap<>();

    public static VaDirectXShaderManager GetInstance() {
        if(g_Instance == null){
            g_Instance = new VaDirectXShaderManager();
        }
        return g_Instance;
    }

    VaDirectXShaderManager( )
    {
        m_cacheLoaded = false;

        // this should be set externally, but good enough for now
        m_cacheFileDir = /*VaCore.GetExecutableDirectory( ) +*/ "cache\\";
    }

    public ShaderSourceItem FindInCache(VaShaderCacheKey key, boolean[] foundButModified ){
        boolean _foundButModified = false;

        if( !m_cacheLoaded )
        {
//#ifdef USE_SHADER_CACHE_SYSTEM
            LoadCache( GetCacheFileName()/*.c_str()*/ );
//#endif
        }

//        std::map<vaShaderCacheKey, vaShaderCacheEntry *>::iterator it = m_cache.find( key );
        VaShaderCacheEntry it = m_cache.get(key);

        try {
            if( it != /*m_cache.end( )*/ null )
            {
                if( /*( * it).second->*/it.IsModified( ) )
                {
                    _foundButModified = true;

                    // Have to recompile...
                /*delete ( *it ).second;
                m_cache.erase( it );*/
                    m_cache.remove(key);

                    return null;
                }

//            return ( *it ).second->GetCompiledShader( );
                return it.GetCompiledShader();
            }
            return null;
        }finally {
            foundButModified[0] = _foundButModified;
        }
    }

    public void AddToCache( VaShaderCacheKey key, ShaderSourceItem shaderBlob, List<VaShaderCacheEntry.FileDependencyInfo> dependencies ){
        /*std::map<vaShaderCacheKey, vaShaderCacheEntry *>::iterator it = m_cache.find( key );

        if( it != m_cache.end( ) )
        {
            // Already in?
            assert( false );
            delete ( *it ).second;
            m_cache.erase( it );
        }*/
        m_cache.remove(key);

        /*m_cache.insert( std::pair<vaShaderCacheKey, vaShaderCacheEntry *>( key, new vaShaderCacheEntry( shaderBlob, dependencies ) ) );*/
        m_cache.put(key, new VaShaderCacheEntry(shaderBlob, dependencies));
    }

    public void                ClearCache( ){
        /*for( std::map<vaShaderCacheKey, vaShaderCacheEntry *>::iterator it = m_cache.begin( ); it != m_cache.end( ); it++ )
        {
            delete ( *it ).second;;
        }*/
        m_cache.clear( );
    }

    // pushBack (searched last) or pushFront (searched first)
    public void RegisterShaderSearchPath( String path/*, boolean pushBack = true*/ ){
        String cleanedSearchPath = VaFileTools.CleanupPath( path + "\\", false );
//        if( pushBack )
            m_searchPaths.addLast(cleanedSearchPath);
//        else
//            m_searchPaths.addFirst(cleanedSearchPath);
    }

    public String             FindShaderFile(String fileName ){
//        for( int i = 0; i < m_searchPaths.size( ); i++ )
        for(String searchPath : m_searchPaths)
        {
            String filePath = searchPath + "\\" + fileName;
            if( VaFileTools.FileExists( filePath ) )
            {
                return VaFileTools.CleanupPath( filePath, false );
            }
            if( VaFileTools.FileExists( ( VaCore.GetWorkingDirectory( ) + filePath ) ) )
            {
                return VaFileTools.CleanupPath( VaCore.GetWorkingDirectory( ) + filePath, false );
            }
        }

        if( VaFileTools.FileExists( fileName ) )
        return VaFileTools.CleanupPath( fileName, false );

        if( VaFileTools.FileExists( ( VaCore.GetWorkingDirectory( ) + fileName ) ) )
        return VaFileTools.CleanupPath( VaCore.GetWorkingDirectory( ) + fileName, false );

        return "";
    }

    public void                RecompileFileLoadedShaders( ){
        VaDirectXShader.ReloadAllShaders( );
    }

    public boolean          Settings( )                                             { return m_settings; }
    //const Settings &    Settings( ) const                                       { return m_settings; }

    private String      GetCacheFileName( )  { return "shaders_debug_";}
    private void        LoadCache( String filePath ){
        String cacheDir = m_cacheFileDir;

        m_cacheLoaded = true;

        String fullFileName = cacheDir + filePath;

        if( fullFileName /*== L""*/.isEmpty() )
        {
            return;
        }

        if( !VaFileTools.FileExists( fullFileName ) )
        return;

        ClearCache( );

        /*vaFileStream inFile;  TODO
        if( inFile.Open( fullFileName.c_str( ), FileCreationMode::Open, FileAccessMode::Read ) )
        {
            int version = -1;
            inFile.ReadValue<int32>( version );

            assert( version == 0 );

            int32 entryCount = 0;
            inFile.ReadValue<int32>( entryCount );

            for( int i = 0; i < entryCount; i++ )
            {
                vaShaderCacheKey key;
                key.Load( inFile );
                vaShaderCacheEntry * entry = new vaShaderCacheEntry( inFile );

                m_cache.insert( std::pair<vaShaderCacheKey, vaShaderCacheEntry *>( key, entry ) );
            }

            int32 terminator;
            inFile.ReadValue<int32>( terminator );
            assert( terminator == 0xFF );
        }*/
        throw new UnsupportedOperationException();
    }
    private void                SaveCache( String filePath ){
        throw new UnsupportedOperationException();
    }

    List<VaDirectXShader> GetAllShaderList( )        { return m_allShaderList; }

    @Override
    public void dispose() {
        SaveCache( GetCacheFileName());
        ClearCache( );

        // Ensure no shaders remain
        assert( m_allShaderList.size( ) == 0 );
        g_Instance = null;
    }
}
