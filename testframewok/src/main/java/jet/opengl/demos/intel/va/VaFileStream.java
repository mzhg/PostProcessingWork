package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Writable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.util.BufferUtils;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by Administrator on 2017/11/21 0021.
 */

public class VaFileStream extends VaStream {
    private static final int CACHE_SIZE = 1024;

//    private Path m_file;
    private FileAccessMode m_accessMode;
    private SeekableByteChannel m_file;
    private ByteBuffer m_CacheBuffer;

    public VaFileStream(){
        m_file = null;
        m_accessMode = FileAccessMode.Default;
    }

    // Specifies how the operating system should open a file.
    public enum FileCreationMode
    {
        /** Create a new file. If the file already exists, the call will fail. */
        CreateNew            /*= 1*/,

        // Create a new file. If the file already exists, it will be overwritten and truncated to 0 size.
        Create               /*= 2*/,

        // Specifies that the operating system should open an existing file. If the file
        // doesn't exist the call will fail.
        Open                 /*= 3*/,

        // Open a file if it exists; otherwise, a new file will be created.
        OpenOrCreate         /*= 4*/,

        // Open existing file, truncate it's size to 0.
        Truncate             /*= 5*/,

        // Opens the file if it exists and seeks to the end of the file, or creates a new file.
        Append               /*= 6*/,
    };

    public enum FileAccessMode
    {
        /** Chose the access mode automatically based on creation mode */
        Default              /*= -1*/,

        // Self-explanatory
        Read                 /*= 1*/,

        // Self-explanatory
        Write                /*= 2*/,

        // Self-explanatory
        ReadWrite            /*= 3*/,
    };

    public enum FileShareMode
    {
        // Chose the mode automatically based on creation mode
        Default              /*= -1*/,

        // Don't share.
        // Any request to open the file (by this process or another process) will fail until the
        // file is closed.
        None                 /*= 0*/,

        // Share only for read. Subsequent opening of the file for reading will be allowed.
        Read                 /*= 1*/,

        // Share only for write. Allows subsequent opening of the file for writing.
        Write                /*= 2*/,

        // Share for read and write. Subsequent opening of the file for reading or writing will
        // be allowed.
        ReadWrite            /*= 3*/,

        // Share for delete. Subsequent deleting of a file will be allowed.
        Delete               /*= 4*/,
    };
    @Override
    public boolean IsOpen() {
        return  m_file != null;
    }

    public boolean Open( String file_path, FileCreationMode creationMode) throws IOException{
        return Open(file_path, creationMode, FileAccessMode.Default);
    }

