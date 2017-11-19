package jet.opengl.demos.intel.va;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderProgram;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/11/18.
 */

public abstract class VaDirectXShader implements VaDirectXNotifyTarget, Disposeable{
    protected ShaderProgram m_shader;

    protected String                     m_entryPoint;
    protected String                     m_shaderModel;

    protected String                     m_shaderFilePath;
    protected CharSequence               m_shaderCode;

    protected String                     m_disasm;
    protected boolean                    m_disasmAutoDumpToFile;

//    typedef std::vector<std::pair<std::string, std::string>> MacroContaner;

    protected static final int           c_maxMacros = 128;
    protected final List<Macro>                m_macros = new ArrayList<>();

    protected boolean                    m_lastLoadedFromCache;

    protected int			m_storageIndex;
    //
//    friend class vaDirectXCore;
    //
    protected boolean        m_helperMacroConstructorCalled;
    protected boolean        m_helperMacroDesctructorCalled;

    public VaDirectXShader( ){
        m_shader = null;

        m_entryPoint = "";
        m_shaderFilePath = "";
        m_shaderCode = "";
        m_shaderModel = "";

        // prevent asserts
        m_helperMacroConstructorCalled = true;
        m_helperMacroDesctructorCalled = true;

        m_lastLoadedFromCache = false;
        GetAllShadersList( ).add( this );

//#ifdef VA_HOLD_SHADER_DISASM
        m_disasmAutoDumpToFile = false;
    }

    @Override
    public void dispose() {
        DestroyShaderBase( );

        boolean found = false;
        List<VaDirectXShader> shaderList = GetAllShadersList();
        for( int i = 0; i < shaderList.size( ); i++ )
        {
            if( shaderList.get(i) == this )
            {
//                GetAllShadersList( ).erase( GetAllShadersList( ).begin( ) + i );
                shaderList.remove(i);
                found = true;
                break;
            }
        }
        assert( found );
    }

    //
    public void  CreateShaderFromFile(String filePath, String shaderModel, String entryPoint, /*D3D_SHADER_MACRO * macros = NULL*/Macro... macros ){
        DestroyShader( );
        m_shaderCode = "";
        m_shaderFilePath = filePath;
        m_entryPoint = entryPoint;
        m_shaderModel = shaderModel;
        StoreMacros( macros );
        AddBuiltInMacros( );
        CreateShader( );
    }

    public void  CreateShaderFromBuffer(CharSequence shaderCode, String shaderModel, String entryPoint, /*D3D_SHADER_MACRO * macros = NULL*/Macro... macros ){
        DestroyShader( );
        m_shaderCode = shaderCode;
        m_shaderFilePath = "";
        m_entryPoint = entryPoint;
        m_shaderModel = shaderModel;
        StoreMacros( macros );
        AddBuiltInMacros( );
        CreateShader( );
    }
    //
    public void  CreateShaderFromFile(String filePath, String shaderModel, String entryPoint, List<Macro> macros ){
        DestroyShader( );
        m_shaderCode = "";
        m_shaderFilePath = filePath;
        m_entryPoint = entryPoint;
        m_shaderModel = shaderModel;
        m_macros.clear();
        m_macros.addAll(macros);
        AddBuiltInMacros( );
        CreateShader( );
    }

    public void  CreateShaderFromBuffer( CharSequence shaderCode, String shaderModel, String entryPoint, List<Macro> macros ){
        DestroyShader( );
        m_shaderCode = shaderCode;
        m_shaderFilePath = "";
        m_entryPoint = entryPoint;
        m_shaderModel = shaderModel;
        m_macros.clear();
        m_macros.addAll(macros);
        AddBuiltInMacros( );
        CreateShader( );
    }

