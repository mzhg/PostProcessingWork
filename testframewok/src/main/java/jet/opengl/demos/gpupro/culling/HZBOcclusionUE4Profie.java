package jet.opengl.demos.gpupro.culling;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.BoundingBox;
import jet.opengl.postprocessing.util.BufferUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StackInt;

final class HZBOcclusionUE4Profie implements OcclusionTester {
    private Texture2D mHZBuffer;
    private final GLSLProgram[] mBuildHZB = new GLSLProgram[2];
    private GLSLProgram mHZBTester;

    private final List<Texture2D> mHZBMipLevels = new ArrayList<>();
    private boolean m_bCoarseTesting;
    private GLFuncProvider gl;
    private int m_PointSampler;
    private int m_PointMipmapSampler;

    private Texture2D mCenterTex;
    private Texture2D mExtentTex;
    private Texture2D mVisibleTex;
    private BufferGL  mVisibleBuffer;

    private RenderTargets mFBO;
    private int mDummyVAO;

    private int mFrameNumber;
    private final FloatBuffer mExtentBuffers = BufferUtils.createFloatBuffer(256 * 256 * 4);

    private final StackInt m_InvisiableMeshIdx = new StackInt();

    public Texture2D getHZBSlice(int slice){ return mHZBMipLevels.get(slice);}

    public void newFrame(int frameNumber){
        m_bCoarseTesting = false;
        mFrameNumber = frameNumber;
    }

    @Override
    public boolean isValid() {
        return m_bCoarseTesting;
    }

    @Override
    public void cullingCoarse(Renderer renderer, Scene scene) {
        try{
            if(renderer.mDepthBuffer == null || scene.mExpandMeshes.size() == 0 || mFrameNumber == 0) {
                return;
            }

            updateResources(renderer.mDepthBuffer);
            generateHZB(renderer.mDepthBuffer, scene);
            packingMeshInfomations(scene, false);
            HizTesting(scene, true);
        }finally {
            m_bCoarseTesting = true;
            gl.glBindSampler(0,0);
        }
    }

    @Override
    public void cullingFine(Renderer renderer, Scene scene) {
        /* Nothing need to do here. */
    }

    private void updateResources(Texture2D depth){
        if(gl == null)
            gl = GLFuncProviderFactory.getGLFuncProvider();

        // View.ViewRect.{Width,Height}() are most likely to be < 2^24, so the float
        // conversion won't loss any precision (assuming float have 23bits for mantissa)
        final int NumMipsX = (int) Math.max(Math.ceil(Numeric.log2(depth.getWidth())) - 1, 1);
        final int NumMipsY = (int) Math.max(Math.ceil(Numeric.log2(depth.getHeight())) - 1, 1);
        final int NumMips = Math.max(NumMipsX, NumMipsY);

        final int HZBSizeX = 1 << NumMipsX;
        final int HZBSizeY = 1 << NumMipsY;

        if(mHZBuffer == null || mHZBuffer.getWidth() != HZBSizeX || mHZBuffer.getHeight() != HZBSizeY){
            CommonUtil.safeRelease(mHZBuffer);
            mHZBMipLevels.clear();

            Texture2DDesc desc = new Texture2DDesc(HZBSizeX, HZBSizeY, GLenum.GL_R32F);
            desc.mipLevels =NumMips;
            mHZBuffer = TextureUtils.createTexture2D(desc, null);

            for(int i = 0; i < NumMips; i++){
                mHZBMipLevels.add(TextureUtils.createTextureView(mHZBuffer, GLenum.GL_TEXTURE_2D, i, 1, 0,1));
            }

            GLCheck.checkError();
        }

        final String DEFULAT_VERT = "shader_libs/PostProcessingDefaultScreenSpaceVS.vert";
        final String root = "gpupro/Culling/shaders/";
        if(mBuildHZB[0] == null){
            mBuildHZB[0] = GLSLProgram.createProgram(DEFULAT_VERT,root + "HZBBuildPS.frag", CommonUtil.toArray(new Macro("STAGE", 0)));
            mBuildHZB[0].setName("HZBBuildMipLevel0");
            mBuildHZB[1] = GLSLProgram.createProgram(DEFULAT_VERT,root + "HZBBuildPS.frag", CommonUtil.toArray(new Macro("STAGE", 1)));
            mBuildHZB[1].setName("HZBBuildMipLevels");
        }

        if(mHZBTester == null){
            mHZBTester = GLSLProgram.createProgram(DEFULAT_VERT,root + "HZBTestPS.frag", null);
            mHZBTester.setName("HZBTestPS");
        }

        if(m_PointSampler == 0){
            SamplerDesc desc = new SamplerDesc();
            desc.minFilter = desc.magFilter = GLenum.GL_NEAREST;
            m_PointSampler = SamplerUtils.createSampler(desc);
            desc.minFilter = GLenum.GL_NEAREST_MIPMAP_NEAREST;
            m_PointMipmapSampler = SamplerUtils.createSampler(desc);
        }

        if(mCenterTex == null){
            Texture2DDesc desc = new Texture2DDesc(256, 256, GLenum.GL_RGBA32F);
            mCenterTex = TextureUtils.createTexture2D(desc, null);
            mExtentTex = TextureUtils.createTexture2D(desc, null);

            desc.format = GLenum.GL_R8;
            mVisibleTex = TextureUtils.createTexture2D(desc, null);
            mVisibleBuffer = new BufferGL();
            mVisibleBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, 256*256, null, GLenum.GL_DYNAMIC_READ);

            mFBO = new RenderTargets();
            mDummyVAO = gl.glGenVertexArray();
        }

