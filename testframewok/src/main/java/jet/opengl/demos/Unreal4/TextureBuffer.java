package jet.opengl.demos.Unreal4;

import java.nio.Buffer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.LogUtil;

public class TextureBuffer {
    public int NumBytes;
    public String mName = "TextureBuffer";

    private int mBuffer;
    private int mTexture;
    public int InternalFormat;
    private boolean mInilized;
    private int mStructSize;
    private int mElementCount;

    public TextureBuffer(){}

    public TextureBuffer(String name){ mName = name;}

    public void Initialize(int structSize, int numElements, int internalFormat){
        Initialize(structSize, numElements, internalFormat, 0);
    }

    public void Initialize(int structSize, int numElements, int internalFormat, int flags){
        mStructSize = structSize;
        mElementCount = numElements;
        InternalFormat = internalFormat;

        if(mInilized){
            Release();
        }

        if(!GLFuncProviderFactory.isInitlized()){
            LogUtil.e(LogUtil.LogType.DEFAULT, "The GL content hasn't initialized in the method 'TextureBuffer::Initialize'");
            return;
        }

        NumBytes = structSize * numElements;
        if(NumBytes == 0)
            throw new IllegalStateException("The NumBytes is 0 in the TextureBuffer: " + mName);

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        mBuffer = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_TEXTURE_BUFFER, mBuffer);
        gl.glBufferData(GLenum.GL_TEXTURE_BUFFER, NumBytes, GLenum.GL_DYNAMIC_DRAW);

        // Generate a new name for texture.
        mTexture = gl.glGenTexture();
        // Bind it toe the buffer texture target to create it
        gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER, mTexture);
        // Attach the buffer object to the texture and specify format
        gl.glTexBuffer(GLenum.GL_TEXTURE_BUFFER, InternalFormat, mBuffer);

        gl.glBindTexture(GLenum.GL_TEXTURE_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_TEXTURE_BUFFER, 0);
    }

    public void update(int offset, Buffer buffer){
        if(mInilized){
            if(GLFuncProviderFactory.isInitlized()){
                if(offset < 0)
                    throw new IllegalArgumentException("invalid offset: " + offset);
                GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

                gl.glBindBuffer(GLenum.GL_TEXTURE_BUFFER, mBuffer);
                gl.glBufferSubData(GLenum.GL_TEXTURE_BUFFER, offset, buffer);
                gl.glBindBuffer(GLenum.GL_TEXTURE_BUFFER, 0);
            }
        }
    }

    public int getTexture() { return mTexture;}
    public int getBuffer()  {return mBuffer;}

    public void Release(){
        if(mInilized){
            if(GLFuncProviderFactory.isInitlized()){
                GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
                gl.glDeleteBuffer(mBuffer);
                gl.glDeleteTexture(mTexture);
                mInilized = false;
                NumBytes = 0;
                InternalFormat = 0;
            }
        }
    }
}