    //
    public void                            Clear( ){
        DestroyShader();

        m_shader = null;

        m_entryPoint = "";
        m_shaderFilePath = "";
        m_shaderCode = "";
        m_shaderModel = "";
//#ifdef VA_HOLD_SHADER_DISASM
        m_disasm = "";
    }
    //
    public  void                    Reload( )                                       { DestroyShader( ); CreateShader( ); }
    public  void                    Reload( Macro[] newMacroDefines ){
        StoreMacros( newMacroDefines );
        Reload( );
    }
    public  void                    Reload( List<Macro> macros ){
        m_macros.clear();
        m_macros.addAll(macros);
        Reload( );
    }
    //
    public abstract void            SetToD3DContext( /*ID3D11DeviceContext * context*/ ) /*= 0*/;
    //
    public boolean                  IsLoadedFromCache( )                       { return m_lastLoadedFromCache; }
    //
//#ifdef VA_HOLD_SHADER_DISASM
    public void                     SetShaderDisasmAutoDumpToFile( boolean enable )    { m_disasmAutoDumpToFile = enable; }
    public String                   GetShaderDisAsm( )                                 { return m_disasm; }
//#endif
    //
    protected static List<VaDirectXShader> GetAllShadersList( ){
        return VaDirectXShaderManager.GetInstance( ).GetAllShaderList();
    }
    //
    protected void                    CreateCacheKey( VaShaderCacheKey outKey ){
        // supposed to be empty!
        assert( outKey.StringPart.length() == 0 );
//        outKey.StringPart.clear( );

//        outKey.StringPart += vaStringTools::Format( "%d", (int)m_macros.size( ) ) + " ";
        StringBuilder key = new StringBuilder(m_macros.size()).append(' ');
        for( int i = 0; i < m_macros.size( ); i++ )
        {
            /*outKey.StringPart += m_macros[i].first + " ";
            outKey.StringPart += m_macros[i].second + " ";*/

            Macro macro = m_macros.get(i);
            key.append(macro.key).append(' ');
            key.append(macro.value).append(' ');
        }
        /*outKey.StringPart += m_shaderModel + " ";
        outKey.StringPart += m_entryPoint + " ";
        outKey.StringPart += vaStringTools::ToLower( vaStringTools::SimpleNarrow( m_shaderFilePath ) ) + " ";*/
        key.append(m_shaderModel).append(' ');
        key.append(m_entryPoint).append(' ');
        key.append(m_shaderFilePath.toLowerCase()).append(' ');

        outKey.StringPart = key.toString();
    }
    //
    protected void                            StoreMacros(Macro[] macroDefines ){
        m_macros.clear( );

        if( macroDefines == null )
            return;

        /*CONST D3D_SHADER_MACRO* pNext = macroDefines;
        while( pNext->Name != NULL && pNext->Definition != NULL )*/
        for(Macro macro : macroDefines)
        {
            if(macro != null) {
//                m_macros.push_back(pair < string, string > (pNext -> Name, pNext -> Definition) );
                m_macros.add(macro);
            }
//            pNext++;

            if( m_macros.size( ) > c_maxMacros - 1 )
            {
                assert( false ); // no more than c_maxMacros-1 supported, increase c_maxMacros
                break;
            }
        }
    }
    // returns NULL if macro count == 0
    protected final Macro[]        GetStoredMacros() { return GetStoredMacros(null);}
    protected Macro[]        GetStoredMacros( /*D3D_SHADER_MACRO * buffer, int bufferElementCount*/ Macro[] buffer ){
        if( m_macros.size( ) == 0 )
            return null;

        if(buffer == null)
            buffer = new Macro[m_macros.size( )];

        int bufferElementCount = buffer.length;
        if( bufferElementCount </*=*/ m_macros.size() )
        {
            // output buffer element too small!
            assert( false );
            return null;
        }

        /*int i;
        for( i = 0; i < m_macros.size( ); i++ )
        {
            buffer[i].Name = m_macros[i].first.c_str( );
            buffer[i].Definition = m_macros[i].second.c_str( );
        }
        buffer[i].Name = NULL;
        buffer[i].Definition = NULL;*/

        m_macros.toArray(buffer);
        return buffer;
    }
    //
    public static void                     ReloadAllShaders( ){
        LogUtil.i(LogUtil.LogType.DEFAULT, "Recompiling shaders...");

        final List<VaDirectXShader> shaderList = GetAllShadersList();
        final int totalLoaded = shaderList.size();
        int totalLoadedFromCache = 0;
        for( int i = 0; i < shaderList.size( ); i++ )
        {
            shaderList.get(i).Reload( );
            if( shaderList.get(i).IsLoadedFromCache( ) )
            totalLoadedFromCache++;
        }

        final int _totalLoadedFromCache = totalLoadedFromCache;
        LogUtil.i(LogUtil.LogType.DEFAULT, ()->String.format("... %d shaders reloaded (%d from cache)", totalLoaded, _totalLoadedFromCache));
    }
    //
    public void                    OnDeviceCreated( /*ID3D11Device* device, IDXGISwapChain* swapChain*/ ){
        DestroyShader( );
        CreateShader( );
    }

