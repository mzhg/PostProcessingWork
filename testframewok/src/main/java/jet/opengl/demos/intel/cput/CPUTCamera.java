package jet.opengl.demos.intel.cput;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.util.BoundingBox;

/**
 * Created by mazhen'gui on 2017/11/14.
 */
public class CPUTCamera extends CPUTRenderNode {
    protected float mFov = 45;                // the field of view in degrees
    protected float mNearPlaneDistance = 1.0f;  // near plane distance
    protected float mFarPlaneDistance = 100.f;   // far plane distance
    protected float mAspectRatio =16.0f/9.0f;        // width/height.  TODO: Support separate pixel and viewport aspect ratios
    protected float mWidth = 64;
    protected float mHeight = 64;

    protected final Matrix4f mView = new Matrix4f();
    protected final Matrix4f mProjection = new Matrix4f();
    protected final Matrix4f mViewProj = new Matrix4f();

    private final BoundingBox mBoundingBox = new BoundingBox();
    private final Vector4f[] m_corners = new Vector4f[8];
    private CPUT_PROJECTION_MODE mMode;

    public CPUTCamera(){
        this(CPUT_PROJECTION_MODE.CPUT_PERSPECTIVE);
    }

    public CPUTCamera(CPUT_PROJECTION_MODE mode){
        mMode = mode;
    }

    public final void Update(  ) {
        Update(0);
    }

    @Override
    public void Update(float deltaSeconds) {
        Matrix4f.perspective(mFov, mAspectRatio, mNearPlaneDistance, mFarPlaneDistance, mProjection);
        // We asuume the view matrix has already setup.
        // Upda the world matrix first
        Matrix4f.invertRigid(mView, mViewProj);
        SetParentMatrix(mViewProj);

        Matrix4f.mul(mProjection, mView, mViewProj);

//        mFrustum.InitializeFrustum(this);
    }

    public int LoadCamera(CPUTConfigBlock pBlock/*, int *pParentID*/){
        // TODO: Have render node load common properties.

        mName = pBlock.GetValueByName(_L("name")).ValueAsString();
        int parentID = pBlock.GetValueByName(_L("parent")).ValueAsInt();

        mFov = pBlock.GetValueByName(_L("FieldOfView")).ValueAsFloat();
//        mFov *= (3.14159265f/180.0f);
        mNearPlaneDistance = pBlock.GetValueByName(_L("NearPlane")).ValueAsFloat();
        mFarPlaneDistance = pBlock.GetValueByName(_L("FarPlane")).ValueAsFloat();

        LoadParentMatrixFromParameterBlock( pBlock );
        return parentID;
    }

    public Matrix4f GetViewMatrix()
    {
        // Update();  We can't afford to do this every time we're asked for the view matrix.  Caller needs to make sure camera is updated before entering render loop.
        return mView;
    }

    public Matrix4f        GetViewProjMatrix() { return mViewProj;}
    public Matrix4f        GetProjectionMatrix()  { return mProjection; }
    public void            SetProjectionMatrix(Matrix4f projection) { mProjection.load(projection); }
    public float           GetAspectRatio() { return mAspectRatio; }
    public float           GetFov() { return mFov; }
    public void            SetAspectRatio(float aspectRatio) {mAspectRatio = aspectRatio;}
    public void            SetFov( float fov ){mFov = fov;}
    public float           GetNearPlaneDistance() { return mNearPlaneDistance; }
    public float           GetFarPlaneDistance() {  return mFarPlaneDistance; }
    public void            SetNearPlaneDistance( float nearPlaneDistance ) { mNearPlaneDistance = nearPlaneDistance; }
    public void            SetFarPlaneDistance(  float farPlaneDistance ) { mFarPlaneDistance = farPlaneDistance; }
    public void            SetWidth( float width)  { /*ASSERT_ORTHOGRAPHIC;*/ mWidth  = width;}
    public void            SetHeight(float height) { /*ASSERT_ORTHOGRAPHIC;*/ mHeight = height;};
    public void            LookAt( float xx, float yy, float zz ){
        Matrix4f.lookAt(mParentMatrix.m30, mParentMatrix.m31,  mParentMatrix.m32, xx, yy, zz, 0,1,0, mView);
    }

    public boolean isCenterExtentVisible(Vector3f centerWorldSpace, Vector3f halfWorldSpace) {
        mBoundingBox.setFromExtent(centerWorldSpace, halfWorldSpace);

        if(m_corners[0] == null){
            for(int i = 0; i < m_corners.length; i++)
                m_corners[i] = new Vector4f();
        }

        for(int iClipPlaneCorner=0; iClipPlaneCorner < 8; ++iClipPlaneCorner) {
            Vector4f f3PlaneCornerProjSpace = m_corners[iClipPlaneCorner];
            mBoundingBox.corner(iClipPlaneCorner, f3PlaneCornerProjSpace);

            Matrix4f.transform(mViewProj, f3PlaneCornerProjSpace, f3PlaneCornerProjSpace);  // Transform the position from world to projection
        }

        if (m_corners[0].x < -m_corners[0].w && m_corners[1].x < -m_corners[1].w && m_corners[2].x < -m_corners[2].w && m_corners[3].x < -m_corners[3].w &&
                m_corners[4].x < -m_corners[4].w && m_corners[5].x < -m_corners[5].w && m_corners[6].x < -m_corners[6].w && m_corners[7].x < -m_corners[7].w)
            return false;

        if (m_corners[0].x > m_corners[0].w && m_corners[1].x > m_corners[1].w && m_corners[2].x > m_corners[2].w && m_corners[3].x > m_corners[3].w &&
                m_corners[4].x > m_corners[4].w && m_corners[5].x > m_corners[5].w && m_corners[6].x > m_corners[6].w && m_corners[7].x > m_corners[7].w)
            return false;

        if (m_corners[0].y < -m_corners[0].w && m_corners[1].y < -m_corners[1].w && m_corners[2].y < -m_corners[2].w && m_corners[3].y < -m_corners[3].w &&
                m_corners[4].y < -m_corners[4].w && m_corners[5].y < -m_corners[5].w && m_corners[6].y < -m_corners[6].w && m_corners[7].y < -m_corners[7].w)
            return false;

        if (m_corners[0].y > m_corners[0].w && m_corners[1].y > m_corners[1].w && m_corners[2].y > m_corners[2].w && m_corners[3].y > m_corners[3].w &&
                m_corners[4].y > m_corners[4].w && m_corners[5].y > m_corners[5].w && m_corners[6].y > m_corners[6].w && m_corners[7].y > m_corners[7].w)
            return false;

        if (m_corners[0].z < -m_corners[0].w && m_corners[1].z < -m_corners[1].w && m_corners[2].z < -m_corners[2].w && m_corners[3].z < -m_corners[3].w &&
                m_corners[4].z < -m_corners[4].w && m_corners[5].z < -m_corners[5].w && m_corners[6].z < -m_corners[6].w && m_corners[7].z < -m_corners[7].w)
            return false;

        if (m_corners[0].z > m_corners[0].w && m_corners[1].z > m_corners[1].w && m_corners[2].z > m_corners[2].w && m_corners[3].z > m_corners[3].w &&
                m_corners[4].z > m_corners[4].w && m_corners[5].z > m_corners[5].w && m_corners[6].z > m_corners[6].w && m_corners[7].z > m_corners[7].w)
            return false;

        return true;
    }
}
