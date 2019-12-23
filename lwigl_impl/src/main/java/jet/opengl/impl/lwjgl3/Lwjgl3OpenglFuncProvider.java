package jet.opengl.impl.lwjgl3;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.ARBGetProgramBinary;
import org.lwjgl.opengl.ARBMultiBind;
import org.lwjgl.opengl.ARBSeparateShaderObjects;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL41;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL44;
import org.lwjgl.opengl.GL45;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.NVXGPUMemoryInfo;
import org.lwjgl.system.MemoryUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import jet.opengl.postprocessing.common.GLAPI;
import jet.opengl.postprocessing.common.GLAPIVersion;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GPUMemoryInfo;
import jet.opengl.postprocessing.texture.NativeAPI;

/**
 * Created by mazhen'gui on 2017/4/10.
 */

public class Lwjgl3OpenglFuncProvider implements GLFuncProvider{
    private Lwjgl3ImageLoader m_ImageLoader;
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
    public void glClearStencil(int s) {
        GL11.glClearStencil(s);
    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GL11.glColorMask(red, green, blue, alpha);
    }

    @Override
    public void glCopyTexImage2D(int target, int level, int internalformat, int x, int y, int width, int height, int border) {
        GL11.glCopyTexImage2D(target, level, internalformat, x, y, width, height, border);
    }