    public void                    OnDeviceDestroyed( ){
        DestroyShader( );
    }
    //
    protected void CreateShader(){
//        HRESULT hr;
        boolean[] loadedFromCache = new boolean[1];
        ID3DBlob shaderBlob = null;
        try {
            shaderBlob = CreateShaderBase( loadedFromCache );
        } catch (IOException e) {
            e.printStackTrace();
        }

        if( shaderBlob == null )
        {
            return;
        }
        m_lastLoadedFromCache = loadedFromCache[0];

        /*ID3D11PixelShader * shader = NULL;
        hr = vaDirectXCore::GetDevice( )->CreatePixelShader( shaderBlob->GetBufferPointer( ), shaderBlob->GetBufferSize( ), NULL, &shader );
        assert( SUCCEEDED( hr ) );
        if( SUCCEEDED( hr ) )
        {
            V( shader->QueryInterface( IID_IUnknown, (void**)&m_shader ) );
            shader->Release( );
            vaDirectXCore::NameObject( shader, "vaDirectXPixelShader shader object" );
        }

        SAFE_RELEASE( shaderBlob );*/
        throw new UnsupportedOperationException();
    }

    protected void                    DestroyShader( )            { DestroyShaderBase( ); }
    protected void                            DestroyShaderBase( ){
        m_lastLoadedFromCache = false;
        SAFE_RELEASE( m_shader );
    }

