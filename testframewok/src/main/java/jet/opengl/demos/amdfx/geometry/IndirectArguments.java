package jet.opengl.demos.amdfx.geometry;

import org.lwjgl.util.vector.Readable;

import java.nio.ByteBuffer;

final class IndirectArguments implements Readable {
    static final int SIZE = 20;

    /**
     Static function to ensure IndirectArguments remains a POD
     */
        /*static void Init (IndirectArguments &ia)
            {
                    ia.IndexCountPerInstance = 0;
        ia.InstanceCount = 1;
        ia.StartIndexLocation = 0;
        ia.BaseVertexLocation = 0;
        ia.StartInstanceLocation = 0;
    }*/

    int IndexCountPerInstance;
    int InstanceCount = 1;
    int StartIndexLocation;
    int BaseVertexLocation;
    int StartInstanceLocation;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        buf.putInt(IndexCountPerInstance);
        buf.putInt(InstanceCount);
        buf.putInt(StartIndexLocation);
        buf.putInt(BaseVertexLocation);
        buf.putInt(StartInstanceLocation);
        return buf;
    }
}
