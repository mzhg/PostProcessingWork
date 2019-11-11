package jet.opengl.impl.jogl;

import com.jogamp.common.util.VersionNumber;
import com.jogamp.opengl.GL4;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import jet.opengl.postprocessing.common.GLAPI;
import jet.opengl.postprocessing.common.GLAPIVersion;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.common.GPUMemoryInfo;
import jet.opengl.postprocessing.texture.NativeAPI;
import jet.opengl.postprocessing.util.BufferUtils;

/**
 * Created by mazhen'gui on 2017/4/12.
 */

public class JoglOpenglFuncProvider implements GLFuncProvider {
    private GL4 gl;
    private GLAPIVersion m_GLVersion;
    private final int[] intValues = new int[1];
    private final byte[] byteValues = new byte[1];
    private final float[] floatValues = new float[1];
    private final double[] doubleValues = new double[1];
    private final long[] longValues = new long[1];

    public JoglOpenglFuncProvider(GL4 gl){
        this.gl = gl;
    }

    @Override
    public boolean isSupportExt(String ext) {
        return gl.isExtensionAvailable(ext);
    }

    @Override
    public GLAPI getHostAPI() {
        return GLAPI.JOGL;
    }

    @Override
    public GLAPIVersion getGLAPIVersion() {
        if(m_GLVersion == null){
            VersionNumber versionNumber = gl.getContext().getGLVersionNumber();
            m_GLVersion = new GLAPIVersion(gl.getGLProfile().usesNativeGLES(), versionNumber.getMajor(), versionNumber.getMinor(), false);
            if(m_GLVersion.major == 4 && m_GLVersion.minor > 5){
                m_GLVersion = new GLAPIVersion(false, 4, 5, false);
            }
        }

        return m_GLVersion;
    }

    @Override
    public void glActiveTexture(int texture) {
        gl.glActiveTexture(texture);
    }

    @Override
    public void glBindTexture(int target, int texture) {
        gl.glBindTexture(target, texture);
    }

    @Override
    public void glBlendFunc(int sfactor, int dfactor) {
        gl.glBlendFunc(sfactor, dfactor);
    }

    @Override
    public void glClear(int mask) {
        gl.glClear(mask);
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        gl.glClearColor(red, green, blue, alpha);
    }

    @Override
    public void glClearDepthf(float depth) {
        gl.glClearDepthf(depth);
    }

    @Override
    public void glClearStencil(int s) {
        gl.glClearStencil(s);
    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        gl.glColorMask(red, green, blue, alpha);
    }

    @Override
    public void glCopyTexImage2D(int target, int level, int internalformat, int x, int y, int width, int height, int border) {
        gl.glCopyTexImage2D(target, level, internalformat, x, y, width, height, border);
    }

