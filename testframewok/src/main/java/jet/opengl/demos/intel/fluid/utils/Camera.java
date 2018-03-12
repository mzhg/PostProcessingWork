package jet.opengl.demos.intel.fluid.utils;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.demos.intel.fluid.scene.ISceneManager;
import jet.opengl.demos.intel.fluid.scene.SceneNodeBase;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2018/3/12.
 */

public class Camera extends SceneNodeBase {
    public static final int sTypeId = 1667329394;

    private static final int NUM_FRUSTUM_PLANES = 6 ;

    /** World-space position at which this camera looks */
    private final Vector3f mLookAt = new Vector3f(0,0,0);
    /** World-space approximate direction toward which this camera top points  */
    private final Vector3f mUpApproximate = new Vector3f(0,1,0);

    // Projection transformation parameters
    /** Field of view angle (in degrees) along vertical direction */
    private float               mFieldOfViewVert = 90;
    /** Aspect ratio (height to width) */
    private float               mAspectRatio = 16f/9f;
    /** World-space distance to near clip plane */
    private float               mNearClipDist = 1.f;
    /** World-space distance to far clip plane */
    private float               mFarClipDist = 1000.f;

    public Camera( ISceneManager sceneManager ){
        super(sceneManager, sTypeId);

        // Move camera away from default look-at point.
        setPosition( new Vector3f( 2.0f , 0.0f , 0.0f ) ) ;
    }

    @Override
    public void render() {
        renderChildren();
    }

    /** Render the scene which this camera sees.

     Caller (e.g. Viewport::TriggerCamera) should set Camera aspect ratio
     (by calling Camera::SetAspectRatio) just before calling this routine.
     */
    public void renderScene(){
        getSceneManager().renderScene(this ) ;
    }

    public void setEye(ReadableVector3f eye )          { setPosition( eye ) ; }

    /// Return World-space position of this camera.
    /// \see GetPosition.
    public ReadableVector3f    getEye()                { return getPos3() ; }

    /// Set world-space position at which this camera looks.
    public void setLookAt(ReadableVector3f lookAt )    { mLookAt.set(lookAt); }

    /// Return world-space position at which this camera looks.
    public ReadableVector3f  getLookAt()               { return mLookAt; }

    /// Return World-space approximate direction toward which this camera top point
    public ReadableVector3f getUpApproximate()         { return mUpApproximate ; }

    public void  setFieldOfViewVert( float fovVert ) { mFieldOfViewVert = fovVert ; }
    /// Return Field of view angle (in degrees) along vertical direction.
    public float getFieldOfViewVert()                { return mFieldOfViewVert ; }

    /** Set ratio of width to height.<p></p>

     This should match the viewport aspect ratio, otherwise the image will be distorted.
     This should therefore be set by the Viewport just prior to calling Camera::RenderScene.
     */
    public void  setAspectRatio( float aspectRatio ){
        assert (aspectRatio >= 0.0f);
        assert (!Float.isNaN(aspectRatio));
        assert (!Float.isInfinite(aspectRatio));

        mAspectRatio = aspectRatio ;
    }

    /// Return Ratio of width to height.
    public float getAspectRatio()  { return mAspectRatio ; }

    /// Return World-space distance to near clip plane.
    public float getNearClipDist() { return mNearClipDist ; }


    /// Return World-space distance to far clip plane.
    public float getFarClipDist()  { return mFarClipDist ; }

    public void setPerspective( float verticalFov , float aspectRatio , float nearClipDistance , float farClipDistance ){
        setFieldOfViewVert( verticalFov ) ;
        setAspectRatio( aspectRatio ) ;
        mNearClipDist = nearClipDistance ;
        mFarClipDist = farClipDistance ;
    }

    /** Get camera position in spherical coordinates, relative to target.

     @param azimuth      Angle, in radians, along horizontal direction, off positive x axis.

     @param elevation    Angle, in radians, off upward vertical axis.

     @param radius       Distance, in world units, of camera (eye) from target (look-at)
     */
    public void getOrbit( Vector3f out ){
//        Vec3 vRelativeEyePos    = GetEye() - GetLookAt() ;
        Vector3f vRelativeEyePos = Vector3f.sub(getEye(), getLookAt(), null);
        out.z   = vRelativeEyePos.length() ;
        out.x   = (float) Math.atan2( vRelativeEyePos.y , vRelativeEyePos.x );
        out.y   = (float) Math.acos( vRelativeEyePos.z / out.z );
    }

    /// Set camera position in spherical coordinates, relative to target.
    public void setOrbit( float azimuth , float elevation , float radius ){
        // Avoid "gimbal lock" by limiting elevation angle to avoid the poles.
        final float sAvoidPoles = 0.001f ;
        elevation = Numeric.clamp( elevation , sAvoidPoles , Numeric.PI * ( 1.0f - sAvoidPoles ) ) ;

        Vector3f vRelativeEyePos = new Vector3f(   (float)(Math.sin( elevation ) * Math.cos( azimuth ) * radius)
                ,   (float)(Math.sin( elevation ) * Math.sin( azimuth ) * radius)
                ,   (float)(Math.cos( elevation ) * radius)) ;

        Vector3f.add(vRelativeEyePos, getLookAt(), vRelativeEyePos);
        setEye( vRelativeEyePos /*+ GetLookAt()*/ ) ;
    }
}
