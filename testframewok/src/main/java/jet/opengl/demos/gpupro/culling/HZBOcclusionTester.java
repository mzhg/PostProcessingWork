package jet.opengl.demos.gpupro.culling;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;
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
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.BoundingBox;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StackInt;

class HZBOcclusionTester implements OcclusionTester{

    private Texture2D mHZBuffer;
    private final GLSLProgram[][] mBuildHZB = new GLSLProgram[2][2];
    private GLSLProgram mHZBTester;
    private GLSLProgram mReprojection;
    private Texture2D mReprojectDepth;

    private final List<Texture2D> mHZBMipLevels = new ArrayList<>();
    private boolean m_bCoarseTesting;
    private boolean m_bPreciselyTesting;
    private GLFuncProvider gl;
    private int m_PointSampler;
    private int m_PointMipmapSampler;

    private BufferGL m_MeshBoundingBoxes;
    private BufferGL m_MeshVisiableBuffer;

    private int mFrameNumber;

    private final StackInt m_InvisiableMeshIdx = new StackInt();

    public Texture2D getHZBSlice(int slice){ return mHZBMipLevels.get(slice);}

    public void newFrame(int frameNumber){
        m_bCoarseTesting = false;
        m_bPreciselyTesting = false;

        mFrameNumber = frameNumber;
    }

    @Override
    public void cullingCoarse(Renderer renderer, Scene scene) {
        try{
            if(renderer.mDepthBuffer == null || scene.mExpandMeshes.size() == 0 || mFrameNumber == 0) {
                return;
            }

            updateResources(renderer.mDepthBuffer);
            reprojectionFull(renderer.mDepthBuffer, scene);
            generateHZB(mReprojectDepth, 0, scene);
            packingMeshInfomations(scene, false);
            HizTesting(scene, true);
        }finally {
            m_bCoarseTesting = true;
            gl.glBindSampler(0,0);
        }
    }

    @Override
    public void cullingFine(Renderer renderer, Scene scene) {
        if(!m_bCoarseTesting)
            throw new IllegalStateException("cullingCoarse must be called before cullingFine");

        if(renderer.mDepthBuffer == null || scene.mExpandMeshes.size() == 0 || mFrameNumber ==0)
            return;

        if(m_InvisiableMeshIdx.isEmpty())
            return;

        generateHZB(renderer.mDepthBuffer, 0, scene);
        packingMeshInfomations(scene, true);
        HizTesting(scene, false);
        gl.glBindSampler(0,0);
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

        /*final int HZBSizeX = depth.getWidth()/2;
        final int HZBSizeY = depth.getHeight()/2;
        final int NumMips = Numeric.calculateMipLevels(HZBSizeX, HZBSizeY);*/

        if(mHZBuffer == null || mHZBuffer.getWidth() != HZBSizeX || mHZBuffer.getHeight() != HZBSizeY){
            CommonUtil.safeRelease(mHZBuffer);
            mHZBMipLevels.clear();

            Texture2DDesc desc = new Texture2DDesc(HZBSizeX, HZBSizeY, GLenum.GL_R16F);
            desc.mipLevels =NumMips;
            mHZBuffer = TextureUtils.createTexture2D(desc, null);

            for(int i = 0; i < NumMips; i++){
                mHZBMipLevels.add(TextureUtils.createTextureView(mHZBuffer, GLenum.GL_TEXTURE_2D, i, 1, 0,1));
            }

            desc.width = depth.getWidth();
            desc.height = depth.getHeight();
            desc.mipLevels = 1;
            mReprojectDepth = TextureUtils.createTexture2D(desc, null);

            GLCheck.checkError();
        }

        final String root = "gpupro/Culling/shaders/";
        if(mBuildHZB[0][0] == null){
            mBuildHZB[0][0] = GLSLProgram.createProgram(root + "BuildHZB.comp", CommonUtil.toArray(new Macro("STAGE", 0), new Macro("REPROJECTION", 0)));
            mBuildHZB[1][1] = GLSLProgram.createProgram(root + "BuildHZB.comp", CommonUtil.toArray(new Macro("STAGE", 1), new Macro("REPROJECTION", 1)));

            mBuildHZB[0][1] = GLSLProgram.createProgram(root + "BuildHZB.comp", CommonUtil.toArray(new Macro("STAGE", 0), new Macro("REPROJECTION", 1)));
            mBuildHZB[1][0] = GLSLProgram.createProgram(root + "BuildHZB.comp", CommonUtil.toArray(new Macro("STAGE", 1), new Macro("REPROJECTION", 0)));

            mBuildHZB[0][0].setName("BuildHZB_Copy");
            mBuildHZB[0][1].setName("BuildHZB_Copy_Reproject");
            mBuildHZB[1][0].setName("BuildHZB_Down");
            mBuildHZB[1][1].setName("BuildHZB_Down_Reproject");

            mReprojection = GLSLProgram.createProgram(root + "ReprojectionFull.comp", null);
            mReprojection.setName("ReprojectionFull");
        }

        if(mHZBTester == null){
            mHZBTester = GLSLProgram.createProgram(root + "HZBTestCS.comp", null);
            mHZBTester.setName("HZBTestCS");
        }

        if(m_PointSampler == 0){
            SamplerDesc desc = new SamplerDesc();
            desc.minFilter = desc.magFilter = GLenum.GL_NEAREST;
            m_PointSampler = SamplerUtils.createSampler(desc);
            desc.minFilter = GLenum.GL_NEAREST_MIPMAP_NEAREST;
            m_PointMipmapSampler = SamplerUtils.createSampler(desc);
        }

        GLCheck.checkError();
    }

