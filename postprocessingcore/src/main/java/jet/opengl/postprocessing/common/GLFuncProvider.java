package jet.opengl.postprocessing.common;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Collection;

import jet.opengl.postprocessing.texture.ImageLoader;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/3/28.
 */

public interface GLFuncProvider {
    public boolean isSupportExt(String ext);

    public GLAPI getHostAPI();
    public GLAPIVersion getGLAPIVersion();

    public void glActiveTexture (int texture);

    public void glBindTexture (int target, int texture);

    public void glBlendFunc (int sfactor, int dfactor);

    public void glClear (int mask);

    public void glClearColor (float red, float green, float blue, float alpha);

    public void glClearDepthf (float depth);

    public void glClearStencil (int s);

    public void glColorMask (boolean red, boolean green, boolean blue, boolean alpha);

    /*
    public default void glCompressedTexImage2D (int target, int level, int internalformat, int width, int height, int border, int imageSize, Buffer data){
        defaultImplemented("glCompressedTexImage2D");
    }

    public default void glCompressedTexSubImage2D (int target, int level, int xoffset, int yoffset, int width, int height, int format,
                                                   int imageSize, Buffer data){
        defaultImplemented("glCompressedTexSubImage2D");
    }
    */

    public void glCopyTexImage2D (int target, int level, int internalformat, int x, int y, int width, int height, int border);
    public void glCopyTexSubImage2D (int target, int level, int xoffset, int yoffset, int x, int y, int width, int height);

    public void glCullFace (int mode);

    public void glDeleteTextures (int...textures);
    public void glDeleteTextures (int[]textures, int offset, int length);

    static void defaultImplemented(String msg){
        throw new RuntimeException("Unsupport the func: " + msg);
    }

    public void glDeleteTexture (int texture);

    public void glDepthFunc (int func);

    public void glDepthMask (boolean flag);

    // Default is OK.
//    public default void glDepthRangef (float zNear, float zFar){ defaultImplemented("glDepthRangef");}

    public void glDisable (int cap);

    public void glDrawArrays (int mode, int first, int count);

    public void glDrawElements (int mode, int count, int type, Buffer indices);

    public void glEnable (int cap);

    public void glFinish ();

    public void glFlush ();

    public void glFrontFace (int mode);

    public int glGenTexture ();

    public int glGetError ();

    public int glGetInteger(int pname);
    public void glGetInteger (int pname, IntBuffer params);

    public String glGetString (int name);

    public void glHint (int target, int mode);

    public void glLineWidth (float width);

    public void glPixelStorei (int pname, int param);

    public void glPolygonOffset (float factor, float units);

    public void glReadPixels (int x, int y, int width, int height, int format, int type, ByteBuffer pixels);

    public void glScissor (int x, int y, int width, int height);

    public void glStencilFunc (int func, int ref, int mask);

    public void glStencilMask (int mask);

    public void glStencilOp (int fail, int zfail, int zpass);

    public void glTexImage2D (int target, int level, int internalformat, int width, int height, int border, int format, int type, Buffer pixels);
    public void glTexImage2D (int target, int level, int internalformat, int width, int height, int border, int format, int type, long pixels_offset);
    public default void glTexImage2D (int target, int level, int internalformat, int width, int height){
        int format = TextureUtils.measureFormat(internalformat);
        int type =   TextureUtils.measureDataType(internalformat);
        glTexImage2D(target, level, internalformat, width, height, 0, format, type, (Buffer)null);
    }

    public void glTexParameterf (int target, int pname, float param);

    public void glTexSubImage2D (int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, Buffer pixels);
    public void glTexSubImage2D (int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, long pixels_offset);

    public void glViewport (int x, int y, int width, int height);

    public void glAttachShader (int program, int shader);

    public void glBindAttribLocation (int program, int index, CharSequence name);

    public void glBindBuffer (int target, int buffer);

    public void glBindFramebuffer (int target, int framebuffer);

    public void glBindRenderbuffer (int target, int renderbuffer);

    public void glBlendColor (float red, float green, float blue, float alpha);

    public void glBlendEquation (int mode);

    public void glBlendEquationSeparate (int modeRGB, int modeAlpha);

    public void glBlendFuncSeparate (int srcRGB, int dstRGB, int srcAlpha, int dstAlpha);

    public void glBufferSubData (int target, int offset, Buffer data);

    public int glCheckFramebufferStatus (int target);

    public void glCompileShader (int shader);

    public int glCreateProgram ();

    public int glCreateShader (int type);

    public void glDeleteBuffer (int buffer);

    public void glDeleteFramebuffer (int framebuffer);

    public void glDeleteProgram (int program);

    public void glDeleteRenderbuffer (int renderbuffer);

    public void glDeleteShader (int shader);

    public void glDetachShader (int program, int shader);

    public void glDisableVertexAttribArray (int index);

    public void glDrawElements (int mode, int count, int type, long indices);

    public void glEnableVertexAttribArray (int index);

    public void glFramebufferRenderbuffer (int target, int attachment, int renderbuffertarget, int renderbuffer);

    public void glFramebufferTexture2D (int target, int attachment, int textarget, int texture, int level);

    public int glGenBuffer ();
    public void glGenBuffers (IntBuffer buffers);

    public void glGenerateMipmap (int target);

    public int glGenFramebuffer ();

    public int glGenRenderbuffer ();

    // deviates
    public String glGetActiveAttrib (int program, int index, int maxLength, IntBuffer size, IntBuffer type);
    // deviates
    public String glGetActiveUniform (int program, int index, int maxLength, IntBuffer size, IntBuffer type);

    public void glGetAttachedShaders (int program, int[] count, int[] shaders);

    public int glGetAttribLocation (int program, CharSequence name);

    public boolean glGetBoolean(int pname);
    public void glGetBooleanv (int pname, ByteBuffer params);

    public int glGetBufferParameteri(int target, int pname);
    public void glGetBufferParameteriv (int target, int pname, IntBuffer params);

    public float glGetFloat(int pname);
    public void glGetFloatv (int pname, FloatBuffer params);

    public default int glGetFramebufferAttachmentParameteri(int target, int attachment, int pname){
        defaultImplemented("glGetFramebufferAttachmentParameteri");
        return 0;
    }
    public default void glGetFramebufferAttachmentParameteriv (int target, int attachment, int pname, IntBuffer params){
        defaultImplemented("glGetFramebufferAttachmentParameteriv");
    }

