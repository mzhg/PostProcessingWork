package jet.opengl.renderer.Unreal4.utils;

public enum EClearBinding {
    ENoneBound, //no clear color associated with this target.  Target will not do hardware clears on most platforms
    EColorBound, //target has a clear color bound.  Clears will use the bound color, and do hardware clears.
    EDepthStencilBound, //target has a depthstencil value bound.  Clears will use the bound values and do hardware clears.
}