    private void reprojectionFull(Texture2D depth, Scene scene){
        mReprojection.enable();

        gl.glClearTexImage(mReprojectDepth.getTexture(), 0, GLenum.GL_RED, GLenum.GL_FLOAT, CacheBuffer.wrap(1.f));

        gl.glBindImageTexture(0, mReprojectDepth.getTexture(), 0, false, 0, GLenum.GL_READ_WRITE, mReprojectDepth.getFormat());
        gl.glBindTextureUnit(0,depth.getTexture());
        gl.glBindSampler(0, m_PointSampler);

        {
            final Matrix4f tmp = CacheBuffer.getCachedMatrix();
            Matrix4f viewProj = Matrix4f.mul(scene.mProj, scene.mView, tmp);
            GLSLUtil.setMat4(mReprojection, "gViewProj",viewProj);

            Matrix4f.mul(scene.mProj, scene.mPrevView, tmp);
            Matrix4f.invert(tmp, tmp);
            GLSLUtil.setMat4(mReprojection, "gPrevViewProjInv",tmp);

            CacheBuffer.free(tmp);
        }

        int numGroupX = Numeric.divideAndRoundUp(depth.getWidth(), 32);
        int numGroupY = Numeric.divideAndRoundUp(depth.getHeight(), 32);
        gl.glDispatchCompute(numGroupX, numGroupY, 1);
        gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        GLCheck.checkError();
        mReprojection.printOnce();
    }

