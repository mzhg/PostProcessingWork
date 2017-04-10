package jet.opengl.impl.lwjgl3;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vector.Matrix4f;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

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

    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, long pixels_offset) {

    }

    @Override
    public void glTexParameterf(int target, int pname, float param) {

    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, Buffer pixels) {

    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, long pixels_offset) {

    }

    @Override
    public void glViewport(int x, int y, int width, int height) {

    }

    @Override
    public void glAttachShader(int program, int shader) {

    }

    @Override
    public void glBindAttribLocation(int program, int index, CharSequence name) {

    }

    @Override
    public void glBindBuffer(int target, int buffer) {

    }

    @Override
    public void glBindFramebuffer(int target, int framebuffer) {

    }

    @Override
    public void glBindRenderbuffer(int target, int renderbuffer) {

    }

    @Override
    public void glBlendColor(float red, float green, float blue, float alpha) {

    }

    @Override
    public void glBlendEquation(int mode) {

    }

    @Override
    public void glBlendEquationSeparate(int modeRGB, int modeAlpha) {

    }

    @Override
    public void glBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {

    }

    @Override
    public void glBufferData(int target, int size, Buffer data, int usage) {

    }

    @Override
    public void glBufferSubData(int target, int offset, int size, Buffer data) {

    }

    @Override
    public int glCheckFramebufferStatus(int target) {
        return 0;
    }

    @Override
    public void glCompileShader(int shader) {

    }

    @Override
    public int glCreateProgram() {
        return 0;
    }

    @Override
    public int glCreateShader(int type) {
        return 0;
    }

    @Override
    public void glDeleteBuffer(int buffer) {

    }

    @Override
    public void glDeleteFramebuffer(int framebuffer) {

    }

    @Override
    public void glDeleteProgram(int program) {

    }

    @Override
    public void glDeleteRenderbuffer(int renderbuffer) {

    }

    @Override
    public void glDeleteShader(int shader) {

    }

    @Override
    public void glDetachShader(int program, int shader) {

    }

    @Override
    public void glDisableVertexAttribArray(int index) {

    }

    @Override
    public void glDrawElements(int mode, int count, int type, long indices) {

    }

    @Override
    public void glEnableVertexAttribArray(int index) {

    }

    @Override
    public void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {

    }

    @Override
    public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {

    }

    @Override
    public int glGenBuffer() {
        return 0;
    }

    @Override
    public void glGenBuffers(IntBuffer buffers) {

    }

    @Override
    public void glGenerateMipmap(int target) {

    }

    @Override
    public int glGenFramebuffer() {
        return 0;
    }

    @Override
    public int glGenRenderbuffer() {
        return 0;
    }

    @Override
    public String glGetActiveAttrib(int program, int index, int maxLength, IntBuffer size, IntBuffer type) {
        return null;
    }

    @Override
    public String glGetActiveUniform(int program, int index, int maxLength, IntBuffer size, IntBuffer type) {
        return null;
    }

    @Override
    public void glGetAttachedShaders(int program, int maxcount, ByteBuffer count, ByteBuffer shaders) {

    }

    @Override
    public int glGetAttribLocation(int program, CharSequence name) {
        return 0;
    }

    @Override
    public boolean glGetBoolean(int pname) {
        return false;
    }

    @Override
    public void glGetBooleanv(int pname, ByteBuffer params) {

    }

    @Override
    public int glGetBufferParameteri(int target, int pname) {
        return 0;
    }

    @Override
    public void glGetBufferParameteriv(int target, int pname, IntBuffer params) {

    }

    @Override
    public float glGetFloat(int pname) {
        return 0;
    }

    @Override
    public void glGetFloatv(int pname, FloatBuffer params) {

    }

    @Override
    public int glGetProgrami(int program, int pname) {
        return 0;
    }

    @Override
    public void glGetProgramiv(int program, int pname, IntBuffer params) {

    }

    @Override
    public String glGetProgramInfoLog(int program) {
        return null;
    }

    @Override
    public String glGetShaderInfoLog(int shader) {
        return null;
    }

    @Override
    public float glGetTexParameterf(int target, int pname) {
        return 0;
    }

    @Override
    public void glGetTexParameterfv(int target, int pname, FloatBuffer params) {

    }

    @Override
    public int glGetTexParameteri(int target, int pname) {
        return 0;
    }

    @Override
    public void glGetTexParameteriv(int target, int pname, IntBuffer params) {

    }

    @Override
    public float glGetUniformf(int program, int location) {
        return 0;
    }

    @Override
    public void glGetUniformfv(int program, int location, FloatBuffer params) {

    }

    @Override
    public int glGetUniformi(int program, int location) {
        return 0;
    }

    @Override
    public void glGetUniformiv(int program, int location, IntBuffer params) {

    }

    @Override
    public int glGetUniformLocation(int program, CharSequence name) {
        return 0;
    }

    @Override
    public boolean glIsBuffer(int buffer) {
        return false;
    }

    @Override
    public boolean glIsEnabled(int cap) {
        return false;
    }

    @Override
    public boolean glIsFramebuffer(int framebuffer) {
        return false;
    }

    @Override
    public boolean glIsProgram(int program) {
        return false;
    }

    @Override
    public boolean glIsRenderbuffer(int renderbuffer) {
        return false;
    }

    @Override
    public boolean glIsShader(int shader) {
        return false;
    }

    @Override
    public boolean glIsTexture(int texture) {
        return false;
    }

    @Override
    public void glLinkProgram(int program) {

    }

    @Override
    public void glReleaseShaderCompiler() {

    }

    @Override
    public void glRenderbufferStorage(int target, int internalformat, int width, int height) {

    }

    @Override
    public void glSampleCoverage(float value, boolean invert) {

    }

    @Override
    public void glShaderBinary(int count, ByteBuffer shaders, int binaryformat, ByteBuffer binary, int length) {

    }

    @Override
    public void glShaderSource(int shader, CharSequence string) {

    }

    @Override
    public void glStencilFuncSeparate(int face, int func, int ref, int mask) {

    }

    @Override
    public void glStencilMaskSeparate(int face, int mask) {

    }

    @Override
    public void glStencilOpSeparate(int face, int fail, int zfail, int zpass) {

    }

    @Override
    public void glTexParameterfv(int target, int pname, FloatBuffer params) {

    }

    @Override
    public void glTexParameteri(int target, int pname, int param) {

    }

    @Override
    public void glTexParameteriv(int target, int pname, int[] params) {

    }

    @Override
    public void glUniform1f(int location, float x) {

    }

    @Override
    public void glUniform1fv(int location, FloatBuffer v) {

    }

    @Override
    public void glUniform1i(int location, int x) {

    }

    @Override
    public void glUniform1iv(int location, IntBuffer v) {

    }

    @Override
    public void glUniform2f(int location, float x, float y) {

    }

    @Override
    public void glUniform2fv(int location, FloatBuffer v) {

    }

    @Override
    public void glUniform2i(int location, int x, int y) {

    }

    @Override
    public void glUniform2iv(int location, IntBuffer v) {

    }

    @Override
    public void glUniform3f(int location, float x, float y, float z) {

    }

    @Override
    public void glUniform3fv(int location, FloatBuffer v) {

    }

    @Override
    public void glUniform3i(int location, int x, int y, int z) {

    }

    @Override
    public void glUniform3iv(int location, IntBuffer v) {

    }

    @Override
    public void glUniform4f(int location, float x, float y, float z, float w) {

    }

    @Override
    public void glUniform4fv(int location, FloatBuffer v) {

    }

    @Override
    public void glUniform4i(int location, int x, int y, int z, int w) {

    }

    @Override
    public void glUniform4iv(int location, IntBuffer v) {

    }

    @Override
    public void glUniformMatrix2fv(int location, boolean transpose, FloatBuffer value) {

    }

    @Override
    public void glUniformMatrix3fv(int location, boolean transpose, FloatBuffer value) {

    }

    @Override
    public void glUniformMatrix4fv(int location, boolean transpose, FloatBuffer value) {

    }

    @Override
    public void glUniformMatrix4fv(int location, boolean transpose, Matrix4f matrix) {

    }

    @Override
    public void glUseProgram(int program) {

    }

    @Override
    public void glValidateProgram(int program) {

    }

    @Override
    public void glVertexAttrib1f(int indx, float x) {

    }

    @Override
    public void glVertexAttrib1fv(int indx, FloatBuffer values) {

    }

    @Override
    public void glVertexAttrib2f(int indx, float x, float y) {

    }

    @Override
    public void glVertexAttrib2fv(int indx, FloatBuffer values) {

    }

    @Override
    public void glVertexAttrib3f(int indx, float x, float y, float z) {

    }

    @Override
    public void glVertexAttrib3fv(int indx, FloatBuffer values) {

    }

    @Override
    public void glVertexAttrib4f(int indx, float x, float y, float z, float w) {

    }

    @Override
    public void glVertexAttrib4fv(int indx, FloatBuffer values) {

    }

    @Override
    public void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, Buffer ptr) {

    }

    @Override
    public void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, int ptr) {

    }

    @Override
    public void glReadBuffer(int mode) {

    }

    @Override
    public void glDrawRangeElements(int mode, int start, int end, int count, int type, Buffer indices) {

    }

    @Override
    public void glDrawRangeElements(int mode, int start, int end, int count, int type, int offset) {

    }

    @Override
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, Buffer pixels) {

    }

    @Override
    public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, long offset) {

    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, Buffer pixels) {

    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, long pixels_offset) {

    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, int offset) {

    }

    @Override
    public void glCopyTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int x, int y, int width, int height) {

    }

    @Override
    public int glGenQuery() {
        return 0;
    }

    @Override
    public void glGenQueries(IntBuffer ids) {

    }

    @Override
    public void glDeleteQuery(int query) {

    }

    @Override
    public void glDeleteQueries(IntBuffer ids) {

    }

    @Override
    public boolean glIsQuery(int id) {
        return false;
    }

    @Override
    public void glBeginQuery(int target, int id) {

    }

    @Override
    public void glEndQuery(int target) {

    }

    @Override
    public int glGetQueryi(int target, int pname) {
        return 0;
    }

    @Override
    public void glGetQueryiv(int target, int pname, IntBuffer params) {

    }

    @Override
    public int glGetQueryObjectuiv(int id, int pname) {
        return 0;
    }

    @Override
    public void glGetQueryObjectuiv(int id, int pname, IntBuffer params) {

    }

    @Override
    public boolean glUnmapBuffer(int target) {
        return false;
    }

    @Override
    public Buffer glGetBufferPointerv(int target, int pname) {
        return null;
    }

    @Override
    public void glDrawBuffers(int buffer) {

    }

    @Override
    public void glDrawBuffers(IntBuffer bufs) {

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
