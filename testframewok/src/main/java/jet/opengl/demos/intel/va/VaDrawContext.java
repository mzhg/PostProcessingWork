package jet.opengl.demos.intel.va;

/**
 * Created by mazhen'gui on 2017/11/17.
 */

public class VaDrawContext {
    // vaRenderDeviceContext is used to get/set current render targets and access rendering API stuff like contexts, etc.
    public VaRenderDeviceContext         APIContext;
    public VaCameraBase             Camera;             // Currently selected camera
    public VaRenderingGlobals            Globals;            // Used to set global shader constants, track current frame index, provide some debugging tools, etc.
    public VaLighting Lighting;
    public Object                    UserContext;

    // can be changed at runtime
    public VaRenderPassType                PassType;
    public VaSimpleShadowMap       SimpleShadowMap;

    private boolean renderingGlobalsUpdated;

    public boolean  GetRenderingGlobalsUpdated( )             { return renderingGlobalsUpdated; }
}
