package jet.opengl.demos.gpupro.culling;

import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.ModelGenerator;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.BoundingBox;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StackInt;

final class DawnCulling implements  OcclusionTester, Disposeable {
    private int mFrameNumber;
    private Texture2D mHZBuffer;
    private Texture2D mReprojection;

    private GLSLProgram mReprojectProg;
    private int mPointSampler;
    private GLFuncProvider gl;

    private GLSLProgram mFillVisibilittProg;
    private BufferGL mMeshWorldMats;
    private RenderTargets mFramebuffer;

    private GLSLProgram mCopyDepthProg;

    private final StackInt m_InvisiableMeshIdx = new StackInt();

    private BufferGL mVisibilityBuffer;
    static final int REPROJECTION_THREAD_GROUP_SIZE = 32;

    private GLVAO mCubeVao;

    @Override
    public void newFrame(int frameNumber) {
        mFrameNumber = frameNumber;

        if(gl == null)
            gl = GLFuncProviderFactory.getGLFuncProvider();

        if(mFramebuffer == null){
            mFramebuffer = new RenderTargets();
            mCubeVao = ModelGenerator.genCube(2, false, false, false).genVAO();
        }
    }

    @Override
    public void cullingCoarse(Renderer renderer, Scene scene) {
        if(renderer.mDepthBuffer == null || scene.mExpandMeshes.size() == 0 || mFrameNumber == 0) {
            return;
        }

        reproject(renderer.mDepthBuffer, scene);
        copyDepth();
        clearVisibilityBuffer(scene.mExpandMeshes.size());
        fillVisibilityBuffer(scene, true, null);
    }

    @Override
    public void cullingFine(Renderer renderer, Scene scene) {
        if(renderer.mDepthBuffer == null || scene.mExpandMeshes.size() == 0 || mFrameNumber == 0 || m_InvisiableMeshIdx.isEmpty()) {
            return;
        }

        clearVisibilityBuffer(m_InvisiableMeshIdx.size());
        fillVisibilityBuffer(scene, false, renderer.mDepthBuffer);
    }

    private void reproject(Texture2D depth, Scene scene){
        final int width = depth.getWidth()/4;
        final int height = depth.getHeight()/4;

        if(mHZBuffer == null || mHZBuffer.getWidth() != width || mHZBuffer.getHeight() != height){
            SAFE_RELEASE(mHZBuffer);
            SAFE_RELEASE(mReprojection);

            Texture2DDesc desc = new Texture2DDesc(width, height, GLenum.GL_DEPTH24_STENCIL8);
            mHZBuffer = TextureUtils.createTexture2D(desc, null);

            desc.format = GLenum.GL_R32UI;
            mReprojection = TextureUtils.createTexture2D(desc, null);
        }

        if(mReprojectProg == null){
            final String root = "gpupro/Culling/shaders/";
            mReprojectProg = GLSLProgram.createProgram(root + "Reprojection.comp", null);
            mReprojectProg.setName("Reprojection");
        }

        if(mPointSampler == 0){
            SamplerDesc desc = new SamplerDesc();
            desc.minFilter = desc.magFilter = GLenum.GL_NEAREST;
            mPointSampler = SamplerUtils.createSampler(desc);
//            desc.minFilter = GLenum.GL_NEAREST_MIPMAP_NEAREST;
//            m_PointMipmapSampler = SamplerUtils.createSampler(desc);
        }
        mReprojectProg.enable();
        gl.glClearTexImage(mReprojection.getTexture(), 0, GLenum.GL_RED_INTEGER, GLenum.GL_UNSIGNED_INT, null);
        gl.glBindTextureUnit(0, depth.getTexture());
        gl.glBindSampler(0, mPointSampler);
        gl.glBindImageTexture(0, mReprojection.getTexture(), 0, false, 0, GLenum.GL_READ_WRITE, mReprojection.getFormat());
        {
            final Matrix4f tmp = CacheBuffer.getCachedMatrix();
            Matrix4f viewProj = Matrix4f.mul(scene.mProj, scene.mView, tmp);
            GLSLUtil.setMat4(mReprojectProg, "gViewProj",viewProj);

            Matrix4f.mul(scene.mProj, scene.mPrevView, tmp);
            Matrix4f.invert(tmp, tmp);
            GLSLUtil.setMat4(mReprojectProg, "gPrevViewProjInv",tmp);

            CacheBuffer.free(tmp);
        }

        final int numGroupX = Numeric.divideAndRoundUp(width, REPROJECTION_THREAD_GROUP_SIZE);
        final int numGroupY = Numeric.divideAndRoundUp(height, REPROJECTION_THREAD_GROUP_SIZE);
        gl.glDispatchCompute(numGroupX, numGroupY, 1);
        gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

        GLCheck.checkError();
        gl.glBindTextureUnit(0, 0);
        gl.glBindSampler(0, 0);
        gl.glBindImageTexture(0, 0, 0, false, 0, GLenum.GL_READ_WRITE, mReprojection.getFormat());
        GLCheck.checkError();
        mReprojectProg.printOnce();
    }

    private void copyDepth(){
        if(mCopyDepthProg == null){
            final String root = "gpupro/Culling/shaders/";
            mCopyDepthProg = GLSLProgram.createProgram("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", root + "CopyDepth.frag", null);
            mCopyDepthProg.setName("CopyDepth");
        }

        mCopyDepthProg.enable();
        gl.glBindTextureUnit(0, mReprojection.getTexture());

        mFramebuffer.bind();
        mFramebuffer.setRenderTexture(mHZBuffer, null);
        gl.glViewport(0,0, mHZBuffer.getWidth(), mHZBuffer.getHeight());

        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDepthFunc(GLenum.GL_ALWAYS);

        gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

        gl.glDepthFunc(GLenum.GL_LESS);

        mCopyDepthProg.printOnce();
    }

