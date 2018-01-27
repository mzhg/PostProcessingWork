package jet.opengl.demos.intel.avsm;

import com.nvidia.developer.opengl.utils.BoundingBox;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Random;

import jet.opengl.demos.intel.cput.CPUTCamera;
import jet.opengl.demos.intel.cput.ID3D11InputLayout;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/9/29.
 */

final class ParticleSystem implements Disposeable{

    // Version of the binary particles format
    static final int NUM_VERTS_PER_PARTICLE = 4;

//    extern D3DXMATRIX gLightViewProjection;

    //--------------------------------------------------------------------------------------
    static final int MAX_SLICES    = 32;

    private int mMaxNumParticles;
    private int mMaxNumEmitters;
    private ParticleEmitter[] mpEmitters;

    private int  mEmitterCount;
    private int  mNumSlices;
    private int  mActiveParticleCount;
    private int  mEvenOdd;
    private int  mNewParticleCount;
    private float mLookDistance;
    private float mLightDistance;
    private float mLightWidth;
    private float mLightHeight;
    private float mLightNearClipDistance;
    private float mLightFarClipDistance;
    private float mSign;

    // Used to track min and max particle distance values for partitioning sort bins (i.e., slices)
    private float mMinDist;
    private float mMaxDist;
    private float mMinDistPrev;
    private float mMaxDistPrev;
    private boolean  mUnderBlend;

    // Used to track BB for all particles
    private final BoundingBox mBBox = new BoundingBox();

    // Each particle has two triangles.  Each triangle has three vertices.  Total;
    private int  mNumVertices;
    private final int[]  mpParticleCount = new int[MAX_SLICES];  // TODO This variable could remove

    private final Particle[][] mpParticleBuffer = new Particle[2][];
    private Particle[] mpFirstParticleSource;
    private Particle[] mpFirstParticleDest;
    private Particle[] mpCurParticleDest;
    private int mpCurParticleDestOffset;
    private int mCurParticleIndex;
    private final IntBuffer[] mpParticleSortIndex = new IntBuffer[MAX_SLICES];
    private final IntBuffer[] mpSortBinHead = new IntBuffer[MAX_SLICES];
    private final Integer[]  mpSortDummy;
    private final Vector3f   mLightPosition = new Vector3f();
    private final Vector3f   mLightLook = new Vector3f();
    private final Vector3f   mLightUp = new Vector3f();
    private final Vector3f   mLightRight = new Vector3f();
    private final Vector3f   mHalfAngleLook = new Vector3f();
    private final Vector3f   mEyeDirection = new Vector3f();
    private final Random     mRandom = new Random();

    Camera mpCamera;
    private boolean          mEnableSizeUpdate;
    private boolean          mEnableOpacityUpdate;

    private ID3D11InputLayout mpParticleVertexLayout;
    private BufferGL      mpParticleVertexBuffer;
    private BufferGL mpParticleVertexBufferSRV;
    private GLFuncProvider gl;

    // sort dest buffer
    static Particle[] mpSortedParticleBuffer;
    private Matrix4f gLightViewProjection;

    ParticleSystem(int maxNumParticles){
        mMaxNumParticles = maxNumParticles;
        gLightViewProjection = new Matrix4f();

        mEmitterCount = 0;
        mpEmitters = null;
        mNumSlices = MAX_SLICES;
        mActiveParticleCount = 0;
        mEvenOdd = 0;  // We ping-pong the particle source buffers.  This allows us to discard dead particles.
        mNewParticleCount = 2; // Num particles to spawn each frame
        mLookDistance  = 10.0f;
        mLightDistance = 20.0f;
        mLightWidth    = 1.0f;
        mLightHeight   = 1.0f;
        mLightNearClipDistance = 0.1f;
        mLightFarClipDistance  = 100.0f;
        mUnderBlend = false;
        mEnableOpacityUpdate = true;
        mEnableSizeUpdate    = true;

        // Used to track min and max particle distance values for partitioning sort bins (i.e., slices)
        mMinDist = Float.MAX_VALUE;
        mMaxDist = -mMinDist;
        mMinDistPrev = Float.MAX_VALUE;
        mMaxDistPrev = -mMinDistPrev;


        // Each particle has two triangles.  Each triangle has three vertices.  Total = six vertices per particle
        final int maxVertices = mMaxNumParticles * 6;
        mNumVertices = maxVertices;

        mpFirstParticleSource = null;
        mpFirstParticleDest = null;
        mpCurParticleDest = null;
        mCurParticleIndex = 0;
//        mpParticleVertexLayout = NULL;

        mpParticleBuffer[0] = null;
        mpParticleBuffer[1] = null;
        mpSortDummy = new Integer[mMaxNumParticles];

//        mpCamera = NULL;

        int ii;
        mpParticleVertexBuffer = null;
        for(ii = 0; ii < MAX_SLICES; ii++)
        {
            mpParticleCount[ii] = 0;
            mpParticleSortIndex[ii] = null;
            mpSortBinHead[ii] = null;
        }
    }

    void SetNewParticleCount(int val) { mNewParticleCount = val; }
    void EnableOpacityUpdate(boolean b) { mEnableOpacityUpdate = b; }
    void EnableSizeUpdate(boolean b)    { mEnableSizeUpdate = b; }
    int GetParticleCount() {
        int particleCount = 0;
        for(int i = 0; i < mNumSlices; ++i) {
            particleCount += mpParticleCount[i];
        }

        return  particleCount;
    }
    BufferGL GetParticleBufferSRV() {return mpParticleVertexBufferSRV;}
    boolean GetUnderBlendState() { return mUnderBlend; }
    BoundingBox GetBBox() {return mBBox ;}