    public int glGetProgrami(int program, int pname);
    public void glGetProgramiv(int program, int pname, IntBuffer params);

    // deviates
    public String glGetProgramInfoLog (int program);

    public default void glGetRenderbufferParameteriv (int target, int pname, IntBuffer params){
        defaultImplemented("glGetRenderbufferParameteriv");
    }

    public int glGetShaderi(int shader, int pname);

    public void glGetShaderiv (int shader, int pname, IntBuffer params);

    // deviates
    public String glGetShaderInfoLog (int shader);

    public default void glGetShaderPrecisionFormat (int shadertype, int precisiontype, IntBuffer range, IntBuffer precision){
        defaultImplemented("glGetShaderPrecisionFormat");
    }

    public float glGetTexParameterf (int target, int pname);
    public void glGetTexParameterfv (int target, int pname, FloatBuffer params);

    public int glGetTexParameteri (int target, int pname);
    public void glGetTexParameteriv (int target, int pname, IntBuffer params);

    public float glGetUniformf (int program, int location);
    public void glGetUniformfv (int program, int location, FloatBuffer params);

    public int glGetUniformi(int program, int location);
    public void glGetUniformiv (int program, int location, IntBuffer params);

    public int glGetUniformLocation (int program, CharSequence name);

    public default void glGetVertexAttribfv (int index, int pname, FloatBuffer params){
        defaultImplemented("glGetVertexAttribfv");
    }

    public default void glGetVertexAttribiv (int index, int pname, IntBuffer params){
        defaultImplemented("glGetVertexAttribiv");
    }

    public default void glGetVertexAttribPointerv (int index, int pname, Buffer pointer){
        defaultImplemented("glGetVertexAttribPointerv");
    }

    public boolean glIsBuffer (int buffer);

    public boolean glIsEnabled (int cap);

    public boolean glIsFramebuffer (int framebuffer);

    public boolean glIsProgram (int program);

    public boolean glIsRenderbuffer (int renderbuffer);

    public boolean glIsShader (int shader);

    public boolean glIsTexture (int texture);

    public void glLinkProgram (int program);

    public void glReleaseShaderCompiler ();

    public void glRenderbufferStorage (int target, int internalformat, int width, int height);

    public void glSampleCoverage (float value, boolean invert);

    public void glShaderBinary (IntBuffer shaders, int binaryformat, ByteBuffer binary);

    // Deviates
    public void glShaderSource (int shader, CharSequence string);

    public void glStencilFuncSeparate (int face, int func, int ref, int mask);

    public void glStencilMaskSeparate (int face, int mask);

    public void glStencilOpSeparate (int face, int fail, int zfail, int zpass);

    public void glTexParameterfv (int target, int pname, FloatBuffer params);

    public void glTexParameteri (int target, int pname, int param);
    public void glTexParameteriv (int target, int pname, int[] params);

    public void glUniform1f (int location, float x);
    public void glUniform1fv (int location, FloatBuffer v);

    public void glUniform1i (int location, int x);

    public void glUniform1iv (int location, IntBuffer v);

    public void glUniform2f (int location, float x, float y);

    public void glUniform2fv (int location, FloatBuffer v);

    public void glUniform2i (int location, int x, int y);

    public void glUniform2iv (int location, IntBuffer v);

    public void glUniform3f (int location, float x, float y, float z);

    public void glUniform3fv (int location, FloatBuffer v);

    public void glUniform3i (int location, int x, int y, int z);

    public void glUniform3iv (int location, IntBuffer v);

    public void glUniform4f (int location, float x, float y, float z, float w);

    public void glUniform4fv (int location, FloatBuffer v);

    public void glUniform4i (int location, int x, int y, int z, int w);

    public void glUniform4iv (int location, IntBuffer v);

    public void glUniformMatrix2fv (int location, boolean transpose, FloatBuffer value);
    public void glUniformMatrix3fv (int location, boolean transpose, FloatBuffer value);
    public void glUniformMatrix4fv (int location, boolean transpose, FloatBuffer value);

    public void glUseProgram (int program);

    public void glValidateProgram (int program);

    public void glVertexAttrib1f (int indx, float x);

    public void glVertexAttrib1fv (int indx, FloatBuffer values);

    public void glVertexAttrib2f (int indx, float x, float y);

    public void glVertexAttrib2fv (int indx, FloatBuffer values);

    public void glVertexAttrib3f (int indx, float x, float y, float z);

    public void glVertexAttrib3fv (int indx, FloatBuffer values);

    public void glVertexAttrib4f (int indx, float x, float y, float z, float w);

    public void glVertexAttrib4fv (int indx, FloatBuffer values);

    public void glVertexAttribPointer (int indx, int size, int type, boolean normalized, int stride, Buffer ptr);

    public void glVertexAttribPointer (int indx, int size, int type, boolean normalized, int stride, int ptr);

    // C function void glReadBuffer ( GLenum mode )

    public void glReadBuffer (int mode);

    // C function void glDrawRangeElements ( GLenum mode, GLuint start, GLuint end, GLsizei count, GLenum type, const GLvoid
    // *indices )

    public void glDrawRangeElements (int mode, int start, int end, int count, int type, java.nio.Buffer indices);

    // C function void glDrawRangeElements ( GLenum mode, GLuint start, GLuint end, GLsizei count, GLenum type, GLsizei offset )

    public void glDrawRangeElements (int mode, int start, int end, int count, int type, int offset);

    // C function void glTexImage3D ( GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLsizei
    // depth, GLint border, GLenum format, GLenum type, const GLvoid *pixels )

    public default void glTexImage3D (int target, int level, int internalformat, int width, int height, int depth){
        int format = TextureUtils.measureFormat(internalformat);
        int type   = TextureUtils.measureDataType(internalformat);

        glTexImage3D(target, level, internalformat, width, height, depth, 0, format, type, (Buffer)null);
    }

    public void glTexImage3D (int target, int level, int internalformat, int width, int height, int depth, int border, int format,
                              int type, java.nio.Buffer pixels);

    // C function void glTexImage3D ( GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLsizei
    // depth, GLint border, GLenum format, GLenum type, GLsizei offset )

    public void glTexImage3D (int target, int level, int internalformat, int width, int height, int depth, int border, int format,
                              int type, long offset);

