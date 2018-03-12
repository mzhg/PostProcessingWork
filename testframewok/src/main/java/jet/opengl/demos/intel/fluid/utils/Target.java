package jet.opengl.demos.intel.fluid.utils;

import org.lwjgl.util.vector.ReadableVector4f;

import java.util.ArrayList;

/**
 * Canvas on which to draw.<p></p>

 A Target contains one or more Viewports, each of which has its own Camera.
 A Window is a kind of Target, which usually maps to some on screen.
 Targets can also be off-screen textures.<p></p>
 * Created by mazhen'gui on 2018/3/12.
 */

public class Target {
    /** Type identifier for this class */
    public static final int sTypeId = 1414678356 ; ///<

    /** Type identifier */
    protected int  mTypeId;
    /** Position, in pixels, of left edge this target */
    protected int     mLeft;
    /** Position, in pixels, of top edge this target */
    protected int     mTop;
    /** Width, in pixels, of this target */
    protected int     mWidth = 1280;
    /** Height, in pixels, of this target */
    protected int     mHeight = 720;
    /** Whether this target has a depth buffer */
    protected boolean    mDepth;

    /** Render system that owns this target */
    private RenderSystem mRenderSystem;
    /** Viewports within this render target */
    private final ArrayList<Viewport> mViewports = new ArrayList<>();

    public Target( RenderSystem renderSystem , int typeId ){
        mRenderSystem = renderSystem;
        mTypeId = typeId;
    }

    public void clear(){
        mViewports.clear();
    }

    public void preRenderViewports(){}

    public int getTypeId() { return mTypeId ; }

    public void addViewport( Camera camera ,  ReadableVector4f viewport){
        Viewport  pViewport = new Viewport( camera , this , viewport ) ;
        // TODO: Set setAspectRatio on camera given target/viewport shape
        mViewports.add( pViewport ) ;
    }

    public void renderViewports(){
        // See comment in System::UpdateTargets.
        preRenderViewports() ;

        for( Viewport viewport :  mViewports)
        {
            viewport.triggerCamera() ;
        }
    }

    public ArrayList<Viewport> getViewports() { return mViewports ; }

    public RenderSystem getRenderSystem() { return mRenderSystem ; }

    public void setLeft( int left )        { mLeft   = left    ; }
    public void setTop( int top )          { mTop    = top     ; }
    public void setWidth( int width )      { mWidth  = width   ; }
    public void setHeight( int height )    { mHeight = height  ; }
    public void setDepth( boolean depth )  { mDepth  = depth   ; }

    public int getLeft()      { return mLeft   ; }
    public int getTop()       { return mTop    ; }
    public int getWidth()     { return mWidth  ; }
    public int getHeight()    { return mHeight ; }
    public boolean getDepth() { return mDepth  ; }
}
