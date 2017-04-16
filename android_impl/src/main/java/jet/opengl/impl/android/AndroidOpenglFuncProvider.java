package jet.opengl.impl.android;

import android.annotation.TargetApi;
import android.opengl.GLES10;
import android.opengl.GLES11;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLES31;
import android.opengl.GLES31Ext;
import android.opengl.GLES32;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.HashSet;
import java.util.StringTokenizer;

import jet.opengl.postprocessing.common.GLAPI;
import jet.opengl.postprocessing.common.GLAPIVersion;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.util.BufferUtils;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/4/14.
 */

public class AndroidOpenglFuncProvider implements GLFuncProvider {

    private final HashSet<String> m_extensions = new HashSet<>();
    private GLAPIVersion m_GLVersion;
    private final int[] intValues = new int[1];
    private final boolean[] boolValues = new boolean[1];
    private final byte[] byteValues = new byte[1];
    private final float[] floatValues = new float[1];

    @Override
    public boolean isSupportExt(String ext) {
        if(m_extensions.isEmpty()){
            String exts = GLES11.glGetString(GLES10.GL_EXTENSIONS);
            StringTokenizer tokens = new StringTokenizer(exts);

            while(tokens.hasMoreElements()){
                m_extensions.add(tokens.nextToken());
            }
        }
        return m_extensions.contains(ext);
    }

    @Override
    public GLAPI getHostAPI() {
        return GLAPI.ANDROID;
    }

    @Override
    public GLAPIVersion getGLAPIVersion() {
        if(m_GLVersion == null){
            // TODO Not implemented
            m_GLVersion = new GLAPIVersion(true, 3, 2, false);
        }
        return m_GLVersion;
    }

    @Override
    public void glActiveTexture(int texture) {
        GLES11.glActiveTexture(texture);
    }

    @Override
    public void glBindTexture(int target, int texture) {
        GLES11.glBindTexture(target, texture);
    }

    @Override
    public void glBlendFunc(int sfactor, int dfactor) {
        GLES11.glBlendFunc(sfactor, dfactor);
    }

    @Override
    public void glClear(int mask) {
        GLES11.glClear(mask);
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        GLES11.glClearColor(red, green, blue, alpha);
    }

    @Override
    public void glClearDepthf(float depth) {
        GLES11.glClearDepthf(depth);
    }

    @Override
    public void glClearStencil(int s) {
        GLES11.glClearStencil(s);
    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GLES11.glColorMask(red, green, blue, alpha);
    }

    @Override
    public void glCopyTexImage2D(int target, int level, int internalformat, int x, int y, int width, int height, int border) {
        GLES11.glCopyTexImage2D(target, level, internalformat, x, y, width, height, border);
    }