    void InitializeParticles(ParticleEmitter[] pEmitter, int emitterCount, //ID3D11Device *pD3d,
                             int width, int height,
                             Macro[] shaderDefines){
        InitializeParticlesLayoutAndEffect(/*pD3d, hr,*/ shaderDefines);
        CreateParticlesBuffersAndVertexBuffer(mNumVertices/*, hr, pD3d*/);

        // ***************************************************
        // Intialize the emitters
        // ***************************************************
        mpEmitters = new ParticleEmitter[emitterCount];
        assert(mpEmitters != null);
        for (int i = 0; i < emitterCount; ++i)
        {
            if (null != pEmitter[i]) {
//                memcpy(&mpEmitters[i], pEmitter[i], sizeof(ParticleEmitter));
                mpEmitters[i] = pEmitter[i];  // reference copy TODO
            } else {
//                memset(&mpEmitters[i], 0, sizeof(ParticleEmitter));
                mpEmitters[i] = new ParticleEmitter();  // TODO Note here make the emitter to zeros.
            }
        }

        mEmitterCount = emitterCount;

        int seed = 1234;  // choose a seed value
//        srand(seed);  //initialize random number generator
        mRandom.setSeed(seed);

        ReadableVector3f i0 = Vector3f.ZERO;
        ReadableVector3f i1 = Vector3f.X_AXIS;
        ReadableVector3f i2 = Vector3f.Z_AXIS;

        mpCamera = new Camera(1.0f,
                1.0f,
                1.0f,
                1000.0f,
                i0,
                i1,
                i2 );
    }