    private void clearVisibilityBuffer(int numMeshes){
        if(mVisibilityBuffer == null || mVisibilityBuffer.getBufferSize() < numMeshes * 4){
            SAFE_RELEASE(mVisibilityBuffer);
            mVisibilityBuffer = new BufferGL();
            mVisibilityBuffer.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, numMeshes * 4, null, GLenum.GL_DYNAMIC_COPY);
        }

        gl.glClearNamedBufferSubData(mVisibilityBuffer.getBuffer(), GLenum.GL_R32UI, 0, numMeshes * 4, GLenum.GL_RED_INTEGER, GLenum.GL_UNSIGNED_INT, null);
    }

    private void fillVisibilityBuffer(Scene scene, boolean bCoarse, Texture2D sceneDepth){
        final int numMeshes = bCoarse ? scene.mExpandMeshes.size() : m_InvisiableMeshIdx.size();

        if(mMeshWorldMats == null || mMeshWorldMats.getBufferSize() < numMeshes * Vector4f.SIZE * 2){
            SAFE_RELEASE(mMeshWorldMats);
            mMeshWorldMats = new BufferGL();
            mMeshWorldMats.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, numMeshes * Vector4f.SIZE * 2, null, GLenum.GL_DYNAMIC_DRAW);
        }

        ByteBuffer boundingBoxes = CacheBuffer.getCachedByteBuffer(numMeshes * Vector4f.SIZE * 2);
        for(int meshIdx = 0; meshIdx < numMeshes; meshIdx++){
//            scene.mExpandMeshes.get(bCoarse ? meshIdx : m_InvisiableMeshIdx.get(meshIdx)).mWorld.store(boundingBoxes);
            BoundingBox aabb = scene.mExpandMeshes.get(bCoarse ? meshIdx : m_InvisiableMeshIdx.get(meshIdx)).mAABB;
            aabb._min.store(boundingBoxes); boundingBoxes.putInt(0);
            aabb._max.store(boundingBoxes); boundingBoxes.putInt(0);
        }
        boundingBoxes.flip();
        mMeshWorldMats.update(0, boundingBoxes);

        if(mFillVisibilittProg == null){
            final String root = "gpupro/Culling/shaders/";
            mFillVisibilittProg = GLSLProgram.createProgram(root + "CullingVS.vert",root + "CullingPS.frag", null);
            mFillVisibilittProg.setName("FillVisibility");
        }

        mFramebuffer.bind();
        mFramebuffer.setRenderTexture(bCoarse ? mHZBuffer: sceneDepth, null);
        if(bCoarse)
            gl.glViewport(0,0, mHZBuffer.getWidth(), mHZBuffer.getHeight());
        else
            gl.glViewport(0,0, sceneDepth.getWidth(), sceneDepth.getHeight());
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDepthFunc(GLenum.GL_LEQUAL);
        gl.glDepthMask(false);
        gl.glColorMask(false, false, false, false);
        gl.glDisable(GLenum.GL_CULL_FACE);

        mFillVisibilittProg.enable();
        {
            final Matrix4f tmp = CacheBuffer.getCachedMatrix();
            Matrix4f viewProj = Matrix4f.mul(scene.mProj, scene.mView, tmp);
            GLSLUtil.setMat4(mFillVisibilittProg, "gViewProj",viewProj);
            CacheBuffer.free(tmp);
        }

        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 0, mMeshWorldMats.getBuffer());
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, mVisibilityBuffer.getBuffer());

//        gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_LINE);  todo this is not a good way

        /*gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);

        gl.glDrawArraysInstanced(GLenum.GL_TRIANGLES, 0, 36,numMeshes);*/
        mCubeVao.bind();
        mCubeVao.draw(GLenum.GL_TRIANGLES, numMeshes);
        mCubeVao.unbind();

        gl.glDepthMask(true);
        gl.glColorMask(true, true, true, true);

        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 0, 0);
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, 0);
//        gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
        mFillVisibilittProg.printOnce();

        if(bCoarse)
            m_InvisiableMeshIdx.clear();

        int numCulling = 0;
        ByteBuffer results = mVisibilityBuffer.map(0, 4 * numMeshes, GLenum.GL_MAP_READ_BIT);
        for(int meshIdx = 0; meshIdx < numMeshes; meshIdx++){
            boolean visible = (results.getInt(meshIdx * 4) == 1);
            scene.mExpandMeshVisible.set(bCoarse ? meshIdx : m_InvisiableMeshIdx.get(meshIdx),visible);
            if(!visible && bCoarse){
                m_InvisiableMeshIdx.push(meshIdx);
            }

            if(!visible){
                numCulling++;
            }
        }

        if(mFrameNumber % 60 == 0){
            if(bCoarse)
                System.out.println("Culling Count: " + numCulling);
            else
                System.out.println("Fine Culling Count: " + numCulling);
        }
        mVisibilityBuffer.unmap();
    }

    @Override
    public void dispose() {
        SAFE_RELEASE(mHZBuffer);
        SAFE_RELEASE(mReprojection);
        SAFE_RELEASE(mReprojectProg);
        SAFE_RELEASE(mFillVisibilittProg);
        SAFE_RELEASE(mMeshWorldMats);
        SAFE_RELEASE(mFramebuffer);
        SAFE_RELEASE(mCopyDepthProg);
        SAFE_RELEASE(mVisibilityBuffer);
    }
}