    // C function void glTexSubImage3D ( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei width,
    // GLsizei height, GLsizei depth, GLenum format, GLenum type, const GLvoid *pixels )

    public void glTexSubImage3D (int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth,
                                 int format, int type, java.nio.Buffer pixels);
    public void glTexSubImage3D (int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth,
                                 int format, int type, long pixels_offset);

    // C function void glCopyTexSubImage3D ( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLint x,
    // GLint y, GLsizei width, GLsizei height )

    public void glCopyTexSubImage3D (int target, int level, int xoffset, int yoffset, int zoffset, int x, int y, int width,
                                     int height);

    // // C function void glCompressedTexImage3D ( GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height,
    // GLsizei depth, GLint border, GLsizei imageSize, const GLvoid *data )
    //
    // public void glCompressedTexImage3D(
    // int target,
    // int level,
    // int internalformat,
    // int width,
    // int height,
    // int depth,
    // int border,
    // int imageSize,
    // java.nio.Buffer data
    // );
    //
    // // C function void glCompressedTexImage3D ( GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height,
    // GLsizei depth, GLint border, GLsizei imageSize, GLsizei offset )
    //
    // public void glCompressedTexImage3D(
    // int target,
    // int level,
    // int internalformat,
    // int width,
    // int height,
    // int depth,
    // int border,
    // int imageSize,
    // int offset
    // );
    //
    // // C function void glCompressedTexSubImage3D ( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei
    // width, GLsizei height, GLsizei depth, GLenum format, GLsizei imageSize, const GLvoid *data )
    //
    // public void glCompressedTexSubImage3D(
    // int target,
    // int level,
    // int xoffset,
    // int yoffset,
    // int zoffset,
    // int width,
    // int height,
    // int depth,
    // int format,
    // int imageSize,
    // java.nio.Buffer data
    // );
    //
    // // C function void glCompressedTexSubImage3D ( GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei
    // width, GLsizei height, GLsizei depth, GLenum format, GLsizei imageSize, GLsizei offset )
    //
    // public void glCompressedTexSubImage3D(
    // int target,
    // int level,
    // int xoffset,
    // int yoffset,
    // int zoffset,
    // int width,
    // int height,
    // int depth,
    // int format,
    // int imageSize,
    // int offset
    // );

    // C function void glGenQueries ( GLsizei n, GLuint *ids )


    public int glGenQuery ();

    // C function void glGenQueries ( GLsizei n, GLuint *ids )

    public void glGenQueries (java.nio.IntBuffer ids);

    // C function void glDeleteQueries ( GLsizei n, const GLuint *ids )

    public void glDeleteQuery (int query);

    // C function void glDeleteQueries ( GLsizei n, const GLuint *ids )

    public void glDeleteQueries (java.nio.IntBuffer ids);

    // C function GLboolean glIsQuery ( GLuint id )

    public boolean glIsQuery (int id);

    // C function void glBeginQuery ( GLenum target, GLuint id )

    public void glBeginQuery (int target, int id);

    // C function void glEndQuery ( GLenum target )

    public void glEndQuery (int target);

    // // C function void glGetQueryiv ( GLenum target, GLenum pname, GLint *params )
    //
    // public void glGetQueryiv(
    // int target,
    // int pname,
    // int[] params,
    // int offset
    // );

    // C function void glGetQueryiv ( GLenum target, GLenum pname, GLint *params )

    public int glGetQueryi (int target, int pname);
    public void glGetQueryiv (int target, int pname, java.nio.IntBuffer params);

    // // C function void glGetQueryObjectuiv ( GLuint id, GLenum pname, GLuint *params )
    //
    // public void glGetQueryObjectuiv(
    // int id,
    // int pname,
    // int[] params,
    // int offset
    // );

    // C function void glGetQueryObjectuiv ( GLuint id, GLenum pname, GLuint *params )

    public int glGetQueryObjectuiv (int id, int pname);
    public void glGetQueryObjectuiv (int id, int pname, java.nio.IntBuffer params);

    // C function GLboolean glUnmapBuffer ( GLenum target )

    public boolean glUnmapBuffer (int target);

    // C function void glGetBufferPointerv ( GLenum target, GLenum pname, GLvoid** params )

    public java.nio.ByteBuffer glGetBufferPointerv (int target, int pname);

    // // C function void glDrawBuffers ( GLsizei n, const GLenum *bufs )
    //
    // public void glDrawBuffers(
    // int n,
    // int[] bufs,
    // int offset
    // );

    // C function void glDrawBuffers ( GLsizei n, const GLenum *bufs )

    public void glDrawBuffers (int buffer);
    public void glDrawBuffers (java.nio.IntBuffer bufs);

    // // C function void glUniformMatrix2x3fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )
    //
    // public void glUniformMatrix2x3fv(
    // int location,
    // int count,
    // boolean transpose,
    // float[] value,
    // int offset
    // );

    // C function void glUniformMatrix2x3fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )

    public void glUniformMatrix2x3fv (int location, boolean transpose, java.nio.FloatBuffer value);

    // // C function void glUniformMatrix3x2fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )
    //
    // public void glUniformMatrix3x2fv(
    // int location,
    // int count,
    // boolean transpose,
    // float[] value,
    // int offset
    // );

    // C function void glUniformMatrix3x2fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )

    public void glUniformMatrix3x2fv (int location, boolean transpose, java.nio.FloatBuffer value);

    // // C function void glUniformMatrix2x4fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )
    //
    // public void glUniformMatrix2x4fv(
    // int location,
    // int count,
    // boolean transpose,
    // float[] value,
    // int offset
    // );

    // C function void glUniformMatrix2x4fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )

    public void glUniformMatrix2x4fv (int location, boolean transpose, java.nio.FloatBuffer value);

    // // C function void glUniformMatrix4x2fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )
    //
    // public void glUniformMatrix4x2fv(
    // int location,
    // int count,
    // boolean transpose,
    // float[] value,
    // int offset
    // );

    // C function void glUniformMatrix4x2fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )

    public void glUniformMatrix4x2fv (int location, boolean transpose, java.nio.FloatBuffer value);

    // // C function void glUniformMatrix3x4fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )
    //
    // public void glUniformMatrix3x4fv(
    // int location,
    // int count,
    // boolean transpose,
    // float[] value,
    // int offset
    // );

