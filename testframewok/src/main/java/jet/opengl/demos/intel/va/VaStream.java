package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Writable;

import java.io.Closeable;
import java.io.IOException;

import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StackByte;
import jet.opengl.postprocessing.util.StackFloat;
import jet.opengl.postprocessing.util.StackInt;

/**
 * Created by mazhen'gui on 2017/11/16.
 */

public abstract class VaStream implements Closeable{

    public abstract boolean      IsOpen( );
    public abstract int          GetLength( ) throws IOException;
    public abstract int          GetPosition( ) throws IOException;
    public abstract boolean      CanSeek( );
    public abstract void         Seek( int position ) throws IOException;
    public abstract void         Truncate( )throws IOException;     // truncate everything behind current

    public abstract int          Read( byte[] buffer, int offset, int count /*int64 * outCountRead = NULL*/ ) throws IOException;
    public abstract int          Write(byte[] buffer, int offset, int count/*, int64 * outCountWritten = NULL*/ ) throws IOException;

    public abstract long         ReadLong() throws IOException;
    public abstract byte         Read() throws IOException;
    public abstract short        ReadShort() throws IOException;
    public abstract int          ReadInt() throws IOException;
    public abstract float        ReadFloat() throws IOException;
    public abstract void         ReadObject(int size, Writable obj) throws IOException;

    public abstract boolean      WriteLong(long value) throws IOException;
    public abstract boolean      Write(byte value) throws IOException;
    public abstract boolean      WriteShort(short value) throws IOException;
    public abstract boolean      WriteInt(int value) throws IOException;
    public abstract boolean      WriteFloat(float value) throws IOException;
    public abstract boolean      WriteObject(int size, Readable obj) throws IOException;

    // these use internal binary representation prefixed with size
    public boolean WriteString( String str )throws IOException{
        int lengthInBytes = str.length( );
        if( !/*WriteValue<uint32>*/ WriteInt( lengthInBytes ) )      // 32nd bit flags it as unicode (utf16) for verification when reading
            return false;
        assert( lengthInBytes < ( 1 << 31 ) );
        if( lengthInBytes == 0 )
            return true;
        else {
            byte[] buffer = new byte[lengthInBytes];
            for(int i = 0; i < str.length(); i++){
                buffer[i] = (byte) str.charAt(i);
            }

            return Write(buffer, 0, lengthInBytes) > 0;
        }
    }
    public boolean WriteStringW( String str )throws IOException{
        int lengthInBytes = str.length( ) * 2;
        if( !/*WriteValue<uint32>*/ WriteInt( lengthInBytes | ( 1 << 31 ) ) )      // 32nd bit flags it as unicode (utf16) for verification when reading
            return false;
        assert( lengthInBytes < ( 1 << 31 ) );
        if( lengthInBytes == 0 )
            return true;
        else {
            byte[] buffer = new byte[lengthInBytes];
            for(int i = 0; i < str.length(); i++){
                Numeric.getBytes((short)str.charAt(i), buffer, i<<1);
            }
            return Write(buffer, 0, lengthInBytes) > 0;
        }
    }

    public String ReadString( )throws IOException{
        int lengthInBytes;
//        if( !ReadValue<uint32>( lengthInBytes ) )
        if((lengthInBytes = ReadInt()) == -1)
            return null;
        assert( ( lengthInBytes & ( 1 << 31 ) ) == 0 );                // not reading an ansi string?

        // Empty string?
        if( lengthInBytes == 0 )
        {
//            outStr = "";
            return "";
        }

        byte[] buffer = new byte[lengthInBytes];          // TODO: remove dynamic alloc for smaller (most) reads - just use stack memory
//        if( !Read( buffer, lengthInBytes ) )
        int readBytes;
        if( (readBytes = Read( buffer,0, lengthInBytes )) != lengthInBytes )
        {
//            delete[] buffer;
            return null;
        }
        /*buffer[lengthInBytes] = 0;

        outStr = std::string( buffer, lengthInBytes );
        delete[] buffer;*/
        return new String(buffer);
    }

    public String ReadStringW( )throws IOException{
        int lengthInBytes;
//        if( !ReadValue<uint32>( lengthInBytes ) )
        if((lengthInBytes = ReadInt()) == -1)
            return null;

        assert( ( lengthInBytes & ( 1 << 31 ) ) != 0 );                // not reading a unicode string?
        lengthInBytes &= ~( 1 << 31 );
        assert( lengthInBytes % 2 == 0 );                           // must be an even number!

        // Empty string?
        if( lengthInBytes == 0 )
        {
//            outStr = L"";
            return "";
        }

        byte[] buffer = new byte[lengthInBytes];          // TODO: remove dynamic alloc for smaller (most) reads - just use stack memory
        int readBytes;
        if( (readBytes = Read( buffer,0, lengthInBytes )) != lengthInBytes )
        {
//            delete[] buffer;
            return null;
        }
//        buffer[lengthInBytes / 2] = 0;

//        outStr = std::wstring( buffer, lengthInBytes / 2 );
//        delete[] buffer;
        return new String(buffer);  // TODO
    }

    // these are supposed to just read a text file but I haven't sorted out any line ending or encoding conversions
    public final String ReadTXTW(  ) throws IOException{ return ReadTXTW(-1);}
    public final String ReadTXT(  )  throws IOException{ return ReadTXT(-1);}
    public String ReadTXTW( int count /*= -1*/ )throws IOException{
        int remainingSize = GetLength( ) - GetPosition( );
        if( count == -1 )
            count = remainingSize;
        else
            count = Math.min( count, remainingSize );

        // not tested unicode .txt file reading - there's a header at the top I think? need to implement that, sorry.
        assert( false );

        // Empty string?
        if( count == 0 )
        {
//            outStr = L"";
            return "";
        }

        byte[] buffer = new byte[count];          // TODO: remove dynamic alloc for smaller (most) reads - just use stack memory
        int readBytes;
        if( (readBytes = Read( buffer,0, count )) != count )
        {
//            delete[] buffer;
            return null;
        }
        /*buffer[count / 2] = 0;
        outStr = std::wstring( buffer, count / 2 );
        delete[] buffer;
        return true;*/
        return new String(buffer);
    }

