package jet.opengl.demos.demos.os;

import org.lwjgl.util.vector.Matrix4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/6/7.
 */

final class Eyepoint {
    private static final int SIZE = 128;
    private Texture2D m_startTex;
    private final Matrix4f m_model = new Matrix4f();
    private GLFuncProvider gl;

    void initlize(){
        Texture2DDesc desc = new Texture2DDesc(SIZE, SIZE, GLenum.GL_RGBA8);
        TextureDataDesc dataDesc = new TextureDataDesc(GLenum.GL_RGBA, GLenum.GL_UNSIGNED_BYTE, load_frame(0.5f));
        m_startTex = TextureUtils.createTexture2D(desc, dataDesc);

        gl = GLFuncProviderFactory.getGLFuncProvider();
    }

    void draw(SingleTextureProgram program, VertexArrayObject rectVAO, float sizeX, float sizeY, boolean touched){
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glEnable(GLenum.GL_BLEND);
        gl.glBlendFunc(GLenum.GL_SRC_ALPHA, GLenum.GL_ONE_MINUS_SRC_ALPHA);

        gl.glBindTexture(m_startTex.getTarget(), m_startTex.getTexture());
        m_model.m00 = sizeX;
        m_model.m11 = sizeY;
        program.enable();
        program.setMVP(m_model);
        if(touched)
            program.setMaskColor(1.0f, 0.9f, 0.11f, 1.0f);

        rectVAO.bind();
        gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);
        rectVAO.unbind();
        if(touched)
            program.setMaskColor(1,1,1,1);
    }

    private static float star(int x, int y, float t)
    {
        float c = SIZE / 2.0f;

        float i = (0.25f * (float)Math.sin(2.0f * 3.1415926f * t) + 0.75f);
        float k = SIZE * 0.046875f * i;

        float dist = (float)Math.sqrt((x - c) * (x - c) + (y - c) * (y - c));

        float salpha = 1.0f - dist / c;
        float xalpha = (float)x == c ? c : k / Math.abs(x - c);
        float yalpha = (float)y == c ? c : k / Math.abs(y - c);

        return Math.max(0.0f, Math.min(1.0f, i * salpha * 0.2f + salpha * xalpha * yalpha));
    }

    private static ByteBuffer load_frame(float t){
        int x, y;
        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(SIZE * SIZE * 4);

        for(y = 0; y < SIZE; y++){
            for (x = 0;  x < SIZE;  x++){
                buffer.put((byte) 255);
                buffer.put((byte) 255);
                buffer.put((byte) 255);
                buffer.put((byte) (255 * star(x, y, t)));
            }
        }
        buffer.flip();
        return buffer;
    }
}
