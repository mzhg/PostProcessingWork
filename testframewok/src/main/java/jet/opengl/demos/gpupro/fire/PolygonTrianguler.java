package jet.opengl.demos.gpupro.fire;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import jet.opengl.postprocessing.buffer.AttribDesc;
import jet.opengl.postprocessing.buffer.BufferBinding;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/10/28.
 */

final class PolygonTrianguler {
    private float[] mBuffer;
    private int mOffset;

    public PolygonTrianguler(int count){
        mBuffer = new float[count * 5];
    }

    public void addVertex(float x, float y, float z, float s, float t){
        mBuffer[mOffset++] = x;
        mBuffer[mOffset++] = y;
        mBuffer[mOffset++] = z;
        mBuffer[mOffset++] = s;
        mBuffer[mOffset++] = t;
    }

    public static VertexArrayObject triangular(PolygonTrianguler[] buffers, int[] indicesCountAndType){
        int totalSize = 0;

        int[] indices_offset = new int[buffers.length];
        int i = 0;
        int indices_count = 0;
        for(PolygonTrianguler polygon : buffers){
            if(polygon.mBuffer.length != polygon.mOffset)
                throw new IllegalArgumentException();

            indices_offset[i++] = totalSize / 5;
            totalSize += polygon.mOffset;

            indices_count += Math.max(0, polygon.mOffset/5 - 2);
        }

        FloatBuffer buffer = CacheBuffer.getCachedFloatBuffer(totalSize);
        for(PolygonTrianguler polygon : buffers){
            buffer.put(polygon.mBuffer);
        }

        buffer.flip();

        BufferGL array = new BufferGL();
        array.initlize(GLenum.GL_ARRAY_BUFFER, totalSize * 4, buffer, GLenum.GL_STATIC_DRAW);

        i = 0;
        final boolean use32Bit = indices_count > Numeric.MAX_USHORT;
        ByteBuffer indices = CacheBuffer.getCachedByteBuffer( use32Bit ? (4 * indices_count) : (2 * indices_count));
        for(int start_index : indices_offset){
            PolygonTrianguler polygon = buffers[i++];
            for(int j = 2; j < polygon.mOffset/5; j++){
                if(use32Bit){
                    indices.putInt(start_index);
                    indices.putInt(start_index + j - 1);
                    indices.putInt(start_index + j);
                }else{
                    indices.putShort((short)start_index);
                    indices.putShort((short) (start_index + j - 1));
                    indices.putShort((short) (start_index + j));
                }
            }
        }

        BufferGL indicesBuffer = new BufferGL();
        indicesBuffer.initlize(GLenum.GL_ELEMENT_ARRAY_BUFFER, indices.remaining(), indices, GLenum.GL_STATIC_DRAW);

        VertexArrayObject vao = new VertexArrayObject();
        vao.initlize(new BufferBinding[]{
                new BufferBinding(array, new AttribDesc(0, 3, GLenum.GL_FLOAT, false, 20, 0),
                                        new AttribDesc(1, 2, GLenum.GL_FLOAT, false, 20, 12)),
        }, indicesBuffer);

        vao.unbind();

        indicesCountAndType[0] = indices_count;
        indicesCountAndType[1] = use32Bit ? GLenum.GL_UNSIGNED_INT : GLenum.GL_UNSIGNED_SHORT;

        return vao;
    }
}