    @Override
    public void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height) {
        gl.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
    }

    @Override
    public void glCullFace(int mode) {
        gl.glCullFace(mode);
    }

    @Override
    public void glDeleteTextures(int... textures) {
        if(textures == null)
            return;

        glDeleteTextures(textures, 0, textures.length);
    }

    @Override
    public void glDeleteTextures(int[] textures, int offset, int length) {
        gl.glDeleteTextures(length, textures, offset);
    }

    @Override
    public void glDeleteTextures(IntBuffer textures) {
        gl.glDeleteTextures(textures.remaining(), textures);
    }

    @Override
    public void glDeleteTexture(int texture) {
        glDeleteTextures(texture);
    }

    @Override
    public void glDepthFunc(int func) {
        gl.glDepthFunc(func);
    }

    @Override
    public void glDepthMask(boolean flag) {
        gl.glDepthMask(flag);
    }

    @Override
    public void glDisable(int cap) {
        gl.glDisable(cap);
    }

    @Override
    public void glDrawArrays(int mode, int first, int count) {
        gl.glDrawArrays(mode, first, count);
    }

    @Override
    public void glDrawElements(int mode, int count, int type, Buffer indices) {
        if(indices == null){
            gl.glDrawElements(mode, count, type, 0);
        }else{
            throw new UnsupportedOperationException("JOGL doesn't support the Buffer with glDrawElements!!!");
        }
    }

    @Override
    public void glEnable(int cap) {
        gl.glEnable(cap);
    }

    @Override
    public void glEnablei(int cap, int index) {
        gl.glEnablei(cap, index);
    }

    @Override
    public void glDisablei(int cap, int index) {
        gl.glDisablei(cap, index);
    }

    @Override
    public boolean glIsEnabledi(int cap, int index) {
        return gl.glIsEnabledi(cap, index);
    }

    @Override
    public void glFinish() {
        gl.glFinish();
    }

    @Override
    public void glFlush() {
        gl.glFlush();
    }

    @Override
    public void glFrontFace(int mode) {
        gl.glFrontFace(mode);
    }

    @Override
    public int glGenTexture() {
        gl.glGenTextures(1, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGenTextures(IntBuffer textures) {
        gl.glGenTextures(textures.remaining(), textures);
    }

    @Override
    public int glGetError() {
        return gl.glGetError();
    }

    @Override
    public int glGetInteger(int pname) {
        gl.glGetIntegerv(pname, intValues, 0);
        return intValues[0];
    }

    @Override
    public String glGetString(int name) {
        return gl.glGetString(name);
    }

    @Override
    public void glHint(int target, int mode) {
        gl.glHint(target, mode);
    }

    @Override
    public void glLineWidth(float width) {
        gl.glLineWidth(width);
    }

    @Override
    public void glPixelStorei(int pname, int param) {
        gl.glPixelStorei(pname, param);
    }

    @Override
    public void glPolygonOffset(float factor, float units) {
        gl.glPolygonOffset(factor, units);
    }

    @Override
    public void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer pixels) {
        gl.glReadPixels(x, y, width, height, format, type, pixels);
    }

    @Override
    public void glReadPixels(int x, int y, int width, int height, int format, int type, long offset) {
        gl.glReadPixels(x, y, width, height, format, type, offset);
    }

    @Override
    public void glScissor(int x, int y, int width, int height) {
        gl.glScissor(x, y, width, height);
    }

    @Override
    public void glStencilFunc(int func, int ref, int mask) {
        gl.glStencilFunc(func, ref, mask);
    }

    @Override
    public void glStencilMask(int mask) {
        gl.glStencilMask(mask);
    }

    @Override
    public void glStencilOp(int fail, int zfail, int zpass) {
        gl.glStencilOp(fail, zfail, zpass);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, Buffer pixels) {
        gl.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, long pixels_offset) {
        gl.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels_offset);
    }

    @Override
    public void glTexParameterf(int target, int pname, float param) {
        gl.glTexParameterf(target, pname, param);
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, Buffer pixels) {
        gl.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, long pixels_offset) {
        gl.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels_offset);
    }

    @Override
    public void glTexImage1D(int target, int level, int internalformat, int width, int border, int format, int type, Buffer pixels) {
        gl.glTexImage1D(target, level, internalformat, width, border, format, type, pixels);
    }

    @Override
    public void glViewport(int x, int y, int width, int height) {
        gl.glViewport(x, y, width, height);
    }

    @Override
    public void glAttachShader(int program, int shader) {
        gl.glAttachShader(program, shader);
    }

    @Override
    public void glBindAttribLocation(int program, int index, CharSequence name) {
        gl.glBindAttribLocation(program, index, name.toString());
    }

    @Override
    public void glBindBuffer(int target, int buffer) {
        gl.glBindBuffer(target, buffer);
    }

    @Override
    public void glBindFramebuffer(int target, int framebuffer) {
        gl.glBindFramebuffer(target, framebuffer);
    }

    @Override
    public void glBindRenderbuffer(int target, int renderbuffer) {
        gl.glBindRenderbuffer(target, renderbuffer);
    }

    @Override
    public void glBlendColor(float red, float green, float blue, float alpha) {
        gl.glBlendColor(red, green, blue, alpha);
    }

    @Override
    public void glBlendEquation(int mode) {
        gl.glBlendEquation(mode);
    }

    @Override
    public void glBlendEquationSeparate(int modeRGB, int modeAlpha) {
        gl.glBlendEquationSeparate(modeRGB, modeAlpha);
    }

    @Override
    public void glBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        gl.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
    }

    @Override
    public void glBlendEquationi(int buf, int mode) {
        gl.glBlendEquationi(buf, mode);
    }

    @Override
    public void glBlendEquationSeparatei(int buf, int modeRGB, int modeAlpha) {
        gl.glBlendEquationSeparatei(buf, modeRGB, modeAlpha);
    }

    @Override
    public void glBlendFunci(int buf, int sfactor, int dfactor) {
        gl.glBlendFunci(buf, sfactor, dfactor);
    }

    @Override
    public void glBlendFuncSeparatei(int buf, int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        gl.glBlendFuncSeparatei(buf, srcRGB, dstRGB, srcAlpha, dstAlpha);
    }

    @Override
    public void glBufferSubData(int target, int offset, Buffer data) {
        gl.glBufferSubData(target, offset, BufferUtils.measureSize(data), data);
    }

    @Override
    public int glCheckFramebufferStatus(int target) {
        return gl.glCheckFramebufferStatus(target);
    }

    @Override
    public void glCompileShader(int shader) {
        gl.glCompileShader(shader);
    }

    @Override
    public int glCreateProgram() {
        return gl.glCreateProgram();
    }

    @Override
    public int glCreateShader(int type) {
        return gl.glCreateShader(type);
    }

    final int[] wrap(int i){ intValues[0] = i; return intValues;}

    @Override
    public void glDeleteBuffer(int buffer) {
        gl.glDeleteBuffers(1, wrap(buffer), 0);
    }

    @Override
    public void glDeleteBuffers(IntBuffer buffers) {
        gl.glDeleteBuffers(buffers.remaining(), buffers);
    }

    @Override
    public void glDeleteFramebuffer(int framebuffer) {
        gl.glDeleteFramebuffers(1, wrap(framebuffer), 0);
    }

    @Override
    public void glDeleteFramebuffers(IntBuffer framebuffer) {
        gl.glDeleteFramebuffers(framebuffer.remaining(), framebuffer);
    }

    @Override
    public void glDeleteProgram(int program) {
        gl.glDeleteProgram(program);
    }

    @Override
    public void glDeleteRenderbuffer(int renderbuffer) {
        gl.glDeleteRenderbuffers(1, wrap(renderbuffer), 0);
    }

    @Override
    public void glDeleteRenderbuffers(IntBuffer renderbuffers) {
        gl.glDeleteRenderbuffers(renderbuffers.remaining(), renderbuffers);
    }

    @Override
    public void glDeleteShader(int shader) {
        gl.glDeleteShader(shader);
    }

    @Override
    public void glDetachShader(int program, int shader) {
        gl.glDetachShader(program, shader);
    }

    @Override
    public void glDisableVertexAttribArray(int index) {
        gl.glDisableVertexAttribArray(index);
    }

    @Override
    public void glDrawElements(int mode, int count, int type, long indices) {
        gl.glDrawElements(mode, count, type, indices);
    }

    @Override
    public void glEnableVertexAttribArray(int index) {
        gl.glEnableVertexAttribArray(index);
    }

    @Override
    public void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {
        gl.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
    }

    @Override
    public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        gl.glFramebufferTexture2D(target, attachment, textarget, texture, level);
    }

    @Override
    public int glGenBuffer() {
        gl.glGenBuffers(1, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGenBuffers(IntBuffer buffers) {
        gl.glGenBuffers(buffers.remaining(), buffers);
    }

    @Override
    public void glGenerateMipmap(int target) {
        gl.glGenerateMipmap(target);
    }

    @Override
    public int glGenFramebuffer() {
        gl.glGenFramebuffers(1, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGenFramebuffers(IntBuffer framebuffers) {
        gl.glGenFramebuffers(framebuffers.remaining(), framebuffers);
    }

    @Override
    public int glGenRenderbuffer() {
        gl.glGenRenderbuffers(1, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGenRenderbuffers(IntBuffer renderbuffers) {
        gl.glGenRenderbuffers(renderbuffers.remaining(),renderbuffers);
    }

    @Override
    public String glGetActiveAttrib(int program, int index, int maxLength, IntBuffer size, IntBuffer type) {
        IntBuffer length = BufferUtils.createIntBuffer(1);
        ByteBuffer name = BufferUtils.createByteBuffer(maxLength); // TODO null-termined string
        gl.glGetActiveAttrib(program, index, maxLength, length, size, type, name);
        byte[] bytes = new byte[maxLength];
        name.get(bytes);
        return new String(bytes);
    }

    @Override
    public String glGetActiveUniform(int program, int index, int maxLength, IntBuffer size, IntBuffer type) {
        IntBuffer length = BufferUtils.createIntBuffer(1);
        ByteBuffer name = BufferUtils.createByteBuffer(maxLength); // TODO null-termined string
        gl.glGetActiveUniform(program, index, maxLength, length, size, type, name);
        byte[] bytes = new byte[maxLength];
        name.get(bytes);
        return new String(bytes);
    }

    @Override
    public void glGetAttachedShaders(int program, int[] count, int[] shaders) {
        gl.glGetAttachedShaders(program, count.length, count, 0, shaders, 0);
    }

    @Override
    public int glGetAttribLocation(int program, CharSequence name) {
        return gl.glGetAttribLocation(program, name.toString());
    }

    @Override
    public boolean glGetBoolean(int pname) {
        gl.glGetBooleanv(pname, byteValues, 0);return byteValues[0] != 0;
    }

    @Override
    public void glGetBooleanv(int pname, ByteBuffer params) {
        gl.glGetBooleanv(pname, params);
    }

    @Override
    public int glGetBufferParameteri(int target, int pname) {
        gl.glGetBufferParameteriv(target, pname, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGetBufferParameteriv(int target, int pname, IntBuffer params) {
        gl.glGetBufferParameteriv(target, pname, params);
    }

    @Override
    public float glGetFloat(int pname) {
        gl.glGetFloatv(pname, floatValues, 0);
        return floatValues[0];
    }

    @Override
    public void glGetFloatv(int pname, FloatBuffer params) {
        gl.glGetFloatv(pname, params);
    }

    @Override
    public int glGetProgrami(int program, int pname) {
        gl.glGetProgramiv(program, pname, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGetProgramiv(int program, int pname, IntBuffer params) {
        gl.glGetProgramiv(program, pname, params);
    }

    @Override
    public String glGetProgramInfoLog(int program) {
        throw new RuntimeException("Unimplementation!");
    }

    @Override
    public int glGetShaderi(int shader, int pname) {
        gl.glGetShaderiv(shader, pname, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGetShaderiv(int shader, int pname, IntBuffer params) {
        gl.glGetShaderiv(shader, pname, params);
    }

    @Override
    public String glGetShaderInfoLog(int shader) {
        throw new RuntimeException();
    }

    @Override
    public float glGetTexParameterf(int target, int pname) {
        gl.glGetTexParameterfv(target, pname, floatValues, 0);
        return floatValues[0];
    }

    @Override
    public void glGetTexParameterfv(int target, int pname, FloatBuffer params) {
        gl.glGetTexParameterfv(target, pname, params);
    }

    @Override
    public int glGetTexParameteri(int target, int pname) {
        gl.glGetTexParameteriv(target, pname, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGetTexParameteriv(int target, int pname, IntBuffer params) {
        gl.glGetTexParameteriv(target, pname, params);
    }

    @Override
    public float glGetUniformf(int program, int location) {
        gl.glGetUniformfv(program, location, floatValues, 0);
        return floatValues[0];
    }

    @Override
    public void glGetUniformfv(int program, int location, FloatBuffer params) {
        gl.glGetUniformfv(program, location, params);
    }

    @Override
    public int glGetUniformi(int program, int location) {
        gl.glGetUniformiv(program, location, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGetUniformiv(int program, int location, IntBuffer params) {
        gl.glGetUniformiv(program, location, params);
    }

    @Override
    public int glGetUniformLocation(int program, CharSequence name) {
        return gl.glGetUniformLocation(program, name.toString());
    }

    @Override
    public boolean glIsBuffer(int buffer) {
        return gl.glIsBuffer(buffer);
    }

    @Override
    public boolean glIsEnabled(int cap) {
        return gl.glIsEnabled(cap);
    }

    @Override
    public boolean glIsFramebuffer(int framebuffer) {
        return gl.glIsFramebuffer(framebuffer);
    }

    @Override
    public boolean glIsProgram(int program) {
        return gl.glIsProgram(program);
    }

    @Override
    public boolean glIsRenderbuffer(int renderbuffer) {
        return gl.glIsRenderbuffer(renderbuffer);
    }

    @Override
    public boolean glIsShader(int shader) {
        return gl.glIsShader(shader);
    }

    @Override
    public boolean glIsTexture(int texture) {
        return gl.glIsTexture(texture);
    }

    @Override
    public void glLinkProgram(int program) {
        gl.glLinkProgram(program);
    }

    @Override
    public void glReleaseShaderCompiler() {
        gl.glReleaseShaderCompiler();
    }

    @Override
    public void glRenderbufferStorage(int target, int internalformat, int width, int height) {
        gl.glRenderbufferStorage(target, internalformat, width, height);
    }

    @Override
    public void glSampleCoverage(float value, boolean invert) {
        gl.glSampleCoverage(value, invert);
    }

    @Override
    public void glShaderBinary(IntBuffer shaders, int binaryformat, ByteBuffer binary) {
        gl.glShaderBinary(shaders.remaining(), shaders, binaryformat, binary, binary.remaining());
    }

    @Override
    public void glShaderSource(int shader, CharSequence string) {
        gl.glShaderSource(shader, 1, new String[]{string.toString()}, wrap(string.length()), 0);
    }

    @Override
    public void glStencilFuncSeparate(int face, int func, int ref, int mask) {
        gl.glStencilFuncSeparate(face, func, ref, mask);
    }

    @Override
    public void glStencilMaskSeparate(int face, int mask) {
        gl.glStencilMaskSeparate(face, mask);
    }

    @Override
    public void glStencilOpSeparate(int face, int fail, int zfail, int zpass) {
        gl.glStencilOpSeparate(face, fail, zfail, zpass);
    }

    @Override
    public void glTexParameterfv(int target, int pname, FloatBuffer params) {
        gl.glTexParameterfv(target, pname, params);
    }

    @Override
    public void glTexParameteri(int target, int pname, int param) {
        gl.glTexParameteri(target, pname, param);
    }

    @Override
    public void glTexParameteriv(int target, int pname, int[] params) {
        gl.glTexParameteriv(target, pname, params, 0);
    }

    @Override
    public void glUniform1f(int location, float x) {
        gl.glUniform1f(location, x);
    }

    @Override
    public void glUniform1fv(int location, FloatBuffer v) {
        gl.glUniform1fv(location, v.remaining(), v);
    }

    @Override
    public void glUniform1i(int location, int x) {
        gl.glUniform1i(location, x);
    }

    @Override
    public void glUniform1iv(int location, IntBuffer v) {
        gl.glUniform1iv(location, v.remaining(), v);
    }

    @Override
    public void glUniform2f(int location, float x, float y) {
        gl.glUniform2f(location, x, y);
    }

    @Override
    public void glUniform2fv(int location, FloatBuffer v) {
        gl.glUniform2fv(location, v.remaining()/2, v);
    }

    @Override
    public void glUniform2i(int location, int x, int y) {
        gl.glUniform2i(location, x, y);
    }

    @Override
    public void glUniform2iv(int location, IntBuffer v) {
        gl.glUniform2iv(location, v.remaining()/2, v);
    }

    @Override
    public void glUniform3f(int location, float x, float y, float z) {
        gl.glUniform3f(location, x, y, z);
    }

    @Override
    public void glUniform3fv(int location, FloatBuffer v) {
        gl.glUniform3fv(location, v.remaining()/3, v);
    }

    @Override
    public void glUniform3i(int location, int x, int y, int z) {
        gl.glUniform3i(location, x, y, z);
    }

    @Override
    public void glUniform3iv(int location, IntBuffer v) {
        gl.glUniform3iv(location, v.remaining()/3, v);
    }

    @Override
    public void glUniform4f(int location, float x, float y, float z, float w) {
        gl.glUniform4f(location, x, y, z, w);
    }

    @Override
    public void glUniform4fv(int location, FloatBuffer v) {
        gl.glUniform4fv(location, v.remaining()/4, v);
    }

    @Override
    public void glUniform4i(int location, int x, int y, int z, int w) {
        gl.glUniform4i(location, x, y, z, w);
    }

    @Override
    public void glUniform4iv(int location, IntBuffer v) {
        gl.glUniform4iv(location, v.remaining()/4, v);
    }

    @Override
    public void glUniformMatrix2fv(int location, boolean transpose, FloatBuffer value) {
        gl.glUniformMatrix2fv(location, value.remaining()/4, transpose, value);
    }

    @Override
    public void glUniformMatrix3fv(int location, boolean transpose, FloatBuffer value) {
        gl.glUniformMatrix3fv(location, value.remaining()/9, transpose, value);
    }

    @Override
    public void glUniformMatrix4fv(int location, boolean transpose, FloatBuffer value) {
        gl.glUniformMatrix4fv(location, value.remaining()/16, transpose, value);
    }

    @Override
    public void glUseProgram(int program) {
        gl.glUseProgram(program);
    }

    @Override
    public void glValidateProgram(int program) {
        gl.glValidateProgram(program);
    }

    @Override
    public void glVertexAttrib1f(int indx, float x) {
        gl.glVertexAttrib1f(indx, x);
    }

    @Override
    public void glVertexAttrib1fv(int indx, FloatBuffer values) {
        gl.glVertexAttrib1fv(indx, values);
    }

    @Override
    public void glVertexAttrib2f(int indx, float x, float y) {
        gl.glVertexAttrib2f(indx, x, y);
    }

    @Override
    public void glVertexAttrib2fv(int indx, FloatBuffer values) {
        gl.glVertexAttrib2fv(indx, values);
    }

    @Override
    public void glVertexAttrib3f(int indx, float x, float y, float z) {
        gl.glVertexAttrib3f(indx, x, y, z);
    }

    @Override
    public void glVertexAttrib3fv(int indx, FloatBuffer values) {
        gl.glVertexAttrib3fv(indx, values);
    }

    @Override
    public void glVertexAttrib4f(int indx, float x, float y, float z, float w) {
        gl.glVertexAttrib4f(indx, x, y, z, w);
    }

    @Override
    public void glVertexAttrib4fv(int indx, FloatBuffer values) {
        gl.glVertexAttrib4fv(indx, values);
    }

    @Override
    public void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, Buffer ptr) {
        throw new UnsupportedOperationException("Jogl doesn't support this function! Ha Ha Ha");
    }

    @Override
    public void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, int ptr) {
        gl.glVertexAttribPointer(indx, size, type, normalized, stride, ptr);
    }

    @Override
    public void glReadBuffer(int mode) {
        gl.glReadBuffer(mode);
    }

    @Override
    public void glDrawRangeElements(int mode, int start, int end, int count, int type, Buffer indices) {
        throw new UnsupportedOperationException("Jogl doesn't support this function! Ha Ha Ha");
    }

    @Override
    public void glDrawRangeElements(int mode, int start, int end, int count, int type, int offset) {
        gl.glDrawRangeElements(mode, start, end, count, type, offset);
    }

    @Override
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, Buffer pixels) {
        gl.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, pixels);
    }

    @Override
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, long offset) {
        gl.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, offset);
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, Buffer pixels) {
        gl.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, pixels);
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, long offset) {
        gl.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, offset);
    }

    @Override
    public void glCopyTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int x, int y, int width, int height) {
        gl.glCopyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height);
    }

    @Override
    public int glGenQuery() {
        gl.glGenQueries(1, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGenQueries(IntBuffer ids) {
        gl.glGenQueries(ids.remaining(), ids);
    }

    @Override
    public void glDeleteQuery(int query) {
        gl.glDeleteQueries(1, wrap(query), 0);
    }

    @Override
    public void glDeleteQueries(IntBuffer ids) {
        gl.glDeleteQueries(ids.remaining(), ids);
    }

    @Override
    public boolean glIsQuery(int id) {
        return gl.glIsQuery(id);
    }

    @Override
    public void glBeginQuery(int target, int id) {
        gl.glBeginQuery(target, id);
    }

    @Override
    public void glBeginQueryIndexed(int target, int index, int id) {
        gl.glBeginQueryIndexed(target, index, id);
    }

    @Override
    public void glEndQuery(int target) {
        gl.glEndQuery(target);
    }

    @Override
    public void glEndQueryIndexed(int target, int index) {
        gl.glEndQueryIndexed(target, index);
    }

    @Override
    public int glGetQueryi(int target, int pname) {
        gl.glGetQueryiv(target, pname, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGetQueryiv(int target, int pname, IntBuffer params) {
        gl.glGetQueryiv(target, pname, params);
    }

    @Override
    public int glGetQueryObjectuiv(int id, int pname) {
        gl.glGetQueryObjectuiv(id, pname, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGetQueryObjectuiv(int id, int pname, IntBuffer params) {
        gl.glGetQueryObjectuiv(id, pname, params);
    }

    @Override
    public boolean glUnmapBuffer(int target) {
        return gl.glUnmapBuffer(target);
    }

    @Override
    public ByteBuffer glGetBufferPointerv(int target, int pname) {
        throw new UnsupportedOperationException("Jogl doesn't support this function! Ha Ha Ha");
    }

    @Override
    public void glDrawBuffers(int buffer) {
        gl.glDrawBuffers(1, wrap(buffer), 0);
    }

    @Override
    public void glDrawBuffers(IntBuffer bufs) {
        gl.glDrawBuffers(bufs.remaining(), bufs);
    }

    @Override
    public void glUniformMatrix2x3fv(int location, boolean transpose, FloatBuffer value) {
        gl.glUniformMatrix2x3fv(location, value.remaining()/6, transpose,value);
    }

    @Override
    public void glUniformMatrix3x2fv(int location, boolean transpose, FloatBuffer value) {
        gl.glUniformMatrix3x2fv(location, value.remaining()/6, transpose,value);
    }

    @Override
    public void glUniformMatrix2x4fv(int location, boolean transpose, FloatBuffer value) {
        gl.glUniformMatrix2x4fv(location, value.remaining()/8, transpose,value);
    }

    @Override
    public void glUniformMatrix4x2fv(int location, boolean transpose, FloatBuffer value) {
        gl.glUniformMatrix4x2fv(location, value.remaining()/8, transpose,value);
    }

    @Override
    public void glUniformMatrix3x4fv(int location, boolean transpose, FloatBuffer value) {
        gl.glUniformMatrix3x4fv(location, value.remaining()/12, transpose,value);
    }

    @Override
    public void glUniformMatrix4x3fv(int location, boolean transpose, FloatBuffer value) {
        gl.glUniformMatrix4x3fv(location, value.remaining()/12, transpose,value);
    }

    @Override
    public void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        gl.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    @Override
    public void glRenderbufferStorageMultisample(int target, int samples, int internalformat, int width, int height) {
        gl.glRenderbufferStorageMultisample(target, samples, internalformat, width, height);
    }

    @Override
    public void glFramebufferTextureLayer(int target, int attachment, int texture, int level, int layer) {
        gl.glFramebufferTextureLayer(target, attachment, texture, level, layer);
    }

    @Override
    public void glFlushMappedBufferRange(int target, int offset, int length) {
        gl.glFlushMappedBufferRange(target, offset, length);
    }

    @Override
    public void glBindVertexArray(int array) {
        gl.glBindVertexArray(array);
    }

    @Override
    public void glDeleteVertexArray(int vao) {
        gl.glDeleteVertexArrays(1, wrap(vao), 0);
    }

    @Override
    public void glDeleteVertexArrays(IntBuffer arrays) {
        gl.glDeleteVertexArrays(arrays.remaining(), arrays);
    }

    @Override
    public int glGenVertexArray() {
        gl.glGenVertexArrays(1, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGenVertexArrays(IntBuffer arrays) {
        gl.glGenVertexArrays(arrays.remaining(), arrays);
    }

    @Override
    public boolean glIsVertexArray(int array) {
        return gl.glIsVertexArray(array);
    }

    @Override
    public void glBeginTransformFeedback(int primitiveMode) {
        gl.glBeginTransformFeedback(primitiveMode);
    }

    @Override
    public void glEndTransformFeedback() {
        gl.glEndTransformFeedback();
    }

    @Override
    public void glBindBufferRange(int target, int index, int buffer, int offset, int size) {
        gl.glBindBufferRange(target, index, buffer, offset, size);
    }

    @Override
    public void glBindBufferBase(int target, int index, int buffer) {
        gl.glBindBufferBase(target, index, buffer);
    }

    @Override
    public void glTransformFeedbackVaryings(int program, CharSequence[] varyings, int bufferMode) {
        String[] strings = null;
        if(varyings instanceof String[]){
            strings = (String[])(varyings);
        }else{
            strings= new String[varyings.length];
            for(int i = 0; i < strings.length;i++){
                strings[i] = varyings[i].toString();
            }
        }

        gl.glTransformFeedbackVaryings(program, strings.length, strings, bufferMode);
    }

    @Override
    public void glVertexAttribIPointer(int index, int size, int type, int stride, int offset) {
        gl.glVertexAttribIPointer(index, size, type, stride, offset);
    }

    @Override
    public void glGetVertexAttribIiv(int index, int pname, IntBuffer params) {
        gl.glGetVertexAttribIiv(index, pname, params);
    }

    @Override
    public void glGetVertexAttribIuiv(int index, int pname, IntBuffer params) {
        gl.glGetVertexAttribIuiv(index, pname, params);
    }

    @Override
    public void glVertexAttribI4i(int index, int x, int y, int z, int w) {
        gl.glVertexAttribI4i(index, x, y, z, w);
    }

    @Override
    public void glVertexAttribI4ui(int index, int x, int y, int z, int w) {
        gl.glVertexAttribI4ui(index, x, y, z, w);
    }

    @Override
    public void glGetUniformuiv(int program, int location, IntBuffer params) {
        gl.glGetUniformuiv(program, location, params);
    }

    @Override
    public int glGetFragDataLocation(int program, String name) {
        return gl.glGetFragDataLocation(program, name);
    }

    @Override
    public void glUniform1uiv(int location, IntBuffer value) {
        gl.glUniform1uiv(location, value.remaining(), value);
    }

    @Override
    public void glUniform3uiv(int location, IntBuffer value) {
        gl.glUniform3uiv(location, value.remaining()/3, value);
    }

    @Override
    public void glUniform4uiv(int location, IntBuffer value) {
        gl.glUniform4uiv(location, value.remaining()/4, value);
    }

    @Override
    public void glClearBufferiv(int buffer, int drawbuffer, IntBuffer value) {
        gl.glClearBufferiv(buffer, drawbuffer, value);
    }

    @Override
    public void glClearBufferuiv(int buffer, int drawbuffer, IntBuffer value) {
        gl.glClearBufferuiv(buffer, drawbuffer, value);
    }

    @Override
    public void glClearBufferfv(int buffer, int drawbuffer, FloatBuffer value) {
        gl.glClearBufferfv(buffer, drawbuffer, value);
    }

    @Override
    public void glClearBufferfi(int buffer, int drawbuffer, float depth, int stencil) {
        gl.glClearBufferfi(buffer, drawbuffer, depth, stencil);
    }

    @Override
    public String glGetStringi(int name, int index) {
        return gl.glGetStringi(name, index);
    }

    @Override
    public void glCopyBufferSubData(int readTarget, int writeTarget, int readOffset, int writeOffset, int size) {
        gl.glCopyBufferSubData(readTarget,writeTarget,readOffset,writeOffset,size);
    }

    @Override
    public void glGetUniformIndices(int program, String[] uniformNames, IntBuffer uniformIndices) {
        gl.glGetUniformIndices(program, uniformNames.length, uniformNames, uniformIndices);
    }

    @Override
    public void glGetActiveUniformsiv(int program, IntBuffer uniformIndices, int pname, IntBuffer params) {
        gl.glGetActiveUniformsiv(program, uniformIndices.remaining(), uniformIndices, pname, params);
    }

    @Override
    public int glGetUniformBlockIndex(int program, String uniformBlockName) {
        return gl.glGetUniformBlockIndex(program, uniformBlockName);
    }

    @Override
    public void glGetActiveUniformBlockiv(int program, int uniformBlockIndex, int pname, IntBuffer params) {
        gl.glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, params);
    }

    @Override
    public void glGetActiveUniformBlockName(int program, int uniformBlockIndex, IntBuffer length, ByteBuffer uniformBlockName) {
        gl.glGetActiveUniformBlockName(program, uniformBlockIndex, uniformBlockName.remaining(), length, uniformBlockName);
    }

    @Override
    public String glGetActiveUniformBlockName(int program, int uniformBlockIndex) {
//        return gl.glGetActiveUniformBlockName(program, uniformBlockIndex);
        throw new RuntimeException("Unimplemented!");
    }


    @Override
    public void glUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding) {
        gl.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
    }

    @Override
    public void glDrawArraysInstanced(int mode, int first, int count, int instanceCount) {
        gl.glDrawArraysInstanced(mode, first, count, instanceCount);
    }

    @Override
    public void glDrawElementsInstanced(int mode, int count, int type, int indicesOffset, int instanceCount) {
        gl.glDrawElementsInstanced(mode,count, type, indicesOffset, instanceCount);
    }

    @Override
    public void glGetInteger64v(int pname, LongBuffer params) {
        gl.glGetInteger64v(pname, params);
    }

    @Override
    public void glGetBufferParameteri64v(int target, int pname, LongBuffer params) {
        gl.glGetBufferParameteri64v(target, pname, params);
    }

    @Override
    public int glGenSampler() {
        gl.glGenSamplers(1,intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGenSamplers(IntBuffer samplers) {
        gl.glGenSamplers(samplers.remaining(), samplers);
    }

    @Override
    public void glDeleteSampler(int sampler) {
        gl.glDeleteSamplers(1,wrap(sampler), 0);
    }

    @Override
    public void glDeleteSamplers(IntBuffer samplers) {
        gl.glDeleteSamplers(samplers.remaining(), samplers);
    }

    @Override
    public boolean glIsSampler(int sampler) {
        return gl.glIsSampler(sampler);
    }

    @Override
    public void glBindSampler(int unit, int sampler) {
        gl.glBindSampler(unit, sampler);
    }

    @Override
    public void glSamplerParameteri(int sampler, int pname, int param) {
        gl.glSamplerParameteri(sampler, pname, param);
    }

    @Override
    public void glSamplerParameteriv(int sampler, int pname, IntBuffer param) {
        gl.glSamplerParameteriv(sampler, pname, param);
    }

    @Override
    public void glSamplerParameterf(int sampler, int pname, float param) {
        gl.glSamplerParameterf(sampler, pname, param);
    }

    @Override
    public void glSamplerParameterfv(int sampler, int pname, FloatBuffer param) {
        gl.glSamplerParameterfv(sampler, pname, param);
    }

    @Override
    public void glGetSamplerParameteriv(int sampler, int pname, IntBuffer params) {
        gl.glGetSamplerParameteriv(sampler, pname, params);
    }

    @Override
    public void glGetSamplerParameterfv(int sampler, int pname, FloatBuffer params) {
        gl.glGetSamplerParameterfv(sampler, pname, params);
    }

    @Override
    public void glVertexAttribDivisor(int index, int divisor) {
        gl.glVertexAttribDivisor(index, divisor);
    }

    @Override
    public void glBindTransformFeedback(int target, int id) {
        gl.glBindTransformFeedback(target, id);
    }

    @Override
    public void glDeleteTransformFeedback(int feedback) {
        gl.glDeleteTransformFeedbacks(1,wrap(feedback),0);
    }

    @Override
    public void glDeleteTransformFeedbacks(IntBuffer ids) {
        gl.glDeleteTransformFeedbacks(ids.remaining(), ids);
    }

    @Override
    public int glGenTransformFeedback() {
        gl.glGenTransformFeedbacks(1, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGenTransformFeedbacks(IntBuffer ids) {
        gl.glGenTransformFeedbacks(ids.remaining(), ids);
    }

    @Override
    public boolean glIsTransformFeedback(int id) {
        return gl.glIsTransformFeedback(id);
    }

    @Override
    public void glPauseTransformFeedback() {
        gl.glPauseTransformFeedback();
    }

    @Override
    public void glResumeTransformFeedback() {
        gl.glResumeTransformFeedback();
    }

    @Override
    public void glProgramBinary(int program, int binaryFormat, ByteBuffer binary) {

    }

    @Override
    public void glProgramParameteri(int program, int pname, int value) {
        gl.glProgramParameteri(program, pname, value);
    }

    @Override
    public void glInvalidateFramebuffer(int target, IntBuffer attachments) {
        gl.glInvalidateFramebuffer(target, attachments.remaining(), attachments);
    }

    @Override
    public void glInvalidateSubFramebuffer(int target, IntBuffer attachments, int x, int y, int width, int height) {
        gl.glInvalidateSubFramebuffer(target, attachments.remaining(), attachments, x,y,width,height);
    }

    @Override
    public void glGetProgramBinary(int program, int[] length, int[] binaryFormat, ByteBuffer binary) {
        gl.glGetProgramBinary(program, binary.remaining(), length, 0, binaryFormat, 0, binary);
    }

    @Override
    public void glBindImageTexture(int unit, int texture, int level, boolean layered, int layer, int access, int format) {
        gl.glBindImageTexture(unit, texture, level, layered, layer, access, format);
    }

    @Override
    public void glDispatchCompute(int num_groups_x, int num_groups_y, int num_groups_z) {
        gl.glDispatchCompute(num_groups_x, num_groups_y, num_groups_z);
    }

    @Override
    public void glMemoryBarrier(int barriers) {
        gl.glMemoryBarrier(barriers);
    }

    @Override
    public void glPolygonMode(int face, int mode) {
        gl.glPolygonMode(face, mode);
    }

    @Override
    public void glPatchParameterfv(int pname, float[] value) {
        gl.glPatchParameterfv(pname, value, 0);
    }

    @Override
    public void glPatchParameteri(int pname, int value) {
        gl.glPatchParameteri(pname, value);
    }

    @Override
    public void glBufferData(int target, Buffer data, int mode) {
        gl.glBufferData(target, BufferUtils.measureSize(data),data, mode);
    }

    @Override
    public void glBufferData(int target, int size, int mode) {
        gl.glBufferData(target, size, null, mode);
    }

    @Override
    public void glTextureParameteri(int textureID, int pname, int mode) {
        gl.glTextureParameteri(textureID, pname, mode);
    }

    @Override
    public void glTextureParameteriv(int textureID, int pname, int[] rgba) {
        gl.glTextureParameteriv(textureID, pname, rgba, 0);
    }

    @Override
    public void glTextureParameterf(int textureID, int pname, float mode) {
        gl.glTextureParameterf(textureID, pname, mode);
    }

    @Override
    public void glTextureParameterfv(int textureID, int pname, float[] mode) {
        gl.glTextureParameterfv(textureID, pname, mode, 0);
    }

    @Override
    public int glGetTexLevelParameteri(int target, int level, int pname) {
        gl.glGetTexLevelParameteriv(target, level, pname, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGetTexImage(int target, int level, int format, int type, ByteBuffer result) {
        gl.glGetTexImage(target, level, format, type, result);
    }

    @Override
    public void glGetIntegerv(int pname, IntBuffer values) {
        gl.glGetIntegerv(pname,values);
    }

    @Override
    public void glTextureView(int dstTexture, int target, int srcTexture, int srcFormat, int minlevel, int numlevels, int minlayer, int numlayers) {
        gl.glTextureView(dstTexture, target, srcTexture, srcFormat, minlevel, numlevels, minlayer, numlayers);
    }

    @Override
    public int glCreateTextures(int target) {
        gl.glCreateTextures(target, 1,intValues, 0);
        return intValues[0];
    }

    @Override
    public void glTextureStorage3D(int textureID, int mipLevels, int format, int width, int height, int depth) {
        gl.glTextureStorage3D(textureID, mipLevels, format, width, height, depth);
    }

    @Override
    public void glTexStorage3D(int glTexture2dArray, int mipLevels, int format, int width, int height, int depth) {
        gl.glTexStorage3D(glTexture2dArray, mipLevels, format, width, height, depth);
    }

    @Override
    public void glTextureStorage2DMultisample(int textureID, int sampleCount, int format, int width, int height, boolean fixedsamplelocations) {
        gl.glTextureStorage2DMultisample(textureID, sampleCount, format, width, height, fixedsamplelocations);
    }

    @Override
    public void glTextureStorage3DMultisample(int textureID, int sampleCount, int format, int width, int height, int arraySize, boolean fixedsamplelocations) {
        gl.glTextureStorage3DMultisample(textureID, sampleCount, format, width, height, arraySize, fixedsamplelocations);
    }

    @Override
    public void glTexStorage2DMultisample(int target, int sampleCount, int format, int width, int height, boolean fixedsamplelocations) {
        gl.glTexStorage2DMultisample(target, sampleCount, format, width, height, fixedsamplelocations);
    }

    @Override
    public void glTexImage2DMultisample(int target, int sampleCount, int format, int width, int height, boolean fixedsamplelocations) {
        gl.glTexImage2DMultisample(target, sampleCount, format, width, height, fixedsamplelocations);
    }

    @Override
    public void glTextureStorage2D(int textureID, int mipLevels, int format, int width, int height) {
        gl.glTextureStorage2D(textureID, mipLevels, format, width, height);
    }

    @Override
    public void glTexStorage3DMultisample(int target, int sampleCount, int format, int width, int height, int arraySize, boolean fixedsamplelocations) {
        gl.glTexStorage3DMultisample(target, sampleCount, format, width, height, arraySize, fixedsamplelocations);
    }

    @Override
    public void glTexImage3DMultisample(int target, int sampleCount, int format, int width, int height, int arraySize, boolean fixedsamplelocations) {
        gl.glTexImage3DMultisample(target, sampleCount, format, width, height, arraySize, fixedsamplelocations);
    }

    @Override
    public void glTexStorage2D(int target, int mipLevels, int format, int width, int height) {
        gl.glTexStorage2D(target, mipLevels, format, width, height);
    }

    @Override
    public void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, ByteBuffer data) {
        gl.glCompressedTexImage2D(target, level, internalformat, width, height, border, data.remaining(), data);
    }

    @Override
    public void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, int image_size, long offset) {
        gl.glCompressedTexImage2D(target, level, internalformat, width, height, border, image_size, offset);
    }

    @Override
    public void glCompressedTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int boder, ByteBuffer byteBuffer) {
        gl.glCompressedTexImage3D(target, level, internalformat, width, height, depth, boder, byteBuffer.remaining(), byteBuffer);
    }

    @Override
    public void glCompressedTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int boder, int image_size, long offset) {
        gl.glCompressedTexImage3D(target, level, internalformat, width, height, depth, boder, image_size, offset);
    }

    @Override
    public void glTextureSubImage2D(int texture, int level, int x_offset, int y_offset, int width, int height, int format, int type, Buffer pixels) {
        gl.glTextureSubImage2D(texture, level, x_offset, y_offset, width, height, format, type, pixels);
    }

    @Override
    public void glTextureSubImage2D(int texture, int level, int x_offset, int y_offset, int width, int height, int format, int type, long data_offset) {
        gl.glTextureSubImage2D(texture, level, x_offset, y_offset, width, height, format, type, data_offset);
    }

    @Override
    public void glTextureSubImage3D(int textureID, int level, int x_offset, int y_offset, int z_offset, int width, int height, int depth, int format, int type, Buffer pixels) {
        gl.glTextureSubImage3D(textureID, level, x_offset, y_offset, z_offset, width, height, depth, format, type, pixels);
    }

    @Override
    public void glTextureSubImage3D(int textureID, int level, int x_offset, int y_offset, int z_offset, int width, int height, int depth, int format, int type, long offset) {
        gl.glTextureSubImage3D(textureID, level, x_offset, y_offset, z_offset, width, height, depth, format, type, offset);
    }

    @Override
    public int glGetVertexAttribi(int index, int pname) {
        gl.glGetVertexAttribiv(index, pname, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glFramebufferTexture(int target, int attachment, int texture, int level) {
        gl.glFramebufferTexture(target, attachment, texture, level);
    }

    @Override
    public void glFramebufferTexture1D(int target, int attachment, int texturetarget, int texture, int level) {
        gl.glFramebufferTexture1D(target, attachment, texturetarget, texture, level);
    }

    @Override
    public void glFramebufferTexture3D(int target, int attachment, int texturetarget, int texture, int level, int layer) {
        gl.glFramebufferTexture3D(target, attachment, texturetarget, texture, level, layer);
    }

    @Override
    public void glBindProgramPipeline(int programPipeline) {
        gl.glBindProgramPipeline(programPipeline);
    }

    @Override
    public void glDeleteProgramPipeline(int programPipeline) {
        gl.glDeleteProgramPipelines(1, wrap(programPipeline), 0);
    }

    @Override
    public int glGenProgramPipeline() {
        gl.glGenProgramPipelines(1, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glUseProgramStages(int programPipeline, int shaderBit, int program) {
        gl.glUseProgramStages(programPipeline, shaderBit, program);
    }

    @Override
    public boolean glIsProgramPipeline(int programPipeline) {
        return gl.glIsProgramPipeline(programPipeline);
    }

    @Override
    public void glBindTextures(int first, IntBuffer texturenames) {
        gl.glBindTextures(first, texturenames.remaining(), texturenames);
    }

    @Override
    public void glBindTextureUnit(int unit, int texture) {
        gl.glBindTextureUnit(unit, texture);
    }

    @Override
    public void glBindSamplers(int first, IntBuffer samplernames) {
        gl.glBindSamplers(first, samplernames.remaining(), samplernames);
    }

    @Override
    public void glDrawElementsInstancedBaseVertex(int mode, int count, int type, int offset, int instance_count, int base_vertex) {
        gl.glDrawElementsInstancedBaseVertex(mode, count, type, offset, instance_count, base_vertex);
    }

    @Override
    public NativeAPI getNativeAPI() {
        return null;
    }

    @Override
    public int glGetIntegeri(int panme, int index) {
        gl.glGetIntegeri_v(panme, index, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glSampleMaski(int index, int mask) {
        gl.glSampleMaski(index, mask);
    }

    @Override
    public void glCopyImageSubData(int srcName, int srcTarget, int srcLevel, int srcX, int srcY, int srcZ, int dstName, int dstTarget, int dstLevel, int dstX, int dstY, int dstZ, int srcWidth, int srcHeight, int srcDepth) {
        gl.glCopyImageSubData(srcName, srcTarget, srcLevel, srcX, srcY, srcZ, dstName, dstTarget, dstLevel, dstX, dstY, dstZ, srcWidth, srcHeight, srcDepth);
    }

    @Override
    public void glPrimitiveRestartIndex(int restartIndex) {
        gl.glPrimitiveRestartIndex(restartIndex);
    }

    @Override
    public int glCreateShaderProgramv(int target, CharSequence source) {
        return gl.glCreateShaderProgramv(target, 1, new String[]{source.toString()});
    }

    @Override
    public void glProgramUniform1f(int program, int index, float radius) {
        gl.glProgramUniform1f(program, index, radius);
    }

    @Override
    public void glProgramUniform2f(int program, int index, float x, float y) {
        gl.glProgramUniform2f(program, index, x, y);
    }

    @Override
    public void glProgramUniform4f(int program, int index, float x, float y, float z, float w) {
        gl.glProgramUniform4f(program, index, x, y, z, w);
    }

    @Override
    public void glProgramUniformMatrix4fv(int program, int index, boolean transpose, FloatBuffer data) {
        gl.glProgramUniformMatrix4fv(program, index, data.remaining()/16, transpose, data);
    }

    @Override
    public void glProgramUniform4fv(int program, int index, FloatBuffer data) {
        gl.glProgramUniform4fv(program, index, data.remaining()/4, data);
    }

    @Override
    public void glProgramUniform1fv(int program, int index, FloatBuffer data) {
        gl.glProgramUniform1fv(program, index, data.remaining(), data);
    }

    @Override
    public void glProgramUniform1i(int program, int index, int i) {
        gl.glProgramUniform1i(program, index, i);
    }

    @Override
    public void glProgramUniform4i(int program, int index, int x, int y, int z, int w) {
        gl.glProgramUniform4i(program, index, x, y, z, w);
    }

    @Override
    public void glProgramUniform2fv(int program, int index, FloatBuffer values) {
        gl.glProgramUniform2fv(program, index, values.remaining()/2, values);
    }

    @Override
    public int glGetUniformui(int programId, int location) {
        gl.glGetUniformuiv(programId, location, intValues, 0);
        return intValues[0];
    }

    @Override
    public double glGetUniformd(int programId, int location) {
        gl.glGetUniformdv(programId, location, doubleValues, 0);
        return doubleValues[0];
    }

    @Override
    public void glGetUniformdv(int programId, int location, DoubleBuffer buf) {
        gl.glGetUniformdv(programId, location, buf);
    }

    @Override
    public void glDrawElementsBaseVertex(int primType, int count, int type, int start, int baseVertex) {
        gl.glDrawElementsBaseVertex(primType, count, type, start, baseVertex);
    }

    @Override
    public void glTexBuffer(int target, int internalFormat, int buffer) {
        gl.glTexBuffer(target, internalFormat, buffer);
    }

    @Override
    public void glClearBufferData(int target, int internalformat, int format, int type, ByteBuffer data) {
        gl.glClearBufferData(target, internalformat, format, type, data);
    }

    @Override
    public void glGetBufferSubData(int target, int offset, int size, ByteBuffer data) {
        gl.glGetBufferSubData(target, offset, size, data);
    }

    @Override
    public void glDrawTransformFeedbackStream(int m_currentMode, int transformFeedback, int index) {
        gl.glDrawTransformFeedbackStream(m_currentMode, transformFeedback, index);
    }

    @Override
    public void glClearTexImage(int texture, int level, int format, int type, Buffer data) {
        gl.glClearTexImage(texture, level, format, type, data);
    }

    @Override
    public void glUniform1ui(int location, int i) {
        gl.glUniform1ui(location, i);
    }

    @Override
    public void glDispatchComputeIndirect(int indirect) {
        gl.glDispatchComputeIndirect(indirect);
    }

    @Override
    public void glDrawArraysIndirect(int mode, int indirect) {
        gl.glDrawArraysIndirect(mode, indirect);
    }

    @Override
    public int glGetProgramInterfacei(int program, int programInterface, int pname) {
        gl.glGetProgramInterfaceiv(program, programInterface, pname, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGetProgramInterfaceiv(int program, int programInterface, int pname, IntBuffer params) {
        gl.glGetProgramInterfaceiv(program, programInterface, pname, params);
    }

    @Override
    public void glGetProgramResourceiv(int program, int programInterface, int index, IntBuffer props, IntBuffer length, IntBuffer params) {
        gl.glGetProgramResourceiv(program, programInterface, index, props.remaining(), props, params.remaining(), length, params);
    }

    @Override
    public int glGetProgramResourceIndex(int program, int programInterface, CharSequence name) {
        byte[] bytes = new byte[name.length()];
        for(int i = 0; i < bytes.length; i++){
            bytes[i] = (byte) name.charAt(i);
        }
        return gl.glGetProgramResourceIndex(program, programInterface, bytes, 0);
    }

    @Override
    public int glGetProgramResourceLocation(int program, int programInterface, CharSequence name) {
        byte[] bytes = new byte[name.length()];
        for(int i = 0; i < bytes.length; i++){
            bytes[i] = (byte) name.charAt(i);
        }
        return gl.glGetProgramResourceLocation(program, programInterface, bytes, 0);
    }

    @Override
    public String glGetProgramResourceName(int program, int programInterface, int index, int bufSize) {
        byte[] bytes = new byte[bufSize];
        gl.glGetProgramResourceName(program, programInterface, index, bufSize, intValues, 0, bytes, 0);
        return new String(bytes, 0, intValues[0]);
    }

    @Override
    public int glGetProgramResourceLocationIndex(int program, int programInterface, CharSequence name) {
        byte[] bytes = new byte[name.length()];
        for(int i = 0; i < bytes.length; i++){
            bytes[i] = (byte) name.charAt(i);
        }
        return gl.glGetProgramResourceLocationIndex(program, programInterface, bytes, 0);
    }

    @Override
    public void glGenerateTextureMipmap(int texture) {
        gl.glGenerateTextureMipmap(texture);
    }

    @Override
    public void glQueryCounter(int id, int target) {
        gl.glQueryCounter(id, target);
    }

    @Override
    public long glGetQueryObjectui64ui(int id, int pname) {
        gl.glGetQueryObjectui64v(id, pname, longValues, 0);
        return longValues[0];
    }

    @Override
    public int glGetSubroutineIndex(int program, int type, String name) {
        return gl.glGetSubroutineIndex(program, type, name);
    }

    @Override
    public void glUniformSubroutinesui(int type, int index) {
        intValues[0] = index;
        gl.glUniformSubroutinesuiv(type, 1, intValues, 0);
    }

    @Override
    public void glVertexAttribFormat(int index, int size, int type, boolean normalized, long offset) {
        gl.glVertexAttribFormat(index, size, type, normalized, (int)offset);
    }

    @Override
    public void glVertexAttribBinding(int attribindex, int bindingindex) {
        gl.glVertexAttribBinding(attribindex, bindingindex);
    }

    @Override
    public void glBindVertexBuffer(int bindingindex, int buffer, long offset, int stride) {
        gl.glBindVertexBuffer(bindingindex, buffer, offset, stride);
    }

    @Override
    public void glTextureBuffer(int texture, int internalformat, int buffer) {
        gl.glTextureBuffer(texture, internalformat, buffer);
    }

    @Override
    public void glDrawElementsIndirect(int mode, int type, long indirect) {
        gl.glDrawElementsIndirect(mode, type, indirect);
    }

    @Override
    public void glTexBufferRange(int target, int internalFormat, int buffer, long offset, int size) {
        gl.glTexBufferRange(target, internalFormat, buffer, (int)offset, size);
    }

    @Override
    public void glTextureBufferRange(int texture, int internalformat, int buffer, long offset, int size) {
        gl.glTextureBufferRange(texture, internalformat, buffer, (int)offset, size);
    }

    @Override
    public long glFenceSync() {
        return gl.glFenceSync(GLenum.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
    }

    @Override
    public int glClientWaitSync(long sync, int flags, long timeout) {
        return gl.glClientWaitSync(sync, flags, timeout);
    }

    @Override
    public ByteBuffer glMapBufferRange(int target, int offset, int length, int access, ByteBuffer old_buffer) {
        return gl.glMapBufferRange(target, offset, length, access);
    }

    @Override
    public ByteBuffer glMapBufferRange(int target, int offset, int length, int access) {
        return gl.glMapBufferRange(target, offset, length, access);
    }

    @Override
    public void glDeleteSync(long sync) {
        gl.glDeleteSync(sync);
    }

    @Override
    public void glProgramUniform3f(int program, int index, float x, float y, float z) {
        gl.glProgramUniform3f(program, index, x, y, z);
    }

    @Override
    public void glScissorIndexed(int index, int x, int y, int width, int height) {
        gl.glScissorIndexed(index, x, y, width, height);
    }

    @Override
    public void glViewportIndexedf(int index, float x, float y, float width, float height) {
        gl.glViewportIndexedf(index, x, y, width, height);
    }

    @Override
    public void glBindImageTextures(int unit, IntBuffer images) {
        gl.glBindImageTextures(unit, images.remaining(), images);
    }

    @Override
    public void glViewportArrayv(int index, FloatBuffer viewports) {
        gl.glViewportArrayv(index, viewports.remaining()/4, viewports);
    }

    @Override
    public void glBindFragDataLocation(int program, int index, String name) {
        gl.glBindFragDataLocation(program, index, name);
    }

    @Override
    public void glClearNamedBufferData(int buffer, int internalformat, int format, int type, Buffer data) {
        gl.glClearNamedBufferData(buffer, internalformat, format, type, data);
    }

    @Override
    public void glClearNamedBufferSubData(int buffer, int internalformat, long offset, long size, int format, int type, Buffer data) {
        gl.glClearNamedBufferSubData(buffer, internalformat, offset, size, format, type, data);
    }

    @Override
    public void glGetMemoryInfo(GPUMemoryInfo info) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int glCreateBuffer() {
        gl.glCreateBuffers(1, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glBufferStorage(int target, long size, int flags) {
        gl.glBufferStorage(target, size, null, flags);
    }

    @Override
    public void glBufferStorage(int target, Buffer data, int flags) {
        gl.glBufferStorage(target, BufferUtils.measureSize(data), data, flags);
    }

    @Override
    public void glNamedBufferStorage(int buffer, long size, int flags) {
        gl.glNamedBufferStorage(buffer, size, null, flags);
    }

    @Override
    public void glNamedBufferStorage(int buffer, Buffer data, int flags) {
        gl.glNamedBufferStorage(buffer, BufferUtils.measureSize(data), data, flags);
    }

    @Override
    public void glCopyNamedBufferSubData(int readBuffer, int writeBuffer, long readOffset, long writeOffset, long size) {
        gl.glCopyNamedBufferSubData(readBuffer, writeBuffer, readOffset, writeOffset, size);
    }

    @Override
    public ByteBuffer glMapNamedBufferRange(int buffer, long offset, long length, int access, ByteBuffer old_buffer) {
        throw new UnsupportedOperationException("Unsupporte the 'glMapNamedBufferRange' in gogl");
    }

    @Override
    public ByteBuffer glMapNamedBufferRange(int buffer, long offset, long length, int access) {
        return gl.glMapNamedBufferRange(buffer, offset, length, access);
    }
}
