package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/11/18.
 */

public class VaCameraBase {
    // attached controller
    protected VaCameraControllerBase m_controller;
    //
    // Primary values
    protected float                           m_YFOV;
    protected float                           m_XFOV;
    protected boolean                         m_YFOVMain;
    protected float                           m_aspect;
    protected float                           m_nearPlane;
    protected float                           m_farPlane;
    protected int                             m_viewportWidth;
    protected int                             m_viewportHeight;
    protected boolean                         m_useReversedZ;
    //

    // in world space
    protected final Vector3f m_position = new Vector3f();
    protected final Quaternion m_orientation = new Quaternion();
    //
    // Secondary values (updated by Tick() )
    protected final Matrix4f m_worldTrans = new Matrix4f();
    protected final Matrix4f m_viewTrans = new Matrix4f();
    protected final Matrix4f m_projTrans = new Matrix4f();
    protected final Vector3f m_direction = new Vector3f();

    public VaCameraBase(){
        m_YFOVMain          = true;
        m_YFOV              = 90.0f /*/ 360.0f * VA_PIf*/;
        m_XFOV              = 0.0f;
        m_aspect            = 1.0f;
        m_nearPlane         = 0.2f;
        m_farPlane          = 100000.0f;
        m_viewportWidth     = 64;
        m_viewportHeight    = 64;
        /*m_position          = vaVector3( 0.0f, 0.0f, 0.0f );
        m_orientation       = vaQuaternion::Identity;
        //
        m_viewTrans         = vaMatrix4x4::Identity;
        m_projTrans         = vaMatrix4x4::Identity;*/
        //
        m_useReversedZ      = true;
        //
        UpdateSecondaryFOV( );
    }

    public float                           GetYFOV( )                                    { return m_YFOV; }
    public float                           GetXFOV( )                                    { return m_XFOV; }
    public float                           GetAspect( )                                  { return m_aspect; }
    //                              
    public void                            SetYFOV( float yFov )                               { m_YFOV = yFov; m_YFOVMain = true;    UpdateSecondaryFOV( ); }
    public void                            SetXFOV( float xFov )                               { m_XFOV = xFov; m_YFOVMain = false;   UpdateSecondaryFOV( ); }
    //
    public void                            SetNearPlane( float nearPlane )                     { m_nearPlane = nearPlane; }
    public void                            SetFarPlane( float farPlane )                       { m_farPlane = farPlane; }
    //
    public float                           GetNearPlane( )                               { return m_nearPlane; }
    public float                           GetFarPlane( )                                { return m_farPlane; }
    //
    public void                            SetPosition(ReadableVector3f newPos )             { m_position.set(newPos); }
    public void                            SetOrientation(ReadableVector4f newOri )       { m_orientation.set(newOri); }
    public void                            SetOrientationLookAt(ReadableVector3f lookAtPos,ReadableVector3f upVector /*= vaVector3( 0.0f, 0.0f, 1.0f )*/ ){
        Matrix4f mat = CacheBuffer.getCachedMatrix();
        try{
            Matrix4f.lookAt(m_position, lookAtPos, upVector, mat);
            m_orientation.setFromMatrix(mat).invert();
        }finally {
            CacheBuffer.free(mat);
        }
    }
    //
    public ReadableVector3f GetPosition( )                                { return m_position; }
    public Quaternion       GetOrientation( )                             { return m_orientation; }
    public ReadableVector3f GetDirection( )                               { return m_direction; }
    //                              
    public void                            CalcFrustumPlanes(Vector4f planes[] ){
        /*vaMatrix4x4 cameraViewProj = m_viewTrans * m_projTrans;

        vaGeometry::CalculateFrustumPlanes( planes, cameraViewProj );*/

        final Matrix4f cameraViewProj = CacheBuffer.getCachedMatrix();
        try {
            Matrix4f.mul(m_projTrans, m_viewTrans, cameraViewProj);
            Matrix4f.extractFrustumPlanes(cameraViewProj, planes);
        }finally {
            CacheBuffer.free(cameraViewProj);
        }
    }
    //                   
    public Matrix4f             GetWorldMatrix( )                            { return m_worldTrans; }
    public Matrix4f             GetViewMatrix( )                             { return m_viewTrans; }
    public Matrix4f             GetProjMatrix( )                             { return m_projTrans; }
    // same as GetWorldMatrix !!
    public Matrix4f             GetInvViewMatrix( )                          { return m_worldTrans; }
    //

