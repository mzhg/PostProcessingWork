package jet.opengl.demos.intel.va;

import jet.opengl.postprocessing.util.FileUtils;

/**
 * Created by mazhen'gui on 2017/11/17.
 */

public class VaFileTools {

    public final class EmbeddedFileData{
        public String                             Name = null;
        public VaMemoryStream     MemStream;
//        int64                               TimeStamp;

        public EmbeddedFileData() /*: Name(L""), MemStream(NULL), TimeStamp(0)*/  { }
        public EmbeddedFileData( String name, VaMemoryStream memStream/*, const int64 & timeStamp*/ )/*
                                                : Name(name), MemStream(memStream), TimeStamp(timeStamp) */ {
                                                    Name = name;
                                                    MemStream = memStream;
        }

        public  boolean                                HasContents( )                      { return MemStream != null; }


    }

    public static void EmbeddedFilesRegister( String _pathName, byte[] data, int dataSize/*, int64 timeStamp*/ )
    {
        // case insensitive
        String pathName = _pathName.toLowerCase();

        /*std::map<wstring, EmbeddedFileData>::iterator it = s_EmbeddedFiles.find( pathName );

        if( it != s_EmbeddedFiles.end() )
        {
            VA_WARN( L"Embedded file %s already registered!", pathName.c_str() )
            return;
        }

        s_EmbeddedFiles.insert( std::pair<wstring, EmbeddedFileData>( pathName,
                EmbeddedFileData( pathName, std::shared_ptr<vaMemoryStream>( new vaMemoryStream( data, dataSize ) ), timeStamp ) ) );*/

        throw new UnsupportedOperationException();
    }

    public static String CleanupPath( String inputPath, boolean convertToLowercase )
    {
        String ret = inputPath;
        if( convertToLowercase )
            ret = /*vaStringTools::ToLower( ret )*/ret.toLowerCase();

        /*int foundPos;
        while( (foundPos = ret.indexOf("/")) != -1 ) {
            ret.replace(foundPos, 1, "\\");
        }

        while( (foundPos = ret.find(L"\\\\")) != wstring::npos )
            ret.replace( foundPos, 2, L"\\" );*/

        ret = ret.replaceAll("/", "\\");
        ret = ret.replaceAll("\\\\", "\\");

        // restore network path
        if( (ret.length() > 0) && (ret.charAt(0) == '\\') )
            ret = "\\"+ret;

        return ret;
    }

    public static  boolean FileExists(String filename){
        return FileUtils.g_IntenalFileLoader.exists(filename);
    }
}