    public boolean Open( String file_path, FileCreationMode creationMode, FileAccessMode accessMode ) throws IOException{
        file_path = FileUtils.g_IntenalFileLoader.resolvePath(file_path);

        if( IsOpen( ) ) return false;

        if(creationMode == FileCreationMode.Open){
            if( !VaFileTools.FileExists( file_path ) )
                return false;
        }

        StandardOpenOption dwCreationDisposition;
        if(creationMode == FileCreationMode.CreateNew){
            if( VaFileTools.FileExists( file_path ) )
            return false;
            creationMode = FileCreationMode.Create;
        }

        if( creationMode == FileCreationMode.Create )
        {
            if( VaFileTools.FileExists( file_path ) )
            creationMode = FileCreationMode.Truncate;
        }

        if( ( accessMode == FileAccessMode.Read ) && ( creationMode == FileCreationMode.Create || creationMode == FileCreationMode.OpenOrCreate ||
                creationMode == FileCreationMode.Truncate || creationMode == FileCreationMode.Append ) )
        {
            LogUtil.e(LogUtil.LogType.DEFAULT, "vaFileStream::Open - access mode and creation mode mismatch");
            return false;
        }

        if( creationMode == FileCreationMode.OpenOrCreate )
        {
            if( VaFileTools.FileExists( file_path ) )
            {
                creationMode = FileCreationMode.Open;
                if( accessMode == FileAccessMode.Default )   accessMode = FileAccessMode.ReadWrite;
            }
            else
            {
                creationMode = FileCreationMode.Create;
            }
        }

        if( ( creationMode == FileCreationMode.Open ) && ( accessMode == FileAccessMode.Default ) )
            accessMode = FileAccessMode.Read;

        switch( creationMode )
        {
            case Create : dwCreationDisposition = StandardOpenOption.CREATE;      break;
            case Open   : dwCreationDisposition = StandardOpenOption.READ;      break;
            case Append  : dwCreationDisposition = StandardOpenOption.APPEND;        break;
            case Truncate  : dwCreationDisposition = StandardOpenOption.TRUNCATE_EXISTING;  break;
            default: LogUtil.e(LogUtil.LogType.DEFAULT, "Incorrect creationMode parameter"); ;  return false;
        }

        Set<StandardOpenOption> options = new HashSet<>();
        options.add(dwCreationDisposition);

        /*DWORD dwDesiredAccess = 0;  TODO
        dwDesiredAccess |= ( ( accessMode & FileAccessMode::Read ) != 0 ) ? ( GENERIC_READ ) : ( 0 );
        dwDesiredAccess |= ( ( accessMode & FileAccessMode::Write ) != 0 ) ? ( GENERIC_WRITE ) : ( 0 );

        DWORD dwShareMode = 0;
        dwShareMode |= ( ( shareMode & FileShareMode::Read ) != 0 ) ? ( FILE_SHARE_READ ) : ( 0 );
        dwShareMode |= ( ( shareMode & FileShareMode::Write ) != 0 ) ? ( FILE_SHARE_WRITE ) : ( 0 );
        dwShareMode |= ( ( shareMode & FileShareMode::Delete ) != 0 ) ? ( FILE_SHARE_DELETE ) : ( 0 );


        m_file = ::CreateFileW( file_path, dwDesiredAccess, dwShareMode, NULL, dwCreationDisposition, FILE_ATTRIBUTE_NORMAL, NULL );

        if( m_file == INVALID_HANDLE_VALUE )
        {
            wstring errorStr = GetLastErrorAsStringW( );
            VA_LOG( L"vaFileStream::Open( ""%s"", ... ): %s", file_path, errorStr.c_str( ) );
            return false;
        }*/

        if(accessMode == FileAccessMode.Read || accessMode== FileAccessMode.Default){
            options.add(StandardOpenOption.READ);
        }else if(accessMode == FileAccessMode.Write){
            options.add(StandardOpenOption.WRITE);
        }else { // read and write
            options.add(StandardOpenOption.READ);
            options.add(StandardOpenOption.WRITE);
        }

        m_file = Files.newByteChannel(Paths.get(file_path), options.toArray(new StandardOpenOption[options.size()]));
        m_accessMode = accessMode;
        m_CacheBuffer = BufferUtils.createByteBuffer(CACHE_SIZE);  // 1Kb

        if( creationMode == FileCreationMode.Append )
        {
            Seek( GetLength( ) );
        }

        return true;
    }

    @Override
    public int GetLength() throws IOException{
        return (int) m_file.size();
    }

    @Override
    public int GetPosition()  throws IOException{
        return (int)m_file.position();
    }

    @Override
    public boolean CanSeek() {
        return true;
    }

    @Override
    public void Seek(int position) throws IOException {
        assert( position >= 0 );
        m_file.position(position);
    }

    @Override
    public void Truncate() throws IOException{
        if(GLCheck.CHECK){
            if(m_accessMode != FileAccessMode.Write && m_accessMode != FileAccessMode.ReadWrite){
                throw new IOException("File not opened for writing");
            }
        }

        m_file.truncate(0);  // TODO
    }

    @Override
    public int Read(byte[] buffer, int offset, int count) throws IOException {
        /*assert ( count > 0, L"count parameter must be > 0" );
        assert( ( m_accessMode & FileAccessMode::Read ) != 0, L"File not opened for reading" );
        assert( count < INT_MAX, L"File system current doesn't support reads bigger than INT_MAX" );*/
        if(GLCheck.CHECK){
            if(count <=0)
                throw new IllegalArgumentException("count parameter must be > 0");

            if(m_accessMode != FileAccessMode.Read && m_accessMode != FileAccessMode.ReadWrite){
                throw new IOException("File not opened for reading");
            }
        }

        int oldOffset = offset;
        int remainLength = count;
        while (remainLength > 0){
            m_CacheBuffer.position(0).limit(remainLength > CACHE_SIZE ? CACHE_SIZE : remainLength);
            int readCount = m_file.read(m_CacheBuffer);
            if(readCount == -1){
                break;
            }

            m_CacheBuffer.flip();
            m_CacheBuffer.get(buffer, offset, readCount);

            offset += readCount;
            remainLength -= readCount;
        }

        /*DWORD dwRead;
        if( !::ReadFile( m_file, buffer, (DWORD)count, &dwRead, NULL ) )
        return false;

        if( outCountRead == NULL )
        {
            return count == (int)dwRead;
        }
        else
        {
        *outCountRead = dwRead;
            return dwRead > 0;
        }
        return 0;*/
        int dwRead = offset - oldOffset;
        return  dwRead== 0 ? -1 : dwRead;
    }