    // C function void glUniformMatrix3x4fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )

    public void glUniformMatrix3x4fv (int location, boolean transpose, java.nio.FloatBuffer value);

    // // C function void glUniformMatrix4x3fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )
    //
    // public void glUniformMatrix4x3fv(
    // int location,
    // int count,
    // boolean transpose,
    // float[] value,
    // int offset
    // );

    // C function void glUniformMatrix4x3fv ( GLint location, GLsizei count, GLboolean transpose, const GLfloat *value )

    public void glUniformMatrix4x3fv (int location, boolean transpose, java.nio.FloatBuffer value);

    // C function void glBlitFramebuffer ( GLint srcX0, GLint srcY0, GLint srcX1, GLint srcY1, GLint dstX0, GLint dstY0, GLint
    // dstX1, GLint dstY1, GLbitfield mask, GLenum filter )

    public void glBlitFramebuffer (int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1,
                                   int mask, int filter);

    // C function void glRenderbufferStorageMultisample ( GLenum target, GLsizei samples, GLenum internalformat, GLsizei width,
    // GLsizei height )

    public void glRenderbufferStorageMultisample (int target, int samples, int internalformat, int width, int height);

    // C function void glFramebufferTextureLayer ( GLenum target, GLenum attachment, GLuint texture, GLint level, GLint layer )

    public void glFramebufferTextureLayer (int target, int attachment, int texture, int level, int layer);

    // // C function GLvoid * glMapBufferRange ( GLenum target, GLintptr offset, GLsizeiptr length, GLbitfield access )
    //
    // public java.nio.Buffer glMapBufferRange(
    // int target,
    // int offset,
    // int length,
    // int access
    // );

    // C function void glFlushMappedBufferRange ( GLenum target, GLintptr offset, GLsizeiptr length )

    public void glFlushMappedBufferRange (int target, int offset, int length);

    // C function void glBindVertexArray ( GLuint array )

    public void glBindVertexArray (int array);

    // C function void glDeleteVertexArrays ( GLsizei n, const GLuint *arrays )

    public void glDeleteVertexArray (int vao);
    public void glDeleteVertexArrays (java.nio.IntBuffer arrays);

    // C function void glGenVertexArrays ( GLsizei n, GLuint *arrays )

    public int glGenVertexArray ();

    // C function void glGenVertexArrays ( GLsizei n, GLuint *arrays )

    public void glGenVertexArrays (java.nio.IntBuffer arrays);

    // C function GLboolean glIsVertexArray ( GLuint array )

    public boolean glIsVertexArray (int array);

    //
    // // C function void glGetIntegeri_v ( GLenum target, GLuint index, GLint *data )
    //
    // public void glGetIntegeri_v(
    // int target,
    // int index,
    // int[] data,
    // int offset
    // );
    //
    // // C function void glGetIntegeri_v ( GLenum target, GLuint index, GLint *data )
    //
    // public void glGetIntegeri_v(
    // int target,
    // int index,
    // java.nio.IntBuffer data
    // );

    // C function void glBeginTransformFeedback ( GLenum primitiveMode )

    public void glBeginTransformFeedback (int primitiveMode);

    // C function void glEndTransformFeedback ( void )

    public void glEndTransformFeedback ();

    // C function void glBindBufferRange ( GLenum target, GLuint index, GLuint buffer, GLintptr offset, GLsizeiptr size )

    public void glBindBufferRange (int target, int index, int buffer, int offset, int size);

    // C function void glBindBufferBase ( GLenum target, GLuint index, GLuint buffer )

    public void glBindBufferBase (int target, int index, int buffer);

    // C function void glTransformFeedbackVaryings ( GLuint program, GLsizei count, const GLchar *varyings, GLenum bufferMode )

    public void glTransformFeedbackVaryings (int program, CharSequence[] varyings, int bufferMode);

    // // C function void glGetTransformFeedbackVarying ( GLuint program, GLuint index, GLsizei bufSize, GLsizei *length, GLint *size,
    // GLenum *type, GLchar *name )
    //
    // public void glGetTransformFeedbackVarying(
    // int program,
    // int index,
    // int bufsize,
    // int[] length,
    // int lengthOffset,
    // int[] size,
    // int sizeOffset,
    // int[] type,
    // int typeOffset,
    // byte[] name,
    // int nameOffset
    // );
    //
    // // C function void glGetTransformFeedbackVarying ( GLuint program, GLuint index, GLsizei bufSize, GLsizei *length, GLint *size,
    // GLenum *type, GLchar *name )
    //
    // public void glGetTransformFeedbackVarying(
    // int program,
    // int index,
    // int bufsize,
    // java.nio.IntBuffer length,
    // java.nio.IntBuffer size,
    // java.nio.IntBuffer type,
    // byte name
    // );
    //
    // // C function void glGetTransformFeedbackVarying ( GLuint program, GLuint index, GLsizei bufSize, GLsizei *length, GLint *size,
    // GLenum *type, GLchar *name )
    //
    // public String glGetTransformFeedbackVarying(
    // int program,
    // int index,
    // int[] size,
    // int sizeOffset,
    // int[] type,
    // int typeOffset
    // );
    //
    // // C function void glGetTransformFeedbackVarying ( GLuint program, GLuint index, GLsizei bufSize, GLsizei *length, GLint *size,
    // GLenum *type, GLchar *name )
    //
    // public String glGetTransformFeedbackVarying(
    // int program,
    // int index,
    // java.nio.IntBuffer size,
    // java.nio.IntBuffer type
    // );

    // C function void glVertexAttribIPointer ( GLuint index, GLint size, GLenum type, GLsizei stride, GLsizei offset )

    public void glVertexAttribIPointer (int index, int size, int type, int stride, int offset);

    // // C function void glGetVertexAttribIiv ( GLuint index, GLenum pname, GLint *params )
    //
    // public void glGetVertexAttribIiv(
    // int index,
    // int pname,
    // int[] params,
    // int offset
    // );

    // C function void glGetVertexAttribIiv ( GLuint index, GLenum pname, GLint *params )

    public void glGetVertexAttribIiv (int index, int pname, java.nio.IntBuffer params);

    // // C function void glGetVertexAttribIuiv ( GLuint index, GLenum pname, GLuint *params )
    //
    // public void glGetVertexAttribIuiv(
    // int index,
    // int pname,
    // int[] params,
    // int offset
    // );