    void UpdateParticles(CPUTCamera pViewCamera, CPUTCamera pLightCamera, float deltaSeconds){
        deltaSeconds *= 2.0f;

        // Update the camera
        float fov       = pViewCamera.GetFov();
        float aspect    = pViewCamera.GetAspectRatio();
        float nearClip  = pViewCamera.GetNearPlaneDistance();
        float farClip   = pViewCamera.GetFarPlaneDistance();

        Matrix4f viewWorld    = pViewCamera.GetWorldMatrix();
        Matrix4f lightWorld   = pLightCamera.GetWorldMatrix();

        /*Vector3f position = new Vector3f();
        Vector3f lookat = new Vector3f();
        Vector3f up = new Vector3f();

        pViewCamera.GetPosition(position);
        pViewCamera.GetLook(lookat);
        pViewCamera.GetUp(up);*/

        mpCamera.SetPositionAndOrientation(
                fov,
                aspect,
                nearClip,
                farClip,
                /**(( D3DXVECTOR3* )&viewWorld._41)   position,*/
        /**(( D3DXVECTOR3* )&viewWorld._31)  lookat,*/
        /**(( D3DXVECTOR3* )&viewWorld._21) up*/
                pViewCamera.GetViewMatrix()  // TODO
        );

        ResetBBox();

        // Ping pong between buffers
        mEvenOdd = /*mEvenOdd ? 0 : 1*/1-mEvenOdd;
        mpFirstParticleSource = mpParticleBuffer[/*mEvenOdd ? 0 : 1*/1-mEvenOdd];
        mpFirstParticleDest   = mpParticleBuffer[/*mEvenOdd ? 1 : 0*/mEvenOdd];

        mpCurParticleDest = mpFirstParticleDest;
        mpCurParticleDestOffset = 0;
        mCurParticleIndex = 0;

        /*mLightRight = *(( D3DXVECTOR3* )&lightWorld._11);
        mLightUp    = *(( D3DXVECTOR3* )&lightWorld._21);
        mLightLook  = *(( D3DXVECTOR3* )&lightWorld._31);*/

        pLightCamera.GetRight(mLightRight);
        pLightCamera.GetUp(mLightUp);
        pLightCamera.GetLook(mLightLook);

        UpdateLightViewProjection();

        // Use last frame's min and max as a starting point for this frame.
        mMinDist = mMinDistPrev;
        mMaxDist = mMaxDistPrev;

        // Reset last frame's min and max so we can determine them for this frame.
        mMinDistPrev = Float.MAX_VALUE;
        mMaxDistPrev = -mMinDistPrev;

        // Reset the sort bins
        int ii;
        for(ii = 0; ii < mNumSlices; ii++) {
            mpSortBinHead[ii] = mpParticleSortIndex[ii];
            mpSortBinHead[ii].position(0);
            mpParticleCount[ii] = 0;
        }

        // Update existing particles
        int endIndex   = Math.min( mActiveParticleCount, mMaxNumParticles );
        mActiveParticleCount = 0;

//        Particle *pCurParticleSource = &mpFirstParticleSource[0];
        int pCurParticleSourceIndex = 0;
        Particle pCurParticleSource = mpFirstParticleSource[pCurParticleSourceIndex];
        for( ii=0; ii<endIndex; ii++ ) {
            // Subtract time before processing particle to avoid negative values.
            pCurParticleSource.mRemainingLife -= deltaSeconds;

            // Ignore "dead" particles.
            if( pCurParticleSource.mRemainingLife > 0.0f ) {
                // Get pointer to next particle and increment it.
                // Do it within a mutex to be sure we're the only one updating this particle.
//                Particle *pDest = mpCurParticleDest++;
                Particle pDest = mpCurParticleDest[mpCurParticleDestOffset++];
                int particleIndex = mCurParticleIndex++;
                mActiveParticleCount++;

                pDest.mRemainingLife = pCurParticleSource.mRemainingLife;

                pDest.mpPos[0] = pCurParticleSource.mpPos[0] + pCurParticleSource.mpVelocity[0] * deltaSeconds;
                pDest.mpPos[1] = pCurParticleSource.mpPos[1] + pCurParticleSource.mpVelocity[1] * deltaSeconds;
                pDest.mpPos[2] = pCurParticleSource.mpPos[2] + pCurParticleSource.mpVelocity[2] * deltaSeconds;

//                D3DXVECTOR3 pos(pDest->mpPos[0], pDest->mpPos[1], pDest->mpPos[2]);
//                float sliceDistance = D3DXVec3Dot( &pos, &mHalfAngleLook );
                float sliceDistance = pDest.mpPos[0]*mHalfAngleLook.x+pDest.mpPos[1]*mHalfAngleLook.y+pDest.mpPos[2]*mHalfAngleLook.z;

                mMinDist = Math.min( mMinDist, sliceDistance );
                mMaxDist = Math.max( mMaxDist, sliceDistance );

                mMinDistPrev = Math.min( mMinDistPrev, sliceDistance );
                mMaxDistPrev = Math.max( mMaxDistPrev, sliceDistance );

                // *************************
                // Add this particle's index to the slice's bin
                // *************************
                float range = mMaxDist - mMinDist;
                float sliceWidth = range / mNumSlices;
                float minDist = mMaxDist - mNumSlices * sliceWidth;  // minDist == mMinDist

                int sliceIndex = (int)((sliceDistance-minDist)/sliceWidth);
                sliceIndex = Math.min(mNumSlices-1, sliceIndex);
                sliceIndex = Math.max(0, sliceIndex);

            /**mpSortBinHead[sliceIndex] = particleIndex;
                mpSortBinHead[sliceIndex]++;*/
                mpSortBinHead[sliceIndex].put(particleIndex);
                mpParticleCount[sliceIndex]++;

                // Update velocity
                int idx = 0;//pCurParticleSource->mEmitterIdx;
                float velocityScale = 1.0f - (mpEmitters[idx].mDrag * deltaSeconds);
                pDest.mpVelocity[0] = pCurParticleSource.mpVelocity[0] * velocityScale;
                pDest.mpVelocity[1] = pCurParticleSource.mpVelocity[1] * velocityScale
                        + mpEmitters[idx].mGravity * deltaSeconds; // Y also gets gravity
                pDest.mpVelocity[2] = pCurParticleSource.mpVelocity[2] * velocityScale;

                if (mEnableSizeUpdate) {
                    pDest.mSize = pCurParticleSource.mSize * (1.0f + mpEmitters[idx].mSizeRate * deltaSeconds);
                }

                final float opacityScale = 0.5f;
                if (mEnableOpacityUpdate) {
                    pDest.mOpacity = opacityScale * pCurParticleSource.mRemainingLife / mpEmitters[idx].mLifetime;
                }

                UpdateBBox(pDest);
            }
//            pCurParticleSource++;
            pCurParticleSourceIndex++;
            pCurParticleSource = mpFirstParticleSource[pCurParticleSourceIndex];
        } // foreach particle


        if (deltaSeconds > 0) {
            SpawnNewParticles();
        }
    }

    static int CompareZ(Integer a, Integer b){
        float fa = mpSortedParticleBuffer[a].mSortDistance;
        float fb = mpSortedParticleBuffer[b].mSortDistance;
        if (fa > fb) {
            return 1;
        }
        if (fa < fb) {
            return  -1;
        }
        return 0;
    }