    @Override
    public void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height) {
        GLES11.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
    }

    @Override
    public void glCullFace(int mode) {
        GLES11.glCullFace(mode);
    }

    @Override
    public void glDeleteTextures(int... textures) {
        if(textures == null)
            return;

        glDeleteTextures(textures, 0, textures.length);
    }

    @Override
    public void glDeleteTextures(int[] textures, int offset, int length) {
        GLES11.glDeleteTextures(length, textures, offset);
    }

    final int[] wrap(int i){ intValues[0] = i; return intValues;}

    @Override
    public void glDeleteTexture(int texture) {
        glDeleteTextures(wrap(texture), 0, 1);
    }

    @Override
    public void glDepthFunc(int func) {
        GLES11.glDepthFunc(func);
    }

    @Override
    public void glDepthMask(boolean flag) {
        GLES11.glDepthMask(flag);
    }

    @Override
    public void glDisable(int cap) {
        GLES11.glDisable(cap);
    }

    @Override
    public void glDrawArrays(int mode, int first, int count) {
        GLES11.glDrawArrays(mode, first, count);
    }

    @Override
    public void glDrawElements(int mode, int count, int type, Buffer indices) {
        GLES11.glDrawElements(mode, count, type, indices);
    }

    @Override
    public void glEnable(int cap) {
        GLES11.glEnable(cap);
    }

    @Override
    public void glFinish() {
        GLES11.glFinish();
    }

    @Override
    public void glFlush() {
        GLES11.glFlush();
    }

    @Override
    public void glFrontFace(int mode) {
        GLES11.glFrontFace(mode);
    }

    @Override
    public int glGenTexture() {
        GLES11.glGenTextures(1, intValues, 0);
        return intValues[0];
    }

    @Override
    public int glGetError() {
        return GLES10.glGetError();
    }

    @Override
    public int glGetInteger(int pname) {
        GLES11.glGetIntegerv(pname, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGetInteger(int pname, IntBuffer params) {
        GLES11.glGetIntegerv(pname, params);
    }

    @Override
    public String glGetString(int name) {
        return GLES11.glGetString(name);
    }

    @Override
    public void glHint(int target, int mode) {
        GLES11.glHint(target, mode);
    }

    @Override
    public void glLineWidth(float width) {
        GLES11.glLineWidth(width);
    }

    @Override
    public void glPixelStorei(int pname, int param) {
        GLES11.glPixelStorei(pname, param);
    }

    @Override
    public void glPolygonOffset(float factor, float units) {
        GLES11.glPolygonOffset(factor, units);
    }

    @Override
    public void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer pixels) {
        GLES11.glReadPixels(x, y, width, height, format, type, pixels);
    }

    @Override
    public void glScissor(int x, int y, int width, int height) {
        GLES11.glScissor(x, y, width, height);
    }

    @Override
    public void glStencilFunc(int func, int ref, int mask) {
        GLES11.glStencilFunc(func, ref, mask);
    }

    @Override
    public void glStencilMask(int mask) {
        GLES11.glStencilMask(mask);
    }

    @Override
    public void glStencilOp(int fail, int zfail, int zpass) {
        GLES11.glStencilOp(fail, zfail, zpass);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, Buffer pixels) {
        GLES11.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, long pixels_offset) {
        if(pixels_offset != 0L){
            final String error_msg = "Unspport glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, long pixels_offset) on Android platform!";
            GLCheck.printUnsupportFuncError(error_msg);
        }else{
            GLES11.glTexImage2D(target, level, internalformat, width, height, border, format, type, null);
        }
    }

    @Override
    public void glTexParameterf(int target, int pname, float param) {
        GLES11.glTexParameterf(target, pname, param);
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, Buffer pixels) {
        GLES11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, long pixels_offset) {
        if(pixels_offset != 0L){
            final String error_msg = "Unspport glTexSubImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, long pixels_offset) on Android platform!";
            GLCheck.printUnsupportFuncError(error_msg);
        }else{
            GLES11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, null);
        }
    }

    @Override
    public void glViewport(int x, int y, int width, int height) {
        GLES11.glViewport(x, y, width, height);
    }

    @Override
    public void glAttachShader(int program, int shader) {
        GLES20.glAttachShader(program, shader);
    }

    @Override
    public void glBindAttribLocation(int program, int index, CharSequence name) {
        GLES20.glBindAttribLocation(program, index, name.toString());
    }

    @Override
    public void glBindBuffer(int target, int buffer) {
        GLES11.glBindBuffer(target, buffer);
    }

    @Override
    public void glBindFramebuffer(int target, int framebuffer) {
        GLES20.glBindFramebuffer(target, framebuffer);
    }

    @Override
    public void glBindRenderbuffer(int target, int renderbuffer) {
        GLES20.glBindRenderbuffer(target, renderbuffer);
    }

    @Override
    public void glBlendColor(float red, float green, float blue, float alpha) {
        GLES20.glBlendColor(red, green, blue, alpha);
    }

    @Override
    public void glBlendEquation(int mode) {
        GLES20.glBlendEquation(mode);
    }

    @Override
    public void glBlendEquationSeparate(int modeRGB, int modeAlpha) {
        GLES20.glBlendEquationSeparate(modeRGB, modeAlpha);
    }

    @Override
    public void glBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        GLES20.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
    }

    @Override
    public void glBufferSubData(int target, int offset, Buffer data) {
        GLES20.glBufferSubData(target, offset, BufferUtils.measureSize(data), data);
    }

    @Override
    public int glCheckFramebufferStatus(int target) {
        return GLES20.glCheckFramebufferStatus(target);
    }

    @Override
    public void glCompileShader(int shader) {
        GLES20.glCompileShader(shader);
    }

    @Override
    public int glCreateProgram() {
        return GLES20.glCreateProgram();
    }

    @Override
    public int glCreateShader(int type) {
        return GLES20.glCreateShader(type);
    }

    @Override
    public void glDeleteBuffer(int buffer) {
        GLES11.glDeleteBuffers(1, wrap(buffer), 0);
    }

    @Override
    public void glDeleteFramebuffer(int framebuffer) {
        GLES20.glDeleteFramebuffers(1, wrap(framebuffer), 0);
    }

    @Override
    public void glDeleteProgram(int program) {
        GLES20.glDeleteProgram(program);
    }

    @Override
    public void glDeleteRenderbuffer(int renderbuffer) {
        GLES20.glDeleteRenderbuffers(1, wrap(renderbuffer), 0);
    }

    @Override
    public void glDeleteShader(int shader) {
        GLES20.glDeleteShader(shader);
    }

    @Override
    public void glDetachShader(int program, int shader) {
        GLES20.glDetachShader(program, shader);
    }

    @Override
    public void glDisableVertexAttribArray(int index) {
        GLES20.glDisableVertexAttribArray(index);
    }

    @Override
    public void glDrawElements(int mode, int count, int type, long indices) {
        GLES11.glDrawElements(mode, count, type, (int)indices);
    }

    @Override
    public void glEnableVertexAttribArray(int index) {
        GLES20.glEnableVertexAttribArray(index);
    }

    @Override
    public void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {
        GLES20.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
    }

    @Override
    public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        GLES20.glFramebufferTexture2D(target, attachment, textarget, texture, level);
    }

    @Override
    public int glGenBuffer() {
        GLES20.glGenBuffers(1, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGenBuffers(IntBuffer buffers) {
        GLES20.glGenBuffers(buffers.remaining(), buffers);
    }

    @Override
    public void glGenerateMipmap(int target) {
        GLES20.glGenerateMipmap(target);
    }

    @Override
    public int glGenFramebuffer() {
        GLES20.glGenFramebuffers(1, intValues, 0);
        return intValues[0];
    }

    @Override
    public int glGenRenderbuffer() {
        GLES20.glGenRenderbuffers(1, intValues, 0);
        return intValues[0];
    }

    @Override
    public String glGetActiveAttrib(int program, int index, int maxLength, IntBuffer size, IntBuffer type) {
        return GLES20.glGetActiveAttrib(program, index, size, type);
    }

    @Override
    public String glGetActiveUniform(int program, int index, int maxLength, IntBuffer size, IntBuffer type) {
        return GLES20.glGetActiveUniform(program, index, size, type);
    }

    @Override
    public void glGetAttachedShaders(int program, int[] count, int[] shaders) {
        GLES20.glGetAttachedShaders(program, count.length, count, 0, shaders, 0);
    }

    @Override
    public int glGetAttribLocation(int program, CharSequence name) {
        return GLES20.glGetAttribLocation(program, name.toString());
    }

    @Override
    public boolean glGetBoolean(int pname) {
        GLES11.glGetBooleanv(pname, boolValues, 0);
        return boolValues[0];
    }

    @Override
    public void glGetBooleanv(int pname, ByteBuffer params) {
//        GLES11.glGetBooleanv();
        throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public int glGetBufferParameteri(int target, int pname) {
        GLES20.glGetBufferParameteriv(target, pname, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGetBufferParameteriv(int target, int pname, IntBuffer params) {
        GLES20.glGetBufferParameteriv(target, pname, params);
    }

    @Override
    public float glGetFloat(int pname) {
        GLES11.glGetFloatv(pname, floatValues, 0);
        return floatValues[0];
    }

    @Override
    public void glGetFloatv(int pname, FloatBuffer params) {
        GLES11.glGetFloatv(pname, params);
    }

    @Override
    public int glGetProgrami(int program, int pname) {
        GLES20.glGetProgramiv(program, pname, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGetProgramiv(int program, int pname, IntBuffer params) {
        GLES20.glGetProgramiv(program, pname, params);
    }

    @Override
    public String glGetProgramInfoLog(int program) {
        return GLES20.glGetProgramInfoLog(program);
    }

    @Override
    public int glGetShaderi(int shader, int pname) {
        GLES20.glGetShaderiv(shader, pname, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGetShaderiv(int shader, int pname, IntBuffer params) {
        GLES20.glGetShaderiv(shader, pname, params);
    }

    @Override
    public String glGetShaderInfoLog(int shader) {
        return GLES20.glGetShaderInfoLog(shader);
    }

    @Override
    public float glGetTexParameterf(int target, int pname) {
        GLES20.glGetTexParameterfv(target, pname, floatValues, 0);
        return floatValues[0];
    }

    @Override
    public void glGetTexParameterfv(int target, int pname, FloatBuffer params) {
        GLES20.glGetTexParameterfv(target, pname, params);
    }

    @Override
    public int glGetTexParameteri(int target, int pname) {
        GLES20.glGetTexParameteriv(target, pname, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGetTexParameteriv(int target, int pname, IntBuffer params) {
        GLES20.glGetTexParameteriv(target, pname, params);
    }

    @Override
    public float glGetUniformf(int program, int location) {
        GLES20.glGetUniformfv(program, location, floatValues, 0);
        return floatValues[0];
    }

    @Override
    public void glGetUniformfv(int program, int location, FloatBuffer params) {
        GLES20.glGetUniformfv(program, location, params);
    }

    @Override
    public int glGetUniformi(int program, int location) {
        GLES20.glGetUniformiv(program, location, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGetUniformiv(int program, int location, IntBuffer params) {
        GLES20.glGetUniformiv(program, location, params);
    }

    @Override
    public int glGetUniformLocation(int program, CharSequence name) {
        return GLES20.glGetUniformLocation(program, name.toString());
    }

    @Override
    public boolean glIsBuffer(int buffer) {
        return GLES20.glIsBuffer(buffer);
    }

    @Override
    public boolean glIsEnabled(int cap) {
        return GLES20.glIsEnabled(cap );
    }

    @Override
    public boolean glIsFramebuffer(int framebuffer) {
        return GLES20.glIsFramebuffer(framebuffer);
    }

    @Override
    public boolean glIsProgram(int program) {
        return GLES20.glIsProgram(program);
    }

    @Override
    public boolean glIsRenderbuffer(int renderbuffer) {
        return GLES20.glIsRenderbuffer(renderbuffer);
    }

    @Override
    public boolean glIsShader(int shader) {
        return GLES20.glIsShader(shader);
    }

    @Override
    public boolean glIsTexture(int texture) {
        return GLES20.glIsTexture(texture);
    }

    @Override
    public void glLinkProgram(int program) {
        GLES20.glLinkProgram(program);
    }

    @Override
    public void glReleaseShaderCompiler() {
        GLES20.glReleaseShaderCompiler();
    }

    @Override
    public void glRenderbufferStorage(int target, int internalformat, int width, int height) {
        GLES20.glRenderbufferStorage(target, internalformat, width, height);
    }

    @Override
    public void glSampleCoverage(float value, boolean invert) {
        GLES20.glSampleCoverage(value, invert);
    }

    @Override
    public void glShaderBinary(IntBuffer shaders, int binaryformat, ByteBuffer binary) {
        GLES20.glShaderBinary(shaders.remaining(), shaders, binaryformat, binary, binary.remaining());
    }

    @Override
    public void glShaderSource(int shader, CharSequence string) {
        GLES20.glShaderSource(shader, string.toString());
    }

    @Override
    public void glStencilFuncSeparate(int face, int func, int ref, int mask) {
        GLES20.glStencilFuncSeparate(face, func, ref, mask);
    }

    @Override
    public void glStencilMaskSeparate(int face, int mask) {
        GLES20.glStencilMaskSeparate(face, mask);
    }

    @Override
    public void glStencilOpSeparate(int face, int fail, int zfail, int zpass) {
        GLES20.glStencilOpSeparate(face, fail, zfail, zpass);
    }

    @Override
    public void glTexParameterfv(int target, int pname, FloatBuffer params) {
        GLES20.glTexParameterfv(target, pname, params);
    }

    @Override
    public void glTexParameteri(int target, int pname, int param) {
        GLES20.glTexParameteri(target, pname, param);
    }

    @Override
    public void glTexParameteriv(int target, int pname, int[] params) {
        GLES20.glTexParameteriv(target, pname, params, 0);
    }

    @Override
    public void glUniform1f(int location, float x) {
        GLES20.glUniform1f(location, x);
    }

    @Override
    public void glUniform1fv(int location, FloatBuffer v) {
        GLES20.glUniform1fv(location, v.remaining(), v);
    }

    @Override
    public void glUniform1i(int location, int x) {
        GLES20.glUniform1i(location, x);
    }

    @Override
    public void glUniform1iv(int location, IntBuffer v) {
        GLES20.glUniform1iv(location, v.remaining(), v);
    }

    @Override
    public void glUniform2f(int location, float x, float y) {
        GLES20.glUniform2f(location, x, y);
    }

    @Override
    public void glUniform2fv(int location, FloatBuffer v) {
        GLES20.glUniform2fv(location, v.remaining()/2, v);
    }

    @Override
    public void glUniform2i(int location, int x, int y) {
        GLES20.glUniform2i(location,x,y);
    }

    @Override
    public void glUniform2iv(int location, IntBuffer v) {
        GLES20.glUniform2iv(location, v.remaining()/2, v);
    }

    @Override
    public void glUniform3f(int location, float x, float y, float z) {
        GLES20.glUniform3f(location, x, y, z);
    }

    @Override
    public void glUniform3fv(int location, FloatBuffer v) {
        GLES20.glUniform3fv(location, v.remaining()/3, v);
    }

    @Override
    public void glUniform3i(int location, int x, int y, int z) {
        GLES20.glUniform3i(location, x, y, z);
    }

    @Override
    public void glUniform3iv(int location, IntBuffer v) {
        GLES20.glUniform3iv(location, v.remaining()/3, v);
    }

    @Override
    public void glUniform4f(int location, float x, float y, float z, float w) {
        GLES20.glUniform4f(location, x, y, z, w);
    }

    @Override
    public void glUniform4fv(int location, FloatBuffer v) {
        GLES20.glUniform4fv(location, v.remaining()/4, v);
    }

    @Override
    public void glUniform4i(int location, int x, int y, int z, int w) {
        GLES20.glUniform4i(location, x, y, z, w);
    }

    @Override
    public void glUniform4iv(int location, IntBuffer v) {
        GLES20.glUniform4iv(location, v.remaining()/4, v);
    }

    @Override
    public void glUniformMatrix2fv(int location, boolean transpose, FloatBuffer value) {
        GLES20.glUniformMatrix2fv(location, value.remaining()/4, transpose, value);
    }

    @Override
    public void glUniformMatrix3fv(int location, boolean transpose, FloatBuffer value) {
        GLES20.glUniformMatrix3fv(location, value.remaining()/9, transpose, value);
    }

    @Override
    public void glUniformMatrix4fv(int location, boolean transpose, FloatBuffer value) {
        GLES20.glUniformMatrix4fv(location, value.remaining()/16, transpose, value);
    }

    @Override
    public void glUseProgram(int program) {
        GLES20.glUseProgram(program);
    }

    @Override
    public void glValidateProgram(int program) {
        GLES20.glValidateProgram(program);
    }

    @Override
    public void glVertexAttrib1f(int indx, float x) {
        GLES20.glVertexAttrib1f(indx, x);
    }

    @Override
    public void glVertexAttrib1fv(int indx, FloatBuffer values) {
        GLES20.glVertexAttrib1fv(indx, values);
    }

    @Override
    public void glVertexAttrib2f(int indx, float x, float y) {
        GLES20.glVertexAttrib2f(indx, x, y);
    }

    @Override
    public void glVertexAttrib2fv(int indx, FloatBuffer values) {
        GLES20.glVertexAttrib2fv(indx, values);
    }

    @Override
    public void glVertexAttrib3f(int indx, float x, float y, float z) {
        GLES20.glVertexAttrib3f(indx, x, y, z);
    }

    @Override
    public void glVertexAttrib3fv(int indx, FloatBuffer values) {
        GLES20.glVertexAttrib3fv(indx, values);
    }

    @Override
    public void glVertexAttrib4f(int indx, float x, float y, float z, float w) {
        GLES20.glVertexAttrib4f(indx, x, y, z, w);
    }

    @Override
    public void glVertexAttrib4fv(int indx, FloatBuffer values) {
        GLES20.glVertexAttrib4fv(indx, values);
    }

    @Override
    public void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, Buffer ptr) {
        GLES20.glVertexAttribPointer(indx, size, type, normalized, stride, ptr);
    }

    @Override
    public void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, int ptr) {
        GLES20.glVertexAttribPointer(indx, size, type, normalized, stride, ptr);
    }

    @Override
    public void glReadBuffer(int mode) {
        GLES30.glReadBuffer(mode);
    }

    @Override
    public void glDrawRangeElements(int mode, int start, int end, int count, int type, Buffer indices) {
        GLES30.glDrawRangeElements(mode, start, end, count, type, indices);
    }

    @Override
    public void glDrawRangeElements(int mode, int start, int end, int count, int type, int offset) {
        GLES30.glDrawRangeElements(mode, start, end, count, type, offset);
    }

    @Override
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, Buffer pixels) {
        GLES30.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, pixels);
    }

    @Override
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, long offset) {
        GLES30.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, (int)offset);
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, Buffer pixels) {
        GLES30.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, pixels);
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, long pixels_offset) {
        GLES30.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, (int)pixels_offset);
    }

    @Override
    public void glCopyTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int x, int y, int width, int height) {
        GLES30.glCopyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height);
    }

    @Override
    public int glGenQuery() {
        GLES30.glGenQueries(1, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGenQueries(IntBuffer ids) {
        GLES30.glGenQueries(ids.remaining(), ids);
    }

    @Override
    public void glDeleteQuery(int query) {
        GLES30.glDeleteQueries(1, wrap(query), 0);
    }

    @Override
    public void glDeleteQueries(IntBuffer ids) {
        GLES30.glDeleteQueries(ids.remaining(), ids);
    }

    @Override
    public boolean glIsQuery(int id) {
        return GLES30.glIsQuery(id);
    }

    @Override
    public void glBeginQuery(int target, int id) {
        GLES30.glBeginQuery(target, id);
    }

    @Override
    public void glEndQuery(int target) {
        GLES30.glEndQuery(target);
    }

    @Override
    public int glGetQueryi(int target, int pname) {
        GLES30.glGetQueryiv(target, pname, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGetQueryiv(int target, int pname, IntBuffer params) {
        GLES30.glGetQueryiv(target, pname, params);
    }

    @Override
    public int glGetQueryObjectuiv(int id, int pname) {
        GLES30.glGetQueryObjectuiv(id, pname, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGetQueryObjectuiv(int id, int pname, IntBuffer params) {
        GLES30.glGetQueryObjectuiv(id, pname, params);
    }

    @Override
    public boolean glUnmapBuffer(int target) {
        return GLES30.glUnmapBuffer(target);
    }

    @Override
    public ByteBuffer glGetBufferPointerv(int target, int pname) {
        return (ByteBuffer) GLES30.glGetBufferPointerv(target, pname);
    }

    @Override
    public void glDrawBuffers(int buffer) {
        GLES30.glDrawBuffers(1, wrap(buffer), 0);
    }

    @Override
    public void glDrawBuffers(IntBuffer bufs) {
        GLES30.glDrawBuffers(bufs.remaining(), bufs);
    }

    @Override
    public void glUniformMatrix2x3fv(int location, boolean transpose, FloatBuffer value) {
        GLES30.glUniformMatrix2x3fv(location, value.remaining()/6, transpose,value);
    }

    @Override
    public void glUniformMatrix3x2fv(int location, boolean transpose, FloatBuffer value) {
        GLES30.glUniformMatrix3x2fv(location, value.remaining()/6, transpose,value);
    }

    @Override
    public void glUniformMatrix2x4fv(int location, boolean transpose, FloatBuffer value) {
        GLES30.glUniformMatrix2x4fv(location, value.remaining()/8, transpose,value);
    }

    @Override
    public void glUniformMatrix4x2fv(int location, boolean transpose, FloatBuffer value) {
        GLES30.glUniformMatrix4x2fv(location, value.remaining()/8, transpose,value);
    }

    @Override
    public void glUniformMatrix3x4fv(int location, boolean transpose, FloatBuffer value) {
        GLES30.glUniformMatrix3x4fv(location, value.remaining()/12, transpose,value);
    }

    @Override
    public void glUniformMatrix4x3fv(int location, boolean transpose, FloatBuffer value) {
        GLES30.glUniformMatrix4x3fv(location, value.remaining()/12, transpose,value);
    }

    @Override
    public void glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        GLES30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    @Override
    public void glRenderbufferStorageMultisample(int target, int samples, int internalformat, int width, int height) {
        GLES30.glRenderbufferStorageMultisample(target, samples, internalformat, width, height);
    }

    @Override
    public void glFramebufferTextureLayer(int target, int attachment, int texture, int level, int layer) {
        GLES30.glFramebufferTextureLayer(target, attachment, texture, level, layer);
    }

    @Override
    public void glFlushMappedBufferRange(int target, int offset, int length) {
        GLES30.glFlushMappedBufferRange(target, offset, length);
    }

    @Override
    public void glBindVertexArray(int array) {
        GLES30.glBindVertexArray(array);
    }

    @Override
    public void glDeleteVertexArray(int vao) {
        GLES30.glDeleteVertexArrays(1, wrap(vao), 0);
    }

    @Override
    public void glDeleteVertexArrays(IntBuffer arrays) {
        GLES30.glDeleteVertexArrays(arrays.remaining(), arrays);
    }

    @Override
    public int glGenVertexArray() {
        GLES30.glGenVertexArrays(1, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGenVertexArrays(IntBuffer arrays) {
        GLES30.glGenVertexArrays(arrays.remaining(), arrays);
    }

    @Override
    public boolean glIsVertexArray(int array) {
        return GLES30.glIsVertexArray(array);
    }

    @Override
    public void glBeginTransformFeedback(int primitiveMode) {
        GLES30.glBeginTransformFeedback(primitiveMode);
    }

    @Override
    public void glEndTransformFeedback() {
        GLES30.glEndTransformFeedback();
    }

    @Override
    public void glBindBufferRange(int target, int index, int buffer, int offset, int size) {
        GLES30.glBindBufferRange(target, index, buffer, offset, size);
    }

    @Override
    public void glBindBufferBase(int target, int index, int buffer) {
        GLES30.glBindBufferBase(target, index, buffer);
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

        GLES30.glTransformFeedbackVaryings(program, strings, bufferMode);
    }

    @Override
    public void glVertexAttribIPointer(int index, int size, int type, int stride, int offset) {
        GLES30.glVertexAttribIPointer(index, size, type, stride, offset);
    }

    @Override
    public void glGetVertexAttribIiv(int index, int pname, IntBuffer params) {
        GLES30.glGetVertexAttribIiv(index, pname, params);
    }

    @Override
    public void glGetVertexAttribIuiv(int index, int pname, IntBuffer params) {
        GLES30.glGetVertexAttribIuiv(index, pname, params);
    }

    @Override
    public void glVertexAttribI4i(int index, int x, int y, int z, int w) {
        GLES30.glVertexAttribI4i(index, x, y, z, w);
    }

    @Override
    public void glVertexAttribI4ui(int index, int x, int y, int z, int w) {
        GLES30.glVertexAttribI4ui(index, x, y, z, w);
    }

    @Override
    public void glGetUniformuiv(int program, int location, IntBuffer params) {
        GLES30.glGetUniformuiv(program, location, params);
    }

    @Override
    public int glGetFragDataLocation(int program, String name) {
        return GLES30.glGetFragDataLocation(program, name);
    }

    @Override
    public void glUniform1uiv(int location, IntBuffer value) {
        GLES30.glUniform1uiv(location, value.remaining(), value);
    }

    @Override
    public void glUniform3uiv(int location, IntBuffer value) {
        GLES30.glUniform3uiv(location, value.remaining()/3, value);
    }

    @Override
    public void glUniform4uiv(int location, IntBuffer value) {
        GLES30.glUniform4uiv(location, value.remaining()/4, value);
    }

    @Override
    public void glClearBufferiv(int buffer, int drawbuffer, IntBuffer value) {
        GLES30.glClearBufferiv(buffer, drawbuffer, value);
    }

    @Override
    public void glClearBufferuiv(int buffer, int drawbuffer, IntBuffer value) {
        GLES30.glClearBufferuiv(buffer, drawbuffer, value);
    }

    @Override
    public void glClearBufferfv(int buffer, int drawbuffer, FloatBuffer value) {
        GLES30.glClearBufferfv(buffer, drawbuffer, value);
    }

    @Override
    public void glClearBufferfi(int buffer, int drawbuffer, float depth, int stencil) {
        GLES30.glClearBufferfi(buffer, drawbuffer, depth, stencil);
    }

    @Override
    public String glGetStringi(int name, int index) {
        return GLES30.glGetStringi(name, index);
    }

    @Override
    public void glCopyBufferSubData(int readTarget, int writeTarget, int readOffset, int writeOffset, int size) {
        GLES30.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
    }

    @Override
    public void glGetUniformIndices(int program, String[] uniformNames, IntBuffer uniformIndices) {
        GLES30.glGetUniformIndices(program, uniformNames, uniformIndices);
    }

    @Override
    public void glGetActiveUniformsiv(int program, IntBuffer uniformIndices, int pname, IntBuffer params) {
        GLES30.glGetActiveUniformsiv(program, uniformIndices.remaining(), uniformIndices, pname, params);
    }

    @Override
    public int glGetUniformBlockIndex(int program, String uniformBlockName) {
        return GLES30.glGetUniformBlockIndex(program, uniformBlockName);
    }

    @Override
    public void glGetActiveUniformBlockiv(int program, int uniformBlockIndex, int pname, IntBuffer params) {
        GLES30.glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, params);
    }

    @Override
    public void glGetActiveUniformBlockName(int program, int uniformBlockIndex, IntBuffer length, ByteBuffer uniformBlockName) {
        GLES30.glGetActiveUniformBlockName(program, uniformBlockIndex, length, uniformBlockName);
    }

    @Override
    public String glGetActiveUniformBlockName(int program, int uniformBlockIndex) {
        return GLES30.glGetActiveUniformBlockName(program, uniformBlockIndex);
    }

    @Override
    public void glUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding) {
        GLES30.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
    }

    @Override
    public void glDrawArraysInstanced(int mode, int first, int count, int instanceCount) {
        GLES30.glDrawArraysInstanced(mode, first, count, instanceCount);
    }

    @Override
    public void glDrawElementsInstanced(int mode, int count, int type, int indicesOffset, int instanceCount) {
        GLES30.glDrawElementsInstanced(mode, count, type, indicesOffset, instanceCount);
    }

    @Override
    public void glGetInteger64v(int pname, LongBuffer params) {
        GLES31.glGetInteger64v(pname, params);
    }

    @Override
    public void glGetBufferParameteri64v(int target, int pname, LongBuffer params) {
        GLES31.glGetBufferParameteri64v(target, pname, params);
    }

    @Override
    public int glGenSampler() {
        GLES30.glGenSamplers(1, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGenSamplers(IntBuffer samplers) {
        GLES30.glGenSamplers(samplers.remaining(), samplers);
    }

    @Override
    public void glDeleteSampler(int sampler) {
        GLES30.glDeleteBuffers(1, wrap(sampler), 0);
    }

    @Override
    public void glDeleteSamplers(IntBuffer samplers) {
        GLES30.glDeleteBuffers(samplers.remaining(), samplers);
    }

    @Override
    public boolean glIsSampler(int sampler) {
        return GLES30.glIsSampler(sampler);
    }

    @Override
    public void glBindSampler(int unit, int sampler) {
        GLES30.glBindSampler(unit, sampler);
    }

    @Override
    public void glSamplerParameteri(int sampler, int pname, int param) {
        GLES30.glSamplerParameteri(sampler, pname, param);
    }

    @Override
    public void glSamplerParameteriv(int sampler, int pname, IntBuffer param) {
        GLES30.glSamplerParameteriv(sampler, pname, param);
    }

    @Override
    public void glSamplerParameterf(int sampler, int pname, float param) {
        GLES30.glSamplerParameterf(sampler, pname, param);
    }

    @Override
    public void glSamplerParameterfv(int sampler, int pname, FloatBuffer param) {
        GLES30.glSamplerParameterfv(sampler, pname, param);
    }

    @Override
    public void glGetSamplerParameteriv(int sampler, int pname, IntBuffer params) {
        GLES30.glGetSamplerParameteriv(sampler, pname, params);
    }

    @Override
    public void glGetSamplerParameterfv(int sampler, int pname, FloatBuffer params) {
        GLES30.glGetSamplerParameterfv(sampler, pname, params);
    }

    @Override
    public void glVertexAttribDivisor(int index, int divisor) {
        GLES30.glVertexAttribDivisor(index, divisor);
    }

    @Override
    public void glBindTransformFeedback(int target, int id) {
        GLES30.glBindTransformFeedback(target, id);
    }

    @Override
    public void glDeleteTransformFeedback(int feedback) {
        GLES30.glDeleteTransformFeedbacks(1,wrap(feedback), 0);
    }

    @Override
    public void glDeleteTransformFeedbacks(IntBuffer ids) {
        GLES30.glDeleteTransformFeedbacks(ids.remaining(), ids);
    }

    @Override
    public int glGenTransformFeedback() {
        GLES30.glGenTransformFeedbacks(1, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGenTransformFeedbacks(IntBuffer ids) {
        GLES30.glGenTransformFeedbacks(ids.remaining(), ids);
    }

    @Override
    public boolean glIsTransformFeedback(int id) {
        return GLES30.glIsTransformFeedback(id);
    }

    @Override
    public void glPauseTransformFeedback() {
        GLES30.glPauseTransformFeedback();
    }

    @Override
    public void glResumeTransformFeedback() {
        GLES30.glResumeTransformFeedback();
    }

    @Override
    public void glProgramParameteri(int program, int pname, int value) {
        GLES30.glProgramParameteri(program, pname, value);
    }

    @Override
    public void glInvalidateFramebuffer(int target, IntBuffer attachments) {
        GLES30.glInvalidateFramebuffer(target, attachments.remaining(), attachments);
    }

    @Override
    public void glInvalidateSubFramebuffer(int target, IntBuffer attachments, int x, int y, int width, int height) {
        GLES30.glInvalidateSubFramebuffer(target, attachments.remaining(), attachments, x, y,width,height);
    }

    @Override
    public void glGetProgramBinary(int program, int[] length, int[] binaryFormat, ByteBuffer binary) {
        GLES30.glGetProgramBinary(program, binary.remaining(), length, 0, binaryFormat, 0, binary);
    }

    @TargetApi(21)
    @Override
    public void glBindImageTexture(int unit, int texture, int level, boolean layered, int layer, int access, int format) {
        GLES31.glBindImageTexture(unit, texture, level, layered, layer, access, format);
    }

    @TargetApi(21)
    @Override
    public void glDispatchCompute(int num_groups_x, int num_groups_y, int num_groups_z) {
        GLES31.glDispatchCompute(num_groups_x, num_groups_y, num_groups_z);
    }

    @TargetApi(21)
    @Override
    public void glMemoryBarrier(int barriers) {
        GLES31.glMemoryBarrier(barriers);
    }

    @Override
    public void glPolygonMode(int face, int mode) {
        GLCheck.printUnsupportFuncError("Unsupport glPolygonMode(int face, int mode) on Android Platform!");
    }

    @Override
    public void glPatchParameterfv(int pname, float[] value) {
        GLCheck.printUnsupportFuncError("Unsupport glPatchParameterfv(int face, int mode) on Android Platform!");
    }

    @TargetApi(24)
    @Override
    public void glPatchParameteri(int pname, int value) {
        GLES32.glPatchParameteri(pname,value);
    }

    @Override
    public void glBufferData(int target, Buffer data, int mode) {
        GLES20.glBufferData(target, BufferUtils.measureSize(data), data, mode);
    }

    @Override
    public void glBufferData(int target, int size, int mode) {
        GLES20.glBufferData(target, size, null, mode);
    }

    @Override
    public void glTextureParameteri(int textureID, int pname, int mode) {
        GLCheck.printUnsupportFuncError("Unsupport glTextureParameteri(int textureID, int pname, int mode) on Android Platform!");
    }

    @Override
    public void glTextureParameteriv(int textureID, int pname, int[] rgba) {
        GLCheck.printUnsupportFuncError("Unsupport glTextureParameteriv(int textureID, int pname, int[] rgba) on Android Platform!");
    }

    @TargetApi(21)
    @Override
    public int glGetTexLevelParameteri(int target, int level, int pname) {
        GLES31.glGetTexLevelParameteriv(target, level, pname, intValues, 0);
        return intValues[0];
    }

    @Override
    public void glGetTexImage(int target, int level, int format, int type, ByteBuffer result) {
        GLCheck.printUnsupportFuncError("Unsupport glGetTexImage(int target, int level, int format, int type, ByteBuffer result) on Android Platform!");
    }

    @Override
    public void glGetIntegerv(int pname, IntBuffer values) {
        GLES20.glGetIntegerv(pname, values);
    }

    @Override
    public void glTextureView(int dstTexture, int target, int srcTexture, int srcFormat, int minlevel, int numlevels, int minlayer, int numlayers) {
        GLCheck.printUnsupportFuncError("Unsupport glTextureView(int dstTexture, int target, int srcTexture, int srcFormat, int minlevel, int numlevels, int minlayer, int numlayers) on Android Platform!");
    }

    @Override
    public int glCreateTextures(int target) {
        GLCheck.printUnsupportFuncError("Unsupport \'int glCreateTextures(int target)\' on Android Platform!");
        return 0;
    }

    @Override
    public void glTextureStorage3D(int textureID, int mipLevels, int format, int width, int height, int depth) {
        GLCheck.printUnsupportFuncError("Unsupport \'glTextureStorage3D(int textureID, int mipLevels, int format, int width, int height, int depth)\' on Android Platform!");
    }

    @Override
    public void glTexStorage3D(int target, int levels, int format, int width, int height, int depth) {
        GLES30.glTexStorage3D(target, levels, format, width, height, depth);
    }

    @Override
    public void glTextureStorage2DMultisample(int textureID, int sampleCount, int format, int width, int height, boolean fixedsamplelocations) {
        GLCheck.printUnsupportFuncError("Unsupport \'glTextureStorage2DMultisample(int textureID, int sampleCount, int format, int width, int height, boolean fixedsamplelocations)\' on Android Platform!");
    }

    @Override
    public void glTextureStorage3DMultisample(int textureID, int sampleCount, int format, int width, int height, int arraySize, boolean fixedsamplelocations) {
        GLCheck.printUnsupportFuncError("Unsupport \'glTextureStorage3DMultisample(int textureID, int sampleCount, int format, int width, int height, int arraySize, boolean fixedsamplelocations)\' on Android Platform!");
    }

    @TargetApi(21)
    @Override
    public void glTexStorage2DMultisample(int target, int sampleCount, int format, int width, int height, boolean fixedsamplelocations) {
        GLES31.glTexStorage2DMultisample(target, sampleCount, format, width, height, fixedsamplelocations);
    }

    @TargetApi(21)
    @Override
    public void glTexImage2DMultisample(int target, int sampleCount, int format, int width, int height, boolean fixedsamplelocations) {
        GLCheck.printUnsupportFuncError("Unsupport \'glTexImage2DMultisample(int target, int sampleCount, int format, int width, int height, boolean fixedsamplelocations)\' on Android Platform!");
    }

    @Override
    public void glTextureStorage2D(int textureID, int mipLevels, int format, int width, int height) {
        GLCheck.printUnsupportFuncError("Unsupport \'glTextureStorage2D(int textureID, int mipLevels, int format, int width, int height)\' on Android Platform!");
    }

    @TargetApi(24)
    @Override
    public void glTexStorage3DMultisample(int target, int sampleCount, int format, int width, int height, int arraySize, boolean fixedsamplelocations) {
        GLES32.glTexStorage3DMultisample(target, sampleCount, format, width, height, arraySize, fixedsamplelocations);
    }

    @Override
    public void glTexImage3DMultisample(int target, int sampleCount, int format, int width, int height, int arraySize, boolean fixedsamplelocations) {
        GLCheck.printUnsupportFuncError("Unsupport \'glTexImage3DMultisample(int target, int sampleCount, int format, int width, int height, int arraySize, boolean fixedsamplelocations)\' on Android Platform!");
    }

    @Override
    public void glTexStorage2D(int target, int mipLevels, int format, int width, int height) {
        GLES30.glTexStorage2D(target, mipLevels, format, width, height);
    }

    @Override
    public void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, ByteBuffer data) {
        GLES20.glCompressedTexImage2D(target, level, internalformat, width, height, border, BufferUtils.measureSize(data), data);
    }

    @Override
    public void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, int image_size, long offset) {
        if(offset == 0L){
            GLES20.glCompressedTexImage2D(target, level, internalformat, width, height, border, image_size, null);
        }else{
            GLCheck.printUnsupportFuncError("Unsupport 'glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, int image_size, long offset)' on Android Platform!");
        }
    }

    @Override
    public void glCompressedTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int boder, ByteBuffer byteBuffer) {
        GLES30.glCompressedTexImage3D(target, level, internalformat, width, height, depth, boder, BufferUtils.measureSize(byteBuffer), byteBuffer);
    }

    @Override
    public void glCompressedTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int boder, int image_size, long offset) {
        GLES30.glCompressedTexImage3D(target, level, internalformat, width, height, depth, boder, image_size, (int)offset);
    }

    @Override
    public void glTextureSubImage2D(int texture, int level, int x_offset, int y_offset, int width, int height, int format, int type, Buffer pixels) {
        GLCheck.printUnsupportFuncError("Unsupport 'glTextureSubImage2D(int texture, int level, int x_offset, int y_offset, int width, int height, int format, int type, Buffer pixels)' on Android Platform!");
    }

    @Override
    public void glTextureSubImage2D(int texture, int level, int x_offset, int y_offset, int width, int height, int format, int type, long data_offset) {
        GLCheck.printUnsupportFuncError("Unsupport 'glTextureSubImage2D(int texture, int level, int x_offset, int y_offset, int width, int height, int format, int type, long data_offset)' on Android Platform!");
    }

    @Override
    public void glTextureSubImage3D(int textureID, int level, int x_offset, int y_offset, int z_offset, int width, int height, int depth, int format, int type, Buffer pixels) {
        GLCheck.printUnsupportFuncError("Unsupport 'glTextureSubImage3D(int textureID, int level, int x_offset, int y_offset, int z_offset, int width, int height, int depth, int format, int type, Buffer pixels)' on Android Platform!");
    }

    @Override
    public void glTextureSubImage3D(int textureID, int level, int x_offset, int y_offset, int z_offset, int width, int height, int depth, int format, int type, long offset) {
        GLCheck.printUnsupportFuncError("Unsupport 'glTextureSubImage3D(int textureID, int level, int x_offset, int y_offset, int z_offset, int width, int height, int depth, int format, int type, long offset)' on Android Platform!");
    }

    @Override
    public int glGetVertexAttribi(int index, int pname) {
        GLES30.glGetVertexAttribiv(index, pname, intValues, 0);
        return intValues[0];
    }

    @TargetApi(24)
    @Override
    public void glFramebufferTexture(int target, int attachment, int texture, int level) {
        GLES32.glFramebufferTexture(target, attachment, texture, level);
    }

    @Override
    public void glFramebufferTexture1D(int target, int attachment, int texturetarget, int texture, int level) {
        GLCheck.printUnsupportFuncError("Unsupport 'glFramebufferTexture1D(int target, int attachment, int texturetarget, int texture, int level)' on Android Platform!");
    }

    @Override
    public void glFramebufferTexture3D(int target, int attachment, int texturetarget, int texture, int level, int layer) {
        GLCheck.printUnsupportFuncError("Unsupport 'glFramebufferTexture3D(int target, int attachment, int texturetarget, int texture, int level, int layer)' on Android Platform!");
    }
}