    // C function void glGetVertexAttribIuiv ( GLuint index, GLenum pname, GLuint *params )

    public void glGetVertexAttribIuiv (int index, int pname, java.nio.IntBuffer params);

    // C function void glVertexAttribI4i ( GLuint index, GLint x, GLint y, GLint z, GLint w )

    public void glVertexAttribI4i (int index, int x, int y, int z, int w);

    // C function void glVertexAttribI4ui ( GLuint index, GLuint x, GLuint y, GLuint z, GLuint w )

    public void glVertexAttribI4ui (int index, int x, int y, int z, int w);

    // // C function void glVertexAttribI4iv ( GLuint index, const GLint *v )
    //
    // public void glVertexAttribI4iv(
    // int index,
    // int[] v,
    // int offset
    // );
    //
    // // C function void glVertexAttribI4iv ( GLuint index, const GLint *v )
    //
    // public void glVertexAttribI4iv(
    // int index,
    // java.nio.IntBuffer v
    // );
    //
    // // C function void glVertexAttribI4uiv ( GLuint index, const GLuint *v )
    //
    // public void glVertexAttribI4uiv(
    // int index,
    // int[] v,
    // int offset
    // );
    //
    // // C function void glVertexAttribI4uiv ( GLuint index, const GLuint *v )
    //
    // public void glVertexAttribI4uiv(
    // int index,
    // java.nio.IntBuffer v
    // );
    //
    // // C function void glGetUniformuiv ( GLuint program, GLint location, GLuint *params )
    //
    // public void glGetUniformuiv(
    // int program,
    // int location,
    // int[] params,
    // int offset
    // );

    // C function void glGetUniformuiv ( GLuint program, GLint location, GLuint *params )

    public void glGetUniformuiv (int program, int location, java.nio.IntBuffer params);

    // C function GLint glGetFragDataLocation ( GLuint program, const GLchar *name )

    public int glGetFragDataLocation (int program, String name);

    // // C function void glUniform1ui ( GLint location, GLuint v0 )
    //
    // public void glUniform1ui(
    // int location,
    // int v0
    // );
    //
    // // C function void glUniform2ui ( GLint location, GLuint v0, GLuint v1 )
    //
    // public void glUniform2ui(
    // int location,
    // int v0,
    // int v1
    // );
    //
    // // C function void glUniform3ui ( GLint location, GLuint v0, GLuint v1, GLuint v2 )
    //
    // public void glUniform3ui(
    // int location,
    // int v0,
    // int v1,
    // int v2
    // );
    //
    // // C function void glUniform4ui ( GLint location, GLuint v0, GLuint v1, GLuint v2, GLuint v3 )
    //
    // public void glUniform4ui(
    // int location,
    // int v0,
    // int v1,
    // int v2,
    // int v3
    // );
    //
    // // C function void glUniform1uiv ( GLint location, GLsizei count, const GLuint *value )
    //
    // public void glUniform1uiv(
    // int location,
    // int count,
    // int[] value,
    // int offset
    // );

    // C function void glUniform1uiv ( GLint location, GLsizei count, const GLuint *value )

    public void glUniform1uiv (int location, java.nio.IntBuffer value);

    // // C function void glUniform2uiv ( GLint location, GLsizei count, const GLuint *value )
    //
    // public void glUniform2uiv(
    // int location,
    // int count,
    // int[] value,
    // int offset
    // );
    //
    // // C function void glUniform2uiv ( GLint location, GLsizei count, const GLuint *value )
    //
    // public void glUniform2uiv(
    // int location,
    // int count,
    // java.nio.IntBuffer value
    // );
    //
    // // C function void glUniform3uiv ( GLint location, GLsizei count, const GLuint *value )
    //
    // public void glUniform3uiv(
    // int location,
    // int count,
    // int[] value,
    // int offset
    // );

    // C function void glUniform3uiv ( GLint location, GLsizei count, const GLuint *value )

    public void glUniform3uiv (int location, java.nio.IntBuffer value);

    // // C function void glUniform4uiv ( GLint location, GLsizei count, const GLuint *value )
    //
    // public void glUniform4uiv(
    // int location,
    // int count,
    // int[] value,
    // int offset
    // );

    // C function void glUniform4uiv ( GLint location, GLsizei count, const GLuint *value )

    public void glUniform4uiv (int location, java.nio.IntBuffer value);

    // // C function void glClearBufferiv ( GLenum buffer, GLint drawbuffer, const GLint *value )
    //
    // public void glClearBufferiv(
    // int buffer,
    // int drawbuffer,
    // int[] value,
    // int offset
    // );

    // C function void glClearBufferiv ( GLenum buffer, GLint drawbuffer, const GLint *value )

    public void glClearBufferiv (int buffer, int drawbuffer, java.nio.IntBuffer value);

    // // C function void glClearBufferuiv ( GLenum buffer, GLint drawbuffer, const GLuint *value )
    //
    // public void glClearBufferuiv(
    // int buffer,
    // int drawbuffer,
    // int[] value,
    // int offset
    // );

    // C function void glClearBufferuiv ( GLenum buffer, GLint drawbuffer, const GLuint *value )

    public void glClearBufferuiv (int buffer, int drawbuffer, java.nio.IntBuffer value);

    // // C function void glClearBufferfv ( GLenum buffer, GLint drawbuffer, const GLfloat *value )
    //
    // public void glClearBufferfv(
    // int buffer,
    // int drawbuffer,
    // float[] value,
    // int offset
    // );

    // C function void glClearBufferfv ( GLenum buffer, GLint drawbuffer, const GLfloat *value )

    public void glClearBufferfv (int buffer, int drawbuffer, java.nio.FloatBuffer value);

    // C function void glClearBufferfi ( GLenum buffer, GLint drawbuffer, GLfloat depth, GLint stencil )

    public void glClearBufferfi (int buffer, int drawbuffer, float depth, int stencil);

    // C function const GLubyte * glGetStringi ( GLenum name, GLuint index )

    public String glGetStringi (int name, int index);

    // C function void glCopyBufferSubData ( GLenum readTarget, GLenum writeTarget, GLintptr readOffset, GLintptr writeOffset,
    // GLsizeiptr size )

