package jet.opengl.demos.intel.fluid.utils;


import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2018/3/12.
 */

public class Viewport {
    /** Camera used to view scene through this viewport. */
    private Camera                  mCamera                 ;   ///
    private Target                  mTarget                 ;   /// Render target this viewport belongs to
    private float                   mRelLeft                ;   /// Position relative to target (in [0,1]) of left edge of this viewport
    private float                   mRelTop                 ;   /// Position relative to target (in [0,1]) of top edge of this viewport
    private float                   mRelWidth               ;   /// Width relative to target (in [0,1]) of this viewport
    private float                   mRelHeight              ;   /// Height relative to target (in [0,1]) of this viewport
    private final Vector4f          mClearColor = new Vector4f();   /// Color used to clear this viewport before rendering into it.
    private DiagnosticTextOverlay   mDiagnosticTextOverlay  ;   /// Field of text used for display diagnostic messages

    public Viewport( Camera camera , Target target , ReadableVector4f viewport ){
        mCamera = camera;
        mTarget = target;
        mRelLeft = viewport.getX();
        mRelTop = viewport.getY();
    }

    private static float fmod(float x, float y) { return x%y;}

    public void triggerCamera(){
        // Cycle clear color.
        mClearColor.x = Math.max( fmod( mClearColor.x + 0.05f , 1.0f ) , 0.2f ) ;
        mClearColor.y = Math.max( fmod( mClearColor.y + 0.02f , 1.0f ) , 0.2f ) ;
        mClearColor.z = Math.max( fmod( mClearColor.z + 0.01f , 1.0f ) , 0.2f ) ;

        // Set up render target.
        mTarget.getRenderSystem().getApi().setViewport(this ) ;
        final float viewportAspectRatio = getRelWidth() * mTarget.getWidth() / ( getRelHeight() * mTarget.getHeight()) ;
        mCamera.setAspectRatio( viewportAspectRatio ) ;

        // Render scene.
        mCamera.renderScene() ;

        // TODO: Set orthogonal projection for overlay. Add SetOrthographic to either Api or Camera, and call from here.
        // TODO: Render overlays here.

        // Render debug text overlay.
        mDiagnosticTextOverlay.render() ;
    }

    public Camera getCamera()     { return mCamera     ; }
    public Target getTarget()     { return mTarget     ; }
    public float getRelLeft()     { return mRelLeft    ; }
    public float getRelTop()      { return mRelTop     ; }
    public float getRelWidth()    { return mRelWidth   ; }
    public float getRelHeight()   { return mRelHeight  ; }
    public Vector4f  getClearColor()  { return mClearColor ; }

    public DiagnosticTextOverlay getDiagnosticTextOverlay() { return mDiagnosticTextOverlay ; }
}