    void SortParticles(float depthBounds[/*2*/], Matrix4f SortSpaceMat, boolean SortBackToFront /*= true*/, int SliceCount /*= 1*/, boolean EnableUnderBlend /*= false*/){
        float depthMin =   1e20f;
        float depthMax =  -1e20f;

        int i;
        int particleCount = GetParticleCount();
        // reset particles count and indices
        for(i = 0; i < mNumSlices; i++) {
            mpSortBinHead[i] = mpParticleSortIndex[i];
            mpSortBinHead[i].position(0);
            mpParticleCount[i] = 0;
        }

        // Store slice count
        mNumSlices = SliceCount;

        // Compute sort key
        final Vector3f transformedPos = new Vector3f();
        mCurParticleIndex = 0;
        int pDestIndex = 0;
        Particle pDest = mpFirstParticleDest[pDestIndex];
        final Vector3f pos = new Vector3f(pDest.mpPos[0], pDest.mpPos[1], pDest.mpPos[2]);
        for(i = 0; i < particleCount; i++) {
//            D3DXVECTOR3 pos(pDest->mpPos[0], pDest->mpPos[1], pDest->mpPos[2]);
//            D3DXVec3Transform(&transformedPos, &pos, SortSpaceMat);
            pos.set(pDest.mpPos[0], pDest.mpPos[1], pDest.mpPos[2]);
            Matrix4f.transformVector(SortSpaceMat, pos, transformedPos);
            float sliceDistance = transformedPos.z;

            depthMin = Math.min(depthMin, sliceDistance);
            depthMax = Math.max(depthMax, sliceDistance);

            pDest.mSortDistance = sliceDistance;

        /**mpSortBinHead[0] = mCurParticleIndex;
            mpSortBinHead[0]++;*/
            mpSortBinHead[0].put(mCurParticleIndex);
            mpParticleCount[0]++;

//            pDest++;
            pDestIndex++;
            pDest = mpFirstParticleDest[pDestIndex];
            mCurParticleIndex++;
        }

        if (null != depthBounds) {
            depthBounds[0] = depthMin;
            depthBounds[1] = depthMax;
        }

        final float sliceWidth  = (depthMax - depthMin) / mNumSlices;
        if (mNumSlices > 1) {
            mCurParticleIndex = 0;
//            pDest = mpFirstParticleDest;

            pDestIndex = 0;
            pDest = mpFirstParticleDest[pDestIndex];
            for(i = 0; i < mNumSlices; i++) {
                mpSortBinHead[i] = mpParticleSortIndex[i];
                mpSortBinHead[i].position(0);
                mpParticleCount[i] = 0;
            }

            // Slice!
            for(i = 0; i < particleCount; i++) {
                int sliceIndex = (int)((pDest.mSortDistance - depthMin) / sliceWidth);

                sliceIndex = Math.min(mNumSlices - 1, sliceIndex);
                sliceIndex = Math.max(0, sliceIndex);

                /*mpSortBinHead[sliceIndex] = mCurParticleIndex;
                mpSortBinHead[sliceIndex]++;*/
                mpSortBinHead[sliceIndex].put(mCurParticleIndex);
                mpParticleCount[sliceIndex]++;

//                pDest++;
                pDestIndex++;
                pDest = mpFirstParticleDest[pDestIndex];
                mCurParticleIndex++;
            }
        }

        // Compute sort distance
        int slice;
        if (mNumSlices == 1) {
            int ii;
//            UINT *pSortIndex = mpParticleSortIndex[0];
            IntBuffer pSortIndex = mpParticleSortIndex[0];
            particleCount = mpParticleCount[0];
            final Vector3f transfPos = new Vector3f();
//            final Vector3f pos = new Vector3f();
            final float sortSign = SortBackToFront ? -1.0f : 1.0f;
            for(ii =0; ii < particleCount; ii++)
            {
//                D3DXVECTOR4 transfPos;
                pos.set(mpFirstParticleDest[pSortIndex.get(ii)].mpPos[0],   // pSortIndex[ii]
                    mpFirstParticleDest[pSortIndex.get(ii)].mpPos[1],
                    mpFirstParticleDest[pSortIndex.get(ii)].mpPos[2]);

//                D3DXVec4Transform(&transfPos, &pos, SortSpaceMat);

                mpFirstParticleDest[pSortIndex.get(ii)].mSortDistance = sortSign * transfPos.z;   // pSortIndex[ii]
            }
        }

        for(slice = 0; slice < mNumSlices; slice++) {
//            UINT *pSortIndex = mpParticleSortIndex[slice];
            IntBuffer pSortIndex = mpParticleSortIndex[slice];
            particleCount = mpParticleCount[slice];
            if( particleCount > 1) {
                // Pass destination particle buffer to the qsort callback
                mpSortedParticleBuffer = mpFirstParticleDest;
                qsort(pSortIndex);

            }
        } // foreach slice
    }

    void qsort(IntBuffer data){
        for(int i = 0; i < data.position(); i++){
            mpSortDummy[i] = data.get(i);
        }

        Arrays.sort(mpSortDummy, 0, data.position(), ParticleSystem::CompareZ);

        for(int i = 0; i < data.position(); i++){
//            mpSortDummy[i] = data.get(i);
            data.put(i, mpSortDummy[i]);
        }
    }