    public void glCopyBufferSubData (int readTarget, int writeTarget, int readOffset, int writeOffset, int size);

    // // C function void glGetUniformIndices ( GLuint program, GLsizei uniformCount, const GLchar *const *uniformNames, GLuint
    // *uniformIndices )
    //
    // public void glGetUniformIndices(
    // int program,
    // String[] uniformNames,
    // int[] uniformIndices,
    // int uniformIndicesOffset
    // );

    // C function void glGetUniformIndices ( GLuint program, GLsizei uniformCount, const GLchar *const *uniformNames, GLuint
    // *uniformIndices )

    public void glGetUniformIndices (int program, String[] uniformNames, java.nio.IntBuffer uniformIndices);

    // // C function void glGetActiveUniformsiv ( GLuint program, GLsizei uniformCount, const GLuint *uniformIndices, GLenum pname,
    // GLint *params )
    //
    // public void glGetActiveUniformsiv(
    // int program,
    // int uniformCount,
    // int[] uniformIndices,
    // int uniformIndicesOffset,
    // int pname,
    // int[] params,
    // int paramsOffset
    // );

    // C function void glGetActiveUniformsiv ( GLuint program, GLsizei uniformCount, const GLuint *uniformIndices, GLenum pname,
    // GLint *params )

    public void glGetActiveUniformsiv (int program, java.nio.IntBuffer uniformIndices, int pname,
                                       java.nio.IntBuffer params);

    // C function GLuint glGetUniformBlockIndex ( GLuint program, const GLchar *uniformBlockName )

    public int glGetUniformBlockIndex (int program, String uniformBlockName);

    // // C function void glGetActiveUniformBlockiv ( GLuint program, GLuint uniformBlockIndex, GLenum pname, GLint *params )
    //
    // public void glGetActiveUniformBlockiv(
    // int program,
    // int uniformBlockIndex,
    // int pname,
    // int[] params,
    // int offset
    // );

    // C function void glGetActiveUniformBlockiv ( GLuint program, GLuint uniformBlockIndex, GLenum pname, GLint *params )

    public void glGetActiveUniformBlockiv (int program, int uniformBlockIndex, int pname, java.nio.IntBuffer params);

    // // C function void glGetActiveUniformBlockName ( GLuint program, GLuint uniformBlockIndex, GLsizei bufSize, GLsizei *length,
    // GLchar *uniformBlockName )
    //
    // public void glGetActiveUniformBlockName(
    // int program,
    // int uniformBlockIndex,
    // int bufSize,
    // int[] length,
    // int lengthOffset,
    // byte[] uniformBlockName,
    // int uniformBlockNameOffset
    // );

    // C function void glGetActiveUniformBlockName ( GLuint program, GLuint uniformBlockIndex, GLsizei bufSize, GLsizei *length,
    // GLchar *uniformBlockName )

    public void glGetActiveUniformBlockName (int program, int uniformBlockIndex, IntBuffer length, ByteBuffer uniformBlockName);

    // C function void glGetActiveUniformBlockName ( GLuint program, GLuint uniformBlockIndex, GLsizei bufSize, GLsizei *length,
    // GLchar *uniformBlockName )

    public String glGetActiveUniformBlockName (int program, int uniformBlockIndex);

    // C function void glUniformBlockBinding ( GLuint program, GLuint uniformBlockIndex, GLuint uniformBlockBinding )

    public void glUniformBlockBinding (int program, int uniformBlockIndex, int uniformBlockBinding);

    // C function void glDrawArraysInstanced ( GLenum mode, GLint first, GLsizei count, GLsizei instanceCount )

    public void glDrawArraysInstanced (int mode, int first, int count, int instanceCount);

    // // C function void glDrawElementsInstanced ( GLenum mode, GLsizei count, GLenum type, const GLvoid *indices, GLsizei
    // instanceCount )
    //
    // public void glDrawElementsInstanced(
    // int mode,
    // int count,
    // int type,
    // java.nio.Buffer indices,
    // int instanceCount
    // );

    // C function void glDrawElementsInstanced ( GLenum mode, GLsizei count, GLenum type, const GLvoid *indices, GLsizei
    // instanceCount )

    public void glDrawElementsInstanced (int mode, int count, int type, int indicesOffset, int instanceCount);

    // // C function GLsync glFenceSync ( GLenum condition, GLbitfield flags )
    //
    // public long glFenceSync(
    // int condition,
    // int flags
    // );
    //
    // // C function GLboolean glIsSync ( GLsync sync )
    //
    // public boolean glIsSync(
    // long sync
    // );
    //
    // // C function void glDeleteSync ( GLsync sync )
    //
    // public void glDeleteSync(
    // long sync
    // );
    //
    // // C function GLenum glClientWaitSync ( GLsync sync, GLbitfield flags, GLuint64 timeout )
    //
    // public int glClientWaitSync(
    // long sync,
    // int flags,
    // long timeout
    // );
    //
    // // C function void glWaitSync ( GLsync sync, GLbitfield flags, GLuint64 timeout )
    //
    // public void glWaitSync(
    // long sync,
    // int flags,
    // long timeout
    // );

    // // C function void glGetInteger64v ( GLenum pname, GLint64 *params )
    //
    // public void glGetInteger64v(
    // int pname,
    // long[] params,
    // int offset
    // );

    // C function void glGetInteger64v ( GLenum pname, GLint64 *params )

    public void glGetInteger64v (int pname, java.nio.LongBuffer params);

    // // C function void glGetSynciv ( GLsync sync, GLenum pname, GLsizei bufSize, GLsizei *length, GLint *values )
    //
    // public void glGetSynciv(
    // long sync,
    // int pname,
    // int bufSize,
    // int[] length,
    // int lengthOffset,
    // int[] values,
    // int valuesOffset
    // );
    //
    // // C function void glGetSynciv ( GLsync sync, GLenum pname, GLsizei bufSize, GLsizei *length, GLint *values )
    //
    // public void glGetSynciv(
    // long sync,
    // int pname,
    // int bufSize,
    // java.nio.IntBuffer length,
    // java.nio.IntBuffer values
    // );
    //
    // // C function void glGetInteger64i_v ( GLenum target, GLuint index, GLint64 *data )
    //
    // public void glGetInteger64i_v(
    // int target,
    // int index,
    // long[] data,
    // int offset
    // );
    //
    // // C function void glGetInteger64i_v ( GLenum target, GLuint index, GLint64 *data )
    //
    // public void glGetInteger64i_v(
    // int target,
    // int index,
    // java.nio.LongBuffer data
    // );
    //
    // // C function void glGetBufferParameteri64v ( GLenum target, GLenum pname, GLint64 *params )
    //
    // public void glGetBufferParameteri64v(
    // int target,
    // int pname,
    // long[] params,
    // int offset
    // );