    public String ReadTXT( int count /*= -1*/ )throws IOException{
        int remainingSize = GetLength( ) - GetPosition( );
        if( count == -1 )
            count = remainingSize;
        else
            count = Math.min( count, remainingSize );

        // not tested unicode .txt file reading - there's a header at the top I think? need to implement that, sorry.
        assert( false );

        // Empty string?
        if( count == 0 )
        {
//            outStr = L"";
            return "";
        }

        byte[] buffer = new byte[count];          // TODO: remove dynamic alloc for smaller (most) reads - just use stack memory
        int readBytes;
        if( (readBytes = Read( buffer,0, count )) != count )
        {
//            delete[] buffer;
            return null;
        }
        /*buffer[count / 2] = 0;
        outStr = std::wstring( buffer, count / 2 );
        delete[] buffer;
        return true;*/
        return new String(buffer);
    }

    public boolean WriteTXTW( String str )throws IOException{
        int lengthInBytes = str.length( ) * 2;
        assert( lengthInBytes < ( 1 << 31 ) );
        if( lengthInBytes == 0 )
            return true;
        else {
            byte[] buffer = new byte[lengthInBytes];
            for(int i = 0; i < str.length(); i++){
                Numeric.getBytes((short)str.charAt(i), buffer, i<<1);
            }
            return Write(buffer,0, lengthInBytes) > 0;
        }
    }

    public boolean WriteTXT( String str )throws IOException{
        int lengthInBytes = str.length( );
        assert( lengthInBytes < ( 1 << 31 ) );
        if( lengthInBytes == 0 )
            return true;
        else {
            byte[] buffer = new byte[lengthInBytes];
            for(int i = 0; i < str.length(); i++){
                buffer[i] = (byte) str.charAt(i);
            }
            return Write(buffer, 0, lengthInBytes) > 0;
        }
    }

    public boolean ReadVector( StackInt elements ) throws IOException {
        assert( elements.size( ) == 0 ); // must be empty at the moment

        /*int count;
        if( !ReadValue<int>( count, -1 ) )
        return false;*/
        int count = ReadInt();
        assert( count >= 0 ); if( count < 0 ) return false;
        if( count == 0 ) return true;

        elements.resize( count );

        int[] content = elements.getData();
        for(int i = 0; i < count; i++){
            content[i] = ReadInt();
        }

        return true;
    }

    public boolean WriteVector( StackInt elements ) throws IOException {
        assert( elements.size( ) < Integer.MAX_VALUE ); // not supported; to add support for 64bit size use most significant bit (sign) to indicate that the size is >= INT_MAX; this is backwards compatible and will not unnecessarily increase file size

        boolean ret = WriteInt(elements.size()); // /*WriteValue<int>( (int)elements.size( ) )*/;
        assert( ret ); if( !ret ) return false;

        for( int i = 0; i < elements.size( ); i++ )
        {
            ret = WriteInt(elements.get(i) );
            assert( ret ); if( !ret ) return false;
        }

        return true;
    }

    public boolean ReadVector( StackFloat elements ) throws IOException {
        assert( elements.size( ) == 0 ); // must be empty at the moment

        /*int count;
        if( !ReadValue<int>( count, -1 ) )
        return false;*/
        int count = ReadInt();
        assert( count >= 0 ); if( count < 0 ) return false;
        if( count == 0 ) return true;

        elements.resize( count );

        float[] content = elements.getData();
        for(int i = 0; i < count; i++){
            content[i] = ReadFloat();
        }

        return true;
    }

    public boolean WriteVector( StackFloat elements ) throws IOException {
        assert( elements.size( ) < Integer.MAX_VALUE ); // not supported; to add support for 64bit size use most significant bit (sign) to indicate that the size is >= INT_MAX; this is backwards compatible and will not unnecessarily increase file size

        boolean ret = WriteInt(elements.size()); // /*WriteValue<int>( (int)elements.size( ) )*/;
        assert( ret ); if( !ret ) return false;

        for( int i = 0; i < elements.size( ); i++ )
        {
            ret = WriteFloat(elements.get(i) );
            assert( ret ); if( !ret ) return false;
        }

        return true;
    }

    public boolean ReadVector( StackByte elements ) throws IOException {
        assert( elements.size( ) == 0 ); // must be empty at the moment
        if(true)
            throw new Error("Inner Error!!!");

        /*int count;
        if( !ReadValue<int>( count, -1 ) )
        return false;*/
        int count = ReadInt();
        assert( count >= 0 ); if( count < 0 ) return false;
        if( count == 0 ) return true;

        elements.resize( count );

        byte[] content = elements.getData();
        for(int i = 0; i < count; i++){
            content[i] = Read();
        }

        return true;
    }

    public boolean WriteVector( StackByte elements ) throws IOException {
        assert( elements.size( ) < Integer.MAX_VALUE ); // not supported; to add support for 64bit size use most significant bit (sign) to indicate that the size is >= INT_MAX; this is backwards compatible and will not unnecessarily increase file size

        boolean ret = WriteInt(elements.size()); // /*WriteValue<int>( (int)elements.size( ) )*/;
        assert( ret ); if( !ret ) return false;

        for( int i = 0; i < elements.size( ); i++ )
        {
            ret = Write(elements.get(i) );
            assert( ret ); if( !ret ) return false;
        }

        return true;
    }
}
