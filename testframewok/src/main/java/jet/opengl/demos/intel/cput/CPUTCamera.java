package jet.opengl.demos.intel.cput;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by mazhen'gui on 2017/11/14.
 */

public class CPUTCamera extends CPUTRenderNode {
    protected float mFov = 45;                // the field of view in degrees
    protected float mNearPlaneDistance = 1.0f;  // near plane distance
    protected float mFarPlaneDistance = 100.f;   // far plane distance
    protected float mAspectRatio =16.0f/9.0f;        // width/height.  TODO: Support separate pixel and viewport aspect ratios

    protected final Matrix4f mView = new Matrix4f();
    protected final Matrix4f mProjection = new Matrix4f();
    protected final Matrix4f mViewProj = new Matrix4f();

    public CPUTCamera(){
        SetPosition(1.0f, 8.0f, 1.0f);
    }

    public void Update( float deltaSeconds/*=0.0f*/ ) {
        // TODO: Do only if required (i.e. if dirty)
        /*mProjection = float4x4PerspectiveFovLH( mFov, mAspectRatio, mNearPlaneDistance, mFarPlaneDistance );
        mView = inverse(*GetWorldMatrix());
        mFrustum.InitializeFrustum(this);*/

        throw new RuntimeException();
    };

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
    public void            LookAt( float xx, float yy, float zz ){throw new RuntimeException();}

    public boolean isCenterExtentVisible(Vector3f mBoundingBoxCenterWorldSpace, Vector3f mBoundingBoxHalfWorldSpace) {
        throw new UnsupportedOperationException();
    }
}