    // C function void glGetBufferParameteri64v ( GLenum target, GLenum pname, GLint64 *params )

    public void glGetBufferParameteri64v (int target, int pname, java.nio.LongBuffer params);

    // C function void glGenSamplers ( GLsizei count, GLuint *samplers )

    public int glGenSampler ();

    // C function void glGenSamplers ( GLsizei count, GLuint *samplers )

    public void glGenSamplers (java.nio.IntBuffer samplers);

    // C function void glDeleteSamplers ( GLsizei count, const GLuint *samplers )

    public void glDeleteSampler (int sampler);

    // C function void glDeleteSamplers ( GLsizei count, const GLuint *samplers )

    public default void glDeleteSamplers (Collection<Integer> samplers) {
        if(samplers == null)
            return;

        IntBuffer int_samplers = CacheBuffer.getCachedIntBuffer(samplers.size());
        for(Integer integer : samplers) {
            int_samplers.put(integer);
        }
        int_samplers.flip();

        glDeleteSamplers(int_samplers);
    }

    public default void glDeleteSamplers (int[] samplers, int offset, int count){
        if(samplers == null)
            return;

        IntBuffer int_samplers = CacheBuffer.getCachedIntBuffer(count);
        for(int i = 0; i < count; i++) {
            int_samplers.put(samplers[i + offset]);
        }
        int_samplers.flip();

        glDeleteSamplers(int_samplers);
    }

    public default void glDeleteSamplers (int sampler){
        IntBuffer int_samplers = CacheBuffer.wrap(sampler);
        glDeleteSamplers(int_samplers);
    }

    public void glDeleteSamplers (IntBuffer samplers);

    // C function GLboolean glIsSampler ( GLuint sampler )

    public boolean glIsSampler (int sampler);

    // C function void glBindSampler ( GLuint unit, GLuint sampler )

    public void glBindSampler (int unit, int sampler);

    // C function void glSamplerParameteri ( GLuint sampler, GLenum pname, GLint param )

    public void glSamplerParameteri (int sampler, int pname, int param);

    // // C function void glSamplerParameteriv ( GLuint sampler, GLenum pname, const GLint *param )
    //
    // public void glSamplerParameteriv(
    // int sampler,
    // int pname,
    // int[] param,
    // int offset
    // );

    // C function void glSamplerParameteriv ( GLuint sampler, GLenum pname, const GLint *param )

    public void glSamplerParameteriv (int sampler, int pname, java.nio.IntBuffer param);

    // C function void glSamplerParameterf ( GLuint sampler, GLenum pname, GLfloat param )

    public void glSamplerParameterf (int sampler, int pname, float param);

    // // C function void glSamplerParameterfv ( GLuint sampler, GLenum pname, const GLfloat *param )
    //
    // public void glSamplerParameterfv(
    // int sampler,
    // int pname,
    // float[] param,
    // int offset
    // );

    // C function void glSamplerParameterfv ( GLuint sampler, GLenum pname, const GLfloat *param )

    public void glSamplerParameterfv (int sampler, int pname, java.nio.FloatBuffer param);

    // // C function void glGetSamplerParameteriv ( GLuint sampler, GLenum pname, GLint *params )
    //
    // public void glGetSamplerParameteriv(
    // int sampler,
    // int pname,
    // int[] params,
    // int offset
    // );

    // C function void glGetSamplerParameteriv ( GLuint sampler, GLenum pname, GLint *params )

    public void glGetSamplerParameteriv (int sampler, int pname, java.nio.IntBuffer params);

    // // C function void glGetSamplerParameterfv ( GLuint sampler, GLenum pname, GLfloat *params )
    //
    // public void glGetSamplerParameterfv(
    // int sampler,
    // int pname,
    // float[] params,
    // int offset
    // );

    // C function void glGetSamplerParameterfv ( GLuint sampler, GLenum pname, GLfloat *params )

    public void glGetSamplerParameterfv (int sampler, int pname, java.nio.FloatBuffer params);

    // C function void glVertexAttribDivisor ( GLuint index, GLuint divisor )

    public void glVertexAttribDivisor (int index, int divisor);

    // C function void glBindTransformFeedback ( GLenum target, GLuint id )

    public void glBindTransformFeedback (int target, int id);

    // C function void glDeleteTransformFeedbacks ( GLsizei n, const GLuint *ids )

    public void glDeleteTransformFeedback (int feedback);

    // C function void glDeleteTransformFeedbacks ( GLsizei n, const GLuint *ids )

    public void glDeleteTransformFeedbacks (java.nio.IntBuffer ids);

    // C function void glGenTransformFeedbacks ( GLsizei n, GLuint *ids )

    public int glGenTransformFeedback ();

    // C function void glGenTransformFeedbacks ( GLsizei n, GLuint *ids )

    public void glGenTransformFeedbacks (java.nio.IntBuffer ids);

    // C function GLboolean glIsTransformFeedback ( GLuint id )

    public boolean glIsTransformFeedback (int id);

    // C function void glPauseTransformFeedback ( void )

    public void glPauseTransformFeedback ();

    // C function void glResumeTransformFeedback ( void )

    public void glResumeTransformFeedback ();

    // // C function void glGetProgramBinary ( GLuint program, GLsizei bufSize, GLsizei *length, GLenum *binaryFormat, GLvoid *binary
    // )
    //
    // public void glGetProgramBinary(
    // int program,
    // int bufSize,
    // int[] length,
    // int lengthOffset,
    // int[] binaryFormat,
    // int binaryFormatOffset,
    // java.nio.Buffer binary
    // );
    //
    // // C function void glGetProgramBinary ( GLuint program, GLsizei bufSize, GLsizei *length, GLenum *binaryFormat, GLvoid *binary
    // )
    //
    // public void glGetProgramBinary(
    // int program,
    // int bufSize,
    // java.nio.IntBuffer length,
    // java.nio.IntBuffer binaryFormat,
    // java.nio.Buffer binary
    // );
    //
    // // C function void glProgramBinary ( GLuint program, GLenum binaryFormat, const GLvoid *binary, GLsizei length )
    //
    // public void glProgramBinary(
    // int program,
    // int binaryFormat,
    // java.nio.Buffer binary,
    // int length
    // );

