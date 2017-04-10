package jet.opengl.impl.lwjgl3;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL41;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import jet.opengl.postprocessing.common.GLAPI;
import jet.opengl.postprocessing.common.GLAPIVersion;
import jet.opengl.postprocessing.common.GLFuncProvider;

/**
 * Created by mazhen'gui on 2017/4/10.
 */

public class Lwjgl3OpenglFuncProvider implements GLFuncProvider{
    @Override
    public boolean isSupportExt(String ext) {
        return GLFW.glfwExtensionSupported(ext);
    }

    @Override
    public GLAPI getHostAPI() {
        return GLAPI.LWJGL;
    }

    private static GLAPIVersion g_GLVersion = null;

    @Override
    public GLAPIVersion getGLAPIVersion() {
       if(g_GLVersion == null){
           int major = -1;
           int minor = -1;
           GLCapabilities cap = GL.getCapabilities();
           if(cap.OpenGL45){
               major = 4;
               minor = 5;
           }else if(cap.OpenGL44){
               major = 4;
               minor = 4;
           }else if(cap.OpenGL43){
               major = 4;
               minor = 3;
           }else if(cap.OpenGL42){
               major = 4;
               minor = 2;
           }else if(cap.OpenGL41){
               major = 4;
               minor = 1;
           }else if(cap.OpenGL40){
               major = 4;
               minor = 0;
           }else if(cap.OpenGL33){
               major = 3;
               minor = 3;
           }else if(cap.OpenGL32){
               major = 3;
               minor = 2;
           }else if(cap.OpenGL31){
               major = 3;
               minor = 1;
           }else if(cap.OpenGL30){
               major = 3;
               minor = 0;
           }else{
               // Alomost every machine that support the OpenGL 2.1
               major = 2;
               minor = 1;
           }

           g_GLVersion = new GLAPIVersion(false, major, minor, cap.glBegin != 0L);
       }

        return g_GLVersion;
    }

    @Override
    public void glActiveTexture(int texture) {
        GL13.glActiveTexture(texture);
    }

    @Override
    public void glBindTexture(int target, int texture) {
        GL11.glBindTexture(target, texture);
    }

    @Override
    public void glBlendFunc(int sfactor, int dfactor) {
        GL11.glBlendFunc(sfactor, dfactor);
    }

    @Override
    public void glClear(int mask) {
        GL11.glClear(mask);
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        GL11.glClearColor(red, green, blue, alpha);
    }

    @Override
    public void glClearDepthf(float depth) {
        GL11.glClearDepth(depth);
    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GL11.glColorMask(red, green, blue, alpha);
    }

    @Override
    public void glCullFace(int mode) {
        GL11.glCullFace(mode);
    }

    @Override
    public void glDeleteTexture(int texture) {
        GL11.glDeleteTextures(texture);
    }

    @Override
    public void glDepthFunc(int func) {
        GL11.glDepthFunc(func);
    }

    @Override
    public void glDepthMask(boolean flag) {
        GL11.glDepthMask(flag);
    }

    @Override
    public void glDisable(int cap) {
        GL11.glDisable(cap);
    }

    @Override
    public void glDrawArrays(int mode, int first, int count) {
        GL11.glDrawArrays(mode, first, count);
    }

    @Override
    public void glDrawElements(int mode, int count, int type, Buffer indices) {
        glDrawElements(mode, count, type, MemoryUtil.memAddress0(indices));
    }

    @Override
    public void glEnable(int cap) {
        GL11.glEnable(cap);
    }

    @Override
    public void glFinish() {
        GL11.glFinish();
    }

    @Override
    public void glFlush() {
        GL11.glFlush();
    }

    @Override
    public void glFrontFace(int mode) {
        GL11.glFrontFace(mode);
    }

    @Override
    public int glGenTexture() {
        return GL11.glGenTextures();
    }

    @Override
    public int glGetError() {
        return GL11.glGetError();
    }

    @Override
    public int glGetInteger(int pname) {
        return GL11.glGetInteger(pname);
    }

    @Override
    public void glGetInteger(int pname, IntBuffer params) {
        GL11.glGetIntegerv(pname, params);
    }

    @Override
    public String glGetString(int name) {
        return GL11.glGetString(name);
    }