    protected static ID3DBlob CompileShaderFromFile(String szFileName, Macro[] pDefines, String szEntryPoint,
                                                String szShaderModel, /*byte[] ppBlobOut,*/ List<VaShaderCacheEntry.FileDependencyInfo> outDependencies ) throws IOException
    {
//        HRESULT hr = S_OK;


        /*DWORD dwShaderFlags = D3DCOMPILE_ENABLE_STRICTNESS;  TODO how to handle this case???
#if defined( DEBUG ) || defined( _DEBUG )
        // Set the D3DCOMPILE_DEBUG flag to embed debug information in the shaders.
        // Setting this flag improves the shader debugging experience, but still allows
        // the shaders to be optimized and to run exactly the way they will run in
        // the release configuration of this program.
        dwShaderFlags |= D3DCOMPILE_DEBUG;
#endif
        dwShaderFlags |= D3DCOMPILE_OPTIMIZATION_LEVEL3;

        if( vaDirectXShaderManager::GetInstance( ).Settings().WarningsAreErrors )
        dwShaderFlags |= D3DCOMPILE_WARNINGS_ARE_ERRORS;*/

        // find the file
        String fullFileName = VaDirectXShaderManager.GetInstance( ).FindShaderFile( szFileName );

        if( fullFileName /*!= ""*/.isEmpty() == false )
        {
            boolean attemptReload;

            do
            {
                outDependencies.clear( );
                outDependencies.add(new VaShaderCacheEntry.FileDependencyInfo( szFileName ) );
//                vaShaderIncludeHelper includeHelper( outDependencies );

                attemptReload = false;
                /*ID3DBlob* pErrorBlob;  TODO
                hr = D3DCompileFromFile( fullFileName.c_str( ), pDefines, (ID3DInclude*)&includeHelper,
                        szEntryPoint, szShaderModel, dwShaderFlags, 0, ppBlobOut, &pErrorBlob );
                if( FAILED( hr ) )
                {
                    if( pErrorBlob != NULL )
                    {
                        wstring absFileName = vaFileTools::GetAbsolutePath( fullFileName );

                        OutputDebugStringA( vaStringTools::Format( "\nError while compiling '%s' shader, SM: '%s', EntryPoint: '%s' :\n", vaStringTools::SimpleNarrow(absFileName).c_str(), szShaderModel, szEntryPoint ).c_str() );
                        OutputDebugStringA( CorrectErrorIfNotFullPath( (char*)pErrorBlob->GetBufferPointer( ) ).c_str() );
#if 1 || defined( _DEBUG ) // display always for now
                        wstring errorMsg = vaStringTools::SimpleWiden( (char*)pErrorBlob->GetBufferPointer( ) );
                        wstring shmStr = vaStringTools::SimpleWiden( szShaderModel );
                        wstring entryStr = vaStringTools::SimpleWiden( szEntryPoint );
                        wstring fullMessage = vaStringTools::Format( L"Error while compiling '%s' shader, SM: '%s', EntryPoint: '%s'."
                        L"\n\n---\n%s\n---\nAttempt reload?",
                            fullFileName.c_str( ), shmStr.c_str( ), entryStr.c_str( ), errorMsg.c_str( ) );
                        if( MessageBox( NULL, fullMessage.c_str( ), L"Shader compile error", MB_YESNO | MB_SETFOREGROUND ) == IDYES )
                            attemptReload = true;
#endif
                    }
                    SAFE_RELEASE( pErrorBlob );
                    if( !attemptReload )
                        return hr;
                }
                SAFE_RELEASE( pErrorBlob );*/


            } while( attemptReload );
        }
        else
        {
            // ...then try embedded storage
            VaFileTools.EmbeddedFileData embeddedData = VaFileTools.EmbeddedFilesFind( "shaders:\\" +szFileName );

            if( /*!embeddedData.HasContents( )*/ embeddedData == null )
            {
//                VA_ERROR( L"Error trying to find shader file '%s'!", szFileName );
                LogUtil.e(LogUtil.LogType.DEFAULT, "Error trying to find shader file " + szFileName);
                return /*E_FAIL*/ null;
            }

            VaShaderCacheEntry.FileDependencyInfo fileDependencyInfo = new VaShaderCacheEntry.FileDependencyInfo( szFileName, embeddedData.TimeStamp );

            outDependencies.clear( );
            outDependencies.add( fileDependencyInfo );
//            vaShaderIncludeHelper includeHelper( outDependencies );

            String ansiName = /*vaStringTools::SimpleNarrow*/( embeddedData.Name );
            /*ID3DBlob* pErrorBlob;
            hr = D3DCompile( (LPCSTR)embeddedData.MemStream->GetBuffer( ), (SIZE_T)embeddedData.MemStream->GetLength( ), ansiName.c_str( ), pDefines, (ID3DInclude*)&includeHelper,
                szEntryPoint, szShaderModel, dwShaderFlags, 0, ppBlobOut, &pErrorBlob );
            if( FAILED( hr ) )
            {
                if( pErrorBlob != NULL )
                {
                    OutputDebugStringA( CorrectErrorIfNotFullPath( (char*)pErrorBlob->GetBufferPointer( ) ).c_str() );
#if 1 || defined( _DEBUG ) // display always for now
                    wstring errorMsg = vaStringTools::SimpleWiden( (char*)pErrorBlob->GetBufferPointer( ) );
                    wstring shmStr = vaStringTools::SimpleWiden( szShaderModel );
                    wstring entryStr = vaStringTools::SimpleWiden( szEntryPoint );
                    wstring fullMessage = vaStringTools::Format( L"Error while compiling '%s' shader, SM: '%s', EntryPoint: '%s'."
                    L"\n\n---\n%s\n---",
                        fullFileName.c_str( ), shmStr.c_str( ), entryStr.c_str( ), errorMsg.c_str( ) );
                    MessageBox( NULL, fullMessage.c_str( ), L"Shader compile error", MB_OK | MB_SETFOREGROUND );
#endif
                }
                SAFE_RELEASE( pErrorBlob );
                return hr;
            }
            SAFE_RELEASE( pErrorBlob );*/
            throw new UnsupportedOperationException();
        }

        return null;
    }