    @Override
    public void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height) {
        GL11.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
    }

    @Override
    public void glCullFace(int mode) {
        GL11.glCullFace(mode);
    }

    @Override
    public void glDeleteTextures(int... textures) {
        if(textures== null)
            return;

        glDeleteTextures(textures, 0, textures.length);
    }

    @Override
    public void glDeleteTextures(int[] textures, int offset, int length) {
        if(offset == 0 && length == textures.length){
            GL11.glDeleteTextures(textures);
        }else{
            int[] copy_texs = Arrays.copyOfRange(textures, offset, offset + length);
            GL11.glDeleteTextures(copy_texs);
        }
    }

    @Override
    public void glDeleteTextures(IntBuffer textures) {
        GL11.glDeleteTextures(textures);
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
    public void glEnablei(int cap, int index) {
        GL30.glEnablei(cap, index);
    }

    @Override
    public void glDisablei(int cap, int index) {GL30.glDisablei(cap, index);}

    @Override
    public boolean glIsEnabledi(int cap, int index) {
        return GL30.glIsEnabledi(cap, index);
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
    public void glGenTextures(IntBuffer textures) {
        GL11.glGenTextures(textures);
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
    public void glPolygonOffset(float factor, float units) {
        GL11.glPolygonOffset(factor, units);
    }

    @Override
    public void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer pixels) {
        GL11.glReadPixels(x, y, width, height, format, type, MemoryUtil.memAddressSafe(pixels));
    }

    @Override
    public void glReadPixels(int x, int y, int width, int height, int format, int type, long offset) {
        GL11.glReadPixels(x, y, width, height, format, type, offset);
    }

    @Override
    public void glScissor(int x, int y, int width, int height) {
        GL11.glScissor(x, y, width, height);
    }

    @Override
    public void glStencilFunc(int func, int ref, int mask) {
        GL11.glStencilFunc(func, ref, mask);
    }

    @Override
    public void glStencilMask(int mask) {
        GL11.glStencilMask(mask);
    }

    @Override
    public void glStencilOp(int fail, int zfail, int zpass) {
        GL11.glStencilOp(fail, zfail, zpass);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, Buffer pixels) {
        GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels != null ? MemoryUtil.memAddress0(pixels): 0);
    }

    @Override
    public void glTexImage1D(int target, int level, int internalformat, int width, int border, int format, int type, Buffer pixels) {
        GL11.glTexImage1D(target, level, internalformat, width, border, format, type, pixels != null ? MemoryUtil.memAddress0(pixels): 0);
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
    public void glBlendEquationi(int buf, int mode) {
        GL40.glBlendEquationi(buf, mode);
    }

    @Override
    public void glBlendEquationSeparatei(int buf, int modeRGB, int modeAlpha) {
        GL40.glBlendEquationSeparatei(buf, modeRGB, modeAlpha);
    }

    @Override
    public void glBlendFunci(int buf, int sfactor, int dfactor) {
        GL40.glBlendFunci(buf,sfactor,dfactor);
    }

    @Override
    public void glBlendFuncSeparatei(int buf, int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        GL40.glBlendFuncSeparatei(buf, srcRGB, dstRGB, srcAlpha, dstAlpha);
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
    public void glDeleteBuffers(IntBuffer buffers) {
        GL15.glDeleteBuffers(buffers);
    }

    @Override
    public void glDeleteFramebuffer(int framebuffer) {
        GL30.glDeleteFramebuffers(framebuffer);
    }

    @Override
    public void glDeleteFramebuffers(IntBuffer framebuffers) {
        GL30.glDeleteFramebuffers(framebuffers);
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
    public void glDeleteRenderbuffers(IntBuffer renderbuffers) {
        GL30.glDeleteRenderbuffers(renderbuffers);
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
    public void glGenFramebuffers(IntBuffer framebuffers) {
        GL30.glGenFramebuffers(framebuffers);
    }

    @Override
    public int glGenRenderbuffer() {
        return GL30.glGenRenderbuffers();
    }

    @Override
    public void glGenRenderbuffers(IntBuffer renderbuffers) {
        GL30.glGenRenderbuffers(renderbuffers);
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
    public int glGetShaderi(int shader, int pname) {
        return GL20.glGetShaderi(shader, pname);
    }

    @Override
    public void glGetShaderiv(int shader, int pname, IntBuffer params) {
        GL20.glGetShaderiv(shader, pname, params);
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
    public void glBeginQueryIndexed(int target, int index, int id) {GL40.glBeginQueryIndexed(target, index, id);}

    @Override
    public void glEndQuery(int target) {
        GL15.glEndQuery(target);
    }

    @Override
    public void glEndQueryIndexed(int target, int index) {
        GL40.glEndQueryIndexed(target, index);
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
        GL21.glUniformMatrix2x3fv(location, transpose, value);
    }

    @Override
    public void glUniformMatrix3x2fv(int location, boolean transpose, FloatBuffer value) {
        GL21.glUniformMatrix3x2fv(location, transpose, value);
    }

    @Override
    public void glUniformMatrix2x4fv(int location, boolean transpose, FloatBuffer value) {
        GL21.glUniformMatrix2x4fv(location, transpose, value);
    }

    @Override
    public void glUniformMatrix4x2fv(int location, boolean transpose, FloatBuffer value) {
        GL21.glUniformMatrix4x2fv(location, transpose, value);
    }

    @Override
    public void glUniformMatrix3x4fv(int location, boolean transpose, FloatBuffer value) {
        GL21.glUniformMatrix3x4fv(location, transpose, value);
    }

    @Override
    public void glUniformMatrix4x3fv(int location, boolean transpose, FloatBuffer value) {
        GL21.glUniformMatrix4x3fv(location, transpose, value);
    }

    @Override
    public void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        GL30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    @Override
    public void glRenderbufferStorageMultisample(int target, int samples, int internalformat, int width, int height) {
        GL30.glRenderbufferStorageMultisample(target, samples, internalformat, width, height);
    }

    @Override
    public void glFramebufferTextureLayer(int target, int attachment, int texture, int level, int layer) {
        GL30.glFramebufferTextureLayer(target, attachment, texture, level, layer);
    }

    @Override
    public void glFlushMappedBufferRange(int target, int offset, int length) {
        GL30.glFlushMappedBufferRange(target, offset, length);
    }

    @Override
    public void glBindVertexArray(int array) {
        GL30.glBindVertexArray(array);
    }

    @Override
    public void glDeleteVertexArray(int vao) {
        GL30.glDeleteVertexArrays(vao);
    }

    @Override
    public void glDeleteVertexArrays(IntBuffer arrays) {
        GL30.glDeleteVertexArrays(arrays);
    }

    @Override
    public int glGenVertexArray() {
        return GL30.glGenVertexArrays();
    }

    @Override
    public void glGenVertexArrays(IntBuffer arrays) {
        GL30.glGenVertexArrays(arrays);
    }

    @Override
    public boolean glIsVertexArray(int array) {
        return GL30.glIsVertexArray(array);
    }

    @Override
    public void glBeginTransformFeedback(int primitiveMode) {
        GL30.glBeginTransformFeedback(primitiveMode);
    }

    @Override
    public void glEndTransformFeedback() {
        GL30.glEndTransformFeedback();
    }

    @Override
    public void glBindBufferRange(int target, int index, int buffer, int offset, int size) {
        GL30.glBindBufferRange(target, index, buffer, offset, size);
    }

    @Override
    public void glBindBufferBase(int target, int index, int buffer) {
        GL30.glBindBufferBase(target, index, buffer);
    }

    @Override
    public void glTransformFeedbackVaryings(int program, CharSequence[] varyings, int bufferMode) {
        GL30.glTransformFeedbackVaryings(program, varyings, bufferMode);
    }

    @Override
    public void glVertexAttribIPointer(int index, int size, int type, int stride, int offset) {
        GL30.glVertexAttribIPointer(index, size, type, stride, offset);
    }

    @Override
    public void glGetVertexAttribIiv(int index, int pname, IntBuffer params) {
        GL30.glGetVertexAttribIiv(index, pname, params);
    }

    @Override
    public void glGetVertexAttribIuiv(int index, int pname, IntBuffer params) {
        GL30.glGetVertexAttribIuiv(index, pname, params);
    }

    @Override
    public void glVertexAttribI4i(int index, int x, int y, int z, int w) {
        GL30.glVertexAttribI4i(index, x, y, z, w);
    }

    @Override
    public void glVertexAttribI4ui(int index, int x, int y, int z, int w) {
        GL30.glVertexAttribI4ui(index, x, y, z, w);
    }

    @Override
    public void glGetUniformuiv(int program, int location, IntBuffer params) {
        GL30.glGetUniformuiv(program, location, params);
    }

    @Override
    public int glGetFragDataLocation(int program, String name) {
        return GL30.glGetFragDataLocation(program, name);
    }

    @Override
    public void glUniform1uiv(int location, IntBuffer value) {
        GL30.glUniform1uiv(location, value);
    }

    @Override
    public void glUniform3uiv(int location, IntBuffer value) {
        GL30.glUniform3uiv(location, value);
    }

    @Override
    public void glUniform4uiv(int location, IntBuffer value) {
        GL30.glUniform4uiv(location, value);
    }

    @Override
    public void glClearBufferiv(int buffer, int drawbuffer, IntBuffer value) {
        GL30.glClearBufferiv(buffer, drawbuffer, value);
    }

    @Override
    public void glClearBufferuiv(int buffer, int drawbuffer, IntBuffer value) {
        GL30.glClearBufferuiv(buffer, drawbuffer, value);
    }

    @Override
    public void glClearBufferfv(int buffer, int drawbuffer, FloatBuffer value) {
        GL30.glClearBufferfv(buffer, drawbuffer, value);
    }

    @Override
    public void glClearBufferfi(int buffer, int drawbuffer, float depth, int stencil) {
        GL30.glClearBufferfi(buffer, drawbuffer, depth, stencil);
    }

    @Override
    public String glGetStringi(int name, int index) {
        return GL30.glGetStringi(name,index);
    }

    @Override
    public void glCopyBufferSubData(int readTarget, int writeTarget, int readOffset, int writeOffset, int size) {
        GL31.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
    }

    @Override
    public void glGetUniformIndices(int program, String[] uniformNames, IntBuffer uniformIndices) {
        GL31.glGetUniformIndices(program, uniformNames, uniformIndices);
    }

    @Override
    public void glGetActiveUniformsiv(int program, IntBuffer uniformIndices, int pname, IntBuffer params) {
        GL31.glGetActiveUniformsiv(program, uniformIndices, pname, params);
    }

    @Override
    public int glGetUniformBlockIndex(int program, String uniformBlockName) {
        return GL31.glGetUniformBlockIndex(program, uniformBlockName);
    }

    @Override
    public void glGetActiveUniformBlockiv(int program, int uniformBlockIndex, int pname, IntBuffer params) {
        GL31.glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, params);
    }

    @Override
    public void glGetActiveUniformBlockName(int program, int uniformBlockIndex, IntBuffer length, ByteBuffer uniformBlockName) {
        GL31.glGetActiveUniformBlockName(program, uniformBlockIndex, length, uniformBlockName);
    }

    @Override
    public String glGetActiveUniformBlockName(int program, int uniformBlockIndex) {
        return GL31.glGetActiveUniformBlockName(program, uniformBlockIndex);
    }

    @Override
    public void glUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding) {
        GL31.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
    }

    @Override
    public void glDrawArraysInstanced(int mode, int first, int count, int instanceCount) {
        GL31.glDrawArraysInstanced(mode, first, count, instanceCount);
    }

    @Override
    public void glDrawElementsInstanced(int mode, int count, int type, int indicesOffset, int instanceCount) {
        GL31.glDrawElementsInstanced(mode, count, type, indicesOffset, instanceCount);
    }

    @Override
    public void glGetInteger64v(int pname, LongBuffer params) {
        GL32.glGetInteger64v(pname, params);
    }

    @Override
    public void glGetBufferParameteri64v(int target, int pname, LongBuffer params) {
        GL32.glGetBufferParameteri64v(target, pname, params);
    }

    @Override
    public int glGenSampler() {
        return GL33.glGenSamplers();
    }

    @Override
    public void glGenSamplers(IntBuffer samplers) {
        GL33.glGenSamplers(samplers);
    }

    @Override
    public void glDeleteSampler(int sampler) {
        GL33.glDeleteSamplers(sampler);
    }

    @Override
    public void glDeleteSamplers(IntBuffer samplers) {
        GL33.glDeleteSamplers(samplers);
    }

    @Override
    public boolean glIsSampler(int sampler) {
        return GL33.glIsSampler(sampler);
    }

    @Override
    public void glBindSampler(int unit, int sampler) {
        GL33.glBindSampler(unit, sampler);
    }

    @Override
    public void glSamplerParameteri(int sampler, int pname, int param) {
        GL33.glSamplerParameteri(sampler, pname, param);
    }

    @Override
    public void glSamplerParameteriv(int sampler, int pname, IntBuffer param) {
        GL33.glSamplerParameteriv(sampler, pname, param);
    }

    @Override
    public void glSamplerParameterf(int sampler, int pname, float param) {
        GL33.glSamplerParameterf(sampler, pname, param);
    }

    @Override
    public void glSamplerParameterfv(int sampler, int pname, FloatBuffer param) {
        GL33.glSamplerParameterfv(sampler, pname, param);
    }

    @Override
    public void glGetSamplerParameteriv(int sampler, int pname, IntBuffer params) {
        GL33.glGetSamplerParameteriv(sampler, pname, params);
    }

    @Override
    public void glGetSamplerParameterfv(int sampler, int pname, FloatBuffer params) {
        GL33.glGetSamplerParameterfv(sampler, pname, params);
    }

    @Override
    public void glVertexAttribDivisor(int index, int divisor) {
        GL33.glVertexAttribDivisor(index,divisor);
    }

    @Override
    public void glBindTransformFeedback(int target, int id) {
        GL40.glBindTransformFeedback(target, id);
    }

    @Override
    public void glDeleteTransformFeedback(int feedback) {
        GL40.glDeleteTransformFeedbacks(feedback);
    }

    @Override
    public void glDeleteTransformFeedbacks(IntBuffer ids) {
        GL40.glDeleteTransformFeedbacks(ids);
    }

    @Override
    public int glGenTransformFeedback() {
        return GL40.glGenTransformFeedbacks();
    }

    @Override
    public void glGenTransformFeedbacks(IntBuffer ids) {
        GL40.glGenTransformFeedbacks(ids);
    }

    @Override
    public boolean glIsTransformFeedback(int id) {
        return GL40.glIsTransformFeedback(id);
    }

    @Override
    public void glPauseTransformFeedback() {
        GL40.glPauseTransformFeedback();
    }

    @Override
    public void glResumeTransformFeedback() {
        GL40.glResumeTransformFeedback();
    }

    @Override
    public void glProgramBinary(int program, int binaryFormat, ByteBuffer binary) {
        ARBGetProgramBinary.glProgramBinary(program, binaryFormat, binary);
    }

    @Override
    public void glProgramParameteri(int program, int pname, int value) {
        GL41.glProgramParameteri(program, pname, value);
    }

    @Override
    public void glInvalidateFramebuffer(int target, IntBuffer attachments) {
        GL43.glInvalidateFramebuffer(target, attachments);
    }

    @Override
    public void glInvalidateSubFramebuffer(int target, IntBuffer attachments, int x, int y, int width, int height) {
        GL43.glInvalidateSubFramebuffer(target, attachments, x, y, width, height);
    }

    @Override
    public void glGetProgramBinary(int program, int[] length, int[] binaryFormat, ByteBuffer binary) {
        GL41.glGetProgramBinary(program, length, binaryFormat, binary);
    }

    @Override
    public void glBindImageTexture(int unit, int texture, int level, boolean layered, int layer, int access, int format) {
        GL42.glBindImageTexture(unit, texture, level, layered, layer, access, format);
    }

    @Override
    public void glDispatchCompute(int num_groups_x, int num_groups_y, int num_groups_z) {
        GL43.glDispatchCompute(num_groups_x, num_groups_y, num_groups_z);
    }

    @Override
    public void glMemoryBarrier(int barriers) {
        GL42.glMemoryBarrier(barriers);
    }

    @Override
    public void glPolygonMode(int face, int mode) {
        GL11.glPolygonMode(face, mode);
    }

    @Override
    public void glPatchParameterfv(int pname, float[] value) {
        GL40.glPatchParameterfv(pname, value);
    }

    @Override
    public void glPatchParameteri(int pname, int value) {
        GL40.glPatchParameteri(pname, value);
    }

    @Override
    public void glBufferData(int target, Buffer data, int mode) {
        if(data instanceof ByteBuffer){
            GL15.glBufferData(target, (ByteBuffer)data, mode);
        }else if(data instanceof ShortBuffer){
            GL15.glBufferData(target, (ShortBuffer)data, mode);
        }else if(data instanceof IntBuffer){
            GL15.glBufferData(target, (IntBuffer)data, mode);
        }else if(data instanceof FloatBuffer){
            GL15.glBufferData(target, (FloatBuffer)data, mode);
        }else if(data instanceof DoubleBuffer){
            GL15.glBufferData(target, (DoubleBuffer)data, mode);
        }else{
            throw new IllegalArgumentException("Unkown data type:" + data.getClass().getName());
        }
    }

    public void glBufferData(int target, int size, int mode){
        GL15.glBufferData(target, size, mode);
    }

    @Override
    public void glTextureParameteri(int textureID, int pname, int mode) {
        ARBDirectStateAccess.glTextureParameteri(textureID, pname, mode);
    }

    @Override
    public void glTextureParameteriv(int textureID, int pname, int[] rgba) {
        ARBDirectStateAccess.glTextureParameteriv(textureID, pname, rgba);
    }

    @Override
    public void glTextureParameterf(int textureID, int pname, float mode) {
        ARBDirectStateAccess.glTextureParameterf(textureID, pname, mode);
    }

    @Override
    public void glTextureParameterfv(int textureID, int pname, float[] mode) {
        ARBDirectStateAccess.glTextureParameterfv(textureID, pname, mode);
    }

    @Override
    public int glGetTexLevelParameteri(int target, int level, int pname) {
        return GL11.glGetTexLevelParameteri(target, level, pname);
    }

    @Override
    public void glGetTexImage(int target, int level, int format, int type, ByteBuffer result) {
        GL11.glGetTexImage(target, level, format, type, result);
    }

    @Override
    public void glGetIntegerv(int pname, IntBuffer values) {
        GL11.glGetIntegerv(pname, values);
    }

    @Override
    public void glTextureView(int dstTexture, int target, int srcTexture, int srcFormat, int minlevel, int numlevels, int minlayer, int numlayers) {
        GL43.glTextureView(dstTexture, target, srcTexture, srcFormat, minlevel, numlevels, minlayer, numlayers);
    }

    @Override
    public int glCreateTextures(int target) {
        return ARBDirectStateAccess.glCreateTextures(target);
    }

    @Override
    public void glTextureStorage3D(int textureID, int mipLevels, int format, int width, int height, int depth) {
        GL45.glTextureStorage3D(textureID, mipLevels, format, width, height, depth);
    }

    @Override
    public void glTexStorage3D(int textureID, int mipLevels, int format, int width, int height, int depth) {
        GL42.glTexStorage3D(textureID, mipLevels, format, width, height, depth);
    }

    @Override
    public void glTextureStorage2DMultisample(int textureID, int sampleCount, int format, int width, int height, boolean fixedsamplelocations) {
        ARBDirectStateAccess.glTextureStorage2DMultisample(textureID, sampleCount, format, width, height, fixedsamplelocations);
    }

    @Override
    public void glTextureStorage3DMultisample(int textureID, int sampleCount, int format, int width, int height, int arraySize, boolean fixedsamplelocations) {
        ARBDirectStateAccess.glTextureStorage3DMultisample(textureID, sampleCount, format, width, height, arraySize, fixedsamplelocations);
    }

    @Override
    public void glTexStorage2DMultisample(int target, int sampleCount, int format, int width, int height, boolean fixedsamplelocations) {
        GL43.glTexStorage2DMultisample(target, sampleCount, format, width, height, fixedsamplelocations);
    }

    @Override
    public void glTexImage2DMultisample(int target, int sampleCount, int format, int width, int height, boolean fixedsamplelocations) {
        GL32.glTexImage2DMultisample(target, sampleCount,format, width, height, fixedsamplelocations);
    }

    @Override
    public void glTextureStorage2D(int textureID, int mipLevels, int format, int width, int height) {
        ARBDirectStateAccess.glTextureStorage2D(textureID, mipLevels, format, width, height);
    }

    @Override
    public void glTexStorage3DMultisample(int target, int sampleCount, int format, int width, int height, int arraySize, boolean fixedsamplelocations) {
        GL43.glTexStorage3DMultisample(target, sampleCount, format, width, height, arraySize, fixedsamplelocations);
    }

    @Override
    public void glTexImage3DMultisample(int target, int sampleCount, int format, int width, int height, int arraySize, boolean fixedsamplelocations) {
        GL32.glTexImage3DMultisample(target, sampleCount, format, width, height, arraySize, fixedsamplelocations);
    }

    @Override
    public void glTexStorage2D(int target, int mipLevels, int format, int width, int height) {
        GL42.glTexStorage2D(target, mipLevels, format, width, height);
    }

    @Override
    public void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, ByteBuffer data) {
        GL13.glCompressedTexImage2D(target, level, internalformat, width, height, border, data);
    }

    @Override
    public void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, int image_size, long offset) {
        GL13.glCompressedTexImage2D(target, level, internalformat, width, height, border, image_size, offset);
    }

    @Override
    public void glCompressedTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int boder, ByteBuffer byteBuffer) {
        GL13.glCompressedTexImage3D(target, level, internalformat, width, height, depth, boder, byteBuffer);
    }

    @Override
    public void glCompressedTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int boder, int image_size, long offset) {
        GL13.glCompressedTexImage3D(target, level, internalformat, width, height, depth, boder, image_size, offset);
    }

    @Override
    public void glTextureSubImage2D(int texture, int level, int x_offset, int y_offset, int width, int height, int format, int type, Buffer pixels) {
        ARBDirectStateAccess.glTextureSubImage2D(texture, level, x_offset, y_offset, width, height, format, type, pixels != null ? MemoryUtil.memAddress0(pixels): 0);
    }

    @Override
    public void glTextureSubImage2D(int texture, int level, int x_offset, int y_offset, int width, int height, int format, int type, long data_offset) {
        ARBDirectStateAccess.glTextureSubImage2D(texture, level, x_offset, y_offset, width, height, format, type, data_offset);
    }

    @Override
    public void glTextureSubImage3D(int textureID, int level, int x_offset, int y_offset, int z_offset, int width, int height, int depth, int format, int type, Buffer pixels) {
        ARBDirectStateAccess.glTextureSubImage3D(textureID, level, x_offset, y_offset, z_offset, width, height, depth, format, type, pixels != null ? MemoryUtil.memAddress0(pixels): 0);
    }

    @Override
    public void glTextureSubImage3D(int textureID, int level, int x_offset, int y_offset, int z_offset, int width, int height, int depth, int format, int type, long offset) {
        ARBDirectStateAccess.glTextureSubImage3D(textureID, level, x_offset, y_offset, z_offset, width, height, depth, format, type, offset);
    }

    @Override
    public int glGetVertexAttribi(int index, int pname) {
        return GL20.glGetVertexAttribi(index, pname);
    }

    @Override
    public void glFramebufferTexture(int target, int i, int texture, int level) {
        GL32.glFramebufferTexture(target, i, texture, level);
    }

    @Override
    public void glFramebufferTexture1D(int glFramebuffer, int i, int glTexture1d, int texture, int level) {
        GL30.glFramebufferTexture1D(glFramebuffer, i, glTexture1d, texture, level);
    }

    @Override
    public void glFramebufferTexture3D(int glFramebuffer, int i, int glTexture3d, int texture, int level, int layer) {
        GL30.glFramebufferTexture3D(glFramebuffer, i, glTexture3d, texture, level, layer);
    }

    @Override
    public void glBindProgramPipeline(int programPipeline) {
        ARBSeparateShaderObjects.glBindProgramPipeline(programPipeline);
    }

    @Override
    public void glDeleteProgramPipeline(int programPipeline) {
        ARBSeparateShaderObjects.glDeleteProgramPipelines(programPipeline);
    }

    @Override
    public int glGenProgramPipeline() {
        return ARBSeparateShaderObjects.glGenProgramPipelines();
    }

    @Override
    public void glUseProgramStages(int programPipeline, int shaderBit, int program) {
        ARBSeparateShaderObjects.glUseProgramStages(programPipeline, shaderBit, program);
    }

    @Override
    public boolean glIsProgramPipeline(int programPipeline) {
        return ARBSeparateShaderObjects.glIsProgramPipeline(programPipeline);
    }

    @Override
    public void glBindTextures(int first, IntBuffer texturenames) {
        ARBMultiBind.glBindTextures(first, texturenames);
    }

    @Override
    public void glBindTextureUnit(int unit, int texture) {
        GL45.glBindTextureUnit(unit, texture);
    }

    @Override
    public void glBindSamplers(int first, IntBuffer samplernames) {
        ARBMultiBind.glBindSamplers(first, samplernames);
    }

    @Override
    public void glDrawElementsInstancedBaseVertex(int mode, int count, int type, int offset, int instance_count, int base_vertex) {
        GL32.glDrawElementsInstancedBaseVertex(mode, count, type, offset, instance_count, base_vertex);
    }

    @Override
    public NativeAPI getNativeAPI() {
        if(m_ImageLoader == null)
            m_ImageLoader = new Lwjgl3ImageLoader();

        return m_ImageLoader;
    }

    @Override
    public int glGetIntegeri(int panme, int index) {
        return GL30.glGetIntegeri(panme, index);
    }

    @Override
    public void glSampleMaski(int index, int mask) {
        GL32.glSampleMaski(index, mask);
    }

    @Override
    public void glCopyImageSubData(int srcName, int srcTarget, int srcLevel, int srcX, int srcY, int srcZ, int dstName, int dstTarget, int dstLevel, int dstX, int dstY, int dstZ, int srcWidth, int srcHeight, int srcDepth) {
        GL43.glCopyImageSubData(srcName, srcTarget, srcLevel, srcX, srcY, srcZ, dstName, dstTarget, dstLevel, dstX, dstY, dstZ, srcWidth, srcHeight, srcDepth);
    }

    @Override
    public void glPrimitiveRestartIndex(int restartIndex) {
        GL31.glPrimitiveRestartIndex(restartIndex);
    }

    @Override
    public int glCreateShaderProgramv(int target, CharSequence source) {
        return GL41.glCreateShaderProgramv(target, source);
    }

    @Override
    public void glProgramUniform1f(int program, int index, float radius) {
        GL41.glProgramUniform1f(program, index, radius);
    }

    @Override
    public void glProgramUniform2f(int program, int index, float x, float y) {
        GL41.glProgramUniform2f(program, index, x, y);
    }

    @Override
    public void glProgramUniform4f(int program, int index, float x, float y, float z, float w) {
        GL41.glProgramUniform4f(program, index, x, y, z, w);
    }

    @Override
    public void glProgramUniformMatrix4fv(int program, int index, boolean transpose, FloatBuffer data) {
        GL41.glProgramUniformMatrix4fv(program, index, transpose, data);
    }

    @Override
    public void glProgramUniform4fv(int program, int index, FloatBuffer data) {
        GL41.glProgramUniform4fv(program, index, data);
    }

    @Override
    public void glProgramUniform1fv(int program, int index, FloatBuffer data) {
        GL41.glProgramUniform1fv(program, index, data);
    }

    @Override
    public void glProgramUniform1i(int program, int index, int i) {
        GL41.glProgramUniform1i(program, index, i);
    }

    @Override
    public void glProgramUniform4i(int program, int index, int x, int y, int z, int w) {
        GL41.glProgramUniform4i(program, index, x, y, z, w);
    }

    @Override
    public void glProgramUniform2fv(int program, int index, FloatBuffer values) {
        GL41.glProgramUniform2fv(program, index, values);
    }

    @Override
    public int glGetUniformui(int programId, int location) {
        return GL30.glGetUniformui(programId, location);
    }

    @Override
    public double glGetUniformd(int programId, int location) {
        return GL40.glGetUniformd(programId, location);
    }

    @Override
    public void glGetUniformdv(int programId, int location, DoubleBuffer buf) {
        GL40.glGetUniformdv(programId, location, buf);
    }

    @Override
    public void glDrawElementsBaseVertex(int primType, int count, int type, int start, int baseVertex) {
        GL32.glDrawElementsBaseVertex(primType, count, type, start, baseVertex);
    }

    @Override
    public void glTexBuffer(int target, int internalFormat, int buffer) {
        GL31.glTexBuffer(target, internalFormat, buffer);
    }

    @Override
    public void glClearBufferData(int target, int internalformat, int format, int type, ByteBuffer data) {
        GL43.glClearBufferData(target, internalformat, format, type, data);
    }

    @Override
    public void glGetBufferSubData(int target, int offset, int size, ByteBuffer data) {
        GL15.glGetBufferSubData(target, offset, data);
    }

    @Override
    public void glDrawTransformFeedbackStream(int mode, int transformFeedback, int index) {
        GL40.glDrawTransformFeedbackStream(mode, transformFeedback, index);
    }

    @Override
    public void glClearTexImage(int texture, int level, int format, int type, Buffer data) {
        GL44.nglClearTexImage(texture, level, format, type, MemoryUtil.memAddress0Safe(data));
    }

    @Override
    public void glUniform1ui(int location, int i) {
        GL30.glUniform1ui(location, i);
    }

    @Override
    public void glDispatchComputeIndirect(int indirect) {
        GL43.glDispatchComputeIndirect(indirect);
    }

    @Override
    public void glDrawArraysIndirect(int mode, int indirect) {
        GL40.glDrawArraysIndirect(mode, indirect);
    }

    @Override
    public int glGetProgramInterfacei(int program, int programInterface, int pname) {
        return GL43.glGetProgramInterfacei(program, programInterface, pname);
    }

    @Override
    public void glGetProgramInterfaceiv(int program, int programInterface, int pname, IntBuffer params) {
        GL43.glGetProgramInterfaceiv(program, programInterface, pname, params);
    }

    @Override
    public void glGetProgramResourceiv(int program, int programInterface, int index, IntBuffer props, IntBuffer length, IntBuffer params) {
        GL43.glGetProgramResourceiv(program, programInterface, index, props, length, params);
    }

    @Override
    public int glGetProgramResourceIndex(int program, int programInterface, CharSequence name) {
        return GL43.glGetProgramResourceIndex(program, programInterface, name);
    }

    @Override
    public int glGetProgramResourceLocation(int program, int programInterface, CharSequence name) {
        return GL43.glGetProgramResourceLocation(program, programInterface, name);
    }

    @Override
    public String glGetProgramResourceName(int program, int programInterface, int index, int bufSize) {
        return GL43.glGetProgramResourceName(program, programInterface, index, bufSize);
    }

    @Override
    public int glGetProgramResourceLocationIndex(int program, int programInterface, CharSequence name) {
        return GL43.glGetProgramResourceLocationIndex(program, programInterface, name);
    }

    @Override
    public void glGenerateTextureMipmap(int texture) {
        ARBDirectStateAccess.glGenerateTextureMipmap(texture);
    }

    @Override
    public void glQueryCounter(int id, int target) {
        GL33.glQueryCounter(id, target);
    }

    @Override
    public long glGetQueryObjectui64ui(int id, int pname) {
        return GL33.glGetQueryObjectui64(id, pname);
    }

    @Override
    public int glGetSubroutineIndex(int program, int type, String name) {
        return GL40.glGetSubroutineIndex(program, type, name);
    }

    @Override
    public void glUniformSubroutinesui(int type, int index) {
        GL40.glUniformSubroutinesui(type, index);
    }

    @Override
    public void glVertexAttribFormat(int index, int size, int type, boolean normalized, long offset) {
        GL43.glVertexAttribFormat(index, size, type, normalized, (int)offset);
    }

    @Override
    public void glVertexAttribBinding(int attribindex, int bindingindex) {
        GL43.glVertexAttribBinding(attribindex, bindingindex);
    }

    @Override
    public void glBindVertexBuffer(int bindingindex, int buffer, long offset, int stride) {
        GL43.glBindVertexBuffer(bindingindex, buffer, offset, stride);
    }

    @Override
    public void glTextureBuffer(int texture, int internalformat, int buffer) {
        ARBDirectStateAccess.glTextureBuffer(texture, internalformat, buffer);
    }

    @Override
    public void glDrawElementsIndirect(int mode, int type, long indirect) {
        GL40.glDrawElementsIndirect(mode, type, indirect);
    }

    @Override
    public void glTexBufferRange(int target, int internalFormat, int buffer, long offset, int size) {
        GL43.glTexBufferRange(target, internalFormat, buffer, offset, size);
    }

    @Override
    public void glTextureBufferRange(int texture, int internalformat, int buffer, long offset, int size) {
        GL45.glTextureBufferRange(texture, internalformat, buffer, offset, size);
    }

    @Override
    public long glFenceSync() {
        return GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
    }

    @Override
    public int glClientWaitSync(long sync, int flags, long timeout) {
        return GL32.glClientWaitSync(sync, flags, timeout);
    }

    @Override
    public ByteBuffer glMapBufferRange(int target, int offset, int length, int access, ByteBuffer old_buffer) {
        return GL30.glMapBufferRange(target, offset, length, access, old_buffer);
    }

    @Override
    public ByteBuffer glMapBufferRange(int target, int offset, int length, int access) {
        return GL30.glMapBufferRange(target, offset, length, access);
    }

    @Override
    public void glDeleteSync(long sync) {
        GL32.glDeleteSync(sync);
    }

    @Override
    public void glProgramUniform3f(int program, int index, float x, float y, float z) {
        GL41.glProgramUniform3f(program, index, x, y, z);
    }

    @Override
    public void glScissorIndexed(int index, int x, int y, int width, int height) {
        GL41.glScissorIndexed(index, x, y, width, height);
    }

    @Override
    public void glViewportIndexedf(int index, float x, float y, float width, float height) {
        GL41.glViewportIndexedf(index, x, y, width, height);
    }

    @Override
    public void glBindImageTextures(int unit, IntBuffer images) {
        GL44.glBindImageTextures(unit, images);
    }

    @Override
    public void glViewportArrayv(int index, FloatBuffer viewports) {
        GL41.glViewportArrayv(index, viewports);
    }

    @Override
    public void glBindFragDataLocation(int program, int index, String name) {
        GL30.glBindFragDataLocation(program, index, name);
    }

    @Override
    public void glClearNamedBufferData(int buffer, int internalformat, int format, int type, Buffer data) {
        GL45.nglClearNamedBufferData(buffer, internalformat, format, type, MemoryUtil.memAddress0Safe(data));
    }

    @Override
    public void glClearNamedBufferSubData(int buffer, int internalformat, long offset, long size, int format, int type, Buffer data) {
        ARBDirectStateAccess.nglClearNamedBufferSubData(buffer, internalformat, offset, size, format, type, MemoryUtil.memAddress0Safe(data));
    }

    @Override
    public void glGetMemoryInfo(GPUMemoryInfo info) {
        info.dedicatedMemory = GL11.glGetInteger(NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_DEDICATED_VIDMEM_NVX );
        info.maxmumDedicatedMemory = GL11.glGetInteger(NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_TOTAL_AVAILABLE_MEMORY_NVX);
        info.currentMemory = GL11.glGetInteger(NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX);
        info.evictionCount = GL11.glGetInteger(NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_EVICTION_COUNT_NVX );
        info.evictedMemory = GL11.glGetInteger(NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_EVICTED_MEMORY_NVX);
    }

    @Override
    public int glCreateBuffer() {
        return GL45.glCreateBuffers();
    }

    @Override
    public void glBufferStorage(int target, long size, int flags) {
        GL44.glBufferStorage(target, size, flags);
    }

    @Override
    public void glBufferStorage(int target, Buffer data, int flags) {
        if(data instanceof ByteBuffer){
            GL44.glBufferStorage(target, (ByteBuffer)data, flags);
        }else if(data instanceof  ShortBuffer){
            GL44.glBufferStorage(target, (ShortBuffer)data, flags);
        }else if(data instanceof FloatBuffer){
            GL44.glBufferStorage(target, (FloatBuffer)data, flags);
        }else if(data instanceof IntBuffer){
            GL44.glBufferStorage(target, (IntBuffer)data, flags);
        }else if(data instanceof DoubleBuffer){
            GL44.glBufferStorage(target, (DoubleBuffer)data, flags);
        }else{
            throw new UnsupportedOperationException("Unkown data type: " + data.getClass().getName());
        }
    }

    @Override
    public void glNamedBufferStorage(int buffer, long size, int flags) {
        GL45.glNamedBufferStorage(buffer, size, flags);
    }

    @Override
    public void glNamedBufferStorage(int buffer, Buffer data, int flags) {
        if(data instanceof ByteBuffer){
            GL45.glNamedBufferStorage(buffer, (ByteBuffer)data, flags);
        }else if(data instanceof  ShortBuffer){
            GL45.glNamedBufferStorage(buffer, (ShortBuffer)data, flags);
        }else if(data instanceof FloatBuffer){
            GL45.glNamedBufferStorage(buffer, (FloatBuffer)data, flags);
        }else if(data instanceof IntBuffer){
            GL45.glNamedBufferStorage(buffer, (IntBuffer)data, flags);
        }else if(data instanceof DoubleBuffer){
            GL45.glNamedBufferStorage(buffer, (DoubleBuffer)data, flags);
        }else{
            throw new UnsupportedOperationException("Unkown data type: " + data.getClass().getName());
        }
    }

    public void glCopyNamedBufferSubData(int readBuffer, int writeBuffer, long readOffset, long writeOffset, long size){
        GL45.glCopyNamedBufferSubData(readBuffer, writeBuffer, readOffset, writeOffset, size);
    }

    public ByteBuffer glMapNamedBufferRange(
            int buffer,
            long offset,
            long length,
            int access, ByteBuffer old_buffer){
        return GL45.glMapNamedBufferRange(buffer, offset, length, access, old_buffer);
    }

    public ByteBuffer glMapNamedBufferRange(
            int buffer,
            long offset,
            long length,
            int access){
        return GL45.glMapNamedBufferRange(buffer, offset, length, access);
    }
}