    @Override
    public int Write(byte[] buffer, int offset, int count) throws IOException {
        if(GLCheck.CHECK){
            if(count <=0)
                throw new IllegalArgumentException("count parameter must be > 0");

            if(m_accessMode != FileAccessMode.Write && m_accessMode != FileAccessMode.ReadWrite){
                throw new IOException("File not opened for writing");
            }
        }

        int oldOffset = offset;
        int remainLength = count;
        while (remainLength > 0){
            m_CacheBuffer.position(0).limit(CACHE_SIZE);
            m_CacheBuffer.put(buffer, offset, remainLength > CACHE_SIZE ? CACHE_SIZE : remainLength).flip();
            int writtenCount = m_file.write(m_CacheBuffer);
            if(writtenCount == 0){
                break;
            }

            offset += writtenCount;
            remainLength -= writtenCount;
        }

        int dwWrite = offset - oldOffset;
        return  dwWrite== 0 ? -1 : dwWrite;
    }

    @Override
    public long ReadLong() throws IOException {
        m_CacheBuffer.position(0).limit(8);
        int count = m_file.read(m_CacheBuffer);
        if(count != 8){
            throw new IOException();
        }
        return m_CacheBuffer.getLong(0);
    }

    @Override
    public byte Read() throws IOException {
        m_CacheBuffer.position(0).limit(1);
        m_file.read(m_CacheBuffer);
        return m_CacheBuffer.get(0);
    }

    @Override
    public short ReadShort() throws IOException {
        m_CacheBuffer.position(0).limit(2);
        m_file.read(m_CacheBuffer);
        return m_CacheBuffer.getShort(0);
    }

    @Override
    public int ReadInt() throws IOException {
        m_CacheBuffer.position(0).limit(4);
        m_file.read(m_CacheBuffer);
        return m_CacheBuffer.getInt(0);
    }

    @Override
    public float ReadFloat() throws IOException {
        m_CacheBuffer.position(0).limit(4);
        m_file.read(m_CacheBuffer);
        return m_CacheBuffer.getFloat(0);
    }

    @Override
    public void ReadObject(int size, Writable obj) throws IOException {
        ByteBuffer buffer;
        if(CACHE_SIZE >= size){
            buffer = m_CacheBuffer;
        }else{
            buffer = BufferUtils.createByteBuffer(size);
        }

        buffer.position(0).limit(size);
        m_file.read(buffer);
        obj.load(buffer);
    }

    @Override
    public boolean Write(long value) throws IOException {
        m_CacheBuffer.position(0);
        m_CacheBuffer.putLong(value).flip();
        return m_file.write(m_CacheBuffer) != 0;
    }

    @Override
    public boolean Write(byte value) throws IOException {
        m_CacheBuffer.position(0);
        m_CacheBuffer.put(value).flip();
        return m_file.write(m_CacheBuffer) != 0;
    }

    @Override
    public boolean Write(short value) throws IOException {
        m_CacheBuffer.position(0);
        m_CacheBuffer.putShort(value).flip();
        return m_file.write(m_CacheBuffer) != 0;
    }

    @Override
    public boolean Write(int value) throws IOException {
        m_CacheBuffer.position(0);
        m_CacheBuffer.putInt(value).flip();
        return m_file.write(m_CacheBuffer) != 0;
    }

    @Override
    public boolean Write(float value) throws IOException {
        m_CacheBuffer.position(0);
        m_CacheBuffer.putFloat(value).flip();
        return m_file.write(m_CacheBuffer) != 0;
    }

    @Override
    public boolean Write(int size, Readable obj) throws IOException {
        ByteBuffer buffer;
        if(CACHE_SIZE >= size){
            buffer = m_CacheBuffer;
        }else{
            buffer = BufferUtils.createByteBuffer(size);
        }

        obj.store(buffer).flip();
        return m_file.write(buffer) != 0;
    }

    @Override
    public void close() throws IOException {
        if(m_file != null)
            m_file.close();
        m_CacheBuffer = null;
        m_accessMode =FileAccessMode.Default;
        m_file = null;
    }
}