    protected static ID3DBlob CompileShaderFromBuffer( CharSequence shaderCode, Macro[] pDefines, String szEntryPoint,
                                                               String szShaderModel/*, ID3DBlob** ppBlobOut*/ )
    {
//        HRESULT hr = S_OK;

        /*DWORD dwShaderFlags = D3D10_SHADER_ENABLE_STRICTNESS;
#if defined( DEBUG ) || defined( _DEBUG )
        // Set the D3D10_SHADER_DEBUG flag to embed debug information in the shaders.
        // Setting this flag improves the shader debugging experience, but still allows
        // the shaders to be optimized and to run exactly the way they will run in
        // the release configuration of this program.
        dwShaderFlags |= D3D10_SHADER_DEBUG;
#endif*/

        /*ID3DBlob* pErrorBlob;  TODO
        hr = D3DCompile( shaderCode.c_str( ), shaderCode.size( ) + 1, NULL, pDefines, NULL, szEntryPoint, szShaderModel,
                dwShaderFlags, 0, ppBlobOut, &pErrorBlob );
        if( FAILED( hr ) )
        {
            if( pErrorBlob != NULL )
            {
                OutputDebugStringA( CorrectErrorIfNotFullPath( (char*)pErrorBlob->GetBufferPointer( ) ).c_str() );
#if 1 || defined( _DEBUG ) // display always for now
                wstring errorMsg = vaStringTools::SimpleWiden( (char*)pErrorBlob->GetBufferPointer( ) );
                wstring fullMessage = vaStringTools::Format( L"Error while compiling '%s' shader, SM: '%s', EntryPoint: '%s'. \n\n---\n%s\n---",
                    L"<from_buffer>", szShaderModel, szEntryPoint, errorMsg.c_str( ) );
                MessageBox( NULL, fullMessage.c_str( ), L"Shader compile error", MB_OK | MB_SETFOREGROUND );
#endif
            }
            SAFE_RELEASE( pErrorBlob );
            return hr;
        }
        SAFE_RELEASE( pErrorBlob );
        return S_OK;*/

        throw new UnsupportedOperationException();
    }

