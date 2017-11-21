package jet.opengl.demos.intel.va;

import java.io.IOException;

/**
 * Created by mazhen'gui on 2017/11/17.
 */

public class VaMemoryStream extends VaStream {
    @Override
    public boolean IsOpen() {
        return false;
    }

    @Override
    public void Close() throws IOException {

    }

    @Override
    public int GetLength() {
        return 0;
    }

    @Override
    public int GetPosition() {
        return 0;
    }

    @Override
    public boolean CanSeek() {
        return false;
    }

    @Override
    public void Seek(int position) throws IOException {

    }

    @Override
    public void Truncate() {

    }

    @Override
    public int Read(byte[] buffer, int offset, int count) throws IOException {
        return 0;
    }

    @Override
    public int Write(byte[] buffer, int offset, int count) throws IOException {
        return 0;
    }

    @Override
    public long ReadLong() {
        return 0;
    }

    @Override
    public byte Read() throws IOException {
        return 0;
    }

    @Override
    public short ReadShort() throws IOException {
        return 0;
    }

    @Override
    public int ReadInt() throws IOException {
        return 0;
    }

    @Override
    public boolean WriteLong(long value) throws IOException {
        return false;
    }

    @Override
    public boolean Write(byte value) throws IOException {
        return false;
    }

    @Override
    public boolean WriteShort(short value) throws IOException {
        return false;
    }

    @Override
    public boolean WriteInt(int value) throws IOException {
        return false;
    }
}