    void PopulateVertexBuffers(/*ID3D11DeviceContext *pD3dCtx*/){
        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(mpParticleVertexBuffer.getBufferSize());
        SimpleVertex pVertex = new SimpleVertex();

        int sliceIndex;
        for(sliceIndex = 0; sliceIndex < mNumSlices; sliceIndex++) {
            IntBuffer pParticleSortIndex = mpParticleSortIndex[sliceIndex];
            int particleCountThisSlice = mpParticleCount[sliceIndex];
            if(pParticleSortIndex.position() != particleCountThisSlice){
                throw new IllegalArgumentException("Inner error!");
            }

            int ii;
            for( ii=0; ii < particleCountThisSlice; ii++ ) {
                Particle pCurParticle = mpFirstParticleDest[pParticleSortIndex.get(ii)];

                // For now, hack a world-view-projection transformation.
                float xx = pCurParticle.mpPos[0];
                float yy = pCurParticle.mpPos[1];
                float zz = pCurParticle.mpPos[2];
                float opacity = pCurParticle.mOpacity;

                // Fade with square of age.
                // opacity *= opacity;

                // Note: xx, yy, zz, size, UVs, and opacity are the same for all six vertices
                // Also, the UVs are always follow the same pattern (i.e., (0,1), (1,0), (0,0), etc..).
                pVertex.mpPos[0] = xx;     // pVertex[0]
                pVertex.mpPos[1] = yy;
                pVertex.mpPos[2] = zz;
                pVertex.mpUV[0]  = 0.0f;
                pVertex.mpUV[1]  = 1.0f;
                pVertex.mSize    = pCurParticle.mSize;
                pVertex.mOpacity = opacity;
                pVertex.store(buffer);

                pVertex.mpPos[0] = xx;   // pVertex[1]
                pVertex.mpPos[1] = yy;
                pVertex.mpPos[2] = zz;
                pVertex.mpUV[0]  = 1.0f;
                pVertex.mpUV[1]  = 0.0f;
                pVertex.mSize    = pCurParticle.mSize;
                pVertex.mOpacity = opacity;
                pVertex.store(buffer);

                pVertex.mpPos[0] = xx; // pVertex[2]
                pVertex.mpPos[1] = yy;
                pVertex.mpPos[2] = zz;
                pVertex.mpUV[0]  = 0.0f;
                pVertex.mpUV[1]  = 0.0f;
                pVertex.mSize    = pCurParticle.mSize;
                pVertex.mOpacity = opacity;
                pVertex.store(buffer);

                pVertex.mpPos[0] = xx;   // pVertex[3]
                pVertex.mpPos[1] = yy;
                pVertex.mpPos[2] = zz;
                pVertex.mpUV[0]  = 0.0f;
                pVertex.mpUV[1]  = 1.0f;
                pVertex.mSize    = pCurParticle.mSize;
                pVertex.mOpacity = opacity;
                pVertex.store(buffer);

                pVertex.mpPos[0] = xx;   // pVertex[4]
                pVertex.mpPos[1] = yy;
                pVertex.mpPos[2] = zz;
                pVertex.mpUV[0]  = 1.0f;
                pVertex.mpUV[1]  = 1.0f;
                pVertex.mSize    = pCurParticle.mSize;
                pVertex.mOpacity = opacity;
                pVertex.store(buffer);

                pVertex.mpPos[0] = xx;   // pVertex[5]
                pVertex.mpPos[1] = yy;
                pVertex.mpPos[2] = zz;
                pVertex.mpUV[0]  = 1.0f;
                pVertex.mpUV[1]  = 0.0f;
                pVertex.mSize    = pCurParticle.mSize;
                pVertex.mOpacity = opacity;
                pVertex.store(buffer);

//                pVertex += 6;
            }
        } // foreach slice

//        pD3dCtx->Unmap(mpParticleVertexBuffer, 0);
        buffer.flip();
        mpParticleVertexBuffer.update(0, buffer);
    }

    void Draw(/*ID3D11DeviceContext *pD3dCtx,*/ GLSLProgram RenderTechnique, int Start, int Count, boolean Tessellate, boolean DrawSlice /*= false*/){
        // Setup input assembler
        int vbOffset = 0;
        int Stride = SimpleVertex.SIZE;
//        pD3dCtx->IASetInputLayout(mpParticleVertexLayout);
        if(!Tessellate) {
//            pD3dCtx -> IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
        }else {
//            pD3dCtx -> IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_3_CONTROL_POINT_PATCHLIST);
            gl.glPatchParameteri(GLenum.GL_PATCH_VERTICES, 3);
        }
//        pD3dCtx->IASetVertexBuffers(0, 1, &mpParticleVertexBuffer, &Stride, &vbOffset);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, mpParticleVertexBuffer.getBuffer());
        mpParticleVertexLayout.bind();

        if (DrawSlice)
        {
            int sliceIndex;
            int particleCount = 0;

            for(sliceIndex = 0; sliceIndex < Start; sliceIndex++) {
                particleCount += mpParticleCount[sliceIndex];
            }

            Count = mpParticleCount[Start];
            Start = particleCount;
        }

//        pD3dCtx->Draw(6 * Count, 6 * Start);
        if(Tessellate){
            gl.glDrawArrays(GLenum.GL_PATCHES, 6 * Count, 6 * Start);
        }else{
            gl.glDrawArrays(GLenum.GL_TRIANGLES, 6 * Count, 6 * Start);
        }

        mpParticleVertexLayout.unbind();
    }