    @Override
    public void glHint(int target, int mode) {
        GL11.glHint(target, mode);
    }

    @Override
    public void glLineWidth(float width) {
        GL11.glLineWidth(width);
    }

    @Override
    public void glPixelStorei(int pname, int param) {
        GL11.glPixelStorei(pname, param);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, Buffer pixels) {
        GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels != null ? MemoryUtil.memAddress0(pixels): 0);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, long pixels_offset) {
        GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels_offset);
    }

    @Override
    public void glTexParameterf(int target, int pname, float param) {
        GL11.glTexParameterf(target, pname, param);
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, Buffer pixels) {
        GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels != null ? MemoryUtil.memAddress0(pixels): 0);
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, long pixels_offset) {
        GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels_offset);
    }

    @Override
    public void glViewport(int x, int y, int width, int height) {
        GL11.glViewport(x,y,width, height);
    }

    @Override
    public void glAttachShader(int program, int shader) {
        GL20.glAttachShader(program, shader);
    }

    @Override
    public void glBindAttribLocation(int program, int index, CharSequence name) {
        GL20.glBindAttribLocation(program, index, name);
    }

    @Override
    public void glBindBuffer(int target, int buffer) {
        GL15.glBindBuffer(target, buffer);
    }

    @Override
    public void glBindFramebuffer(int target, int framebuffer) {
        GL30.glBindFramebuffer(target, framebuffer);
    }

    @Override
    public void glBindRenderbuffer(int target, int renderbuffer) {
        GL30.glBindRenderbuffer(target, renderbuffer);
    }

    @Override
    public void glBlendColor(float red, float green, float blue, float alpha) {
        GL14.glBlendColor(red, green, blue, alpha);
    }

    @Override
    public void glBlendEquation(int mode) {
        GL14.glBlendEquation(mode);
    }

    @Override
    public void glBlendEquationSeparate(int modeRGB, int modeAlpha) {
        GL20.glBlendEquationSeparate(modeRGB, modeAlpha);
    }

    @Override
    public void glBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        GL14.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
    }

    @Override
    public void glBufferSubData(int target, int offset, Buffer data) {
        if(data instanceof ByteBuffer){
            GL15.glBufferSubData(target, offset, (ByteBuffer)data);
        }else if(data instanceof ShortBuffer){
            GL15.glBufferSubData(target, offset, (ShortBuffer)data);
        }else if(data instanceof IntBuffer){
            GL15.glBufferSubData(target, offset, (IntBuffer)data);
        }else if(data instanceof FloatBuffer){
            GL15.glBufferSubData(target, offset, (FloatBuffer)data);
        }else{
            throw new IllegalArgumentException("Unkown data type: " + data.getClass().getName());
        }

    }

    @Override
    public int glCheckFramebufferStatus(int target) {
        return GL30.glCheckFramebufferStatus(target);
    }

    @Override
    public void glCompileShader(int shader) {
        GL20.glCompileShader(shader);
    }

    @Override
    public int glCreateProgram() {
        return GL20.glCreateProgram();
    }

    @Override
    public int glCreateShader(int type) {
        return GL20.glCreateShader(type);
    }

    @Override
    public void glDeleteBuffer(int buffer) {
        GL15.glDeleteBuffers(buffer);
    }

    @Override
    public void glDeleteFramebuffer(int framebuffer) {
        GL30.glDeleteFramebuffers(framebuffer);
    }

    @Override
    public void glDeleteProgram(int program) {
        GL20.glDeleteProgram(program);
    }

    @Override
    public void glDeleteRenderbuffer(int renderbuffer) {
        GL30.glDeleteRenderbuffers(renderbuffer);
    }

    @Override
    public void glDeleteShader(int shader) {
        GL20.glDeleteShader(shader);
    }

    @Override
    public void glDetachShader(int program, int shader) {
        GL20.glDetachShader(program, shader);
    }

    @Override
    public void glDisableVertexAttribArray(int index) {
        GL20.glDisableVertexAttribArray(index);
    }

    @Override
    public void glDrawElements(int mode, int count, int type, long indices) {
        GL11.glDrawElements(mode, count, type, indices);
    }

    @Override
    public void glEnableVertexAttribArray(int index) {
        GL20.glEnableVertexAttribArray(index);
    }

    @Override
    public void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {
        GL30.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
    }

    @Override
    public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        GL30.glFramebufferTexture2D(target, attachment, textarget, texture, level);
    }

    @Override
    public int glGenBuffer() {
        return GL15.glGenBuffers();
    }

    @Override
    public void glGenBuffers(IntBuffer buffers) {
        GL15.glGenBuffers(buffers);
    }

    @Override
    public void glGenerateMipmap(int target) {
        GL30.glGenerateMipmap(target);
    }

    @Override
    public int glGenFramebuffer() {
        return GL30.glGenFramebuffers();
    }

    @Override
    public int glGenRenderbuffer() {
        return GL30.glGenRenderbuffers();
    }

    @Override
    public String glGetActiveAttrib(int program, int index, int maxLength, IntBuffer size, IntBuffer type) {
        return GL20.glGetActiveAttrib(program, index, maxLength, size, type);
    }

    @Override
    public String glGetActiveUniform(int program, int index, int maxLength, IntBuffer size, IntBuffer type) {
        return GL20.glGetActiveUniform(program, index, maxLength, size, type);
    }

    @Override
    public void glGetAttachedShaders(int program, int[] count, int[] shaders) {
        GL20.glGetAttachedShaders(program, count, shaders);
    }

    @Override
    public int glGetAttribLocation(int program, CharSequence name) {
        return GL20.glGetAttribLocation(program,name);
    }

    @Override
    public boolean glGetBoolean(int pname) {
        return GL11.glGetBoolean(pname);
    }

    @Override
    public void glGetBooleanv(int pname, ByteBuffer params) {
         GL11.glGetBooleanv(pname, params);
    }

    @Override
    public int glGetBufferParameteri(int target, int pname) {
        return GL15.glGetBufferParameteri(target,pname);
    }

    @Override
    public void glGetBufferParameteriv(int target, int pname, IntBuffer params) {
        GL15.glGetBufferParameteriv(target, pname, params);
    }

    @Override
    public float glGetFloat(int pname) {
        return GL11.glGetFloat(pname);
    }

    @Override
    public void glGetFloatv(int pname, FloatBuffer params) {
        GL11.glGetFloatv(pname, params);
    }

    @Override
    public int glGetProgrami(int program, int pname) {
        return GL20.glGetProgrami(program, pname);
    }

    @Override
    public void glGetProgramiv(int program, int pname, IntBuffer params) {
        GL20.glGetProgramiv(program, pname, params);
    }

    @Override
    public String glGetProgramInfoLog(int program) {
        return GL20.glGetProgramInfoLog(program);
    }

    @Override
    public String glGetShaderInfoLog(int shader) {
        return GL20.glGetShaderInfoLog(shader);
    }

    @Override
    public float glGetTexParameterf(int target, int pname) {
        return GL11.glGetTexParameterf(target, pname);
    }

    @Override
    public void glGetTexParameterfv(int target, int pname, FloatBuffer params) {
        GL11.glGetTexParameterfv(target, pname, params);
    }

    @Override
    public int glGetTexParameteri(int target, int pname) {
        return GL11.glGetTexParameteri(target, pname);
    }

    @Override
    public void glGetTexParameteriv(int target, int pname, IntBuffer params) {
        GL11.glGetTexParameteriv(target, pname, params);
    }

    @Override
    public float glGetUniformf(int program, int location) {
        return GL20.glGetUniformf(program, location);
    }

    @Override
    public void glGetUniformfv(int program, int location, FloatBuffer params) {
        GL20.glGetUniformfv(program, location, params);
    }

    @Override
    public int glGetUniformi(int program, int location) {
        return GL20.glGetUniformi(program, location);
    }

    @Override
    public void glGetUniformiv(int program, int location, IntBuffer params) {
        GL20.glGetUniformiv(program, location, params);
    }

    @Override
    public int glGetUniformLocation(int program, CharSequence name) {
        return GL20.glGetUniformLocation(program,name);
    }

    @Override
    public boolean glIsBuffer(int buffer) {
        return GL15.glIsBuffer(buffer);
    }

    @Override
    public boolean glIsEnabled(int cap) {
        return GL11.glIsEnabled(cap);
    }

    @Override
    public boolean glIsFramebuffer(int framebuffer) {
        return GL30.glIsFramebuffer(framebuffer);
    }

    @Override
    public boolean glIsProgram(int program) {
        return GL20.glIsProgram(program);
    }

    @Override
    public boolean glIsRenderbuffer(int renderbuffer) {
        return GL30.glIsRenderbuffer(renderbuffer);
    }

    @Override
    public boolean glIsShader(int shader) {
        return GL20.glIsShader(shader);
    }

    @Override
    public boolean glIsTexture(int texture) {
        return GL11.glIsTexture(texture);
    }

    @Override
    public void glLinkProgram(int program) {
        GL20.glLinkProgram(program);
    }

    @Override
    public void glReleaseShaderCompiler() {
        GL41.glReleaseShaderCompiler();
    }

    @Override
    public void glRenderbufferStorage(int target, int internalformat, int width, int height) {
        GL30.glRenderbufferStorage(target, internalformat, width, height);
    }

    @Override
    public void glSampleCoverage(float value, boolean invert) {
        GL13.glSampleCoverage(value, invert);
    }

    @Override
    public void glShaderBinary(IntBuffer shaders, int binaryformat, ByteBuffer binary) {
        GL41.glShaderBinary(shaders,binaryformat, binary);
    }

    @Override
    public void glShaderSource(int shader, CharSequence string) {
        GL20.glShaderSource(shader, string);
    }

    @Override
    public void glStencilFuncSeparate(int face, int func, int ref, int mask) {
        GL20.glStencilFuncSeparate(face,func, ref, mask);
    }

    @Override
    public void glStencilMaskSeparate(int face, int mask) {
        GL20.glStencilMaskSeparate(face, mask);
    }

    @Override
    public void glStencilOpSeparate(int face, int fail, int zfail, int zpass) {
        GL20.glStencilOpSeparate(face, fail, zfail, zpass);
    }

    @Override
    public void glTexParameterfv(int target, int pname, FloatBuffer params) {
        GL11.glTexParameterfv(target, pname, params);
    }

    @Override
    public void glTexParameteri(int target, int pname, int param) {
        GL11.glTexParameteri(target, pname, param);
    }

    @Override
    public void glTexParameteriv(int target, int pname, int[] params) {
        GL11.glTexParameteriv(target, pname, params);
    }

    @Override
    public void glUniform1f(int location, float x) {
        GL20.glUniform1f(location, x);
    }

    @Override
    public void glUniform1fv(int location, FloatBuffer v) {
        GL20.glUniform1fv(location, v);
    }

    @Override
    public void glUniform1i(int location, int x) {
        GL20.glUniform1i(location, x);
    }

    @Override
    public void glUniform1iv(int location, IntBuffer v) {
        GL20.glUniform1iv(location, v);
    }

    @Override
    public void glUniform2f(int location, float x, float y) {
        GL20.glUniform2f(location, x, y);
    }

    @Override
    public void glUniform2fv(int location, FloatBuffer v) {
        GL20.glUniform2fv(location, v);
    }

    @Override
    public void glUniform2i(int location, int x, int y) {
        GL20.glUniform2i(location, x, y);
    }

    @Override
    public void glUniform2iv(int location, IntBuffer v) {
        GL20.glUniform2iv(location, v);
    }

    @Override
    public void glUniform3f(int location, float x, float y, float z) {
        GL20.glUniform3f(location, x, y, z);
    }

    @Override
    public void glUniform3fv(int location, FloatBuffer v) {
        GL20.glUniform3fv(location, v);
    }

    @Override
    public void glUniform3i(int location, int x, int y, int z) {
        GL20.glUniform3i(location, x, y, z);
    }

    @Override
    public void glUniform3iv(int location, IntBuffer v) {
        GL20.glUniform3iv(location, v);
    }

    @Override
    public void glUniform4f(int location, float x, float y, float z, float w) {
        GL20.glUniform4f(location, x, y, z, w);
    }

    @Override
    public void glUniform4fv(int location, FloatBuffer v) {
        GL20.glUniform4fv(location, v);
    }

    @Override
    public void glUniform4i(int location, int x, int y, int z, int w) {
        GL20.glUniform4i(location, x, y, z, w);
    }

    @Override
    public void glUniform4iv(int location, IntBuffer v) {
        GL20.glUniform4iv(location, v);
    }

    @Override
    public void glUniformMatrix2fv(int location, boolean transpose, FloatBuffer value) {
        GL20.glUniformMatrix2fv(location, transpose, value);
    }

    @Override
    public void glUniformMatrix3fv(int location, boolean transpose, FloatBuffer value) {
        GL20.glUniformMatrix3fv(location, transpose, value);
    }

    @Override
    public void glUniformMatrix4fv(int location, boolean transpose, FloatBuffer value) {
        GL20.glUniformMatrix4fv(location, transpose, value);
    }

    @Override
    public void glUseProgram(int program) {
        GL20.glUseProgram(program);
    }

    @Override
    public void glValidateProgram(int program) {
        GL20.glValidateProgram(program);
    }

    @Override
    public void glVertexAttrib1f(int indx, float x) {
        GL20.glVertexAttrib1f(indx, x);
    }

    @Override
    public void glVertexAttrib1fv(int indx, FloatBuffer values) {
        GL20.glVertexAttrib1fv(indx, values);
    }

    @Override
    public void glVertexAttrib2f(int indx, float x, float y) {
        GL20.glVertexAttrib2f(indx, x, y);
    }

    @Override
    public void glVertexAttrib2fv(int indx, FloatBuffer values) {
        GL20.glVertexAttrib2fv(indx, values);
    }

    @Override
    public void glVertexAttrib3f(int indx, float x, float y, float z) {
        GL20.glVertexAttrib3f(indx, x, y, z);
    }

    @Override
    public void glVertexAttrib3fv(int indx, FloatBuffer values) {
        GL20.glVertexAttrib3fv(indx, values);
    }

    @Override
    public void glVertexAttrib4f(int indx, float x, float y, float z, float w) {
        GL20.glVertexAttrib4f(indx, x, y, z, w);
    }

    @Override
    public void glVertexAttrib4fv(int indx, FloatBuffer values) {
        GL20.glVertexAttrib4fv(indx, values);
    }

    @Override
    public void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, Buffer ptr) {
        GL20.glVertexAttribPointer(indx, size, type, normalized, stride, MemoryUtil.memAddress0(ptr));
    }

    @Override
    public void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, int ptr) {
        GL20.glVertexAttribPointer(indx, size, type, normalized, stride, ptr);
    }

    @Override
    public void glReadBuffer(int mode) {
        GL11.glReadBuffer(mode);
    }

    @Override
    public void glDrawRangeElements(int mode, int start, int end, int count, int type, Buffer indices) {
        GL12.glDrawRangeElements(mode, start, end, count, type, MemoryUtil.memAddress0(indices));
    }

    @Override
    public void glDrawRangeElements(int mode, int start, int end, int count, int type, int offset) {
        GL12.glDrawRangeElements(mode, start, end, count, type, offset);
    }

    @Override
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, Buffer pixels) {
        GL12.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, pixels != null ?MemoryUtil.memAddress0(pixels):0);
    }

    @Override
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, long offset) {
        GL12.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, offset);
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, Buffer pixels) {
        GL12.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, MemoryUtil.memAddress0(pixels));
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, long pixels_offset) {
        GL12.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, pixels_offset);
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, int offset) {
        GL12.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, offset);
    }

    @Override
    public void glCopyTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int x, int y, int width, int height) {
        GL12.glCopyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height);
    }

    @Override
    public int glGenQuery() {
        return GL15.glGenQueries();
    }

    @Override
    public void glGenQueries(IntBuffer ids) {
        GL15.glGenQueries(ids);
    }

    @Override
    public void glDeleteQuery(int query) {
        GL15.glDeleteQueries(query);
    }

    @Override
    public void glDeleteQueries(IntBuffer ids) {
        GL15.glDeleteQueries(ids);
    }

    @Override
    public boolean glIsQuery(int id) {
        return GL15.glIsQuery(id);
    }

    @Override
    public void glBeginQuery(int target, int id) {
        GL15.glBeginQuery(target, id);
    }

    @Override
    public void glEndQuery(int target) {
        GL15.glEndQuery(target);
    }

    @Override
    public int glGetQueryi(int target, int pname) {
        return GL15.glGetQueryi(target, pname);
    }

    @Override
    public void glGetQueryiv(int target, int pname, IntBuffer params) {
        GL15.glGetQueryiv(target, pname, params);
    }

    @Override
    public int glGetQueryObjectuiv(int id, int pname) {
        return GL15.glGetQueryObjectui(id, pname);
    }

    @Override
    public void glGetQueryObjectuiv(int id, int pname, IntBuffer params) {
        GL15.glGetQueryObjectuiv(id, pname, params);
    }

    @Override
    public boolean glUnmapBuffer(int target) {
        return GL15.glUnmapBuffer(target);
    }

    @Override
    public ByteBuffer glGetBufferPointerv(int target, int pname) {
        PointerBuffer buffer = PointerBuffer.allocateDirect(1);
        GL15.glGetBufferPointerv(target, pname, buffer);
        int bufferSize = glGetBufferParameteri(target, GL15.GL_BUFFER_SIZE);
        return buffer.getByteBuffer(bufferSize);
    }

    @Override
    public void glDrawBuffers(int buffer) {
        GL20.glDrawBuffers(buffer);
    }

    @Override
    public void glDrawBuffers(IntBuffer bufs) {
        GL20.glDrawBuffers(bufs);
    }

    @Override
    public void glUniformMatrix2x3fv(int location, boolean transpose, FloatBuffer value) {

    }

    @Override
    public void glUniformMatrix3x2fv(int location, boolean transpose, FloatBuffer value) {

    }

    @Override
    public void glUniformMatrix2x4fv(int location, boolean transpose, FloatBuffer value) {

    }

    @Override
    public void glUniformMatrix4x2fv(int location, boolean transpose, FloatBuffer value) {

    }

    @Override
    public void glUniformMatrix3x4fv(int location, boolean transpose, FloatBuffer value) {

    }

    @Override
    public void glUniformMatrix4x3fv(int location, boolean transpose, FloatBuffer value) {

    }

    @Override
    public void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {

    }

    @Override
    public void glRenderbufferStorageMultisample(int target, int samples, int internalformat, int width, int height) {

    }

    @Override
    public void glFramebufferTextureLayer(int target, int attachment, int texture, int level, int layer) {

    }

    @Override
    public void glFlushMappedBufferRange(int target, int offset, int length) {

    }

    @Override
    public void glBindVertexArray(int array) {

    }

    @Override
    public void glDeleteVertexArray(int vao) {

    }

    @Override
    public void glDeleteVertexArrays(IntBuffer arrays) {

    }

    @Override
    public int glGenVertexArray() {
        return 0;
    }

    @Override
    public void glGenVertexArrays(IntBuffer arrays) {

    }

    @Override
    public boolean glIsVertexArray(int array) {
        return false;
    }

    @Override
    public void glBeginTransformFeedback(int primitiveMode) {

    }

    @Override
    public void glEndTransformFeedback() {

    }

    @Override
    public void glBindBufferRange(int target, int index, int buffer, int offset, int size) {

    }

    @Override
    public void glBindBufferBase(int target, int index, int buffer) {

    }

    @Override
    public void glTransformFeedbackVaryings(int program, CharSequence[] varyings, int bufferMode) {

    }

    @Override
    public void glVertexAttribIPointer(int index, int size, int type, int stride, int offset) {

    }

    @Override
    public void glGetVertexAttribIiv(int index, int pname, IntBuffer params) {

    }

    @Override
    public void glGetVertexAttribIuiv(int index, int pname, IntBuffer params) {

    }

    @Override
    public void glVertexAttribI4i(int index, int x, int y, int z, int w) {

    }

    @Override
    public void glVertexAttribI4ui(int index, int x, int y, int z, int w) {

    }

    @Override
    public void glGetUniformuiv(int program, int location, IntBuffer params) {

    }

    @Override
    public int glGetFragDataLocation(int program, String name) {
        return 0;
    }

    @Override
    public void glUniform1uiv(int location, IntBuffer value) {

    }

    @Override
    public void glUniform3uiv(int location, IntBuffer value) {

    }

    @Override
    public void glUniform4uiv(int location, IntBuffer value) {

    }

    @Override
    public void glClearBufferiv(int buffer, int drawbuffer, IntBuffer value) {

    }

    @Override
    public void glClearBufferuiv(int buffer, int drawbuffer, IntBuffer value) {

    }

    @Override
    public void glClearBufferfv(int buffer, int drawbuffer, FloatBuffer value) {

    }

    @Override
    public void glClearBufferfi(int buffer, int drawbuffer, float depth, int stencil) {

    }

    @Override
    public String glGetStringi(int name, int index) {
        return null;
    }

    @Override
    public void glCopyBufferSubData(int readTarget, int writeTarget, int readOffset, int writeOffset, int size) {

    }

    @Override
    public void glGetUniformIndices(int program, String[] uniformNames, IntBuffer uniformIndices) {

    }

    @Override
    public void glGetActiveUniformsiv(int program, IntBuffer uniformIndices, int pname, IntBuffer params) {

    }

    @Override
    public int glGetUniformBlockIndex(int program, String uniformBlockName) {
        return 0;
    }

    @Override
    public void glGetActiveUniformBlockiv(int program, int uniformBlockIndex, int pname, IntBuffer params) {

    }

    @Override
    public void glGetActiveUniformBlockName(int program, int uniformBlockIndex, IntBuffer length, ByteBuffer uniformBlockName) {

    }

    @Override
    public String glGetActiveUniformBlockName(int program, int uniformBlockIndex) {
        return null;
    }

    @Override
    public void glUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding) {

    }

    @Override
    public void glDrawArraysInstanced(int mode, int first, int count, int instanceCount) {

    }

    @Override
    public void glDrawElementsInstanced(int mode, int count, int type, int indicesOffset, int instanceCount) {

    }

    @Override
    public void glGetInteger64v(int pname, LongBuffer params) {

    }

    @Override
    public void glGetBufferParameteri64v(int target, int pname, LongBuffer params) {

    }

    @Override
    public int glGenSampler() {
        return 0;
    }

    @Override
    public void glGenSamplers(IntBuffer samplers) {

    }

    @Override
    public int glGenSamplers() {
        return 0;
    }

    @Override
    public void glDeleteSampler(int sampler) {

    }

    @Override
    public void glDeleteSamplers(IntBuffer samplers) {

    }

    @Override
    public boolean glIsSampler(int sampler) {
        return false;
    }

    @Override
    public void glBindSampler(int unit, int sampler) {

    }

    @Override
    public void glSamplerParameteri(int sampler, int pname, int param) {

    }

    @Override
    public void glSamplerParameteriv(int sampler, int pname, IntBuffer param) {

    }

    @Override
    public void glSamplerParameterf(int sampler, int pname, float param) {

    }

    @Override
    public void glSamplerParameterfv(int sampler, int pname, FloatBuffer param) {

    }

    @Override
    public void glGetSamplerParameteriv(int sampler, int pname, IntBuffer params) {

    }

    @Override
    public void glGetSamplerParameterfv(int sampler, int pname, FloatBuffer params) {

    }

    @Override
    public void glVertexAttribDivisor(int index, int divisor) {

    }

    @Override
    public void glBindTransformFeedback(int target, int id) {

    }

    @Override
    public void glDeleteTransformFeedback(int feedback) {

    }

    @Override
    public void glDeleteTransformFeedbacks(IntBuffer ids) {

    }

    @Override
    public int glGenTransformFeedback() {
        return 0;
    }

    @Override
    public void glGenTransformFeedbacks(IntBuffer ids) {

    }

    @Override
    public boolean glIsTransformFeedback(int id) {
        return false;
    }

    @Override
    public void glPauseTransformFeedback() {

    }

    @Override
    public void glResumeTransformFeedback() {

    }

    @Override
    public void glProgramParameteri(int program, int pname, int value) {

    }

    @Override
    public void glInvalidateFramebuffer(int target, IntBuffer attachments) {

    }

    @Override
    public void glInvalidateSubFramebuffer(int target, IntBuffer attachments, int x, int y, int width, int height) {

    }

    @Override
    public ByteBuffer glGetProgramBinary(int program, IntBuffer format) {
        return null;
    }

    @Override
    public int glGenTextures() {
        return 0;
    }

    @Override
    public void glBindImageTexture(int unit, int texture, int level, boolean layered, int layer, int access, int format) {

    }

    @Override
    public void glDispatchCompute(int num_groups_x, int num_groups_y, int num_groups_z) {

    }

    @Override
    public void glMemoryBarrier(int barriers) {

    }

    @Override
    public int glGetSubroutineIndex(int program, int shader, String name) {
        return 0;
    }

    @Override
    public void glUniformSubroutinesui(int shaderType, int index) {

    }

    @Override
    public void glPolygonMode(int face, int mode) {

    }

    @Override
    public void glPatchParameterfv(int pname, Buffer value) {

    }

    @Override
    public void glPatchParameteri(int pname, int value) {

    }

    @Override
    public int glGenBuffers() {
        return 0;
    }

    @Override
    public void glBufferData(int target, Buffer data, int mode) {

    }

    @Override
    public int glGenVertexArrays() {
        return 0;
    }

    @Override
    public void glTextureParameteri(int textureID, int pname, int mode) {

    }

    @Override
    public void glTextureParameteriv(int textureID, int pname, int[] rgba) {

    }

    @Override
    public int glGetTexLevelParameteri(int target, int level, int pname) {
        return 0;
    }

    @Override
    public void glGetTexImage(int target, int level, int format, int type, ByteBuffer result) {

    }

    @Override
    public void glGetIntegerv(int pname, IntBuffer values) {

    }

    @Override
    public void glTextureView(int dstTexture, int target, int srcTexture, int srcFormat, int minlevel, int numlevels, int minlayer, int numlayers) {

    }

    @Override
    public int glCreateTextures(int target) {
        return 0;
    }

    @Override
    public void glTextureStorage3D(int textureID, int mipLevels, int format, int width, int height, int depth) {

    }

    @Override
    public void glTexStorage3D(int glTexture2dArray, int mipLevels, int format, int width, int height, int depth) {

    }

    @Override
    public void glTextureStorage2DMultisample(int textureID, int sampleCount, int format, int width, int height, boolean fixedsamplelocations) {

    }

    @Override
    public void glTextureStorage3DMultisample(int textureID, int sampleCount, int format, int width, int height, int arraySize, boolean fixedsamplelocations) {

    }

    @Override
    public void glTexStorage2DMultisample(int target, int sampleCount, int format, int width, int height, boolean fixedsamplelocations) {

    }

    @Override
    public void glTexImage2DMultisample(int target, int sampleCount, int format, int width, int height, boolean fixedsamplelocations) {

    }

    @Override
    public void glTextureStorage2D(int textureID, int mipLevels, int format, int width, int height) {

    }

    @Override
    public void glTexStorage3DMultisample(int target, int sampleCount, int format, int width, int height, int arraySize, boolean fixedsamplelocations) {

    }

    @Override
    public void glTexImage3DMultisample(int target, int sampleCount, int format, int width, int height, int arraySize, boolean fixedsamplelocations) {

    }

    @Override
    public void glTexStorage2D(int target, int mipLevels, int format, int width, int height) {

    }

    @Override
    public void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, Buffer data) {

    }

    @Override
    public void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, long data_offset) {

    }

    @Override
    public void glCompressedTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int boder, Buffer byteBuffer) {

    }

    @Override
    public void glCompressedTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int boder, long data_offset) {

    }

    @Override
    public void glTextureSubImage2D(int texture, int level, int x_offset, int y_offset, int width, int height, int format, int type, Buffer pixels) {

    }

    @Override
    public void glTextureSubImage2D(int texture, int level, int x_offset, int y_offset, int width, int height, int format, int type, long data_offset) {

    }

    @Override
    public void glTextureSubImage3D(int textureID, int level, int x_offset, int y_offset, int z_offset, int width, int height, int depth, int format, int type, Buffer pixels) {

    }

    @Override
    public void glTextureSubImage3D(int textureID, int level, int x_offset, int y_offset, int z_offset, int width, int height, int depth, int format, int type, long offset) {

    }

    @Override
    public void glDeleteBuffers(int m_vbo) {

    }

    @Override
    public int glGetVertexAttribi(int index, int pname) {
        return 0;
    }
}
