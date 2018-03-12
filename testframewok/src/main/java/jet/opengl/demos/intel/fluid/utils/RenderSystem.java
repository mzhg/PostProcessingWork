package jet.opengl.demos.intel.fluid.utils;

import java.util.ArrayList;

import jet.opengl.demos.intel.fluid.render.ApiBase;

/**
 * Created by mazhen'gui on 2018/3/12.
 */

public class RenderSystem {
    private final ArrayList<Target> mTargets = new ArrayList<>();   /// Render targets
    /** Address of low-level render system device, which this object owns. */
    private ApiBase                 mApi;

    public RenderSystem( ApiBase  renderApi ){
        mApi = renderApi;
    }

    /// Return address of Render API object.
    public ApiBase getApi() { return mApi ; }

    /** Add a Render Target such as a window or texture.
     @param target   Render Target
     */
    public void  addTarget( Target  target )
    {
        mTargets.add( target ) ;
    }

    /** Update all Render Targets in this System.<p></p>

     Each Target has Viewports, which this routine renders by calling
     RenderViewports.
     */
    public void updateTargets(){
        for( Target target : mTargets )
        {   // For each render target...

            target.renderViewports() ;
        }
    }
}