    void InitializeParticles(int numVerts, SimpleVertex[] sv,// ID3D11Device *pD3d,
                             int width,  int height,
                             Macro[] shaderDefines){
        InitializeParticlesLayoutAndEffect(/*pD3d, hr,*/ shaderDefines);
        CreateParticlesBuffersAndVertexBuffer(numVerts/*, hr, pD3d*/);

        //
        // UpdateLightViewProjection  relies on the fact that
        // Emitter is in front of the camera.
        // So, set the Emitter to one of the particles position.
        //

        mpEmitters = new ParticleEmitter[1];
        mpEmitters[0].mpPos[0] = sv[0].mpPos[0];
        mpEmitters[0].mpPos[1] = sv[0].mpPos[1];
        mpEmitters[0].mpPos[2] = sv[0].mpPos[2];
        mEmitterCount = 1;

        /*D3DXVECTOR3 i0(0.0f, 0.0f, 0.0f);
        D3DXVECTOR3 i1(1.0f, 0.0f, 0.0f);
        D3DXVECTOR3 i2(0.0f, 0.0f, 1.0f);
        mpCamera = new Camera(1.0f,
                1.0f,
                1.0f,
                1000.0f,
                i0,
                i1,
                i2 );*/
        mpCamera = new Camera();
        mpCamera.SetPositionAndOrientation((float)Math.toDegrees(1.0f), 1.0f,
                1.0f, 1000.0f,
                Vector3f.ZERO, Vector3f.X_AXIS, Vector3f.Z_AXIS);

        //Update the particles' system state.
        mEvenOdd = /*mEvenOdd ? 0 : 1*/ 1- mEvenOdd;
        mpFirstParticleSource = mpParticleBuffer[/*mEvenOdd ? 0 : 1*/1-mEvenOdd];
        mpFirstParticleDest   = mpParticleBuffer[/*mEvenOdd ? 1 : 0*/mEvenOdd];

        mpCurParticleDest = mpFirstParticleDest;
        mCurParticleIndex = 0;

        // Use last frame's min and max as a starting point for this frame.
        mMinDist = mMinDistPrev;
        mMaxDist = mMaxDistPrev;

        // Reset last frame's min and max so we can determine them for this frame.
        mMinDistPrev = Float.MAX_VALUE;
        mMaxDistPrev = -mMinDistPrev;

        // Reset the sort bins
        int ii;
        for(ii = 0; ii < mNumSlices; ii++) {
            mpSortBinHead[ii] = mpParticleSortIndex[ii];
            mpParticleCount[ii] = 0;
        }

        // Update existing particles
        mActiveParticleCount = 0;

        //Initialize rest of the things and let the particles' system going.
        int newParticleCount = numVerts/6;
        int pCurParticleDestIndex = 0;
        Particle pDest = mpCurParticleDest[pCurParticleDestIndex];
        int oldActiveParticleCount = mActiveParticleCount;
        mActiveParticleCount = Math.min(mActiveParticleCount + newParticleCount, mMaxNumParticles);
        newParticleCount = mActiveParticleCount - oldActiveParticleCount;

        for(ii=0; ii < newParticleCount; ii++ ) {
            int jj = 6*ii;
            pDest.mpPos[0] = sv[jj].mpPos[0];
            pDest.mpPos[1] = sv[jj].mpPos[1];
            pDest.mpPos[2] = sv[jj].mpPos[2];

            /*D3DXVECTOR3 pos(pDest->mpPos[0], pDest->mpPos[1], pDest->mpPos[2]);
            float sliceDistance  = D3DXVec3Dot( &pos, &mLightLook );*/
            float sliceDistance = pDest.mpPos[0] * mLightLook.x + pDest.mpPos[1] * mLightLook.y + pDest.mpPos[2] * mLightLook.z;
            pDest.mSortDistance = mSign * /*D3DXVec3Dot( &pos, &mEyeDirection )*/
                    (pDest.mpPos[0] * mEyeDirection.x + pDest.mpPos[1] * mEyeDirection.y + pDest.mpPos[2] * mEyeDirection.z);

            // Add this particle's index to the slice's bin
            float range = mMaxDist - mMinDist;
            float sliceWidth = range / mNumSlices;
            float minDist = mMaxDist - mNumSlices * sliceWidth;

            int sliceIndex = (int) ((sliceDistance-minDist)/sliceWidth);
            sliceIndex = Math.min(mNumSlices-1, sliceIndex);
            sliceIndex = Math.max(0, sliceIndex);

            /**mpSortBinHead[sliceIndex] = mCurParticleIndex;
            mpSortBinHead[sliceIndex]++;*/
            mpSortBinHead[sliceIndex].put(mCurParticleIndex);
            mpParticleCount[sliceIndex]++;

            pDest.mOpacity = sv[jj].mOpacity;
            pDest.mSize = sv[jj].mSize;
            pDest.mpVelocity[0] = pDest.mpVelocity[0] = pDest.mpVelocity[0] = 0.0f;

            pDest.mRemainingLife = mpEmitters[0].mLifetime;
//            pDest++;  TODO
            pCurParticleDestIndex++;
            pDest = mpCurParticleDest[pCurParticleDestIndex];
            mCurParticleIndex++;
        }

        // Copy all the particles into the second buffer too.
//        memcpy(mpFirstParticleSource, mpCurParticleDest, newParticleCount*sizeof(Particle));  TODO
    }

    void CreateParticlesBuffersAndVertexBuffer(int numVerts/*, HRESULT hr, ID3D11Device *pD3d*/){
        // ***************************************************
        // Create particle simulation buffers
        // ***************************************************
        mpParticleBuffer[0] = new Particle[mMaxNumParticles];
        mpParticleBuffer[1] = new Particle[mMaxNumParticles];
        for(int i = 0; i < mMaxNumParticles; i++){
            mpParticleBuffer[0][i] = new Particle();
            mpParticleBuffer[1][i] = new Particle();
        }


        int ii;
        for(ii = 0; ii < MAX_SLICES; ii++) {
            mpParticleSortIndex[ii] = IntBuffer.allocate(mMaxNumParticles);
        }

        /*// ***************************************************
        // Create Vertex Buffers
        // ***************************************************
        D3D11_BUFFER_DESC bd;
        bd.Usage = D3D11_USAGE_DYNAMIC;
        bd.ByteWidth = sizeof(SimpleVertex) * numVerts;
        bd.BindFlags = D3D11_BIND_VERTEX_BUFFER | D3D10_BIND_SHADER_RESOURCE;
        bd.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
        bd.MiscFlags = 0;CleanupParticles

        hr = pD3d->CreateBuffer(&bd, NULL, &mpParticleVertexBuffer);

        CD3D11_SHADER_RESOURCE_VIEW_DESC shaderResourceDesc(
                D3D11_SRV_DIMENSION_BUFFER,
                DXGI_FORMAT_R32_FLOAT,
                0, (sizeof(SimpleVertex) * numVerts) / sizeof(float), 1);
        hr = pD3d->CreateShaderResourceView(mpParticleVertexBuffer,
                &shaderResourceDesc,
                                        &mpParticleVertexBufferSRV);
        return hr;*/
        mpParticleVertexBuffer = new BufferGL();
        mpParticleVertexBuffer.initlize(GLenum.GL_ARRAY_BUFFER, SimpleVertex.SIZE * numVerts, null, GLenum.GL_DYNAMIC_DRAW);
        mpParticleVertexBuffer.unbind();
    }