    // C function void glProgramParameteri ( GLuint program, GLenum pname, GLint value )

    public void glProgramParameteri (int program, int pname, int value);

    // // C function void glInvalidateFramebuffer ( GLenum target, GLsizei numAttachments, const GLenum *attachments )
    //
    // public void glInvalidateFramebuffer(
    // int target,
    // int numAttachments,
    // int[] attachments,
    // int offset
    // );

    // C function void glInvalidateFramebuffer ( GLenum target, GLsizei numAttachments, const GLenum *attachments )

    public void glInvalidateFramebuffer (int target, java.nio.IntBuffer attachments);

    // // C function void glInvalidateSubFramebuffer ( GLenum target, GLsizei numAttachments, const GLenum *attachments, GLint x,
    // GLint y, GLsizei width, GLsizei height )
    //
    // public void glInvalidateSubFramebuffer(
    // int target,
    // int numAttachments,
    // int[] attachments,
    // int offset,
    // int x,
    // int y,
    // int width,
    // int height
    // );

    // C function void glInvalidateSubFramebuffer ( GLenum target, GLsizei numAttachments, const GLenum *attachments, GLint x,
    // GLint y, GLsizei width, GLsizei height )

    public void glInvalidateSubFramebuffer (int target, java.nio.IntBuffer attachments, int x, int y,
                                            int width, int height);

    public void glGetProgramBinary(int program, int[] length, int[] binaryFormat, ByteBuffer binary);

    public void glBindImageTexture(int unit, int texture, int level, boolean layered, int layer, int access, int format);
    public void glDispatchCompute(int num_groups_x, int num_groups_y, int num_groups_z);
    public void glMemoryBarrier(int barriers);

    /**
     * The method doesn't support on the mobine -device, declare here just for debugging on the descktop.
     * @param face
     * @param mode
     */
    public void glPolygonMode(int face, int mode);
    public void glPatchParameterfv(int pname, float[] value);
    public void glPatchParameteri(int pname, int value);

    public void glBufferData(int target, Buffer data, int mode);
    public void glBufferData(int target, int size, int mode);

    void glTextureParameteri(int textureID, int pname, int mode);
    void glTextureParameteriv(int textureID, int pname, int[] rgba);
    void glTextureParameterf(int textureID, int pname, float mode);
    void glTextureParameterfv(int textureID, int pname, float[] mode);

    int glGetTexLevelParameteri(int target, int level, int pname);

    void glGetTexImage(int target, int level, int format, int type, ByteBuffer result);

    void glGetIntegerv(int pname, IntBuffer values);

    void glTextureView(int dstTexture, int target, int srcTexture, int srcFormat, int minlevel, int numlevels, int minlayer, int numlayers);

    int glCreateTextures(int target);
    void glTextureStorage3D(int textureID, int mipLevels, int format, int width, int height, int depth);

    void glTexStorage3D(int glTexture2dArray, int mipLevels, int format, int width, int height, int depth);

    void glTextureStorage2DMultisample(int textureID, int sampleCount, int format, int width, int height, boolean fixedsamplelocations);
    void glTextureStorage3DMultisample(int textureID, int sampleCount, int format, int width, int height, int arraySize, boolean fixedsamplelocations);
    void glTexStorage2DMultisample(int target, int sampleCount, int format, int width, int height, boolean fixedsamplelocations);
    void glTexImage2DMultisample(int target, int sampleCount, int format, int width, int height, boolean fixedsamplelocations);
    void glTextureStorage2D(int textureID, int mipLevels, int format, int width, int height);

    void glTexStorage3DMultisample(int target, int sampleCount, int format, int width, int height, int arraySize, boolean fixedsamplelocations);
    void glTexImage3DMultisample(int target, int sampleCount, int format, int width, int height, int arraySize, boolean fixedsamplelocations);
    void glTexStorage2D(int target, int mipLevels, int format, int width, int height);
    void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, ByteBuffer data);
    void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, int image_size, long offset);
    void glCompressedTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int boder, ByteBuffer byteBuffer);
    void glCompressedTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int boder, int image_size, long offset);

    void glTextureSubImage2D(int texture, int level, int x_offset, int y_offset, int width, int height, int format, int type, Buffer pixels);
    void glTextureSubImage2D(int texture, int level, int x_offset, int y_offset, int width, int height, int format, int type, long data_offset);

    void glTextureSubImage3D(int textureID, int level, int x_offset, int y_offset, int z_offset, int width, int height, int depth, int format, int type, Buffer pixels);
    void glTextureSubImage3D(int textureID, int level, int x_offset, int y_offset, int z_offset, int width, int height, int depth, int format, int type, long offset);

    int glGetVertexAttribi(int index, int pname);


    void glFramebufferTexture(int target, int attachment, int texture, int level);
    void glFramebufferTexture1D(int target, int attachment, int texturetarget, int texture, int level);
    void glFramebufferTexture3D(int target, int attachment, int texturetarget, int texture, int level, int layer);

    void glBindProgramPipeline(int programPipeline);
    void glDeleteProgramPipeline(int programPipeline);
    int glGenProgramPipeline();
    void glUseProgramStages(int programPipeline, int shaderBit, int program);
    boolean glIsProgramPipeline(int programPipeline);

    void glBindTextures(int first, IntBuffer texturenames);
    void glBindTextureUnit(int unit, int texture);

    void glBindSamplers(int first, IntBuffer samplernames);
    void glDrawElementsInstancedBaseVertex(int mode, int count, int type, int offset, int instance_count, int base_vertex);

    ImageLoader getImageLoader();

    int glGetIntegeri(int panme, int index);
    /**
     * <p><a href="http://www.opengl.org/sdk/docs/man/html/glSampleMaski.xhtml">OpenGL SDK Reference</a></p>
     *
     * Sets the value of a sub-word of the sample mask.
     *
     * @param index which 32-bit sub-word of the sample mask to update
     * @param mask  the new value of the mask sub-word
     */
    void glSampleMaski(int index, int mask);
}
