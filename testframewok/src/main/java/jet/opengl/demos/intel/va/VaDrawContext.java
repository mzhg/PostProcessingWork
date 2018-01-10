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

    boolean renderingGlobalsUpdated;

    public VaDrawContext(VaCameraBase camera, VaRenderDeviceContext deviceContext, VaRenderingGlobals globals, VaLighting lighting){
        this(camera, deviceContext, globals, lighting, null, null);
    }

    public VaDrawContext(VaCameraBase camera, VaRenderDeviceContext deviceContext, VaRenderingGlobals globals, VaLighting lighting /*= nullptr*/,
                         VaSimpleShadowMap simpleShadowMap, Object userContext /*= nullptr*/ )
//            : Camera( camera ), APIContext( deviceContext ), Globals( globals ), Lighting( lighting ), SimpleShadowMap( simpleShadowMap ), UserContext( userContext ), renderingGlobalsUpdated( false )
    {
        Camera = camera;
        APIContext = deviceContext;
        Globals = globals;
        Lighting = lighting;
        SimpleShadowMap = simpleShadowMap;
        UserContext = userContext;
        renderingGlobalsUpdated = false;
        PassType = VaRenderPassType.Unknown;
    }

    public boolean  GetRenderingGlobalsUpdated( )             { return renderingGlobalsUpdated; }
}