    private void generateHZB(Texture2D depth, int reprojection, Scene scene){
        int downsampling = 0;

        // Create HZB of first mipLevel
        mBuildHZB[downsampling][reprojection].enable();
        gl.glClearTexImage(mHZBuffer.getTexture(), 0, GLenum.GL_RED, GLenum.GL_FLOAT, CacheBuffer.wrap(1.f));

        gl.glBindImageTexture(0, mHZBuffer.getTexture(), 0, false, 0, GLenum.GL_READ_WRITE, mHZBuffer.getFormat());
        gl.glBindTextureUnit(0,depth.getTexture());
        gl.glBindSampler(0, m_PointSampler);

        // Parameters
        {
//            uniform vec2 InvSize;
//            uniform vec4 InputUvFactorAndOffset;
//            uniform vec2 InputViewportMaxBound;

            GLSLUtil.setFloat2(mBuildHZB[downsampling][reprojection], "InvSize", 1.f/depth.getWidth(), 1.f/depth.getHeight());
            GLSLUtil.setFloat4(mBuildHZB[downsampling][reprojection],"InputUvFactorAndOffset", 2.0f * mHZBuffer.getWidth()/depth.getWidth(),
                    2.0f * mHZBuffer.getHeight()/depth.getHeight(),0,0);
            GLSLUtil.setFloat2(mBuildHZB[downsampling][reprojection], "InputViewportMaxBound", 1 - 0.5f/depth.getWidth(), 1-0.5f/depth.getHeight());
        }

        if(reprojection != 0){
            final Matrix4f tmp = CacheBuffer.getCachedMatrix();
            Matrix4f viewProj = Matrix4f.mul(scene.mProj, scene.mView, tmp);
            GLSLUtil.setMat4(mBuildHZB[downsampling][reprojection], "gViewProj",viewProj);

            Matrix4f.mul(scene.mProj, scene.mPrevView, tmp);
            Matrix4f.invert(tmp, tmp);
            GLSLUtil.setMat4(mBuildHZB[downsampling][reprojection], "gPrevViewProjInv",tmp);

            CacheBuffer.free(tmp);
        }

        int numGroupX = Numeric.divideAndRoundUp(mHZBuffer.getWidth(), 4);
        int numGroupY = Numeric.divideAndRoundUp(mHZBuffer.getHeight(), 4);
        gl.glDispatchCompute(numGroupX, numGroupY, 1);
        gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        GLCheck.checkError();
        mBuildHZB[downsampling][reprojection].printOnce();

        // downsampling
        downsampling = 1;
        reprojection = 0;
        mBuildHZB[downsampling][reprojection].enable();
        for(int i = 1; i < mHZBMipLevels.size(); i++){
            Texture2D src = mHZBMipLevels.get(i-1);
            Texture2D dst = mHZBMipLevels.get(i);

            gl.glBindImageTexture(0, dst.getTexture(), 0, false, 0, GLenum.GL_READ_WRITE, mHZBuffer.getFormat());
            gl.glBindTextureUnit(0,src.getTexture());

            GLSLUtil.setFloat2(mBuildHZB[downsampling][reprojection], "InvSize", 1.f/src.getWidth(), 1.f/src.getHeight());

            numGroupX = Numeric.divideAndRoundUp(dst.getWidth(), 4);
            numGroupY = Numeric.divideAndRoundUp(dst.getHeight(), 4);
            gl.glDispatchCompute(numGroupX, numGroupY, 1);
            gl.glMemoryBarrier(GLenum.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
            GLCheck.checkError();
        }
        mBuildHZB[downsampling][reprojection].printOnce();
    }

    private void packingMeshInfomations(Scene scene, boolean fromInvisible){
        final int numMeshes = fromInvisible ? m_InvisiableMeshIdx.size() : scene.mExpandMeshes.size();
        final int boundingBoxBytes = numMeshes * (Vector4f.SIZE * 2);
        final int visibleBytes = numMeshes * 4;

        boolean fillData = false;

        if(m_MeshBoundingBoxes == null || m_MeshBoundingBoxes.getBufferSize() < boundingBoxBytes){
            CommonUtil.safeRelease(m_MeshBoundingBoxes);

            m_MeshBoundingBoxes = new BufferGL();
            m_MeshBoundingBoxes.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, boundingBoxBytes, null, GLenum.GL_DYNAMIC_DRAW);

            fillData = true;
        }

        if(m_MeshVisiableBuffer == null || m_MeshVisiableBuffer.getBufferSize() < visibleBytes){
            CommonUtil.safeRelease(m_MeshVisiableBuffer);

            m_MeshVisiableBuffer = new BufferGL();
            m_MeshVisiableBuffer.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, visibleBytes, null, GLenum.GL_DYNAMIC_READ); // todo
        }

//        if(!fillData) return;

