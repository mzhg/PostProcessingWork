package jet.opengl.renderer.Unreal4.mesh;

import jet.opengl.postprocessing.buffer.BufferGL;

public class FVertexInputStream {
    public int StreamIndex;
    public int Offset;
    public BufferGL VertexBuffer;

    public FVertexInputStream(){}

    public FVertexInputStream(int streamIndex, int offset, BufferGL vertexBuffer) {
        StreamIndex = streamIndex;
        Offset = offset;
        VertexBuffer = vertexBuffer;
    }
}