        GLCheck.checkError();
    }

    private void generateHZB(Texture2D depth, Scene scene){

        // Create HZB of first mipLevel
        mBuildHZB[0].enable();
        gl.glClearTexImage(mHZBuffer.getTexture(), 0, GLenum.GL_RED, GLenum.GL_FLOAT, CacheBuffer.wrap(1.f));

        Texture2D HZBLevel0 = mHZBMipLevels.get(0);
        mFBO.bind();
        mFBO.setRenderTexture(HZBLevel0, null);
        gl.glViewport(0,0, HZBLevel0.getWidth(), HZBLevel0.getHeight());
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_BLEND);

        gl.glBindVertexArray(mDummyVAO);

        gl.glBindTextureUnit(0,depth.getTexture());
        gl.glBindSampler(0, m_PointSampler);

        // Parameters
        {
            GLSLUtil.setFloat2(mBuildHZB[0], "InvSize", 1.f/depth.getWidth(), 1.f/depth.getHeight());
            GLSLUtil.setFloat4(mBuildHZB[0],"InputUvFactorAndOffset", 2.0f * mHZBuffer.getWidth()/depth.getWidth(),
                    2.0f * mHZBuffer.getHeight()/depth.getHeight(),0,0);
            GLSLUtil.setFloat2(mBuildHZB[0], "InputViewportMaxBound", 1 - 0.5f/depth.getWidth(), 1-0.5f/depth.getHeight());
        }

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        GLCheck.checkError();
        mBuildHZB[0].printOnce();

        // downsampling
        mBuildHZB[1].enable();
        for(int i = 1; i < mHZBMipLevels.size(); i++){
            Texture2D src = mHZBMipLevels.get(i-1);
            Texture2D dst = mHZBMipLevels.get(i);

            gl.glBindTextureUnit(0,src.getTexture());

            mFBO.setRenderTexture(dst, null);
            gl.glViewport(0,0, dst.getWidth(), dst.getHeight());

            GLSLUtil.setFloat2(mBuildHZB[1], "InvSize", 1.f/src.getWidth(), 1.f/src.getHeight());

            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
            GLCheck.checkError();
        }
        mBuildHZB[1].printOnce();

        gl.glBindVertexArray(0);
    }

    private void packingMeshInfomations(Scene scene, boolean fromInvisible){
        final int numMeshes = fromInvisible ? m_InvisiableMeshIdx.size() : scene.mExpandMeshes.size();

        Vector3f center = CacheBuffer.getCachedVec3();
        Vector3f extent = CacheBuffer.getCachedVec3();
        FloatBuffer centerBuffers = CacheBuffer.getCachedFloatBuffer(256 * 256 * 4);
        FloatBuffer extentBuffers = mExtentBuffers;

        final float NeverOcclusionTestDistanceSquare = 1f;
        final int height = Numeric.divideAndRoundUp(numMeshes, 256);
        final int totalNumAABBBs = 256 * height;
        for(int meshIdx = 0; meshIdx < totalNumAABBBs; meshIdx++){
            if(meshIdx < numMeshes){
                BoundingBox boundingBox = scene.mExpandMeshes.get(fromInvisible ? m_InvisiableMeshIdx.get(meshIdx):meshIdx ).mAABB;

                Vector3f.sub(boundingBox._max, boundingBox._min, extent);
                extent.scale(0.5f);
                Vector3f.add(boundingBox._min, extent, center);

                float nearCamera;
                {
                    float distSquFromCenterToEye = Vector3f.distanceSquare(center, scene.mEye);
                    float extentSqu = Vector3f.lengthSquared(extent);

                    nearCamera = (distSquFromCenterToEye - extentSqu) > NeverOcclusionTestDistanceSquare ? 1: 0;
                }

                center.store(centerBuffers);centerBuffers.put(0);
                extent.store(extentBuffers);extentBuffers.put(nearCamera);
            }
            else
            {
                // todo this is too slowly
                centerBuffers.put(0).put(0).put(0).put(0);
                extentBuffers.put(0).put(0).put(0).put(0);
            }
        }

        centerBuffers.flip();
        extentBuffers.flip();
        gl.glTextureSubImage2D(mCenterTex.getTexture(), 0, 0, 0, 256, height, GLenum.GL_RGBA, GLenum.GL_FLOAT, centerBuffers);
        gl.glTextureSubImage2D(mExtentTex.getTexture(), 0, 0, 0, 256, height, GLenum.GL_RGBA, GLenum.GL_FLOAT, extentBuffers);

        CacheBuffer.free(center);
        CacheBuffer.free(extent);

        GLCheck.checkError();
    }

    private void HizTesting(Scene scene, boolean bCoarse){
        final Matrix4f viewProj = CacheBuffer.getCachedMatrix();
        Matrix4f.mul(scene.mProj, scene.mView, viewProj);

        /*
         * Defines the maximum number of mipmaps the HZB test is considering
         * to avoid memory cache trashing when rendering on high resolution.
         */
        final float kHZBTestMaxMipmap = 9.0f;

        final float HZBMipmapCounts = Numeric.log2(Math.max(mHZBuffer.getWidth(), mHZBuffer.getHeight()));
        final float HZBUvFactorX = scene.mViewWidth/(2.0f * mHZBuffer.getWidth());
        final float HZBUvFactorY = scene.mViewHeight/(2.0f * mHZBuffer.getHeight());
        final float HZBUvFactorZ = Math.max(HZBMipmapCounts - kHZBTestMaxMipmap, 0.0f);
        final int numMeshes = bCoarse ? scene.mExpandMeshes.size() : m_InvisiableMeshIdx.size();

        mFBO.bind();
        mFBO.setRenderTexture(mVisibleTex, null);
        gl.glViewport(0,0, mVisibleTex.getWidth(), mVisibleTex.getHeight());  // todo need reset the viewport when the testing done!

        boolean enableScissor = false;
        final int height = Numeric.divideAndRoundUp(numMeshes, 256);
        if(height < mVisibleTex.getHeight()){
            gl.glEnable(GLenum.GL_SCISSOR_TEST);
            enableScissor = true;
            gl.glScissor(0,0, 256, height);
        }

        mHZBTester.enable();
        GLSLUtil.setMat4(mHZBTester, "gViewProj", viewProj);
        GLSLUtil.setFloat3(mHZBTester, "HZBUvFactor", HZBUvFactorX, HZBUvFactorY,HZBUvFactorZ);
        GLSLUtil.setInt(mHZBTester, "OccluderCount", numMeshes);

        gl.glBindTextureUnit(0, mHZBuffer.getTexture());
        gl.glBindTextureUnit(1, mCenterTex.getTexture());
        gl.glBindTextureUnit(2, mExtentTex.getTexture());
        gl.glBindSampler(0, m_PointMipmapSampler);
        gl.glBindSampler(1, m_PointSampler);
        gl.glBindSampler(2, m_PointSampler);

        gl.glBindVertexArray(mDummyVAO);
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        mHZBTester.printOnce();
        gl.glBindVertexArray(0);

        gl.glBindSampler(0, 0);
        gl.glBindSampler(1, 0);
        gl.glBindSampler(2, 0);

        if(enableScissor){
            gl.glDisable(GLenum.GL_SCISSOR_TEST);
        }

        CacheBuffer.free(viewProj);
        GLCheck.checkError();
        // Reading results.
        gl.glBindBuffer(GLenum.GL_PIXEL_PACK_BUFFER, mVisibleBuffer.getBuffer());
        gl.glReadBuffer(GLenum.GL_COLOR_ATTACHMENT0);
        gl.glReadPixels(0,0, 256, height, GLenum.GL_RED, GLenum.GL_UNSIGNED_BYTE, 0);
        gl.glBindBuffer(GLenum.GL_PIXEL_PACK_BUFFER, 0);
        GLCheck.checkError();
    }

    public void getResults(Scene scene){
        if(!m_bCoarseTesting)
            throw new IllegalStateException("The Coarse Testing hasn't invoked!");

        final boolean bCoarse = true;
        final int numMeshes = bCoarse ? scene.mExpandMeshes.size() : m_InvisiableMeshIdx.size();
        ByteBuffer results = mVisibleBuffer.map(0, numMeshes, GLenum.GL_MAP_READ_BIT);
        if(bCoarse)
            m_InvisiableMeshIdx.clear();

        int cullingCount = 0;
        for(int meshIdx = 0; meshIdx < numMeshes; meshIdx++){
            boolean visible = (results.get(meshIdx) != 0);
            scene.mExpandMeshVisible.set(bCoarse ? meshIdx : m_InvisiableMeshIdx.get(meshIdx),visible);
            if(!visible && bCoarse){
                m_InvisiableMeshIdx.push(meshIdx);
            }

            if(!visible){
                cullingCount++;
            }
        }

        mVisibleBuffer.unmap();
        if(mFrameNumber % 60 == 0){
            System.out.println((bCoarse ? "Culling Count: ": "Fine Count: ") + cullingCount);
        }
    }
}