    void InitializeParticlesLayoutAndEffect(//ID3D11Device* pD3d, HRESULT hr,
                                               Macro[] shaderDefines){
        /*ID3D10Blob *vertexShaderBlob = NULL;
        hr = D3DX11CompileFromFile(L".\\media\\Shader\\Particle.hlsl", shaderDefines, 0,
                "DynamicParticlesShading_VS", "vs_4_0",
                0, 0, 0, &vertexShaderBlob, 0, 0);
        assert(SUCCEEDED(hr));
        // ***************************************************
        // Define the input layout
        // ***************************************************
        D3D11_INPUT_ELEMENT_DESC layout[] =
                {
                        { "POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0,  0, D3D11_INPUT_PER_VERTEX_DATA, 0 },
                        { "TEXCOORD", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 12, D3D11_INPUT_PER_VERTEX_DATA, 0 },
                        { "TEXCOORD", 1, DXGI_FORMAT_R32_FLOAT,       0, 24, D3D11_INPUT_PER_VERTEX_DATA, 0 },
                };
        UINT numElements = sizeof( layout ) / sizeof( layout[0] );
        hr = pD3d->CreateInputLayout(
                layout,
                numElements,
                vertexShaderBlob->GetBufferPointer(),
                vertexShaderBlob->GetBufferSize(),
                &mpParticleVertexLayout
        );
        return hr;*/

        mpParticleVertexLayout = new ID3D11InputLayout() {
            @Override
            public void bind() {
                final int strideInBytes = Vector3f.SIZE * 2 + 4;
                gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, strideInBytes, 0);  // POSITION
                gl.glEnableVertexAttribArray(0);

                gl.glVertexAttribPointer(1, 3, GLenum.GL_FLOAT, false, strideInBytes, Vector3f.SIZE);  // TEXCOORD0
                gl.glEnableVertexAttribArray(1);

                gl.glVertexAttribPointer(2, 1, GLenum.GL_FLOAT, false, strideInBytes, Vector3f.SIZE * 2);  // TEXCOORD1
                gl.glEnableVertexAttribArray(2);
            }

            @Override
            public void unbind() {
                gl.glDisableVertexAttribArray(0);
                gl.glDisableVertexAttribArray(1);
                gl.glDisableVertexAttribArray(2);
            }
        };
    }

    @Override
    public void dispose() {
        CleanupParticles();
    }

    private void CleanupParticles(){
        int ii;
        for(ii = 0; ii < MAX_SLICES; ii++) {
//            SAFE_DELETE_ARRAY( mpParticleSortIndex[ii] );
        }
        CommonUtil.safeRelease(mpParticleVertexBuffer);
        CommonUtil.safeRelease(mpParticleVertexBufferSRV);
//        SAFE_RELEASE(mpParticleVertexLayout );

//        SAFE_DELETE_ARRAY( mpParticleBuffer[0] );
//        SAFE_DELETE_ARRAY( mpParticleBuffer[1] );
//        SAFE_DELETE_ARRAY( mpEmitters );
//        SAFE_DELETE( mpCamera );
    }

    private void UpdateLightViewProjection(){
        // temp hack - create a Camera from the DXUT Camera
//        const D3DXVECTOR3 &cameraLook     = mpCamera->GetLook();
        final ReadableVector3f cameraLook = mpCamera.GetLook();

        mUnderBlend = Vector3f.dot(cameraLook, mLightLook ) > 0.0f;

        mSign = mUnderBlend ? 1.0f : -1.0f;
//        mHalfAngleLook = mSign * cameraLook + mLightLook;
        Vector3f.linear(mLightLook, cameraLook, mSign, mHalfAngleLook);
//        D3DXVec3Normalize( &mHalfAngleLook, &mHalfAngleLook );
        mHalfAngleLook.normalise();

        mEyeDirection.set(cameraLook);
//        D3DXVec3Normalize( &mEyeDirection, &mEyeDirection );
        mEyeDirection.normalise();

        /*D3DXVec3Cross( &mLightUp, &mLightRight, &mLightLook );
        D3DXVec3Normalize( &mLightUp, &mLightUp );*/
        Vector3f.cross(mLightRight, mLightLook, mLightUp);
        mLightUp.normalise();
//        D3DXVec3Cross( &mLightRight, &mLightLook, &mLightUp );

        // Arbitrarily choose a look point in front of the camera
        // D3DXVECTOR3 lookPoint = cameraPosition + mLookDistance * cameraLook;
        /*D3DXVECTOR3 lookPoint(
                mpEmitters[0].mpPos[0],
                mpEmitters[0].mpPos[1],
                mpEmitters[0].mpPos[2]
    );
        // Fake a view position
        mLightPosition = lookPoint - mLightLook * mLightDistance;
        D3DXMATRIX viewMatrix, projectionMatrix;
        D3DXMatrixLookAtLH
                (
                &viewMatrix,
        &mLightPosition,
        &lookPoint,
        &mLightUp
    );*/

        Vector3f lookPoint = new Vector3f(
                mpEmitters[0].mpPos[0],
                mpEmitters[0].mpPos[1],
                mpEmitters[0].mpPos[2]);
        Vector3f.linear(lookPoint, mLightLook, -mLightDistance, mLightPosition);

        Matrix4f tmp = CacheBuffer.getCachedMatrix();
        Matrix4f viewMatrix = Matrix4f.lookAt(mLightPosition, lookPoint, mLightUp, tmp);

        /*D3DXMatrixOrthoLH
                (
                &projectionMatrix,
                mLightWidth,
                mLightHeight,
                mLightNearClipDistance,
                mLightFarClipDistance);*/
        Matrix4f.ortho(mLightWidth, mLightHeight, mLightNearClipDistance, mLightFarClipDistance, gLightViewProjection);  // TODO
//        gLightViewProjection = viewMatrix * projectionMatrix;
        Matrix4f.mul(gLightViewProjection, viewMatrix, gLightViewProjection);
        CacheBuffer.free(tmp);
    }

    private void SpawnNewParticles(){
        // UINT newParticleCount = mpEmitter->mSpawnRate * mDeltaSeconds;
        int newParticleCount = mNewParticleCount;

//        Particle pDest = mpCurParticleDest;
        int pDestIndex = 0;
        Particle pDest = mpCurParticleDest[pDestIndex];
        for (int k = 0; k < mEmitterCount; ++k) {
            int oldActiveParticleCount = mActiveParticleCount;
            mActiveParticleCount = Math.min(mActiveParticleCount + newParticleCount, mMaxNumParticles);
            newParticleCount = mActiveParticleCount - oldActiveParticleCount;
            int ii;
            for( ii=0; ii < newParticleCount; ii++ ) {
                pDest.mEmitterIdx = k;
                pDest.mpPos[0] = mpEmitters[k].mpPos[0];
                pDest.mpPos[1] = mpEmitters[k].mpPos[1];
                pDest.mpPos[2] = mpEmitters[k].mpPos[2];

//                D3DXVECTOR3 pos(pDest->mpPos[0], pDest->mpPos[1], pDest->mpPos[2]);
                float sliceDistance  = /*D3DXVec3Dot( &pos, &mLightLook );*/
                        pDest.mpPos[0]*mLightLook.x + pDest.mpPos[1]*mLightLook.y + pDest.mpPos[2]*mLightLook.z;
                pDest.mSortDistance = mSign * /*D3DXVec3Dot( &pos, &mEyeDirection );*/
                        (pDest.mpPos[0]*mEyeDirection.x + pDest.mpPos[1]*mEyeDirection.y + pDest.mpPos[2]*mEyeDirection.z);

                // Add this particle's index to the slice's bin
                float range = mMaxDist - mMinDist;
                float sliceWidth = range / mNumSlices;
                float minDist = mMaxDist - mNumSlices * sliceWidth;

                int sliceIndex = (int)((sliceDistance-minDist)/sliceWidth);
                sliceIndex = Math.min(mNumSlices-1, sliceIndex);
                sliceIndex = Math.max(0, sliceIndex);

            /**mpSortBinHead[sliceIndex] = mCurParticleIndex;
                mpSortBinHead[sliceIndex]++;*/
                mpSortBinHead[sliceIndex].put(mCurParticleIndex);
                mpParticleCount[sliceIndex]++;

                // Radomize the angle and radius.
                float  angle  = (2.0f * Numeric.PI * mRandom.nextFloat());
                float  radius = (mpEmitters[k].mRandScaleX *mRandom.nextFloat());
                float  randX = (float)Math.cos(angle) * radius;
                float  randY = (float)(mpEmitters[k].mRandScaleY * (mRandom.nextFloat() - 0.5));
                float  randZ = (float)Math.sin(angle) * radius;

                pDest.mpVelocity[0] = mpEmitters[k].mpVelocity[0] + randX;
                pDest.mpVelocity[1] = mpEmitters[k].mpVelocity[1] + randY;
                pDest.mpVelocity[2] = mpEmitters[k].mpVelocity[2] + randZ;

                pDest.mSize    = mpEmitters[k].mStartSize;
                pDest.mOpacity = 1.0f;

                pDest.mRemainingLife = mpEmitters[k].mLifetime;
                UpdateBBox(pDest);

//                pDest++;
                pDestIndex++;
                pDest = mpCurParticleDest[pDestIndex];  // pointer to next
                mCurParticleIndex++;
            }
        }
    }

    private void ResetBBox(){
        mBBox.init();
    }

    private void UpdateBBox(Particle particle){
        float radius = particle.mSize * 0.5f;
        final Vector3f min = mBBox._min;
        final Vector3f max = mBBox._max;
        for (int p = 0; p < 3; ++p) {
            min.setValue(p, Math.min(particle.mpPos[p] - radius, min.get(p)));
            max.setValue(p, Math.max(particle.mpPos[p] + radius, max.get(p)));
        }
    }
}
