package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Writable;
import org.lwjgl.util.vector.WritableVector3f;
import org.lwjgl.util.vector.WritableVector4f;

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;

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

    public final int             Read( byte[] buffer) throws IOException{
        return Read(buffer, 0, buffer.length);
    }

    public abstract int          Read( byte[] buffer, int offset, int count /*int64 * outCountRead = NULL*/ ) throws IOException;
    public abstract int          Write(byte[] buffer, int offset, int count/*, int64 * outCountWritten = NULL*/ ) throws IOException;

    public abstract long         ReadLong() throws IOException;
    public abstract byte         Read() throws IOException;
    public abstract short        ReadShort() throws IOException;
    public abstract int          ReadInt() throws IOException;
    public abstract float        ReadFloat() throws IOException;
    public abstract void         ReadObject(int size, Writable obj) throws IOException;

    public abstract boolean      Write(long value) throws IOException;
    public abstract boolean      Write(byte value) throws IOException;
    public abstract boolean      Write(short value) throws IOException;
    public abstract boolean      Write(int value) throws IOException;
    public abstract boolean      Write(float value) throws IOException;
    public abstract boolean      Write(int size, Readable obj) throws IOException;

    public final boolean Write(boolean b) throws IOException{
        return Write((byte)(b?1:0));
    }

    public final boolean Write(ReadableVector3f v) throws IOException{
        Write(v.getX());
        Write(v.getY());
        Write(v.getZ());
        return true;
    }

    public final boolean Write(ReadableVector4f v) throws IOException{
        Write(v.getX());
        Write(v.getY());
        Write(v.getZ());
        return Write(v.getW());
    }

    public final boolean Write(UUID uuid) throws IOException{
        if(uuid != null) {
            Write(uuid.getMostSignificantBits());
            Write(uuid.getLeastSignificantBits());
        }else{
            final long zero = 0;
            Write(zero);
            Write(zero);
        }
        return true;
    }

    public final boolean ReadBoolean() throws IOException{
        return Read() != 0;
    }

    public final boolean Read(WritableVector4f v) throws IOException{
        v.setX(ReadFloat());
        v.setY(ReadFloat());
        v.setZ(ReadFloat());
        v.setW(ReadFloat());

        return true;
    }

    public final boolean Read(WritableVector3f v) throws IOException{
        v.setX(ReadFloat());
        v.setY(ReadFloat());
        v.setZ(ReadFloat());

        return true;
    }

    public final int Write(byte[] buffer ) throws IOException{
        return Write(buffer, 0, buffer.length);
    }

    public final UUID ReadUUID() throws IOException{
        long mostSigBits = ReadLong();
        long leastSigBits = ReadLong();

        return new UUID(mostSigBits, leastSigBits);
        /*byte[] bits = new byte[16];
        Read(bits);
        return UUID.nameUUIDFromBytes(bits);*/
    }

    // these use internal binary representation prefixed with size
    public boolean WriteString( String str )throws IOException{
        int lengthInBytes = str.length( );
        if( !/*WriteValue<uint32>*/ Write( lengthInBytes ) )      // 32nd bit flags it as unicode (utf16) for verification when reading
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
        if( !/*WriteValue<uint32>*/ Write( lengthInBytes | ( 1 << 31 ) ) )      // 32nd bit flags it as unicode (utf16) for verification when reading
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
            return "";

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

        byte[] buffer = new byte[lengthInBytes/2];          // TODO: remove dynamic alloc for smaller (most) reads - just use stack memory
        for(int i = 0; i < buffer.length; i++){
            buffer[i] = Read();
            Read(); // skip
        }

        String str = new String(buffer);
        return str;
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

        boolean ret = Write(elements.size()); // /*WriteValue<int>( (int)elements.size( ) )*/;
        assert( ret ); if( !ret ) return false;

        for( int i = 0; i < elements.size( ); i++ )
        {
            ret = Write(elements.get(i) );
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

        boolean ret = Write(elements.size()); // /*WriteValue<int>( (int)elements.size( ) )*/;
        assert( ret ); if( !ret ) return false;

        for( int i = 0; i < elements.size( ); i++ )
        {
            ret = Write(elements.get(i) );
            assert( ret ); if( !ret ) return false;
        }

        return true;
    }

    public boolean ReadVector( StackByte elements, int strideInBytes ) throws IOException {
        assert( elements.size( ) == 0 ); // must be empty at the moment
        /*int count;
        if( !ReadValue<int>( count, -1 ) )
        return false;*/
        int count = ReadInt();
        assert( count >= 0 ); if( count < 0 ) return false;
        if( count == 0 ) return true;

        elements.resize( count * strideInBytes);

        byte[] content = elements.getData();
        Read(content);
//        for(int i = 0; i < count * strideInBytes; i++){
//            content[i] = Read();
//        }

        return true;
    }

    public boolean WriteVector( StackByte elements, int strideInBytes ) throws IOException {
        assert( elements.size( ) < Integer.MAX_VALUE ); // not supported; to add support for 64bit size use most significant bit (sign) to indicate that the size is >= INT_MAX; this is backwards compatible and will not unnecessarily increase file size

        boolean ret = Write(elements.size()/strideInBytes); // /*WriteValue<int>( (int)elements.size( ) )*/;
        assert( ret ); if( !ret ) return false;

        for( int i = 0; i < elements.size( ); i++ )
        {
            ret = Write(elements.get(i) );
            assert( ret ); if( !ret ) return false;
        }

        return true;
    }
}