        Vector3f center = CacheBuffer.getCachedVec3();
        Vector3f extent = CacheBuffer.getCachedVec3();
        ByteBuffer boundingBoxes = CacheBuffer.getCachedByteBuffer(boundingBoxBytes);
        for(int meshIdx = 0; meshIdx < numMeshes; meshIdx++){
            BoundingBox boundingBox = scene.mExpandMeshes.get(fromInvisible ? m_InvisiableMeshIdx.get(meshIdx):meshIdx ).mAABB;

            Vector3f.sub(boundingBox._max, boundingBox._min, extent);
            extent.scale(0.5f);

            Vector3f.add(boundingBox._min, extent, center);

            center.store(boundingBoxes);boundingBoxes.putInt(0);
            extent.store(boundingBoxes);boundingBoxes.putInt(0);
        }

        boundingBoxes.flip();
        m_MeshBoundingBoxes.update(0, boundingBoxes);

        CacheBuffer.free(center);
        CacheBuffer.free(extent);
        gl.glClearNamedBufferData(m_MeshVisiableBuffer.getBuffer(), GLenum.GL_R32UI,GLenum.GL_RED_INTEGER,  GLenum.GL_UNSIGNED_INT, null);
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
//        final Vector3f HZBUvFactorValue(
//			float(View.ViewRect.Width()) / float(2 * View.HZBMipmap0Size.X),
//			float(View.ViewRect.Height()) / float(2 * View.HZBMipmap0Size.Y),
//                FMath::Max(HZBMipmapCounts - kHZBTestMaxMipmap, 0.0f)
//			);

        final float HZBUvFactorX = scene.mViewWidth/(2.0f * mHZBuffer.getWidth());
        final float HZBUvFactorY = scene.mViewHeight/(2.0f * mHZBuffer.getHeight());
        final float HZBUvFactorZ = Math.max(HZBMipmapCounts - kHZBTestMaxMipmap, 0.0f);
        final int numMeshes = bCoarse ? scene.mExpandMeshes.size() : m_InvisiableMeshIdx.size();
        final int numGroupX = Numeric.divideAndRoundUp(numMeshes, 32);

        mHZBTester.enable();
        GLSLUtil.setMat4(mHZBTester, "gViewProj", viewProj);
        GLSLUtil.setFloat3(mHZBTester, "HZBUvFactor", HZBUvFactorX, HZBUvFactorY,HZBUvFactorZ);
        GLSLUtil.setInt(mHZBTester, "OccluderCount", numMeshes);

        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, m_MeshBoundingBoxes.getBuffer());
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 2, m_MeshVisiableBuffer.getBuffer());
        gl.glBindTextureUnit(0, mHZBuffer.getTexture());
        gl.glBindSampler(0, m_PointMipmapSampler);

        gl.glDispatchCompute(numGroupX, 1, 1);
        gl.glMemoryBarrier(GLenum.GL_SHADER_STORAGE_BARRIER_BIT);
        mHZBTester.printOnce();

        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 1, 0);
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 2, 0);
        gl.glBindSampler(0, 0);

        CacheBuffer.free(viewProj);
        ByteBuffer results = m_MeshVisiableBuffer.map(GLenum.GL_MAP_READ_BIT);
        if(bCoarse)
            m_InvisiableMeshIdx.clear();

        int cullingCount = 0;
        for(int meshIdx = 0; meshIdx < numMeshes; meshIdx++){
            boolean visible = (results.getInt(meshIdx * 4) == 1);
            scene.mExpandMeshVisible.set(bCoarse ? meshIdx : m_InvisiableMeshIdx.get(meshIdx),visible);
            if(!visible && bCoarse){
                m_InvisiableMeshIdx.push(meshIdx);
            }

            if(!visible){
                cullingCount++;
            }
        }

        m_MeshVisiableBuffer.unmap();
        if(mFrameNumber % 60 == 0){
            System.out.println((bCoarse ? "Culling Count: ": "Fine Count: ") + cullingCount);
        }
    }
}
