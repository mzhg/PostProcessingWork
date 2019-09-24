package jet.opengl.loader.assimp;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;

final class VertexAttrib implements Disposeable {

    BufferGL mVertexData;
    int internalFormat;
    boolean isInstanced;
    int instanceRate;

    @Override
    public void dispose() {
        if(mVertexData != null)
        {
            mVertexData.dispose();
            mVertexData = null;
        }
    }
}