    //
    protected ID3DBlob                  CreateShaderBase( boolean[] loadedFromCache ) throws IOException{
        ID3DBlob shaderBlob = null;
        loadedFromCache[0] = false;

        /*ID3D11Device * device = vaDirectXCore::GetDevice( );
        if( device == NULL )
        {
            return NULL;
        }*/
        if( ( m_shaderFilePath.length( ) == 0 ) && ( m_shaderCode.length( ) == 0 ) )
        {
            // still no path or code set provided
            return null;
        }

        if( m_shaderFilePath.length( ) != 0 )
        {
            VaShaderCacheKey cacheKey = new VaShaderCacheKey();
            CreateCacheKey( cacheKey );

//#ifdef USE_SHADER_CACHE_SYSTEM
            boolean[] foundButModified = new boolean[1];
            shaderBlob = VaDirectXShaderManager.GetInstance( ).FindInCache( cacheKey, foundButModified );

            if( shaderBlob == null )
            {
                /*wstring entryw = vaStringTools::SimpleWiden( m_entryPoint );
                wstring shadermodelw = vaStringTools::SimpleWiden( m_shaderModel );*/
                String entryw = m_entryPoint;
                String shadermodelw = m_shaderModel;

                if( foundButModified[0] )
//                    vaLog::GetInstance( ).Add( LOG_COLORS_SHADERS, L" > file '%s' for '%s', entry '%s', found in cache but modified; recompiling...", m_shaderFilePath.c_str(), shadermodelw.c_str(), entryw.c_str() );
                    LogUtil.i(LogUtil.LogType.DEFAULT, String.format(" > file '%s' for '%s', entry '%s', found in cache but modified; recompiling...", m_shaderFilePath, shadermodelw, entryw));
                else
//                vaLog::GetInstance( ).Add( LOG_COLORS_SHADERS, L" > file '%s' for '%s', entry '%s', not found in cache; recompiling...", m_shaderFilePath.c_str(), shadermodelw.c_str(), entryw.c_str() );
                    LogUtil.i(LogUtil.LogType.DEFAULT, String.format(" > file '%s' for '%s', entry '%s', not found in cache; recompiling...", m_shaderFilePath, shadermodelw, entryw));
            }
//#endif

            loadedFromCache[0] = shaderBlob != null;

            if( shaderBlob == null )
            {
//                vector<VaShaderCacheEntry.FileDependencyInfo> dependencies;
                ArrayList<VaShaderCacheEntry.FileDependencyInfo> dependencies = new ArrayList<>();

//                D3D_SHADER_MACRO macrosBuffer[ c_maxMacros ];

                shaderBlob = CompileShaderFromFile( m_shaderFilePath, GetStoredMacros( /*macrosBuffer, c_maxMacros*/ ), m_entryPoint, m_shaderModel,dependencies );

                if( shaderBlob != null )
                {
                    VaDirectXShaderManager.GetInstance( ).AddToCache( cacheKey, shaderBlob, dependencies );
                }
            }
        }
        else if( m_shaderCode.length( ) != 0 )
        {
//            D3D_SHADER_MACRO macrosBuffer[c_maxMacros];
            shaderBlob = CompileShaderFromBuffer( m_shaderCode, GetStoredMacros( /*macrosBuffer, c_maxMacros*/ ), m_entryPoint, m_shaderModel );
        }
        else
        {
            assert( false );
        }


//#ifdef VA_HOLD_SHADER_DISASM
        /*{
            ID3DBlob * disasmBlob = NULL;

            HRESULT hr = D3DDisassemble( shaderBlob->GetBufferPointer(), shaderBlob->GetBufferSize(), 0, nullptr, &disasmBlob );
            assert( SUCCEEDED( hr ) );

            m_disasm = "";
            if( (disasmBlob != nullptr) && (disasmBlob->GetBufferPointer() != nullptr) )
            {
                m_disasm = (const char *)disasmBlob->GetBufferPointer();

                if( m_disasmAutoDumpToFile )
                {
                    wstring fileName = vaCore::GetWorkingDirectory( );

                    vaShaderCacheKey cacheKey;
                    CreateCacheKey( cacheKey );
                    vaCRC64 crc;
                    crc.AddString( cacheKey.StringPart );

                    fileName += L"shaderdump_" + vaStringTools::SimpleWiden( m_entryPoint ) + L"_" + vaStringTools::SimpleWiden( m_shaderModel ) *//*+ L"_" + vaStringTools::SimpleWiden( vaStringTools::Format( "0x%" PRIx64, crc.GetCurrent() ) )*//* + L".shaderasm";

                    vaStringTools::WriteTextFile( fileName, m_disasm );
                }

            }

            SAFE_RELEASE( disasmBlob );
        }*/
//#endif

        return shaderBlob;
    }
    //
    protected void                    AddBuiltInMacros( ){
        m_macros.add( new Macro( "VA_COMPILED_AS_SHADER_CODE", "" ) );
    }
}