    public final void                            GetZOffsettedProjMatrix( Matrix4f outMat){
        GetZOffsettedProjMatrix(outMat, 1.0f, 0.0f);
    }
    public void                            GetZOffsettedProjMatrix( Matrix4f outMat, float zModMul /*= 1.0f*/, float zModAdd /*= 0.0f*/ ){
        UpdateSecondaryFOV( );
//        mat = vaMatrix4x4::PerspectiveFovLH( m_YFOV, m_aspect, m_nearPlane * zModMul + zModAdd, m_farPlane * zModMul + zModAdd );
        Matrix4f.perspective(m_YFOV, m_aspect, m_nearPlane * zModMul + zModAdd, m_farPlane * zModMul + zModAdd, outMat);
    }
    //                         
    public void                            SetUseReversedZ( boolean useReversedZ )                { m_useReversedZ = useReversedZ; }
    public boolean                         GetUseReversedZ( )                           { return m_useReversedZ; }
    //
    //
        /*
        void                            FillSelectionParams( VertexAsylum::vaSRMSelectionParams & selectionParams );
        void                            FillRenderParams( VertexAsylum::vaSRMRenderParams & renderParams );
        */
    //
    public void                    SetViewportSize( int width, int height ){
        m_viewportWidth = width;
        m_viewportHeight = height;
        m_aspect = width / (float)height;
    }
    //
    public int                             GetViewportWidth( )                   { return m_viewportWidth; }
    public int                             GetViewportHeight( )                  { return m_viewportHeight; }
    //
    public void                            UpdateFrom( VaCameraBase copyFrom ){
        m_YFOVMain          = copyFrom.m_YFOVMain      ;
        m_YFOV              = copyFrom.m_YFOV          ;
        m_XFOV              = copyFrom.m_XFOV          ;
        m_aspect            = copyFrom.m_aspect        ;
        m_nearPlane         = copyFrom.m_nearPlane     ;
        m_farPlane          = copyFrom.m_farPlane      ;
        m_viewportWidth     = copyFrom.m_viewportWidth ;
        m_viewportHeight    = copyFrom.m_viewportHeight;
        m_position          .set(copyFrom.m_position)  ;
        m_orientation       .set(copyFrom.m_orientation)   ;
        UpdateSecondaryFOV( );
    }
    //
    public boolean                            Load( VaStream inStream ){
        throw new UnsupportedOperationException();
    }

    public boolean                            Save( VaStream outStream ){
        throw new UnsupportedOperationException();
    }
    //
//       std::shared_ptr<> &
    public VaCameraControllerBase GetAttachedController( )                    { return m_controller; }
    public void                            AttachController(VaCameraControllerBase cameraController ){
        m_controller = cameraController;

        if( m_controller != null )
            m_controller.CameraAttached( this );
    }
    //
    public void                            Tick( float deltaTime, boolean hasFocus ){
        if( (m_controller != null) && (deltaTime != 0.0f) )
            m_controller.CameraTick( deltaTime, this, hasFocus );

        UpdateSecondaryFOV( );

        final Quaternion orientation = GetOrientation( );
        final ReadableVector3f position = GetPosition( );

        /*m_worldTrans = vaMatrix4x4::FromQuaternion( orientation );
        m_worldTrans.SetTranslation( m_position );*/
        orientation.toMatrix(m_worldTrans);
        m_worldTrans.m30 = m_position.getX();
        m_worldTrans.m31 = m_position.getY();
        m_worldTrans.m32 = m_position.getZ();
        m_worldTrans.m33 = 1;

        /*m_direction = m_worldTrans.GetAxisX();  // forward
        m_viewTrans = m_worldTrans.Inverse( );*/
        Matrix4f.invertRigid(m_worldTrans, m_viewTrans);
        Matrix4f.decompseRigidMatrix(m_viewTrans, null, null, null, m_direction);
        m_direction.scale(-1);

        if( m_useReversedZ )
        {
//            m_projTrans = vaMatrix4x4::PerspectiveFovLH( m_YFOV, m_aspect, m_farPlane, m_nearPlane );
            Matrix4f.perspective(m_YFOV, m_aspect, m_farPlane, m_nearPlane, m_projTrans);

/*#if 0
            // just for testing - use UE4 approach to see if it works with the postprocess stuff
            const float ueGNearClippingPlane = 2.0f;
            m_projTrans.m[2][2] = 0.0f;                 //zf / ( zf - zn );
            m_projTrans.m[3][2] = ueGNearClippingPlane; //( zf * zn ) / ( zn - zf );
#endif*/
        }
        else {
//            m_projTrans = vaMatrix4x4::PerspectiveFovLH (m_YFOV, m_aspect, m_nearPlane, m_farPlane );
            Matrix4f.perspective(m_YFOV, m_aspect, m_nearPlane, m_farPlane, m_projTrans);
        }

        // a hacky way to record camera flythroughs!
//#if 1  TODO
        /*if( vaInputKeyboardBase::GetCurrent( )->IsKeyClicked( ( vaKeyboardKeys )'K' ) && vaInputKeyboardBase::GetCurrent( )->IsKeyDown( KK_CONTROL ) )
        {
            vaFileStream fileOut;
            if( fileOut.Open( vaCore::GetExecutableDirectory( ) + L"camerakeys.txt", FileCreationMode::Append ) )
            {
                string newKey = vaStringTools::Format( "m_flythroughCameraController->AddKey( vaCameraControllerFocusLocationsFlythrough::Keyframe( vaVector3( %.3ff, %.3ff, %.3ff ), vaQuaternion( %.3ff, %.3ff, %.3ff, %.3ff ), 10.0f ) );\n\n",
                    m_position.x, m_position.y, m_position.z, m_orientation.x, m_orientation.y, m_orientation.z, m_orientation.w );
                fileOut.WriteTXT( newKey );
                VA_LOG( "Logging camera key: %s", newKey.c_str() );
            }
        }*/
//#endif
    }

    protected void                            UpdateSecondaryFOV( ){
        if( m_YFOVMain )
        {
            m_XFOV = m_YFOV / m_aspect;
        }
        else
        {
            m_YFOV = m_XFOV * m_aspect;
        }
    }
    //
    protected void                    SetAspect( float aspect )                    { m_aspect = aspect; }
}
