package jet.opengl.demos.intel.va;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/11/18.
 */

public class VaShaderCacheEntry {

    private ID3DBlob  m_compiledShader;
    private final List<FileDependencyInfo> m_dependencies;

    public VaShaderCacheEntry(ID3DBlob  compiledShader, List<FileDependencyInfo> dependencies ){
        m_compiledShader = compiledShader;
//        m_compiledShader->AddRef( );

        m_dependencies = dependencies;
    }
    public VaShaderCacheEntry( VaStream inFile )   { m_compiledShader = null; m_dependencies = Collections.emptyList(); Load( inFile ); }
//        ~vaShaderCacheEntry( );

    public boolean                       IsModified( ){
//        for( std::vector<vaShaderCacheEntry::FileDependencyInfo>::iterator it = m_dependencies.begin( ); it != m_dependencies.end( ); it++ )
        for(FileDependencyInfo it : m_dependencies)
        {
            if( ( it ).IsModified( ) )
            return true;
        }
        return false;
    }
    public ID3DBlob GetCompiledShader( )                       { /*m_compiledShader->AddRef( );*/ return m_compiledShader; }

    public void                       Save( VaStream outStream ) { throw  new UnsupportedOperationException();}
    public void                       Load( VaStream inStream ) { throw  new UnsupportedOperationException();}

    public static final class FileDependencyInfo
    {
        public String            FilePath;
        public long              ModifiedTimeDate;

        FileDependencyInfo( ) /*: FilePath( L"" ), ModifiedTimeDate( 0 )*/    { }
        FileDependencyInfo( String filePath ){
            String fullFileName = VaDirectXShaderManager.GetInstance( ).FindShaderFile( filePath );

            if( fullFileName /*== L""*/.isEmpty() )
            {
//                VA_ERROR( L"Error trying to find shader file '%s'!", filePath );
                LogUtil.e(LogUtil.LogType.DEFAULT, "Error trying to find shader file " + filePath);

                assert( false );
                this.FilePath = "";
                this.ModifiedTimeDate = 0;
            }
        else
            {
                this.FilePath = filePath;

                // struct _stat64 fileInfo;
                // _wstat64( fullFileName.c_str( ), &fileInfo ); // check error code?
                //this->ModifiedTimeDate = fileInfo.st_mtime;

                // maybe add some CRC64 here too? that would require reading contents of every file and every dependency which would be costly!
                /*WIN32_FILE_ATTRIBUTE_DATA attrInfo;
            ::GetFileAttributesEx( fullFileName.c_str( ), GetFileExInfoStandard, &attrInfo );
                this->ModifiedTimeDate = (((int64)attrInfo.ftLastWriteTime.dwHighDateTime) << 32) | ((int64)attrInfo.ftLastWriteTime.dwLowDateTime);*/
                this.ModifiedTimeDate = /*new File(filePath).lastModified() TODO Not safe*/ 0;
            }
        }

        FileDependencyInfo(String filePath, long modifiedTimeDate ){
            this.FilePath = filePath;
            this.ModifiedTimeDate = modifiedTimeDate;
        }
        FileDependencyInfo( VaStream inStream )                      { Load( inStream ); }

        public boolean                    IsModified( ){
            String fullFileName = VaDirectXShaderManager.GetInstance( ).FindShaderFile( this.FilePath );

            //vaLog::GetInstance( ).Add( LOG_COLORS_SHADERS, (L"vaShaderCacheEntry::FileDependencyInfo::IsModified, file name %s", fullFileName.c_str() ) );

            if( fullFileName /*== L""*/.isEmpty() )  // Can't find the file?
            {
                VaFileTools.EmbeddedFileData embeddedData = VaFileTools.EmbeddedFilesFind("shaders:\\" + this.FilePath );

                if( !embeddedData.HasContents( ) )
                {
                    //vaLog::GetInstance( ).Add( LOG_COLORS_SHADERS, L"        > embedded data has NO contents" );
//                    VA_ERROR( L"Error trying to find shader file '%s'!", this->FilePath.c_str( ) );
                    LogUtil.e(LogUtil.LogType.DEFAULT, "Error trying to find shader file " + FilePath);
                    return true;
                }
                else
                {
                    //vaLog::GetInstance( ).Add( LOG_COLORS_SHADERS,  L"        > embedded data has contents" );
                    return this.ModifiedTimeDate != embeddedData.TimeStamp;
                }
            }

            // struct _stat64 fileInfo;
            // _wstat64( fullFileName.c_str( ), &fileInfo ); // check error code?
            // return this->ModifiedTimeDate != fileInfo.st_mtime;

            // maybe add some CRC64 here too? that would require reading contents of every file and every dependency which would be costly!
            /*WIN32_FILE_ATTRIBUTE_DATA attrInfo;
        ::GetFileAttributesEx( fullFileName.c_str( ), GetFileExInfoStandard, &attrInfo );
            bool ret = this->ModifiedTimeDate != (( ( (int64)attrInfo.ftLastWriteTime.dwHighDateTime ) << 32 ) | ( (int64)attrInfo.ftLastWriteTime.dwLowDateTime ));*/
            boolean ret = /*ModifiedTimeDate != ??? TODO */ false;

            // if( ret )
            // {
            //     vaLog::GetInstance( ).Add( LOG_COLORS_SHADERS,  (const wchar_t*)((ret)?(L"   > shader file '%s' modified "):(L"   > shader file '%s' NOT modified ")), fullFileName.c_str() );
            // }

            return ret;
        }

        void                    Save( VaStream outStream ) { throw  new UnsupportedOperationException();}

        void                    Load( VaStream inStream ) { throw  new UnsupportedOperationException();}
    }
}
